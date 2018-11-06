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
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.*;
import com.haulmont.cuba.gui.components.Window;
import com.haulmont.cuba.gui.config.WindowConfig;
import com.haulmont.cuba.gui.config.WindowInfo;
import com.haulmont.cuba.gui.navigation.NavigationAware;
import com.haulmont.cuba.gui.navigation.NavigationState;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.gui.screen.compatibility.LegacyFrame;
import com.haulmont.cuba.gui.util.OperationResult;
import com.haulmont.cuba.security.entity.EntityOp;
import com.haulmont.cuba.security.entity.PermissionType;
import com.haulmont.cuba.web.App;
import com.haulmont.cuba.web.AppUI;
import com.haulmont.cuba.web.WebConfig;
import com.haulmont.cuba.web.app.ui.navigation.notfoundwindow.NotFoundScreen;
import com.haulmont.cuba.web.controllers.ControllerUtils;
import com.haulmont.cuba.web.gui.UrlHandlingMode;
import com.haulmont.cuba.web.gui.WebWindow;
import com.haulmont.cuba.web.navigation.NavigationException;
import com.haulmont.cuba.web.navigation.UrlIdBase64Converter;
import com.haulmont.cuba.web.navigation.accessfilter.NavigationFilter;
import com.haulmont.cuba.web.navigation.accessfilter.NavigationFilter.AccessCheckResult;
import com.vaadin.server.Page;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

import static com.haulmont.cuba.gui.screen.UiControllerUtils.getScreenContext;

public class UrlChangeHandler {

    private static final Logger log = LoggerFactory.getLogger(UrlChangeHandler.class);

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
    protected List<NavigationFilter> accessFilters;

    @Inject
    protected BeanLocator beanLocator;

    @Inject
    protected Security security;

    protected AppUI ui;

    public UrlChangeHandler(AppUI ui) {
        this.ui = ui;
    }

    public void handleUriChange() {
        if (notSuitableUrlHandlingMode()) {
            return;
        }

        NavigationState requestedState = getNavigation().getState();
        if (requestedState == null) {
            log.debug("Unable to handle requested state: \"{}\"", Page.getCurrent().getUriFragment());
            reloadApp();
            return;
        }

        if (!App.getInstance().getConnection().isAuthenticated()) {
            handleNoAuthNavigation(requestedState);
            return;
        }

        handleUriChange(requestedState);
    }

    protected void handleUriChange(NavigationState navigationState) {
        if (historyNavigation(navigationState)) {
            handleHistoryNavigation(navigationState);
        } else {
            handleScreenNavigation(navigationState);
        }
    }

    protected boolean historyNavigation(NavigationState state) {
        return Objects.equals(state, getHistory().getPrevious())
                || Objects.equals(state, getHistory().getNext());
    }

    protected void handleHistoryNavigation(NavigationState navigationState) {
        if (Objects.equals(navigationState, getHistory().getPrevious())) {
            handleHistoryBackward();
        } else {
            handleHistoryForward();
        }
    }

    protected void handleHistoryBackward() {
        NavigationState prevState = getHistory().getPrevious();
        Screen prevScreen = findScreenByState(getScreens().getOpenedScreens().getAll(), prevState);

        AccessCheckResult accessCheckResult = navigationAllowed(prevState);
        if (!accessCheckResult.isAllowed()) {
            showNotification(accessCheckResult.getMessage());
            revertNavigationState();
            return;
        }

        //noinspection ConstantConditions
        if (prevScreen == null && StringUtils.isNotEmpty(prevState.getStateMark())) {
            revertNavigationState();
            showNotification(messages.getMainMessage("navigation.unableToGoBackward"));
            return;
        }

        Screen lastOpenedScreen = findActiveScreenByState(getHistory().getNow());
        if (lastOpenedScreen != null) {
            OperationResult screenCloseResult = lastOpenedScreen
                    .getWindow().getFrameOwner()
                    .close(FrameOwner.WINDOW_CLOSE_ACTION)
                    .then(this::proceedHistoryBackward);

            if (OperationResult.Status.FAIL == screenCloseResult.getStatus()
                    || OperationResult.Status.UNKNOWN == screenCloseResult.getStatus()) {
                revertNavigationState();
            }
        } else {
            proceedHistoryBackward();
        }
    }

