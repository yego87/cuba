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

import com.haulmont.cuba.core.global.BeanLocator;
import com.haulmont.cuba.core.global.Events;
import com.haulmont.cuba.gui.Navigation;
import com.haulmont.cuba.gui.Screens;
import com.haulmont.cuba.gui.components.DialogWindow;
import com.haulmont.cuba.gui.components.RootWindow;
import com.haulmont.cuba.gui.components.Window;
import com.haulmont.cuba.gui.config.WindowConfig;
import com.haulmont.cuba.gui.navigation.UriState;
import com.haulmont.cuba.gui.navigation.UriStateChangedEvent;
import com.haulmont.cuba.gui.screen.EditorScreen;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.sys.UiControllerDefinition.PageDefinition;
import com.haulmont.cuba.web.AppUI;
import com.haulmont.cuba.web.WebConfig;
import com.haulmont.cuba.web.gui.UrlHandlingMode;
import com.haulmont.cuba.web.gui.WebWindow;
import com.haulmont.cuba.web.navigation.IdToBase64Converter;
import com.haulmont.cuba.web.navigation.UrlTools;
import com.vaadin.server.Page;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

@Scope(UIScope.NAME)
@Component(Navigation.NAME)
public class WebNavigation implements Navigation {

    protected static final int MAX_NESTED_ROUTES = 2;

    private static final Logger log = LoggerFactory.getLogger(WebNavigation.class);

    protected AppUI ui;

    @Inject
    protected WindowConfig windowConfig;

    @Inject
    protected Events events;

    @Inject
    protected BeanLocator beanLocator;

    @Inject
    protected WebConfig webConfig;

    public WebNavigation(AppUI ui) {
        this.ui = ui;
    }

    @Override
    public void pushState(Screen screen, Map<String, String> uriParams, boolean fireStateChanged) {
        if (UrlHandlingMode.NATIVE != webConfig.getUrlHandlingMode()) {
            log.debug("Navigation bean invocations are ignored for {} URL handling mode", webConfig.getUrlHandlingMode());
            return;
        }

        UriState oldUriState = fireStateChanged ? getState() : null;

        String navigationState = buildNavState(screen, uriParams);

        screen.getScreenContext().getNavigationInfo()
                .update(navigationState, uriParams);

        Page.getCurrent().setUriFragment(navigationState, false);

        UriState newUriState = getState();
        ui.getHistory().push(newUriState);

        if (fireStateChanged) {
            fireStateChange(oldUriState, newUriState);
        }
    }

    @Override
    public void replaceState(Screen screen, Map<String, String> uriParams, boolean fireStateChanged) {
        if (UrlHandlingMode.NATIVE != webConfig.getUrlHandlingMode()) {
            log.debug("Navigation bean invocations are ignored for {} URL handling mode", webConfig.getUrlHandlingMode());
            return;
        }

        UriState oldUriState = fireStateChanged ? getState() : null;

        String navigationState = buildNavState(screen, uriParams);

        screen.getScreenContext().getNavigationInfo()
                .update(navigationState, uriParams);

        Page.getCurrent().replaceState("#" + navigationState);

        if (fireStateChanged) {
            fireStateChange(oldUriState, getState());
        }
    }

    @Override
    public UriState getState() {
        if (UrlHandlingMode.NATIVE != webConfig.getUrlHandlingMode()) {
            log.debug("Navigation bean invocations are ignored for {} URL handling mode", webConfig.getUrlHandlingMode());
            return UriState.empty();
        }
        return UrlTools.parseState(Page.getCurrent().getUriFragment());
    }

    protected void fireStateChange(UriState oldState, UriState newState) {
        if (!Objects.equals(oldState, newState)) {
            events.publish(new UriStateChangedEvent(oldState, newState));
        }
    }

    protected String buildNavState(Screen screen, Map<String, String> urlParams) {
        StringBuilder state = new StringBuilder();

        if (screen.getWindow() instanceof RootWindow) {
            state.append(getRoute(screen));
        } else {
            Screen rootScreen = getScreens().getOpenedScreens().getRootScreen();
            state.append(getRoute(rootScreen));

            String stateMark = getScreenStateMark(screen);
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
            return buildScreenRoute(screen);
        }
    }

    protected String buildDialogRoute(Screen dialog) {
        PageDefinition page = getPage(dialog);
        if (page == null) {
            return buildScreenRoute(getCurrentScreen());
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

        String contextRoute = buildScreenRoute(currentScreen);
        return StringUtils.isNotEmpty(dialogRoute) ? contextRoute + "/" + dialogRoute
                : contextRoute;
    }

    protected String buildScreenRoute(Screen screen) {
        if (screen == null) {
            return "";
        }

        Iterator<Window> breadCrumbsScreens = ((TabWindowContainer) screen.getWindow()
                .unwrapComposition(com.vaadin.ui.Component.class)
                .getParent())
                .getBreadCrumbs()
                .getWindows()
                .iterator();

        StringBuilder state = new StringBuilder();
        int depth = 0;

        while (breadCrumbsScreens.hasNext() && depth < MAX_NESTED_ROUTES) {
            Screen nestedScreen = breadCrumbsScreens.next().getFrameOwner();
            String route = formNestedScreenRoute(state.toString(), nestedScreen);

            if (!state.toString().isEmpty() && !route.isEmpty()) {
                state.append('/');
            }
            state.append(route);

            depth++;
        }

        return state.toString();
    }

    protected String formNestedScreenRoute(String state, Screen screen) {
        String screenRoute = getRoute(screen);

        if (screen instanceof EditorScreen) {
            int slashIdx = screenRoute.indexOf('/');
            if (slashIdx > 0) {
                String editorContext = screenRoute.substring(0, slashIdx + 1);
                if (state.endsWith(editorContext)) {
                    screenRoute = screenRoute.substring(slashIdx + 1);
                }
            }
        }

        return screenRoute;
    }

    protected String buildParamsString(Screen screen, Map<String, String> urlParams) {
        String route = getRoute(screen);
        if (StringUtils.isEmpty(route)) {
            log.info("There's no route for screen {}. Ignore URL params");
            return "";
        }

        Map<String, String> params = new LinkedHashMap<>();

        if (screen instanceof EditorScreen) {
            Object entityId = ((EditorScreen) screen).getEditedEntity().getId();
            String base64Id = IdToBase64Converter.serialize(entityId);

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
            return screen.getScreenContext().getWindowInfo().getPageDefinition();
        }
        return null;
    }

    protected Screen getCurrentScreen() {
        Iterator<Screen> screens = getScreens().getOpenedScreens().getCurrentBreadcrumbs()
                .iterator();
        return screens.hasNext() ? screens.next() : null;
    }

    protected Screens getScreens() {
        return ui.getScreens();
    }

    protected String getScreenStateMark(Screen screen) {
        return String.valueOf(((WebWindow) screen.getWindow()).getStateMark());
    }
}
