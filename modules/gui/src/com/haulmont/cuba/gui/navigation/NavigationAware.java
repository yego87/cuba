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

/**
 * Indicates that an annotated class is aware of URI changes and able to react on them.
 * <p>
 * Annotated class must by a direct or indirect subclass of {@link Screen}.
 */
public interface NavigationAware {

    /**
     * @return whether a screen can be navigated on URI change
     */
    default boolean canBeNavigatedTo() {
        return true;
    }

    /**
     * Hook to be implemented in subclasses. Called when URI params are changed when the screen is opened.
     *
     * @param uriParams URI params
     */
    default void uriParamsChanged(Map<String, String> uriParams) {
    }
}
