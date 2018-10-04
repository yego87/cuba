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
package com.haulmont.cuba.gui.xml.layout.loaders;

import com.google.common.base.Strings;
import com.haulmont.bali.util.Dom4j;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.cuba.core.app.dynamicattributes.DynamicAttributesUtils;
import com.haulmont.cuba.core.entity.CategoryAttribute;
import com.haulmont.cuba.core.global.MessageTools;
import com.haulmont.cuba.core.global.MetadataTools;
import com.haulmont.cuba.core.global.Security;
import com.haulmont.cuba.gui.ComponentsHelper;
import com.haulmont.cuba.gui.GuiDevelopmentException;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.FieldGroup.FieldCaptionAlignment;
import com.haulmont.cuba.gui.components.data.HasValueBinding;
import com.haulmont.cuba.gui.components.data.ValueSource;
import com.haulmont.cuba.gui.components.data.meta.EntityValueSource;
import com.haulmont.cuba.gui.components.data.value.DatasourceValueSource;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.data.DsContext;
import com.haulmont.cuba.gui.dynamicattributes.DynamicAttributeCustomFieldGenerator;
import com.haulmont.cuba.gui.dynamicattributes.DynamicAttributesGuiTools;
import com.haulmont.cuba.gui.screen.compatibility.LegacyFrame;
import com.haulmont.cuba.gui.xml.DeclarativeFieldGenerator;
import com.haulmont.cuba.gui.xml.layout.ComponentLoader;
import com.haulmont.cuba.gui.xml.layout.LayoutLoader;
import com.haulmont.cuba.security.entity.EntityOp;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Element;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;
import static com.haulmont.bali.util.Preconditions.checkNotNullArgument;
import static com.haulmont.cuba.core.app.dynamicattributes.DynamicAttributesUtils.getCategoryAttribute;
import static com.haulmont.cuba.core.app.dynamicattributes.DynamicAttributesUtils.isDynamicAttribute;

public class FieldGroupLoader extends AbstractComponentLoader<FieldGroup> {

    protected FieldGroupFieldFactory fieldFactory;

    @Override
    public void createComponent() {
        resultComponent = factory.create(FieldGroup.NAME);
        loadId(resultComponent, element);

        // required for border visible
        loadBorder(resultComponent, element);
    }

    @Override
    public void loadComponent() {
        assignFrame(resultComponent);

        String fieldFactoryBean = element.attributeValue("fieldFactoryBean");
        if (StringUtils.isNotEmpty(fieldFactoryBean)) {
            fieldFactory = beanLocator.get(fieldFactoryBean, FieldGroupFieldFactory.class);
        } else {
            fieldFactory = beanLocator.get(FieldGroupFieldFactory.NAME);
        }

        assignXmlDescriptor(resultComponent, element);

        loadVisible(resultComponent, element);
        loadWidth(resultComponent, element);

        loadEditable(resultComponent, element);
        loadEnable(resultComponent, element);

        loadStyleName(resultComponent, element);

        loadIcon(resultComponent, element);
        loadCaption(resultComponent, element);
        loadDescription(resultComponent, element);
        loadContextHelp(resultComponent, element);

        loadHeight(resultComponent, element);

        loadAlign(resultComponent, element);

        loadCaptionAlignment(resultComponent, element);

        loadFieldCaptionWidth(resultComponent, element);

        Datasource ds = loadDatasource(element);
        resultComponent.setDatasource(ds);

        if (element.elements("column").isEmpty()) {
            Iterable<Component> rootFields = loadFields(resultComponent, element, ds, null);
            Iterable<FieldConfig> dynamicAttributeFields = loadDynamicAttributeFields(ds);
            for (FieldConfig field : dynamicAttributeFields) {
                if (resultComponent.getWidth() > 0 && field.getWidth() == null) {
                    field.setWidth("100%");
                }
            }
            /*for (FieldConfig field : Iterables.concat(rootFields, dynamicAttributeFields)) {
                resultComponent.addField(field);
            }*/
            // TODO: gg, add usage of dynamic attributes
            for (Component component : rootFields) {
                resultComponent.add(component);
            }
        } else {
            @SuppressWarnings("unchecked")
            List<Element> columnElements = element.elements("column");
            @SuppressWarnings("unchecked")
            List<Element> fieldElements = element.elements("field");
            if (fieldElements.size() > 0) {
                Map<String, Object> params = new HashMap<>();
                String fieldGroupId = resultComponent.getId();
                if (StringUtils.isNotEmpty(fieldGroupId)) {
                    params.put("FieldGroup ID", fieldGroupId);
                }
                throw new GuiDevelopmentException("FieldGroup field elements should be placed within its column.", context.getFullFrameId(), params);
            }
            resultComponent.setColumns(columnElements.size());

            int colIndex = 0;
            for (Element columnElement : columnElements) {
                String flex = columnElement.attributeValue("flex");
                if (StringUtils.isNotEmpty(flex)) {
                    resultComponent.setColumnExpandRatio(colIndex, Float.parseFloat(flex));
                }

                String columnWidth = loadThemeString(columnElement.attributeValue("width"));

                Iterable<Component> columnFields = loadFields(resultComponent, columnElement, ds, columnWidth);
                // TODO: gg, add usage of dynamic attributes
//                if (colIndex == 0) {
//                    columnFields = Iterables.concat(columnFields, loadDynamicAttributeFields(ds));
//                }
                for (Component field : columnFields) {
                    resultComponent.add(field, colIndex);
                }

                String columnFieldCaptionWidth = columnElement.attributeValue("fieldCaptionWidth");
                if (StringUtils.isNotEmpty(columnFieldCaptionWidth)) {
                    if (columnFieldCaptionWidth.startsWith(MessageTools.MARK)) {
                        columnFieldCaptionWidth = loadResourceString(columnFieldCaptionWidth);
                    }
                    if (columnFieldCaptionWidth.endsWith("px")) {
                        columnFieldCaptionWidth = columnFieldCaptionWidth.substring(0, columnFieldCaptionWidth.indexOf("px"));
                    }

                    resultComponent.setFieldCaptionWidth(colIndex, Integer.parseInt(columnFieldCaptionWidth));
                }

                colIndex++;
            }
        }

        for (FieldConfig field : resultComponent.getFields()) {
            if (!field.isCustom()) {
                if (!isDynamicAttribute(field.getProperty())) {
                    // the following does not make sense for dynamic attributes
                    loadValidators(resultComponent, field);
                    loadRequired(resultComponent, field);
                    loadEnable(resultComponent, field);
                }
                loadVisible(resultComponent, field);
                loadEditable(resultComponent, field);
            }
        }

        // TODO: gg, remove
//        resultComponent.bind();

        for (FieldConfig field : resultComponent.getFields()) {
            if (field.getXmlDescriptor() != null) {
                String generator = field.getXmlDescriptor().attributeValue("generator");
                if (generator != null) {
                    DeclarativeFieldGenerator fieldGenerator = new DeclarativeFieldGenerator(resultComponent, generator);
                    Component fieldComponent = fieldGenerator.generateField(field.getTargetDatasource(), field.getProperty());
                    field.setComponent(fieldComponent);
                }
            }
        }
    }

