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

import com.haulmont.bali.events.Subscription;
import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.cuba.gui.ComponentsHelper;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.app.security.role.edit.UiPermissionDescriptor;
import com.haulmont.cuba.gui.app.security.role.edit.UiPermissionValue;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.data.HasValueBinding;
import com.haulmont.cuba.gui.components.data.ValueSource;
import com.haulmont.cuba.gui.components.data.ValueSourceProvider;
import com.haulmont.cuba.gui.components.security.ActionsPermissions;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.sys.TestIdManager;
import com.haulmont.cuba.gui.theme.ThemeConstants;
import com.haulmont.cuba.gui.xml.layout.loaders.FieldGroupLoader.FieldConfig;
import com.haulmont.cuba.web.App;
import com.haulmont.cuba.web.AppUI;
import com.haulmont.cuba.web.widgets.CubaFieldGroup;
import com.haulmont.cuba.web.widgets.CubaFieldGroupLayout;
import com.vaadin.ui.AbstractComponent;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.haulmont.bali.util.Preconditions.checkNotNullArgument;

public class WebFieldGroup extends WebAbstractComponent<CubaFieldGroupLayout> implements FieldGroup, UiPermissionAware {

    protected CubaFieldGroup wrapper;
    protected boolean wrapperAttached = false;

    protected Map<String, Component> fields = new HashMap<>(); // TODO: gg, remove
    protected List<List<Component>> columnFieldMapping = new ArrayList<>();

    protected UiComponents uiComponents;

    protected boolean editable = true;

    {
        columnFieldMapping.add(new ArrayList<>());
    }

    protected ValueSourceProvider valueSourceProvider;

    public WebFieldGroup() {
        wrapper = createWrapper();
        component = createComponent();
    }

    protected CubaFieldGroupLayout createComponent() {
        return new CubaFieldGroupLayout();
    }

    protected CubaFieldGroup createWrapper() {
        return new CubaFieldGroup();
    }

    @Inject
    public void setUiComponents(UiComponents uiComponents) {
        this.uiComponents = uiComponents;
    }

    @Override
    public void setDebugId(String id) {
        super.setDebugId(id);

        AppUI ui = AppUI.getCurrent();
        if (ui != null && id != null) {
            for (final Component field : fields.values()) {
                if (!(field instanceof FieldGroupEmptyField)) {
                    com.vaadin.ui.Component vComponent = field.unwrapComposition(com.vaadin.ui.Component.class);
                    vComponent.setId(ui.getTestIdManager().getTestId(id + "_" + field.getId()));
                }
            }
        }
    }

    @Override
    public void setId(String id) {
        super.setId(id);

        if (id != null && AppUI.getCurrent().isTestMode()) {
            for (final Component field : fields.values()) {
                if (!(field instanceof FieldGroupEmptyField)) {
                    com.vaadin.ui.Component vComponent = field.unwrapComposition(com.vaadin.ui.Component.class);
                    vComponent.setCubaId(field.getId());
                }
            }
        }
    }

    @Override
    public FieldConfig createField(String id) {
        return new FieldConfigImpl(id, null);
    }

    @Override
    public List<FieldConfig> getFields() {
        return getColumnOrderedFields();
    }

    @Override
    public List<FieldConfig> getFields(int column) {
        List<Component> colComponents = columnFieldMapping.get(column);
        return wrapComponents(colComponents);
    }

    @Override
    public FieldConfig getField(int column, int row) {
        Component component = columnFieldMapping.get(column).get(row);
        String fieldId = getFieldId(component);
        return fieldId != null ? wrapComponent(fieldId, component) : null;
    }

    @Override
    public FieldConfig getField(String fieldId) {
        Component component = findComponent(fieldId);
        return component != null
                ? wrapComponent(fieldId, component)
                : null;
    }

    @Override
    public FieldConfig getFieldNN(String fieldId) {
        Component component = findComponent(fieldId);
        if (component == null) {
            throw new IllegalArgumentException("Unable to find field with id " + fieldId);
        }

        return wrapComponent(fieldId, component);
    }

    @Override
    public List<Component> getOwnComponents() {
        return getColumnOrderedComponents();
    }

    @Override
    public FieldCaptionAlignment getCaptionAlignment() {
        if (component.isUseInlineCaption()) {
            return FieldCaptionAlignment.LEFT;
        }
        return FieldCaptionAlignment.TOP;
    }

