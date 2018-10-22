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

package com.haulmont.cuba.gui.components.filter.addcondition;

import com.google.common.base.Strings;
import com.haulmont.bali.datastruct.Node;
import com.haulmont.bali.datastruct.Tree;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.core.global.MetadataTools;
import com.haulmont.cuba.core.global.Security;
import com.haulmont.cuba.core.global.filter.ConditionType;
import com.haulmont.cuba.gui.ComponentsHelper;
import com.haulmont.cuba.gui.components.Filter;
import com.haulmont.cuba.gui.components.FilterImplementation;
import com.haulmont.cuba.gui.components.filter.ConditionsTree;
import com.haulmont.cuba.gui.components.filter.FilterConditions;
import com.haulmont.cuba.gui.components.filter.FilterConditionsProvider;
import com.haulmont.cuba.gui.components.filter.ConditionDescriptor;
import com.haulmont.cuba.gui.components.filter.PropertyConditionDescriptor;
import com.haulmont.cuba.gui.components.sys.ValuePathHelper;
import com.haulmont.cuba.gui.screen.FrameOwner;
import com.haulmont.cuba.security.entity.EntityAttrAccess;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Builds a {@link com.haulmont.bali.datastruct.Tree} of {@link ConditionDescriptor}.
 * These descriptors are used in a new condition dialog.
 */
@Component(ConditionDescriptorsTreeBuilderAPI.NAME)
@Scope("prototype")
public class ConditionDescriptorsTreeBuilder implements ConditionDescriptorsTreeBuilderAPI {

    private static final Logger log = LoggerFactory.getLogger(ConditionDescriptorsTreeBuilder.class);

    protected static final List<String> defaultExcludedProps = Collections.unmodifiableList(Collections.singletonList("version"));
    protected static final String CUSTOM_CONDITIONS_PERMISSION = "cuba.gui.filter.customConditions";

    protected Filter filter;
    protected String filterComponentName;

    protected String sourceQuery;
    protected MetaClass entityMetaClass;
    protected String storeName;
    protected String messagesPack;

    protected boolean hideDynamicAttributes;
    protected boolean hideCustomConditions;
    protected boolean excludePropertiesRecursively;
    protected List<String> excludedProperties;
    protected int hierarchyDepth;

    protected ConditionsTree conditionsTree;
    protected FilterConditions filterConditions;

    @Inject
    protected Security security;
    @Inject
    protected Messages messages;
    @Inject
    protected MetadataTools metadataTools;
    @Inject
    protected FilterConditionsProvider filterConditionsProvider;

    /**
     * @param filter                filter
     * @param hierarchyDepth        max level of properties hierarchy
     * @param hideDynamicAttributes hide dynamic attributes conditions from wizard
     */
    public ConditionDescriptorsTreeBuilder(Filter filter,
                                           int hierarchyDepth,
                                           boolean hideDynamicAttributes,
                                           boolean hideCustomConditions,
                                           ConditionsTree conditionsTree) {
        this.filter = filter;
        this.hierarchyDepth = hierarchyDepth;
        this.hideDynamicAttributes = hideDynamicAttributes;
        this.hideCustomConditions = hideCustomConditions;

        this.conditionsTree = conditionsTree;

        FilterImplementation filterImpl = (FilterImplementation) filter;
        Class<? extends FrameOwner> controllerClass = filter.getFrame().getFrameOwner().getClass();
        this.entityMetaClass = filterImpl.getEntityMetaClass();
        this.sourceQuery = filterImpl.getSourceQuery();
        this.messagesPack = controllerClass.getPackage().getName();

        this.filterComponentName = getFilterComponentName();
        this.excludedProperties = new ArrayList<>();
    }

    @PostConstruct
    protected void init() {
        storeName = metadataTools.getStoreName(entityMetaClass);
        filterConditions = filterConditionsProvider.getFilterConditions(entityMetaClass.getName());
    }

