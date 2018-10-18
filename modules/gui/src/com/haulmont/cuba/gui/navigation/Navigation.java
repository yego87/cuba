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

import com.haulmont.cuba.gui.screen.Screen;

import java.util.Map;

public interface Navigation {

    String NAME = "cuba_Navigation";

    default void pushState(Screen screen) {
        pushState(screen, null);
    }

    void pushState(Screen screen, Map<String, String> urlParams);

    default void replaceState(Screen screen) {
        replaceState(screen, null);
    }

    void replaceState(Screen screen, Map<String, String> urlParams);

    UriState getState();

    class UriState {

        protected final String root;
        protected final String nestedRoute;
        protected final Map<String, String> params;

        public UriState(String root, String nestedRoute, Map<String, String> params) {
            this.root = root;
            this.nestedRoute = nestedRoute;
            this.params = params;
        }

        public String getRoot() {
            return root;
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

            StringBuilder sb = new StringBuilder();
            boolean paramAdded = false;

            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (!paramAdded) {
                    paramAdded = true;
                } else {
                    sb.append('&');
                }

                sb.append(entry.getKey())
                        .append("=")
                        .append(entry.getValue());
            }

            return sb.toString();
        }
    }
}
