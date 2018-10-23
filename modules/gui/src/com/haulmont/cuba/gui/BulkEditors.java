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

package com.haulmont.cuba.gui;

import com.haulmont.bali.util.ParamsMap;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.gui.app.core.bulk.BulkEditorWindow;
import com.haulmont.cuba.gui.components.Field;
import com.haulmont.cuba.gui.components.ListComponent;
import com.haulmont.cuba.gui.components.Window;
import com.haulmont.cuba.gui.components.data.DataUnit;
import com.haulmont.cuba.gui.components.data.meta.ContainerDataUnit;
import com.haulmont.cuba.gui.config.WindowConfig;
import com.haulmont.cuba.gui.config.WindowInfo;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.DataLoader;
import com.haulmont.cuba.gui.model.HasLoader;
import com.haulmont.cuba.gui.screen.*;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.haulmont.bali.util.Preconditions.checkNotNullArgument;

@Component("cuba_BulkEditor")
public class BulkEditors {

    private static final Logger log = LoggerFactory.getLogger(BulkEditors.class);

    @Inject
    protected WindowConfig windowConfig;

    public <E extends Entity> BulkEditorBuilder<E> builder(MetaClass metaClass,
                                                           Collection<E> entities, FrameOwner origin) {
        checkNotNullArgument(metaClass);
        checkNotNullArgument(entities);
        checkNotNullArgument(origin);

        return new BulkEditorBuilder<>(metaClass, entities, origin, this::buildBulkEditor);
    }

    protected <E extends Entity> BulkEditorWindow buildBulkEditor(BulkEditorBuilder<E> builder) {
        FrameOwner origin = builder.getOrigin();
        Screens screens = origin.getScreenContext().getScreens();

        if (CollectionUtils.isEmpty(builder.getEntities())) {
            throw new IllegalStateException(String.format("BulkEditor of %s cannot be open with no entities were set",
                    builder.getMetaClass()));
        }

        ScreenOptions options = new MapScreenOptions(ParamsMap.of()
                .pair("metaClass", builder.getMetaClass())
                .pair("selected", builder.getEntities())
                .pair("exclude", builder.getExclude())
                .pair("includeProperties", builder.getIncludeProperties() != null
                        ? builder.getIncludeProperties()
                        : Collections.emptyList())
                .pair("fieldValidators", builder.getFieldValidators())
                .pair("modelValidators", builder.getModelValidators())
                .pair("loadDynamicAttributes", builder.isLoadDynamicAttributes())
                .pair("useConfirmDialog", builder.isUseConfirmDialog())
                .create());

        WindowInfo windowInfo = windowConfig.getWindowInfo("bulkEditor");
        BulkEditorWindow bulkEditorWindow = (BulkEditorWindow) screens.create(windowInfo, builder.launchMode, options);

        bulkEditorWindow.addAfterCloseListener(afterCloseEvent -> {
            ListComponent<E> listComponent = builder.getListComponent();
            CloseAction closeAction = afterCloseEvent.getCloseAction();
            if (isCommitCloseAction(closeAction)
                    && listComponent != null) {
                refreshItems(listComponent.getItems());
            }
            if (listComponent instanceof com.haulmont.cuba.gui.components.Component.Focusable) {
                ((com.haulmont.cuba.gui.components.Component.Focusable) listComponent).focus();
            }
        });

        return bulkEditorWindow;
    }

    protected <E extends Entity> void refreshItems(DataUnit<E> dataSource) {
        CollectionContainer<E> container = dataSource instanceof ContainerDataUnit ?
                ((ContainerDataUnit) dataSource).getContainer() : null;
        if (container != null) {
            DataLoader loader = null;
            if (container instanceof HasLoader) {
                loader = ((HasLoader) container).getLoader();
            }
            if (loader != null) {
                loader.load();
            } else {
                log.warn("Target container has no loader, refresh is impossible");
            }
        }
    }

