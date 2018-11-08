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

public final class UiControllerDefinition {

    private static final String UI_CONTROLLER_DEF = "UiControllerDefinition{id='%s', controllerClass='%s'%s}";

    private final String id;
    private final String controllerClass;
    private final PageDefinition pageDefinition;

    public UiControllerDefinition(String id, String controllerClass) {
        this.id = id;
        this.controllerClass = controllerClass;
        this.pageDefinition = null;
    }

    public UiControllerDefinition(String id, String controllerClass, PageDefinition pageDefinition) {
        this.id = id;
        this.controllerClass = controllerClass;
        this.pageDefinition = pageDefinition;
    }

    public String getId() {
        return id;
    }

    public String getControllerClass() {
        return controllerClass;
    }

    public PageDefinition getPageDefinition() {
        return pageDefinition;
    }

    @Override
    public String toString() {
        String pageDef = pageDefinition == null
                ? ""
                : ", " + pageDefinition.toString();

        return String.format(UI_CONTROLLER_DEF, id, controllerClass, pageDef);
    }

}