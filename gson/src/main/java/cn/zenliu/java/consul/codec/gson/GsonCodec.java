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

package cn.zenliu.java.consul.codec.gson;

import cn.zenliu.java.consul.trasport.Codec;
import com.google.auto.service.AutoService;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.ByteBufUtil;
import lombok.SneakyThrows;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.util.Base64;

/**
 * @author Zen.Liu
 * @since 2023-08-20
 */
@SuppressWarnings("unused")
public class GsonCodec extends Codec.BaseCodec {
    protected static class Base64TypeAdapter extends TypeAdapter<byte[]> {

        @Override
        public void write(JsonWriter out, byte[] value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(Base64.getEncoder().encodeToString(value));
            }
        }

        @Override
        public byte[] read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return new byte[0];
            } else {
                String data = in.nextString();
                return Base64.getDecoder().decode(data);
            }
        }
    }

    protected static final Base64TypeAdapter Base64TypeAdapter = new Base64TypeAdapter();
    protected final Gson gson;
    protected final Logger logger;

    public GsonCodec(@Nullable Gson gson, boolean debug) {

        this.gson = (gson == null ? new Gson() : gson).newBuilder()
                .registerTypeAdapter(byte[].class, Base64TypeAdapter)
                .create();
        logger = debug ? LoggerFactory.getLogger(this.getClass()) : null;
    }

    @Override
    protected <T> T fromJson(ByteBuf buf, Type type) {
        if (logger != null && logger.isDebugEnabled()) logger.debug("will decode:\n{}", ByteBufUtil.prettyHexDump(buf));
        var is = new ByteBufInputStream(buf);
        var r = new InputStreamReader(is);
        return gson.fromJson(r, type);
    }

    @Override
    @SneakyThrows
    protected void toJson(ByteBuf buf, Object value) {
        var os = new ByteBufOutputStream(buf);
        var w = new OutputStreamWriter(os);
        gson.toJson(value, w);
        w.flush(); //!! required
        if (logger != null && logger.isDebugEnabled()) logger.debug("encoded:\n{}", ByteBufUtil.prettyHexDump(buf));
    }


    @AutoService(Codec.Provider.class)
    public static class Provider implements Codec.Provider {

        @Override
        public Codec get(boolean debug) {
            return new GsonCodec(null, true);
        }
    }
}
