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

package cn.zenliu.java.consul.trasport;


import lombok.EqualsAndHashCode;
import lombok.Synchronized;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Requester build a request
 *
 * @author Zen.Liu
 * @since 2023-08-19
 */
public interface Requester<S extends Requester<S>> {

    /**
     * Set a base url , otherwise will prepend '/' at path. Each time invoke this method will replace exists base url.
     *
     * @param baseUrl the base url.
     * @return self
     */
    S base(@Nullable CharSequence baseUrl);

    /**
     * add or replace header
     *
     * @param key null or empty will ignore adding header.
     * @param val null or empty will ignore adding header and also remove exists one.
     * @return self
     */
    S header(@Nullable CharSequence key, @Nullable CharSequence val);

    /**
     * Add path segments.
     *
     * @param segments path segments:<br/>
     *                 1. Null or empty array will ignore the invocation.<br/>
     *                 2. Null or empty array value will ignore the value.<br/>
     * @return self
     */
    S path(@Nullable CharSequence... segments);

    /**
     * Add query parameters,  first success invocation will end path input.
     *
     * @param key    key of query parameter, if null will ignore this action.
     * @param values values for query parameters: <br/>
     *               1. Null or empty array will ignore full invoke action.<br/>
     *               2. Null or empty array value will ignore just current value.<br/>
     *               3. empty array will just write the key without value.<br/>
     * @return self
     */
    S query(@Nullable CharSequence key, @Nullable CharSequence... values);

    /**
     * @param use the setter for this requester.
     * @return self
     */
    S query(@Nullable Consumer<Requester<?>> use);


    /**
     * Add query parameters, first success invocation will end path input.
     *
     * @param cond   condition, if false will ignore this action.
     * @param key    key of query parameter.
     * @param values values for query parameter.
     * @return self
     * @see #query(CharSequence, CharSequence...)
     */
    S query(boolean cond, @Nullable CharSequence key, @Nullable CharSequence... values);

    /**
     * @param type the response body type, maybe {@link TypeRef}
     * @param def  the optional default value if response with 404
     * @param <T>  type
     * @return Request
     */
    <T> Sender<T> get(@Nullable Type type, @Nullable T def);


    /**
     * @param type the response body type, maybe {@link TypeRef}
     * @param def  the optional default value if response with 404
     * @param <T>  type
     * @return BodyRequest
     */
    <T> Sender<T> put(@Nullable Type type, @Nullable T def);

    /**
     * @param type the response body type, maybe {@link TypeRef}
     * @param def  the optional default value if response with 404
     * @param <T>  type
     * @return BodyRequest
     */
    <T> Sender<T> delete(@Nullable Type type, @Nullable T def);


    @EqualsAndHashCode
    abstract class AbstractRequester<S extends AbstractRequester<S>> implements Requester<S> {
        protected final StringBuilder uri = new StringBuilder();
        protected final AtomicInteger state = new AtomicInteger(-1);
        protected volatile Map<CharSequence, CharSequence> header;
        protected volatile CharSequence base;

        protected AbstractRequester() {
        }

        protected abstract S self();

        @Override
        public S base(@Nullable CharSequence baseUrl) {
            assert state.get() != Integer.MIN_VALUE : "request already built";
            synchronized (uri) {
                base = baseUrl;
            }
            return self();
        }

        @Override
        @Synchronized("uri")
        public S path(@Nullable CharSequence... p) {
            assert state.get() < 0 && state.get() != Integer.MIN_VALUE : "path already ended";
            if (p != null) {
                for (var cs : p) {
                    if (cs != null && !cs.isEmpty()) {
                        var v = state.getAndDecrement();
                        if (v < -1) {
                            uri.append("/").append(cs);
                        } else {
                            uri.append(cs);
                        }
                    }
                }
            }
            return self();
        }

        @Override
        @Synchronized("uri")
        public S query(@Nullable CharSequence key, @Nullable CharSequence... values) {
            if (key == null || key.isEmpty() || values == null) return self();
            if (state.get() < 0) state.set(1);
            assert state.get() >= 1 : "query already end";
            if (values.length > 0) {
                for (var cs : values) {
                    if (cs != null && !cs.isEmpty()) {
                        var v = state.getAndIncrement();
                        if (v > 1) {
                            uri.append('&').append(key).append('=').append(cs);
                        } else {
                            uri.append('?').append(key).append('=').append(cs);
                        }
                    }
                }
            } else {
                var v = state.getAndIncrement();
                if (v > 1) {
                    uri.append('&').append(key);
                } else {
                    uri.append('?').append(key);
                }
            }
            return self();
        }

        @Override
        public S query(Consumer<Requester<?>> use) {
            if (use == null) return self();
            use.accept(self());
            return self();
        }


        @Override
        public S query(boolean cond, @Nullable CharSequence key, @Nullable CharSequence... values) {
            if (!cond || key == null || key.isEmpty() || values == null) return self();
            return query(key, values);
        }

        @Override
        public S header(CharSequence key, CharSequence val) {
            if (key == null || key.isEmpty()) return self();
            if (header == null) {
                synchronized (uri) {
                    if (this.header == null)
                        this.header = new ConcurrentHashMap<>();
                }
            }
            if (val == null || val.isEmpty()) {
                header.remove(key);
            } else
                header.put(key, val);
            return self();
        }

        protected String url() {
            if (state.get() == Integer.MIN_VALUE) {
                return uri.toString();
            }
            state.set(Integer.MIN_VALUE);
            if (base != null) {
                if (uri.charAt(0) == '/')
                    return uri.insert(0, base).toString();
                else {
                    return uri.insert(0, '/').insert(0, base).toString();
                }
            } else
                return uri.insert(0, '/').toString();
        }
    }


    interface Factory {
        static Factory load() {
            return ServiceLoader.load(Factory.class, Factory.class.getClassLoader()).findFirst().orElseThrow(() -> new NoSuchElementException("missing Factory implement"));
        }

        Requester<?> make(ExecutorService executor, String baseUrl, Codec codec, boolean debug);
    }

}