    @Override
    public Tree<ConditionDescriptor> build() {
        Tree<ConditionDescriptor> tree = new Tree<>();
        List<ConditionDescriptor> propertyConditions = new ArrayList<>();
        List<ConditionDescriptor> customDescriptors = new ArrayList<>();

        boolean propertiesExplicitlyDefined = false;
        if (filter.getXmlDescriptor() != null) {
            for (Element element : filter.getXmlDescriptor().elements()) {
                if ("properties".equals(element.getName())) {
                    addMultiplePropertyDescriptors(element, propertyConditions);
                    propertiesExplicitlyDefined = true;
                } else if ("property".equals(element.getName())) {
                    propertyConditions.add(createPropertyCondition(element));
                    propertiesExplicitlyDefined = true;
                } else if ("custom".equals(element.getName())) {
                    customDescriptors.add(createCustomCondition(element));
                    propertiesExplicitlyDefined = true;
                } else {
                    throw new UnsupportedOperationException("Element not supported: " + element.getName());
                }
            }
        }

        if (!propertiesExplicitlyDefined) {
            addMultiplePropertyDescriptors(".*", "", propertyConditions);
        }

        propertyConditions.sort(new ConditionDescriptorComparator());
        customDescriptors.sort(new ConditionDescriptorComparator());

        int currentDepth = 0;
        Node<ConditionDescriptor> propertyHeaderNode = new Node<>(ConditionDescriptor.of("propertyConditions")
                .setLocCaption(messages.getMainMessage("filter.addCondition.propertyConditions")));
        Node<ConditionDescriptor> customHeaderNode = new Node<>(ConditionDescriptor.of("customConditions")
                .setLocCaption(messages.getMainMessage("filter.addCondition.customConditions")));

        for (ConditionDescriptor propertyCondition : propertyConditions) {
            MetaClass propertyDsMetaClass = propertyCondition.getEntityMetaClass();
            MetaPropertyPath propertyPath = propertyDsMetaClass.getPropertyPath(propertyCondition.getName());
            if (propertyPath == null) {
                log.error("Property path for {} of metaClass {} not found",
                        propertyCondition.getName(), propertyDsMetaClass.getName());
                continue;
            }

            MetaProperty metaProperty = propertyPath.getMetaProperty();
            MetaClass propertyEnclosingMetaClass = metadataTools.getPropertyEnclosingMetaClass(propertyPath);

            if (isPropertyAllowed(propertyEnclosingMetaClass, metaProperty)
                    && !excludedProperties.contains(metaProperty.getName())
                    && (filter.getPropertiesFilterPredicate() == null || filter.getPropertiesFilterPredicate().test(propertyPath))) {
                Node<ConditionDescriptor> node = new Node<>(propertyCondition);
                propertyHeaderNode.addChild(node);

                if (currentDepth < hierarchyDepth) {
                    recursivelyFillPropertyDescriptors(node, currentDepth);
                }
            }
        }

        for (ConditionDescriptor customDescriptor : customDescriptors) {
            Node<ConditionDescriptor> node = new Node<>(customDescriptor);
            customHeaderNode.addChild(node);
        }

        List<Node<ConditionDescriptor>> rootNodes = new ArrayList<>();

        rootNodes.add(propertyHeaderNode);

        if (!customDescriptors.isEmpty()) {
            rootNodes.add(customHeaderNode);
        }

        if (!hideCustomConditions && security.isSpecificPermitted(CUSTOM_CONDITIONS_PERMISSION)) {
            rootNodes.add(new Node<>(createNewCustomCondition()));
        }

        if (!hideDynamicAttributes && filterConditions.supportsDynamicAttributes(entityMetaClass)) {
            rootNodes.add(new Node<>(createDynamicAttributeCondition(null)));
        }

        if (filterConditions.supportsFts(entityMetaClass)) {
            rootNodes.add(new Node<>(createFtsCondition()));
        }

        tree.setRootNodes(rootNodes);

        return tree;
    }

