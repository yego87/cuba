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

import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;

public class IdToBase64Converter {

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
        if (id instanceof UUID) {
            UUID uuid = (UUID) id;

            ByteBuffer bb = ByteBuffer.allocate(Long.BYTES * 2);
            bb.putLong(uuid.getMostSignificantBits());
            bb.putLong(uuid.getLeastSignificantBits());

            return bb.array();
        } else {
            return SerializationUtils.serialize(((Serializable) id));
        }
    }

    protected static Object fromBytes(Class idClass, ByteBuffer byteBuffer) {
        if (idClass == UUID.class) {
            return new UUID(byteBuffer.getLong(), byteBuffer.getLong());
        } else {
            return SerializationUtils.deserialize(byteBuffer.array());
        }
    }
}