    protected DynamicAttributesGuiTools getDynamicAttributesGuiTools() {
        return beanLocator.get(DynamicAttributesGuiTools.NAME);
    }

    protected MetadataTools getMetadataTools() {
        return beanLocator.get(MetadataTools.NAME);
    }

    protected void applyPermissions(Component fieldComponent) {
        if (fieldComponent instanceof DatasourceComponent) {
            DatasourceComponent dsComponent = (DatasourceComponent) fieldComponent;
            MetaPropertyPath propertyPath = dsComponent.getMetaPropertyPath();
            Datasource datasource = dsComponent.getDatasource();

            if (datasource != null && propertyPath != null) {
                MetaClass metaClass = datasource.getMetaClass();

                Security security = getSecurity();

                if (!security.isEntityAttrUpdatePermitted(metaClass, propertyPath.toString())
                        && dsComponent instanceof Component.Editable) {
                    ((Component.Editable) dsComponent).setEditable(false);
                }
                if (!security.isEntityAttrReadPermitted(metaClass, propertyPath.toString())) {
                    dsComponent.setVisible(false);
                }
            }
        }
    }

    protected List<FieldConfig> loadDynamicAttributeFields(Datasource ds) {
        if (ds != null && getMetadataTools().isPersistent(ds.getMetaClass())) {
            String windowId = ComponentsHelper.getWindow(resultComponent).getId();

            Set<CategoryAttribute> attributesToShow =
                    getDynamicAttributesGuiTools().getAttributesToShowOnTheScreen(ds.getMetaClass(),
                            windowId, resultComponent.getId());

            if (!attributesToShow.isEmpty()) {
                List<FieldConfig> fields = new ArrayList<>();

                ds.setLoadDynamicAttributes(true);

                for (CategoryAttribute attribute : attributesToShow) {
                    FieldConfig field = resultComponent.createField(
                            DynamicAttributesUtils.encodeAttributeCode(attribute.getCode()));
                    field.setProperty(DynamicAttributesUtils.encodeAttributeCode(attribute.getCode()));
                    field.setCaption(attribute.getLocaleName());
                    field.setDatasource(ds);
                    field.setRequired(attribute.getRequired());
                    field.setRequiredMessage(getMessages().formatMainMessage(
                            "validation.required.defaultMsg",
                            attribute.getLocaleName()));
                    loadWidth(field, attribute.getWidth());

                    // Currently, ListEditor does not support datasource binding so we create custom field
                    if (BooleanUtils.isTrue(attribute.getIsCollection())) {
                        FieldGroup.CustomFieldGenerator fieldGenerator = new DynamicAttributeCustomFieldGenerator();

                        Component fieldComponent = fieldGenerator.generateField(ds, DynamicAttributesUtils.encodeAttributeCode(attribute.getCode()));
                        field.setCustom(true);
                        field.setComponent(fieldComponent);
                        applyPermissions(fieldComponent);
                    }
                    fields.add(field);
                }

                getDynamicAttributesGuiTools().listenDynamicAttributesChanges(ds);
                return fields;
            }
        }
        return Collections.emptyList();
    }

    protected void loadFieldCaptionWidth(FieldGroup resultComponent, Element element) {
        String fieldCaptionWidth = element.attributeValue("fieldCaptionWidth");
        if (StringUtils.isNotEmpty(fieldCaptionWidth)) {
            if (fieldCaptionWidth.startsWith(MessageTools.MARK)) {
                fieldCaptionWidth = loadResourceString(fieldCaptionWidth);
            }
            if (fieldCaptionWidth.endsWith("px")) {
                fieldCaptionWidth = fieldCaptionWidth.substring(0, fieldCaptionWidth.indexOf("px"));
            }

            resultComponent.setFieldCaptionWidth(Integer.parseInt(fieldCaptionWidth));
        }
    }

    protected Datasource loadDatasource(Element element) {
        String datasource = element.attributeValue("datasource");
        if (!StringUtils.isBlank(datasource)) {
            Datasource ds = context.getDsContext().get(datasource);
            if (ds == null) {
                throw new GuiDevelopmentException("Can't find datasource by name: " + datasource, context.getFullFrameId());
            }
            return ds;
        }
        return null;
    }

    protected List<Component> loadFields(FieldGroup resultComponent, Element element, Datasource ds,
                                                      @Nullable String columnWidth) {
        @SuppressWarnings("unchecked")
        List<Element> fieldElements = element.elements();
        return fieldElements.isEmpty()
                ? Collections.emptyList()
                : loadFields(resultComponent, fieldElements, ds, columnWidth);
    }

