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

package com.haulmont.cuba.gui.components.filter.condition;

import com.google.common.base.Strings;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.annotation.SystemLevel;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.QueryTransformer;
import com.haulmont.cuba.core.global.QueryTransformerFactory;
import com.haulmont.cuba.gui.components.filter.ConditionParamBuilder;
import com.haulmont.cuba.gui.components.filter.Param;
import com.haulmont.cuba.gui.components.filter.descriptor.AbstractJPQLConditionDescriptor;
import org.dom4j.Element;

@com.haulmont.chile.core.annotations.MetaClass(name = "sec$AbstractJPQLCondition")
@SystemLevel
public abstract class AbstractJPQLCondition extends AbstractCondition {
    protected String entityAlias;
    protected String entityParamWhere;
    protected String entityParamView;

    public AbstractJPQLCondition() {
    }

    protected AbstractJPQLCondition(AbstractJPQLCondition other) {
        super(other);
        this.entityAlias = other.entityAlias;
        this.entityParamWhere = other.entityParamWhere;
        this.entityParamView = other.entityParamView;
    }

    protected AbstractJPQLCondition(Element element, String messagesPack, String filterComponentName, MetaClass metaClass) {
        super(element, messagesPack, filterComponentName, metaClass);
        entityParamWhere = element.attributeValue("paramWhere");
        entityParamView = element.attributeValue("paramView");
    }

    protected AbstractJPQLCondition(AbstractJPQLConditionDescriptor descriptor) {
        super(descriptor);
        entityParamWhere = descriptor.getEntityParamWhere();
        entityParamView = descriptor.getEntityParamView();

        ConditionParamBuilder paramBuilder = AppBeans.get(ConditionParamBuilder.class);
        param = paramBuilder.createParam(this);
    }

    public void toXml(Element element, Param.ValueProperty valueProperty) {
        super.toXml(element, valueProperty);
        if (param != null) {
            if (entityParamWhere != null)
                element.addAttribute("paramWhere", entityParamWhere);
            if (entityParamView != null)
                element.addAttribute("paramView", entityParamView);
        }
    }

    public String getEntityParamView() {
        return entityParamView;
    }

    public void setEntityParamView(String entityParamView) {
        this.entityParamView = entityParamView;
    }

    public String getEntityParamWhere() {
        return entityParamWhere;
    }

    public void setEntityParamWhere(String entityParamWhere) {
        this.entityParamWhere = entityParamWhere;
    }

    public String getEntityParamQuery() {
        String query = null;
        Metadata metadata = AppBeans.get(Metadata.class);
        Class clazz = paramClass == null ? javaClass : paramClass;
        if (clazz != null && Entity.class.isAssignableFrom(clazz)) {
            MetaClass paramMetaClass = metadata.getClass(clazz);
            if (paramMetaClass != null) {
                query = String.format("select e from %s e", paramMetaClass.getName());
                if (!Strings.isNullOrEmpty(entityParamWhere)) {
                    QueryTransformerFactory queryTransformerFactory = AppBeans.get(QueryTransformerFactory.NAME);
                    QueryTransformer queryTransformer = queryTransformerFactory.transformer(query);
                    queryTransformer.addWhere(entityParamWhere);
                    query = queryTransformer.getResult();
                }
            }
        }
        return query;
    }
}
