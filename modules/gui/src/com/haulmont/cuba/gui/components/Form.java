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

package com.haulmont.cuba.gui.components;

import java.util.Collection;

public interface Form extends Component, Component.BelongToFrame, Component.HasCaption, Component.HasIcon,
        ComponentContainer, Component.Editable, EditableChangeNotifier, HasContextHelp, ChildEditableController {

    String NAME = "form";

    void add(Component childComponent, int column);

    void add(Component childComponent, int column, int row);

    Collection<Component> getComponents(int column);

    Component getComponent(int column, int row);

    CaptionAlignment getChildCaptionAlignment();

    void setChildCaptionAlignment(CaptionAlignment captionAlignment);

    /**
     * @return fixed field caption width
     */
    int getChildCaptionWidth();

    /**
     * Set fixed captions width. Set -1 to use auto size.
     *
     * @param width fixed field caption width
     */
    void setChildCaptionWidth(int width);

    /**
     * @param column column index
     * @return fixed field caption width for column {@code colIndex}
     */
    int getChildCaptionWidth(int column);

    /**
     * Set fixed field captions width for column {@code colIndex}. Set -1 to use auto size.
     *
     * @param column column index
     * @param width  width
     */
    void setChildCaptionWidth(int column, int width);

    /**
     * @return column count
     */
    int getColumns();

    /**
     * Set column count.
     *
     * @param columns column count
     */
    void setColumns(int columns);

    // TODO: gg, row / col span

    /**
     * Caption alignment.
     */
    enum CaptionAlignment {
        LEFT,
        TOP
    }
}
