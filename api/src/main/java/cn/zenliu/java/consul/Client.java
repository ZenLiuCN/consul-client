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

package cn.zenliu.java.consul;


import cn.zenliu.java.consul.trasport.Codec;
import cn.zenliu.java.consul.trasport.Requester;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;

/**
 * Consul client
 *
 * @author Zen.Liu
 * @since 2023-08-18
 */
public interface Client extends AutoCloseable {


    void close();


    Endpoints.Acl<?> acl(@Nullable String token, @Nullable Values.QueryParameter query);

    Endpoints.Agent<?> agent(@Nullable String token, @Nullable Values.QueryParameter query);

    Endpoints.Catalog<?> catalog(@Nullable String token, @Nullable Values.QueryParameter query);

    Endpoints.Coordinate<?> coordinate(@Nullable String token, @Nullable Values.QueryParameter query);

    Endpoints.Events<?> event(@Nullable String token, @Nullable Values.QueryParameter query);

    Endpoints.Health<?> health(@Nullable String token, @Nullable Values.QueryParameter query);

    Endpoints.Store<?> store(@Nullable String token, @Nullable Values.QueryParameter query);

    Endpoints.Query<?> query(@Nullable String token, @Nullable Values.QueryParameter query);

    Endpoints.Sessions<?> session(@Nullable String token, @Nullable Values.QueryParameter query);

    Endpoints.Status<?> status(@Nullable String token, @Nullable Values.QueryParameter query);


    class Acl extends Context.Base<Acl> implements Endpoints.Acl<Acl> {


        protected Acl(Supplier<Requester<?>> createRequester, @Nullable String token, @Nullable QueryParameter parameter) {
            super(createRequester, token, parameter);
        }
    }

    class Agent extends Context.Base<Agent> implements Endpoints.Agent<Agent> {


        protected Agent(Supplier<Requester<?>> createRequester, @Nullable String token, @Nullable QueryParameter parameter) {
            super(createRequester, token, parameter);
        }
    }

    class Catalog extends Context.Base<Catalog> implements Endpoints.Catalog<Catalog> {


        protected Catalog(Supplier<Requester<?>> createRequester, @Nullable String token, @Nullable QueryParameter parameter) {
            super(createRequester, token, parameter);
        }
    }

    class Coordinate extends Context.Base<Coordinate> implements Endpoints.Coordinate<Coordinate> {


        protected Coordinate(Supplier<Requester<?>> createRequester, @Nullable String token, @Nullable QueryParameter parameter) {
            super(createRequester, token, parameter);
        }
    }

    class Events extends Context.Base<Events> implements Endpoints.Events<Events> {


        protected Events(Supplier<Requester<?>> createRequester, @Nullable String token, @Nullable QueryParameter parameter) {
            super(createRequester, token, parameter);
        }
    }

    class Health extends Context.Base<Health> implements Endpoints.Health<Health> {


        protected Health(Supplier<Requester<?>> createRequester, @Nullable String token, @Nullable QueryParameter parameter) {
            super(createRequester, token, parameter);
        }
    }

    class Store extends Context.Base<Store> implements Endpoints.Store<Store> {


        protected Store(Supplier<Requester<?>> createRequester, @Nullable String token, @Nullable QueryParameter parameter) {
            super(createRequester, token, parameter);
        }
    }

    class Query extends Context.Base<Query> implements Endpoints.Query<Query> {

        protected Query(Supplier<Requester<?>> createRequester, @Nullable String token, @Nullable QueryParameter parameter) {
            super(createRequester, token, parameter);
        }
    }

    class Sessions extends Context.Base<Sessions> implements Endpoints.Sessions<Sessions> {


        protected Sessions(Supplier<Requester<?>> createRequester, @Nullable String token, @Nullable QueryParameter parameter) {
            super(createRequester, token, parameter);
        }
    }

    class Status extends Context.Base<Status> implements Endpoints.Status<Status> {

        protected Status(Supplier<Requester<?>> createRequester, @Nullable String token, @Nullable QueryParameter parameter) {
            super(createRequester, token, parameter);
        }
    }

    abstract class BaseClient implements Client {
        protected abstract Requester<?> createRequester();


        @Override
        public void close() {

        }

        @Override
        public Endpoints.Acl<?> acl(@Nullable String token, @Nullable Values.QueryParameter query) {
            return new Acl(this::createRequester, token, query);
        }

        @Override
        public Endpoints.Agent<?> agent(@Nullable String token, @Nullable Values.QueryParameter query) {
            return new Agent(this::createRequester, token, query);
        }

        @Override
        public Endpoints.Catalog<?> catalog(@Nullable String token, @Nullable Values.QueryParameter query) {
            return new Catalog(this::createRequester, token, query);
        }

        @Override
        public Endpoints.Coordinate<?> coordinate(@Nullable String token, @Nullable Values.QueryParameter query) {
            return new Coordinate(this::createRequester, token, query);
        }

        @Override
        public Endpoints.Events<?> event(@Nullable String token, @Nullable Values.QueryParameter query) {
            return new Events(this::createRequester, token, query);
        }

        @Override
        public Endpoints.Health<?> health(@Nullable String token, @Nullable Values.QueryParameter query) {
            return new Health(this::createRequester, token, query);
        }

        @Override
        public Endpoints.Store<?> store(@Nullable String token, @Nullable Values.QueryParameter query) {
            return new Store(this::createRequester, token, query);
        }

        @Override
        public Endpoints.Query<?> query(@Nullable String token, @Nullable Values.QueryParameter query) {
            return new Query(this::createRequester, token, query);
        }

        @Override
        public Endpoints.Sessions<?> session(@Nullable String token, @Nullable Values.QueryParameter query) {
            return new Sessions(this::createRequester, token, query);
        }

        @Override
        public Endpoints.Status<?> status(@Nullable String token, @Nullable Values.QueryParameter query) {
            return new Status(this::createRequester, token, query);
        }
    }

    final class ClientImpl extends BaseClient {
        private final Requester.Factory factory;
        private final ExecutorService executor;
        private final String baseUrl;
        private final Codec codec;
        private final boolean debug;

        public ClientImpl(Requester.Factory factory, ExecutorService executor, String baseUrl, Codec codec, boolean debug) {
            this.factory = factory;
            this.executor = executor;
            this.baseUrl = baseUrl;
            this.codec = codec;
            this.debug = debug;
        }

        @Override
        protected Requester<?> createRequester() {
            return factory.make(executor, baseUrl, codec, debug);
        }
    }

    /**
     * Create a Client via ServiceLoader
     *
     * @param executor the executor for requests, default will use {@link ForkJoinPool#commonPool()}
     * @param baseUrl  the base url of Consul HTTP api, never matters ended with slash or not.
     * @param debug    does debug mode, some implement may not support this parameter.
     * @return created client
     */
    static Client create(@Nullable ExecutorService executor, String baseUrl, boolean debug) {
        return new ClientImpl(Requester.Factory.load(), executor == null ? ForkJoinPool.commonPool() : executor, baseUrl, Codec.Provider.load(debug), debug);
    }
}
