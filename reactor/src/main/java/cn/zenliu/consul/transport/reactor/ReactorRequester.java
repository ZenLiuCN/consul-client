/*
 * Source of consul_client
 * Copyright (C) 2023.  Zen.Liu
 *
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0"
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; version 2.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Class Path Exception
 * Linking this library statically or dynamically with other modules is making a combined work based on this library. Thus, the terms and conditions of the GNU General Public License cover the whole combination.
 *  As a special exception, the copyright holders of this library give you permission to link this library with independent modules to produce an executable, regardless of the license terms of these independent modules, and to copy and distribute the resulting executable under terms of your choice, provided that you also meet, for each linked independent module, the terms and conditions of the license of that module. An independent module is a module which is not derived from or based on this library. If you modify this library, you may extend this exception to your version of the library, but you are not obligated to do so. If you do not wish to do so, delete this exception statement from your version.
 */

package cn.zenliu.consul.transport.reactor;


import cn.zenliu.java.consul.trasport.*;
import com.google.auto.service.AutoService;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.client.HttpClient;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class ReactorRequester extends Requester.AbstractRequester<ReactorRequester> {
    record RectorResponder<T>(
            HttpClient.ResponseReceiver<?> client,
            ExecutorService executor,
            Codec codec,
            Type type,
            T def
    ) implements Responder<T> {
        @SuppressWarnings("unchecked")
        @Override
        public Response<Data<T>> response() {
            var t = type == null || type.equals(Void.class) || type.equals(Void.TYPE) ? null : type;
            return new Response<>(CompletableFuture.supplyAsync(() -> client.responseSingle((r, b) -> {
                var h = new HashMap<String, String>();
                r.responseHeaders().forEach(e -> h.put(e.getKey(), e.getValue()));
                var d = Data.BaseData.builder()
                        .code(r.status().code())
                        .headers(h);

                if (r.status() == HttpResponseStatus.OK) {
                    if (t != null)
                        return b.doOnDiscard(ByteBuf.class, buf -> {
                            try {
                                buf.release();
                            } catch (Exception ignore) {
                            }
                        }).map(buf -> d.body(codec.decode(buf, t)).build());
                    return Mono.just(d.build());
                } else if (def != null && r.status() == HttpResponseStatus.NOT_FOUND) {
                    return Mono.just(d.body(def).build());
                } else {
                    return b.asString().map(err -> (Data<T>) d.error(err).build());
                }
            }).block(), executor));
        }
    }

    record ReactorSender<T>(
            HttpClient.RequestSender client,
            ExecutorService executor,
            Codec codec,
            Type type,
            T def
    ) implements Sender<T> {
        @Override
        public Responder<T> send(@Nullable Object body) {
            if (body == null) {
                return new RectorResponder<>(client.send(Mono.empty()), executor, codec, type, def);
            }
            return new RectorResponder<>(client.send(Mono.fromSupplier(() -> {
                var buf = ByteBufAllocator.DEFAULT.buffer();
                buf.retain();
                try {
                    codec.encode(buf, body);
                    return buf;
                } finally {
                    buf.release();
                }
            }).subscribeOn(Schedulers.fromExecutor(executor)).doOnDiscard(ByteBuf.class, buf -> {
                try {
                    buf.release();
                } catch (Exception ignore) {
                }
            })), executor, codec, type, def);
        }

        @Override
        public Responder<T> sendRaw(byte @Nullable [] body) {
            if (body == null) {
                return new RectorResponder<>(client.send(Mono.empty()), executor, codec, type, def);
            }
            return new RectorResponder<>(client.send(Mono.just(Unpooled.wrappedBuffer(body)).subscribeOn(Schedulers.fromExecutor(executor)).doOnDiscard(ByteBuf.class, buf -> {
                try {
                    buf.release();
                } catch (Exception ignore) {
                }
            })), executor, codec, type, def);
        }

        @Override
        public Responder<T> sendRaw(@Nullable String body) {
            if (body == null) {
                return new RectorResponder<>(client.send(Mono.empty()), executor, codec, type, def);
            }
            return new RectorResponder<>(client.send(ByteBufFlux.fromString(Mono.just(body)).subscribeOn(Schedulers.fromExecutor(executor)).doOnDiscard(ByteBuf.class, buf -> {
                try {
                    buf.release();
                } catch (Exception ignore) {
                }
            })), executor, codec, type, def);
        }
    }

    final HttpClient client;
    final ExecutorService executor;
    final Codec codec;

    public ReactorRequester(HttpClient client, ExecutorService executor, Codec codec) {
        this.client = client == null ? HttpClient.create() : client;
        this.executor = executor;
        this.codec = codec;
    }

    public <T> Sender<T> request(HttpMethod method, @Nullable Type type, @Nullable T def) {
        return new ReactorSender<>(client
                .headers(h -> {
                    if (header != null && !header.isEmpty()) {
                        header.forEach(h::add);
                    }
                })
                .request(method).uri(url()),
                executor, codec,
                type, def);
    }

    @Override
    public <T> Sender<T> get(@Nullable Type type, @Nullable T def) {
        return request(HttpMethod.GET, type, def);
    }

    @Override
    public <T> Sender<T> put(@Nullable Type type, @Nullable T def) {
        return request(HttpMethod.PUT, type, def);
    }

    @Override
    public <T> Sender<T> delete(@Nullable Type type, @Nullable T def) {
        return request(HttpMethod.POST, type, def);
    }

    @Override
    protected ReactorRequester self() {
        return this;
    }

    @AutoService(Requester.Factory.class)
    static class Factory implements Requester.Factory {

        @Override
        public Requester<?> make(ExecutorService executor, String baseUrl, Codec codec, boolean debug) {
            return new ReactorRequester(HttpClient.create().baseUrl(baseUrl).secure().wiretap(debug), executor, codec);
        }
    }
}