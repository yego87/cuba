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

import com.haulmont.cuba.gui.navigation.UriState;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlTools {

    protected static final String ROOT_ROUTE = "^!(\\w+)$";
    protected static final Pattern ROOT_ROUTE_PATTERN = Pattern.compile(ROOT_ROUTE);

    protected static final String NESTED_ROUTE = "^!(\\w+)(?:/(\\d+))?/(\\w+(?:|/\\w+)*)$";
    protected static final Pattern NESTED_ROUTE_PATTERN = Pattern.compile(NESTED_ROUTE);

    protected static final String PARAMS_ROUTE = "^!(\\w+)(?:(?:/(\\d+))?/(\\w+(?:|/\\w+)*))?\\?(.+)$";
    protected static final Pattern PARAMS_ROUTE_PATTERN = Pattern.compile(PARAMS_ROUTE);

    protected static final String PARAMS_REGEX = "^(?:(?:\\w+=[a-zA-Z0-9_/+]+)?|\\w+=[a-zA-Z0-9_/+]+(?:&\\w+=[a-zA-Z0-9_/+]+)+)$";
    protected static final Pattern PARAMS_PATTERN = Pattern.compile(PARAMS_REGEX);

    public static UriState parseState(String uriFragment) {
        if (uriFragment == null || uriFragment.isEmpty() || "!".equals(uriFragment)) {
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

        throw new RuntimeException("Failed to match URL");
    }

    protected static UriState parseRootRoute(String uriFragment) {
        Matcher matcher = ROOT_ROUTE_PATTERN.matcher(uriFragment);
        if (matcher.matches()) {
            String root = matcher.group(1);
            return new UriState(root, "", "", Collections.emptyMap());
        }

        throw new RuntimeException("Unable to parse root route");
    }

    protected static UriState parseNestedRoute(String uriFragment) {
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

            return new UriState(root, stateMark, nestedRoute, Collections.emptyMap());
        }

        throw new RuntimeException("Unable to parse nested route");
    }

    protected static UriState parseParamsRoute(String uriFragment) {
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

            return new UriState(root, stateMark, nestedRoute, extractParams(params));
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
}
