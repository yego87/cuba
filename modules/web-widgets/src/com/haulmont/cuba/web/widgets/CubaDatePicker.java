/*
 * Copyright (c) 2008-2017 Haulmont.
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

package com.haulmont.cuba.web.widgets;

import com.vaadin.server.CompositeErrorMessage;
import com.vaadin.server.ErrorMessage;
import com.vaadin.server.UserError;
import com.vaadin.ui.InlineDateField;

public class CubaDatePicker extends InlineDateField {

    public CubaDatePicker() {
        // VAADIN8: gg,
//        setValidationVisible(false);
//        setShowBufferedSourceException(false);
    }

    @Override
    public ErrorMessage getErrorMessage() {
        return getComponentError();
    }

    @Override
    public ErrorMessage getComponentError() {
        ErrorMessage superError = super.getErrorMessage();
        if (!isReadOnly() && isRequiredIndicatorVisible() && isEmpty()) {
            ErrorMessage error = new UserError(getRequiredError());
            return new CompositeErrorMessage(superError, error);
        }
        return superError;
    }
}