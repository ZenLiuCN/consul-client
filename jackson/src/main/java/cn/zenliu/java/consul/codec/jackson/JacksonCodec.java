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

package cn.zenliu.java.consul.codec.jackson;

import cn.zenliu.java.consul.JsonValue;
import cn.zenliu.java.consul.trasport.Codec;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.*;
import com.google.auto.service.AutoService;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.ByteBufUtil;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.lang.reflect.Type;

/**
 * Jackson-Databind based JsonCodec
 *
 * @author Zen.Liu
 * @since 2023-08-20
 */
@SuppressWarnings("unused")
public class JacksonCodec extends Codec.BaseCodec {
    protected final ObjectMapper mapper;
    protected final Logger logger;

    protected static class JsonValueAccessorNamingStrategy extends AccessorNamingStrategy {
        @Override
        public String findNameForIsGetter(AnnotatedMethod method, String name) {
            if (!Character.isUpperCase(name.charAt(0))) return null;
            return name;
        }

        @Override
        public String findNameForRegularGetter(AnnotatedMethod method, String name) {
            if (!Character.isUpperCase(name.charAt(0))) return null;
            return name;
        }

        @Override
        public String findNameForMutator(AnnotatedMethod method, String name) {
            if (!Character.isUpperCase(name.charAt(0))) return null;
            return name;
        }

        @Override
        public String modifyFieldName(AnnotatedField field, String name) {
            if (!Character.isUpperCase(name.charAt(0))) return null;
            return name;
        }
    }

    protected static class JsonValuePropertyNamingStrategy extends PropertyNamingStrategy {
        protected final PropertyNamingStrategy old;

        public JsonValuePropertyNamingStrategy(PropertyNamingStrategy old) {
            this.old = old;
        }

        @Override
        public String nameForField(MapperConfig<?> config, AnnotatedField field, String defaultName) {
            return old != null ? old.nameForField(config, field, defaultName) : super.nameForField(config, field, defaultName);
        }

        @Override
        public String nameForGetterMethod(MapperConfig<?> config, AnnotatedMethod method, String defaultName) {
            if (JsonValue.class.isAssignableFrom(method.getDeclaringClass()) || method.getDeclaringClass().getPackageName().equals("cn.zenliu.java.consul")) {
                return method.getName();
            }
            return old != null ? old.nameForGetterMethod(config, method, defaultName) : super.nameForGetterMethod(config, method, defaultName);
        }

        @Override
        public String nameForSetterMethod(MapperConfig<?> config, AnnotatedMethod method, String defaultName) {
            if (JsonValue.class.isAssignableFrom(method.getDeclaringClass()) || method.getDeclaringClass().getPackageName().equals("cn.zenliu.java.consul")) {
                return method.getName();
            }
            return old != null ? old.nameForSetterMethod(config, method, defaultName) : super.nameForSetterMethod(config, method, defaultName);
        }

        @Override
        public String nameForConstructorParameter(MapperConfig<?> config, AnnotatedParameter ctorParam, String defaultName) {
            return old != null ? old.nameForConstructorParameter(config, ctorParam, defaultName) : super.nameForConstructorParameter(config, ctorParam, defaultName);
        }
    }

    protected static class JsonValueAccessorNamingStrategyProvider extends DefaultAccessorNamingStrategy.Provider {
        final JsonValueAccessorNamingStrategy strategy;//TODO

        public JsonValueAccessorNamingStrategyProvider(JsonValueAccessorNamingStrategy strategy) {
            this.strategy = strategy;
        }

        @Override
        public AccessorNamingStrategy forPOJO(MapperConfig<?> config, AnnotatedClass targetClass) {
            if (JsonValue.class.isAssignableFrom(targetClass.getRawType())) {
                return strategy;
            }
            return super.forPOJO(config, targetClass);
        }
    }

    public JacksonCodec(ObjectMapper mapper, boolean debug) {
        var m = (mapper == null ? new ObjectMapper().findAndRegisterModules() : mapper);
        var ans = new JsonValueAccessorNamingStrategy();
        var anp = new JsonValueAccessorNamingStrategyProvider(ans);
        var pns = new JsonValuePropertyNamingStrategy(m.getPropertyNamingStrategy());
        this.mapper = m
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
                .setAccessorNaming(anp)
                .setPropertyNamingStrategy(pns)
        ;
        logger = debug ? LoggerFactory.getLogger(this.getClass()) : null;
    }


    @Override
    @SneakyThrows
    protected <T> T fromJson(ByteBuf buf, Type type) {
        if (logger != null && logger.isDebugEnabled()) logger.debug("will decode:\n{}", ByteBufUtil.prettyHexDump(buf));
        var is = new ByteBufInputStream(buf);
        return mapper.readValue((DataInput) is, mapper.constructType(type));
    }

    @Override
    @SneakyThrows
    protected void toJson(ByteBuf buf, Object value) {
        var os = new ByteBufOutputStream(buf);
        mapper.writeValue((DataOutput) os, value);
        if (logger != null && logger.isDebugEnabled()) logger.debug("encode:\n{}", ByteBufUtil.prettyHexDump(buf));
    }


    @AutoService(Codec.Provider.class)
    public static class Provider implements Codec.Provider {
        @Override
        public Codec get(boolean debug) {
            return new JacksonCodec(null, debug);
        }
    }
}