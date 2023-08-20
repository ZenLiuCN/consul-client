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

import cn.zenliu.java.consul.trasport.Data;
import cn.zenliu.java.consul.trasport.Requester;
import cn.zenliu.java.consul.trasport.TypeRef;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.ApiStatus;

import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;


/**
 * Constants and data types for consul api.
 * All generated types are with suffix of 'Value' inside 'ConsulValues'.
 *
 * @author Zen.Liu
 * @since 2023-08-20
 */

public interface Values {
    /**
     * The HTTP api version
     */
    String VERSION = "v1";

    String TOKEN = "X-Consul-Token";
    String INDEX = "X-Consul-Index";
    String KNOWN_LEADER = "X-Consul-Knownleader";
    String LAST_CONTACT = "X-Consul-Lastcontact";
    String QUERY_BACKED = "X-Consul-Query-Backend";
    String DEFAULT_ACL_POLICY = "X-Consul-Default-Acl-Policy";


    public static final Type STRING_LIST = new TypeRef<List<String>>() {
    }.type();
    public static final Type MAP_STRING_LIST = new TypeRef<Map<String, List<String>>>() {
    }.type();


    enum Consistency {
        DEFAULT, STALE, CONSISTENT;

        public static boolean valid(Consistency consistency) {
            return consistency != null && consistency != DEFAULT;
        }
    }

    /**
     * Base Query Parameter
     */
    interface Parameter extends Consumer<Requester<?>> {
        static String encode(String value) {
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
        }


        static String seconds(long timeSeconds) {
            return timeSeconds + "s";
        }

    }

    @Value
    @Builder
    @Accessors(fluent = true)
    class QueryParameter implements Parameter {
        public static final QueryParameter DEFAULT = QueryParameter.builder().build();


        String dataCenter;

        @Builder.Default
        Consistency consistency = Consistency.DEFAULT;

        @Builder.Default
        long waitTime = -1;

        @Builder.Default
        long index = -1;

        String near;

        @Override
        public void accept(Requester<?> q) {
            if (this == DEFAULT) return;
            if (dataCenter() != null) q.query("dc", dataCenter());
            if (Consistency.valid(consistency())) q.query(consistency().name().toLowerCase());
            if (waitTime() != -1) q.query("wait", Parameter.seconds(waitTime()));
            if (index() != -1) q.query("index", Long.toUnsignedString(index()));
            if (near() != null) q.query("near", Parameter.encode(near()));
        }
    }

    @Value
    @Builder
    @Accessors(fluent = true)
    class Info<T> {

        T value;

        Long index;

        Boolean knownleader;

        Long contract;

        String queryBackend;

        String defaultAclPolicy;


        public <R> Info<R> map(Function<T, R> map) {
            return Info.<R>builder()
                    .index(index())
                    .knownleader(knownleader())
                    .contract(contract())
                    .queryBackend(queryBackend())
                    .defaultAclPolicy(defaultAclPolicy())
                    .value(map.apply(value()))
                    .build();
        }

        @ApiStatus.Internal
        public static <T> Info<T> parse(Data<T> i) {
            if (i.error() != null) throw new Error(i);
            return Info.<T>builder()
                    .index(Optional.ofNullable(i.header(INDEX)).map(Long::parseUnsignedLong).orElse(null))
                    .knownleader(Optional.ofNullable(i.header(KNOWN_LEADER)).map(Boolean::parseBoolean).orElse(null))
                    .contract(Optional.ofNullable(i.header(LAST_CONTACT)).map(Long::parseUnsignedLong).orElse(null))
                    .queryBackend(i.header(QUERY_BACKED))
                    .defaultAclPolicy(i.header(DEFAULT_ACL_POLICY))
                    .value(i.body())
                    .build();
        }

        @ApiStatus.Internal
        public static <T> Info<T> one(Data<List<T>> i) {
            if (i.error() != null) throw new Error(i);
            return parse(i).map(l -> {
                if (l.size() == 0) return null;
                if (l.size() > 1) throw new Error(500, "value more than one: size " + l.size());
                return l.get(0);
            });
        }

