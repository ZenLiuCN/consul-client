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

import cn.zenliu.java.consul.Client;
import cn.zenliu.java.consul.Endpoints;
import cn.zenliu.java.consul.Values;
import lombok.SneakyThrows;
import org.junit.jupiter.api.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HttpConsulClientTest {
    static {
        System.setProperty("io.netty.leakDetection.level", "PARANOID");
    }

    final Client client = Client.create(null, "http://127.0.0.1:8500", true);

    @BeforeAll
    @SneakyThrows
    static void envCheck() {
        try (var client = Client.create(null, "http://127.0.0.1:8500", true)) {
            var agent = client.agent(null, null);
            var res = agent.members().get();
            var values = res.value();
            assertNotNull(values);
            assertEquals(1, values.size(), "not an test agent");
            var value = values.get(0);
            assertEquals("127.0.0.1", value.Addr(), "not local agent");
            assertEquals(8301, value.Port(), "not default port");
            assertTrue(agent.checks().get().value().isEmpty(), "not empty checks");
            assertTrue(agent.services().get().value().isEmpty(), "not empty services");
        }

    }

    @AfterAll
    @SneakyThrows
    static void envCleanup() {
        try (var client = Client.create(null, "http://127.0.0.1:8500", true)) {
            var agent = client.agent(null, null);
            var checks = agent.checks().get().value();
            if (!checks.isEmpty()) {
                for (var k : checks.keySet()) {
                    agent.checkDeregister(k).get();
                }
            }
            var services = agent.services().get().value();
            if (!services.isEmpty()) {
                for (var k : services.keySet()) {
                    agent.serviceDeregister(k).get();
                }
            }
        }

    }

    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @Nested
    class AgentTests {
        final Endpoints.Agent<?> agent = client.agent(null, null);

        @Test
        void registers() {
            var serviceId = "some-service";
            var checkId = "some-service-check";
            assertDoesNotThrow(() -> agent.serviceRegister(Values.Agent.CreateService.builder()
                    .ID(serviceId)
                    .Name("test-service")
                    .Address("127.0.0.1")
                    .Port(8500)
                    .build()).get());
            assertDoesNotThrow(() -> assertEquals(1, agent.services().get().value().size()));
            assertDoesNotThrow(() -> agent.checkRegister(Values.Agent.CreateCheck.builder()
                    .ID(checkId)
                    .Name("test-service-checker")
                    .ServiceID(serviceId)
                    .Notes("notes")
                    .Args(List.of())
                    .HTTP("http://127.0.0.1:8500")
                    .Method("GET")
                    .Interval("60s")
                    .Timeout("5s")
                    .DeregisterCriticalServiceAfter("5m")
                    .TLSSkipVerify(true)
                    .build()).get());
            assertDoesNotThrow(() -> assertEquals(1, agent.checks().get().value().size()));
            assertDoesNotThrow(() -> agent.checkDeregister(checkId).get());
            assertDoesNotThrow(() -> assertEquals(0, agent.checks().get().value().size()));
            assertDoesNotThrow(() -> agent.serviceDeregister(serviceId).get());
            assertDoesNotThrow(() -> assertEquals(0, agent.services().get().value().size()));
        }
    }

    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @Nested
    class StoreTests {
        final Endpoints.Store<?> store = client.store(null, null);
        final String key = "some/service";
        final String value = "some-service-check";

        @Test
        @Order(1)
        void readPut() {
            var binary = value.getBytes(StandardCharsets.UTF_8);
            assertDoesNotThrow(() -> assertTrue(store.putText(value, null, key).get().value()));
            assertDoesNotThrow(() -> assertEquals(value, store.text(key).get().value().Value()));
            assertDoesNotThrow(() -> assertArrayEquals(binary, store.binary(key).get().value().Value()));
            assertDoesNotThrow(() -> store.delete(null, key).get());
            assertDoesNotThrow(() -> assertTrue(store.putBinary(binary, null, key).get().value()));
            assertDoesNotThrow(() -> assertEquals(value, store.text(key).get().value().Value()));
            assertDoesNotThrow(() -> assertArrayEquals(binary, store.binary(key).get().value().Value()));
        }

        @Test
        @Order(2)
        void remove() {
            assertDoesNotThrow(() -> store.delete(null, key).get());
        }
    }
}