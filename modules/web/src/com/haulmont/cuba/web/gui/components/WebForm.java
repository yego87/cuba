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

package com.haulmont.cuba.web.gui.components;

import com.haulmont.bali.events.Subscription;
import com.haulmont.bali.util.Preconditions;
import com.haulmont.cuba.gui.ComponentsHelper;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.EditableChangeNotifier;
import com.haulmont.cuba.gui.components.EditableChangeNotifier.EditableChangeEvent;
import com.haulmont.cuba.gui.components.Form;
import com.haulmont.cuba.gui.sys.TestIdManager;
import com.haulmont.cuba.web.AppUI;
import com.haulmont.cuba.web.widgets.CubaFieldGroupLayout;
import com.vaadin.ui.GridLayout;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

public class WebForm extends WebAbstractComponent<CubaFieldGroupLayout> implements Form {

    protected List<List<Component>> columnComponentMapping = new ArrayList<>();

    {
        columnComponentMapping.add(new ArrayList<>());
    }

    public WebForm() {
        component = createComponent();
    }

    protected CubaFieldGroupLayout createComponent() {
        return new CubaFieldGroupLayout();
    }

    protected boolean editable = true;

    @Override
    public void setId(String id) {
        super.setId(id);

        if (id != null && AppUI.getCurrent().isTestMode()) {
            for (Component component : getOwnComponents()) {
                com.vaadin.ui.Component composition = WebComponentsHelper.getComposition(component);
                if (composition != null) {
                    composition.setCubaId(component.getId());
                }
            }
        }
    }

    @Override
    public void setDebugId(String id) {
        super.setDebugId(id);

        AppUI ui = AppUI.getCurrent();
        if (ui != null && id != null) {
            for (final Component component : getOwnComponents()) {
                com.vaadin.ui.Component composition = WebComponentsHelper.getComposition(component);
                if (composition != null) {
                    composition.setId(ui.getTestIdManager().getTestId(id + "_" + component.getId()));
                }
            }
        }
    }

    @Override
    public boolean isEditable() {
        return this.editable;
    }

    @Override
    public void setEditable(boolean editable) {
        if (editable != isEditable()) {
            this.editable = editable;

            EditableChangeEvent event = new EditableChangeEvent(this);
            publish(EditableChangeEvent.class, event);
        }
    }

    @Override
    public Subscription addEditableChangeListener(Consumer<EditableChangeEvent> listener) {
        return getEventHub().subscribe(EditableChangeEvent.class, listener);
    }

    @Override
    public void removeEditableChangeListener(Consumer<EditableChangeEvent> listener) {
        unsubscribe(EditableChangeEvent.class, listener);
    }

    @Override
    public void add(Component childComponent) {
        add(childComponent, 0);
    }

    @Override
    public void add(Component childComponent, int column) {
        List<Component> colFields = columnComponentMapping.get(column);
        add(childComponent, column, colFields.size());
    }

    @Override
    public void add(Component childComponent, int column, int row) {
        checkArgument(column >= 0 && column < this.component.getColumns(),
                "Illegal column number %s, available amount of columns is %s", column, this.component.getColumns());

        List<Component> colFields = columnComponentMapping.get(column);
        checkArgument(row >= 0 && row <= colFields.size(),
                "Illegal row number %s, available amount of rows is %s", row, this.component.getRows());

        addComponentInternal(childComponent, column, row);
    }

    protected void addComponentInternal(Component childComponent, int column, int row) {
        List<Component> colFields = columnComponentMapping.get(column);
        colFields.add(row, childComponent);

        managedComponentAssigned(childComponent, column);
    }

    protected void managedComponentAssigned(Component childComponent, int column) {
        com.vaadin.ui.Component vComponent = WebComponentsHelper.getComposition(childComponent);
        assignTypicalAttributes(childComponent);
        assignDebugId(vComponent, childComponent.getId());

        this.component.setRows(detectRowsCount());

        reattachColumnFields(column);
    }

    protected void assignTypicalAttributes(Component component) {
        if (getFrame() != null && component instanceof BelongToFrame) {
            BelongToFrame belongToFrame = (BelongToFrame) component;
            if (belongToFrame.getFrame() == null) {
                belongToFrame.setFrame(getFrame());
            }
        }

        component.setParent(this);
    }

