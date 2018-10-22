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

package com.haulmont.cuba.gui.components.filter;

import com.google.common.base.MoreObjects;
import com.haulmont.chile.core.annotations.MetaProperty;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.entity.BaseUuidEntity;
import com.haulmont.cuba.core.entity.annotation.SystemLevel;
import com.haulmont.cuba.core.global.filter.ConditionType;
import org.dom4j.Element;

/**
 * Class that encapsulates common filter condition descriptor behaviour. Condition descriptors are used for
 * generating condition objects.
 */
@com.haulmont.chile.core.annotations.MetaClass(name = "sec$ConditionDescriptor")
@SystemLevel
public class ConditionDescriptor extends BaseUuidEntity {

    private static final long serialVersionUID = 7975507640064754778L;

    protected String name;
    protected String caption;
    protected String locCaption;
    protected String treeCaption;

    protected String filterComponentName;
    protected MetaClass entityMetaClass;
    protected String messagesPack;

    protected Element element;
    protected String sourceQuery;
    protected String parentPropertyPath;
    protected ConditionType conditionType;

    protected boolean showEditor;

    public static ConditionDescriptor of() {
        //noinspection IncorrectCreateEntity
        return new ConditionDescriptor();
    }

    public static ConditionDescriptor of(String name) {
        //noinspection IncorrectCreateEntity
        ConditionDescriptor conditionInfo = new ConditionDescriptor();
        conditionInfo.setName(name);
        return conditionInfo;
    }

    public String getName() {
        return name;
    }

    public ConditionDescriptor setName(String name) {
        this.name = name;
        return this;
    }

    public String getCaption() {
        return caption;
    }

    public ConditionDescriptor setCaption(String caption) {
        this.caption = caption;
        return this;
    }

    public String getMessagesPack() {
        return messagesPack;
    }

    public ConditionDescriptor setMessagesPack(String messagesPack) {
        this.messagesPack = messagesPack;
        return this;
    }

    public String getFilterComponentName() {
        return filterComponentName;
    }

    public ConditionDescriptor setFilterComponentName(String filterComponentName) {
        this.filterComponentName = filterComponentName;
        return this;
    }

    public MetaClass getEntityMetaClass() {
        return entityMetaClass;
    }

    public ConditionDescriptor setEntityMetaClass(MetaClass entityMetaClass) {
        this.entityMetaClass = entityMetaClass;
        return this;
    }

    public String getSourceQuery() {
        return sourceQuery;
    }

    public ConditionDescriptor setSourceQuery(String sourceQuery) {
        this.sourceQuery = sourceQuery;
        return this;
    }

    @MetaProperty
    public String getLocCaption() {
        return locCaption;
    }

    public ConditionDescriptor setLocCaption(String locCaption) {
        this.locCaption = locCaption;
        return this;
    }

    public Element getElement() {
        return element;
    }

    public ConditionDescriptor setElement(Element element) {
        this.element = element;
        return this;
    }

    public ConditionType getConditionType() {
        return conditionType;
    }

    public ConditionDescriptor setConditionType(ConditionType conditionType) {
        this.conditionType = conditionType;
        return this;
    }

    public String getParentPropertyPath() {
        return parentPropertyPath;
    }

    public ConditionDescriptor setParentPropertyPath(String parentPropertyPath) {
        this.parentPropertyPath = parentPropertyPath;
        return this;
    }

    public boolean isShowEditor() {
        return showEditor;
    }

    public ConditionDescriptor setShowEditor(boolean showEditor) {
        this.showEditor = showEditor;
        return this;
    }

    @MetaProperty
    public String getTreeCaption() {
        return treeCaption != null ? treeCaption : getLocCaption();
    }

    public ConditionDescriptor setTreeCaption(String treeCaption) {
        this.treeCaption = treeCaption;
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .toString();
    }
}