    protected void recursivelyFillPropertyDescriptors(Node<ConditionDescriptor> parentNode, int currentDepth) {
        currentDepth++;
        List<ConditionDescriptor> conditions = new ArrayList<>();
        String propertyPropertyPath = parentNode.getData().getName();
        MetaPropertyPath mpp = entityMetaClass.getPropertyPath(propertyPropertyPath);
        if (mpp == null) {
            throw new RuntimeException("Unable to find property " + propertyPropertyPath);
        }

        MetaProperty metaProperty = mpp.getMetaProperty();
        if (metaProperty.getRange().isClass()
                && (metadataTools.getCrossDataStoreReferenceIdProperty(storeName, metaProperty) == null)) {
            MetaClass childMetaClass = metaProperty.getRange().asClass();
            for (MetaProperty property : childMetaClass.getProperties()) {
                if (isPropertyAllowed(childMetaClass, property)) {
                    String propertyPath = mpp.toString() + "." + property.getName();
                    if (excludedProperties.contains(propertyPath)
                            || excludePropertiesRecursively && excludedProperties.contains(property.getName())
                            || filter.getPropertiesFilterPredicate() != null
                            && !filter.getPropertiesFilterPredicate().test(entityMetaClass.getPropertyPath(propertyPath))) {
                        continue;
                    }
                    conditions.add(createPropertyCondition(propertyPath));
                }
            }
        }

        conditions.sort(new ConditionDescriptorComparator());

        for (ConditionDescriptor descriptor : conditions) {
            Node<ConditionDescriptor> newNode = new Node<>(descriptor);
            parentNode.addChild(newNode);
            if (currentDepth < hierarchyDepth) {
                recursivelyFillPropertyDescriptors(newNode, currentDepth);
            }
        }

        if (metaProperty.getRange().isClass()) {
            MetaClass childMetaClass = metaProperty.getRange().asClass();
            if (!hideDynamicAttributes && filterConditions.supportsDynamicAttributes(childMetaClass)) {
                Node<ConditionDescriptor> newNode = new Node<>(createDynamicAttributeCondition(propertyPropertyPath));
                parentNode.addChild(newNode);
            }
        }
    }

    protected void addMultiplePropertyDescriptors(Element element, List<ConditionDescriptor> conditions) {
        String includeRe = element.attributeValue("include");
        String excludeRe = element.attributeValue("exclude");
        addMultiplePropertyDescriptors(includeRe, excludeRe, conditions);

        if (element.attribute("excludeProperties") != null) {
            String excludeProperties = element.attributeValue("excludeProperties");
            if (StringUtils.isNotEmpty(excludeProperties)) {
                excludedProperties = Arrays.asList(excludeProperties.replace(" ", "").split(","));
            }

            String excludeRecursively = element.attributeValue("excludeRecursively");
            if (excludeProperties != null && !excludeProperties.isEmpty()) {
                excludePropertiesRecursively = Boolean.parseBoolean(excludeRecursively);
            }
        }
    }

    protected void addMultiplePropertyDescriptors(String includeRe, String excludeRe, List<ConditionDescriptor> conditions) {
        List<String> includedProps = new ArrayList<>();
        Pattern inclPattern = Pattern.compile(includeRe.replace(" ", ""));

        for (MetaProperty property : entityMetaClass.getProperties()) {
            if (!isPropertyAllowed(entityMetaClass, property)) {
                continue;
            }

            if (inclPattern.matcher(property.getName()).matches()) {
                includedProps.add(property.getName());
            }
        }

        Pattern exclPattern = null;
        if (!StringUtils.isBlank(excludeRe)) {
            exclPattern = Pattern.compile(excludeRe.replace(" ", ""));
        }

        for (String propertyName : includedProps) {
            if (exclPattern == null || !exclPattern.matcher(propertyName).matches()) {
                conditions.add(createPropertyCondition(propertyName));
            }
        }
    }

    protected boolean isPropertyAllowed(MetaClass metaClass, MetaProperty property) {
        return security.isEntityAttrPermitted(metaClass, property.getName(), EntityAttrAccess.VIEW)
                && !metadataTools.isSystemLevel(property)           // exclude system level attributes
                && (metadataTools.isPersistent(property)            // exclude transient properties
                || (metadataTools.getCrossDataStoreReferenceIdProperty(storeName, property) != null))
                && !defaultExcludedProps.contains(property.getName())
                && !(byte[].class.equals(property.getJavaType()))
                && !property.getRange().getCardinality().isMany();  // exclude ToMany
    }

