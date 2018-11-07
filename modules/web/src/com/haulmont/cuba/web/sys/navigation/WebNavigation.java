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

package com.haulmont.cuba.web.sys.navigation;

import com.haulmont.cuba.core.global.Events;
import com.haulmont.cuba.gui.sys.navigation.Navigation;
import com.haulmont.cuba.gui.components.DialogWindow;
import com.haulmont.cuba.gui.components.RootWindow;
import com.haulmont.cuba.gui.navigation.NavigationState;
import com.haulmont.cuba.gui.screen.EditorScreen;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.sys.UiControllerDefinition.PageDefinition;
import com.haulmont.cuba.web.AppUI;
import com.haulmont.cuba.web.WebConfig;
import com.haulmont.cuba.web.gui.UrlHandlingMode;
import com.haulmont.cuba.web.gui.WebWindow;
import com.vaadin.server.Page;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

import static com.haulmont.bali.util.Preconditions.checkNotNullArgument;
import static com.haulmont.cuba.gui.screen.UiControllerUtils.getScreenContext;

public class WebNavigation implements Navigation {

    protected static final int MAX_NESTED_ROUTES = 2;

    private static final Logger log = LoggerFactory.getLogger(WebNavigation.class);

    @Inject
    protected Events events;

    @Inject
    protected WebConfig webConfig;

    protected AppUI ui;

    public WebNavigation(AppUI ui) {
        this.ui = ui;
    }

    @Override
    public void pushState(Screen screen, Map<String, String> urlParams) {
        if (notSuitableUrlHandlingMode()) {
            return;
        }

        checkNotNullArgument(screen);
        checkNotNullArgument(urlParams);

        changeStateInternal(screen, urlParams, true);
    }

    @Override
        public void replaceState(Screen screen, Map<String, String> urlParams) {
        if (notSuitableUrlHandlingMode()) {
            return;
        }

        checkNotNullArgument(screen);
        checkNotNullArgument(urlParams);

        changeStateInternal(screen, urlParams, false);
    }

    protected void changeStateInternal(Screen screen, Map<String, String> urlParams, boolean pushState) {
        NavigationState oldNavState = getState();
        String newState = buildNavState(screen, urlParams);

        if (pushState && !externalNavigation(oldNavState, newState)) {
            Page.getCurrent().setUriFragment(newState, false);
        } else {
            Page.getCurrent().replaceState("#" + newState);
        }

        NavigationState newNavState = getState();
        getScreenContext(screen).getRouteInfo().update(newNavState);

        if (pushState) {
            ui.getHistory().forward(newNavState);
        }
    }

    @Override
    public NavigationState getState() {
        if (notSuitableUrlHandlingMode()) {
            return NavigationState.empty();
        }
        return UrlTools.parseState(Page.getCurrent().getUriFragment());
    }

    protected String buildNavState(Screen screen, Map<String, String> urlParams) {
        StringBuilder state = new StringBuilder();

        if (screen.getWindow() instanceof RootWindow) {
            state.append(getRoute(screen));
        } else {
            Screen rootScreen = ui.getScreens().getOpenedScreens().getRootScreen();
            state.append(getRoute(rootScreen));

            String stateMark = getStateMark(screen);
            state.append('/').append(stateMark);

            String nestedRoute = buildNestedRoute(screen);
            if (nestedRoute != null && !nestedRoute.isEmpty()) {
                state.append('/').append(nestedRoute);
            }
        }

        state.append(buildParamsString(screen, urlParams));

        return state.toString();
    }

    protected String buildNestedRoute(Screen screen) {
        if (screen.getWindow() instanceof DialogWindow) {
            return buildDialogRoute(screen);
        } else {
            return buildCurrentScreenRoute();
        }
    }

