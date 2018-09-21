/*
 * Copyright (c) 2008-2016 Haulmont.
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
 *
 */
package com.haulmont.cuba.web.gui.components;

import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.gui.components.Action;
import com.haulmont.cuba.gui.components.data.BindingState;
import com.haulmont.cuba.gui.components.data.TreeSource;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.data.impl.CollectionDsListenersWrapper;
import com.haulmont.cuba.web.widgets.CubaTree;
import com.vaadin.event.selection.SelectionEvent;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class WebTree<E extends Entity> extends WebAbstractTree<CubaTree<E>, E> {

    public WebTree() {
    }

    @Override
    protected CubaTree<E> createComponent() {
        return new CubaTree<>();
    }

    @Override
    public void initComponent(CubaTree<E> component) {
        super.initComponent(component);

        setSelectionMode(SelectionMode.SINGLE);
        component.addSelectionListener(this::onSelectionChange);
    }

    protected void onSelectionChange(SelectionEvent<E> event) {
        TreeSource<E> treeSource = getTreeSource();

        if (treeSource == null
                || treeSource.getState() == BindingState.INACTIVE) {
            return;
        }

        Set<E> selected = getSelected();
        if (selected.isEmpty()) {
            treeSource.setSelectedItem(null);
        } else {
            // reset selection and select new item
            if (isMultiSelect()) {
                treeSource.setSelectedItem(null);
            }

            E newItem = selected.iterator().next();
            treeSource.setSelectedItem(newItem);
        }

        LookupSelectionChangeEvent selectionChangeEvent = new LookupSelectionChangeEvent(this);
        publish(LookupSelectionChangeEvent.class, selectionChangeEvent);

        // todo implement selection change events
    }

    @Deprecated
    public class TreeCollectionDsListenersWrapper extends CollectionDsListenersWrapper {

        @SuppressWarnings("unchecked")
        @Override
        public void collectionChanged(CollectionDatasource.CollectionChangeEvent e) {
            // replacement for collectionChangeSelectionListener
            // #PL-2035, reload selection from ds
            Set<E> selectedItems = component.getSelectedItems();
            if (selectedItems == null) {
                selectedItems = Collections.emptySet();
            }

            //noinspection unchecked
            Set<E> newSelection = selectedItems.stream()
                    .filter(entity -> e.getDs().containsItem(entity.getId()))
                    .collect(Collectors.toSet());

            if (e.getDs().getState() == Datasource.State.VALID && e.getDs().getItem() != null) {
                newSelection.add((E) e.getDs().getItem());
            }

            if (newSelection.isEmpty()) {
                setSelected((E) null);
            } else {
                setSelectedInternal(newSelection);
            }

            super.collectionChanged(e);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void itemChanged(Datasource.ItemChangeEvent e) {
            for (Action action : getActions()) {
                action.refreshState();
            }

            super.itemChanged(e);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void itemPropertyChanged(Datasource.ItemPropertyChangeEvent e) {
            for (Action action : getActions()) {
                action.refreshState();
            }

            super.itemPropertyChanged(e);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void stateChanged(Datasource.StateChangeEvent e) {
            for (Action action : getActions()) {
                action.refreshState();
            }

            super.stateChanged(e);
        }
    }
}