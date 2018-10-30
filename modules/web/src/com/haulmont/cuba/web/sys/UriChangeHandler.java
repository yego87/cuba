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

import com.haulmont.bali.util.ParamsMap;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.History;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.Screens;
import com.haulmont.cuba.gui.WindowParams;
import com.haulmont.cuba.gui.components.Window;
import com.haulmont.cuba.gui.components.mainwindow.AppWorkArea;
import com.haulmont.cuba.gui.config.WindowConfig;
import com.haulmont.cuba.gui.config.WindowInfo;
import com.haulmont.cuba.gui.navigation.UriState;
import com.haulmont.cuba.gui.navigation.UriStateChangedEvent;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.gui.xml.layout.ScreenXmlLoader;
import com.haulmont.cuba.web.AppUI;
import com.haulmont.cuba.web.WebConfig;
import com.haulmont.cuba.web.controllers.ControllerUtils;
import com.haulmont.cuba.web.gui.UrlHandlingMode;
import com.haulmont.cuba.web.gui.WebWindow;
import com.haulmont.cuba.web.gui.components.mainwindow.WebAppWorkArea;
import com.haulmont.cuba.web.navigation.IdToBase64Converter;
import com.haulmont.cuba.web.widgets.TabSheetBehaviour;
import com.vaadin.server.Page;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

@Scope(UIScope.NAME)
@Component(UriChangeHandler.NAME)
public class UriChangeHandler {

    public static final String NAME = "cuba_UriChangeHandler";

    private static final Logger log = LoggerFactory.getLogger(UriChangeHandler.class);

    protected AppUI ui;

    @Inject
    protected WindowConfig windowConfig;

    @Inject
    protected ScreenXmlLoader screenXmlLoader;

    @Inject
    protected Scripting scripting;

    @Inject
    protected Metadata metadata;

    @Inject
    protected Messages messages;

    @Inject
    protected DataManager dataManager;

    @Inject
    protected WebConfig webConfig;

    public UriChangeHandler(AppUI ui) {
        this.ui = ui;
    }

    @Order(Events.LOWEST_PLATFORM_PRECEDENCE)
    @EventListener
    protected void handleUriStateChanged(UriStateChangedEvent event) {
        if (UrlHandlingMode.NATIVE != webConfig.getUrlHandlingMode()) {
            log.debug("UriChangeHandler is disabled for {} URL handling mode", webConfig.getUrlHandlingMode());
            return;
        }

        handleUriChange(event.getState());
    }

    public void handleUriChange() {
        if (UrlHandlingMode.NATIVE != webConfig.getUrlHandlingMode()) {
            log.debug("UriChangeHandler is disabled for {} URL handling mode", webConfig.getUrlHandlingMode());
            return;
        }

        UriState uriState = ui.getNavigation().getState();

        if (uriState == null) {
            reloadApp();
            return;
        }

        handleUriChange(uriState);
    }

    protected void handleUriChange(UriState uriState) {
        if (historyNavigation(uriState)) {
            handleHistoryNavigation(uriState);
        } else {
            handleScreenNavigation(uriState);
        }
    }

    protected boolean historyNavigation(UriState uriState) {
        return Objects.equals(uriState, getHistory().lookBackward())
                || Objects.equals(uriState, getHistory().lookForward());
    }

    protected void handleHistoryNavigation(UriState uriState) {
        if (Objects.equals(uriState, getHistory().lookBackward())) {
            handleHistoryBackward();
        } else {
            handleHistoryForward();
        }
    }

    protected void handleHistoryBackward() {
        UriState prevState = getHistory().lookBackward();
        Screen prevScreen = findScreenByState(prevState);

        if (prevScreen == null && StringUtils.isNotEmpty(prevState.getStateMark())) {
            revertHistoryBackward();

            ui.getNotifications().create()
                    .setCaption(messages.getMainMessage("navigation.unableToGoBackward"))
                    .setType(Notifications.NotificationType.TRAY)
                    .show();

            return;
        }

        Screen lastOpenedScreen = findActiveScreenByState(getHistory().now());
        if (lastOpenedScreen != null) {
            lastOpenedScreen.getWindow().getFrameOwner()
                    .close(FrameOwner.WINDOW_CLOSE_ACTION)
                    .then(this::proceedHistoryBackward)
                    .otherwise(this::revertHistoryBackward);
        } else {
            proceedHistoryBackward();
        }
    }