    protected boolean isCommitCloseAction(CloseAction closeAction) {
        return (closeAction instanceof StandardCloseAction)
                && ((StandardCloseAction) closeAction).getActionId().equals(Window.COMMIT_ACTION_ID);
    }

    public static class BulkEditorBuilder<E extends Entity> {

        protected final MetaClass metaClass;
        protected final FrameOwner origin;
        protected final Collection<E> entities;
        protected final Function<BulkEditorBuilder<E>, BulkEditorWindow> handler;

        protected Screens.LaunchMode launchMode = OpenMode.DIALOG;
        protected ListComponent<E> listComponent;

        protected String exclude;
        protected List<String> includeProperties = Collections.emptyList();
        // TODO: gg, replace with no deprecated
        protected Map<String, Field.Validator> fieldValidators;
        // TODO: gg, replace  no deprecated
        protected List<Field.Validator> modelValidators;
        protected Boolean loadDynamicAttributes;
        protected Boolean useConfirmDialog;

        public BulkEditorBuilder(BulkEditorBuilder<E> builder) {
            this.metaClass = builder.metaClass;
            this.origin = builder.origin;
            this.handler = builder.handler;
            this.entities = builder.entities;

            this.launchMode = builder.launchMode;
            this.listComponent = builder.listComponent;

            this.exclude = builder.exclude;
            this.includeProperties = builder.includeProperties;
            this.fieldValidators = builder.fieldValidators;
            this.modelValidators = builder.modelValidators;
            this.loadDynamicAttributes = builder.loadDynamicAttributes;
            this.useConfirmDialog = builder.useConfirmDialog;
        }

        public BulkEditorBuilder(MetaClass metaClass, Collection<E> entities, FrameOwner origin,
                                 Function<BulkEditorBuilder<E>, BulkEditorWindow> handler) {
            this.metaClass = metaClass;
            this.entities = entities;
            this.origin = origin;
            this.handler = handler;
        }

        public BulkEditorBuilder<E> withLaunchMode(Screens.LaunchMode launchMode) {
            this.launchMode = launchMode;
            return this;
        }

        public BulkEditorBuilder<E> withListComponent(ListComponent<E> listComponent) {
            this.listComponent = listComponent;
            return this;
        }

        public BulkEditorBuilder<E> withExclude(String exclude) {
            this.exclude = exclude;
            return this;
        }

        public BulkEditorBuilder<E> withIncludeProperties(List<String> includeProperties) {
            this.includeProperties = includeProperties;
            return this;
        }

        public BulkEditorBuilder<E> withFieldValidators(Map<String, Field.Validator> fieldValidators) {
            this.fieldValidators = fieldValidators;
            return this;
        }

        public BulkEditorBuilder<E> withModelValidators(List<Field.Validator> modelValidators) {
            this.modelValidators = modelValidators;
            return this;
        }

        public BulkEditorBuilder<E> withLoadDynamicAttributes(Boolean loadDynamicAttributes) {
            this.loadDynamicAttributes = loadDynamicAttributes;
            return this;
        }

        public BulkEditorBuilder<E> withUseConfirmDialog(Boolean useConfirmDialog) {
            this.useConfirmDialog = useConfirmDialog;
            return this;
        }

        public FrameOwner getOrigin() {
            return origin;
        }

        public MetaClass getMetaClass() {
            return metaClass;
        }

        public Collection<E> getEntities() {
            return entities;
        }

        public Screens.LaunchMode getLaunchMode() {
            return launchMode;
        }

        public ListComponent<E> getListComponent() {
            return listComponent;
        }

        public String getExclude() {
            return exclude;
        }

        public List<String> getIncludeProperties() {
            return includeProperties;
        }

        public Map<String, Field.Validator> getFieldValidators() {
            return fieldValidators;
        }

        public List<Field.Validator> getModelValidators() {
            return modelValidators;
        }

        public Boolean isLoadDynamicAttributes() {
            return loadDynamicAttributes;
        }

        public Boolean isUseConfirmDialog() {
            return useConfirmDialog;
        }

        public BulkEditorWindow create() {
            return handler.apply(this);
        }
    }
}