        @ApiStatus.Internal
        public static Info<String> id(Data<IDOnly> i) {
            if (i.error() != null) throw new Error(i);
            return parse(i).map(IDOnly::ID);
        }


    }

    @Value
    @Builder
    @Accessors(fluent = true)
    class NodeParameter implements Parameter {

        String datacenter;


        String near;

        Map<String, String> nodeMeta;

        @Override
        public void accept(Requester<?> q) {
            q
                    .query(near() != null, "near", near())
                    .query(datacenter() != null, "dc", datacenter())
                    .query(near() != null, "near", near())
            ;
            if (nodeMeta() != null) {
                nodeMeta().forEach((k, v) -> q.query("node-meta", k + ":" + v));
            }
        }
    }

    @Value
    @Builder
    @Accessors(fluent = true)
    class IDOnly implements JsonValue {

        String ID;
    }

    @Value
    @Builder
    @Accessors(fluent = true)
    class Check implements JsonValue {
        public enum Status implements JsonValue {

            unknown,

            passing,

            warning,

            critical
        }

        public static final Type MAP = new TypeRef<Map<String, Check>>() {
        }.type();
        public static final Type LIST = new TypeRef<List<Check>>() {
        }.type();

        String Node;

        String CheckID;

        String Name;

        Status Status;

        String Notes;

        String Output;

        String ServiceID;

        String ServiceName;


        List<String> ServiceTags;

        Long CreateIndex;

        Long ModifyIndex;

    }

    @Value
    @Builder
    @Accessors(fluent = true)
    class Node implements JsonValue {
        public static final Type LIST = new TypeRef<List<Node>>() {
        }.type();


        String ID;


        String Node;


        String Address;


        String Datacenter;


        Map<String, String> TaggedAddresses;


        Map<String, String> Meta;


        Long CreateIndex;


        Long ModifyIndex;
    }

    @Value
    @Builder
    @Accessors(fluent = true)
    class Service implements JsonValue {
        public static final Type MAP = new TypeRef<Map<String, Service>>() {
        }.type();


        String ID;


        String Service;


        List<String> Tags;


        String Address;


        Map<String, String> Meta;


        Integer Port;


        Boolean EnableTagOverride;


        Long CreateIndex;


        Long ModifyIndex;
    }


    @Value
    @Builder
    @Accessors(fluent = true)
    class ServiceParameter implements Parameter {
        String datacenter;

        String near;

        Map<String, String> nodeMeta;

        String[] tags;

        Boolean passing;

        @Override
        public void accept(Requester<?> q) {
            q.query(near() != null, "near", Parameter.encode(near()))
                    .query(datacenter() != null, "dc", Parameter.encode(datacenter()))
                    .query(passing() != null, "passing", Boolean.toString(Boolean.TRUE.equals(passing())))
            ;
            if (nodeMeta() != null) {
                nodeMeta().forEach((k, v) -> q.query("node-meta", k + ":" + v));
            }
            var tags = tags();
            if (tags != null) {
                for (var tag : tags) {
                    if (tag != null) q.query("tag", tag);
                }
            }
        }

    }


    interface Acl extends Values {
        enum AclType implements JsonValue {
            client,
            management;

        }

        @Value
        @Builder
        @Accessors(fluent = true)
        public static class ACL implements JsonValue {
            public static final Type LIST = new TypeRef<List<ACL>>() {
            }.type();
            public static final List<ACL> EMPTY_LIST = Collections.emptyList();

            long CreateIndex;

            long ModifyIndex;

            String ID;

            String Name;

            AclType Type;

            String Rules;

        }

        @Value
        @Builder
        @Accessors(fluent = true)
        public static class CreateAcl implements JsonValue {
            String Name;

            AclType Type;

            String Rules;
        }

        @Value
        @Builder
        @Accessors(fluent = true)
        public static class UpdateAcl implements JsonValue {
            String ID;

            String Name;

            AclType Type;

            String Rules;
        }
    }