    @Override
    public void setCaptionAlignment(FieldCaptionAlignment captionAlignment) {
        component.setUseInlineCaption(WebComponentsHelper.convertFieldGroupCaptionAlignment(captionAlignment));
    }

    @Override
    public void addField(FieldConfig field) {
        addField(field, 0);
    }

    @Override
    public void addField(FieldConfig fc, int colIndex) {
        List<Component> colFields = columnFieldMapping.get(colIndex);
        addFieldInternal(fc, colIndex, colFields.size());
    }

    @Override
    public void addField(FieldConfig fc, int colIndex, int rowIndex) {
        checkArgument(!fields.containsKey(fc.getId()), "Field '%s' is already registered", fc.getId());
        checkArgument(this == ((FieldConfigImpl) fc).getOwner(), "Field does not belong to this FieldGroup");

        if (colIndex < 0 || colIndex >= component.getColumns()) {
            throw new IllegalArgumentException(String.format("Illegal column number %s, available amount of columns is %s",
                    colIndex, component.getColumns()));
        }
        List<Component> colFields = columnFieldMapping.get(colIndex);
        if (rowIndex < 0 || rowIndex > colFields.size()) {
            throw new IllegalArgumentException(String.format("Illegal row number %s, available amount of rows is %s",
                    rowIndex, component.getRows()));
        }

        addFieldInternal(fc, colIndex, rowIndex);
    }

    @Override
    public void add(Component childComponent) {
        add(childComponent, 0);
    }

    @Override
    public void setComponent(String id, Component field) {
        Component emptyComponent = fields.get(id);
        if (emptyComponent == null) {
            throw new IllegalArgumentException(String.format("Not found component with id '%s'", id));
        }

        if (!(emptyComponent instanceof FieldGroupEmptyField)) {
            throw new IllegalStateException(String.format("Field '%s' must be defined as custom", id));
        }

        int colIndex = findColumnIndex(emptyComponent);
        List<Component> components = columnFieldMapping.get(colIndex);
        int rowIndex = components.indexOf(emptyComponent);

        FieldConfig fieldConfig = ((FieldGroupEmptyField) emptyComponent).getFieldConfig();
        if (fieldConfig != null) {
            applyFieldDefaults(field, fieldConfig);
            assignFieldId(fieldConfig.getId(), field);
        }

        if (field instanceof HasCaption
                && ((HasCaption) field).getCaption() == null) {
            ((HasCaption) field).setCaption(" "); // use space in caption for proper layout
        }

        setComponentInternal(id, field, colIndex, rowIndex);
    }

    // TODO: gg, move to the single place
    protected void applyFieldDefaults(Component fieldComponent, FieldConfig fc) {

        if (fc.isVisible() != null) {
            fieldComponent.setVisible(fc.isVisible());
        }

        if (fieldComponent instanceof Component.HasCaption) {
            if (fc.getCaption() != null) {
                ((Component.HasCaption) fieldComponent).setCaption(fc.getCaption());
            }
            if (fc.getDescription() != null) {
                ((Component.HasCaption) fieldComponent).setDescription(fc.getDescription());
            }
        }

        if (fieldComponent instanceof Field) {
            Field cubaField = (Field) fieldComponent;

            if (fc.isRequired() != null) {
                cubaField.setRequired(fc.isRequired());
            }
            if (fc.getRequiredMessage() != null) {
                cubaField.setRequiredMessage(fc.getRequiredMessage());
            }
            if (fc.isEditable() != null) {
                cubaField.setEditable(fc.isEditable());
            }

            for (Consumer validator : fc.getValidators()) {
                //noinspection unchecked
                cubaField.addValidator(validator);
            }

            if (fc.getWidth() != null) {
                fieldComponent.setWidth(fc.getWidth());
            } else {
                if (App.isBound()) {
                    ThemeConstants theme = App.getInstance().getThemeConstants();
                    fieldComponent.setWidth(theme.get("cuba.web.WebFieldGroup.defaultFieldWidth"));
                }
            }
        }

        if (fieldComponent instanceof HasContextHelp) {
            if (fc.getContextHelpText() != null) {
                ((HasContextHelp) fieldComponent).setContextHelpText(fc.getContextHelpText());
            }
            if (fc.isContextHelpTextHtmlEnabled() != null) {
                ((HasContextHelp) fieldComponent).setContextHelpTextHtmlEnabled(fc.isContextHelpTextHtmlEnabled());
            }
            if (fc.getContextHelpIconClickHandler() != null) {
                ((HasContextHelp) fieldComponent).setContextHelpIconClickHandler(fc.getContextHelpIconClickHandler());
            }
        }

        if (fieldComponent instanceof Component.Focusable && fc.getTabIndex() != null) {
            ((Component.Focusable) fieldComponent).setTabIndex(fc.getTabIndex());
        }

        if (fieldComponent instanceof HasInputPrompt && fc.getInputPrompt() != null) {
            ((HasInputPrompt) fieldComponent).setInputPrompt(fc.getInputPrompt());
        }

        if (fieldComponent instanceof HasFormatter && fc.getFormatter() != null) {
            //noinspection unchecked
            ((HasFormatter) fieldComponent).setFormatter(fc.getFormatter());
        }

        if (StringUtils.isNotEmpty(fc.getStyleName())) {
            fieldComponent.setStyleName(fc.getStyleName());
        }
    }

