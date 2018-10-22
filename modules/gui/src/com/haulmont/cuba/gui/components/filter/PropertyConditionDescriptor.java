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

import com.google.common.base.Strings;
import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.cuba.core.entity.annotation.SystemLevel;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.components.filter.condition.FilterConditionUtils;

@com.haulmont.chile.core.annotations.MetaClass(name = "sec$PropertyConditionInfo")
@SystemLevel
public class PropertyConditionDescriptor extends ConditionDescriptor {
    protected Messages messages = AppBeans.get(Messages.NAME);

    public static ConditionDescriptor of() {
        //noinspection IncorrectCreateEntity
        return new PropertyConditionDescriptor();
    }

    public static ConditionDescriptor of(String name) {
        //noinspection IncorrectCreateEntity
        ConditionDescriptor conditionInfo = new PropertyConditionDescriptor();
        conditionInfo.setName(name);
        return conditionInfo;
    }

    @Override
    public String getLocCaption() {
        if (locCaption == null) {
            if (!Strings.isNullOrEmpty(caption)) {
                locCaption = messages.getTools().loadString(messagesPack, caption);
            } else {
                locCaption = FilterConditionUtils.getPropertyLocCaption(entityMetaClass, name);
            }
        }
        return locCaption;
    }

    @Override
    public String getTreeCaption() {
        if (!Strings.isNullOrEmpty(this.caption)) {
            return getLocCaption();
        } else {
            MetaPropertyPath mpp = entityMetaClass.getPropertyPath(name);
            return mpp != null ? messages.getTools().getPropertyCaption(entityMetaClass, name) : name;
        }
    }
}