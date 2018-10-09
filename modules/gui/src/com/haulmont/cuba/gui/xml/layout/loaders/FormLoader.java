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

package com.haulmont.cuba.gui.xml.layout.loaders;

import com.google.common.base.Strings;
import com.haulmont.bali.util.ParamsMap;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.cuba.core.entity.CategoryAttribute;
import com.haulmont.cuba.core.global.MessageTools;
import com.haulmont.cuba.core.global.MetadataTools;
import com.haulmont.cuba.gui.GuiDevelopmentException;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.Form;
import com.haulmont.cuba.gui.components.data.HasValueSource;
import com.haulmont.cuba.gui.components.data.meta.EntityValueSource;
import com.haulmont.cuba.gui.xml.layout.ComponentLoader;
import com.haulmont.cuba.gui.xml.layout.LayoutLoader;
import org.dom4j.Element;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.haulmont.cuba.core.app.dynamicattributes.DynamicAttributesUtils.getCategoryAttribute;
import static com.haulmont.cuba.core.app.dynamicattributes.DynamicAttributesUtils.isDynamicAttribute;

public class FormLoader extends AbstractComponentLoader<Form> {

    @Override
    public void createComponent() {
        resultComponent = factory.create(Form.NAME);
        loadId(resultComponent, element);
    }

    @Override
    public void loadComponent() {
        assignFrame(resultComponent);

        loadVisible(resultComponent, element);
        loadWidth(resultComponent, element);
        loadHeight(resultComponent, element);

        loadEditable(resultComponent, element);
        loadEnable(resultComponent, element);

        loadStyleName(resultComponent, element);

        loadCaption(resultComponent, element);
        loadDescription(resultComponent, element);
        loadIcon(resultComponent, element);
        loadContextHelp(resultComponent, element);

        loadAlign(resultComponent, element);

        loadCaptionAlignment(resultComponent, element);
        loadChildrenCaptionWidth(resultComponent, element);

        loadColumns(resultComponent, element);
    }

    protected void loadColumns(Form resultComponent, Element element) {
        if (element.elements("column").isEmpty()) {
            List<Component> components = loadComponents(element, null);
            // TODO: gg, dynamic attributes
            for (Component component : components) {
                resultComponent.add(component);
            }
        } else {
            List<Element> columnElements = element.elements("column");
            if (element.elements().size() > columnElements.size()) {
                String fieldGroupId = resultComponent.getId();
                Map<String, Object> params = Strings.isNullOrEmpty(fieldGroupId)
                        ? Collections.emptyMap()
                        : ParamsMap.of("Form ID", fieldGroupId);
                throw new GuiDevelopmentException("Form component elements have to be placed within its column.",
                        context.getFullFrameId(), params);
            }

            resultComponent.setColumns(columnElements.size());

            int colIndex = 0;
            for (Element columnElement : columnElements) {
                String columnWidth = loadThemeString(columnElement.attributeValue("width"));
                List<Component> components = loadComponents(columnElement, columnWidth);
                // TODO: gg, dynamic attributes
                for (Component component : components) {
                    resultComponent.add(component, colIndex);
                }

                loadChildrenCaptionWidth(resultComponent, columnElement, colIndex);

                colIndex++;
            }
        }
    }

    protected List<Component> loadComponents(Element element, @Nullable String columnWidth) {
        List<Element> elements = element.elements();
        if (elements.isEmpty()) {
            return Collections.emptyList();
        } else {
            List<Component> components = new ArrayList<>(elements.size());
            for (Element componentElement : elements) {
                Component component = loadComponent(componentElement, columnWidth);
                components.add(component);
            }
            return components;
        }
    }

    protected Component loadComponent(Element element, @Nullable String columnWidth) {
        LayoutLoader loader = beanLocator.getPrototype(LayoutLoader.NAME, context);
        loader.setLocale(getLocale());
        loader.setMessagesPack(getMessagesPack());

        ComponentLoader childComponentLoader = loader.createComponent(element);
        childComponentLoader.loadComponent();

        Component component = childComponentLoader.getResultComponent();

        // Set default width
        String componentWidth = element.attributeValue("width");
        if (Strings.isNullOrEmpty(componentWidth)
                && columnWidth != null) {
            component.setWidth(columnWidth);
        }

        // TEST: gg,
        // Set default caption
        if (component instanceof HasValueSource
                && ((HasValueSource) component).getValueSource() instanceof EntityValueSource
                && component instanceof Component.HasCaption
                && ((Component.HasCaption) component).getCaption() == null) {
            EntityValueSource valueSource = ((EntityValueSource) ((HasValueSource) component).getValueSource());

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

    protected MetadataTools getMetadataTools() {
        return beanLocator.get(MetadataTools.NAME);
    }

    protected void loadCaptionAlignment(Form resultComponent, Element element) {
        String captionAlignment = element.attributeValue("captionAlignment");
        if (!Strings.isNullOrEmpty(captionAlignment)) {
            resultComponent.setCaptionAlignment(Form.CaptionAlignment.valueOf(captionAlignment));
        }
    }

    @Nullable
    protected String loadChildrenCaptionWidth(Element element) {
        String childCaptionWidth = element.attributeValue("childrenCaptionWidth");
        if (!Strings.isNullOrEmpty(childCaptionWidth)) {
            if (childCaptionWidth.startsWith(MessageTools.MARK)) {
                childCaptionWidth = loadResourceString(childCaptionWidth);
            }
            if (childCaptionWidth.endsWith("px")) {
                childCaptionWidth = childCaptionWidth.substring(0, childCaptionWidth.indexOf("px"));
            }

            return childCaptionWidth;
        }

        return null;
    }

    protected void loadChildrenCaptionWidth(Form resultComponent, Element element) {
        String childrenCaptionWidth = loadChildrenCaptionWidth(element);
        if (childrenCaptionWidth != null) {
            resultComponent.setChildrenCaptionWidth(Integer.parseInt(childrenCaptionWidth));
        }
    }

    protected void loadChildrenCaptionWidth(Form resultComponent, Element element, int colIndex) {
        String childrenCaptionWidth = loadChildrenCaptionWidth(element);
        if (childrenCaptionWidth != null) {
            resultComponent.setChildrenCaptionWidth(colIndex, Integer.parseInt(childrenCaptionWidth));
        }
    }
}