    protected int findColumnIndex(Component component) {
        for (int i = 0; i < columnFieldMapping.size(); i++) {
            List<Component> components = columnFieldMapping.get(i);
            if (components.contains(component)) {
                return i;
            }
        }
        throw new IllegalArgumentException(String.format("Not found component with id '%s'", id));
    }

    protected void setComponentInternal(String id, Component field, int colIndex, int rowIndex) {
        fields.put(id, field);

        List<Component> colFields = columnFieldMapping.get(colIndex);
        colFields.remove(rowIndex);
        colFields.add(rowIndex, field);

        managedFieldComponentAssigned(field, colIndex);
    }

    @Override
    public void add(Component childComponent, int colIndex) {
        List<Component> colFields = columnFieldMapping.get(colIndex);
        add(childComponent, colIndex, colFields.size());
    }

    @Override
    public void add(Component childComponent, int colIndex, int rowIndex) {
        // FIXME: gg, what if id == null?
        checkArgument(!fields.containsKey(childComponent.getId()),
                "Field '%s' is already registered", childComponent.getId());
//        checkArgument(this == ((FieldConfigImpl) fc).getOwner(), "Field does not belong to this FieldGroup");

        if (colIndex < 0 || colIndex >= this.component.getColumns()) {
            throw new IllegalArgumentException(String.format("Illegal column number %s, available amount of columns is %s",
                    colIndex, this.component.getColumns()));
        }

        List<Component> colFields = columnFieldMapping.get(colIndex);
        if (rowIndex < 0 || rowIndex > colFields.size()) {
            throw new IllegalArgumentException(String.format("Illegal row number %s, available amount of rows is %s",
                    rowIndex, this.component.getRows()));
        }

        addComponentInternal(childComponent, colIndex, rowIndex);
    }

    protected void addComponentInternal(Component field, int colIndex, int rowIndex) {
        List<Component> colFields = columnFieldMapping.get(colIndex);

        // FIXME: gg, what if a field has no id?
        fields.put(field.getId(), field);
        colFields.add(rowIndex, field);

        if (!(field instanceof FieldGroupEmptyField)) {
            managedFieldComponentAssigned(field, colIndex);
        }
    }

    protected void managedFieldComponentAssigned(Component field, int colIndex) {
        com.vaadin.ui.Component fieldImpl = WebComponentsHelper.getComposition(field);
        assignTypicalAttributes(field);
        assignDebugId(fieldImpl, field.getId());

        this.component.setRows(detectRowsCount());

        reattachColumnFields(colIndex);
    }

    @Nullable
    @Override
    public Component getOwnComponent(String id) {
        FieldConfig fieldConfig = getField(id);
        if (fieldConfig != null && fieldConfig.isBound()) {
            return fieldConfig.getComponent();
        }
        return null;
    }

    @Nullable
    @Override
    public Component getComponent(String id) {
        return ComponentsHelper.getComponent(this, id);
    }

    @Override
    public Collection<Component> getComponents() {
        return ComponentsHelper.getComponents(this);
    }

