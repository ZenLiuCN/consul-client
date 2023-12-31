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

import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;

import java.lang.reflect.Type;
import java.util.NoSuchElementException;
import java.util.ServiceLoader;

/**
 * @author Zen.Liu
 * @since 2023-08-19
 */
public interface Codec {
    /**
     * read json from ByteBuf,this method will consume 1 refCnt,if throws error.
     *
     * @param buf  the ByteBuf contains json data.
     * @param type the {@link Type} or {@link TypeRef}.
     * @param <T>  output object type.
     * @return result
     */
    <T> T decode(ByteBuf buf, Type type);

    /**
     * write json into ByteBuf,this method will consume 1 refCnt,if throws error.
     *
     * @param buf   the ByteBuf contains json data.
     * @param value none null object to write.
     */
    void encode(ByteBuf buf, Object value);

    /**
     * the implement should not deal with ByteBuf refCnt.
     */
    abstract class BaseCodec implements Codec {
        @Override
        public <T> T decode(ByteBuf buf, Type type) {
            //assert buf.refCnt() == 1 : " ref count is " + buf.refCnt();
            buf.retain();
            try {
                return fromJson(buf, type instanceof TypeRef<?> ref ? ref.type : type);
            } catch (Exception ex) {
                ReferenceCountUtil.release(buf, 1);
                throw ex;
            } finally {
                ReferenceCountUtil.release(buf, 1);
            }
        }

        @Override
        public void encode(ByteBuf buf, Object value) {
            // assert buf.refCnt() == 1 : " ref count is " + buf.refCnt();
            buf.retain();
            try {
                toJson(buf, value);
            } catch (Exception ex) {
                ReferenceCountUtil.release(buf, 2);
                throw ex;
            } finally {
                ReferenceCountUtil.release(buf, 1);
            }
        }

        /**
         * Implement no need to deal with RefCnt of ByteBuf.
         *
         * @param buf  the buffer
         * @param type type which exactly be the java type, not a TypeRef
         */
        protected abstract <T> T fromJson(ByteBuf buf, Type type);

        /**
         * Implement no need to deal with RefCnt of ByteBuf.
         *
         * @param buf   the buf to write to
         * @param value the value to be serialized.
         */
        protected abstract void toJson(ByteBuf buf, Object value);
    }

    /**
     * Provider of SPI.
     */
    interface Provider {
        static Codec load(boolean debug) {
            return ServiceLoader.load(Provider.class, Provider.class.getClassLoader()).findFirst().orElseThrow(() -> new NoSuchElementException("missing Codec implement")).get(debug);
        }

        Codec get(boolean debug);
    }


}
