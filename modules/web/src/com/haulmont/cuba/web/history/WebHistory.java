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

package com.haulmont.cuba.web.history;

import com.haulmont.cuba.gui.history.History;
import com.haulmont.cuba.gui.navigation.UriState;
import com.haulmont.cuba.web.sys.VaadinSessionScope;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;

@Component(History.NAME)
@Scope(VaadinSessionScope.NAME)
public class WebHistory implements History {

    protected int now = -1;

    protected List<UriState> history = new LinkedList<>();

    @Override
    public void push(UriState uriState) {
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
        if (now < 0) {
            throw new IllegalStateException("There's no history");
        }
        return history.get(now);
    }

    @Override
    public UriState lookBackward() {
        return (now - 1) >= 0 ?
                history.get(now - 1) : null;
    }

    @Override
    public UriState lookForward() {
        return (now + 1) < history.size() ?
                history.get(now + 1) : null;
    }

    @Override
    public boolean searchBackward(UriState uriState) {
        for (int i = now; i >= 0; i--) {
            if (history.get(i).equals(uriState)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean searchForward(UriState uriState) {
        for (int i = now; i < history.size(); i++) {
            if (history.get(i).equals(uriState)) {
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
            throw new IllegalStateException("There is no \"past\" entries in history.");
        }
    }
}
