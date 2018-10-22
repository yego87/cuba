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

package com.haulmont.cuba.gui.components.filter.factories;

import com.haulmont.cuba.gui.components.filter.ConditionFactory;
import com.haulmont.cuba.gui.components.filter.condition.AbstractCondition;
import com.haulmont.cuba.gui.components.filter.condition.AbstractJPQLCondition;
import com.haulmont.cuba.gui.components.filter.ConditionDescriptor;
import org.dom4j.Element;

public abstract class AbstractConditionFactory<R extends AbstractCondition>
        implements ConditionFactory<R> {

    protected void assignCommonParams(ConditionDescriptor descriptor, R condition) {
        condition.setName(descriptor.getName());
        condition.setCaption(descriptor.getCaption());
        condition.setLocCaption(descriptor.getLocCaption());
    }

    protected void assignFilterParams(ConditionDescriptor descriptor, R condition) {
        condition.setFilterComponentName(descriptor.getFilterComponentName());
        condition.setEntityMetaClass(descriptor.getEntityMetaClass());
        condition.setMessagesPack(descriptor.getMessagesPack());
    }

    protected void assignInExpr(R condition, Element element) {
        if (element != null) {
            condition.setInExpr(Boolean.valueOf(element.attributeValue("inExpr")));
        }
    }

    protected void assignParamWhere(R condition, Element element) {
        if (element != null) {
            ((AbstractJPQLCondition)condition).setEntityParamWhere(element.attributeValue("paramWhere"));
        }
    }

    protected void assignParamView(R condition, Element element) {
        if (element != null) {
            ((AbstractJPQLCondition)condition).setEntityParamView(element.attributeValue("paramView"));
        }
    }
}