    protected String getFilterComponentName() {
        String filterComponentName = ComponentsHelper.getFilterComponentPath(filter);
        String[] parts = ValuePathHelper.parse(filterComponentName);
        if (parts.length > 1) {
            filterComponentName = ValuePathHelper.format(Arrays.copyOfRange(parts, 1, parts.length));
        }
        return filterComponentName;
    }

    protected ConditionDescriptor createFtsCondition() {
        return ConditionDescriptor.of("fts")
                .setConditionType(ConditionType.FTS)
                .setFilterComponentName(filterComponentName)
                .setEntityMetaClass(entityMetaClass)
                .setLocCaption(messages.getMainMessage("filter.addCondition.ftsCondition"));
    }

    protected ConditionDescriptor createPropertyCondition(String name) {
        return PropertyConditionDescriptor.of(name)
                .setConditionType(ConditionType.PROPERTY)
                .setFilterComponentName(filterComponentName)
                .setEntityMetaClass(entityMetaClass)
                .setMessagesPack(messagesPack)
                .setSourceQuery(sourceQuery);
    }

    protected ConditionDescriptor createPropertyCondition(Element element) {
        return PropertyConditionDescriptor.of(element.attributeValue("name"))
                .setCaption(element.attributeValue("caption"))
                .setConditionType(ConditionType.PROPERTY)
                .setElement(element)
                .setFilterComponentName(filterComponentName)
                .setEntityMetaClass(entityMetaClass)
                .setMessagesPack(messagesPack)
                .setSourceQuery(sourceQuery);
    }

    protected ConditionDescriptor createCustomCondition(Element element) {
        ConditionDescriptor conditionInfo = ConditionDescriptor.of(element.attributeValue("name"))
                .setCaption(element.attributeValue("caption"))
                .setConditionType(ConditionType.CUSTOM)
                .setElement(element)
                .setFilterComponentName(filterComponentName)
                .setEntityMetaClass(entityMetaClass)
                .setMessagesPack(messagesPack)
                .setSourceQuery(sourceQuery);
        if (!Strings.isNullOrEmpty(conditionInfo.getCaption())) {
            conditionInfo.setLocCaption(messages.getTools().loadString(messagesPack, conditionInfo.getCaption()));
        }
        return conditionInfo;
    }

    protected ConditionDescriptor createNewCustomCondition() {
        return ConditionDescriptor.of(RandomStringUtils.randomAlphabetic(10))
                .setFilterComponentName(filterComponentName)
                .setConditionType(ConditionType.CUSTOM)
                .setEntityMetaClass(entityMetaClass)
                .setMessagesPack(messagesPack)
                .setSourceQuery(sourceQuery)
                .setLocCaption(messages.getMainMessage("filter.customCondition.new"))
                .setTreeCaption(messages.getMainMessage("filter.customConditionCreator"))
                .setShowEditor(true);
    }

    protected ConditionDescriptor createDynamicAttributeCondition(String parentPropertyPath) {
        return ConditionDescriptor.of(RandomStringUtils.randomAlphabetic(10))
                .setConditionType(ConditionType.RUNTIME_PROPERTY)
                .setParentPropertyPath(parentPropertyPath)
                .setFilterComponentName(filterComponentName)
                .setEntityMetaClass(entityMetaClass)
                .setMessagesPack(messagesPack)
                .setLocCaption(messages.getMainMessage("filter.dynamicAttributeConditionCreator"))
                .setShowEditor(true);
    }

    protected class ConditionDescriptorComparator implements Comparator<ConditionDescriptor> {
        @Override
        public int compare(ConditionDescriptor cd1, ConditionDescriptor cd2) {
            return cd1.getLocCaption().compareTo(cd2.getLocCaption());
        }
    }
}