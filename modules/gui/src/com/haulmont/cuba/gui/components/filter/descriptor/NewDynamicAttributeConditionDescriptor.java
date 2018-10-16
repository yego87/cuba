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

package com.haulmont.cuba.gui.components.filter.descriptor;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.entity.annotation.SystemLevel;
import com.haulmont.cuba.gui.components.filter.condition.AbstractCondition;
import com.haulmont.cuba.gui.components.filter.condition.DynamicAttributesCondition;
import org.apache.commons.lang3.RandomStringUtils;

@com.haulmont.chile.core.annotations.MetaClass(name = "sec$DynamicAttributesConditionCreator")
@SystemLevel
public class NewDynamicAttributeConditionDescriptor extends AbstractJPQLConditionDescriptor {

    protected String propertyPath;

    public NewDynamicAttributeConditionDescriptor(String filterComponentName,
                                                  MetaClass metaClass,
                                                  String propertyPath) {
        super(RandomStringUtils.randomAlphabetic(10), filterComponentName, metaClass, null);
        this.propertyPath = propertyPath;
        this.locCaption = messages.getMainMessage("filter.dynamicAttributeConditionCreator");
    }

    @Override
    public AbstractCondition createCondition() {
        //noinspection IncorrectCreateEntity
        return new DynamicAttributesCondition(this, propertyPath);
    }

    @Override
    public Class getJavaClass() {
        return null;
    }

    @Override
    public String getEntityParamWhere() {
        return null;
    }

    @Override
    public String getEntityParamView() {
        return null;
    }
}