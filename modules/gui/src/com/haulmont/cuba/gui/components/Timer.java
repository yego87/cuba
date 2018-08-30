/*
 * Copyright (c) 2008-2016 Haulmont.
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
 *
 */
package com.haulmont.cuba.gui.components;

public interface Timer extends Component.HasXmlDescriptor, Component.BelongToFrame {

    boolean isRepeating();
    void setRepeating(boolean repeating);

    /**
     * @return delay in milliseconds.
     */
    int getDelay();

    /**
     * @param delayMs delay in milliseconds.
     */
    void setDelay(int delayMs);

    /**
     * Starts timer. If timer is already started call will be ignored.
     */
    void start();

    /**
     * Stops timer if it is running.
     */
    void stop();

    /**
     * Listener for timer events.
     */
    interface ActionListener {
        void timerAction(Timer timer);
    }

    /**
     * Listener for timer stop event.
     */
    interface StopListener {
        void timerStopped(Timer timer);
    }

    void addActionListener(ActionListener listener);
    void removeActionListener(ActionListener listener);

    void addStopListener(StopListener listener);
    void removeStopListener(StopListener listener);
}