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

package com.haulmont.cuba.gui.components.data;

import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.gui.components.AggregationInfo;

import java.util.Collection;
import java.util.Map;

public interface AggregatableTableItems<E extends Entity> extends TableItems<E> {

    Map<AggregationInfo, String> aggregate(AggregationInfo[] aggregationInfos, Collection<?> itemIds);
}
