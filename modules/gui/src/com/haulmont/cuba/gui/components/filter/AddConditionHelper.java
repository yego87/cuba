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

package com.haulmont.cuba.gui.components.filter;

import com.haulmont.bali.datastruct.Tree;
import com.haulmont.bali.util.ParamsMap;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.WindowManagerProvider;
import com.haulmont.cuba.gui.components.Filter;
import com.haulmont.cuba.gui.components.Window;
import com.haulmont.cuba.gui.components.filter.addcondition.AddConditionWindow;
import com.haulmont.cuba.gui.components.filter.addcondition.ConditionDescriptorsTreeBuilderAPI;
import com.haulmont.cuba.gui.components.filter.condition.AbstractCondition;
import com.haulmont.cuba.gui.components.filter.condition.CustomCondition;
import com.haulmont.cuba.gui.components.filter.condition.DynamicAttributesCondition;
import com.haulmont.cuba.gui.config.WindowConfig;
import com.haulmont.cuba.gui.config.WindowInfo;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.haulmont.cuba.gui.WindowManager.OpenType;

/**
 * Helper class that does a sequence of steps for adding new condition:
 * <p>
 * <ol> <li>opens add condition dialog</li> <li>if necessary opens new condition editor</li> <li>invokes a handler
 * passing new condition to it</li> </ol>
 */
public class AddConditionHelper {

    public static final int PROPERTIES_HIERARCHY_DEPTH = 2;

    protected Filter filter;
    protected Handler handler;
    protected boolean hideDynamicAttributes;
    protected boolean hideCustomConditions;

    protected WindowManager windowManager = AppBeans.get(WindowManagerProvider.class).get();
    protected WindowConfig windowConfig = AppBeans.get(WindowConfig.class);

    public AddConditionHelper(Filter filter, Handler handler) {
        this(filter, false, false, handler);
    }

    public AddConditionHelper(Filter filter,
                              boolean hideDynamicAttributes,
                              boolean hideCustomConditions,
                              Handler handler) {
        this.filter = filter;
        this.handler = handler;
        this.hideDynamicAttributes = hideDynamicAttributes;
        this.hideCustomConditions = hideCustomConditions;
    }

    public interface Handler {
        void handle(AbstractCondition condition);
    }

    /**
     * Opens AddCondition window. When condition is selected/created a {@code Handler#handle} method will be called
     *
     * @param conditionsTree conditions tree is necessary for custom condition editing. It is used for suggestion of
     *                       other component names in 'param where' field.
     */
    public void addCondition(final ConditionsTree conditionsTree) {
        Map<String, Object> params = new HashMap<>();
        ConditionDescriptorsTreeBuilderAPI descriptorsTreeBuilder = AppBeans.getPrototype(ConditionDescriptorsTreeBuilderAPI.NAME,
                filter,
                PROPERTIES_HIERARCHY_DEPTH,
                hideDynamicAttributes,
                hideCustomConditions,
                conditionsTree);
        Tree<ConditionDescriptor> descriptorsTree = descriptorsTreeBuilder.build();
        params.put("descriptorsTree", descriptorsTree);
        WindowInfo windowInfo = windowConfig.getWindowInfo("addCondition");
        AddConditionWindow window = (AddConditionWindow) windowManager.openWindow(windowInfo, OpenType.DIALOG, params);
        window.addCloseListener(actionId -> {
            if (Window.COMMIT_ACTION_ID.equals(actionId)) {
                Collection<ConditionDescriptor> descriptors = window.getDescriptors();
                if (descriptors != null) {
                    for (ConditionDescriptor descriptor : descriptors) {
                        _addCondition(descriptor, conditionsTree);
                    }
                }
            }
        });
    }

    protected void _addCondition(ConditionDescriptor conditionInfo, ConditionsTree conditionsTree) {
        ConditionFactory conditionFactory = null;
        AbstractCondition condition = conditionFactory.create(conditionInfo, conditionInfo.getElement());
        if (conditionInfo.isShowEditor()) {
            WindowInfo windowInfo = windowConfig.getWindowInfo(getConditionEditor(condition));
            windowManager.openWindow(windowInfo, OpenType.DIALOG, getEditorParams(condition, conditionsTree))
                    .addCloseListener(actionId -> {
                        if (Window.COMMIT_ACTION_ID.equals(actionId)) {
                            handler.handle(condition);
                        }
                    });
        } else {
            handler.handle(condition);
        }
    }

    protected Map<String, Object> getEditorParams(AbstractCondition condition, ConditionsTree conditionsTree) {
        return ParamsMap.of("condition", condition,
                "conditionsTree", conditionsTree);
    }

    protected String getConditionEditor(AbstractCondition condition) {
        //for compatibility with old condition screens
        if (condition instanceof CustomCondition) {
            return "customConditionEditor";
        } else if (condition instanceof DynamicAttributesCondition) {
            return "dynamicAttributesConditionEditor";
        } else {
            return String.format("%s.edit", condition.getMetaClass().getName());
        }
    }
}
