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

package com.haulmont.cuba.gui.model.impl;

import com.haulmont.cuba.gui.model.DataContext;
import com.haulmont.cuba.gui.model.DataLoader;
import com.haulmont.cuba.gui.model.InstanceContainer;
import com.haulmont.cuba.gui.model.ScreenData;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ScreenDataImpl implements ScreenData {

    protected DataContext dataContext;

    protected Map<String, InstanceContainer> containers = new HashMap<>();

    protected Map<String, DataLoader> loaders = new HashMap<>();

    @Override
    public DataContext getDataContext() {
        return dataContext;
    }

    public void setDataContext(DataContext dataContext) {
        this.dataContext = dataContext;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends InstanceContainer> T getContainer(String id) {
        T container = (T) containers.get(id);
        if (container == null) {
            throw new IllegalArgumentException(String.format("Container '%s' not found", id));
        }
        return container;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends DataLoader> T getLoader(String id) {
        T loader = (T) loaders.get(id);
        if (loader == null) {
            throw new IllegalArgumentException(String.format("Loader '%s' not found", id));
        }
        return loader;
    }

    @Override
    public Set<String> getContainerIds() {
        return containers.keySet();
    }

    @Override
    public Set<String> getLoaderIds() {
        return loaders.keySet();
    }

    @Override
    public void loadAll() {
        for (DataLoader loader : loaders.values()) {
            loader.load();
        }
    }

    @Override
    public void registerContainer(String id, InstanceContainer container) {
        containers.put(id, container);
    }

    @Override
    public void registerLoader(String id, DataLoader loader) {
        loaders.put(id, loader);
    }
}