    interface Agent extends Values {
        @Value
        @Builder
        @Accessors(fluent = true)
        public static class Member implements JsonValue {
            public static final Type LIST = new TypeRef<List<Member>>() {
            }.type();

            String Name;

            String Addr;

            Integer Port;

            Map<String, String> Tags;

            int Status;

            int ProtocolMin;

            int ProtocolMax;

            int ProtocolCur;

            int DelegateMin;

            int DelegateMax;

            int DelegateCur;
        }

        @Value
        @Builder
        @Accessors(fluent = true)
        public static class CreateCheck implements JsonValue {
            String ID;

            String Name;

            String ServiceID;

            String Notes;


            List<String> Args;

            String HTTP;

            String Method;

            Map<String, List<String>> Header;

            String TCP;

            String DockerContainerID;

            String Shell;

            String Interval;

            String Timeout;

            String TTL;

            String DeregisterCriticalServiceAfter;

            Boolean TLSSkipVerify;

            String Status;

            String GRPC;

            Boolean GRPCUseTLS;
        }

        @Value
        @Builder
        @Accessors(fluent = true)
        public static class CreateService implements JsonValue {
            @Value
            @Builder
            @Accessors(fluent = true)
            public static class Check implements JsonValue {
                String Script;

                String DockerContainerID;

                String Shell;

                String Interval;

                String TTL;

                String HTTP;

                String Method;

                Map<String, List<String>> Header;

                String TCP;

                String Timeout;

                String DeregisterCriticalServiceAfter;

                Boolean TLSSkipVerify;

                String Status;

                String GRPC;

                Boolean GRPCUseTLS;
            }

            String ID;

            String Name;

            List<String> Tags;

            String Address;

            Map<String, String> Meta;

            Integer Port;

            Boolean EnableTagOverride;

            CreateService.Check Check;

            List<CreateService.Check> Checks;
        }

        @Value
        @Builder
        @Accessors(fluent = true)
        public static class Self implements JsonValue {
            enum LogLevel implements JsonValue {
                trace,
                debug,

                info,
                warn,
                err
            }

            @Value
            @Builder
            @Accessors(fluent = true)
            public static class Config implements JsonValue {
                String Datacenter;
                String PrimaryDatacenter;

                String NodeName;
                String NodeID;

                String BuildDate;

                String Revision;

                boolean Server;

                String Version;
            }

            @Value
            @Builder
            @Accessors(fluent = true)
            public static class DebugConfig implements JsonValue {
                boolean Bootstrap;

                String DataDir;

                String DNSRecursor;

                String DNSDomain;

                LogLevel LogLevel;

                String NodeID;

                String[] ClientAddrs;

                String BindAddr;

                boolean LeaveOnTerm;

                boolean SkipLeaveOnInt;

                boolean EnableDebug;

                boolean VerifyIncoming;

                boolean VerifyOutgoing;

                String CAFile;

                String CertFile;

                String KeyFile;

                String UiDir;

                String PidFile;

                boolean EnableSyslog;

                boolean RejoinAfterLeave;

                boolean ACLEnableKeyListPolicy;

//                Map<String, Object> ACLTokens;

                boolean ACLsEnabled;
                String AEInterval;
                String AdvertiseAddrLAN;
                String AdvertiseAddrWAN;

                String AdvertiseReconnectTimeout;
                String ACLInitialManagementToken;

//                Map<String, Object> ACLResolverSettings;

                boolean ACLTokenReplication;
//                List<Object> AllowWriteHTTPFrom;
//                Map<String, Object> AutoConfig;

                Boolean AutoEncryptAllowTLS;
                List<String> AutoEncryptDNSSAN;
                List<String> AutoEncryptIPSAN;
                Boolean AutoEncryptTLS;
                Boolean AutoReloadConfig;
                String AutoReloadConfigCoalesceInterval;
                Boolean AutopilotCleanupDeadServers;
                Boolean AutopilotDisableUpgradeMigration;
                String AutopilotLastContactThreshold;
                Long AutopilotMaxTrailingLogs;
                Long AutopilotMinQuorum;
                String AutopilotRedundancyZoneTag;
                String AutopilotServerStabilizationTime;
                String AutopilotUpgradeVersionTag;
                Long BootstrapExpect;
                String BuildDate;
//                Map<String, Object> Cache;

//                Map<String,Object> Cloud;
            }

