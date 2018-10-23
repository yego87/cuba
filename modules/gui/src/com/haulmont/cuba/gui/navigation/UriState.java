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

package com.haulmont.cuba.gui.navigation;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class UriState {

    protected final String root;
    protected final String stateMark;
    protected final String nestedRoute;
    protected final Map<String, String> params;

    public UriState(String root, String stateMark, String nestedRoute, Map<String, String> params) {
        this.root = root;
        this.stateMark = stateMark;
        this.nestedRoute = nestedRoute;
        this.params = params;
    }

    public String getRoot() {
        return root;
    }

    public String getStateMark() {
        return stateMark;
    }

    public String getNestedRoute() {
        return nestedRoute;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public String getParamsString() {
        if (params == null || params.isEmpty()) {
            return "";
        }

        return params.entrySet()
                .stream()
                .map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(root);

        if (stateMark != null && !stateMark.isEmpty()) {
            sb.append('/').append(stateMark);
        }

        if (nestedRoute != null && !nestedRoute.isEmpty()) {
            sb.append('/').append(nestedRoute);
        }

        if (params != null && !params.isEmpty()) {
            sb.append("?")
                    .append(getParamsString());
        }

        return sb.toString();
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null || getClass() != that.getClass()) {
            return false;
        }

        return Objects.equals(this.toString(), that.toString());
    }

    @Override
    public int hashCode() {
        return Objects.hash(root, stateMark, nestedRoute, params);
    }
}
