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

import com.haulmont.cuba.core.global.BeanLocator;
import com.haulmont.cuba.core.global.Events;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.DialogWindow;
import com.haulmont.cuba.gui.components.RootWindow;
import com.haulmont.cuba.gui.components.Window;
import com.haulmont.cuba.gui.components.mainwindow.AppWorkArea;
import com.haulmont.cuba.gui.config.WindowConfig;
import com.haulmont.cuba.gui.history.History;
import com.haulmont.cuba.gui.navigation.Navigation;
import com.haulmont.cuba.gui.navigation.UriState;
import com.haulmont.cuba.gui.navigation.UriStateChangedEvent;
import com.haulmont.cuba.gui.screen.EditorScreen;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.sys.UiControllerDefinition.PageDefinition;
import com.haulmont.cuba.web.AppUI;
import com.haulmont.cuba.web.gui.WebWindow;
import com.haulmont.cuba.web.gui.components.mainwindow.WebAppWorkArea;
import com.haulmont.cuba.web.sys.TabWindowContainer;
import com.haulmont.cuba.web.sys.VaadinSessionScope;
import com.haulmont.cuba.web.sys.WindowBreadCrumbs;
import com.haulmont.cuba.web.widgets.TabSheetBehaviour;
import com.vaadin.server.Page;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

@Component(Navigation.NAME)
@Scope(VaadinSessionScope.NAME)
public class WebNavigation implements Navigation {

    protected static final String SHEBANG = "#!";

    // TODO: config property?
    protected static final int MAX_NESTED_ROUTES = 2;

    @Inject
    protected WindowConfig windowConfig;

    @Inject
    protected Events events;

    @Inject
    protected BeanLocator beanLocator;

    @Inject
    protected History history;

    @Override
    public void pushState(Screen screen, Map<String, String> uriParams, boolean fireStateChanged) {
        UriState oldState = fireStateChanged ? getState() : null;

        String navState = buildNavState(screen, uriParams);
        if (navState == null) {
            // TODO: check and remove
            throw new RuntimeException("New nav state is null");
        }

        Page.getCurrent().setUriFragment("!" + navState, false);

        history.push(getState());

        if (fireStateChanged) {
            fireStateChange(oldState);
        }
    }

    @Override
    public void replaceState(Screen screen, Map<String, String> uriParams, boolean fireStateChanged) {
        UriState oldState = fireStateChanged ? getState() : null;

        String navState = buildNavState(screen, uriParams);
        if (navState == null) {
            // TODO: check and remove
            throw new RuntimeException("New nav state is null");
        }

        Page.getCurrent().replaceState(SHEBANG + navState);

        if (fireStateChanged) {
            fireStateChange(oldState);
        }
    }

    @Override
    public UriState getState() {
        return UrlTools.parseState(Page.getCurrent().getUriFragment());
    }

    protected void fireStateChange(UriState oldState) {
        UriState state = getState();
        if (oldState == null || oldState.equals(state)) {
            return;
        }

        events.publish(new UriStateChangedEvent(oldState, state));
    }

    protected String buildNavState(Screen screen, Map<String, String> urlParams) {
        StringBuilder state = new StringBuilder();

        if (screen.getWindow() instanceof RootWindow) {
            state.append(getRoute(getPage(screen)));
        } else {
            Screen rootScreen = AppUI.getCurrent().getTopLevelWindow().getFrameOwner();
            state.append(getRoute(getPage(rootScreen)));

            String stateMark = getScreenStateMark(screen);
            String nestedRoute = buildNestedRoute(screen);
            if (nestedRoute == null || nestedRoute.isEmpty()) {
                return getState().toString();
            }

            state.append('/')
                    .append(stateMark)
                    .append('/')
                    .append(nestedRoute);
        }

        state.append(buildParamsString(screen, urlParams));

        return state.toString();
    }

    protected String getScreenStateMark(Screen screen) {
        return String.valueOf(((WebWindow) screen.getWindow()).getStateMark());
    }