            Self.Config Config;

            Self.DebugConfig DebugConfig;

            Member Member;
            Coordinate.Coord Coord;

            //            Map<String, Object> Stats;
            XDS xDS;
            Map<String, String> Meta;

            @Value
            @Builder
            @Accessors(fluent = true)
            public static class XDS {
                Map<String, List<String>> SupportedProxies;
                int Port;
                Map<String, Integer> Ports;
            }
        }
    }

    interface Catalog extends Values {
        @Value
        @Builder
        @Accessors(fluent = true)
        public static class Service implements JsonValue {
            public static final Type LIST = new TypeRef<List<Service>>() {
            }.type();

            String ID;

            String Node;

            String Address;

            String Datacenter;

            Map<String, String> TaggedAddresses;

            Map<String, String> NodeMeta;

            String ServiceID;

            String ServiceName;

            List<String> ServiceTags;

            String ServiceAddress;

            Map<String, String> ServiceMeta;

            Integer ServicePort;

            Boolean ServiceEnableTagOverride;

            Long CreateIndex;

            Long ModifyIndex;
        }

        @Value
        @Builder
        @Accessors(fluent = true)
        public static class Node implements JsonValue {

            @Value
            @Builder
            @Accessors(fluent = true)
            public static class Service implements JsonValue {
                String ID;

                String Service;

                List<String> Tags;

                Integer Port;
            }

            Node Node;

            Map<String, Service> Services;
        }

        @Value
        @Builder
        @Accessors(fluent = true)
        public static class Deregistration implements JsonValue {
            String Datacenter;

            String Node;

            String CheckID;

            String ServiceID;

            WriteRequest WriteRequest;
        }

        @Value
        @Builder
        @Accessors(fluent = true)
        public static class WriteRequest implements JsonValue {
            String Token;
        }

        @Value
        @Builder
        @Accessors(fluent = true)
        public static class Registration implements JsonValue {
            @Value
            @Builder
            @Accessors(fluent = true)
            public static class Service implements JsonValue {
                String ID;

                String Service;

                List<String> Tags;

                String Address;

                Map<String, String> Meta;

                Integer Port;

            }

            @Value
            @Builder
            @Accessors(fluent = true)
            public static class Check implements JsonValue {
                String Node;

                String CheckID;

                String Name;

                String Notes;

                Values.Check.Status Status;

                String ServiceID;
            }

            String Datacenter;

            String Node;

            String Address;

            Registration.Service Service;

            Registration.Check Check;

            WriteRequest WriteRequest;

            Map<String, String> NodeMeta;

            boolean SkipNodeUpdate;

            Map<String, String> TaggedAddresses;
        }
    }


    interface Coordinate extends Values {
        @Value
        @Builder
        @Accessors(fluent = true)
        public static class Coord implements JsonValue {
            Double Error;

            Double Height;

            Double Adjustment;

            List<Double> Vec;
        }

        @Value
        @Builder
        @Accessors(fluent = true)
        public static class Datacenter implements JsonValue {
            public static final Type LIST = new TypeRef<List<Datacenter>>() {
            }.type();

            String Datacenter;

            String AreaID;

            List<Node> Coordinates;
        }

        @Value
        @Builder
        @Accessors(fluent = true)
        public static class Node implements JsonValue {
            public static final Type LIST = new TypeRef<List<Node>>() {
            }.type();

            String Node;

            Coord Coord;
        }
    }

    interface Events extends Values {
        @Value
        @Builder
        @Accessors(fluent = true)
        public static class Event implements JsonValue {
            public static final Type LIST = new TypeRef<List<Event>>() {
            }.type();

            String ID;


            String Name;


            String Payload;


            String NodeFilter;


            String ServiceFilter;


            String TagFilter;


            int Version;


            int LTime;

        }

        @Value
        @Builder
        @Accessors(fluent = true)
        public static class EventServiceParameter implements Parameter {
            String name;

