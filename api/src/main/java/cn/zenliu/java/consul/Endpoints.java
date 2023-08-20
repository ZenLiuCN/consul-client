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

import cn.zenliu.java.consul.trasport.Response;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Zen.Liu
 * @since 2023-08-20
 */
public interface Endpoints {

    interface Acl<T extends Acl<T>> extends Context<T>, Values.Acl {
        default Response<Info<String>> create(CreateAcl createAcl) {
            return requester()
                    .header(TOKEN, token())
                    .path(VERSION, "acl", "create")
                    .query(parameter())

                    .put(IDOnly.class, (IDOnly) null)
                    .send(createAcl)
                    .response()
                    .map(Info::id)
                    ;
        }

        default Response<Info<Void>> update(UpdateAcl updateAcl) {
            return requester()
                    .header(TOKEN, token())
                    .path(VERSION, "acl", "update")
                    .query(parameter())

                    .put(Void.class, (Void) null)
                    .send(updateAcl)
                    .response()
                    .map(Info::parse)
                    ;
        }

        default Response<Info<Void>> destroy(String id) {
            return requester()

                    .header(TOKEN, token())
                    .path(VERSION, "acl", "destroy", id)
                    .query(parameter())

                    .put(Void.class, (Void) null)
                    .send(null)
                    .response()
                    .map(Info::parse);

        }

        default Response<Info<ACL>> info(String id) {

            return requester()

                    .header(TOKEN, token())
                    .path(VERSION, "acl", "info", id)
                    .query(parameter())

                    .put(ACL.LIST, ACL.EMPTY_LIST)
                    .send(null)
                    .response()
                    .map(Info::one);

        }

        default Response<Info<String>> clone(String id) {
            return requester()

                    .header(TOKEN, token())
                    .path(VERSION, "acl", "clone", id)
                    .query(parameter())

                    .put(IDOnly.class, (IDOnly) null)
                    .send(null)
                    .response()
                    .map(Info::id);

        }

        default Response<Info<List<ACL>>> list() {
            return requester()

                    .header(TOKEN, token())
                    .path(VERSION, "acl", "list")
                    .query(parameter())

                    .put(ACL.LIST, (List<ACL>) null)
                    .send(null)
                    .response()
                    .map(Info::parse);

        }


    }

    interface Agent<T extends Agent<T>> extends Values.Agent, Context<T> {

        default Response<Info<Map<String, Check>>> checks() {
            return requester()
                    .header(TOKEN, token())
                    .path(VERSION, "agent", "checks")
                    .query(parameter())

                    .get(Check.MAP, (Map<String, Check>) null)
                    .send(null)
                    .response()
                    .map(Info::parse);

        }

        default Response<Info<Map<String, Service>>> services() {
            return requester()
                    .header(TOKEN, token())
                    .path(VERSION, "agent", "services")
                    .query(parameter())
                    .get(Service.MAP, (Map<String, Service>) null)
                    .send(null)
                    .response()
                    .map(Info::parse);

        }


        default Response<Info<List<Member>>> members() {
            return requester()
                    .header(TOKEN, token())
                    .path(VERSION, "agent", "members")
                    .query(parameter())
                    .get(Member.LIST, (List<Member>) null)
                    .send(null)
                    .response()
                    .map(Info::parse);
        }


        default Response<Info<Self>> self() {
            return requester()
                    .header(TOKEN, token())
                    .path(VERSION, "agent", "self")
                    .query(parameter())

                    .get(Self.class, (Self) null)
                    .send(null)
                    .response()
                    .map(Info::parse);

        }


        default Response<Info<Void>> maintenance(boolean maintenanceEnabled, @Nullable String reason) {
            return requester()
                    .header(TOKEN, token())
                    .path(VERSION, "agent", "maintenance")
                    .query(parameter())

                    .query("enable", Boolean.toString(maintenanceEnabled))
                    .query(reason != null, "reason", Parameter.encode(reason))

                    .put(Void.class, (Void) null)
                    .send(null)
                    .response()
                    .map(Info::parse);
        }

