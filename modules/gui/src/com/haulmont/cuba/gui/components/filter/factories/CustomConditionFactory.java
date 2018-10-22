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

import com.google.common.base.Strings;
import com.haulmont.cuba.core.global.Scripting;
import com.haulmont.cuba.core.global.filter.Op;
import com.haulmont.cuba.gui.components.filter.condition.CustomCondition;
import com.haulmont.cuba.gui.components.filter.ConditionDescriptor;
import org.dom4j.Element;

import javax.inject.Inject;

public class CustomConditionFactory extends AbstractConditionFactory<CustomCondition> {

    @Inject
    protected Scripting scripting;

    @Override
    public CustomCondition create(ConditionDescriptor descriptor, Element element) {
        CustomCondition condition = create();

        assignCommonParams(descriptor, condition);

        assignFilterParams(descriptor, condition);

        assignOperator(condition, element);

        assignJavaClass(condition, element);

        assignInExpr(condition, element);

        assignParamWhere(condition, element);

        assignParamView(condition, element);

        condition.setWhere(element.getText());

        assignJoin(condition, element);

        return condition;
    }

    @Override
    public CustomCondition create() {
        //noinspection IncorrectCreateEntity
        return new CustomCondition();
    }

    protected void assignOperator(CustomCondition condition, Element element) {
        if (element != null) {
            String operatorType = element.attributeValue("operatorType");
            if (!Strings.isNullOrEmpty(operatorType)) {
                condition.setOperator(Op.valueOf(operatorType));
            }
        }
    }

    protected void assignJavaClass(CustomCondition condition, Element element) {
        if (element != null) {
            String className = element.attributeValue("paramClass");
            if (className != null) {
                condition.setJavaClass(scripting.loadClass(className));
            } else {
                condition.setUnary(true);
            }
        } else {
            condition.setJavaClass(String.class);
        }
    }

    protected void assignJoin(CustomCondition condition, Element element) {
        Element joinElement = element.element("join");
        if (joinElement != null) {
            condition.setJoin(joinElement.getText());
        } else {
            //for backward compatibility
            condition.setJoin(element.attributeValue("join"));
        }
    }
}
