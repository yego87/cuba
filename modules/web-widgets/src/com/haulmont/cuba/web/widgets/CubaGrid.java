/*
 * Copyright (c) 2008-2017 Haulmont.
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

package com.haulmont.cuba.web.widgets;

import com.haulmont.cuba.web.widgets.client.grid.CubaGridState;
import com.haulmont.cuba.web.widgets.grid.CubaEditorField;
import com.haulmont.cuba.web.widgets.grid.CubaEditorImpl;
import com.vaadin.data.ValueProvider;
import com.vaadin.ui.Grid;
import com.vaadin.ui.components.grid.Editor;
import com.vaadin.ui.components.grid.GridSelectionModel;
import com.vaadin.ui.renderers.AbstractRenderer;
import com.vaadin.ui.renderers.Renderer;

import java.util.HashMap;
import java.util.Map;

//import static com.haulmont.cuba.web.widgets.CubaGrid.BeforeEditorOpenListener.EDITOR_OPEN_METHOD;
//import static com.haulmont.cuba.web.widgets.CubaGrid.EditorCloseListener.EDITOR_CLOSE_METHOD;
//import static com.haulmont.cuba.web.widgets.CubaGrid.EditorPostCommitListener.EDITOR_POST_COMMIT_METHOD;
//import static com.haulmont.cuba.web.widgets.CubaGrid.EditorPreCommitListener.EDITOR_PRE_COMMIT_METHOD;

public class CubaGrid<T> extends Grid<T> implements CubaEnhancedGrid<T> {

    protected CubaGridEditorFieldFactory<T> editorFieldFactory;

    @Override
    public void setGridSelectionModel(GridSelectionModel<T> model) {
        setSelectionModel(model);
    }

    @Override
    protected CubaGridState getState() {
        return (CubaGridState) super.getState();
    }

    @Override
    protected CubaGridState getState(boolean markAsDirty) {
        return (CubaGridState) super.getState(markAsDirty);
    }

    @Override
    public Map<String, String> getColumnIds() {
        return getState().columnIds;
    }

    @Override
    public void setColumnIds(Map<String, String> ids) {
        getState().columnIds = ids;
    }

    @Override
    public void addColumnId(String column, String value) {
        if (getState().columnIds == null) {
            getState().columnIds = new HashMap<>();
        }

        getState().columnIds.put(column, value);
    }

    @Override
    public void removeColumnId(String column) {
        if (getState().columnIds != null) {
            getState().columnIds.remove(column);
        }
    }

    @Override
    public void repaint() {
        markAsDirtyRecursive();
        getDataCommunicator().reset();
    }

    @Override
    protected <V, P> Column<T, V> createColumn(ValueProvider<T, V> valueProvider,
                                               ValueProvider<V, P> presentationProvider,
                                               AbstractRenderer<? super T, ? super P> renderer) {
        return new CubaColumn<>(valueProvider, presentationProvider, renderer);
    }

    @Override
    public CubaGridEditorFieldFactory<T> getCubaEditorFieldFactory() {
        return editorFieldFactory;
    }

    @Override
    public void setCubaEditorFieldFactory(CubaGridEditorFieldFactory<T> editorFieldFactory) {
        this.editorFieldFactory = editorFieldFactory;
    }

    @Override
    protected Editor<T> createEditor() {
        return new CubaEditorImpl<>(getPropertySet());
    }

    @Override
    public CubaEditorField<?> getColumnEditorField(T bean, Column<T, ?> column) {
        return editorFieldFactory.createField(bean, column);
    }

    public static class CubaColumn<T, V> extends Column<T, V> {

        protected <P> CubaColumn(ValueProvider<T, V> valueProvider,
                                 ValueProvider<V, P> presentationProvider,
                                 Renderer<? super P> renderer) {
            super(valueProvider, presentationProvider, renderer);
        }

        @Override
        public Column<T, V> setEditable(boolean editable) {
            // Removed check that editorBinding is not null,
            // because we don't use Vaadin binding.
            getState().editable = editable;
            return this;
        }
    }

//    protected void fireEditorPreCommitEvent() {
//        fireEvent(new EditorPreCommitEvent(this, editedItemId));
//    }
//
//    protected void fireEditorPostCommitEvent() {
//        fireEvent(new EditorPostCommitEvent(this, editedItemId));
//    }
//
//    protected void fireEditorCloseEvent(Object editedItemId) {
//        fireEvent(new EditorCloseEvent(this, editedItemId));
//    }
//
//    protected void fireEditorOpenEvent(Map<Column, Field> columnFieldMap) {
//        fireEvent(new BeforeEditorOpenEvent(this, editedItemId, columnFieldMap));
//    }
//
//    public class BeforeEditorOpenEvent extends com.vaadin.v7.ui.Grid.EditorEvent {
//        Map<Column, Field> columnFieldMap;
//
//        protected BeforeEditorOpenEvent(Grid source, Object itemID, Map<Column, Field> columnFieldMap) {
//            super(source, itemID);
//            this.columnFieldMap = columnFieldMap;
//        }
//
//        public Map<Column, Field> getColumnFieldMap() {
//            return columnFieldMap;
//        }
//    }
//
//    public interface BeforeEditorOpenListener {
//        Method EDITOR_OPEN_METHOD =
//                ReflectTools.findMethod(BeforeEditorOpenListener.class, "beforeEditorOpened", BeforeEditorOpenEvent.class);
//
//        void beforeEditorOpened(BeforeEditorOpenEvent event);
//    }
//
//    public void addEditorOpenListener(BeforeEditorOpenListener listener) {
//        addListener(BeforeEditorOpenEvent.class, listener, EDITOR_OPEN_METHOD);
//    }
//
//    public void removeEditorOpenListener(BeforeEditorOpenListener listener) {
//        removeListener(BeforeEditorOpenEvent.class, listener, EDITOR_OPEN_METHOD);
//    }
//
//    public interface EditorCloseListener {
//        Method EDITOR_CLOSE_METHOD =
//                ReflectTools.findMethod(EditorCloseListener.class, "editorClosed", EditorCloseEvent.class);
//
//        void editorClosed(EditorCloseEvent event);
//    }
//
//    public void addEditorCloseListener(EditorCloseListener listener) {
//        addListener(EditorCloseEvent.class, listener, EDITOR_CLOSE_METHOD);
//    }
//
//    public void removeEditorCloseListener(EditorCloseListener listener) {
//        removeListener(EditorCloseEvent.class, listener, EDITOR_CLOSE_METHOD);
//    }
//
//    public interface EditorPreCommitListener {
//        Method EDITOR_PRE_COMMIT_METHOD =
//                ReflectTools.findMethod(EditorPreCommitListener.class, "preCommit", EditorPreCommitEvent.class);
//
//        void preCommit(EditorPreCommitEvent event);
//    }
//
//    public static class EditorPreCommitEvent extends EditorEvent {
//        public EditorPreCommitEvent(Grid source, Object itemId) {
//            super(source, itemId);
//        }
//    }
//
//    public void addEditorPreCommitListener(EditorPreCommitListener listener) {
//        addListener(EditorPreCommitEvent.class, listener, EDITOR_PRE_COMMIT_METHOD);
//    }
//
//    public void removeEditorPreCommitListener(EditorPreCommitListener listener) {
//        removeListener(EditorPreCommitEvent.class, listener, EDITOR_PRE_COMMIT_METHOD);
//    }
//
//    public interface EditorPostCommitListener {
//        Method EDITOR_POST_COMMIT_METHOD =
//                ReflectTools.findMethod(EditorPostCommitListener.class, "postCommit", EditorPostCommitEvent.class);
//
//        void postCommit(EditorPostCommitEvent event);
//    }
//
//    public static class EditorPostCommitEvent extends EditorEvent {
//        public EditorPostCommitEvent(Grid source, Object itemId) {
//            super(source, itemId);
//        }
//    }
//
//    public void addEditorPostCommitListener(EditorPostCommitListener listener) {
//        addListener(EditorPostCommitEvent.class, listener, EDITOR_POST_COMMIT_METHOD);
//    }
//
//    public void removeEditorPostCommitListener(EditorPostCommitListener listener) {
//        removeListener(EditorPostCommitEvent.class, listener, EDITOR_POST_COMMIT_METHOD);
//    }
//
//    public class CubaDefaultEditorErrorHandler implements EditorErrorHandler {
//        @Override
//        public void commitError(CommitErrorEvent event) {
//            Map<Field<?>, Validator.InvalidValueException> invalidFields = event
//                    .getCause().getInvalidFields();
//
//            if (!invalidFields.isEmpty()) {
//                Object firstErrorPropertyId = null;
//                Field<?> firstErrorField = null;
//
//                for (Column column : getColumns()) {
//                    Object propertyId = column.getPropertyId();
//                    Field<?> field = (Field<?>) column.getState().editorConnector;
//
//                    if (invalidFields.keySet().contains(field)) {
//                        event.addErrorColumn(column);
//
//                        if (firstErrorPropertyId == null) {
//                            firstErrorPropertyId = propertyId;
//                            firstErrorField = field;
//                        }
//                    }
//                }
//
//
//                 * Validation error, show first failure as
//                 * "<Column header>: <message>"
//
//                String caption = getColumn(firstErrorPropertyId)
//                        .getHeaderCaption();
//                String message = invalidFields.get(firstErrorField)
//                        .getLocalizedMessage();
//
//                event.setUserErrorMessage(caption + ": " + message);
//            } else {
//                com.vaadin.server.ErrorEvent.findErrorHandler(CubaGrid.this)
//                        .error(new ConnectorErrorEvent(CubaGrid.this, event.getCause()));
//            }
//        }
//    }
}