    protected void proceedHistoryBackward() {
        History history = getHistory();

        UriState nowState = history.now();
        UriState prevState = history.backward();

        focusScreen(findActiveScreenByState(prevState));

        boolean rootBackward = isRootRoute(prevState) && rootChanged(prevState);
        String state = !rootBackward ? prevState.asRoute() : nowState.asRoute();

        if (rootBackward) {
            reloadApp();
        } else {
            Page.getCurrent().replaceState("#" + state);
        }
    }

    protected void handleHistoryForward() {
        Screen currentScreen = findActiveScreenByState(getHistory().now());
        if (currentScreen == null) {
            currentScreen = getCurrentScreen();
        }

        String route = currentScreen.getScreenContext()
                .getNavigationInfo()
                .getResolvedRoute();
        Page.getCurrent().setUriFragment(route);

        ui.getNotifications().create()
                .setCaption(messages.getMainMessage("navigation.unableToGoForward"))
                .setType(Notifications.NotificationType.TRAY)
                .show();
    }

    protected void revertHistoryBackward() {
        Screen screen = findActiveScreenByState(getHistory().now());
        if (screen == null) {
            screen = getCurrentScreen();
        }
        String route = screen
                .getScreenContext()
                .getNavigationInfo()
                .getResolvedRoute();
        Page.getCurrent().setUriFragment(route);
    }

    protected void handleScreenNavigation(UriState uriState) {
        if (rootChanged(uriState)) {
            handleRootChange(uriState);
            return;
        }

        if (screenChanged(uriState)) {
            handleScreenChange(uriState);
            return;
        }

        if (paramsChanged(uriState)) {
            handleParamsChange(uriState);
        }
    }

    protected boolean rootChanged(UriState uriState) {
        Screen rootScreen = getScreens().getOpenedScreens().getRootScreenOrNull();
        if (rootScreen == null) {
            return false;
        }

        String resolvedRoute = rootScreen.getScreenContext()
                .getNavigationInfo()
                .getResolvedRoute();

        return !Objects.equals(resolvedRoute, uriState.getRoot());
    }

    @SuppressWarnings("unused")
    protected void handleRootChange(UriState state) {
    }

    protected boolean screenChanged(UriState uriState) {
        if (rootState(uriState) || !rootScreenHasWorkArea()) {
            return false;
        }

        Screen currentScreen = getCurrentScreen();
        if (currentScreen == null) {
            throw new IllegalStateException("There is no any screen in UI");
        }

        String currentScreenRoute = currentScreen
                .getScreenContext()
                .getNavigationInfo()
                .getResolvedRoute();

        return !Objects.equals(currentScreenRoute, uriState.asRoute());
    }

    protected Map<String, Object> createEditorScreenOptions(WindowInfo windowInfo, UriState state) {
        Element screenTemplate = screenXmlLoader.load(windowInfo.getTemplate(), windowInfo.getId(), Collections.emptyMap());

        Element dsElement = screenTemplate.element("dsContext")
                .element("datasource");

        /*
         * TODO: rewrite with reflection (see UiControllerReflectionInspector.getAddListenerMethodsNotCached)
         * for legacy screens and with "setEntityToEdit" for new screens
         */

        String entityClassFqn = dsElement.attributeValue("class");
        Class<?> entityClass = scripting.loadClassNN(entityClassFqn);
        Object id = IdToBase64Converter.deserialize(findIdType(entityClass), state.getParams().get("id"));

        LoadContext<?> ctx = new LoadContext(metadata.getClassNN(entityClass));
        ctx.setId(id);
        ctx.setView(dsElement.attributeValue("view"));

        Entity entity = dataManager.load(ctx);
        if (entity == null) {
            throw new RuntimeException("Failed to load entity!");
        }

        return ParamsMap.of(WindowParams.ITEM.name(), entity);
    }

    protected Class findIdType(Class entityClass) {
        for (Field field : entityClass.getDeclaredFields()) {
            if ("id".equals(field.getName())) {
                return field.getType();
            }
        }
        return findIdType(entityClass.getSuperclass());
    }

    @SuppressWarnings("unused")
    protected boolean paramsChanged(UriState state) {
        // TODO: check if new root and nestedRoute are the same as current
        return false;
    }

    @SuppressWarnings("unused")
    protected void handleParamsChange(UriState state) {
        // TODO: invoke urlParamsChanged screen hook
    }