        default Response<Info<Void>> join(String address, boolean wan) {
            return requester()
                    .header(TOKEN, token())
                    .path(VERSION, "agent", "join", Parameter.encode(address))
                    .query(parameter())

                    .query(wan, "wan", "1")

                    .put(Void.class, (Void) null)
                    .send(null)
                    .response()
                    .map(Info::parse);
        }

        default Response<Info<Void>> forceLeave(String node) {
            return requester()
                    .header(TOKEN, token())
                    .path(VERSION, "agent", "force-leave", Parameter.encode(node))
                    .query(parameter())

                    .put(Void.class, (Void) null)
                    .send(null)
                    .response()
                    .map(Info::parse);

        }


        default Response<Info<Void>> checkRegister(CreateCheck createCheck) {
            return requester()
                    .header(TOKEN, token())
                    .path(VERSION, "agent", "check", "register")
                    .query(parameter())

                    .put(Void.class, (Void) null)
                    .send(createCheck)
                    .response()
                    .map(Info::parse);

        }


        default Response<Info<Void>> checkDeregister(String checkId) {
            return requester()
                    .header(TOKEN, token())
                    .path(VERSION, "agent", "check", "deregister", checkId)
                    .query(parameter())

                    .put(Void.class, (Void) null)
                    .send(null)
                    .response()
                    .map(Info::parse);

        }


        default Response<Info<Void>> checkPass(String checkId, @Nullable String note) {
            return requester()
                    .header(TOKEN, token())
                    .path(VERSION, "agent", "check", "pass", checkId)
                    .query(parameter())

                    .query(note != null, "note", Parameter.encode(note))

                    .put(Void.class, (Void) null)
                    .send(null)
                    .response()
                    .map(Info::parse);

        }


        default Response<Info<Void>> checkWarn(String checkId, @Nullable String note) {
            return requester()
                    .header(TOKEN, token())
                    .path(VERSION, "agent", "check", "warn", checkId)
                    .query(parameter())

                    .query(note != null, "note", Parameter.encode(note))

                    .put(Void.class, (Void) null)
                    .send(null)
                    .response()
                    .map(Info::parse);


        }


        default Response<Info<Void>> checkFail(String checkId, @Nullable String note) {
            return requester()
                    .header(TOKEN, token())

                    .path(VERSION, "agent", "check", "fail", checkId)
                    .query(parameter())

                    .query(note != null, "note", Parameter.encode(note))

                    .put(Void.class, (Void) null)
                    .send(null)
                    .response()
                    .map(Info::parse);
        }


        default Response<Info<Void>> serviceRegister(CreateService createService) {
            return requester()
                    .header(TOKEN, token())

                    .path(VERSION, "agent", "service", "register")
                    .query(parameter())

                    .put(Void.class, (Void) null)

                    .send(createService)
                    .response()
                    .map(Info::parse);

        }


        default Response<Info<Void>> serviceDeregister(String serviceId) {
            return requester()
                    .header(TOKEN, token())
                    .path(VERSION, "agent", "service", "deregister", serviceId)
                    .query(parameter())

                    .put(Void.class, (Void) null)
                    .send(null)
                    .response()
                    .map(Info::parse);

        }


        default Response<Info<Void>> serviceMaintenance(String serviceId, boolean maintenanceEnabled, @Nullable String reason) {
            return requester()
                    .header(TOKEN, token())
                    .path(VERSION, "agent", "service", "maintenance", serviceId)
                    .query(parameter())

                    .query("enable", Boolean.toString(maintenanceEnabled))
                    .query(reason != null, "reason", Parameter.encode(reason))


                    .put(Void.class, (Void) null)
                    .send(null)
                    .response()
                    .map(Info::parse);

        }

        default Response<Info<Void>> reload() {
            return requester()
                    .header(TOKEN, token())
                    .path(VERSION, "agent", "reload")
                    .query(parameter())

                    .put(Void.class, (Void) null)
                    .send(null)
                    .response()
                    .map(Info::parse);

        }
        //endregion


    }

    interface Catalog<T extends Catalog<T>> extends Values.Catalog, Context<T> {


        //region Function
        default Response<Info<Void>> register(Registration registration) {
            return requester()

                    .header(TOKEN, token())
                    .path(VERSION, "catalog", "register")
                    .query(parameter())

                    .put(Void.class, (Void) null)
                    .send(registration)
                    .response()
                    .map(Info::parse);

        }

