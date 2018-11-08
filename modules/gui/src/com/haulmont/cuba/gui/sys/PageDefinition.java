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

package com.haulmont.cuba.gui.sys;

import com.haulmont.cuba.gui.screen.Screen;

public class PageDefinition {

    private static final String PAGE_DEF = "PageDefinition{route='%s'%s}";

    private final String route;
    private final Class<? extends Screen> parent;

    public PageDefinition(String route, Class<? extends Screen> parent) {
        this.route = route;
        this.parent = parent;
    }

    public String getRoute() {
        return route;
    }

    public Class<? extends Screen> getParent() {
        if (parent == Screen.class) {
            return null;
        }
        return parent;
    }

    @Override
    public String toString() {
        String parentClass = getParentClass();
        String parent = "".equals(parentClass)
                ? ""
                : String.format(", parent=\'%s\'", parentClass);

        return String.format(PAGE_DEF, route, parent);
    }

    protected String getParentClass() {
        if (parent == null || Screen.class == parent) {
            return "";
        }
        return parent.getName();
    }
}