    protected String buildNestedRoute(Screen screen) {
        if (screen.getWindow() instanceof DialogWindow) {
            return buildDialogRoute(screen);
        } else {
            return buildScreenRoute(screen);
        }
    }

    protected String buildDialogRoute(Screen screen) {
        PageDefinition page = getPage(screen);
        if (page == null) {
            return null;
        }
        if (page.getParent() == null) {
            return page.getRoute();
        }

        Screen openedScreen = getCurrentScreen();
        if (openedScreen.getClass() != page.getParent()) {
            Notifications.Notification notification = screen.getScreenContext()
                    .getNotifications()
                    .create();

            notification.setCaption("Dialog is opened out of its parent bounds");
            notification.setType(Notifications.NotificationType.WARNING);
            notification.show();

            return "";
        }

        return buildScreenRoute(screen) + "/" + page.getRoute();
    }

    protected String buildScreenRoute(Screen screen) {
        Iterator<Window> iterator = ((TabWindowContainer) screen.getWindow()
                .unwrapComposition(com.vaadin.ui.Component.class)
                .getParent())
                .getBreadCrumbs().getWindows().iterator();

        StringBuilder state = new StringBuilder();
        int depth = 0;
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

    protected String buildParamsString(Screen screen, Map<String, String> urlParams) {
        String idParam = "";
        if (screen instanceof EditorScreen) {
            EditorScreen editor = (EditorScreen) screen;
            String base64Id = IdToBase64Converter.serialize(editor.getEditedEntity().getId());
            idParam = "id=" + base64Id;
        }

        String params = "";
        if (urlParams != null && !urlParams.isEmpty()) {
            params = urlParams.entrySet()
                    .stream()
                    .map(paramPair -> String.format("%s=%s", paramPair.getKey(), paramPair.getValue()))
                    .collect(Collectors.joining("&"));
        }

        if (idParam.isEmpty() && params.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder("?");

        if (!idParam.isEmpty()) {
            sb.append(idParam);
        }

        if (!params.isEmpty()) {
            sb.append(sb.length() > 1 ? "&" : "")
                    .append(params);
        }

        return sb.toString();
    }

    protected String getRoute(PageDefinition pageDefinition) {
        return pageDefinition == null ? "" : pageDefinition.getRoute();
    }

    protected PageDefinition getPage(Screen screen) {
        return screen.getScreenContext().getWindowInfo().getPageDefinition();
    }

    // Copied from WebScreens
    protected Screen getCurrentScreen() {
        WebAppWorkArea workArea = getConfiguredWorkArea();
        TabWindowContainer windowContainer;
        if (workArea.getMode() == AppWorkArea.Mode.TABBED) {
            TabSheetBehaviour tabSheet = workArea.getTabbedWindowContainer().getTabSheetBehaviour();
            windowContainer = (TabWindowContainer) tabSheet.getSelectedTab();
        } else {
            windowContainer = (TabWindowContainer) workArea.getSingleWindowContainer().getComponent(0);
        }

        if (windowContainer == null) {
            throw new IllegalStateException("Window container is null");
        }

        WindowBreadCrumbs breadCrumbs = windowContainer.getBreadCrumbs();
        if (breadCrumbs == null) {
            throw new IllegalStateException("BreadCrumbs not found");
        }

        return breadCrumbs.getCurrentWindow().getFrameOwner();
    }

    // Copied from WebScreens
    protected WebAppWorkArea getConfiguredWorkArea() {
        Screen controller = AppUI.getCurrent().getTopLevelWindow().getFrameOwner();
        if (controller instanceof Window.HasWorkArea) {
            AppWorkArea workArea = ((Window.HasWorkArea) controller).getWorkArea();
            if (workArea != null) {
                return (WebAppWorkArea) workArea;
            }
        }
        throw new IllegalStateException("RootWindow does not have any configured work area");
    }
}