        default Response<Info<Void>> deregister(Deregistration deregistration) {
            return requester()
                    .header(TOKEN, token())
                    .path(VERSION, "catalog", "deregister")
                    .query(parameter())

                    .put(Void.class, (Void) null)
                    .send(deregistration)
                    .response()
                    .map(Info::parse);


        }


        default Response<Info<List<String>>> datacenters() {
            return requester()
                    .header(TOKEN, token())
                    .path(VERSION, "catalog", "datacenters")
                    .query(parameter())

                    .get(STRING_LIST, (List<String>) null)
                    .send(null)
                    .response()
                    .map(Info::parse);

        }

        default Response<Info<Node>> node(String name) {
            return requester()
                    .header(TOKEN, token())
                    .path(VERSION, "catalog", "node", Parameter.encode(name))
                    .query(parameter())


                    .get(Node.class, (Node) null)
                    .send(null)
                    .response()
                    .map(Info::parse);

        }

        default Response<Info<List<Values.Node>>> nodes(@Nullable Values.NodeParameter query) {
            return requester()
                    .header(TOKEN, token())
                    .path(VERSION, "catalog", "nodes")
                    .query(parameter())

                    .query(query)

                    .get(Values.Node.LIST, (List<Values.Node>) null)
                    .send(null)
                    .response()
                    .map(Info::parse);


        }

        default Response<Info<List<Service>>> service(String serviceName, @Nullable Values.ServiceParameter query) {
            return requester()
                    .header(TOKEN, token())
                    .path(VERSION, "catalog", "service", Parameter.encode(serviceName))
                    .query(parameter())

                    .query(query)

                    .get(Service.LIST, (List<Service>) null)
                    .send(null)
                    .response()
                    .map(Info::parse);


        }

        default Response<Info<Map<String, List<String>>>> services(@Nullable Values.ServiceParameter query) {

            return requester()
                    .header(TOKEN, token())
                    .path(VERSION, "catalog", "services")
                    .query(parameter())

                    .query(query)

                    .get(MAP_STRING_LIST, (Map<String, List<String>>) null)
                    .send(null)
                    .response()
                    .map(Info::parse);


        }
        //endregion


    }

    interface Coordinate<T extends Coordinate<T>> extends Values.Coordinate, Context<T> {


        default Response<Info<List<Datacenter>>> datacenters() {
            return requester()

                    .header(TOKEN, token())
                    .path(VERSION, "coordinate", "datacenters")
                    .query(parameter())


                    .get(Datacenter.LIST, (List<Datacenter>) null)
                    .send(null)
                    .response()
                    .map(Info::parse);

        }


        default Response<Info<List<Node>>> nodes() {
            return requester()

                    .header(TOKEN, token())
                    .path(VERSION, "coordinate", "nodes")
                    .query(parameter())


                    .get(Node.LIST, (List<Node>) null)
                    .send(null)
                    .response()
                    .map(Info::parse);


        }


    }

    interface Events<T extends Events<T>> extends Values.Events, Context<T> {


        default Response<Info<List<Event>>> list(@Nullable EventServiceParameter query) {
            return requester()

                    .header(TOKEN, token())
                    .path(VERSION, "event", "list")
                    .query(parameter())

                    .query(query)

                    .get(Event.LIST, (List<Event>) null)
                    .send(null)
                    .response()
                    .map(Info::parse);


        }

        default Response<Info<Event>> fire(String event, String payload, @Nullable EventServiceParameter query) {
            return requester()

                    .header(TOKEN, token())
                    .path(VERSION, "event", "fire", Parameter.encode(event))
                    .query(parameter())

                    .query(query)

                    .put(Event.class, (Event) null)
                    .send(payload)
                    .response()
                    .map(Info::parse);

        }


    }

    interface Health<T extends Health<T>> extends Values.Health, Context<T> {


        default Response<Info<List<Check>>> checksForNode(String nodeName) {
            return requester()

                    .header(TOKEN, token())
                    .path(VERSION, "health", "node", Parameter.encode(nodeName))
                    .query(parameter())


                    .get(Check.LIST, (List<Check>) null)
                    .send(null)
                    .response()
                    .map(Info::parse);

        }

