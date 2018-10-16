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

package com.haulmont.cuba.gui.components.filter;

import com.google.common.collect.Lists;
import com.haulmont.chile.core.datatypes.Datatype;
import com.haulmont.chile.core.datatypes.DatatypeRegistry;
import com.haulmont.chile.core.datatypes.Datatypes;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.cuba.client.ClientConfig;
import com.haulmont.cuba.core.app.PersistenceManagerService;
import com.haulmont.cuba.core.app.dynamicattributes.DynamicAttributesUtils;
import com.haulmont.cuba.core.app.dynamicattributes.PropertyType;
import com.haulmont.cuba.core.entity.CategoryAttribute;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.annotation.IgnoreUserTimeZone;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.WindowManagerProvider;
import com.haulmont.cuba.gui.actions.picker.ClearAction;
import com.haulmont.cuba.gui.actions.picker.LookupAction;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.data.options.ContainerOptions;
import com.haulmont.cuba.gui.components.data.options.MapOptions;
import com.haulmont.cuba.gui.components.filter.dateinterval.DateInIntervalComponent;
import com.haulmont.cuba.gui.components.listeditor.ListEditorHelper;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.model.DataContextFactory;
import com.haulmont.cuba.gui.theme.ThemeConstants;
import com.haulmont.cuba.gui.theme.ThemeConstantsManager;
import com.haulmont.cuba.security.global.UserSession;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.TextStringBuilder;
import org.dom4j.Element;
import org.springframework.context.annotation.Scope;

import javax.annotation.Nullable;
import javax.persistence.TemporalType;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;

@org.springframework.stereotype.Component(Param.NAME)
@Scope("prototype")
public class Param {

    public enum Type {
        ENTITY,
        ENUM,
        RUNTIME_ENUM,
        DATATYPE,
        UNARY
    }

    public enum ValueProperty {
        VALUE,
        DEFAULT_VALUE
    }

    public static final String NAME = "cuba_FilterParam";
    public static final String NULL = "NULL";

    protected static final List<Class> dateTimeClasses = Lists.newArrayList(LocalDate.class, LocalDateTime.class,
            OffsetDateTime.class);

    protected String name;
    protected Type type;
    protected Class javaClass;
    protected Object value;
    protected Object defaultValue;
    protected boolean useUserTimeZone;
    protected String entityQuery;
    protected String entityView;
    protected MetaProperty property;
    protected boolean inExpr;
    protected boolean required;
    protected boolean isDateInterval;
    protected List<String> runtimeEnum;
    protected UUID categoryAttrId;
    protected Component editComponent;
    protected boolean isFoldersFilterEntitiesSet = false;

    protected Messages messages = AppBeans.get(Messages.class);
    protected UserSessionSource userSessionSource = AppBeans.get(UserSessionSource.class);
    protected Metadata metadata = AppBeans.get(Metadata.class);
    protected UiComponents uiComponents = AppBeans.get(UiComponents.class);
    protected Actions actions = AppBeans.get(Actions.class);
    protected MetadataTools metadataTools = AppBeans.get(MetadataTools.class);
    protected DatatypeRegistry datatypeRegistry = AppBeans.get(DatatypeRegistry.class);
    protected ThemeConstants theme = AppBeans.get(ThemeConstantsManager.class).getConstants();
    protected DataManager dataManager = AppBeans.get(DataManager.class);
    protected DataContextFactory dataContextFactory = AppBeans.get(DataContextFactory.class);

    protected List<ParamValueChangeListener> listeners = new ArrayList<>();

    public static class Builder {
        private String name;
        private Class javaClass;
        private String entityQuery;
        private String entityView;
        private MetaClass metaClass;
        private MetaProperty property;
        private boolean inExpr;
        private boolean required;
        private UUID categoryAttrId;
        private boolean isDateInterval;
        private boolean useUserTimeZone;

        private Builder() {
        }

