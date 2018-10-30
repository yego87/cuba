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

package com.haulmont.cuba.web.sys;

import com.haulmont.bali.util.Preconditions;
import com.haulmont.cuba.gui.History;
import com.haulmont.cuba.gui.navigation.UriState;
import com.haulmont.cuba.web.AppUI;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

@Scope(UIScope.NAME)
@Component(History.NAME)
public class WebHistory implements History {

    protected AppUI ui;

    protected int now = -1;

    protected List<UriState> history = new LinkedList<>();

    public WebHistory(AppUI ui) {
        this.ui = ui;
    }

    @Override
    public void push(UriState uriState) {
        Preconditions.checkNotNullArgument(uriState);

        if (uriState.equals(now())) {
            return;
        }

        dropFutureEntries();

        history.add(++now, uriState);
    }

    @Override
    public UriState backward() {
        checkIfCanGoBackward();

        return history.get(--now);
    }

    @Override
    public UriState now() {
        UriState uriState = now < 0 ? null
                : history.get(now);

        if (uriState == null && now >= 0) {
            throw new IllegalStateException("History is broken");
        }

        return uriState;
    }

    @Override
    public UriState lookBackward() {
        return (now - 1) >= 0 ? history.get(now - 1) : null;
    }

    @Override
    public UriState lookForward() {
        return (now + 1) < history.size() ? history.get(now + 1) : null;
    }

    @Override
    public boolean searchBackward(UriState uriState) {
        for (int i = now - 1; i >= 0; i--) {
            if (Objects.equals(history.get(i), uriState)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean searchForward(UriState uriState) {
        for (int i = now + 1; i < history.size(); i++) {
            if (Objects.equals(history.get(i), uriState)) {
                return true;
            }
        }
        return false;
    }

    protected void dropFutureEntries() {
        for (int i = now + 1; i < history.size(); i++) {
            history.set(i, null);
        }
    }

    protected void checkIfCanGoBackward() {
        if (now - 1 < 0) {
            throw new IllegalStateException("There is no past entries in history.");
        }
    }
}
