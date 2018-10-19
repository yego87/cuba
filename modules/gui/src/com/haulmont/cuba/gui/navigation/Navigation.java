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

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This bean is intended for reflecting app state to URI based on currently opened screen.
 * <p>
 * It can be used either for creating new browser history entry and replacing current state.
 * <p>
 * Passed params map enables to reflect inner screen state to URI to use it later.
 */
public interface Navigation {

    String NAME = "cuba_Navigation";

    /**
     * Pushes the state corresponding to the given {@code screen}.
     * <p>
     * Creates new entry in browser history.
     *
     * @param screen screen that is used to build new navigation state
     */
    default void pushState(Screen screen) {
        pushState(screen, false);
    }

    /**
     * Pushes the state corresponding to the given {@code screen} and fires state changed event.
     * <p>
     * Creates new entry in browser history.
     *
     * @param screen           screen that is used to build new navigation state
     * @param fireStateChanged whether state changed event should be fired
     */
    default void pushState(Screen screen, boolean fireStateChanged) {
        pushState(screen, Collections.emptyMap(), fireStateChanged);
    }

    /**
     * Pushes the state corresponding to the given {@code screen}.
     * <p>
     * The given {@code uriParams} will be reflected in URI as GET request params.
     * <p>
     * Creates new entry in browser history.
     *
     * @param screen    screen that is used to build new navigation state
     * @param uriParams URI params map
     */
    default void pushState(Screen screen, Map<String, String> uriParams) {
        pushState(screen, uriParams, false);
    }

    /**
     * Pushes the state corresponding to the given {@code screen} and fires state changed event.
     * <p>
     * The given {@code uriParams} will be reflected in URI as GET request params.
     * <p>
     * Creates new entry in browser history.
     *
     * @param screen           screen that is used to build new navigation state
     * @param uriParams        URI params map
     * @param fireStateChanged whether state changed event should be fired
     */
    void pushState(Screen screen, Map<String, String> uriParams, boolean fireStateChanged);

    /**
     * Replaces current state by the state corresponding to the given {@code screen}.
     * <p>
     * Doesn't create new entry in browser history.
     *
     * @param screen screen that is used to build new navigation state
     */
    default void replaceState(Screen screen) {
        replaceState(screen, false);
    }

    /**
     * Replaces current state by the state corresponding to the given {@code screen}.
     * <p>
     * Doesn't create new entry in browser history.
     *
     * @param screen           screen that is used to build new navigation state
     * @param fireStateChanged whether state changed event should be fired
     */
    default void replaceState(Screen screen, boolean fireStateChanged) {
        replaceState(screen, Collections.emptyMap(), fireStateChanged);
    }

    /**
     * Replaces current state by the state corresponding to the given {@code screen}.
     * <p>
     * The given {@code uriParams} will be reflected in URI as GET request params.
     * <p>
     * Doesn't create new entry in browser history.
     *
     * @param screen    screen that is used to build new navigation state
     * @param uriParams URI params map
     */
    default void replaceState(Screen screen, Map<String, String> uriParams) {
        replaceState(screen, uriParams, false);
    }

    /**
     * Replaces current state by the state corresponding to the given {@code screen} and fires state changed event.
     * <p>
     * The given {@code uriParams} will be reflected in URI as GET request params.
     * <p>
     * Doesn't create new entry in browser history.
     *
     * @param screen           screen that is used to build new navigation state
     * @param uriParams        URI params map
     * @param fireStateChanged whether state changed event should be fired
     */
    void replaceState(Screen screen, Map<String, String> uriParams, boolean fireStateChanged);

    /**
     * @return current state parsed from URI fragment.
     */
    UriState getState();

    class UriState {

        protected final String root;
        protected final String stateMark;
        protected final String nestedRoute;
        protected final Map<String, String> params;

        public UriState(String root, String nestedRoute, Map<String, String> params) {
            this.root = root;
            this.nestedRoute = nestedRoute;
            this.params = params;

            // TODO: implement
            stateMark = "";
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

            return params.entrySet()
                    .stream()
                    .map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue()))
                    .collect(Collectors.joining("&"));
        }
    }
}
