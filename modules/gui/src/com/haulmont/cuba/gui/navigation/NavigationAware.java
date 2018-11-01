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

import com.haulmont.cuba.gui.Page;
import com.haulmont.cuba.gui.screen.Screen;

import java.util.Map;

/**
 * Enables to handle URL params changing for an annotated screen.
 * <p>
 * A screen should have a route specified with {@link Page} annotation and be a direct or indirect subclass
 * of {@link Screen}.
 * <p>
 * @see Page
 * @see Screen
 */
public interface NavigationAware {

    /**
     * Hook to be implemented in subclasses. Called when URL params are changed when annotated screen is opened.
     *
     * @param urlParams URL params
     */
    default void urlParamsChanged(Map<String, String> urlParams) {
    }
}
