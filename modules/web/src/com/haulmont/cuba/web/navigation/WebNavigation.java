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

import com.haulmont.cuba.core.global.Events;
import com.haulmont.cuba.gui.components.RootWindow;
import com.haulmont.cuba.gui.components.Window;
import com.haulmont.cuba.gui.config.WindowConfig;
import com.haulmont.cuba.gui.navigation.Navigation;
import com.haulmont.cuba.gui.navigation.UriStateChangedEvent;
import com.haulmont.cuba.gui.screen.EditorScreen;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.sys.UiControllerDefinition.PageDefinition;
import com.haulmont.cuba.web.AppUI;
import com.haulmont.cuba.web.sys.TabWindowContainer;
import com.vaadin.server.Page;
import com.vaadin.ui.HasComponents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Component(Navigation.NAME)
public class WebNavigation implements Navigation {

    private static final Logger log = LoggerFactory.getLogger(WebNavigation.class);

    protected static final String SHEBANG = "#!";
    protected static final int MAX_NESTED_ROUTES = 2;

    @Inject
    protected WindowConfig windowConfig;

    @Inject
    protected Events events;

    @Override
    public void pushState(Screen screen, Map<String, String> uriParams, boolean fireStateChanged) {
        UriState oldState = fireStateChanged ? getState() : null;

        String navState = buildNavState(screen, uriParams);
        if (navState == null) {
            throw new RuntimeException("New nav state is null");
        }

        Page.getCurrent().setUriFragment("!" + navState, false);

        if (fireStateChanged) {
            UriState state = getState();
            events.publish(new UriStateChangedEvent(oldState, state));
        }
    }

    @Override
    public void replaceState(Screen screen, Map<String, String> uriParams, boolean fireStateChanged) {
        UriState oldState = fireStateChanged ? getState() : null;

        String navState = buildNavState(screen, uriParams);
        if (navState == null) {
            throw new RuntimeException("New nav state is null");
        }

        Page.getCurrent().replaceState(SHEBANG + navState);

        if (fireStateChanged) {
            UriState state = getState();
            events.publish(new UriStateChangedEvent(oldState, state));
        }
    }

    @Override
    public UriState getState() {
        // TODO: omfg, check, refactor, etc
        String uriFragment = Page.getCurrent().getUriFragment();
        if (uriFragment == null || uriFragment.isEmpty() || "!".equals(uriFragment)) {
            return null;
        }
        uriFragment = uriFragment.substring(1);

        int slashIdx = uriFragment.indexOf('/');
        if (slashIdx < 0) {
            int paramsIdx = uriFragment.indexOf('?');
            if (paramsIdx < 0) {
                return new UriState(uriFragment, null, null);
            } else {
                String root = uriFragment.substring(0, paramsIdx);
                Map<String, String> params = extractParams(uriFragment.substring(paramsIdx + 1));

                return new UriState(root, null, params);
            }
        }

        String root = uriFragment.substring(0, slashIdx);
        if (uriFragment.length() <= slashIdx + 1) {
            return new UriState(root, null, null);
        }
        String screenAndParams = uriFragment.substring(slashIdx + 1);

        int paramsIdx = screenAndParams.indexOf('?');
        if (paramsIdx < 0) {
            return new UriState(root, screenAndParams, null);
        }
        String screen = screenAndParams.substring(0, paramsIdx);

        if (screenAndParams.length() <= paramsIdx + 1) {
            return new UriState(root, screen, null);
        }
        String paramString = screenAndParams.substring(paramsIdx + 1);
        Map<String, String> params = extractParams(paramString);

        return new UriState(root, screen, params);
    }

    protected Map<String, String> extractParams(String paramsString) {
        String[] splittedParams = paramsString.split("&");
        Map<String, String> paramsMap = new HashMap<>(splittedParams.length);

        for (String splittedParam : splittedParams) {
            String[] param = splittedParam.split("=");
            paramsMap.put(param[0], param[1]);
        }

        return paramsMap;
    }

    protected String buildNavState(Screen screen, Map<String, String> urlParams) {
        StringBuilder state = new StringBuilder();

        if (screen.getWindow() instanceof RootWindow) {
            state.append(getRoute(getPage(screen)));
        } else {
            RootWindow topLevelWindow = AppUI.getCurrent().getTopLevelWindow();
            Screen rootScreen = topLevelWindow.getFrameOwner();

            state.append(getRoute(getPage(rootScreen)));

            // TODO: append UI state id

            String compositeState = buildCompositeState(screen);

            if (compositeState == null || compositeState.isEmpty()) {
                return null;
            }

            state.append('/')
                    .append(compositeState);
        }

        state.append(buildParamsString(screen, urlParams));

        return state.toString();
    }

    protected String buildCompositeState(Screen screen) {
        com.vaadin.ui.Component screenComposition = screen.getWindow().unwrapComposition(com.vaadin.ui.Component.class);
        HasComponents parent = screenComposition.getParent();

        if (!(parent instanceof TabWindowContainer)) {
            return getRoute(getPage(screen));
        }

        StringBuilder state = new StringBuilder();
        int depth = 0;
        Iterator<Window> iterator = ((TabWindowContainer) parent).getBreadCrumbs().getWindows().iterator();

        while (iterator.hasNext() && depth < MAX_NESTED_ROUTES) {
            if (depth > 0) {
                state.append('/');
            }

            depth++;
            Screen _screen = iterator.next().getFrameOwner();
            String route = getRoute(getPage(_screen));

            if (_screen instanceof EditorScreen) {
                route = squashEditorRoute(state.toString(), route);
            }

            state.append(route);
        }
        return state.toString();
    }

    // Helpers

    protected String squashEditorRoute(String state, String route) {
        int slashIdx = route.indexOf('/');
        if (slashIdx <= 0) {
            return route;
        }

        String commonPart = route.substring(0, slashIdx + 1);
        return state.endsWith(commonPart) ? route.substring(slashIdx + 1) : route;
    }

    protected String buildParamsString(Screen screen, Map<String, String> params) {
        StringBuilder sb = null;

        if (screen instanceof EditorScreen) {
            EditorScreen editor = (EditorScreen) screen;
            String base64Id = IdToBase64Converter.serialize(editor.getEditedEntity().getId());

            sb = new StringBuilder("?")
                    .append("id=")
                    .append(base64Id);
        }

        if (params == null || params.isEmpty()) {
            return sb == null ? "" : sb.toString();
        }

        boolean paramAdded = false;
        if (sb == null) {
            sb = new StringBuilder("?");
        } else {
            paramAdded = true;
        }

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

    protected String getRoute(PageDefinition pageDefinition) {
        return pageDefinition == null ? "" : pageDefinition.getRoute();
    }

    protected PageDefinition getPage(Screen screen) {
        return screen.getScreenContext().getWindowInfo().getPageDefinition();
    }
}
