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

package com.haulmont.cuba.web.sys.navigation;

import com.google.common.collect.ImmutableMap;
import com.haulmont.bali.util.URLEncodeUtils;
import com.haulmont.cuba.gui.navigation.NavigationState;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlTools {

    protected static final String ROOT_ROUTE = "^(\\w+)$";
    protected static final Pattern ROOT_ROUTE_PATTERN = Pattern.compile(ROOT_ROUTE);

    protected static final String NESTED_ROUTE = "^(\\w+)(?:/(\\d+))?/(\\w+(?:|/\\w+)*)$";
    protected static final Pattern NESTED_ROUTE_PATTERN = Pattern.compile(NESTED_ROUTE);

    protected static final String PARAMS_ROUTE = "^(\\w+)(?:(?:/(\\d+))?/(\\w+(?:|/\\w+)*))?\\?(.+)$";
    protected static final Pattern PARAMS_ROUTE_PATTERN = Pattern.compile(PARAMS_ROUTE);

    protected static final String PARAMS_REGEX = "^(?:(?:\\w+=[a-zA-Z0-9_/+%]+)?|\\w+=[a-zA-Z0-9_/+%]+(?:&\\w+=[a-zA-Z0-9_/+%]+)+)$";
    protected static final Pattern PARAMS_PATTERN = Pattern.compile(PARAMS_REGEX);

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

    public static String serializeIdT(Object id) {
        String encoded = Base64.getEncoder().withoutPadding()
                .encodeToString(getBytes(id));
        return URLEncodeUtils.encodeUtf8(encoded);
    }

    public static Object deserializeId(Class idClass, String base64) {
        byte[] decoded = Base64.getDecoder().decode(URLEncodeUtils.decodeUtf8(base64));
        return fromBytes(idClass, ByteBuffer.wrap(decoded));
    }

    public static NavigationState parseState(String uriFragment) {
        if (uriFragment == null || uriFragment.isEmpty()) {
            return null;
        }

        if (ROOT_ROUTE_PATTERN.matcher(uriFragment).matches()) {
            return parseRootRoute(uriFragment);
        }

        if (NESTED_ROUTE_PATTERN.matcher(uriFragment).matches()) {
            return parseNestedRoute(uriFragment);
        }

        if (PARAMS_ROUTE_PATTERN.matcher(uriFragment).matches()) {
            return parseParamsRoute(uriFragment);
        }

        return NavigationState.empty();
    }

    protected static NavigationState parseRootRoute(String uriFragment) {
        Matcher matcher = ROOT_ROUTE_PATTERN.matcher(uriFragment);
        if (matcher.matches()) {
            String root = matcher.group(1);
            return new NavigationState(root, "", "", Collections.emptyMap());
        }

        throw new RuntimeException("Unable to parse root route");
    }

    protected static NavigationState parseNestedRoute(String uriFragment) {
        Matcher matcher = NESTED_ROUTE_PATTERN.matcher(uriFragment);
        if (matcher.matches()) {
            String root = matcher.group(1);

            String stateMark;
            String nestedRoute;
            if (matcher.groupCount() == 2) {
                stateMark = "";
                nestedRoute = matcher.group(2);
            } else {
                stateMark = matcher.group(2);
                nestedRoute = matcher.group(3);
            }

            return new NavigationState(root, stateMark, nestedRoute, Collections.emptyMap());
        }

        throw new RuntimeException("Unable to parse nested route");
    }

    protected static NavigationState parseParamsRoute(String uriFragment) {
        Matcher matcher = PARAMS_ROUTE_PATTERN.matcher(uriFragment);
        if (matcher.matches()) {
            String root = matcher.group(1);
            String params = matcher.group(matcher.groupCount());

            String stateMark;
            String nestedRoute;
            if (matcher.groupCount() == 3) {
                stateMark = "";
                nestedRoute = matcher.group(2);
            } else {
                stateMark = matcher.group(2);
                nestedRoute = matcher.group(3);
            }

            return new NavigationState(root, stateMark, nestedRoute, extractParams(params));
        }

        throw new RuntimeException("Unable to parse params route");
    }

    protected static Map<String, String> extractParams(String paramsString) {
        if (!PARAMS_PATTERN.matcher(paramsString).matches()) {
            throw new RuntimeException("Params string is broken");
        }

        String[] paramPairs = paramsString.split("&");
        Map<String, String> paramsMap = new HashMap<>(paramPairs.length);

        for (String paramPair : paramPairs) {
            String[] param = paramPair.split("=");
            paramsMap.put(param[0], param[1]);
        }

        return paramsMap;
    }

    protected static byte[] getBytes(Object id) {
        return serializers.get(id.getClass()).apply(id).array();
    }

    protected static Object fromBytes(Class idClass, ByteBuffer byteBuffer) {
        return deserializers.get(idClass).apply(byteBuffer);
    }
}
