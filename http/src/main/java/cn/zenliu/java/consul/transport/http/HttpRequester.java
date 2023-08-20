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

package cn.zenliu.java.consul.transport.http;

import cn.zenliu.java.consul.trasport.*;
import com.google.auto.service.AutoService;
import io.netty.buffer.*;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;

/**
 * @author Zen.Liu
 * @since 2023-08-20
 */
public class HttpRequester extends Requester.AbstractRequester<HttpRequester> {
    @EqualsAndHashCode(callSuper = true)
    @ToString
    static class ToByteBufSubscriber extends CompletableFuture<ByteBuf> implements Flow.Subscriber<List<ByteBuffer>> {

        final List<ByteBuffer> buffers = new ArrayList<>();
        volatile boolean cancelled;
        volatile Throwable error;
        volatile ByteBuf buf;

        volatile Flow.Subscription sub;

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            if (this.sub != null) {
                sub.cancel();
                return;
            }
            sub = subscription;
            sub.request(Integer.MAX_VALUE);
        }

        @Override
        public void onNext(List<ByteBuffer> item) {
            synchronized (buffers) {
                buffers.addAll(item);
            }
            sub.request(Integer.MAX_VALUE);
        }

        @Override
        public void onError(Throwable throwable) {
            assert this.error == null : "already have error";
            this.error = throwable;
            failedFuture(throwable);
        }

        @Override
        public void onComplete() {
            synchronized (buffers) {
                this.buf = Unpooled.wrappedBuffer(buffers.toArray(ByteBuffer[]::new));
                complete(this.buf);
            }
        }

        @SneakyThrows
        public String getString() {
            return new String(ByteBufUtil.getBytes(get()));
        }

        @SneakyThrows
        public <T> T getJson(Codec codec, Type type) {
            return codec.decode(get(), type);
        }
    }

    record HttpSender<T>(
            ExecutorService executor,
            HttpClient client,
            HttpRequest.Builder request,
            String method,
            Codec codec,
            Type type,
            T def

    ) implements Sender<T> {
        @Override
        public Responder<T> send(@Nullable Object body) {
            if (body == null) {
                return new HttpResponder<>(executor, client, request
                        .method(method, HttpRequest.BodyPublishers.noBody())
                        .build(), codec, type, def);
            }
            var buf = ByteBufAllocator.DEFAULT.buffer();
            codec.encode(buf, body);
            @SuppressWarnings("resource") var is = new ByteBufInputStream(buf, true);
            return new HttpResponder<>(executor, client, request
                    .method(method, HttpRequest.BodyPublishers.ofInputStream(() -> is))
                    .build(), codec, type, def);
        }

        @Override
        public Responder<T> sendRaw(byte @Nullable [] body) {
            if (body == null) {
                return new HttpResponder<>(executor, client, request
                        .method(method, HttpRequest.BodyPublishers.noBody())
                        .build(), codec, type, def);
            }
            return new HttpResponder<>(executor, client, request
                    .method(method, HttpRequest.BodyPublishers.ofByteArray(body))
                    .build(), codec, type, def);
        }

        @Override
        public Responder<T> sendRaw(@Nullable String body) {
            if (body == null) {
                return new HttpResponder<>(executor, client, request
                        .method(method, HttpRequest.BodyPublishers.noBody())
                        .build(), codec, type, def);
            }
            return new HttpResponder<>(executor, client, request
                    .method(method, HttpRequest.BodyPublishers.ofString(body))
                    .build(), codec, type, def);
        }

        @Override
        public Responder<T> sendRaw(@Nullable ByteBuf body) {
            if (body == null) {
                return new HttpResponder<>(executor, client, request
                        .method(method, HttpRequest.BodyPublishers.noBody())
                        .build(), codec, type, def);
            }
            //assert body.refCnt() == 1 : "buf have refCnt " + body.refCnt();
            @SuppressWarnings("resource") var is = new ByteBufInputStream(body, true);
            return new HttpResponder<>(executor, client, request
                    .method(method, HttpRequest.BodyPublishers.ofInputStream(() -> is))
                    .build(), codec, type, def);
        }
    }


    record HttpResponder<T>(
            ExecutorService executor,
            HttpClient client,
            HttpRequest request,
            Codec codec,
            Type type,
            T def
    ) implements Responder<T> {

        @Override
        public Response<Data<T>> response() {
            var t = type == null || type.equals(Void.class) || type.equals(Void.TYPE) ? null : type;
            return new Response<>(CompletableFuture.supplyAsync(() -> {
                try {
                    var r = client.send(request, HttpResponse.BodyHandlers.ofPublisher());
                    var d = Data.BaseData.builder();
                    var h = new HashMap<String, String>();
                    r.headers().map().forEach((k, v) -> h.put(k, v.isEmpty() ? null : String.join(",", v)));
                    d.code(r.statusCode()).headers(h);
                    if (r.statusCode() == 200) {
                        if (t != null) {
                            var buf = new ToByteBufSubscriber();
                            r.body().subscribe(buf);
                            return d.body(buf.getJson(codec, t)).build();
                        } else {
                            return d.build();
                        }
                    } else if (def != null && r.statusCode() == 404) {
                        return d.body(def).build();
                    } else {
                        var buf = new ToByteBufSubscriber();
                        r.body().subscribe(buf);
                        return d.error(r.request().uri().toASCIIString() + "\n" + buf.getString()).build();
                    }
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }, executor));
        }
    }

    protected final HttpClient client;
    protected final ExecutorService executor;
    protected final Codec codec;


    public HttpRequester(@Nullable HttpClient client, String baseUrl, ExecutorService executor, Codec codec) {
        this.client = client == null ? HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build() : client;
        this.executor = executor;
        this.codec = codec;
        super.base(baseUrl);
    }

    protected HttpRequest.Builder request() {
        var b = HttpRequest.newBuilder();
        if (header != null && !header.isEmpty()) {
            header.forEach((k, v) -> b.header(k.toString(), v.toString()));
        }
        b.uri(URI.create(url()));
        return b;
    }

    @Override
    public <T> Sender<T> get(@Nullable Type type, @Nullable T def) {
        return new HttpSender<>(executor, client, request(), "GET", codec, type, def);
    }

    @Override
    public <T> Sender<T> put(@Nullable Type type, @Nullable T def) {
        return new HttpSender<>(executor, client, request(), "PUT", codec, type, def);
    }

    @Override
    public <T> Sender<T> delete(@Nullable Type type, @Nullable T def) {
        return new HttpSender<>(executor, client, request(), "DELETE", codec, type, def);
    }

    @Override
    protected HttpRequester self() {
        return this;
    }

    @AutoService(Requester.Factory.class)
    public static class Factory implements Requester.Factory {
        @Override
        public Requester<?> make(ExecutorService executor, String baseUrl, Codec codec, boolean debug) {
            if (debug) {
                System.setProperty("jdk.httpclient.HttpClient.log", "errors,requests,headers,frames:all,ssl,trace,channel");
                //try install jul-to-slf4j
                try {
                    var cls = Class.forName("org.slf4j.bridge.SLF4JBridgeHandler");
                    cls.getMethod("removeHandlersForRootLogger").invoke(null);
                    cls.getMethod("install").invoke(null);
                } catch (Exception ignore) {

                }
            }

            return new HttpRequester(HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).executor(executor).build(), baseUrl, executor, codec);
        }
    }
}
