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

package com.haulmont.cuba.web.toolkit.ui.client.aggregation;

import com.google.gwt.dom.client.InputElement;

public class AggregationInputFieldInfo {

    protected String oldValue;
    protected int columnIndex;
    protected InputElement inputElement;

    public AggregationInputFieldInfo(String oldValue, int columnIndex, InputElement inputElement) {
        this.oldValue = oldValue;
        this.inputElement = inputElement;
        this.columnIndex = columnIndex;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public InputElement getInputElement() {
        return inputElement;
    }

    public void setInputElement(InputElement inputElement) {
        this.inputElement = inputElement;
    }

    public int getColumnIndex() {
        return columnIndex;
    }

    public void setColumnIndex(int columnIndex) {
        this.columnIndex = columnIndex;
    }
}