    protected void proceedHistoryBackward() {
        History history = getHistory();

        NavigationState nowState = history.getNow();
        NavigationState prevState = history.backward();

        selectScreen(findActiveScreenByState(prevState));

        boolean rootBackward = rootState(prevState) && rootChanged(prevState);
        //noinspection ConstantConditions
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
            currentScreen = getAnyCurrentScreen();
        }

        String route = getScreenContext(currentScreen)
                .getRouteInfo()
                .getResolvedState()
                .asRoute();
        Page.getCurrent().setUriFragment(route);

        showNotification(messages.getMainMessage("navigation.unableToGoForward"));
    }

    protected void handleScreenNavigation(NavigationState navigationState) {
        if (rootChanged(navigationState)) {
            handleRootChange(navigationState);
            return;
        }

        if (screenChanged(navigationState)) {
            handleScreenChange(navigationState);
            return;
        }

        if (paramsChanged(navigationState)) {
            handleParamsChange(navigationState);
        }

        if (screensClosed()) {
            handleScreensClosed();
        }
    }

    protected boolean screensClosed() {
        return !rootState(getHistory().getNow())
                && rootState(getNavigation().getState());
    }

    protected void handleScreensClosed() {
        Screen lastOpenedScreen = findActiveScreenByState(getHistory().getNow());
        if (lastOpenedScreen != null) {
            lastOpenedScreen.getWindow().getFrameOwner()
                    .close(FrameOwner.WINDOW_CLOSE_ACTION)
                    .otherwise(this::revertNavigationState);
        }
    }

    protected boolean rootChanged(NavigationState navigationState) {
        Screen rootScreen = getScreens().getOpenedScreens().getRootScreenOrNull();
        if (rootScreen == null) {
            return false;
        }

        String resolvedRoute = getScreenContext(rootScreen)
                .getRouteInfo()
                .getResolvedState()
                .getRoot();

        return !Objects.equals(resolvedRoute, navigationState.getRoot());
    }

    protected void handleRootChange(NavigationState state) {
        AccessCheckResult result = navigationAllowed(state);
        if (!result.isAllowed()) {
            showNotification(result.getMessage());
            revertNavigationState();
            return;
        }

        showNotification(messages.getMainMessage("navigation.rootChangeIsNotSupported"));
        revertNavigationState();
    }

    protected boolean screenChanged(NavigationState requestedState) {
        if (rootState(requestedState) || !rootScreenHasWorkArea()) {
            return false;
        }

        Screen currentScreen = findActiveScreenByState(getHistory().getNow());

        if (currentScreen == null) {
            Iterator<Screen> screensIterator = getScreens().getOpenedScreens().getCurrentBreadcrumbs().iterator();
            currentScreen = screensIterator.hasNext() ? screensIterator.next() : null;
        }

        if (currentScreen == null) {
            return true;
        }

        NavigationState currentState = getScreenContext(currentScreen)
                .getRouteInfo()
                .getResolvedState();

        if (!Objects.equals(currentState.getStateMark(), requestedState.getStateMark())) {
            return true;
        }

        return !Objects.equals(currentState.getNestedRoute(), requestedState.getNestedRoute());
    }

    protected void handleScreenChange(NavigationState navigationState) {
        // TODO: handle few opened screens
        WindowInfo windowInfo = windowConfig.findWindowInfoByRoute(navigationState.getNestedRoute());

        if (windowInfo == null) {
            handle404(navigationState);
            return;
        }

        boolean screenPermitted = security.isScreenPermitted(windowInfo.getId());
        if (!screenPermitted) {
            revertNavigationState();
            throw new AccessDeniedException(PermissionType.SCREEN, windowInfo.getId());
        }

        Screen screen;

        if (isEditor(windowInfo)) {
            screen = createEditor(windowInfo, navigationState);
            if (screen == null) {
                revertNavigationState();
                showNotification(messages.getMainMessage("navigation.failedToOpenEditor"));
                return;
            }
        } else {
            screen = getScreens().create(windowInfo.getId(), OpenMode.NEW_TAB);
        }

        getScreens().show(screen);
    }

    protected void handle404(NavigationState navigationState) {
        MapScreenOptions params = new MapScreenOptions(ParamsMap.of("requestedRoute", navigationState.getNestedRoute()));
        NotFoundScreen notFoundScreen = getScreens().create(NotFoundScreen.class, OpenMode.NEW_TAB, params);

        getScreens().show(notFoundScreen);
    }

    protected boolean isEditor(WindowInfo windowInfo) {
        return EditorScreen.class.isAssignableFrom(windowInfo.getControllerClass());
    }

    protected Screen createEditor(WindowInfo windowInfo, NavigationState navigationState) {
        Map<String, Object> screenOptions = createEditorScreenOptions(windowInfo, navigationState);
        if (screenOptions.isEmpty()) {
            return null;
        }

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

    protected Map<String, Object> createEditorScreenOptions(WindowInfo windowInfo, NavigationState state) {
        Type screenSuperclass = windowInfo.getControllerClass().getGenericSuperclass();

        if (!(screenSuperclass instanceof ParameterizedType)) {
            return Collections.emptyMap();
        }

        String idParam = state.getParams().get("id");
        if (StringUtils.isEmpty(idParam)) {
            return Collections.emptyMap();
        }

        ParameterizedType parameterizedEditor = (ParameterizedType) screenSuperclass;
        //noinspection unchecked
        Class<? extends Entity> entityClass = (Class<? extends Entity>) parameterizedEditor.getActualTypeArguments()[0];
        MetaClass metaClass = metadata.getClassNN(entityClass);

        if (!security.isEntityOpPermitted(metaClass, EntityOp.READ)) {
            revertNavigationState();
            throw new AccessDeniedException(PermissionType.ENTITY_OP, EntityOp.READ, entityClass.getSimpleName());
        }

        Class<?> idType = metaClass
                .getPropertyNN("id")
                .getJavaType();
        Object id = UrlIdBase64Converter.deserialize(idType, idParam);

        LoadContext<?> ctx = new LoadContext(metaClass);
        ctx.setId(id);
        ctx.setView(findSuitableView(entityClass, state));

        Entity entity = dataManager.load(ctx);
        if (entity == null) {
            return Collections.emptyMap();
        }

        return ParamsMap.of(WindowParams.ITEM.name(), entity);
    }

    protected View findSuitableView(Class<? extends Entity> entityClass, NavigationState navigationState) {
        for (String viewName : viewRepository.getViewNames(entityClass)) {
            if (viewName.endsWith(".edit")) {
                return viewRepository.getView(entityClass, viewName);
            }
        }
        throw new NavigationException("Unable to find suitable view to open editor for entity: " + entityClass.getName(),
                navigationState);
    }

    protected boolean paramsChanged(NavigationState state) {
        String currentParams = getScreenContext(getAnyCurrentScreen())
                .getRouteInfo()
                .getResolvedState().getParamsString();

        String requestedParams = state.getParamsString();

        return !Objects.equals(currentParams, requestedParams);
    }

    protected void handleParamsChange(NavigationState state) {
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

    protected Screen getAnyCurrentScreen() {
        Iterator<Screen> dialogsIterator = getScreens()
                .getOpenedScreens()
                .getDialogScreens()
                .iterator();
        if (dialogsIterator.hasNext()) {
            return dialogsIterator.next();
        }

        Iterator<Screen> screensIterator = getScreens()
                .getOpenedScreens()
                .getCurrentBreadcrumbs()
                .iterator();
        if (screensIterator.hasNext()) {
            return screensIterator.next();
        }

        return getScreens().getOpenedScreens().getRootScreenOrNull();
    }

    protected boolean rootState(NavigationState navigationState) {
        return StringUtils.isEmpty(navigationState.getStateMark())
                && StringUtils.isEmpty(navigationState.getNestedRoute());
    }

    protected boolean rootScreenHasWorkArea() {
        Screen rootScreen = getScreens().getOpenedScreens().getRootScreenOrNull();
        return rootScreen instanceof Window.HasWorkArea;
    }

    protected String getStateMark(Screen screen) {
        WebWindow window = (WebWindow) screen.getWindow();
        return String.valueOf(window.getStateMark());
    }

    protected Screen findActiveScreenByState(NavigationState navigationState) {
        Collection<Screen> activeScreens = getScreens().getOpenedScreens().getActiveScreens();
        return findScreenByState(activeScreens, navigationState);
    }

    protected Screen findScreenByState(Collection<Screen> screens, NavigationState state) {
        return screens.stream()
                .filter(s -> Objects.equals(state.getStateMark(), getStateMark(s)))
                .findFirst().orElse(null);
    }

    protected void selectScreen(Screen screen) {
        if (screen == null) {
            return;
        }
        for (Screens.WindowStack windowStack : getScreens().getOpenedScreens().getWorkAreaStacks()) {
            Iterator<Screen> breadCrumbs = windowStack.getBreadcrumbs().iterator();
            if (breadCrumbs.hasNext() && breadCrumbs.next() == screen) {
                windowStack.select();
                return;
            }
        }
    }

    protected void showNotification(String msg) {
        ui.getNotifications().create()
                .setCaption(msg)
                .setType(Notifications.NotificationType.TRAY)
                .show();
    }

    protected void revertNavigationState() {
        Screen screen = findActiveScreenByState(getHistory().getNow());
        if (screen == null) {
            screen = getAnyCurrentScreen();
        }
        String route = getScreenContext(screen)
                .getRouteInfo()
                .getResolvedState()
                .asRoute();
        Page.getCurrent().setUriFragment(route);
    }

    protected void handleNoAuthNavigation(NavigationState requestedState) {
        if (Objects.equals(getHistory().getNow(), requestedState)) {
            return;
        }

        String nestedRoute = requestedState.getNestedRoute();
        if (StringUtils.isNotEmpty(nestedRoute)) {
            RedirectHandler redirectHandler = beanLocator.getPrototype(RedirectHandler.NAME, ui);
            redirectHandler.schedule(requestedState);
            App.getInstance().setRedirectHandler(redirectHandler);
        }

        showNotification(messages.getMainMessage("navigation.shouldLogInFirst"));
    }

    protected boolean notSuitableUrlHandlingMode() {
        boolean notSuitableMode = UrlHandlingMode.URL_ROUTES != webConfig.getUrlHandlingMode();
        if (notSuitableMode) {
            log.debug("UrlChangeHandler is disabled for {} URL handling mode", webConfig.getUrlHandlingMode());
            return true;
        }
        return false;
    }

    protected AccessCheckResult navigationAllowed(NavigationState navigationState) {
        for (NavigationFilter filter : accessFilters) {
            AccessCheckResult result = filter.allowed(getHistory().getNow(), navigationState);
            if (!result.isAllowed()) {
                return result;
            }
        }
        return AccessCheckResult.allowed();
    }

    protected Navigation getNavigation() {
        return ui.getNavigation();
    }

    protected Screens getScreens() {
        return ui.getScreens();
    }

    protected History getHistory() {
        return ui.getHistory();
    }

    /**
     * INTERNAL.
     * Used by {@link RedirectHandler}.
     *
     * @param navigationState new navigation state
     */
    public void handleUrlChangeInternal(NavigationState navigationState) {
        if (notSuitableUrlHandlingMode()) {
            return;
        }
        handleUriChange(navigationState);
    }
}
