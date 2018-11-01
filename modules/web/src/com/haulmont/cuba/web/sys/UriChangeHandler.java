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
import com.haulmont.bali.util.URLEncodeUtils;
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
import com.haulmont.cuba.gui.navigation.NavigationAware;
import com.haulmont.cuba.gui.navigation.UriState;
import com.haulmont.cuba.gui.navigation.UriStateChangedEvent;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.gui.screen.compatibility.LegacyFrame;
import com.haulmont.cuba.web.AppUI;
import com.haulmont.cuba.web.WebConfig;
import com.haulmont.cuba.web.app.ui.navigation.notfoundwindow.NotFoundScreen;
import com.haulmont.cuba.web.controllers.ControllerUtils;
import com.haulmont.cuba.web.gui.UrlHandlingMode;
import com.haulmont.cuba.web.gui.WebWindow;
import com.haulmont.cuba.web.gui.components.mainwindow.WebAppWorkArea;
import com.haulmont.cuba.web.navigation.Base64Converter;
import com.haulmont.cuba.web.navigation.NavigationException;
import com.haulmont.cuba.web.navigation.accessfilter.NavigationAccessFilter;
import com.haulmont.cuba.web.navigation.accessfilter.NavigationAccessFilter.AccessCheckResult;
import com.haulmont.cuba.web.widgets.TabSheetBehaviour;
import com.vaadin.server.Page;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.lang.reflect.ParameterizedType;
import java.util.Iterator;
import java.util.List;
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
    protected Metadata metadata;

    @Inject
    protected Messages messages;

    @Inject
    protected DataManager dataManager;

    @Inject
    protected ViewRepository viewRepository;

    @Inject
    protected WebConfig webConfig;

    @Inject
    protected List<NavigationAccessFilter> accessFilters;

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
        return Objects.equals(uriState, getHistory().getPrevious())
                || Objects.equals(uriState, getHistory().getNext());
    }

    protected void handleHistoryNavigation(UriState uriState) {
        if (Objects.equals(uriState, getHistory().getPrevious())) {
            handleHistoryBackward();
        } else {
            handleHistoryForward();
        }
    }

    protected void handleHistoryBackward() {
        UriState prevState = getHistory().getPrevious();
        Screen prevScreen = findScreenByState(prevState);

        AccessCheckResult accessCheckResult = historyNavigationAllowed(prevState);
        if (!accessCheckResult.isAllowed()) {
            ui.getNotifications()
                    .create()
                    .setCaption(accessCheckResult.getMessage())
                    .setType(Notifications.NotificationType.TRAY)
                    .show();

            revertHistoryBackward();

            return;
        }

        if (prevScreen == null && StringUtils.isNotEmpty(prevState.getStateMark())) {
            revertHistoryBackward();

            ui.getNotifications().create()
                    .setCaption(messages.getMainMessage("navigation.unableToGoBackward"))
                    .setType(Notifications.NotificationType.TRAY)
                    .show();

            return;
        }

        Screen lastOpenedScreen = findActiveScreenByState(getHistory().getNow());
        if (lastOpenedScreen != null) {
            lastOpenedScreen.getWindow().getFrameOwner()
                    .close(FrameOwner.WINDOW_CLOSE_ACTION)
                    .then(this::proceedHistoryBackward)
                    .otherwise(this::revertHistoryBackward);
        } else {
            proceedHistoryBackward();
        }
    }

    protected AccessCheckResult historyNavigationAllowed(UriState uriState) {
        for (NavigationAccessFilter filter : accessFilters) {
            AccessCheckResult result = filter.allowed(uriState);
            if (!result.isAllowed()) {
                return result;
            }
        }
        return AccessCheckResult.allowed();
    }

    protected void proceedHistoryBackward() {
        History history = getHistory();

        UriState nowState = history.getNow();
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
        Screen currentScreen = findActiveScreenByState(getHistory().getNow());
        if (currentScreen == null) {
            currentScreen = getCurrentScreen();
        }

        String route = currentScreen.getScreenContext()
                .getRouteInfo()
                .getResolvedState()
                .asRoute();
        Page.getCurrent().setUriFragment(route);

        ui.getNotifications().create()
                .setCaption(messages.getMainMessage("navigation.unableToGoForward"))
                .setType(Notifications.NotificationType.TRAY)
                .show();
    }

    protected void revertHistoryBackward() {
        Screen screen = findActiveScreenByState(getHistory().getNow());
        if (screen == null) {
            screen = getCurrentScreen();
        }
        String route = screen
                .getScreenContext()
                .getRouteInfo()
                .getResolvedState()
                .asRoute();
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

        if (screensClosed(uriState)) {
            handleScreensClosed();
        }
    }

    protected boolean screensClosed(UriState uriState) {
        return !rootState(getHistory().getNow()) && rootState(ui.getNavigation().getState());
    }

    protected void handleScreensClosed() {
        Screen lastOpenedScreen = findActiveScreenByState(getHistory().getNow());
        if (lastOpenedScreen != null) {
            lastOpenedScreen.getWindow().getFrameOwner()
                    .close(FrameOwner.WINDOW_CLOSE_ACTION)
                    .otherwise(this::revertHistoryBackward);
        }
    }

    protected boolean rootChanged(UriState uriState) {
        Screen rootScreen = getScreens().getOpenedScreens().getRootScreenOrNull();
        if (rootScreen == null) {
            return false;
        }

        String resolvedRoute = rootScreen.getScreenContext()
                .getRouteInfo()
                .getResolvedState()
                .getRoot();

        return !Objects.equals(resolvedRoute, uriState.getRoot());
    }

    @SuppressWarnings("unused")
    protected void handleRootChange(UriState state) {
        throw new NavigationException("Unable to handle requested state", state);
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
                .getRouteInfo()
                .getResolvedState()
                .getNestedRoute();

        return !Objects.equals(currentScreenRoute, uriState.asRoute());
    }

    protected void handleScreenChange(UriState uriState) {
        // TODO: handle few opened screens
        WindowInfo windowInfo = windowConfig.findWindowInfoByRoute(uriState.getNestedRoute());

        if (windowInfo == null) {
            handle404(uriState);
            return;
        }

        Screen screen = !isEditor(windowInfo)
                ? getScreens().create(windowInfo.getId(), OpenMode.NEW_TAB)
                : createEditor(windowInfo, uriState);

        getScreens().show(screen);
    }

    protected void handle404(UriState uriState) {
        MapScreenOptions params = new MapScreenOptions(ParamsMap.of("requestedRoute", uriState.getNestedRoute()));
        NotFoundScreen notFoundScreen = getScreens().create(NotFoundScreen.class, OpenMode.NEW_TAB, params);

        getScreens().show(notFoundScreen);
    }

    protected boolean isEditor(WindowInfo windowInfo) {
        return EditorScreen.class.isAssignableFrom(windowInfo.getControllerClass());
    }

    protected Screen createEditor(WindowInfo windowInfo, UriState uriState) {
        Map<String, Object> screenOptions = createEditorScreenOptions(windowInfo, uriState);

        Screen editor;
        if (LegacyFrame.class.isAssignableFrom(windowInfo.getControllerClass())) {
            editor = getScreens().create(windowInfo.getId(), OpenMode.NEW_TAB, new MapScreenOptions(screenOptions));
        } else {
            editor = getScreens().create(windowInfo.getId(), OpenMode.NEW_TAB);
        }

        Entity entity = (Entity) screenOptions.get(WindowParams.ITEM.name());
        //noinspection unchecked
        ((EditorScreen<Entity>) editor).setEntityToEdit(entity);

        return editor;
    }

    protected Map<String, Object> createEditorScreenOptions(WindowInfo windowInfo, UriState state) {
        //noinspection unchecked
        Class<? extends Entity> entityClass = (Class<? extends Entity>) ((ParameterizedType) windowInfo
                .getControllerClass()
                .getGenericSuperclass())
                .getActualTypeArguments()[0];

        Object id = Base64Converter.deserialize(
                metadata.getClassNN(entityClass).getPropertyNN("id").getJavaType(),
                URLEncodeUtils.decodeUtf8(state.getParams().get("id")));

        LoadContext<?> ctx = new LoadContext(metadata.getClassNN(entityClass));
        ctx.setId(id);
        ctx.setView(findSuitableView(entityClass, state));

        Entity entity = dataManager.load(ctx);

        return ParamsMap.of(WindowParams.ITEM.name(), entity);
    }

    protected View findSuitableView(Class<? extends Entity> entityClass, UriState uriState) {
        for (String viewName : viewRepository.getViewNames(entityClass)) {
            if (viewName.endsWith(".edit")) {
                return viewRepository.getView(entityClass, viewName);
            }
        }
        throw new NavigationException("Unable to find suitable view to open editor for entity: " + entityClass.getName(), uriState);
    }

    @SuppressWarnings("unused")
    protected boolean paramsChanged(UriState state) {
        return false;
    }

    @SuppressWarnings("unused")
    protected void handleParamsChange(UriState state) {
        Screen screen = findActiveScreenByState(state);
        if (screen instanceof NavigationAware) {
            ((NavigationAware) screen).urlParamsChanged(state.getParams());
        }
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
            if (tabId != null && !tabId.isEmpty()) {
                tabSheet.setSelectedTab(tabId);
            }
        }
    }
}
