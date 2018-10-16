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

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.app.dynamicattributes.DynamicAttributes;
import com.haulmont.cuba.core.global.FtsConfigHelper;
import com.haulmont.cuba.gui.components.filter.descriptor.AbstractConditionDescriptor;
import com.haulmont.cuba.gui.components.filter.descriptor.CustomConditionCreator;
import com.haulmont.cuba.gui.components.filter.descriptor.CustomConditionDescriptor;
import com.haulmont.cuba.gui.components.filter.descriptor.PropertyConditionDescriptor;
import org.dom4j.Element;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
@Component(JPQLFilterConditions.NAME)
public class JPQLFilterConditions implements FilterConditions {

    public static final String NAME = "cuba_JPQLFilterConditions";

    @Inject
    protected DynamicAttributes dynamicAttributes;

    @Override
    public String getStoreType() {
        return "jpql";
    }

    @Override
    public boolean supportsFts(MetaClass metaClass) {
        return FtsConfigHelper.getEnabled();
    }

    @Override
    public boolean supportsDynamicAttributes(MetaClass metaClass) {
        return !dynamicAttributes.getAttributesForMetaClass(metaClass).isEmpty();
    }

    @Override
    public AbstractConditionDescriptor createNewPropertyDescriptor(String name,
                                                                   String filterComponentName,
                                                                   MetaClass metaClass,
                                                                   String messagesPack,
                                                                   String sourceQuery) {
        //noinspection IncorrectCreateEntity
        return new PropertyConditionDescriptor(name,
                filterComponentName,
                metaClass,
                messagesPack,
                sourceQuery);
    }

    @Override
    public AbstractConditionDescriptor createExistingPropertyDescriptor(Element element,
                                                                        String filterComponentName,
                                                                        MetaClass metaClass,
                                                                        String messagesPack,
                                                                        String sourceQuery) {
        //noinspection IncorrectCreateEntity
        return new PropertyConditionDescriptor(element,
                filterComponentName,
                metaClass,
                messagesPack,
                sourceQuery);
    }

    @Override
    public AbstractConditionDescriptor createNewCustomDescriptor(String filterComponentName,
                                                                 MetaClass metaClass,
                                                                 String sourceQuery) {
        //noinspection IncorrectCreateEntity
        return new CustomConditionCreator(filterComponentName, metaClass);
    }

    @Override
    public AbstractConditionDescriptor createExistingCustomDescriptor(Element element,
                                                                      String filterComponentName,
                                                                      MetaClass metaClass,
                                                                      String messagesPack,
                                                                      String sourceQuery) {
        //noinspection IncorrectCreateEntity
        return new CustomConditionDescriptor(element,
                filterComponentName,
                metaClass,
                messagesPack);
    }
}