    protected void reloadApp() {
        AppUI ui = AppUI.getCurrent();
        if (ui != null) {
            String url = ControllerUtils.getLocationWithoutParams() + "?restartApp";
            ui.getPage().open(url, "_self");
        }
    }

    protected boolean isRootRoute(UriState uriState) {
        return StringUtils.isEmpty(uriState.getStateMark())
                && StringUtils.isEmpty(uriState.getNestedRoute());
    }

    protected Screen getCurrentScreen() {
        Iterator<Screen> dialogs = getScreens().getOpenedScreens().getDialogScreens().iterator();
        if (dialogs.hasNext()) {
            return dialogs.next();
        }

        Iterator<Screen> breadCrumbsScreens = getScreens()
                .getOpenedScreens()
                .getCurrentBreadcrumbs()
                .iterator();
        if (breadCrumbsScreens.hasNext()) {
            return breadCrumbsScreens.next();
        }

        return getScreens().getOpenedScreens().getRootScreenOrNull();
    }

    protected Screens getScreens() {
        return ui.getScreens();
    }

    protected History getHistory() {
        return ui.getHistory();
    }

    protected boolean rootState(UriState uriState) {
        return StringUtils.isEmpty(uriState.getStateMark())
                && StringUtils.isEmpty(uriState.getNestedRoute());
    }

    protected boolean rootScreenHasWorkArea() {
        Screen rootScreen = getScreens().getOpenedScreens().getRootScreenOrNull();
        return rootScreen instanceof Window.HasWorkArea;
    }

    protected WebAppWorkArea getConfiguredWorkArea() {
        Screen rootScreen = getScreens().getOpenedScreens().getRootScreen();
        if (rootScreen instanceof Window.HasWorkArea) {
            AppWorkArea workArea = ((Window.HasWorkArea) rootScreen).getWorkArea();
            if (workArea != null) {
                return (WebAppWorkArea) workArea;
            }
        }

        throw new IllegalStateException("RootWindow does not have any configured work area");
    }

    protected void handleScreenChange(UriState state) {
        String route = state.getNestedRoute();
        WindowInfo windowInfo = windowConfig.findWindowInfoByRoute(route);

        if (windowInfo == null) {
            throw new RuntimeException("Unable to find WindowInfo for route: " + route);
        }

        Map<String, Object> screenOptions = null;
        if (EditorScreen.class.isAssignableFrom(windowInfo.getControllerClass())) {
            screenOptions = createEditorScreenOptions(windowInfo, state);
        }

        Screen screen = getScreens().create(windowInfo, OpenMode.THIS_TAB, new MapScreenOptions(screenOptions));

        if (screen instanceof EditorScreen) {
            //noinspection unchecked
            ((EditorScreen) screen).setEntityToEdit(
                    (Entity) screenOptions.get(WindowParams.ITEM.name()));
        }

        getScreens().show(screen);
    }

    protected String getStateMark(Window window) {
        return String.valueOf(((WebWindow) window).getStateMark());
    }

    protected Screen findScreenByState(UriState uriState) {
        String stateMark = uriState.getStateMark();
        return getScreens().getOpenedScreens()
                .getAll()
                .stream()
                .filter(s -> Objects.equals(stateMark, getStateMark(s.getWindow())))
                .findFirst()
                .orElse(null);
    }

    protected Screen findActiveScreenByState(UriState uriState) {
        String stateMark = uriState.getStateMark();
        return getScreens().getOpenedScreens()
                .getActiveScreens()
                .stream()
                .filter(s -> Objects.equals(stateMark, getStateMark(s.getWindow())))
                .findFirst()
                .orElse(null);
    }

    protected void focusScreen(Screen screen) {
        if (screen == null) {
            return;
        }

        Window window = screen.getWindow();

        WebAppWorkArea workArea = getConfiguredWorkArea();
        if (workArea.getMode() == AppWorkArea.Mode.TABBED) {
            TabSheetBehaviour tabSheet = workArea.getTabbedWindowContainer().getTabSheetBehaviour();
            TabWindowContainer windowContainer = (TabWindowContainer) window
                    .unwrapComposition(com.vaadin.ui.Component.class)
                    .getParent();

            String tabId = tabSheet.getTab(windowContainer);
            if (tabId == null || tabId.isEmpty()) {
                throw new IllegalStateException("Unable to find tab");
            }

            tabSheet.setSelectedTab(tabId);
        }
    }
}