        default Response<Info<List<Check>>> checksForService(String serviceName, @Nullable Values.NodeParameter query) {
            return requester()

                    .header(TOKEN, token())
                    .path(VERSION, "health", "checks", Parameter.encode(serviceName))
                    .query(parameter())

                    .query(query)

                    .get(Check.LIST, (List<Check>) null)
                    .send(null)
                    .response()
                    .map(Info::parse);


        }

        default Response<Info<List<Service>>> services(String serviceName, @Nullable Values.ServiceParameter query) {
            return requester()

                    .header(TOKEN, token())
                    .path(VERSION, "health", "service", Parameter.encode(serviceName))
                    .query(parameter())

                    .query(query)

                    .get(Service.LIST, (List<Service>) null)
                    .send(null)
                    .response()
                    .map(Info::parse);


        }


        default Response<Info<List<Check>>> checksState(@Nullable Check.Status status) {
            return requester()

                    .header(TOKEN, token())
                    .path(VERSION, "health", "state", status == null ? "any" : status.name().toLowerCase())
                    .query(parameter())


                    .get(Check.LIST, (List<Check>) null)
                    .send(null)
                    .response()
                    .map(Info::parse);


        }


    }

    interface Query<T extends Query<T>> extends Values.Query, Context<T> {


        default Response<Info<QueryExecution>> execute(String uuid) {
            return requester()

                    .header(TOKEN, token())
                    .path(VERSION, "query", uuid, "execute")
                    .query(parameter())


                    .get(QueryExecution.class, (QueryExecution) null)
                    .send(null)
                    .response()
                    .map(Info::parse);


        }


    }

    interface Sessions<T extends Sessions<T>> extends Values.Sessions, Context<T> {


        default Response<Info<String>> create(CreateSession create) {

            return requester()

                    .header(TOKEN, token())
                    .path(VERSION, "session", "create")
                    .query(parameter())


                    .put(IDOnly.class, (IDOnly) null)
                    .send(create)
                    .response()
                    .map(Info::id);

        }

        default Response<Info<Void>> destroy(String session) {

            return requester()

                    .header(TOKEN, token())
                    .path(VERSION, "session", "destroy", Parameter.encode(session))
                    .query(parameter())


                    .put(Void.class, (Void) null)
                    .send(null)
                    .response()
                    .map(Info::parse);


        }

        default Response<Info<Session>> info(String session) {
            return requester()

                    .header(TOKEN, token())
                    .path(VERSION, "session", "info", Parameter.encode(session))
                    .query(parameter())


                    .get(Session.LIST, Session.EMPTY_LIST)// (List<Session>) null)
                    .send(null)
                    .response()
                    .map(Info::one);

        }

        default Response<Info<List<Session>>> node(String node) {

            return requester()

                    .header(TOKEN, token())
                    .path(VERSION, "session", "node", Parameter.encode(node))
                    .query(parameter())


                    .get(Session.LIST, (List<Session>) null)
                    .send(null)
                    .response()
                    .map(Info::parse);


        }

        default Response<Info<List<Session>>> list() {

            return requester()

                    .header(TOKEN, token())
                    .path(VERSION, "session", "list")
                    .query(parameter())


                    .get(Session.LIST, (List<Session>) null)
                    .send(null)
                    .response()
                    .map(Info::parse);


        }

        default Response<Info<Session>> renew(String id) {
            return requester()

                    .header(TOKEN, token())
                    .path(VERSION, "session", "renew", id)
                    .query(parameter())


                    .put(Session.LIST, (List<Session>) null)
                    .send(null)
                    .response()
                    .map(Info::one);


        }


    }

    interface Status<T extends Status<T>> extends Values.Status, Context<T> {

        default Response<Info<String>> leader() {
            return requester()

                    .header(TOKEN, token())
                    .path(VERSION, "status", "leader")
                    .query(parameter())


                    .get(String.class, (String) null)
                    .send(null)
                    .response()
                    .map(Info::parse);
        }

