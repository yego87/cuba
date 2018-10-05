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
package com.haulmont.cuba.gui.components;

import com.haulmont.cuba.gui.components.data.HasValueSourceProvider;
import com.haulmont.cuba.gui.components.data.ValueSourceProvider;
import com.haulmont.cuba.gui.components.data.value.DatasourceValueSourceProvider;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.xml.layout.loaders.FieldGroupLoader.FieldConfig;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Multi-column form component.
 */
public interface FieldGroup extends Component, Component.BelongToFrame, Component.HasCaption, Component.HasIcon,
        HasBorder, HasContextHelp, Component.Editable, Validatable,
        EditableChangeNotifier, ChildEditableController,
        ComponentContainer, HasSubParts, HasValueSourceProvider {
    String NAME = "fieldGroup";

    /**
     * Create new field config.
     * Field will not be automatically added to layout use additional {@link #addField(FieldConfig)} call.
     *
     * @param id field id
     * @return field config
     */
    @Deprecated
    FieldConfig createField(String id);

    /**
     * @return all added field configs
     */
    @Deprecated
    List<FieldConfig> getFields();

    /**
     * @param column column index
     * @return added field configs of {@code column}
     */
    @Deprecated
    List<FieldConfig> getFields(int column);

    /**
     * @param column column index
     * @param row    row index
     * @return field config
     */
    @Deprecated
    FieldConfig getField(int column, int row);

    /**
     * Get field config by id.
     *
     * @param fieldId field id
     * @return field config or null
     */
    @Deprecated
    @Nullable
    FieldConfig getField(String fieldId);

    /**
     * Get field config by id.
     *
     * @param fieldId field id
     * @return field config. Throws exception if not found.
     */
    @Deprecated
    FieldConfig getFieldNN(String fieldId);

    /**
     * Append field to 1 column.
     *
     * @param field field config
     */
    @Deprecated
    void addField(FieldConfig field);

    /**
     * Append field to {@code colIndex} column.
     *
     * @param fieldConfig field config
     * @param colIndex    column index
     */
    @Deprecated
    void addField(FieldConfig fieldConfig, int colIndex);

    /**
     * Insert field to {@code colIndex} column to {@code rowIndex} position.
     *
     * @param fieldConfig field config
     * @param colIndex    column index
     * @param rowIndex    row index
     */
    @Deprecated
    void addField(FieldConfig fieldConfig, int colIndex, int rowIndex);

    /**
     * Remove field by id.
     *
     * @param fieldId field id
     */
    void removeField(String fieldId);

    /**
     * Remove field associated with {@code fieldConfig}.
     *
     * @param fieldConfig field id
     */
    @Deprecated
    void removeField(FieldConfig fieldConfig);

    void setComponent(String id, Component field);

    void add(Component field, int colIndex);

    void add(Component field, int colIndex, int rowIndex);

    /**
     * Request focus on field. <br>
     * Throws exception if field is not found or field does not have component.
     *
     * @param fieldId field id
     * @deprecated Use {@link #focusField(String)} instead.
     */
    @Deprecated
    default void requestFocus(String fieldId) {
        focusField(fieldId);
    }

    /**
     * @deprecated Use {@link #focusFirstField()} instead.
     */
    @Deprecated
    default void requestFocus() {
        focusFirstField();
    }

    /**
     * Focus the first enabled, visible and editable field.
     */
    void focusFirstField();

    /**
     * Request focus on field. <br>
     * Throws exception if field is not found or field does not have component.
     *
     * @param fieldId field id
     */
    void focusField(String fieldId);

    /**
     * @return default datasource for declarative fields
     * @deprecated Use {@link #getValueSourceProvider()} instead
     */
    @Deprecated
    default Datasource getDatasource() {
        ValueSourceProvider provider = getValueSourceProvider();
        return provider instanceof DatasourceValueSourceProvider
                ? ((DatasourceValueSourceProvider) provider).getDatasource()
                : null;
    }

    /**
     * Set default datasource for declarative fields.
     *
     * @param datasource datasource
     * @deprecated Use {@link #setValueSourceProvider(ValueSourceProvider)} instead
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    default void setDatasource(Datasource datasource) {
        setValueSourceProvider(datasource != null
                ? new DatasourceValueSourceProvider(datasource)
                : null);
    }

    /**
     * Create and bind components for all declarative fields.
     */
    // TODO: gg, remove?
    @Deprecated
    void bind();

    /**
     * @return attached field components
     */
    List<Component> getOwnComponents();

    /**
     * @return alignment of field captions
     */
    FieldCaptionAlignment getCaptionAlignment();

    /**
     * Set alignment of field captions
     *
     * @param captionAlignment field captions alignment
     */
    void setCaptionAlignment(FieldCaptionAlignment captionAlignment);

    /**
     * @return fixed field caption width
     */
    int getFieldCaptionWidth();

    /**
     * Set fixed field captions width. Set -1 to use auto size.
     *
     * @param fixedCaptionWidth fixed field caption width
     */
    void setFieldCaptionWidth(int fixedCaptionWidth);

    /**
     * @param colIndex column index
     * @return fixed field caption width for column {@code colIndex}
     */
    int getFieldCaptionWidth(int colIndex);

    /**
     * Set fixed field captions width for column {@code colIndex}. Set -1 to use auto size.
     *
     * @param colIndex column index
     * @param width    width
     */
    void setFieldCaptionWidth(int colIndex, int width);

    /**
     * @return column count
     */
    int getColumns();

    /**
     * Set column count.
     *
     * @param columns column count
     */
    void setColumns(int columns);

    /**
     * @param colIndex column index
     * @return column expand ratio
     */
    float getColumnExpandRatio(int colIndex);

    /**
     * Set column expand ratio.
     *
     * @param colIndex column index
     * @param ratio    column expand ratio
     */
    void setColumnExpandRatio(int colIndex, float ratio);

    boolean isValid();

    void validate() throws ValidationException;

    /**
     * For internal use only
     */
    @Deprecated
    void assignFieldId(String fieldId, Component component);

    /**
     * Field caption alignment.
     */
    enum FieldCaptionAlignment {
        LEFT,
        TOP
    }

    /**
     * Whether apply declarative defaults for custom field or not.
     */
    enum FieldAttachMode {
        APPLY_DEFAULTS,
        CUSTOM
    }

    /**
     * Exception that is thrown from {@link #validate()}.
     * Contains validation exceptions for fields that have failed validation.
     */
    class FieldsValidationException extends ValidationException {
        private Map<Validatable, ValidationException> problemFields;

        public FieldsValidationException() {
        }

        public FieldsValidationException(String message) {
            super(message);
        }

        public FieldsValidationException(String message, Throwable cause) {
            super(message, cause);
        }

        public FieldsValidationException(Throwable cause) {
            super(cause);
        }

        public Map<Validatable, ValidationException> getProblemFields() {
            return problemFields;
        }

        public void setProblemFields(Map<Validatable, ValidationException> problemFields) {
            this.problemFields = problemFields;
        }
    }

    @Override
    default boolean isValidateOnCommit() {
        return false;
    }

    @Nullable
    @Override
    default Object getSubPart(String name) {
        return getField(name);
    }

    /*
     * Deprecated API
     */

    /**
     * Allows to show an arbitrary field inside a {@link FieldGroup}. Implementors of this interface have to be passed
     * to one of <code>FieldGroup.addCustomField</code> methods.
     *
     * @deprecated Set Component implementation directly to {@link FieldConfig} using {@link FieldConfig#setComponent(Component)} method.
     */
    @Deprecated
    interface CustomFieldGenerator {
        /**
         * Called by the {@link FieldGroup} to get a generated field instance.
         *
         * @param datasource a datasource specified for the field or the whole FieldGroup in XML
         * @param propertyId field identifier as defined in XML, with <code>custom</code> attribute set to true
         * @return a component to be rendered for the field
         */
        Component generateField(Datasource datasource, String propertyId);
    }
}