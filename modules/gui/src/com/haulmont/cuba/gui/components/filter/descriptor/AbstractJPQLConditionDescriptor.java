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

package com.haulmont.cuba.gui.components.filter.descriptor;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.entity.annotation.SystemLevel;

@com.haulmont.chile.core.annotations.MetaClass(name = "sec$AbstractJPQLConditionDescriptor")
@SystemLevel
public abstract class AbstractJPQLConditionDescriptor extends AbstractConditionDescriptor {

    public AbstractJPQLConditionDescriptor(String name, String filterComponentName, MetaClass metaClass, String messagesPack) {
        super(name, filterComponentName, metaClass, messagesPack);
    }

    public abstract String getEntityParamWhere();

    public abstract String getEntityParamView();
}
