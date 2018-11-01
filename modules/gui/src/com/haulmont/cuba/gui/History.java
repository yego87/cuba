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

package com.haulmont.cuba.gui;

import com.haulmont.cuba.gui.navigation.NavigationState;

/**
 * This bean is intended to store and access local copy of opened screens history.
 * <p>
 * It is mainly used by UriChangeHandler to distinguish history transitions and navigation.
 * <p>
 * <b>Pay attention that manual history mutation can lead to errors.</b>
 */
public interface History {

    String NAME = "cuba_History";

    /**
     * Adds new history entry. Flushes all entries coming after getNow.
     * <p>
     * Mutates history.
     *
     * @param navigationState new history entry
     */
    void forward(NavigationState navigationState);

    /**
     * Performs "Back" transition through history.
     * <p>
     * Mutates history.
     *
     * @return previous history entry
     */
    NavigationState backward();

    /**
     * Doesn't mutate history.
     *
     * @return current history entry
     */
    NavigationState getNow();

    /**
     * Doesn't mutate history.
     *
     * @return previous history entry
     */
    NavigationState getPrevious();

    /**
     * Doesn't mutate history.
     *
     * @return next history entry
     */
    NavigationState getNext();

    /**
     * Performs search for the given history entry in the past.
     * <p>
     * Doesn't mutate history.
     *
     * @param navigationState history entry
     * @return true if entry is found, false otherwise
     */
    boolean searchBackward(NavigationState navigationState);

    /**
     * Performs search for the given history entry in the future.
     * <p>
     * Doesn't mutate history.
     *
     * @param navigationState history entry
     * @return true if entry is found, false otherwise
     */
    boolean searchForward(NavigationState navigationState);

    /**
     * Checks whether history has the given entry.
     *
     * @param navigationState history entry
     * @return true if history has an entry, false otherwise
     */
    boolean has(NavigationState navigationState);
}