    protected List<Component> loadFields(FieldGroup resultComponent, List<Element> elements, Datasource ds,
                                                      @Nullable String columnWidth) {
        List<Component> fields = new ArrayList<>(elements.size());
        List<String> ids = new ArrayList<>();
        for (Element fieldElement : elements) {
            Component component;
            if ("field".equals(fieldElement.getName())) {
                FieldConfig field = loadField(fieldElement, ds, columnWidth);
                component = buildComponent(field);
            } else {
                component = loadComponent(fieldElement, columnWidth);
            }

            if (ids.contains(component.getId())) {
                Map<String, Object> params = new HashMap<>();
                String fieldGroupId = resultComponent.getId();
                if (StringUtils.isNotEmpty(fieldGroupId)) {
                    params.put("FieldGroup ID", fieldGroupId);
                }

                throw new GuiDevelopmentException(
                        String.format("FieldGroup column contains duplicate fields '%s'.", component.getId()),
                        context.getFullFrameId(), params);
            }

            fields.add(component);
            ids.add(component.getId());
        }
        return fields;
    }

    protected Component buildComponent(FieldConfig fc) {
        Component component;

        if (fc.isCustom()) {
            component = fc.getComponent();

            if (component != null) {
                    if (fc.getAttachMode() == FieldGroup.FieldAttachMode.APPLY_DEFAULTS) {
                        applyFieldDefaults(component, fc);
                    }
            } else {
                component = createComponentStub(fc);
            }
        } else {
            ValueSource targetVs = fc.getValueSource();
            if (targetVs == null) {
                targetVs = resultComponent.getValueSourceProvider().getValueSource(fc.getProperty());
                if (targetVs == null) {
                    throw new IllegalStateException(String.format("Unable to get value source for field '%s'", fc.getId()));
                }
            }

            // TODO: gg, set VS
            if (fc.getDatasource() == null) {
                fc.setDatasource(resultComponent.getDatasource());
            }

            FieldGroupFieldFactory.GeneratedField generatedField = fieldFactory.createField(fc);
            component = generatedField.getComponent();

            if (generatedField.getAttachMode() == FieldGroup.FieldAttachMode.APPLY_DEFAULTS) {
                applyFieldDefaults(component, fc);
            }
        }

        if (component.getId() == null) {
            component.setId(fc.getId());
        }

        return component;
    }

    protected Component createComponentStub(FieldConfig fc) {
        FieldGroupFieldFactory.GeneratedField field = fieldFactory.createField(fc);
        return field.getComponent();
    }

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

            for (Field.Validator validator : fc.getValidators()) {
                cubaField.addValidator(validator);
            }

