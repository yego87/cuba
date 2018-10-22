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

import com.haulmont.cuba.gui.components.filter.condition.FtsCondition;
import com.haulmont.cuba.gui.components.filter.ConditionDescriptor;
import org.dom4j.Element;

public class FtsConditionFactory extends AbstractConditionFactory<FtsCondition> {

    @Override
    public FtsCondition create(ConditionDescriptor descriptor, Element element) {
        FtsCondition condition = create();

        assignCommonParams(descriptor, condition);

        assignFilterParams(descriptor, condition);

        //todo: assign parameter

        return null;
    }

    @Override
    public FtsCondition create() {
        //noinspection IncorrectCreateEntity
        FtsCondition condition = new FtsCondition();
        condition.setJavaClass(String.class);

        return condition;
    }
}