        public static Builder getInstance() {
            return new Builder();
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setJavaClass(Class javaClass) {
            this.javaClass = javaClass;
            return this;
        }

        public Builder setEntityQuery(String entityQuery) {
            this.entityQuery = entityQuery;
            return this;
        }

        public Builder setEntityView(String entityView) {
            this.entityView = entityView;
            return this;
        }

        public Builder setMetaClass(MetaClass metaClass) {
            this.metaClass = metaClass;
            return this;
        }

        public Builder setProperty(MetaProperty property) {
            this.property = property;
            return this;
        }

        public Builder setInExpr(boolean inExpr) {
            this.inExpr = inExpr;
            return this;
        }

        public Builder setRequired(boolean required) {
            this.required = required;
            return this;
        }

        public Builder setCategoryAttrId(UUID categoryAttrId) {
            this.categoryAttrId = categoryAttrId;
            return this;
        }

        public Builder setUseUserTimeZone(boolean useUserTimeZone) {
            this.useUserTimeZone = useUserTimeZone;
            return this;
        }

        public Param build() {
            return AppBeans.getPrototype(Param.NAME, this);
        }

        public void setIsDateInterval(boolean isDateInterval) {
            this.isDateInterval = isDateInterval;
        }
    }

    public Param(Builder builder) {
        name = builder.name;
        setJavaClass(builder.javaClass);
        entityQuery = builder.entityQuery;
        entityView = (builder.entityView != null) ? builder.entityView : View.MINIMAL;
        property = builder.property;
        inExpr = builder.inExpr;
        required = builder.required;
        categoryAttrId = builder.categoryAttrId;
        isDateInterval = builder.isDateInterval;
        useUserTimeZone = builder.useUserTimeZone;

        if (DynamicAttributesUtils.isDynamicAttribute(builder.property)) {
            CategoryAttribute categoryAttribute = DynamicAttributesUtils.getCategoryAttribute(builder.property);
            if (categoryAttribute.getDataType() == PropertyType.ENUMERATION) {
                type = Type.RUNTIME_ENUM;
            }
        }
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public void setJavaClass(Class javaClass) {
        if (javaClass != null) {
            this.javaClass = javaClass;
            if (Entity.class.isAssignableFrom(javaClass)) {
                type = Type.ENTITY;
            } else if (Enum.class.isAssignableFrom(javaClass)) {
                type = Type.ENUM;
            } else {
                type = Type.DATATYPE;
            }
        } else {
            type = Type.UNARY;
            this.javaClass = Boolean.class;
        }
    }

    public boolean isDateInterval() {
        return isDateInterval;
    }

    public void setDateInterval(boolean dateInterval) {
        isDateInterval = dateInterval;
    }

    public boolean isInExpr() {
        return inExpr;
    }

    public void setInExpr(boolean inExpr) {
        this.inExpr = inExpr;
    }

    /**
     * @return true if param is used for folder's filter entities set
     */
    public boolean isFoldersFilterEntitiesSet() {
        return isFoldersFilterEntitiesSet;
    }

    /**
     * Set true if param should be used for folder's filter entities set
     *
     * @param isFoldersFilterEntitiesSet filter entities set value
     */
    public void setFoldersFilterEntitiesSet(boolean isFoldersFilterEntitiesSet) {
        this.isFoldersFilterEntitiesSet = isFoldersFilterEntitiesSet;
    }

    public boolean isUseUserTimeZone() {
        return useUserTimeZone;
    }

    public void setUseUserTimeZone(boolean useUserTimeZone) {
        this.useUserTimeZone = useUserTimeZone;
    }

    public Class getJavaClass() {
        return javaClass;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        setValue(value, true);
    }

    protected void setValue(Object value, boolean updateEditComponent) {
        if (!Objects.equals(value, this.value)) {
            Object prevValue = this.value;
            this.value = value;
            for (ParamValueChangeListener listener : new ArrayList<>(listeners)) {
                listener.valueChanged(prevValue, value);
            }
            if (updateEditComponent && this.editComponent instanceof HasValue) {
                if (value instanceof Collection && editComponent instanceof TextField) {
                    //if the value type is an array and the editComponent is a textField ('IN' condition for String attribute)
                    //then we should set the string value (not the array) to the text field
                    String caption = new TextStringBuilder().appendWithSeparators((Collection) value, ",").toString();
                    ((TextField) editComponent).setValue(caption);
                } else {
                    ((HasValue) editComponent).setValue(value);
                }
            }
        }
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
        if (value == null) {
            setValue(defaultValue, true);
        }
    }

    @SuppressWarnings("unchecked")
    public void parseValue(String text) {
        if (NULL.equals(text)) {
            value = null;
            return;
        }

        if (inExpr) {
            if (StringUtils.isBlank(text)) {
                value = new ArrayList<>();
            } else {
                String[] parts = text.split(",");
                List list = new ArrayList(parts.length);
                if (type == Type.ENTITY) {
                    list = loadEntityList(parts);
                } else {
                    for (String part : parts) {
                        Object value = parseSingleValue(part);
                        if (value != null) {
                            list.add(value);
                        }
                    }
                }

                if (isFoldersFilterEntitiesSet) {
                    value = list;
                } else {
                    value = list.isEmpty() ? null : list;
                }
            }
        } else {
            value = parseSingleValue(text);
        }
    }

    protected Object parseSingleValue(String text) {
        Object value;
        switch (type) {
            case ENTITY:
                value = loadEntity(text);
                break;

            case ENUM:
                value = Enum.valueOf(javaClass, text);
                break;

            case RUNTIME_ENUM:
            case DATATYPE:
            case UNARY:
                Datatype datatype = datatypeRegistry.getNN(javaClass);
                // hard code for compatibility with old datatypes
                if (datatype.getJavaClass().equals(Date.class)) {
                    try {
                        value = datatype.parse(text);
                    } catch (ParseException e) {
                        try {
                            value = new SimpleDateFormat("dd/MM/yyyy HH:mm").parse(text);
                        } catch (ParseException exception) {
                            throw new RuntimeException("Can not parse date from string " + text, e);
                        }
                    }
                } else {
                    try {
                        value = datatype.parse(text);
                    } catch (ParseException e) {
                        throw new RuntimeException("Parse exception for string " + text, e);
                    }
                }
                break;

            default:
                throw new IllegalStateException("Param type unknown");
        }
        return value;
    }

    protected List<Entity> loadEntityList(String[] ids) {
        MetaClass metaClass = metadata.getSession().getClassNN(javaClass);
        LoadContext ctx = new LoadContext(javaClass);
        LoadContext.Query query = ctx.setQueryString("select e from " + metaClass.getName() + " e where e.id in :ids");
        query.setParameter("ids", Arrays.asList(ids));
        return dataManager.loadList(ctx);
    }

    protected Object loadEntity(String id) {
        MetaProperty idProperty = metadata.getTools().getPrimaryKeyProperty(metadata.getClassNN(javaClass));
        Object objectId = null;
        if (idProperty != null) {
            Datatype<Object> datatype = idProperty.getRange().asDatatype();
            try {
                objectId = datatype.parse(id);
            } catch (ParseException e) {
                throw new RuntimeException("Error parsing entity ID", e);
            }
        }
        //noinspection unchecked
        return dataManager.load(javaClass).id(objectId).optional().orElse(null);
    }

    public String formatValue(Object value) {
        if (value == null)
            return NULL;

        if (value instanceof Collection) {
            StringBuilder sb = new StringBuilder();
            for (Iterator iterator = ((Collection) value).iterator(); iterator.hasNext(); ) {
                Object v = iterator.next();
                sb.append(formatSingleValue(v));
                if (iterator.hasNext())
                    sb.append(",");
            }
            return sb.toString();
        } else {
            return formatSingleValue(value);
        }
    }

    protected String formatSingleValue(Object v) {
        switch (type) {
            case ENTITY:
                if (v instanceof UUID)
                    return v.toString();
                else if (v instanceof Entity)
                    return ((Entity) v).getId().toString();

            case ENUM:
                return ((Enum) v).name();
            case RUNTIME_ENUM:
            case DATATYPE:
            case UNARY:
                if (isDateInterval) return (String) v;
                //noinspection unchecked
                return datatypeRegistry.getNN(javaClass).format(v);
            default:
                throw new IllegalStateException("Param type unknown");
        }
    }

    /**
     * Creates an GUI component for condition parameter.
     *
     * @param valueProperty What value the editor will be connected with: current filter value or default one.
     * @return GUI component for condition parameter.
     */
    public Component createEditComponent(ValueProperty valueProperty) {
        Component component;

        switch (type) {
            case DATATYPE:
                if (isDateInterval) {
                    DateInIntervalComponent dateInIntervalComponent = AppBeans.get(DateInIntervalComponent.class);
                    dateInIntervalComponent.addValueChangeListener(newValue -> {
                        _setValue(newValue == null ? null : newValue.getDescription(), valueProperty);
                    });
                    component = dateInIntervalComponent.createComponent((String) _getValue(valueProperty));
                } else {
                    component = createDatatypeField(Datatypes.getNN(javaClass), valueProperty);
                }
                break;
            case ENTITY:
                component = createEntityLookup(valueProperty);
                break;
            case UNARY:
                component = createUnaryField(valueProperty);
                break;
            case ENUM:
                component = createEnumLookup(valueProperty);
                break;
            case RUNTIME_ENUM:
                component = createRuntimeEnumLookup(valueProperty);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported param type: " + type);
        }

        this.editComponent = component;

        return component;
    }

    protected CheckBox createUnaryField(ValueProperty valueProperty) {
        CheckBox field = uiComponents.create(CheckBox.class);
        field.addValueChangeListener(e -> _setValue(Boolean.TRUE.equals(e.getValue()), valueProperty));
        field.setValue((Boolean) _getValue(valueProperty));
        field.setAlignment(Component.Alignment.MIDDLE_LEFT);
        return field;
    }

    protected Component createDatatypeField(Datatype datatype, ValueProperty valueProperty) {
        Component component;

        if (String.class.equals(javaClass)) {
            component = createTextField(valueProperty);
        } else if (Date.class.isAssignableFrom(javaClass) || dateTimeClasses.contains(javaClass)) {
            component = createDateField(javaClass, valueProperty);
        } else if (Number.class.isAssignableFrom(javaClass)) {
            component = createNumberField(datatype, valueProperty);
        } else if (Boolean.class.isAssignableFrom(javaClass)) {
            component = createBooleanField(valueProperty);
        } else if (UUID.class.equals(javaClass)) {
            component = createUuidField(valueProperty);
        } else
            throw new UnsupportedOperationException("Unsupported param class: " + javaClass);

        return component;
    }

    protected void _setValue(Object value, ValueProperty valueProperty) {
        switch (valueProperty) {
            case VALUE:
                setValue(value, false);
                break;
            case DEFAULT_VALUE:
                setDefaultValue(value);
                break;
            default:
                throw new IllegalArgumentException("Value property " + valueProperty + " not supported");
        }
    }

    protected Object _getValue(ValueProperty valueProperty) {
        switch (valueProperty) {
            case VALUE:
                return value;
            case DEFAULT_VALUE:
                return defaultValue;
            default:
                throw new IllegalArgumentException("Value property " + valueProperty + " not supported");
        }
    }


    protected Component createTextField(final ValueProperty valueProperty) {
        if (inExpr) {
            ListEditor<Object> listEditor = uiComponents.create(ListEditor.NAME);
            listEditor.setItemType(ListEditor.ItemType.STRING);
            initListEditor(listEditor, valueProperty);
            return listEditor;
        }

        TextField<String> field = uiComponents.create(TextField.TYPE_STRING);
        field.setWidth(theme.get("cuba.gui.filter.Param.textComponent.width"));

        field.addValueChangeListener(e -> {
            String paramValue = null;
            if (StringUtils.isNotBlank(e.getValue())) {
                paramValue = e.getValue();
            }
            if (paramValue != null) {
                Configuration configuration = AppBeans.get(Configuration.NAME);
                if (configuration.getConfig(ClientConfig.class).getGenericFilterTrimParamValues()) {
                    _setValue(StringUtils.trimToNull(paramValue), valueProperty);
                } else {
                    _setValue(paramValue, valueProperty);
                }
            } else {
                _setValue(null, valueProperty);
            }
        });

        Object _value = valueProperty == ValueProperty.DEFAULT_VALUE ? defaultValue : value;

        if (_value instanceof List) {
            field.setValue(StringUtils.join((Collection) _value, ","));
        } else if (_value instanceof String) {
            field.setValue((String) _value);
        } else {
            field.setValue("");
        }

        return field;
    }

    protected Component createDateField(Class javaClass, final ValueProperty valueProperty) {
        UserSession userSession = userSessionSource.getUserSession();
        boolean supportTimezones = false;
        boolean dateOnly = false;
        if (property != null) {
            TemporalType tt = (TemporalType) property.getAnnotations().get(MetadataTools.TEMPORAL_ANN_NAME);
            dateOnly = tt == TemporalType.DATE || LocalDate.class.equals(javaClass);
            Object ignoreUserTimeZone = metadataTools.getMetaAnnotationValue(property, IgnoreUserTimeZone.class);
            supportTimezones = !dateOnly && !Boolean.TRUE.equals(ignoreUserTimeZone);
        } else if (LocalDate.class.equals(javaClass)) {
            dateOnly = true;
        } else if (java.sql.Date.class.equals(javaClass)) {
            dateOnly = true;
            if (useUserTimeZone) {
                supportTimezones = true;
            }
        } else {
            supportTimezones = true;
        }
        if (inExpr) {
            ListEditor<Object> listEditor = uiComponents.create(ListEditor.NAME);
            ListEditor.ItemType itemType = dateOnly ? ListEditor.ItemType.DATE : ListEditor.ItemType.DATETIME;
            listEditor.setItemType(itemType);
            if (userSession.getTimeZone() != null && supportTimezones) {
                listEditor.setTimeZone(userSession.getTimeZone());
            }
            initListEditor(listEditor, valueProperty);
            return listEditor;
        }


        DateField<Object> dateField = uiComponents.create(DateField.NAME);
        //noinspection unchecked
        dateField.setDatatype(datatypeRegistry.get(javaClass));

        DateField.Resolution resolution;
        String formatStr;
        Messages messages = AppBeans.get(Messages.NAME);
        if (dateOnly) {
            resolution = com.haulmont.cuba.gui.components.DateField.Resolution.DAY;
            formatStr = messages.getMainMessage("dateFormat");
        } else {
            resolution = com.haulmont.cuba.gui.components.DateField.Resolution.MIN;
            formatStr = messages.getMainMessage("dateTimeFormat");
        }
        dateField.setResolution(resolution);
        dateField.setDateFormat(formatStr);
        if (userSession.getTimeZone() != null && supportTimezones) {
            dateField.setTimeZone(userSession.getTimeZone());
        }

        dateField.addValueChangeListener(e -> _setValue(e.getValue(), valueProperty));
        dateField.setValue(_getValue(valueProperty));

        return dateField;
    }

    protected Component createNumberField(final Datatype datatype, final ValueProperty valueProperty) {
        if (inExpr) {
            ListEditor listEditor = uiComponents.create(ListEditor.class);
            listEditor.setItemType(ListEditorHelper.itemTypeFromDatatype(datatype));
            //noinspection unchecked
            initListEditor(listEditor, valueProperty);
            return listEditor;
        }
        TextField<String> field = uiComponents.create(TextField.TYPE_STRING);

        field.addValueChangeListener(e -> {
            if (e.getValue() == null) {
                _setValue(e.getValue(), valueProperty);
            } else if (!StringUtils.isBlank(e.getValue())) {
                Object v;
                try {
                    v = datatype.parse(e.getValue(), userSessionSource.getLocale());
                } catch (ParseException ex) {
                    WindowManager wm = AppBeans.get(WindowManagerProvider.class).get();
                    wm.showNotification(messages.getMainMessage("filter.param.numberInvalid"), Frame.NotificationType.TRAY);
                    return;
                }
                _setValue(v, valueProperty);
            } else if (StringUtils.isBlank(e.getValue())) {
                _setValue(null, valueProperty);
            } else {
                throw new IllegalStateException("Invalid value: " + e.getValue());
            }
        });
        //noinspection unchecked
        field.setValue(datatype.format(_getValue(valueProperty), userSessionSource.getLocale()));
        return field;
    }

    protected Component createBooleanField(ValueProperty valueProperty) {
        LookupField<Object> field = uiComponents.create(LookupField.of(Object.class));
        field.setWidth(theme.get("cuba.gui.filter.Param.booleanLookup.width"));

        Map<String, Object> values = new HashMap<>();
        values.put(messages.getMainMessage("filter.param.boolean.true"), Boolean.TRUE);
        values.put(messages.getMainMessage("filter.param.boolean.false"), Boolean.FALSE);

        field.setOptions(new MapOptions<>(values));
        field.addValueChangeListener(e -> _setValue(e.getValue(), valueProperty));
        field.setValue(_getValue(valueProperty));
        return field;
    }

    protected Component createUuidField(ValueProperty valueProperty) {
        if (inExpr) {
            ListEditor<Object> listEditor = uiComponents.create(ListEditor.NAME);
            listEditor.setItemType(ListEditor.ItemType.UUID);
            initListEditor(listEditor, valueProperty);
            return listEditor;
        }

        TextField<String> field = uiComponents.create(TextField.TYPE_STRING);

        field.addValueChangeListener(e -> {
            String strValue = e.getValue();
            if (strValue == null) {
                _setValue(null, valueProperty);
            } else if ((!StringUtils.isBlank(strValue))) {
                Messages messages1 = AppBeans.get(Messages.NAME);

                try {
                    _setValue(UUID.fromString(strValue), valueProperty);
                } catch (IllegalArgumentException ie) {
                    AppBeans.get(WindowManagerProvider.class).get().showNotification(messages1.getMainMessage("filter.param.uuid.Err"), Frame.NotificationType.TRAY);
                }
            } else if (StringUtils.isBlank(strValue)) {
                _setValue(null, valueProperty);
            } else {
                throw new IllegalStateException("Invalid value: " + strValue);
            }
        });

        Object _value = _getValue(valueProperty);
        if (_value instanceof String) {
            field.setValue((String) _value);
        } else {
            field.setValue("");
        }
        return field;
    }


    protected Component createEntityLookup(final ValueProperty valueProperty) {
        Metadata metadata = AppBeans.get(Metadata.NAME);
        MetaClass metaClass = metadata.getSession().getClassNN(javaClass);

        ThemeConstants theme = AppBeans.get(ThemeConstantsManager.class).getConstants();
        PersistenceManagerService persistenceManager = AppBeans.get(PersistenceManagerService.NAME);

        LookupType type = null;
        if (property != null && property.getRange().isClass()) {
            type = (LookupType) metadata.getTools()
                    .getMetaAnnotationAttributes(property.getAnnotations(), Lookup.class)
                    .get("type");
        }
        boolean useLookupScreen = type != null ?
                type == LookupType.SCREEN : persistenceManager.useLookupScreen(metaClass.getName());

        if (useLookupScreen) {
            if (inExpr) {
                ListEditor<Object> listEditor = uiComponents.create(ListEditor.NAME);
                listEditor.setItemType(ListEditor.ItemType.ENTITY);
                listEditor.setEntityName(metaClass.getName());
                initListEditor(listEditor, valueProperty);
                return listEditor;
            } else {
                PickerField<Entity> picker = uiComponents.create(PickerField.NAME);
                picker.setMetaClass(metaClass);
                picker.setWidth(theme.get("cuba.gui.filter.Param.textComponent.width"));
                // TODO filter ds
//                picker.setFrame(datasource.getDsContext().getFrameContext().getFrame());
                picker.addAction(actions.create(LookupAction.class));
                picker.addAction(actions.create(ClearAction.class));

                picker.addValueChangeListener(e ->
                        _setValue(e.getValue(), valueProperty));
                picker.setValue((Entity) _getValue(valueProperty));

                return picker;
            }
        } else {
            if (inExpr) {
                ListEditor<Object> listEditor = uiComponents.create(ListEditor.NAME);
                listEditor.setItemType(ListEditor.ItemType.ENTITY);
                listEditor.setEntityName(metaClass.getName());
                listEditor.setUseLookupField(true);
                listEditor.setEntityQuery(entityQuery);
                initListEditor(listEditor, valueProperty);
                return listEditor;
            } else {
                CollectionContainer<Entity> container = createEntityOptions(metaClass);
                LookupPickerField<Entity> lookup = uiComponents.create(LookupPickerField.NAME);
                lookup.addAction(actions.create(ClearAction.class));
                lookup.setWidth(theme.get("cuba.gui.filter.Param.textComponent.width"));

                //noinspection unchecked
                lookup.setOptions(new ContainerOptions<>(container));

                container.addCollectionChangeListener(e -> lookup.setValue(null));

                lookup.addValueChangeListener(e -> _setValue(e.getValue(), valueProperty));
                lookup.setValue((Entity) _getValue(valueProperty));

                return lookup;
            }
        }
    }

    protected CollectionContainer<Entity> createEntityOptions(MetaClass metaClass) {
        //noinspection unchecked
        CollectionContainer<Entity> container =
                dataContextFactory.createCollectionContainer(metaClass.getJavaClass());
        CollectionLoader<Entity> dataLoader = dataContextFactory.createCollectionLoader();
        dataLoader.setContainer(container);

        dataLoader.setView(entityView);
        dataLoader.setQuery(entityQuery);

        dataLoader.load();

        return container;
    }

    protected Component createEnumLookup(final ValueProperty valueProperty) {
        if (inExpr) {
            ListEditor<Object> listEditor = uiComponents.create(ListEditor.NAME);
            listEditor.setItemType(ListEditor.ItemType.ENUM);
            //noinspection unchecked
            listEditor.setEnumClass(javaClass);
            initListEditor(listEditor, valueProperty);
            return listEditor;

        } else {
            LookupField<Object> lookup = uiComponents.create(LookupField.NAME);
            List options = Arrays.asList(javaClass.getEnumConstants());
            lookup.setOptionsList(options);

            lookup.addValueChangeListener(e -> _setValue(e.getValue(), valueProperty));
            lookup.setValue(_getValue(valueProperty));

            return lookup;
        }
    }

    protected Component createRuntimeEnumLookup(final ValueProperty valueProperty) {
        if (javaClass == Boolean.class) {
            return createBooleanField(valueProperty);
        }

        CategoryAttribute categoryAttribute = dataManager.load(CategoryAttribute.class)
                .view(View.LOCAL)
                .id(categoryAttrId)
                .optional()
                .orElseThrow(() -> new EntityAccessException(CategoryAttribute.class, categoryAttrId));

        runtimeEnum = new LinkedList<>();
        String enumerationString = categoryAttribute.getEnumeration();
        String[] array = StringUtils.split(enumerationString, ',');
        for (String s : array) {
            String trimmedValue = StringUtils.trimToNull(s);
            if (trimmedValue != null) {
                runtimeEnum.add(trimmedValue);
            }
        }

        if (inExpr) {
            ListEditor<Object> listEditor = uiComponents.create(ListEditor.NAME);
            listEditor.setItemType(ListEditor.ItemType.STRING);
            listEditor.setOptionsMap(categoryAttribute.getLocalizedEnumerationMap());
            initListEditor(listEditor, valueProperty);
            return listEditor;
        } else {
            LookupField<Object> lookup = uiComponents.create(LookupField.of(Object.class));
            lookup.setOptions(new MapOptions<>(categoryAttribute.getLocalizedEnumerationMap()));

            lookup.addValueChangeListener(e -> {
                _setValue(e.getValue(), valueProperty);
            });

            lookup.setValue(_getValue(valueProperty));

            return lookup;
        }
    }

    protected void initListEditor(ListEditor<Object> listEditor, ValueProperty valueProperty) {
        listEditor.addValueChangeListener(e -> {
            Object value = e.getValue();
            if (value != null && ((List) value).isEmpty()) {
                value = null;
            }
            _setValue(value, valueProperty);
        });
        Object value = _getValue(valueProperty);
        if (value != null) {
            //noinspection unchecked
            listEditor.setValue((List<Object>) value);
        }
        listEditor.setClearButtonVisible(true);
    }

    public void toXml(Element element, ValueProperty valueProperty) {
        Element paramElem = element.addElement("param");
        paramElem.addAttribute("name", getName());
        paramElem.addAttribute("javaClass", getJavaClass().getName());
        if (runtimeEnum != null) {
            paramElem.addAttribute("categoryAttrId", categoryAttrId.toString());
        }
        if (isDateInterval) {
            paramElem.addAttribute("isDateInterval", "true");
        }

        paramElem.setText(formatValue(_getValue(valueProperty)));
    }

    public void addValueChangeListener(ParamValueChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeValueChangeListener(ParamValueChangeListener listener) {
        listeners.remove(listener);
    }

    public MetaProperty getProperty() {
        return property;
    }

    public interface ParamValueChangeListener {

        void valueChanged(@Nullable Object prevValue, @Nullable Object value);
    }
}