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

package com.haulmont.cuba.gui.screen;

import com.haulmont.bali.events.EventHub;
import com.haulmont.bali.events.Subscription;
import com.haulmont.bali.events.TriggerOnce;
import com.haulmont.cuba.core.global.BeanLocator;
import com.haulmont.cuba.gui.Fragments;
import com.haulmont.cuba.gui.components.Fragment;
import com.haulmont.cuba.gui.components.sys.FragmentImplementation;
import com.haulmont.cuba.gui.model.ScreenData;
import org.springframework.context.ApplicationListener;

import javax.inject.Inject;
import java.util.EventObject;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Base class for all fragment controllers.
 *
 * @see Fragment
 */
public abstract class ScreenFragment implements FrameOwner {

    private String id;

    private ScreenContext screenContext;

    private ScreenData screenData;

    private Fragment fragment;

    private EventHub eventHub = new EventHub();

    private BeanLocator beanLocator;

    private FrameOwner hostController;

    // Global event listeners
    private List<ApplicationListener> uiEventListeners;

    // Extensions state
    private Map<Class<?>, Object> extensions;

    @Inject
    protected void setBeanLocator(BeanLocator beanLocator) {
        this.beanLocator = beanLocator;
    }

    protected BeanLocator getBeanLocator() {
        return beanLocator;
    }

    protected Map<Class<?>, Object> getExtensions() {
        return extensions;
    }

    protected void setExtensions(Map<Class<?>, Object> extensions) {
        this.extensions = extensions;
    }

    protected EventHub getEventHub() {
        return eventHub;
    }

    protected <E> void fireEvent(Class<E> eventType, E event) {
        eventHub.publish(eventType, event);
    }

    protected void setFragment(Fragment fragment) {
        this.fragment = fragment;
    }

    public Fragment getFragment() {
        return fragment;
    }

    public FrameOwner getHostController() {
        return hostController;
    }

    protected void setHostController(FrameOwner hostController) {
        this.hostController = hostController;
    }

    /**
     * @return host screen of the fragment
     * @throws IllegalStateException if host screen cannot be found though hierarchy of fragments
     */
    protected Screen getHostScreen() {
        FrameOwner parent = this.hostController;

        while (parent != null
            && !(parent instanceof Screen)) {

            parent = ((ScreenFragment) parent).getHostController();
        }

        if (parent == null) {
            throw new IllegalStateException("Unable to get screen for Fragment");
        }

        return (Screen) parent;
    }

    /**
     * JavaDoc
     *
     * @param id
     */
    protected void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    void setScreenContext(ScreenContext screenContext) {
        this.screenContext = screenContext;
    }

    /**
     * Use {@link UiControllerUtils#getScreenContext(FrameOwner)} to obtain Screen services from external code.
     *
     * @return screen context
     */
    ScreenContext getScreenContext() {
        return screenContext;
    }

    protected ScreenData getScreenData() {
        return screenData;
    }

    protected void setScreenData(ScreenData data) {
        this.screenData = data;
    }

    protected List<ApplicationListener> getUiEventListeners() {
        return uiEventListeners;
    }

    protected void setUiEventListeners(List<ApplicationListener> listeners) {
        this.uiEventListeners = listeners;

        if (listeners != null && !listeners.isEmpty()) {
            ((FragmentImplementation) this.fragment).initUiEventListeners();
        }
    }

    /**
     * Convenient method to perform programmatic initialization of the fragment.
     *
     * @see Fragments#init(ScreenFragment)
     */
    public ScreenFragment init() {
        getScreenContext().getFragments().init(this);
        return this;
    }

    /**
     * JavaDoc
     *
     * @param listener
     * @return
     */
    protected Subscription addInitListener(Consumer<InitEvent> listener) {
        return eventHub.subscribe(InitEvent.class, listener);
    }

    /**
     * JavaDoc
     *
     * @param listener
     * @return
     */
    protected Subscription addAfterInitListener(Consumer<AfterInitEvent> listener) {
        return eventHub.subscribe(AfterInitEvent.class, listener);
    }

    /**
     * JavaDoc
     */
    @TriggerOnce
    public static class InitEvent extends EventObject {
        protected final ScreenOptions options;

        public InitEvent(ScreenFragment source, ScreenOptions options) {
            super(source);
            this.options = options;
        }

        @Override
        public ScreenFragment getSource() {
            return (ScreenFragment) super.getSource();
        }

        public ScreenOptions getOptions() {
            return options;
        }
    }

    /**
     * JavaDoc
     *
     * Used by UI components to perform actions after UiController initialized.
     */
    @TriggerOnce
    public static class AfterInitEvent extends EventObject {
        protected final ScreenOptions options;

        public AfterInitEvent(ScreenFragment source, ScreenOptions options) {
            super(source);
            this.options = options;
        }

        @Override
        public ScreenFragment getSource() {
            return (ScreenFragment) super.getSource();
        }

        public ScreenOptions getOptions() {
            return options;
        }
    }

    // todo events: attached / detached / events from parent
}