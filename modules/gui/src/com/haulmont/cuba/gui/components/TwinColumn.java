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

import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.gui.components.compatibility.TwinColumnStyleProviderAdapter;

import java.util.Collection;
import java.util.function.Function;

public interface TwinColumn<V> extends OptionsField<Collection<V>, V> {

    String NAME = "twinColumn";

    /**
     * Gets the number of columns for the component.
     *
     * @see #setWidth(String)
     * @deprecated "Columns" does not reflect the exact number of characters that will be displayed. Use
     * {@link #getWidth()} instead.
     */
    @Deprecated
    int getColumns();

    /**
     * Sets the width of the component so that it displays approximately the given number of letters in each of the
     * two selects.
     *
     * @param columns the number of columns to set.
     * @deprecated "Columns" does not reflect the exact number of characters that will be displayed. Use
     * {@link #setWidth(String)} instead.
     */
    @Deprecated
    void setColumns(int columns);

    int getRows();
    void setRows(int rows);

    /**
     * @param styleProvider style provider
     * @deprecated use {@link #setOptionStyleProvider(Function)} instead
     */
    @Deprecated
    default void setStyleProvider(StyleProvider styleProvider) {
        setOptionStyleProvider(new TwinColumnStyleProviderAdapter<>(styleProvider));
    }

    void setAddAllBtnEnabled(boolean enabled);
    boolean isAddAllBtnEnabled();

    /**
     * Set caption for the left column.
     *
     * @param leftColumnCaption
     */
    void setLeftColumnCaption(String leftColumnCaption);
    /**
     * Return caption of the left column.
     *
     * @return caption text or null if not set.
     */
    String getLeftColumnCaption();

    /**
     * Set caption for the right column.
     *
     * @param rightColumnCaption
     */
    void setRightColumnCaption(String rightColumnCaption);
    /**
     * Return caption of the right column.
     *
     * @return caption text or null if not set.
     */
    String getRightColumnCaption();

    /**
     * Sets option style provider. It defines a style for each value.
     *
     * @param optionStyleProvider option style provider function
     */
    void setOptionStyleProvider(Function<OptionStyleItem<V>, String> optionStyleProvider);

    /**
     * @return option style provider function
     */
    Function<OptionStyleItem<V>, String> getOptionStyleProvider();

    /**
     * @deprecated use {@link #setOptionStyleProvider(Function)}
     */
    @Deprecated
    interface StyleProvider {
        @Deprecated
        String getStyleName(Entity item, Object property, boolean selected);

        /**
         * @deprecated Will be removed in 7.0
         */
        @Deprecated
        String getItemIcon(Entity item, boolean selected);
    }

    /**
     * Represents item for option style provider.
     *
     * @param <V> option type
     */
    class OptionStyleItem<V> {
        protected V item;
        protected boolean selected;

        public OptionStyleItem(V item, boolean selected) {
            this.item = item;
            this.selected = selected;
        }

        public boolean isSelected() {
            return selected;
        }

        public V getItem() {
            return item;
        }
    }
}