            String service;

            String tag;

            String node;

            @Override
            public void accept(Requester<?> q) {
                q
                        .query(name() != null, "name", name())
                        .query(service() != null, "service", service())
                        .query(tag() != null, "tag", tag())
                        .query(node() != null, "node", node())
                ;
            }
        }
    }

    interface Health extends Values {
        @Value
        @Builder
        @Accessors(fluent = true)
        public static class Service implements JsonValue {
            public static final Type LIST = new TypeRef<List<Service>>() {
            }.type();

            Node Node;


            Service Service;


            List<Check> Checks;

        }
    }

    interface Query extends Values {
        @Value
        @Builder
        @Accessors(fluent = true)
        public static class QueryCheck implements JsonValue {
            String Node;


            String CheckID;


            String Name;


            Check.Status Status;


            String Notes;


            String Output;


            String ServiceID;


            String ServiceName;


            List<String> ServiceTags;


            Long CreateIndex;


            Long ModifyIndex;


        }

        @Value
        @Builder
        @Accessors(fluent = true)
        public static class QueryDNS implements JsonValue {
            String TTL;

        }

        @Value
        @Builder
        @Accessors(fluent = true)
        public static class QueryNode implements JsonValue {

            Node Node;


            Service Service;


            List<Check> Checks;

        }

        @Value
        @Builder
        @Accessors(fluent = true)
        public static class QueryExecution implements JsonValue {
            String Service;


            List<QueryNode> Nodes;


            QueryDNS DNS;


            String Datacenter;


            Integer Failovers;

        }
    }

    interface Sessions extends Values {
        enum SessionBehavior implements JsonValue {

            release,


            delete
        }

        @Value
        @Builder
        @Accessors(fluent = true)
        public static class Session implements JsonValue {
            public static final Type LIST = new TypeRef<List<Session>>() {
            }.type();
            public static final List<Session> EMPTY_LIST = Collections.emptyList();

            long LockDelay;


            List<String> Checks;


            String Node;


            String ID;


            String Name;


            long CreateIndex;


            long ModifyIndex;


            String TTL;


            SessionBehavior Behavior;

        }

        @Value
        @Builder
        @Accessors(fluent = true)
        public static class CreateSession implements JsonValue {
            long LockDelay;


            String Name;


            String Node;


            List<String> Checks;


            SessionBehavior Behavior;


            String TTL;

        }
    }

    interface Status extends Values {
    }

    interface Store extends Values {
        interface KvPair<T> {
            long CreateIndex();

            long ModifyIndex();

            Long LockIndex();

            long Flags();

            String Session();

            String Key();

            T Value();
        }

        @Value
        @Builder
        @Accessors(fluent = true)
        public static class Text implements KvPair<String>, JsonValue {

            public static final Type LIST = new TypeRef<List<Text>>() {
            }.type();
            public static final List<Text> EMPTY_LIST = Collections.emptyList();

            long CreateIndex;

            long ModifyIndex;

            Long LockIndex;

            long Flags;

            String Session;

            String Key;

            String Value;

        }

        @Value
        @Builder
        @Accessors(fluent = true)
        public static class Binary implements KvPair<byte[]>, JsonValue {

            public static final Type LIST = new TypeRef<List<Binary>>() {
            }.type();
            public static final List<Binary> EMPTY_LIST = Collections.emptyList();

            long CreateIndex;

            long ModifyIndex;

            Long LockIndex;

            long Flags;

            String Session;

            String Key;

            byte[] Value;

        }

        @Value
        @Builder
        @Accessors(fluent = true)
        public static class PutParameter implements Parameter {
            long flags;

            Long cas;

            String acquireSession;

            String releaseSession;

            @Override
            public void accept(Requester<?> q) {
                if (cas() != null) q.query("cas", Long.toUnsignedString(cas()));
                if (flags() != 0) q.query("flags", Long.toUnsignedString(flags()));
                if (acquireSession() != null) q.query("acquire", acquireSession());
                if (releaseSession() != null) q.query("release", releaseSession());
            }
        }
    }
}
