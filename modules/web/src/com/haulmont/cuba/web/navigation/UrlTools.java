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

import com.haulmont.bali.util.Preconditions;
import com.haulmont.cuba.gui.navigation.Navigation;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlTools {

    /*
    protected static final String ROOT_ROUTE = "^\\!(\\w+)$";
    protected static final Pattern ROOT_ROUTE_REGEX = Pattern.compile(ROOT_ROUTE);

    protected static final String NESTED_ROUTE = "^\\!(\\w+)\\/(\\d+)\\/(\\w+(?:|\\/\\w+)*)$";
    protected static final Pattern NESTED_ROUTE_REGEX = Pattern.compile(NESTED_ROUTE);

    protected static final String PARAMS_ROUTE = "^\\!(\\w+)(?:\\/(\\d+)\\/(\\w+(?:|\\/\\w+)*)){0,1}\\?(.+)$";
    protected static final Pattern PARAMS_ROUTE_REGEX = Pattern.compile(PARAMS_ROUTE);
    */

    protected static final String ROOT_ROUTE = "^!(\\w+)$";
    protected static final Pattern ROOT_ROUTE_REGEX = Pattern.compile(ROOT_ROUTE);

    // TODO: optional state mark
    protected static final String NESTED_ROUTE = "^!(\\w+)/(\\d+)/(\\w+(?:|/\\w+)*)$";
    protected static final Pattern NESTED_ROUTE_REGEX = Pattern.compile(NESTED_ROUTE);

    protected static final String PARAMS_ROUTE = "^!(\\w+)(?:/(\\d+)/(\\w+(?:|/\\w+)*))?\\?(.+)$";
    protected static final Pattern PARAMS_ROUTE_REGEX = Pattern.compile(PARAMS_ROUTE);

    public static Navigation.UriState parseState(String uriFragment) {
        Preconditions.checkNotEmptyString(uriFragment);

        if (ROOT_ROUTE_REGEX.matcher(uriFragment).matches()) {
            return parseRootRoute(uriFragment);
        }

        if (NESTED_ROUTE_REGEX.matcher(uriFragment).matches()) {
            return parseNestedRoute(uriFragment);
        }

        if (PARAMS_ROUTE_REGEX.matcher(uriFragment).matches()) {
            return parseParamsRoute(uriFragment);
        }

        throw new RuntimeException("Failed to match URL");
    }

    protected static Navigation.UriState parseRootRoute(String uriFragment) {
        Matcher matcher = ROOT_ROUTE_REGEX.matcher(uriFragment);
        if (matcher.matches()) {
            String root = matcher.group(1);
            return new Navigation.UriState(root, "", "", Collections.emptyMap());
        }

        throw new RuntimeException("Unable to parse root route");
    }

    protected static Navigation.UriState parseNestedRoute(String uriFragment) {
        Matcher matcher = NESTED_ROUTE_REGEX.matcher(uriFragment);
        if (matcher.matches()) {
            String root = matcher.group(1);
            String stateMark = matcher.group(2);
            String nestedRoute = matcher.group(3);
            return new Navigation.UriState(root, stateMark, nestedRoute, Collections.emptyMap());
        }

        throw new RuntimeException("Unable to parse nested route");
    }

    protected static Navigation.UriState parseParamsRoute(String uriFragment) {
        Matcher matcher = PARAMS_ROUTE_REGEX.matcher(uriFragment);
        if (matcher.matches()) {
            // TODO: implement
        }

        throw new RuntimeException("Unable to parse params route");
    }
}
