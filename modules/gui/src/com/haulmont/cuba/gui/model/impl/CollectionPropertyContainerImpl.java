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

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.gui.model.CollectionPropertyContainer;
import com.haulmont.cuba.gui.model.InstanceContainer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

public class CollectionPropertyContainerImpl<E extends Entity>
        extends CollectionContainerImpl<E> implements CollectionPropertyContainer<E> {

    protected InstanceContainer parent;
    protected String property;

    public CollectionPropertyContainerImpl(MetaClass metaClass, InstanceContainer parent, String property) {
        super(metaClass);
        this.parent = parent;
        this.property = property;
    }

    @Override
    public InstanceContainer getParent() {
        return parent;
    }

    @Override
    public String getProperty() {
        return property;
    }

    @Override
    public List<E> getDisconnectedItems() {
        return super.getMutableItems();
    }

    @Override
    public List<E> getMutableItems() {
        return new ObservableList<>(collection, (changeType, changes) -> {
            buildIdMap();
            clearItemIfNotExists();
            updateParent();
            fireCollectionChanged(changeType, changes);
        });
    }

    @SuppressWarnings("unchecked")
    protected void updateParent() {
        MetaClass parentMetaClass = parent.getEntityMetaClass();
        MetaProperty metaProperty = parentMetaClass.getPropertyNN(property);

        if (metaProperty.getRange().getCardinality().isMany()) {
            Collection collection = parent.getItem().getValue(metaProperty.getName());
            if (collection == null) {
                if (List.class.isAssignableFrom(metaProperty.getJavaType())) {
                    collection = new ArrayList(this.collection);
                } else {
                    collection = new LinkedHashSet(this.collection);
                }
                parent.getItem().setValue(metaProperty.getName(), collection);
            }
            collection.clear();
            collection.addAll(this.collection);
        }
    }
}