    protected void addFieldInternal(FieldConfig fc, int colIndex, int rowIndex) {
        Component component = fc.getComponent();
        if (component == null) {
            component = uiComponents.create(FieldGroupEmptyField.NAME);
            ((FieldGroupEmptyField) component).setFieldConfig(fc);
        } else {
            assignFieldId(fc.getId(), component);
        }

        addComponentInternal(component, colIndex, rowIndex);
    }

    protected void reattachColumnFields(int colIndex) {
        for (int i = 0; i < component.getRows(); i++) {
            component.removeComponent(colIndex, i);
        }

        List<Component> columnFields = columnFieldMapping.get(colIndex);
        int insertRowIndex = 0;
        for (Component field : columnFields) {
            if (!(field instanceof FieldGroupEmptyField)) {
                com.vaadin.ui.Component fieldImpl = WebComponentsHelper.getComposition(field);
                this.component.addComponent(fieldImpl, colIndex, insertRowIndex);
                insertRowIndex++;
            }
        }
    }

    @Override
    public void removeField(String fieldId) {
        removeField(getFieldNN(fieldId));
    }

    @Override
    public void removeField(FieldConfig fc) {
        checkArgument(this == ((FieldConfigImpl) fc).getOwner(), "Field is not belong to this FieldGroup");
        Component component = findComponent(fc.getId());
        removeComponentInternal(component);
    }

    @Override
    public void remove(Component childComponent) {
        checkArgument(this == childComponent.getParent(), "Component is not belong to this FieldGroup");
        removeComponentInternal(childComponent);
    }

    protected void removeComponentInternal(Component childComponent) {
        if (fields.values().contains(childComponent)) {
            // TODO: gg, what if id is null?
            int colIndex = findColumnIndex(childComponent);
            columnFieldMapping.get(colIndex).remove(childComponent);
            fields.remove(childComponent.getId());

            component.setRows(detectRowsCount());

            childComponent.setParent(null);
        }
    }

    @Override
    public void removeAll() {
        for (Component component : getColumnOrderedComponents()) {
            remove(component);
        }
    }

    protected void assignTypicalAttributes(Component c) {
        if (getFrame() != null && c instanceof BelongToFrame) {
            BelongToFrame belongToFrame = (BelongToFrame) c;
            if (belongToFrame.getFrame() == null) {
                belongToFrame.setFrame(getFrame());
            }
        }

        c.setParent(this);
    }

    @Override
    public float getColumnExpandRatio(int col) {
        return component.getColumnExpandRatio(col);
    }

    @Override
    public void setColumnExpandRatio(int col, float ratio) {
        component.setColumnExpandRatio(col, ratio);
    }

    @Override
    public int getFieldCaptionWidth() {
        return component.getFixedCaptionWidth();
    }

    @Override
    public void setFieldCaptionWidth(int fixedCaptionWidth) {
        component.setFixedCaptionWidth(fixedCaptionWidth);
    }

    @Override
    public int getFieldCaptionWidth(int column) {
        return component.getFieldCaptionWidth(column);
    }

    @Override
    public void setFieldCaptionWidth(int column, int width) {
        component.setFieldCaptionWidth(column, width);
    }

    @Override
    public ValueSourceProvider getValueSourceProvider() {
        return valueSourceProvider;
    }

    @Override
    public void setValueSourceProvider(ValueSourceProvider provider) {
        if (this.valueSourceProvider != null) {
            throw new UnsupportedOperationException("Changing value source provider is not supported " +
                    "by the FieldGroup component");
        }

        this.valueSourceProvider = provider;
    }

    @Override
    public void bind() {
        throw new UnsupportedOperationException();
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
        int rowsCount = 0;
        for (List<Component> fields : columnFieldMapping) {
            long boundCount = fields.stream()
                    .filter(component ->
                            !(component instanceof FieldGroupEmptyField))
                    .count();

            rowsCount = (int) Math.max(rowsCount, boundCount);
        }
        return Math.max(rowsCount, 1);
    }

    @Override
    public int getColumns() {
        return component.getColumns();
    }

    @Override
    public void setColumns(int columns) {
        if (component.getColumns() != columns) {
            component.setColumns(columns);

            List<List<Component>> oldColumnFields = this.columnFieldMapping;
            this.columnFieldMapping = new ArrayList<>();
            for (int i = 0; i < columns; i++) {
                if (i < oldColumnFields.size()) {
                    columnFieldMapping.add(oldColumnFields.get(i));
                } else {
                    columnFieldMapping.add(new ArrayList<>());
                }
            }
        }
    }

