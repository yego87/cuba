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

package com.haulmont.cuba.web.navigation;

import com.haulmont.cuba.gui.components.RootWindow;
import com.haulmont.cuba.gui.config.WindowConfig;
import com.haulmont.cuba.gui.config.WindowInfo;
import com.haulmont.cuba.gui.navigation.Navigation;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.sys.UiControllerDefinition.PageDefinition;
import com.haulmont.cuba.gui.sys.UiDescriptorUtils;
import com.haulmont.cuba.web.AppUI;
import com.haulmont.cuba.web.sys.TabWindowContainer;
import com.haulmont.cuba.web.sys.WindowBreadCrumbs;
import com.vaadin.server.Page;
import com.vaadin.ui.HasComponents;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Map;
import java.util.stream.Collectors;

@Component(Navigation.NAME)
public class WebNavigation implements Navigation {

    protected static final String SHEBANG = "#!";

    @Inject
    protected WindowConfig windowConfig;

    @Override
    public void pushState(Screen screen, Map<String, String> urlParams) {
        String navState = buildNavState(screen, urlParams);

        Page.getCurrent().setUriFragment("!" + navState, false);
    }

    @Override
    public void replaceState(Screen screen, Map<String, String> urlParams) {
        String navState = buildNavState(screen, urlParams);

        Page.getCurrent().replaceState(SHEBANG + navState);
    }

    protected String buildNavState(Screen screen, Map<String, String> urlParams) {
        StringBuilder state = new StringBuilder();

        if (screen.getWindow() instanceof RootWindow) {
            PageDefinition page = getPageDef(screen);
            state.append(page.getRoute())
                    .append(buildParamsString(urlParams));
        } else {
            RootWindow topLevelWindow = AppUI.getCurrent().getTopLevelWindow();

            Screen rootScreen = topLevelWindow.getFrameOwner();
            PageDefinition rootPageDef = getPageDef(rootScreen);
            state.append(rootPageDef.getRoute());

            // TODO: append UI state id

            state.append('/')
                    .append(buildCompositeState(screen));
        }

        return state.toString();
    }

    protected PageDefinition getPageDef(Screen screen) {
        PageDefinition page = screen.getScreenContext().getWindowInfo().getPageDefinition();

        if (page == null) {
            String screenId = UiDescriptorUtils.getInferredScreenId(null, null, screen.getClass().getName());
            WindowInfo windowInfo = windowConfig.findWindowInfo(screenId);
            if (windowInfo != null) {
                page = windowInfo.getPageDefinition();
            }
        }

        if (page == null) {
            throw new RuntimeException("Unable to find PageDefinition");
        }

        return page;
    }

    protected String buildCompositeState(Screen screen) {
        com.vaadin.ui.Component screenComposition = screen.getWindow().unwrapComposition(com.vaadin.ui.Component.class);
        HasComponents parent = screenComposition.getParent();

        if (parent instanceof TabWindowContainer) {
            WindowBreadCrumbs breadCrumbs = ((TabWindowContainer) parent).getBreadCrumbs();

            return breadCrumbs.getWindows().stream()
                    .map(w -> getPageDef(w.getFrameOwner()).getRoute())
                    .collect(Collectors.joining("/"));
        }

        return "";
    }

    protected String buildParamsString(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder("?");
        boolean paramAdded = false;

        for (Map.Entry<String, String> paramEntry : params.entrySet()) {
            if (!paramAdded) {
                paramAdded = true;
            } else {
                sb.append('&');
            }
            sb.append(String.format("%s=%s", paramEntry.getKey(), paramEntry.getValue()));
        }

        return sb.toString();
    }
}
