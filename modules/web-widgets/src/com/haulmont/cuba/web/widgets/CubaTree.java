/*
 * Copyright (c) 2008-2017 Haulmont.
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

package com.haulmont.cuba.web.widgets;

import com.google.common.base.Preconditions;
import com.vaadin.data.provider.HierarchicalQuery;
import com.vaadin.event.Action;
import com.vaadin.event.ActionManager;
import com.vaadin.event.ShortcutListener;
import com.vaadin.shared.Registration;
import com.vaadin.ui.Tree;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.components.grid.GridSelectionModel;

import java.util.Collection;
import java.util.stream.Collectors;

public class CubaTree<T> extends Tree<T> implements Action.ShortcutNotifier {

    protected Runnable beforePaintListener;

    /**
     * Keeps track of the ShortcutListeners added to this component, and manages the painting and handling as well.
     */
    protected ActionManager shortcutActionManager;

    @Override
    protected TreeGrid<T> createTreeGrid() {
        return new CubaTreeGrid<>();
    }

    @SuppressWarnings("unchecked")
    @Override
    public CubaTreeGrid<T> getCompositionRoot() {
        return (CubaTreeGrid<T>) super.getCompositionRoot();
    }

    public void setGridSelectionModel(GridSelectionModel<T> model) {
        getCompositionRoot().setGridSelectionModel(model);
    }

    public Collection<T> getChildren(T item) {
        return getDataProvider()
                .fetchChildren(new HierarchicalQuery<>(null, item))
                .collect(Collectors.toList());
    }

    public boolean hasChildren(T item) {
        return getDataProvider().hasChildren(item);
    }

    // TODO: gg, replace
    /*@Override
    public void changeVariables(Object source, Map<String, Object> variables) {
        super.changeVariables(source, variables);

        if (shortcutActionManager != null) {
            shortcutActionManager.handleActions(variables, this);
        }
    }*/

    @Override
    public Registration addShortcutListener(ShortcutListener shortcut) {
        if (shortcutActionManager == null) {
            shortcutActionManager = new ShortcutActionManager(this);
        }

        shortcutActionManager.addAction(shortcut);

        return () -> shortcutActionManager.removeAction(shortcut);
    }

    @Override
    public void removeShortcutListener(ShortcutListener shortcut) {
        if (shortcutActionManager != null) {
            shortcutActionManager.removeAction(shortcut);
        }
    }

    // TODO: gg, replace
    /*@Override
    protected void paintActions(PaintTarget target, Set<Action> actionSet) throws PaintException {
        super.paintActions(target, actionSet);

        if (shortcutActionManager != null) {
            shortcutActionManager.paintActions(null, target);
        }
    }*/

    // TODO: gg, replace
    /*@Override
    public void paintContent(PaintTarget target) throws PaintException {
        if (beforePaintListener != null) {
            beforePaintListener.run();
        }

        if (isNodeCaptionsAsHtml()) {
            target.addAttribute("nodeCaptionsAsHtml", true);
        }
        super.paintContent(target);
    }*/

    public void expandAll() {
        // TODO: gg, implement
        /*for (Object id : getItems()) {
            expandItemRecursively(id);
        }*/
    }

    // TODO: gg, implement
    public void expandItemRecursively(T item) {
        /*expandItem(id);
        if (hasChildren(id)) {
            for (Object childId: getChildren(id)) {
                expandItemRecursively(childId);
            }
        }*/
    }

    public void expandItemWithParents(T item) {
        // TODO: gg, implement
        /*Object currentId = id;
        while (currentId != null) {
            expandItem(currentId);

            currentId = getParent(currentId);
        }*/
    }

    public void collapseItemRecursively(T item) {
        // TODO: gg, implement
        /*if (hasChildren(id)) {
            for (Object childId: getChildren(id)) {
                collapseItemRecursively(childId);
            }
        }
        collapseItem(id);*/
    }

    public void collapseAll() {
        // TODO: gg, implement
        /*for (Object id : getItemIds()) {
            collapseItemRecursively(id);
        }*/
    }

    public void expandUpTo(int level) {
        Preconditions.checkArgument(level > 0, "level should be greater than 0");

        // TODO: gg, implement
        /*List<Object> currentLevelItemIds = new ArrayList<>(getItemIds());

        int i = 0;
        while (i < level && !currentLevelItemIds.isEmpty()) {
            for (Object itemId : new ArrayList<>(currentLevelItemIds)) {
                // TODO: gg, implement
                expandItem(itemId);
                currentLevelItemIds.remove(itemId);
                currentLevelItemIds.addAll(getChildren(itemId));
            }
            i++;
        }*/
    }

    public void deselectAll() {
        getSelectionModel().deselectAll();
    }

    public void repaint() {
        markAsDirtyRecursive();
        getCompositionRoot().repaint();
    }

    public void setBeforePaintListener(Runnable beforePaintListener) {
        this.beforePaintListener = beforePaintListener;
    }
}