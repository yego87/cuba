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

package com.haulmont.cuba.gui.navigation;

import org.springframework.context.ApplicationEvent;

public class UriStateChangedEvent extends ApplicationEvent {

    private static final long serialVersionUID = 2542146882366256709L;

    protected final UriState oldState;
    protected final UriState state;

    public UriStateChangedEvent(UriState oldState, UriState state) {
        super(state);
        this.oldState = oldState;
        this.state = state;
    }

    public UriState getSource() {
        return (UriState) super.getSource();
    }

    public UriState getOldState() {
        return oldState;
    }

    public UriState getState() {
        return state;
    }
}