    @Override
    public String getCaption() {
        if (wrapperAttached) {
            return wrapper.getCaption();
        } else {
            return component.getCaption();
        }
    }

    @Override
    public void setCaption(String caption) {
        if (wrapperAttached) {
            wrapper.setCaption(caption);
        } else {
            component.setCaption(caption);
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
    public boolean isBorderVisible() {
        return wrapper.isBorderVisible();
    }

    @Override
    public void setBorderVisible(boolean borderVisible) {
        wrapper.setBorderVisible(borderVisible);

        if (component.getParent() != null && !wrapperAttached) {
            LoggerFactory.getLogger(WebFieldGroup.class)
                    .warn("Unable to set border visible for FieldGroup after adding to component tree");
            return;
        }

        if (borderVisible && !wrapperAttached) {
            wrapper.setContent(component);

            wrapperAttached = true;
        }
    }

    @Override
    public com.vaadin.ui.Component getComposition() {
        if (wrapperAttached) {
            // wrapper is connected to layout
            return wrapper;
        }

        return super.getComposition();
    }

    @Override
    public boolean isValid() {
        try {
            validate();
            return true;
        } catch (ValidationException e) {
            return false;
        }
    }

    @Override
    public void validate() throws ValidationException {
        if (!isVisibleRecursive() || !isEditableWithParent() || !isEnabledRecursive()) {
            return;
        }

        Map<Validatable, ValidationException> problemFields = null; // lazily initialized

        // validate column by column
        List<FieldConfig> fieldsByColumns = getColumnOrderedFields();

        for (FieldConfig fc : fieldsByColumns) {
            Component fieldComponent = fc.getComponent();

            // If has valid state
            if ((fieldComponent instanceof Validatable) &&
                    (fieldComponent instanceof Editable)) {
                // If editable
                try {
                    ((Validatable) fieldComponent).validate();
                } catch (ValidationException ex) {
                    if (problemFields == null) {
                        problemFields = new LinkedHashMap<>();
                    }
                    problemFields.put((Validatable) fieldComponent, ex);
                }
            }
        }

        if (problemFields != null && !problemFields.isEmpty()) {
            FieldsValidationException validationException = new FieldsValidationException();
            validationException.setProblemFields(problemFields);

            throw validationException;
        }
    }

    @Override
    public void focusFirstField() {
        for (FieldConfig fc : getColumnOrderedFields()) {
            Component component = fc.getComponent();
            if (component != null
                    && component.isEnabledRecursive()
                    && component.isVisibleRecursive()
                    && component instanceof Focusable
                    && ((Focusable) component).isFocusable()) {

                ((Focusable) component).focus();
                break;
            }
        }
    }

    @Override
    public void focusField(String fieldId) {
        FieldConfig field = getFieldNN(fieldId);
        Component componentField = field.getComponentNN();
        ((Focusable) componentField).focus();
    }

    protected List<Component> getColumnOrderedComponents() {
        return columnFieldMapping.stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    /**
     * @return flat list of column fields
     */
    protected List<FieldConfig> getColumnOrderedFields() {
        return wrapComponents(getColumnOrderedComponents());
    }

    protected List<FieldConfig> wrapComponents(Collection<Component> components) {
        return components.stream()
                .filter(component -> getFieldId(component) != null)
                .map(component -> {
                    String fieldId = getFieldId(component);
                    return wrapComponent(fieldId, component);
                })
                .collect(Collectors.toList());
    }

    @Nullable
    protected Component findComponent(String fieldId) {
        List<Component> components = getColumnOrderedComponents();
        for (Component component : components) {
            Object componentFieldId = getFieldId(component);
            if (Objects.equals(fieldId, componentFieldId)) {
                return component;
            }
        }
        return null;
    }

    protected FieldConfig wrapComponent(String fieldId, Component component) {
        boolean custom = component instanceof FieldGroupEmptyField;
        FieldConfigImpl fieldConfig = new FieldConfigImpl(fieldId, custom ? null : component);
        fieldConfig.setCustom(custom);
        return fieldConfig;
    }

    @Nullable
    protected String getFieldId(Component component) {
        if (component instanceof FieldGroupEmptyField) {
            FieldConfig fieldConfig = ((FieldGroupEmptyField) component).getFieldConfig();
            return fieldConfig != null ? fieldConfig.getId() : null;
        }

        AbstractComponent vComponent = component.unwrapComposition(AbstractComponent.class);
        //noinspection unchecked
        return vComponent.getData() instanceof FieldGroupFieldData
                ? ((FieldGroupFieldData) vComponent.getData()).getFieldId()
                : null;
    }

    @Override
    public void assignFieldId(String fieldId, Component component) {
        if (component instanceof FieldGroupEmptyField) {
            return;
        }

        AbstractComponent vComponent = component.unwrapComposition(AbstractComponent.class);
        vComponent.setData(new FieldGroupFieldData().setFieldId(fieldId));
    }

    @Override
    protected String getAlternativeDebugId() {
        if (id != null) {
            return id;
        }
        // TODO: gg, implement
        /*if (datasource != null && StringUtils.isNotEmpty(datasource.getId())) {
            return "fieldGroup_" + datasource.getId();
        }*/

        return getClass().getSimpleName();
    }

    @Override
    public void applyPermission(UiPermissionDescriptor permissionDescriptor) {
        checkNotNullArgument(permissionDescriptor);

        final Logger log = LoggerFactory.getLogger(WebFieldGroup.class);

        final String subComponentId = permissionDescriptor.getSubComponentId();
        final UiPermissionValue permissionValue = permissionDescriptor.getPermissionValue();
        final String screenId = permissionDescriptor.getScreenId();

        if (subComponentId != null) {
            final FieldConfig field = getField(subComponentId);
            if (field != null) {
                if (permissionValue == UiPermissionValue.HIDE) {
                    field.setVisible(false);
                } else if (permissionValue == UiPermissionValue.READ_ONLY) {
                    field.setEditable(false);
                }
            } else {
                log.info("Couldn't find suitable component {} in window {} for UI security rule", subComponentId, screenId);
            }
        } else {
            final String actionHolderComponentId = permissionDescriptor.getActionHolderComponentId();
            FieldConfig fieldConfig = getField(actionHolderComponentId);
            if (fieldConfig == null
                    || fieldConfig.getComponent() == null
                    || !((fieldConfig.getComponent() instanceof SecuredActionsHolder))) {
                log.info("Couldn't find suitable component {} in window {} for UI security rule", actionHolderComponentId, screenId);
                return;
            }

            Component fieldComponent = fieldConfig.getComponent();
            String actionId = permissionDescriptor.getActionId();
            ActionsPermissions permissions = ((SecuredActionsHolder) fieldComponent).getActionsPermissions();
            if (permissionValue == UiPermissionValue.HIDE) {
                permissions.addHiddenActionPermission(actionId);
            } else if (permissionValue == UiPermissionValue.READ_ONLY) {
                permissions.addDisabledActionPermission(actionId);
            }
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

    protected class FieldGroupFieldData {
        protected String fieldId;

        public String getFieldId() {
            return fieldId;
        }

        public FieldGroupFieldData setFieldId(String fieldId) {
            this.fieldId = fieldId;
            return this;
        }
    }

    public class FieldConfigImpl implements FieldConfig {
        protected String id;

        protected Component component;
        protected FieldAttachMode attachMode = FieldAttachMode.APPLY_DEFAULTS;
        private boolean custom;

        public FieldConfigImpl(String id, Component component) {
            // TODO: gg, check for nulls

            this.id = id;
            this.component = component;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public boolean isBound() {
            return component != null;
        }

        public FieldGroup getOwner() {
            return WebFieldGroup.this;
        }

        @Override
        public String getWidth() {
            return ComponentsHelper.getComponentWidth(component);
        }

        @Override
        public void setWidth(String width) {
            component.setWidth(width);
        }

        @Override
        public String getStyleName() {
            return component.getStyleName();
        }

        @Override
        public void setStyleName(String stylename) {
            component.setStyleName(stylename);
        }

        /*protected boolean isWrapped() {
            return component != null && component.unwrapComposition(com.vaadin.ui.Component.class) != composition;
        }*/

        // TODO: gg, remove
        @Override
        public ValueSource getTargetValueSource() {
            /*if (component instanceof HasValueBinding) {
                return ((HasValueBinding) component).getValueSource();
            }*/

            /*if (targetValueSource != null) {
                return targetValueSource;
            }*/

            // TODO: gg, that if the property is null?
            return WebFieldGroup.this.valueSourceProvider.getValueSource(getProperty());
        }

        @Override
        public ValueSource getValueSource() {
            return component instanceof HasValueBinding
                    ? ((HasValueBinding) component).getValueSource()
                    : null;
        }

        @Override
        public void setValueSource(ValueSource valueSource) {
            if (component instanceof HasValueBinding) {
                //noinspection unchecked
                ((HasValueBinding) component).setValueSource(valueSource);
            }
        }

        /*public void setValueSource(ValueSource targetValueSource) {
            if (component instanceof HasValueBinding) {
                ((HasValueBinding) component).setValueSource(targetValueSource);
            }
        }*/

        @Override
        public Boolean isRequired() {
            return component instanceof Field
                    && ((Field) component).isRequired();
        }

        @Override
        public void setRequired(Boolean required) {
            if (component instanceof Field) {
                checkNotNullArgument(required, "Unable to reset required flag for the bound FieldConfig");
                ((Field) component).setRequired(required);
            }
        }

        @Override
        public Boolean isEditable() {
            return component instanceof Editable
                    && ((Field) component).isEditable();
        }

        @Override
        public void setEditable(Boolean editable) {
            if (component instanceof Editable) {
                checkNotNullArgument(editable, "Unable to reset editable flag for the bound FieldConfig");
                ((Editable) component).setEditable(editable);
            }
        }

        @Override
        public Boolean isEnabled() {
            return component.isEnabled();
        }

        @Override
        public void setEnabled(Boolean enabled) {
            checkNotNullArgument(enabled, "Unable to reset enabled flag for the bound FieldConfig");
            component.setEnabled(enabled);
        }

        @Override
        public Boolean isVisible() {
            return component.isVisible();
        }

        @Override
        public void setVisible(Boolean visible) {
            checkNotNullArgument(visible, "Unable to reset visible flag for the bound FieldConfig");
            component.setVisible(visible);
        }

        @Override
        public String getProperty() {
            // TODO: gg, VS?
            if (component instanceof DatasourceComponent) {
                MetaPropertyPath metaPropertyPath = ((DatasourceComponent) component).getMetaPropertyPath();
                return metaPropertyPath != null ? metaPropertyPath.toString() : null;
            }
            return null;
        }

        @Override
        public void setProperty(String property) {
            // TODO: gg, throw new UnsupportedOperationException?
            /*checkState(this.component == null, "Unable to change property for bound FieldConfig");

            this.targetProperty = property;*/
        }

        @Override
        public Integer getTabIndex() {
            return component instanceof Focusable
                    ? ((Focusable) component).getTabIndex()
                    : null;
        }

        @Override
        public void setTabIndex(Integer tabIndex) {
            if (component instanceof Focusable) {
                checkNotNullArgument(tabIndex, "Unable to reset tabIndex for the bound FieldConfig");
                ((Focusable) component).setTabIndex(tabIndex);
            }
        }

        @Override
        public String getRequiredError() {
            return getRequiredMessage();
        }

        @Override
        public void setRequiredError(String requiredError) {
            setRequiredMessage(requiredError);
        }

        @Override
        public boolean isCustom() {
            return custom;
        }

        @Override
        public void setCustom(boolean custom) {
            this.custom = custom;
        }

        @Override
        public String getRequiredMessage() {
            return component instanceof Field
                    ? ((Field) component).getRequiredMessage()
                    : null;
        }

        @Override
        public void setRequiredMessage(String requiredMessage) {
            if (component instanceof Field) {
                ((Field) component).setRequiredMessage(requiredMessage);
            }
        }

        @Nullable
        @Override
        public Component getComponent() {
            return component;
        }

        @Override
        public Component getComponentNN() {
            if (component == null) {
                throw new IllegalStateException("FieldConfig is not bound to a Component");
            }
            return component;
        }

        @Override
        public void setComponent(Component component) {
            setComponent(component, FieldAttachMode.APPLY_DEFAULTS);
        }

        @Override
        public void setComponent(Component component, FieldAttachMode mode) {
            checkState(this.component == null, "Unable to change component for bound FieldConfig");

            this.attachMode = mode;
            this.component = component;

            if (component != null) {
                // TODO: gg, fix use etc
//                managedFieldComponentAssigned(component, mode);

            }
        }

        @Override
        public void addValidator(Consumer validator) {
            if (component instanceof Field) {
                //noinspection unchecked
                ((Field) component).addValidator(validator);
            }
        }

        @Override
        public void removeValidator(Consumer validator) {
            if (component instanceof Field) {
                //noinspection unchecked
                ((Field) component).removeValidator(validator);
            }
        }

        @Override
        public List<Consumer> getValidators() {
            //noinspection unchecked
            return component instanceof Field
                    ? new ArrayList<>(((Field) component).getValidators())
                    : Collections.emptyList();
        }

        // TODO: gg, add OptionsSource
        @Override
        public void setOptionsDatasource(CollectionDatasource optionsDatasource) {
            if (component instanceof OptionsField) {
                ((OptionsField) component).setOptionsDatasource(optionsDatasource);
            }
        }

        @Override
        public CollectionDatasource getOptionsDatasource() {
            if (component instanceof OptionsField) {
                return ((OptionsField) component).getOptionsDatasource();
            } else {
                return null;
            }
        }

        @Override
        public String getCaption() {
            return component instanceof HasCaption
                    ? ((HasCaption) component).getCaption()
                    : null;
        }

        @Override
        public void setCaption(String caption) {
            if (component instanceof HasCaption) {
                ((HasCaption) component).setCaption(caption);
            }
        }

        @Override
        public String getDescription() {
            return component instanceof HasCaption
                    ? ((HasCaption) component).getDescription()
                    : null;
        }

        @Override
        public void setDescription(String description) {
            if (component instanceof HasCaption) {
                ((HasCaption) component).setDescription(description);
            }
        }

        @Override
        public String getInputPrompt() {
            return component instanceof HasInputPrompt
                    ? ((HasInputPrompt) component).getInputPrompt()
                    : null;
        }

        @Override
        public void setInputPrompt(String inputPrompt) {
            if (component instanceof HasInputPrompt) {
                ((HasInputPrompt) component).setInputPrompt(inputPrompt);
            }
        }

        @Override
        public String getContextHelpText() {
            return component instanceof HasContextHelp
                    ? ((HasContextHelp) component).getContextHelpText()
                    : null;
        }

        @Override
        public void setContextHelpText(String contextHelpText) {
            if (component instanceof HasContextHelp) {
                ((HasContextHelp) component).setContextHelpText(contextHelpText);
            }
        }

        @Override
        public Boolean isContextHelpTextHtmlEnabled() {
            return component instanceof HasContextHelp
                    && ((HasContextHelp) component).isContextHelpTextHtmlEnabled();
        }

        @Override
        public void setContextHelpTextHtmlEnabled(Boolean enabled) {
            if (component instanceof HasContextHelp) {
                checkNotNullArgument(enabled, "Unable to reset contextHelpTextHtmlEnabled " +
                        "flag for the bound FieldConfig");
                ((HasContextHelp) component).setContextHelpTextHtmlEnabled(enabled);
            }
        }

        @Override
        public Consumer<HasContextHelp.ContextHelpIconClickEvent> getContextHelpIconClickHandler() {
            return component instanceof HasContextHelp
                    ? ((HasContextHelp) component).getContextHelpIconClickHandler()
                    : null;
        }

        @Override
        public void setContextHelpIconClickHandler(Consumer<HasContextHelp.ContextHelpIconClickEvent> handler) {
            if (component instanceof HasContextHelp) {
                ((HasContextHelp) component).setContextHelpIconClickHandler(handler);
            }
        }

        @Override
        public Function getFormatter() {
            return component instanceof HasFormatter
                    ? ((HasFormatter) component).getFormatter()
                    : null;
        }

        @Override
        public void setFormatter(Function formatter) {
            if (component instanceof HasFormatter) {
                //noinspection unchecked
                ((HasFormatter) component).setFormatter(formatter);
            }
        }

        @Override
        public Element getXmlDescriptor() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setXmlDescriptor(Element element) {
            throw new UnsupportedOperationException();
        }

        @Override
        public FieldAttachMode getAttachMode() {
            return attachMode;
        }

        @Override
        public String toString() {
            return "FieldConfig: " + id;
        }
    }
}