        default Response<Info<List<String>>> peers() {
            return requester()

                    .header(TOKEN, token())
                    .path(VERSION, "status", "peers")
                    .query(parameter())


                    .get(STRING_LIST, (List<String>) null)
                    .send(null)
                    .response()
                    .map(Info::parse);


        }


    }

    interface Store<T extends Store<T>> extends Values.Store, Context<T> {


        //region function
        static String[] buildKeys(CharSequence key, CharSequence... segments) {
            var s = new String[segments.length + 3];
            s[0] = VERSION;
            s[1] = "kv";
            s[2] = Parameter.encode(key.toString());
            for (int i = 0; i < segments.length; i++) {
                s[i + 3] = Parameter.encode(segments[i].toString());
            }
            return s;
        }

        default Response<Info<Text>> text(CharSequence key, CharSequence... segments) {
            return requester()

                    .header(TOKEN, token())
                    .path(buildKeys(key, segments))
                    .query(parameter())


                    .get(Text.LIST, Text.EMPTY_LIST)
                    .send(null)
                    .response()
                    .map(Info::one);

        }

        default Response<Info<List<Text>>> textAll(CharSequence key, @Nullable CharSequence... segments) {
            return requester()

                    .header(TOKEN, token())
                    .path(buildKeys(key, segments))
                    .query(parameter())

                    .query("recurse")

                    .get(Text.LIST, Text.EMPTY_LIST)
                    .send(null)
                    .response()
                    .map(Info::parse);


        }

        default Response<Info<Binary>> binary(CharSequence key, CharSequence... segments) {
            return requester()

                    .header(TOKEN, token())
                    .path(buildKeys(key, segments))
                    .query(parameter())

                    .get(Binary.LIST, Binary.EMPTY_LIST)
                    .send(null)
                    .response()
                    .map(Info::one);

        }

        default Response<Info<List<Binary>>> binaryAll(CharSequence key, CharSequence... segments) {
            return requester()

                    .header(TOKEN, token())
                    .path(buildKeys(key, segments))
                    .query(parameter())

                    .query("recurse")

                    .get(Binary.LIST, Binary.EMPTY_LIST)
                    .send(null)
                    .response()
                    .map(Info::parse);

        }


        default Response<Info<List<String>>> keys(@Nullable String separator, CharSequence keys) {
            return requester()

                    .header(TOKEN, token())
                    .path(VERSION, "kv", keys)
                    .query(parameter())

                    .query("keys")
                    .query(separator != null, "separator", separator)

                    .get(STRING_LIST, Collections.<String>emptyList())
                    .send(null)
                    .response()
                    .map(Info::parse);


        }


        default Response<Info<Boolean>> set(@Nullable String value, @Nullable PutParameter parameter, CharSequence key, CharSequence... segments) {
            return requester()

                    .header(TOKEN, token())
                    .path(buildKeys(key, segments))
                    .query(parameter())

                    .query(parameter)


                    .put(Boolean.class, (Boolean) null)
                    .send(value)
                    .response()
                    .map(Info::parse);


        }

        default Response<Info<Boolean>> set(byte @Nullable [] value, @Nullable PutParameter parameter, CharSequence key, CharSequence... segments) {
            return requester()

                    .header(TOKEN, token())
                    .path(buildKeys(key, segments))
                    .query(parameter())

                    .query(parameter)


                    .put(Boolean.class, (Boolean) null)
                    .send(value)
                    .response()
                    .map(Info::parse);

        }

        default Response<Info<Void>> delete(@Nullable PutParameter parameter, CharSequence key, CharSequence... segments) {
            return requester()

                    .header(TOKEN, token())
                    .path(buildKeys(key, segments))
                    .query(parameter())

                    .query(parameter)


                    .delete(Void.class, (Void) null)
                    .send(null)
                    .response()
                    .map(Info::parse);

        }

        default Response<Info<Void>> deleteAll(@Nullable PutParameter parameter, CharSequence key, CharSequence... segments) {
            return requester()

                    .header(TOKEN, token())
                    .path(buildKeys(key, segments))
                    .query(parameter())

                    .query(parameter)
                    .query("recurse")

                    .delete(Void.class, (Void) null)
                    .send(null)
                    .response()
                    .map(Info::parse);

        }
        //endregion


    }
}
