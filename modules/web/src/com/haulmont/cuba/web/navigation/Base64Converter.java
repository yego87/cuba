/*
 * Copyright (c) 2008-2018 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.haulmont.cuba.web.navigation;

import com.google.common.collect.ImmutableMap;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public final class Base64Converter {

    @SuppressWarnings("CodeBlock2Expr")
    protected static final Map<Class, Function<Object, ByteBuffer>> serializers = ImmutableMap.of(
            String.class, obj -> {
                return ByteBuffer.wrap(((String) obj).getBytes());
            },
            Integer.class, obj -> {
                return ByteBuffer.allocate(4).putInt((int) obj);
            },
            Long.class, obj -> {
                return ByteBuffer.allocate(Long.BYTES).putLong((Long) obj);
            },
            UUID.class, obj -> {
                return ByteBuffer.allocate(Long.BYTES * 2)
                        .putLong(((UUID) obj).getMostSignificantBits())
                        .putLong(((UUID) obj).getLeastSignificantBits());
            });

    protected static final Map<Class, Function<ByteBuffer, Object>> deserializers = ImmutableMap.of(
            String.class, bb -> new String(bb.array()),
            Integer.class, ByteBuffer::getInt,
            Long.class, ByteBuffer::getLong,
            UUID.class, bb -> new UUID(bb.getLong(), bb.getLong())
    );

    public static String serialize(Object id) {
        return Base64.getEncoder()
                .withoutPadding()
                .encodeToString(getBytes(id));
    }

    public static Object deserialize(Class idClass, String base64) {
        byte[] decoded = Base64.getDecoder()
                .decode(base64);
        return fromBytes(idClass, ByteBuffer.wrap(decoded));
    }

    protected static byte[] getBytes(Object id) {
        return serializers.get(id.getClass())
                .apply(id)
                .array();
    }

    protected static Object fromBytes(Class idClass, ByteBuffer byteBuffer) {
        return deserializers.get(idClass)
                .apply(byteBuffer);
    }
}