    protected void assignDebugId(com.vaadin.ui.Component composition, String id) {
        AppUI ui = AppUI.getCurrent();
        if (ui == null) {
            return;
        }

        String debugId = getDebugId();

        if (ui.isTestMode()) {
            if (composition != null) {
                composition.setCubaId(id);
            }
        }

        if (ui.isPerformanceTestMode()) {
            if (composition != null && debugId != null) {
                TestIdManager testIdManager = ui.getTestIdManager();
                composition.setId(testIdManager.getTestId(debugId + "_" + id));
            }
        }
    }

    protected int detectRowsCount() {
        int rowsCount = columnComponentMapping.stream()
                .mapToInt(List::size)
                .max().orElse(0);

        return Math.max(rowsCount, 1);
    }

    protected void reattachColumnFields(int colIndex) {
        for (int i = 0; i < component.getRows(); i++) {
            component.removeComponent(colIndex, i);
        }

        List<Component> columnFields = columnComponentMapping.get(colIndex);
        int insertRowIndex = 0;
        for (Component field : columnFields) {
            com.vaadin.ui.Component composition = WebComponentsHelper.getComposition(field);
            this.component.addComponent(composition, colIndex, insertRowIndex++);
        }
    }

    @Override
    public void remove(Component childComponent) {
        int column = 0;
        for (List<Component> components : columnComponentMapping) {
            if (components.remove(childComponent)) {
                reattachColumnFields(column);
                component.setRows(detectRowsCount());
                childComponent.setParent(null);
                break;
            }
            column++;
        }
    }

    @Override
    public void removeAll() {
        // FIXME: gg, the component remains its size
        /*for (Component component : new ArrayList<>(getOwnComponents())) {
            remove(component);
        }*/

        component.removeAllComponents();

        List<Component> components = new ArrayList<>(getOwnComponents());

        columnComponentMapping.clear();
        columnComponentMapping.add(new ArrayList<>());

        for (Component component : components) {
            component.setParent(null);
        }
    }

    @Nullable
    @Override
    public Component getOwnComponent(String id) {
        Preconditions.checkNotNullArgument(id);

        return getOwnComponents().stream()
                .filter(component -> Objects.equals(id, component.getId()))
                .findFirst()
                .orElse(null);
    }

    @Nullable
    @Override
    public Component getComponent(String id) {
        return ComponentsHelper.getComponent(this, id);
    }

    @Override
    public Collection<Component> getOwnComponents() {
        return columnComponentMapping.stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<Component> getComponents() {
        return ComponentsHelper.getComponents(this);
    }

    @Override
    public Collection<Component> getComponents(int column) {
        return Collections.unmodifiableCollection(columnComponentMapping.get(column));
    }

    @Override
    public Component getComponent(int column, int row) {
        return columnComponentMapping.get(column).get(row);
    }

    @Override
    public CaptionAlignment getChildCaptionAlignment() {
        return component.isUseInlineCaption()
                ? CaptionAlignment.LEFT
                : CaptionAlignment.TOP;
    }

    @Override
    public void setChildCaptionAlignment(CaptionAlignment captionAlignment) {
        component.setUseInlineCaption(CaptionAlignment.LEFT.equals(captionAlignment));
    }

    @Override
    public int getChildCaptionWidth() {
        return component.getFixedCaptionWidth();
    }

    @Override
    public void setChildCaptionWidth(int width) {
        component.setFixedCaptionWidth(width);
    }

    @Override
    public int getChildCaptionWidth(int column) {
        return component.getFieldCaptionWidth(column);
    }

    @Override
    public void setChildCaptionWidth(int column, int width) {
        component.setFieldCaptionWidth(column, width);
    }

    @Override
    public int getColumns() {
        return component.getColumns();
    }

    @Override
    public void setColumns(int columns) {
        if (component.getColumns() != columns) {
            try {
                component.setColumns(columns);
            } catch (GridLayout.OutOfBoundsException e) {
                // Replace the default exception with something more meaningful
                throw new IllegalStateException("Can't shrink columns with components within them");
            }

            List<List<Component>> oldColumnComponents = columnComponentMapping;
            columnComponentMapping = new ArrayList<>();
            for (int i = 0; i < columns; i++) {
                if (i < oldColumnComponents.size()) {
                    columnComponentMapping.add(oldColumnComponents.get(i));
                } else {
                    columnComponentMapping.add(new ArrayList<>());
                }
            }
        }
    }
}
