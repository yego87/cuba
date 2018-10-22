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


import com.google.common.base.Preconditions;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.filter.Op;
import com.haulmont.cuba.core.global.filter.OpManager;
import com.haulmont.cuba.gui.components.filter.condition.PropertyCondition;
import com.haulmont.cuba.gui.components.filter.ConditionDescriptor;
import org.dom4j.Element;

import javax.inject.Inject;
import java.util.EnumSet;

public class PropertyConditionFactory extends AbstractConditionFactory<PropertyCondition> {

    @Inject
    protected Metadata metadata;

    @Inject
    protected OpManager opManager;

    @Override
    public PropertyCondition create(ConditionDescriptor descriptor, Element element) {
        PropertyCondition condition = create();

        assignCommonParams(descriptor, condition);

        assignFilterParams(descriptor, condition);

        MetaPropertyPath propertyPath = getMetaPropertyPath(descriptor.getEntityMetaClass(), descriptor.getName());

        assignOperator(condition, propertyPath);

        condition.setJavaClass(propertyPath.getMetaProperty().getJavaType());

        assignInExpr(condition, element);

        assignParamWhere(condition, element);

        assignParamView(condition, element);

        //todo: assign parameter

        return condition;
    }

    @Override
    public PropertyCondition create() {
        //noinspection IncorrectCreateEntity
        return new PropertyCondition();
    }

    protected void assignOperator(PropertyCondition condition, MetaPropertyPath propertyPath) {
        MetaClass propertyMetaClass = metadata.getTools().getPropertyEnclosingMetaClass(propertyPath);
        EnumSet<Op> ops = opManager.availableOps(propertyMetaClass, propertyPath.getMetaProperty());
        condition.setOperator(ops.iterator().next());
    }

    protected MetaPropertyPath getMetaPropertyPath(MetaClass entityMetaClass, String name) {
        MetaPropertyPath propertyPath = entityMetaClass.getPropertyPath(name);
        Preconditions.checkState(propertyPath != null,
                "Unable to find property '%s' in entity %s", name, entityMetaClass);
        return propertyPath;
    }
}
