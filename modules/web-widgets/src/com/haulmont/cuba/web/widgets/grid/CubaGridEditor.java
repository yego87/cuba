package com.haulmont.cuba.web.widgets.grid;

import com.vaadin.data.Binder;
import com.vaadin.data.HasValue;
import com.vaadin.data.PropertyDefinition;
import com.vaadin.data.PropertySet;
import com.vaadin.ui.Component;
import com.vaadin.ui.Grid.Column;
import com.vaadin.ui.components.grid.Editor;
import com.vaadin.ui.components.grid.EditorImpl;
import com.vaadin.ui.components.grid.EditorOpenEvent;
import com.vaadin.ui.components.grid.EditorSaveEvent;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class CubaGridEditor<T> extends EditorImpl<T> {

    protected CubaGridEditorFieldFactory<T> editorFieldFactory;

    /**
     * Constructor for internal implementation of the Editor.
     *
     * @param editorFieldFactory todo
     */
    public CubaGridEditor(CubaGridEditorFieldFactory<T> editorFieldFactory) {
        super(new PropertySet<T>() {
            @Override
            public Stream<PropertyDefinition<T, ?>> getProperties() {
                // No columns configured by default
                return Stream.empty();
            }

            @Override
            public Optional<PropertyDefinition<T, ?>> getProperty(String name) {
                throw new IllegalStateException(
                        "A Grid created without a bean type class literal or a custom property set"
                                + " doesn't support finding properties by name.");
            }
        });
        this.editorFieldFactory = editorFieldFactory;
    }

    public CubaGridEditorFieldFactory<T> getCubaEditorFieldFactory() {
        return editorFieldFactory;
    }

    @Override
    public Editor<T> setBinder(Binder<T> binder) {
        // do noting
        return this;
    }

    @Override
    protected void doEdit(T bean) {
        Objects.requireNonNull(bean, "Editor can't edit null");
        if (!isEnabled()) {
            throw new IllegalStateException(
                    "Editing is not allowed when Editor is disabled.");
        }

        edited = bean;

        List<Column<T, ?>> columns = getParent().getColumns();
        for (Column<T, ?> column : columns) {
            HasValue<?> field = editorFieldFactory.createField(bean, column);

            if (field != null) {
                assert field instanceof Component : "Grid should enforce that the binding field is a component";

                Component component = (Component) field;
                addComponentToGrid(component);
                columnFields.put(column, component);
                getState().columnFields.put(getInternalIdForColumn(column),
                        component.getConnectorId());
            }
        }

        fireEditorOpenEvent();
    }

    @Override
    public boolean save() {
        if (isOpen() && isBuffered()) {
            validate();
//            if (binder.writeBeanIfValid(edited)) {
            refresh(edited);
            eventRouter.fireEvent(new EditorSaveEvent<>(this, edited));
            return true;
//            }
        }
        return false;
    }

    protected void validate() {

    }

    //    protected void fireEditorOpenEvent(Map<Column, Field> columnFieldMap) {
    protected void fireEditorOpenEvent() {
//        eventRouter.fireEvent(new BeforeEditorOpenEvent(this, edited, columnFieldMap));
        eventRouter.fireEvent(new EditorOpenEvent<>(this, edited));
    }

    /*public class BeforeEditorOpenEvent extends com.vaadin.v7.ui.Grid.EditorEvent {
        Map<Column, Field> columnFieldMap;

        protected BeforeEditorOpenEvent(Grid source, Object itemID, Map<Column, Field> columnFieldMap) {
            super(source, itemID);
            this.columnFieldMap = columnFieldMap;
        }

        public Map<Column, Field> getColumnFieldMap() {
            return columnFieldMap;
        }
    }

    public interface BeforeEditorOpenListener {
        Method EDITOR_OPEN_METHOD =
                ReflectTools.findMethod(CubaGrid.BeforeEditorOpenListener.class, "beforeEditorOpened", CubaGrid.BeforeEditorOpenEvent.class);

        void beforeEditorOpened(CubaGrid.BeforeEditorOpenEvent event);
    }

    public void addEditorOpenListener(CubaGrid.BeforeEditorOpenListener listener) {
        addListener(CubaGrid.BeforeEditorOpenEvent.class, listener, EDITOR_OPEN_METHOD);
    }

    public void removeEditorOpenListener(CubaGrid.BeforeEditorOpenListener listener) {
        removeListener(CubaGrid.BeforeEditorOpenEvent.class, listener, EDITOR_OPEN_METHOD);
    }*/
}