    protected String buildDialogRoute(Screen dialog) {
        PageDefinition page = getPage(dialog);
        if (page == null) {
            return buildCurrentScreenRoute();
        }

        String dialogRoute = page.getRoute();
        if (page.getParent() == null) {
            return dialogRoute;
        }

        Screen currentScreen = getCurrentScreen();
        boolean openedInContext = currentScreen.getClass() == page.getParent();
        if (!openedInContext) {
            throw new IllegalStateException("Dialog is opened outside of its context");
        }

        String contextRoute = buildCurrentScreenRoute();
        return StringUtils.isNotEmpty(dialogRoute) ? contextRoute + "/" + dialogRoute
                : contextRoute;
    }

    protected String buildCurrentScreenRoute() {
        List<Screen> screens = new ArrayList<>(ui.getScreens().getOpenedScreens().getCurrentBreadcrumbs());
        Collections.reverse(screens);

        StringBuilder state = new StringBuilder();

        for (int i = 0; i < screens.size() && i < MAX_NESTED_ROUTES; i++) {
            String route = buildRoutePart(state.toString(), screens.get(i));
            if (StringUtils.isNotEmpty(state) && StringUtils.isNotEmpty(route)) {
                state.append('/');
            }
            state.append(route);
        }

        return state.toString();
    }

    // TODO: rewrite with parent prefix
    protected String buildRoutePart(String state, Screen screen) {
        String screenRoute = getRoute(screen);
        if (!(screen instanceof EditorScreen))
            return screenRoute;

        int slashIdx = screenRoute.indexOf('/');
        if (slashIdx <= 0) {
            return screenRoute;
        }

        String editorContext = screenRoute.substring(0, slashIdx);
        if (!state.endsWith(editorContext)) {
            return screenRoute;
        }

        return screenRoute.substring(slashIdx + 1);
    }

    protected String buildParamsString(Screen screen, Map<String, String> urlParams) {
        String route = getRoute(screen);
        if (StringUtils.isEmpty(route) && MapUtils.isNotEmpty(urlParams)) {
            log.info("There's no route for screen {}. Ignore URL params");
            return "";
        }

        Map<String, String> params = new LinkedHashMap<>();

        if (screen instanceof EditorScreen) {
            Object entityId = ((EditorScreen) screen).getEditedEntity().getId();
            String base64Id = UrlTools.serializeIdT(entityId);

            params.put("id", base64Id);
        }

        params.putAll(urlParams != null ? urlParams : Collections.emptyMap());

        String paramsString = params.entrySet()
                .stream()
                .map(param -> String.format("%s=%s", param.getKey(), param.getValue()))
                .collect(Collectors.joining("&"));

        return !paramsString.isEmpty() ? "?" + paramsString : "";
    }

    protected String getRoute(Screen screen) {
        PageDefinition page = getPage(screen);
        String route = page != null ? page.getRoute() : null;

        return route == null || route.isEmpty() ? "" : route;
    }

    protected PageDefinition getPage(Screen screen) {
        if (screen != null) {
            return getScreenContext(screen).getWindowInfo().getPageDefinition();
        }
        return null;
    }

    protected Screen getCurrentScreen() {
        Iterator<Screen> screens = ui.getScreens().getOpenedScreens().getCurrentBreadcrumbs()
                .iterator();
        return screens.hasNext() ? screens.next() : null;
    }

    protected String getStateMark(Screen screen) {
        return String.valueOf(((WebWindow) screen.getWindow()).getStateMark());
    }

    protected boolean externalNavigation(NavigationState requestedState, String newRoute) {
        if (requestedState == null) {
            return false;
        }
        NavigationState newNavigationState = UrlTools.parseState(newRoute);
        return !ui.getHistory().has(requestedState)
                && Objects.equals(requestedState.getRoot(), newNavigationState.getRoot())
                && Objects.equals(requestedState.getNestedRoute(), newNavigationState.getNestedRoute())
                && Objects.equals(requestedState.getParamsString(), newNavigationState.getParamsString());
    }

    protected boolean notSuitableUrlHandlingMode() {
        if (UrlHandlingMode.URL_ROUTES != webConfig.getUrlHandlingMode()) {
            log.debug("Navigation bean invocations are ignored for {} URL handling mode", webConfig.getUrlHandlingMode());
            return true;
        }
        return false;
    }
}