            if (fc.getWidth() != null) {
                fieldComponent.setWidth(fc.getWidth());
            } else {
                // FIXME: gg, how to replace?
                /*if (App.isBound()) {
                    ThemeConstants theme = App.getInstance().getThemeConstants();
                    fieldComponent.setWidth(theme.get("cuba.web.WebFieldGroup.defaultFieldWidth"));
                }*/
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

    protected CollectionDatasource findDatasourceRecursively(DsContext dsContext, String dsName) {
        if (dsContext == null) {
            return null;
        }

        Datasource datasource = dsContext.get(dsName);
        if (datasource != null && datasource instanceof CollectionDatasource) {
            return (CollectionDatasource) datasource;
        } else {
            if (dsContext.getParent() != null) {
                return findDatasourceRecursively(dsContext.getParent(), dsName);
            } else {
                return null;
            }
        }
    }

    protected Component loadComponent(Element element, String columnWidth) {
        String id = element.attributeValue("id");
        String property = element.attributeValue("property");

        if (Strings.isNullOrEmpty(id) && Strings.isNullOrEmpty(property)) {
            throw new GuiDevelopmentException(String.format("id/property is not defined for field of FieldGroup '%s'. " +
                    "Set id or property attribute.", resultComponent.getId()), context.getFullFrameId());
        }

        LayoutLoader loader = beanLocator.getPrototype(LayoutLoader.NAME, context);
        loader.setLocale(getLocale());
        loader.setMessagesPack(getMessagesPack());

        ComponentLoader childComponentLoader = loader.createComponent(element);
        childComponentLoader.loadComponent();

        Component component = childComponentLoader.getResultComponent();

        if (Strings.isNullOrEmpty(component.getId())) {
            component.setId(property);
        }

        // TODO: gg, extract method
        String width = element.attributeValue("width");
        if (StringUtils.isEmpty(width)) {
            width = columnWidth;
        }
        if ("auto".equalsIgnoreCase(width)) {
            component.setWidth(Component.AUTO_SIZE);
        } else if (StringUtils.isNotBlank(width)) {
            component.setWidth(loadThemeString(width));
        }

        if (component instanceof HasValueBinding
                && ((HasValueBinding) component).getValueSource() instanceof EntityValueSource
                && component instanceof Component.HasCaption
                && ((Component.HasCaption) component).getCaption() == null) {
            EntityValueSource valueSource = ((EntityValueSource) ((HasValueBinding) component).getValueSource());

            MetaPropertyPath metaPropertyPath = valueSource.getMetaPropertyPath();

            String propertyName = metaPropertyPath != null ? metaPropertyPath.getMetaProperty().getName() : null;
            if (metaPropertyPath != null) {
                if (isDynamicAttribute(metaPropertyPath.getMetaProperty())) {
                    CategoryAttribute categoryAttribute = getCategoryAttribute(metaPropertyPath.getMetaProperty());
                    ((Component.HasCaption) component).setCaption(categoryAttribute != null
                            ? categoryAttribute.getLocaleName()
                            : propertyName);
                } else {
                    MetaClass propertyMetaClass = getMetadataTools().getPropertyEnclosingMetaClass(metaPropertyPath);
                    String propertyCaption = getMessageTools().getPropertyCaption(propertyMetaClass, propertyName);
                    ((Component.HasCaption) component).setCaption(propertyCaption);
                }
            }
        }

        return component;
    }

    protected FieldConfig loadField(Element element, Datasource ds, String columnWidth) {
        String id = element.attributeValue("id");
        String property = element.attributeValue("property");

        if (Strings.isNullOrEmpty(id) && Strings.isNullOrEmpty(property)) {
            throw new GuiDevelopmentException(String.format("id/property is not defined for field of FieldGroup '%s'. " +
                    "Set id or property attribute.", resultComponent.getId()), context.getFullFrameId());
        }

        if (Strings.isNullOrEmpty(property)) {
            property = id;
        } else if (Strings.isNullOrEmpty(id)) {
            id = property;
        }

        Datasource targetDs = ds;

        Datasource datasource = loadDatasource(element);
        if (datasource != null) {
            targetDs = datasource;
        }

        CollectionDatasource optionsDs = null;
        String optDsName = element.attributeValue("optionsDatasource");
        if (StringUtils.isNotBlank(optDsName)) {
            LegacyFrame frame = (LegacyFrame) getContext().getFrame().getFrameOwner();
            DsContext dsContext = frame.getDsContext();
            optionsDs = findDatasourceRecursively(dsContext, optDsName);
            if (optionsDs == null) {
                throw new GuiDevelopmentException(String.format("Options datasource %s not found for field %s", optDsName, id)
                        , context.getFullFrameId());
            }
        }

        boolean customField = false;
        String custom = element.attributeValue("custom");
        if (StringUtils.isNotEmpty(custom)) {
            customField = Boolean.parseBoolean(custom);
        }

        if (StringUtils.isNotEmpty(element.attributeValue("generator"))) {
            customField = true;
        }

        List<Element> elements = Dom4j.elements(element);
        List<Element> customElements = elements.stream()
                .filter(e -> !("formatter".equals(e.getName()) || "validator".equals(e.getName())))
                .collect(Collectors.toList());

        if (!customElements.isEmpty()) {
            if (customElements.size() > 1) {
                throw new GuiDevelopmentException(
                        String.format("FieldGroup field %s element cannot contains two or more custom field definitions", id),
                        context.getCurrentFrameId());
            }
            if (customField) {
                throw new GuiDevelopmentException(
                        String.format("FieldGroup field %s cannot use both custom/generator attribute and inline component definition", id),
                        context.getCurrentFrameId());
            }
            customField = true;
        }

        if (!customField && targetDs == null) {
            throw new GuiDevelopmentException(String.format("Datasource is not defined for FieldGroup field '%s'. " +
                    "Only custom fields can have no datasource.", property), context.getFullFrameId());
        }

        FieldConfig field = new FieldConfigImpl(id);

        if (property != null) {
            field.setProperty(property);
        }

        // TODO: gg, check for cases like VS and DS, property etc.
        // TODO: gg, ValueSource
        if (datasource != null) {
            field.setDatasource(datasource);
        }

        // TODO: gg, options VS
        if (optionsDs != null) {
            field.setOptionsDatasource(optionsDs);
        }

        String stylename = element.attributeValue("stylename");
        if (StringUtils.isNotEmpty(stylename)) {
            field.setStyleName(stylename);
        }

        MetaPropertyPath metaPropertyPath = null;
        if (targetDs != null && property != null) {
            MetaClass metaClass = targetDs.getMetaClass();
            metaPropertyPath = getMetadataTools().resolveMetaPropertyPath(targetDs.getMetaClass(), property);
            if (metaPropertyPath == null) {
                if (!customField) {
                    throw new GuiDevelopmentException(String.format("Property '%s' is not found in entity '%s'",
                            property, metaClass.getName()), context.getFullFrameId());
                }
            }
        }
        String propertyName = metaPropertyPath != null ? metaPropertyPath.getMetaProperty().getName() : null;
        if (metaPropertyPath != null && isDynamicAttribute(metaPropertyPath.getMetaProperty())) {
            CategoryAttribute categoryAttribute = getCategoryAttribute(metaPropertyPath.getMetaProperty());
            field.setCaption(categoryAttribute != null ? categoryAttribute.getLocaleName() : propertyName);
        } else {
            loadCaption(field, element);

            if (field.getCaption() == null) {
                field.setCaption(getDefaultCaption(field, targetDs));
            }
        }
        loadDescription(field, element);
        loadContextHelp(field, element);

        field.setXmlDescriptor(element);

        Function formatter = loadFormatter(element);
        if (formatter != null) {
            field.setFormatter(formatter);
        }

        String defaultWidth = element.attributeValue("width");
        if (StringUtils.isEmpty(defaultWidth)) {
            defaultWidth = columnWidth;
        }
        loadWidth(field, defaultWidth);

        if (customField) {
            field.setCustom(true);
        }

        String required = element.attributeValue("required");
        if (StringUtils.isNotEmpty(required)) {
            field.setRequired(Boolean.parseBoolean(required));
        }

        String requiredMsg = element.attributeValue("requiredMessage");
        if (requiredMsg != null) {
            requiredMsg = loadResourceString(requiredMsg);
            field.setRequiredMessage(requiredMsg);
        }

        String tabIndex = element.attributeValue("tabIndex");
        if (StringUtils.isNotEmpty(tabIndex)) {
            field.setTabIndex(Integer.parseInt(tabIndex));
        }

        loadInputPrompt(field, element);

        if (customElements.size() == 1) {
            // load nested component defined as inline
            Element customFieldElement = customElements.get(0);

            LayoutLoader loader = beanLocator.getPrototype(LayoutLoader.NAME, context);
            loader.setLocale(getLocale());
            loader.setMessagesPack(getMessagesPack());

            ComponentLoader childComponentLoader = loader.createComponent(customFieldElement);
            childComponentLoader.loadComponent();

            Component customComponent = childComponentLoader.getResultComponent();

            String inlineAttachMode = element.attributeValue("inlineAttachMode");
            if (StringUtils.isNotEmpty(inlineAttachMode)) {
                field.setComponent(customComponent, FieldGroup.FieldAttachMode.valueOf(inlineAttachMode));
            } else {
                field.setComponent(customComponent);
            }
        }

        return field;
    }

    protected void loadContextHelp(FieldConfig field, Element element) {
        String contextHelpText = element.attributeValue("contextHelpText");
        if (StringUtils.isNotEmpty(contextHelpText)) {
            contextHelpText = loadResourceString(contextHelpText);
            field.setContextHelpText(contextHelpText);
        }

        String htmlEnabled = element.attributeValue("contextHelpTextHtmlEnabled");
        if (StringUtils.isNotEmpty(htmlEnabled)) {
            field.setContextHelpTextHtmlEnabled(Boolean.parseBoolean(htmlEnabled));
        }
    }

    protected String getDefaultCaption(FieldConfig fieldConfig, Datasource fieldDatasource) {
        String caption = fieldConfig.getCaption();
        if (caption == null) {
            String propertyId = fieldConfig.getProperty();
            MetaPropertyPath propertyPath = fieldDatasource != null ?
                    fieldDatasource.getMetaClass().getPropertyPath(propertyId) : null;

            if (propertyPath != null) {
                MetaClass propertyMetaClass = getMetadataTools().getPropertyEnclosingMetaClass(propertyPath);
                String propertyName = propertyPath.getMetaProperty().getName();
                caption = getMessageTools().getPropertyCaption(propertyMetaClass, propertyName);
            }
        }
        return caption;
    }

    protected void loadWidth(FieldConfig field, String width) {
        if ("auto".equalsIgnoreCase(width)) {
            field.setWidth(Component.AUTO_SIZE);
        } else if (StringUtils.isNotBlank(width)) {
            field.setWidth(loadThemeString(width));
        }
    }

    protected void loadValidators(FieldGroup resultComponent, FieldConfig field) {
        Element descriptor = field.getXmlDescriptor();
        @SuppressWarnings("unchecked")
        List<Element> validatorElements = (descriptor == null) ? null : descriptor.elements("validator");
        if (validatorElements != null) {
            if (!validatorElements.isEmpty()) {
                for (Element validatorElement : validatorElements) {
                    Field.Validator validator = loadValidator(validatorElement);
                    if (validator != null) {
                        field.addValidator(validator);
                    }
                }
            }
        } else {
            Datasource ds;
            if (field.getDatasource() == null) {
                ds = resultComponent.getDatasource();
            } else {
                ds = field.getDatasource();
            }

            if (ds != null) {
                MetaClass metaClass = ds.getMetaClass();
                MetaPropertyPath metaPropertyPath = metaClass.getPropertyPath(field.getProperty());

                if (metaPropertyPath != null) {
                    MetaProperty metaProperty = metaPropertyPath.getMetaProperty();
                    Field.Validator validator = null;
                    if (descriptor == null) {
                        validator = getDefaultValidator(metaProperty);
                    } else if (!"timeField".equals(descriptor.attributeValue("field"))) {
                        validator = getDefaultValidator(metaProperty); //In this case we no need to use validator
                    }

                    if (validator != null) {
                        field.addValidator(validator);
                    }
                }
            }
        }
    }

    protected void loadRequired(FieldGroup resultComponent, FieldConfig field) {
        if (field.isCustom()) {
            Element element = field.getXmlDescriptor();
            if (element == null) {
                return;
            }

            String required = element.attributeValue("required");
            if (StringUtils.isNotEmpty(required)) {
                field.setRequired(Boolean.parseBoolean(required));
            }

            String requiredMessage = element.attributeValue("requiredMessage");
            if (StringUtils.isNotEmpty(requiredMessage)) {
                field.setRequiredMessage(loadResourceString(requiredMessage));
            }
        } else {
            MetaClass metaClass = getMetaClass(resultComponent, field);

            Element element = field.getXmlDescriptor();

            String required = element.attributeValue("required");
            if (StringUtils.isNotEmpty(required)) {
                field.setRequired(Boolean.parseBoolean(required));
            }

            String requiredMsg = element.attributeValue("requiredMessage");
            if (StringUtils.isEmpty(requiredMsg) && metaClass != null) {
                MetaPropertyPath propertyPath = metaClass.getPropertyPath(field.getProperty());

                checkNotNullArgument(propertyPath, "Could not resolve property path '%s' in '%s'", field.getProperty(), metaClass);

                requiredMsg = getMessageTools().getDefaultRequiredMessage(metaClass, propertyPath.toString());
            }

            field.setRequiredMessage(loadResourceString(requiredMsg));
        }
    }

    @Override
    protected void loadEditable(Component component, Element element) {
        FieldGroup fieldGroup = (FieldGroup) component;

        if (fieldGroup.getDatasource() != null) {
            MetaClass metaClass = fieldGroup.getDatasource().getMetaClass();
            boolean editableByPermission = (getSecurity().isEntityOpPermitted(metaClass, EntityOp.CREATE)
                    || getSecurity().isEntityOpPermitted(metaClass, EntityOp.UPDATE));
            if (!editableByPermission) {
                fieldGroup.setEditable(false);
                return;
            }
        }

        String editable = element.attributeValue("editable");
        if (StringUtils.isNotEmpty(editable)) {
            fieldGroup.setEditable(Boolean.parseBoolean(editable));
        }
    }

    protected MetaClass getMetaClass(FieldGroup resultComponent, FieldConfig field) {
        if (field.isCustom()) {
            return null;
        }
        Datasource datasource;
        if (field.getDatasource() != null) {
            datasource = field.getDatasource();
        } else if (resultComponent.getDatasource() != null) {
            datasource = resultComponent.getDatasource();
        } else {
            throw new GuiDevelopmentException(String.format("Unable to get datasource for field '%s'",
                    field.getId()), context.getFullFrameId());
        }
        return datasource.getMetaClass();
    }

    protected void loadEditable(FieldGroup resultComponent, FieldConfig field) {
        Element element = field.getXmlDescriptor();
        if (element != null) {
            String editable = element.attributeValue("editable");
            if (StringUtils.isNotEmpty(editable)) {
                field.setEditable(Boolean.parseBoolean(editable));
            }
        }

        if (!field.isCustom() && BooleanUtils.isNotFalse(field.isEditable())) {
            MetaClass metaClass = getMetaClass(resultComponent, field);
            MetaPropertyPath propertyPath = getMetadataTools().resolveMetaPropertyPath(metaClass, field.getProperty());

            checkNotNullArgument(propertyPath, "Could not resolve property path '%s' in '%s'", field.getId(), metaClass);

            if (!getSecurity().isEntityAttrUpdatePermitted(metaClass, propertyPath.toString())) {
                field.setEditable(false);
            }
        }
    }

    protected void loadVisible(FieldGroup resultComponent, FieldConfig field) {
        Element element = field.getXmlDescriptor();
        if (element != null) {
            String visible = element.attributeValue("visible");
            if (StringUtils.isNotEmpty(visible)) {
                field.setVisible(Boolean.parseBoolean(visible));
            }
        }

        if (!field.isCustom() && BooleanUtils.isNotFalse(field.isVisible())) {
            MetaClass metaClass = getMetaClass(resultComponent, field);
            MetaPropertyPath propertyPath = getMetadataTools().resolveMetaPropertyPath(metaClass, field.getProperty());

            checkNotNullArgument(propertyPath, "Could not resolve property path '%s' in '%s'", field.getId(), metaClass);

            if (!getSecurity().isEntityAttrReadPermitted(metaClass, propertyPath.toString())) {
                field.setVisible(false);
            }
        }
    }

    protected void loadEnable(FieldGroup resultComponent, FieldConfig field) {
        Element element = field.getXmlDescriptor();
        if (element != null) {
            String enable = element.attributeValue("enable");
            if (StringUtils.isNotEmpty(enable)) {
                field.setEnabled(Boolean.parseBoolean(enable));
            }
        }
    }

    protected void loadCaptionAlignment(FieldGroup resultComponent, Element element) {
        String captionAlignment = element.attributeValue("captionAlignment");
        if (StringUtils.isNotEmpty(captionAlignment)) {
            resultComponent.setCaptionAlignment(FieldCaptionAlignment.valueOf(captionAlignment));
        }
    }

    /**
     * Configuration of a field. Used as declarative configuration object.
     * After component is set it can be used as Field API for a Component that does not implement {@link Field}.
     */
    public interface FieldConfig extends Component.HasXmlDescriptor, Component.HasCaption, HasFormatter, HasInputPrompt {
        /**
         * @return id
         */
        String getId();

        /**
         * @return true if this field config is connected to the concrete component and cannot be reconfigured.
         */
        boolean isBound();

        /**
         * @return width
         */
        String getWidth();
        /**
         * Set width parameter. <br>
         * If {@link #isBound()} is true sets width to the connected Component.
         *
         * @param width width
         */
        void setWidth(String width);

        /**
         * @return style name
         */
        String getStyleName();
        /**
         * Set stylename parameter. <br>
         * If {@link #isBound()} is true sets stylename to the connected Component.
         *
         * @param stylename style name
         */
        void setStyleName(String stylename);

        // TODO: gg, remove
        ValueSource getTargetValueSource();

        ValueSource getValueSource();

        void setValueSource(ValueSource targetValueSource);

        /**
         * @return own datasource of a field or datasource of the parent FieldGroup
         */
        @Deprecated
        default Datasource getTargetDatasource() {
            ValueSource valueSource = getTargetValueSource();
            return valueSource instanceof DatasourceValueSource
                    ? ((DatasourceValueSource) valueSource).getDatasource()
                    : null;
        }

        /**
         * @return datasource
         */
        @Deprecated
        default Datasource getDatasource() {
            ValueSource valueSource = getTargetValueSource();
            return valueSource instanceof DatasourceValueSource
                    ? ((DatasourceValueSource) valueSource).getDatasource()
                    : null;
        }

        /**
         * Set datasource for declarative field. <br>
         * Throws exception if FieldConfig is already connected to Component.
         *
         * @param datasource datasource
         */
        @Deprecated
        default void setDatasource(Datasource datasource) {
            // TODO: gg, that if the property is null?
            setValueSource(datasource != null
                    ? new DatasourceValueSource(datasource, getProperty())
                    : null);
        }

        /**
         * @return true if field is required, null if not set for declarative field
         */
        Boolean isRequired();
        /**
         * Set required for declarative field. <br>
         * If {@link #isBound()} is true and Component implements {@link Field} then sets required to the connected Component.
         *
         * @param required required flag
         */
        void setRequired(Boolean required);

        /**
         * @return true if field is editable, null if not set for declarative field
         */
        Boolean isEditable();
        /**
         * Set editable for declarative field. <br>
         * If {@link #isBound()} is true and Component implements {@link Field} then sets editable to the connected Component.
         *
         * @param editable editable flag
         */
        void setEditable(Boolean editable);

        /**
         * @return true if field is enabled, null if not set for declarative field
         */
        Boolean isEnabled();
        /**
         * Set enabled for declarative field. <br>
         * If {@link #isBound()} is true then sets enabled to the connected Component.
         *
         * @param enabled enabled flag
         */
        void setEnabled(Boolean enabled);

        /**
         * @return true if field is visible, null if not set for declarative field
         */
        Boolean isVisible();
        /**
         * Set visible for declarative field. <br>
         * If {@link #isBound()} is true then sets visible to the connected Component.
         *
         * @param visible visible flag
         */
        void setVisible(Boolean visible);

        /**
         * @return property name
         */
        String getProperty();
        /**
         * Set property for declarative field. <br>
         * Throws exception if FieldConfig is already connected to Component.
         *
         * @param property property name
         */
        void setProperty(String property);

        /**
         * @return tab index
         */
        Integer getTabIndex();
        /**
         * Set tab index for declarative field. <br>
         * If {@link #isBound()} is true and Component implements {@link Component.Focusable} then sets tab index to the connected Component.
         *
         * @param tabIndex tab index
         */
        void setTabIndex(Integer tabIndex);

        /**
         * @return required message
         * @deprecated Use {@link #getRequiredMessage()}
         */
        @Deprecated
        String getRequiredError();
        /**
         * @deprecated Use {@link #setRequiredMessage(String)}}
         */
        @Deprecated
        void setRequiredError(String requiredError);

        /**
         * @return true if field is marked as custom
         */
        boolean isCustom();
        /**
         * Set custom flag. <br>
         *
         * @param custom custom flag
         */
        void setCustom(boolean custom);

        /**
         * @return required message
         */
        String getRequiredMessage();
        /**
         * Set required message for declarative field. <br>
         * If {@link #isBound()} is true and Component implements {@link Field} then sets required message to the connected Component.
         *
         * @param requiredMessage required message
         */
        void setRequiredMessage(String requiredMessage);

        /**
         * @return bound component
         */
        @Nullable
        Component getComponent();
        /**
         * @return bound component. Throws exception if component is null.
         */
        Component getComponentNN();

        /**
         * Bind Component to this field config. Component cannot be changed if it is assigned. <br>
         * FieldConfig will apply default values for caption, description, width, required and other Field properties.
         * <p>
         * When used with custom="true", the datasource and the property should be set up manually.
         *
         * @param component component
         * @see FieldConfig#setComponent(Component, FieldGroup.FieldAttachMode)
         */
        void setComponent(Component component);

        /**
         * Bind Component to this field config. Component cannot be changed if it is assigned. <br>
         * If {@code mode} is {@link FieldGroup.FieldAttachMode#APPLY_DEFAULTS} then FieldConfig will apply default values for
         * caption, description, width, required and other Field properties otherwise it will not.
         * <p>
         * When used with custom="true", the datasource and the property should be set up manually.
         *
         * @param component component
         * @param mode field attach mode
         */
        void setComponent(Component component, FieldGroup.FieldAttachMode mode);

        // TODO: gg, JavaDoc
        FieldGroup.FieldAttachMode getAttachMode();

        /**
         * Add validator for declarative field. <br>
         * If field is bound to Component and Component implements {@link Field} then {@code validator} will be added
         * to Component directly.
         *
         * @param validator validator
         */
        void addValidator(Field.Validator validator);

        /**
         * Remove validator. <br>
         * If field is bound to Component and Component implements {@link Field} then {@code validator} will be removed
         * from Component directly.
         *
         * @param validator validator
         */
        void removeValidator(Field.Validator validator);

        // TODO: gg, JavaDoc
        List<Field.Validator> getValidators();

        /**
         * Set options datasource for declarative field. <br>
         * If field is bound to Component and Component implements {@link OptionsField} then {@code optionsDatasource}
         * will be set to Component directly.
         *
         * @param optionsDatasource options datasource
         */
        void setOptionsDatasource(CollectionDatasource optionsDatasource);
        /**
         * @return options datasource
         */
        CollectionDatasource getOptionsDatasource();

        /**
         * @return context help text
         */
        String getContextHelpText();

        /**
         * Set context help text for declarative field.
         *
         * If {@link #isBound()} is true and Component implements {@link Field} then sets context help text
         * to the connected Component.
         *
         * @param contextHelpText context help text to be set
         */
        void setContextHelpText(String contextHelpText);

        /**
         * @return true if field accepts context help text in HTML format, null if not set for a declarative field
         */
        Boolean isContextHelpTextHtmlEnabled();

        /**
         * Defines if context help text can be presented as HTML.
         * <p>
         * If {@link #isBound()} is true and Component implements {@link Field} then sets this attribute
         * to the connected Component.
         *
         * @param enabled true if field accepts context help text in HTML format
         */
        void setContextHelpTextHtmlEnabled(Boolean enabled);

        /**
         * @return a context help icon click handler
         */
        Consumer<HasContextHelp.ContextHelpIconClickEvent> getContextHelpIconClickHandler();

        /**
         * Sets a context help icon click handler
         *
         * @param handler the handler to set
         */
        void setContextHelpIconClickHandler(Consumer<HasContextHelp.ContextHelpIconClickEvent> handler);
    }

    protected class FieldConfigImpl implements FieldConfig {
        protected String id;
        protected Element xmlDescriptor;
        protected int column;

        protected Component component;

        protected boolean managed = false;

        protected String targetWidth;
        protected String targetStylename;
        protected ValueSource targetValueSource;
        protected Boolean targetRequired;
        protected Boolean targetEditable;
        protected Boolean targetEnabled;
        protected Boolean targetVisible;
        protected String targetProperty;
        protected Integer targetTabIndex;
        protected String targetRequiredMessage;
        protected CollectionDatasource targetOptionsDatasource;
        protected String targetCaption;
        protected String targetDescription;
        protected String targetContextHelpText;
        protected Boolean targetContextHelpTextHtmlEnabled;
        protected String targetInputPrompt;
        protected Function targetFormatter;
        protected boolean isTargetCustom;

        protected List<Field.Validator> targetValidators = new ArrayList<>(0);
        protected Consumer<HasContextHelp.ContextHelpIconClickEvent> targetContextHelpIconClickHandler;
        protected FieldGroup.FieldAttachMode attachMode = FieldGroup.FieldAttachMode.APPLY_DEFAULTS;

        public FieldConfigImpl(String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public boolean isBound() {
            return component != null;
        }

        @Override
        public String getWidth() {
            return targetWidth;
        }

        @Override
        public void setWidth(String width) {
            targetWidth = width;
        }

        @Override
        public String getStyleName() {
            return targetStylename;
        }

        @Override
        public void setStyleName(String stylename) {
            this.targetStylename = stylename;
        }

        // TODO: gg, remove
        @Override
        public ValueSource getTargetValueSource() {
            return getValueSource();
        }

        @Override
        public ValueSource getValueSource() {
            return targetValueSource;
        }

        public void setValueSource(ValueSource targetValueSource) {
            // TODO: gg, do we need this check?
            checkState(this.component == null, "FieldConfig is already bound to component");

            this.targetValueSource = targetValueSource;
        }

        @Override
        public Boolean isRequired() {
            return targetRequired;
        }

        @Override
        public void setRequired(Boolean required) {
            this.targetRequired = required;
        }

        @Override
        public Boolean isEditable() {
            return targetEditable;
        }

        @Override
        public void setEditable(Boolean editable) {
            this.targetEditable = editable;
        }

        @Override
        public Boolean isEnabled() {
            return targetEnabled;
        }

        @Override
        public void setEnabled(Boolean enabled) {
            this.targetEnabled = enabled;
        }

        @Override
        public Boolean isVisible() {
            return targetVisible;
        }

        @Override
        public void setVisible(Boolean visible) {
            this.targetVisible = visible;
        }

        @Override
        public String getProperty() {
            return targetProperty;
        }

        @Override
        public void setProperty(String property) {
            checkState(this.component == null, "Unable to change property for bound FieldConfig");

            this.targetProperty = property;
        }

        @Override
        public Integer getTabIndex() {
            return targetTabIndex;
        }

        @Override
        public void setTabIndex(Integer tabIndex) {
            this.targetTabIndex = tabIndex;
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
            return isTargetCustom;
        }

        @Override
        public void setCustom(boolean custom) {
            checkState(this.component == null, "Unable to change custom flag for bound FieldConfig");

            this.isTargetCustom = custom;
        }

        @Override
        public String getRequiredMessage() {
            return targetRequiredMessage;
        }

        @Override
        public void setRequiredMessage(String requiredMessage) {
            this.targetRequiredMessage = requiredMessage;
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
            checkState(this.component == null, "Unable to change component for bound FieldConfig");

            this.component = component;
        }

        @Override
        public void setComponent(Component component, FieldGroup.FieldAttachMode mode) {
            checkState(this.component == null, "Unable to change component for bound FieldConfig");

            this.attachMode = mode;
            this.component = component;
        }

        public void assignComponent(Component component) {
            checkState(this.component == null, "Unable to change component for bound FieldConfig");

            this.component = component;
        }

        public void setAttachMode(FieldGroup.FieldAttachMode attachMode) {
            this.attachMode = attachMode;
        }

        @Override
        public void addValidator(Field.Validator validator) {
            if (!targetValidators.contains(validator)) {
                targetValidators.add(validator);
            }
        }

        @Override
        public void removeValidator(Field.Validator validator) {
            targetValidators.remove(validator);
        }

        @Override
        public List<Field.Validator> getValidators() {
            return targetValidators;
        }

        // TODO: gg, add OptionsSource
        @Override
        public void setOptionsDatasource(CollectionDatasource optionsDatasource) {
            this.targetOptionsDatasource = optionsDatasource;
        }

        @Override
        public CollectionDatasource getOptionsDatasource() {
            return targetOptionsDatasource;
        }

        @Override
        public String getCaption() {
            return targetCaption;
        }

        @Override
        public void setCaption(String caption) {
            this.targetCaption = caption;
        }

        @Override
        public String getDescription() {
            return targetDescription;
        }

        @Override
        public void setDescription(String description) {
            this.targetDescription = description;
        }

        @Override
        public String getInputPrompt() {
            return targetInputPrompt;
        }

        @Override
        public void setInputPrompt(String inputPrompt) {
            this.targetInputPrompt = inputPrompt;
        }

        @Override
        public String getContextHelpText() {
            return targetContextHelpText;
        }

        @Override
        public void setContextHelpText(String contextHelpText) {
            this.targetContextHelpText = contextHelpText;
        }

        @Override
        public Boolean isContextHelpTextHtmlEnabled() {
            return BooleanUtils.isTrue(targetContextHelpTextHtmlEnabled);
        }

        @Override
        public void setContextHelpTextHtmlEnabled(Boolean enabled) {
            this.targetContextHelpTextHtmlEnabled = enabled;
        }

        @Override
        public Consumer<HasContextHelp.ContextHelpIconClickEvent> getContextHelpIconClickHandler() {
            return targetContextHelpIconClickHandler;
        }

        @Override
        public void setContextHelpIconClickHandler(Consumer<HasContextHelp.ContextHelpIconClickEvent> handler) {
            this.targetContextHelpIconClickHandler = handler;
        }

        @Override
        public Function getFormatter() {
            return targetFormatter;
        }

        @Override
        public void setFormatter(Function formatter) {
            this.targetFormatter = formatter;
        }

        @Override
        public Element getXmlDescriptor() {
            return xmlDescriptor;
        }

        @Override
        public void setXmlDescriptor(Element element) {
            this.xmlDescriptor = element;
        }

        @Override
        public FieldGroup.FieldAttachMode getAttachMode() {
            return attachMode;
        }

        @Override
        public String toString() {
            return "FieldConfig: " + id;
        }
    }
}