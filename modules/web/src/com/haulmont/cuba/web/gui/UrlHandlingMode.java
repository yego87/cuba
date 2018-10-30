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

package com.haulmont.cuba.web.gui;

import com.haulmont.cuba.gui.History;
import com.haulmont.cuba.gui.Navigation;
import com.haulmont.cuba.web.WebConfig;
import com.haulmont.cuba.web.sys.UriChangeHandler;
import com.haulmont.cuba.web.widgets.CubaHistoryControl;

/**
 * Defines how URL changes should be handled.
 */
public enum UrlHandlingMode {

    /**
     * URL changes are not handled at all.
     */
    NONE,
    /**
     * {@link CubaHistoryControl} is used to handle changes.
     * <p>
     * Replacement for {@link WebConfig#getAllowHandleBrowserHistoryBack()}.
     */
    BACK_ONLY,
    /**
     * Changes are handled by {@link Navigation}, {@link History} and {@link UriChangeHandler}.
     *
     * @see Navigation
     * @see History
     * @see UriChangeHandler
     */
    NATIVE
}
