/*
 * Copyright (c) 2008-2016 Haulmont.
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

package com.haulmont.cuba.web.gui.components;

import com.google.common.collect.ImmutableMap;
import com.haulmont.bali.events.EventRouter;
import com.haulmont.bali.util.Dom4j;
import com.haulmont.bali.util.Preconditions;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.MessageTools;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.core.global.MetadataTools;
import com.haulmont.cuba.core.global.Security;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.gui.ComponentsHelper;
import com.haulmont.cuba.gui.components.AbstractAction;
import com.haulmont.cuba.gui.components.Action;
import com.haulmont.cuba.gui.components.ButtonsPanel;
import com.haulmont.cuba.gui.components.DataGrid;
import com.haulmont.cuba.gui.components.Field;
import com.haulmont.cuba.gui.components.Formatter;
import com.haulmont.cuba.gui.components.KeyCombination;
import com.haulmont.cuba.gui.components.LookupComponent;
import com.haulmont.cuba.gui.components.RowsCount;
import com.haulmont.cuba.gui.components.SecuredActionsHolder;
import com.haulmont.cuba.gui.components.VisibilityChangeNotifier;
import com.haulmont.cuba.gui.components.Window;
import com.haulmont.cuba.gui.components.WindowDelegate;
import com.haulmont.cuba.gui.components.data.DataGridSource;
import com.haulmont.cuba.gui.components.data.EntityDataGridSource;
import com.haulmont.cuba.gui.components.formatters.CollectionFormatter;
import com.haulmont.cuba.gui.components.security.ActionsPermissions;
import com.haulmont.cuba.gui.components.sys.ShowInfoAction;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.data.DsBuilder;
import com.haulmont.cuba.gui.data.impl.DatasourceImplementation;
import com.haulmont.cuba.gui.data.impl.WeakCollectionChangeListener;
import com.haulmont.cuba.gui.data.impl.WeakItemChangeListener;
import com.haulmont.cuba.gui.data.impl.WeakItemPropertyChangeListener;
import com.haulmont.cuba.gui.data.impl.WeakStateChangeListener;
import com.haulmont.cuba.gui.theme.ThemeConstants;
import com.haulmont.cuba.web.App;
import com.haulmont.cuba.web.AppUI;
import com.haulmont.cuba.web.gui.components.converters.FormatterBasedConverter;
import com.haulmont.cuba.web.gui.components.datagrid.DataGridColumnValueProvider;
import com.haulmont.cuba.web.gui.components.datagrid.DataGridDataProvider;
import com.haulmont.cuba.web.gui.components.datagrid.SortableDataGridDataProvider;
import com.haulmont.cuba.web.gui.components.renderers.RendererWrapper;
import com.haulmont.cuba.web.gui.components.renderers.WebButtonRenderer;
import com.haulmont.cuba.web.gui.components.renderers.WebCheckBoxRenderer;
import com.haulmont.cuba.web.gui.components.renderers.WebClickableTextRenderer;
import com.haulmont.cuba.web.gui.components.renderers.WebComponentRenderer;
import com.haulmont.cuba.web.gui.components.renderers.WebDateRenderer;
import com.haulmont.cuba.web.gui.components.renderers.WebHtmlRenderer;
import com.haulmont.cuba.web.gui.components.renderers.WebImageRenderer;
import com.haulmont.cuba.web.gui.components.renderers.WebNumberRenderer;
import com.haulmont.cuba.web.gui.components.renderers.WebProgressBarRenderer;
import com.haulmont.cuba.web.gui.components.renderers.WebTextRenderer;
import com.haulmont.cuba.web.gui.data.DataGridIndexedCollectionDsWrapper;
import com.haulmont.cuba.web.gui.data.SortableDataGridIndexedCollectionDsWrapper;
import com.haulmont.cuba.web.gui.icons.IconResolver;
import com.haulmont.cuba.web.widgets.CubaGrid;
import com.haulmont.cuba.web.widgets.CubaGridContextMenu;
import com.haulmont.cuba.web.widgets.addons.contextmenu.Menu;
import com.haulmont.cuba.web.widgets.addons.contextmenu.MenuItem;
import com.vaadin.data.ValueProvider;
import com.vaadin.server.ErrorMessage;
import com.vaadin.shared.ui.grid.HeightMode;
import com.vaadin.ui.AbstractField;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Element;
import org.springframework.beans.factory.InitializingBean;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.haulmont.bali.util.Preconditions.checkNotNullArgument;
import static com.haulmont.cuba.gui.ComponentsHelper.findActionById;

public class WebDataGrid<E extends Entity> extends WebAbstractComponent<CubaGrid<E>>
        implements DataGrid<E>, SecuredActionsHolder, LookupComponent.LookupSelectionChangeNotifier,
        InitializingBean {

    protected static final String HAS_TOP_PANEL_STYLE_NAME = "has-top-panel";
    protected static final String TEXT_SELECTION_ENABLED_STYLE = "text-selection-enabled";

    /* Beans */
    protected MetadataTools metadataTools;
    protected Security security;
    protected Messages messages;
    protected MessageTools messageTools;

    // Style names used by grid itself
    protected final List<String> internalStyles = new ArrayList<>();

//    protected GeneratedPropertyContainer containerWrapper;

    protected final Map<String, Column> columns = new HashMap<>();
    protected List<Column> columnsOrder = new ArrayList<>();
    protected final Map<String, ColumnGenerator<E, ?>> columnGenerators = new HashMap<>();

    protected final List<Action> actionList = new ArrayList<>();
    //    protected final ShortcutsDelegate<ShortcutListener> shortcutsDelegate;
    protected final ActionsPermissions actionsPermissions = new ActionsPermissions(this);

    protected CubaGridContextMenu<E> contextMenu;
    protected final List<ActionMenuItemWrapper> contextMenuItems = new ArrayList<>();

    protected boolean settingsEnabled = true;
    protected boolean sortable = true;
    protected boolean columnsCollapsingAllowed = true;
    protected boolean textSelectionEnabled = false;

    protected Action itemClickAction;
    protected Action enterPressAction;

    protected SelectionMode selectionMode;

    protected GridComposition componentComposition;
    protected HorizontalLayout topPanel;
    protected ButtonsPanel buttonsPanel;
    protected RowsCount rowsCount;

    protected List<RowStyleProvider> rowStyleProviders;
    protected List<CellStyleProvider> cellStyleProviders;

    protected RowDescriptionProvider<E> rowDescriptionProvider;
    protected CellDescriptionProvider<E> cellDescriptionProvider;

    protected DetailsGenerator<E> detailsGenerator = null;

    protected CollectionDsListenersWrapper collectionDsListenersWrapper;

//    protected Grid.ColumnVisibilityChangeListener columnCollapsingChangeListener;
//    protected Grid.ColumnResizeListener columnResizeListener;
//    protected com.vaadin.v7.event.SortEvent.SortListener sortListener;
//    protected com.vaadin.event.ContextClickEvent.ContextClickListener contextClickListener;
//    protected CubaGrid.EditorCloseListener editorCloseListener;
//    protected CubaGrid.BeforeEditorOpenListener beforeEditorOpenListener;
//    protected CubaGrid.EditorPreCommitListener editorPreCommitListener;
//    protected CubaGrid.EditorPostCommitListener editorPostCommitListener;

    protected final List<HeaderRow> headerRows = new ArrayList<>();
    protected final List<FooterRow> footerRows = new ArrayList<>();

    protected static final Map<Class<? extends Renderer>, Class<? extends Renderer>> rendererClasses;

    protected boolean showIconsForPopupMenuActions;

    protected DataGridDataProvider<E> dataBinding;

    static {
        ImmutableMap.Builder<Class<? extends Renderer>, Class<? extends Renderer>> builder =
                new ImmutableMap.Builder<>();

        builder.put(DataGrid.TextRenderer.class, WebTextRenderer.class);
        builder.put(DataGrid.ClickableTextRenderer.class, WebClickableTextRenderer.class);
        builder.put(DataGrid.HtmlRenderer.class, WebHtmlRenderer.class);
        builder.put(DataGrid.ProgressBarRenderer.class, WebProgressBarRenderer.class);
        builder.put(DataGrid.DateRenderer.class, WebDateRenderer.class);
        builder.put(DataGrid.NumberRenderer.class, WebNumberRenderer.class);
        builder.put(DataGrid.ButtonRenderer.class, WebButtonRenderer.class);
        builder.put(DataGrid.ImageRenderer.class, WebImageRenderer.class);
        builder.put(DataGrid.CheckBoxRenderer.class, WebCheckBoxRenderer.class);
        builder.put(DataGrid.ComponentRenderer.class, WebComponentRenderer.class);

        rendererClasses = builder.build();
    }

    public WebDataGrid() {
        component = createComponent();

//        Configuration configuration = AppBeans.get(Configuration.NAME);
//        ClientConfig clientConfig = configuration.getConfig(ClientConfig.class);
//        showIconsForPopupMenuActions = clientConfig.getShowIconsForPopupMenuActions();
//
//        shortcutsDelegate = new ShortcutsDelegate<ShortcutListener>() {
//            @Override
//            protected ShortcutListener attachShortcut(String actionId, KeyCombination keyCombination) {
//                ShortcutListener shortcut =
//                        new ShortcutListenerDelegate(actionId, keyCombination.getKey().getCode(),
//                                KeyCombination.Modifier.codes(keyCombination.getModifiers())
//                        ).withHandler((sender, target) -> {
//                            if (target == component) {
//                                Action action = getAction(actionId);
//                                if (action != null && action.isEnabled() && action.isVisible()) {
//                                    action.actionPerform(WebDataGrid.this);
//                                }
//                            }
//                        });
//
//                component.addShortcutListener(shortcut);
//                return shortcut;
//            }
//
//            @Override
//            protected void detachShortcut(Action action, ShortcutListener shortcutDescriptor) {
//                component.removeShortcutListener(shortcutDescriptor);
//            }
//
//            @Override
//            protected Collection<Action> getActions() {
//                return WebDataGrid.this.getActions();
//            }
//        };
//
//        /*component = new CubaGrid(createEditorFieldFactory()) {
//            @Override
//            protected boolean isEditingPermitted(Object id) {
//                CollectionDatasource collectionDatasource = WebDataGrid.this.datasource;
//
//                if (collectionDatasource != null) {
//                    //noinspection unchecked
//                    Entity entity = collectionDatasource.getItem(id);
//                    return security.isEntityOpPermitted(collectionDatasource.getMetaClass(), EntityOp.UPDATE) &&
//                            (entity != null && security.isPermitted(entity, ConstraintOperationType.UPDATE));
//                }
//                return true;
//            }
//        };*/
//        component = new CubaGrid();
//        initComponent(component);

//        initContextMenu();

//        initHeaderRows(component);
//        initFooterRows(component);
//        initEditor(component);
    }

    protected CubaGrid<E> createComponent() {
        return new CubaGrid<>();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        initComponent(component);

        initContextMenu();
    }

    @Inject
    public void setMetadataTools(MetadataTools metadataTools) {
        this.metadataTools = metadataTools;
    }

    @Inject
    public void setSecurity(Security security) {
        this.security = security;
    }

    @Inject
    public void setMessages(Messages messages) {
        this.messages = messages;
    }

    @Inject
    public void setMessageTools(MessageTools messageTools) {
        this.messageTools = messageTools;
    }

    protected void initComponent(CubaGrid component) {
        setSelectionMode(SelectionMode.SINGLE);

        component.setColumnReorderingAllowed(true);

        // TODO: gg, how to replace?
//        containerWrapper = new GeneratedPropertyContainer(component.getContainerDataSource());
//        component.setContainerDataSource(containerWrapper);

        /*component.addSelectionListener(e -> {
            if (getDataGridSource() == null) {
                return;
            }

            final Set<E> selected = getSelected();
            if (selected.isEmpty()) {
                Entity dsItem = datasource.getItemIfValid();
                //noinspection unchecked
                datasource.setItem(null);
                if (dsItem == null) {
                    // in this case item change event will not be generated
                    refreshActionsState();
                }
            } else {
                // reset selection and select new item
                if (isMultiSelect()) {
                    //noinspection unchecked
                    datasource.setItem(null);
                }

                Entity newItem = selected.iterator().next();
                Entity dsItem = datasource.getItemIfValid();
                //noinspection unchecked
                datasource.setItem(newItem);

                if (Objects.equals(dsItem, newItem)) {
                    // in this case item change event will not be generated
                    refreshActionsState();
                }
            }

            LookupSelectionChangeEvent selectionChangeEvent = new LookupSelectionChangeEvent(this);
            getEventRouter().fireEvent(LookupSelectionChangeListener.class,
                    LookupSelectionChangeListener::lookupValueChanged, selectionChangeEvent);

            if (getEventRouter().hasListeners(SelectionListener.class)) {
                List<E> addedItems = getItemsByIds(e.getAdded());
                List<E> removedItems = getItemsByIds(e.getRemoved());
                List<E> selectedItems = getItemsByIds(e.getSelected());

                SelectionEvent<E> event =
                        new SelectionEvent<>(WebDataGrid.this, addedItems, removedItems, selectedItems);
                //noinspection unchecked
                getEventRouter().fireEvent(SelectionListener.class, SelectionListener::selected, event);
            }
        });*/

//        component.addShortcutListener(
//                new ShortcutListenerDelegate("dataGridEnter", KeyCode.ENTER, null)
//                        .withHandler((sender, target) -> {
//                            if (target == this.component) {
//                                if (WebDataGrid.this.isEditorEnabled()) {
        // Prevent custom actions on Enter if DataGrid editor is enabled
        // since it's the default shortcut to open editor
//                                    return;
//                                }
//
//                                if (enterPressAction != null) {
//                                    enterPressAction.actionPerform(this);
//                                } else {
//                                    handleDoubleClickAction();
//                                }
//                            }
//                        }));

//        component.addItemClickListener(e -> {
//            if (e.isDoubleClick() && e.getItem() != null && !WebDataGrid.this.isEditorEnabled()) {
//                // note: for now Grid doesn't send double click if editor is enabled,
//                // but it's better to handle it manually
//                handleDoubleClickAction();
//            }
//
//            if (getEventRouter().hasListeners(ItemClickListener.class)) {
//                MouseEventDetails mouseEventDetails = WebWrapperUtils.toMouseEventDetails(e);
//
//                //noinspection unchecked
//                E item = (E) datasource.getItem(e.getItemId());
//                if (item == null) {
//                    // this can happen if user clicked on an item which is removed from the
//                    // datasource, so we don't want to send such event because it's useless
//                    return;
//                }
//                Column column = getColumnByPropertyId(e.getPropertyId());
//
//                ItemClickEvent<E> event = new ItemClickEvent<>(WebDataGrid.this,
//                        mouseEventDetails, item, e.getItemId(), column != null ? column.getId() : null);
//                //noinspection unchecked
//                getEventRouter().fireEvent(ItemClickListener.class, ItemClickListener::onItemClick, event);
//            }
//        });

//        component.addColumnReorderListener(e -> {
//            if (e.isUserOriginated()) {
        // Grid doesn't know about columns hidden by security permissions,
        // so we need to return them back to they previous positions
//                columnsOrder = restoreColumnsOrder(getColumnsOrderInternal());
//
//                if (getEventRouter().hasListeners(ColumnReorderListener.class)) {
//                    ColumnReorderEvent event = new ColumnReorderEvent(WebDataGrid.this);
//                    getEventRouter().fireEvent(ColumnReorderListener.class,
//                            ColumnReorderListener::columnReordered, event);
//                }
//            }
//        });

        componentComposition = new GridComposition();
        componentComposition.setPrimaryStyleName("c-data-grid-composition");
        componentComposition.setGrid(component);
        componentComposition.addComponent(component);
        componentComposition.setWidthUndefined();

        component.setSizeUndefined();
        component.setHeightMode(HeightMode.UNDEFINED);

        // TODO: gg, implement
//        component.setRowStyleGenerator(createRowStyleGenerator());
//        component.setCellStyleGenerator(createCellStyleGenerator());
    }

    protected void initEditor(Grid component) {
//        Messages messages = AppBeans.get(Messages.NAME);
//
//        component.setEditorSaveCaption(messages.getMainMessage("actions.Ok"));
//        component.setEditorCancelCaption(messages.getMainMessage("actions.Cancel"));
    }

    protected void initHeaderRows(Grid component) {
//        for (int i = 0; i < component.getHeaderRowCount(); i++) {
//            Grid.HeaderRow gridRow = component.getHeaderRow(i);
//            addHeaderRowInternal(gridRow);
//        }
    }

    protected void initFooterRows(Grid component) {
//        for (int i = 0; i < component.getFooterRowCount(); i++) {
//            Grid.FooterRow gridRow = component.getFooterRow(i);
//            addFooterRowInternal(gridRow);
//        }
    }

    protected List<Column> getColumnsOrderInternal() {
        List<Grid.Column<E, ?>> columnsOrder = component.getColumns();
        return columnsOrder.stream()
                .map(this::getColumnByGridColumn)
                .collect(Collectors.toList());
    }

    /**
     * Inserts columns hidden by security permissions (or with visible = false,
     * which means that where is no Grid.Column associated with DatGrid.Column)
     * into a list of visible columns, passed as a parameter, in they original positions.
     *
     * @param visibleColumns the list of DataGrid columns,
     *                       not hidden by security permissions
     * @return a list of all columns in DataGrid
     */
    protected List<Column> restoreColumnsOrder(List<Column> visibleColumns) {
        List<Column> newColumnsOrder = new ArrayList<>(visibleColumns);
        columnsOrder.stream()
                .filter(column -> !visibleColumns.contains(column))
                .forEach(column -> newColumnsOrder.add(columnsOrder.indexOf(column), column));
        return newColumnsOrder;
    }

    protected void initContextMenu() {
        contextMenu = new CubaGridContextMenu<>(component);

        contextMenu.addGridBodyContextMenuListener(event -> {
            if (!component.getSelectedItems().contains(event.getItem())) {
                // In the multi select model 'setSelected' adds item to selected set,
                // but, in case of context click, we want to have a single selected item,
                // if it isn't in a set of already selected items
                if (isMultiSelect()) {
                    component.deselectAll();
                }
                //noinspection unchecked
                setSelected(event.getItem());
            }
        });
    }

    protected void refreshActionsState() {
        getActions().forEach(Action::refreshState);
    }

    protected void handleDoubleClickAction() {
        Action action = getItemClickAction();
        if (action == null) {
            action = getEnterAction();
            if (action == null) {
                action = getAction("edit");
                if (action == null) {
                    action = getAction("view");
                }
            }
        }

        if (action != null && action.isEnabled()) {
            Window window = ComponentsHelper.getWindowImplementation(WebDataGrid.this);
            if (window instanceof Window.Wrapper) {
                window = ((Window.Wrapper) window).getWrappedWindow();
            }

            if (!(window instanceof Window.Lookup)) {
                action.actionPerform(WebDataGrid.this);
            } else {
                Window.Lookup lookup = (Window.Lookup) window;

                com.haulmont.cuba.gui.components.Component lookupComponent = lookup.getLookupComponent();
                if (lookupComponent != this)
                    action.actionPerform(WebDataGrid.this);
                else if (action.getId().equals(WindowDelegate.LOOKUP_ITEM_CLICK_ACTION_ID)) {
                    action.actionPerform(WebDataGrid.this);
                }
            }
        }
    }

    @Override
    public void focus() {
        component.focus();
    }

    @Override
    public void setLookupSelectHandler(Runnable selectHandler) {
        setEnterPressAction(new AbstractAction(WindowDelegate.LOOKUP_ENTER_PRESSED_ACTION_ID) {
            @Override
            public void actionPerform(com.haulmont.cuba.gui.components.Component component) {
                selectHandler.run();
            }
        });

        setItemClickAction(new AbstractAction(WindowDelegate.LOOKUP_ITEM_CLICK_ACTION_ID) {
            @Override
            public void actionPerform(com.haulmont.cuba.gui.components.Component component) {
                selectHandler.run();
            }
        });
    }

    @Override
    public Collection getLookupSelectedItems() {
        return getSelected();
    }

    protected Action getEnterAction() {
        for (Action action : getActions()) {
            KeyCombination kc = action.getShortcutCombination();
            if (kc != null) {
                if ((kc.getModifiers() == null || kc.getModifiers().length == 0)
                        && kc.getKey() == KeyCombination.Key.ENTER) {
                    return action;
                }
            }
        }
        return null;
    }

//    protected RowStyleGeneratorAdapter createRowStyleGenerator() {
//        return new RowStyleGeneratorAdapter();
//    }

//    protected CellStyleGeneratorAdapter createCellStyleGenerator() {
//        return new CellStyleGeneratorAdapter();
//    }

    /*protected CubaGridEditorFieldFactory createEditorFieldFactory() {
        return new WebDataGridEditorFieldFactory(this);
    }*/

    @Override
    public List<Column> getColumns() {
        return Collections.unmodifiableList(columnsOrder);
    }

    @Override
    public List<Column> getVisibleColumns() {
        return columnsOrder.stream()
                .filter(Column::isVisible)
                .collect(Collectors.toList());
    }

    @Nullable
    @Override
    public Column getColumn(String id) {
        return columns.get(id);
    }

    @Override
    public Column getColumnNN(String id) {
        Column column = getColumn(id);
        if (column == null) {
            throw new IllegalStateException("Unable to find column with id " + id);
        }
        return column;
    }

    @Override
    public void addColumn(Column column) {
        addColumn(column, columnsOrder.size());
    }

    @Override
    public void addColumn(Column column, int index) {
        checkNotNullArgument(column, "Column must be non null");
        if (column.getOwner() != null && column.getOwner() != this) {
            throw new IllegalArgumentException("Can't add column owned by another DataGrid");
        }
        addColumnInternal((ColumnImpl) column, index);
    }

    @Override
    public Column addColumn(String id, MetaPropertyPath propertyPath) {
        return addColumn(id, propertyPath, columnsOrder.size());
    }

    @Override
    public Column addColumn(String id, MetaPropertyPath propertyPath, int index) {
        ColumnImpl column = new ColumnImpl(id, propertyPath, this);
        addColumnInternal(column, index);
        return column;
    }

    protected void addColumnInternal(ColumnImpl column, int index) {
        Grid.Column gridColumn = component.addColumn(
                new DataGridColumnValueProvider<>(column.getPropertyPath()));

        columns.put(column.getId(), column);
        columnsOrder.add(index, column);

        final String caption = StringUtils.capitalize(column.getCaption() != null
                ? column.getCaption()
                : generateColumnCaption(column));
        column.setCaption(caption);

        if (column.getOwner() == null) {
            column.setOwner(this);
        }

        setupGridColumnProperties(gridColumn, column);

        component.setColumnOrder(getColumnOrder());
    }



    protected String generateColumnCaption(Column column) {
        return column.getPropertyPath() != null
                ? column.getPropertyPath().getMetaProperty().getName()
                : column.getId();
    }

    // TODO: gg, implement
    protected void setupGridColumnProperties(Grid.Column gridColumn, Column column) {
        gridColumn.setId(column.getId());
        gridColumn.setCaption(column.getCaption());
        gridColumn.setHidingToggleCaption(column.getCollapsingToggleCaption());
        if (column.isWidthAuto()) {
            gridColumn.setWidthUndefined();
        } else {
            gridColumn.setWidth(column.getWidth());
        }
        gridColumn.setExpandRatio(column.getExpandRatio());
        gridColumn.setMinimumWidth(column.getMinimumWidth());
        gridColumn.setMaximumWidth(column.getMaximumWidth());
        gridColumn.setHidden(column.isCollapsed());
        gridColumn.setHidable(column.isCollapsible() && column.getOwner().isColumnsCollapsingAllowed());
        gridColumn.setResizable(column.isResizable());
        // TODO: gg, set later?
//        gridColumn.setEditable(column.isEditable());

        AppUI current = AppUI.getCurrent();
        if (current != null && current.isTestMode()) {
            addColumnId(gridColumn, column);
        }

        // workaround to prevent exception from GridColumn while Grid is using default IndexedContainer
//        if (getContainerDataSource() instanceof SortableDataGridIndexedCollectionDsWrapper) {
//            gridColumn.setSortable(column.isSortable() && column.getOwner().isSortable());
//        }

        ((ColumnImpl) column).setGridColumn(gridColumn);

        if (column.getFormatter() != null) {
            FormatterBasedConverter converter = new FormatterBasedConverter(column.getFormatter());
//            gridColumn.setConverter(converter);
        } else {
            MetaProperty metaProperty = column.getPropertyPath() != null
                    ? column.getPropertyPath().getMetaProperty()
                    : null;

            if (metaProperty != null && Collection.class.isAssignableFrom(metaProperty.getJavaType())) {
                final FormatterBasedConverter converter = new FormatterBasedConverter(new CollectionFormatter(AppBeans.get(MetadataTools.class)));
//                gridColumn.setConverter(converter);
            } else {
                setDefaultConverter(gridColumn, metaProperty, column.getType());
                setDefaultRenderer(gridColumn, metaProperty, column.getType());
            }
        }
    }

    // TODO: gg, implement
    protected void addColumnId(Grid.Column gridColumn, Column column) {
//        component.addColumnId(gridColumn.getState().id, column.getId());
    }

    // TODO: gg, implement
    protected void removeColumnId(Grid.Column gridColumn) {
//        component.removeColumnId(gridColumn.getState().id);
    }

    // TODO: gg, implement
    protected void setDefaultRenderer(Grid.Column gridColumn, @Nullable MetaProperty metaProperty, Class type) {
        gridColumn.setRenderer(type == Boolean.class && metaProperty != null
                ? new com.vaadin.ui.renderers.HtmlRenderer()
                : new com.vaadin.ui.renderers.TextRenderer());
    }

    protected void setDefaultConverter(Grid.Column gridColumn, @Nullable MetaProperty metaProperty, Class type) {
//        gridColumn.setConverter(type == Boolean.class && metaProperty != null
//                ? new YesNoIconConverter()
//                : new StringToObjectConverter(metaProperty));
    }

    @Override
    public void removeColumn(Column column) {
        if (column == null) {
            return;
        }

        component.removeColumn(column.getId());

        columns.remove(column.getId());
        columnsOrder.remove(column);
        columnGenerators.remove(column.getId());

        ((ColumnImpl) column).setGridColumn(null);
        column.setOwner(null);
    }

    @Override
    public void removeColumn(String id) {
        removeColumn(getColumn(id));
    }

    @Nullable
    @Override
    public DataGridSource<E> getDataGridSource() {
        return this.dataBinding != null ? this.dataBinding.getDataGridSource() : null;
    }

    @Nullable
    protected EntityDataGridSource<E> getEntityDataGridSource() {
        return getDataGridSource() != null ? (EntityDataGridSource<E>) getDataGridSource() : null;
    }

    @Override
    public void setDataGridSource(DataGridSource<E> dataGridSource) {
        if (dataGridSource != null && !(dataGridSource instanceof EntityDataGridSource)) {
            throw new IllegalArgumentException("DataGrid supports only EntityDataGridSource");
        }

        if (this.dataBinding != null) {
            this.dataBinding.unbind();
            this.dataBinding = null;

            this.component.setDataProvider(null);
        }

        if (dataGridSource != null) {
            // DataGrid supports only EntityDataGridSource
            EntityDataGridSource<E> entityDataGridSource = (EntityDataGridSource<E>) dataGridSource;

            if (this.columns.isEmpty()) {
                setupAutowiredColumns(entityDataGridSource);
            }

            // TODO: gg, do we need this?
//            component.removeAllColumns();

            // Bind new datasource
            this.dataBinding = createDataGridDataProvider(dataGridSource);
            this.component.setDataProvider(this.dataBinding);

            createStubsForGeneratedColumns();

            List<Column> visibleColumnsOrder = getInitialVisibleColumns();

            setVisibleColumns(visibleColumnsOrder);

            for (Column column : visibleColumnsOrder) {
                Grid.Column gridColumn = ((ColumnImpl) column).getGridColumn();
                setupGridColumnProperties(gridColumn, column);
            }

            component.setColumnOrder(getColumnOrder());

            initShowInfoAction();

            if (rowsCount != null) {
                rowsCount.setRowsCountTarget(this);
            }

            if (!canBeSorted(dataGridSource)) {
                setSortable(false);
            }

            refreshActionsState();
        }
    }

    protected void setVisibleColumns(List<Column> visibleColumnsOrder) {
        // mark columns hidden by security permissions as visible = false
        // and remove Grid.Column to prevent its property changing
        columnsOrder.stream()
                .filter(column -> !visibleColumnsOrder.contains(column))
                .forEach(column -> {
                    ColumnImpl columnImpl = (ColumnImpl) column;
                    columnImpl.setVisible(false);
                    columnImpl.setGridColumn(null);
                });
    }

    protected void setupAutowiredColumns(EntityDataGridSource<E> entityDataGridSource) {
        Collection<MetaPropertyPath> paths = entityDataGridSource.getAutowiredProperties();

        for (MetaPropertyPath metaPropertyPath : paths) {
            MetaProperty property = metaPropertyPath.getMetaProperty();
            if (!property.getRange().getCardinality().isMany()
                    && !metadataTools.isSystem(property)) {
                String propertyName = property.getName();
                ColumnImpl column = new ColumnImpl(propertyName, metaPropertyPath, this);
                MetaClass propertyMetaClass = metadataTools.getPropertyEnclosingMetaClass(metaPropertyPath);
                column.setCaption(messageTools.getPropertyCaption(propertyMetaClass, propertyName));

                addColumn(column);
            }
        }
    }

    private DataGridDataProvider<E> createDataGridDataProvider(DataGridSource<E> dataGridSource) {
        // TODO: gg, pass this as a EventDelegate
//        return (dataGridSource instanceof DataGridSource.Sortable)
//                ? new SortableDataGridDataProvider<>(dataGridSource)
//                : new DataGridDataProvider<>(dataGridSource);
        return new DataGridDataProvider<>(dataGridSource);
    }

    protected String[] getColumnOrder() {
        //noinspection unchecked
        return columnsOrder.stream()
                .filter(Column::isVisible)
                .map(column -> ((ColumnImpl) column).getId())
                .toArray(String[]::new);
    }

    // TODO: gg, implement
    protected void createStubsForGeneratedColumns() {
//        PropertyValueGenerator generator = createDefaultPropertyValueGenerator();
        for (Column column : columnsOrder) {
            if (column.getPropertyPath() == null) {
//                containerWrapper.addGeneratedProperty(column.getId(), generator);
            }
        }
    }

    protected void initShowInfoAction() {
        if (security.isSpecificPermitted(ShowInfoAction.ACTION_PERMISSION)) {
            if (getAction(ShowInfoAction.ACTION_ID) == null) {
                addAction(new ShowInfoAction());
            }
        }
    }

    @Override
    public String getCaption() {
        return getComposition().getCaption();
    }

    @Override
    public void setCaption(String caption) {
        getComposition().setCaption(caption);
    }

    @Override
    public boolean isTextSelectionEnabled() {
        return textSelectionEnabled;
    }

    @Override
    public void setTextSelectionEnabled(boolean textSelectionEnabled) {
        if (this.textSelectionEnabled != textSelectionEnabled) {
            this.textSelectionEnabled = textSelectionEnabled;

            if (textSelectionEnabled) {
                if (!internalStyles.contains(TEXT_SELECTION_ENABLED_STYLE)) {
                    internalStyles.add(TEXT_SELECTION_ENABLED_STYLE);
                }

                componentComposition.addStyleName(TEXT_SELECTION_ENABLED_STYLE);
            } else {
                internalStyles.remove(TEXT_SELECTION_ENABLED_STYLE);
                componentComposition.removeStyleName(TEXT_SELECTION_ENABLED_STYLE);
            }
        }
    }

    @Override
    public boolean isColumnReorderingAllowed() {
        return component.isColumnReorderingAllowed();
    }

    @Override
    public void setColumnReorderingAllowed(boolean columnReorderingAllowed) {
        component.setColumnReorderingAllowed(columnReorderingAllowed);
    }

    @Override
    public boolean isSortable() {
        return sortable;
    }

    @Override
    public void setSortable(boolean sortable) {
        this.sortable = sortable
                && (getDataGridSource() == null
                || canBeSorted(getDataGridSource()));
        for (Column column : getColumns()) {
            ((ColumnImpl) column).updateSortable();
        }
    }

    @Override
    public boolean isColumnsCollapsingAllowed() {
        return columnsCollapsingAllowed;
    }

    @Override
    public void setColumnsCollapsingAllowed(boolean columnsCollapsingAllowed) {
        this.columnsCollapsingAllowed = columnsCollapsingAllowed;
        for (Column column : getColumns()) {
            ((ColumnImpl) column).updateCollapsible();
        }
    }

    protected Datasource createItemDatasource(Entity item) {
        EntityDataGridSource<E> entityDataGridSource = getEntityDataGridSource();
        if (entityDataGridSource == null) {
            return null;
        }

        Datasource fieldDatasource = DsBuilder.create()
                .setAllowCommit(false)
                .setMetaClass(entityDataGridSource.getEntityMetaClass())
                .setRefreshMode(CollectionDatasource.RefreshMode.NEVER)
                .setViewName(View.LOCAL)
                .buildDatasource();

        ((DatasourceImplementation) fieldDatasource).valid();

        //noinspection unchecked
        fieldDatasource.setItem(item);

        return fieldDatasource;
    }

    @Override
    public boolean isEditorEnabled() {
        return component.getEditor().isEnabled();
    }

    @Override
    public void setEditorEnabled(boolean isEnabled) {
        component.getEditor().setEnabled(isEnabled);
    }

    @Override
    public boolean isEditorBuffered() {
        return component.getEditor().isBuffered();
    }

    @Override
    public void setEditorBuffered(boolean editorBuffered) {
        component.getEditor().setBuffered(editorBuffered);
    }

    @Override
    public String getEditorSaveCaption() {
        return component.getEditor().getSaveCaption();
    }

    @Override
    public void setEditorSaveCaption(String saveCaption) {
        component.getEditor().setSaveCaption(saveCaption);
    }

    @Override
    public String getEditorCancelCaption() {
        return component.getEditor().getCancelCaption();
    }

    @Override
    public void setEditorCancelCaption(String cancelCaption) {
        component.getEditor().setCancelCaption(cancelCaption);
    }

    @Nullable
    @Override
    public Object getEditedItemId() {
        // TODO: gg, implement
//        return component.getEditedItemId();
        return null;
    }

    @Override
    public boolean isEditorActive() {
        return component.getEditor().isOpen();
    }

    @Override
    public void editItem(Object itemId) {
        checkNotNullArgument(itemId, "Item's Id must be non null");
        // TODO: gg, implement
        //noinspection unchecked
//        E item = (E) datasource.getItem(itemId);
//        edit(item);
    }

    @Override
    public void edit(E entity) {
        checkNotNullArgument(entity, "Entity must be non null");
        // TODO: gg, implement
        Object itemId = entity.getId();

        //noinspection unchecked
//        if (!datasource.containsItem(itemId)) {
//            throw new IllegalArgumentException("Datasource doesn't contain item");
//        }
//
//        component.editItem(itemId);
    }

    @Override
    public void addEditorOpenListener(EditorOpenListener listener) {
        getEventRouter().addListener(EditorOpenListener.class, listener);
        // TODO: gg, implement
//        if (beforeEditorOpenListener == null) {
//            beforeEditorOpenListener = event -> {
//                //noinspection ConstantConditions
//                Map<String, Field> fields = event.getColumnFieldMap().entrySet().stream()
//                        .filter(entry ->
//                                getColumnByGridColumn(entry.getKey()) != null)
//                        .collect(Collectors.toMap(
//                                entry -> getColumnByGridColumn(entry.getKey()).getId(),
//                                entry -> ((DataGridEditorCustomField) entry.getValue()).getField())
//                        );
//
//                EditorOpenEvent e = new EditorOpenEvent(WebDataGrid.this, event.getItem(), fields);
//                getEventRouter().fireEvent(EditorOpenListener.class, EditorOpenListener::beforeEditorOpened, e);
//            };
//            component.addEditorOpenListener(beforeEditorOpenListener);
//        }
    }

    @Override
    public void removeEditorOpenListener(EditorOpenListener listener) {
        getEventRouter().removeListener(EditorOpenListener.class, listener);
        // TODO: gg, implement
//        if (!getEventRouter().hasListeners(EditorOpenListener.class)) {
//            component.removeEditorOpenListener(beforeEditorOpenListener);
//            beforeEditorOpenListener = null;
//        }
    }

    @Override
    public void addEditorCloseListener(EditorCloseListener listener) {
        getEventRouter().addListener(EditorCloseListener.class, listener);
        // TODO: gg, implement
//        if (editorCloseListener == null) {
//            editorCloseListener = event -> {
//                EditorCloseEvent e = new EditorCloseEvent(WebDataGrid.this, event.getItem());
//                getEventRouter().fireEvent(EditorCloseListener.class, EditorCloseListener::editorClosed, e);
//            };
//            component.addEditorCloseListener(editorCloseListener);
//        }
    }

    @Override
    public void removeEditorCloseListener(EditorCloseListener listener) {
        getEventRouter().removeListener(EditorCloseListener.class, listener);
        // TODO: gg, implement
//        if (!getEventRouter().hasListeners(EditorCloseListener.class)) {
//            component.removeEditorCloseListener(editorCloseListener);
//            editorCloseListener = null;
//        }
    }

    @Override
    public void addEditorPreCommitListener(EditorPreCommitListener listener) {
        getEventRouter().addListener(EditorPreCommitListener.class, listener);
        // TODO: gg, implement
//        if (editorPreCommitListener == null) {
//            editorPreCommitListener = event -> {
//                EditorPreCommitEvent e = new EditorPreCommitEvent(WebDataGrid.this, event.getItem());
//                getEventRouter().fireEvent(EditorPreCommitListener.class, EditorPreCommitListener::preCommit, e);
//            };
//            component.addEditorPreCommitListener(editorPreCommitListener);
//        }
    }

    @Override
    public void removeEditorPreCommitListener(EditorPreCommitListener listener) {
        getEventRouter().removeListener(EditorPreCommitListener.class, listener);
        // TODO: gg, implement
//        if (!getEventRouter().hasListeners(EditorPreCommitListener.class)) {
//            component.removeEditorPreCommitListener(editorPreCommitListener);
//            editorPreCommitListener = null;
//        }
    }

    @Override
    public void addEditorPostCommitListener(EditorPostCommitListener listener) {
        getEventRouter().addListener(EditorPostCommitListener.class, listener);
        // TODO: gg, implement
//        if (editorPostCommitListener == null) {
//            editorPostCommitListener = event -> {
//                EditorPostCommitEvent e = new EditorPostCommitEvent(WebDataGrid.this, event.getItem());
//                getEventRouter().fireEvent(EditorPostCommitListener.class, EditorPostCommitListener::postCommit, e);
//            };
//            component.addEditorPostCommitListener(editorPostCommitListener);
//        }
    }

    @Override
    public void removeEditorPostCommitListener(EditorPostCommitListener listener) {
        getEventRouter().removeListener(EditorPostCommitListener.class, listener);
        // TODO: gg, implement
//        if (!getEventRouter().hasListeners(EditorPostCommitListener.class)) {
//            component.removeEditorPostCommitListener(editorPostCommitListener);
//            editorPostCommitListener = null;
//        }
    }

    // TODO: gg, implement
    /*protected static class WebDataGridEditorFieldFactory implements CubaGridEditorFieldFactory {

        protected WebDataGrid<?> dataGrid;
        protected DataGridEditorFieldFactory fieldFactory = AppBeans.get(DataGridEditorFieldFactory.NAME);

        public WebDataGridEditorFieldFactory(WebDataGrid dataGrid) {
            this.dataGrid = dataGrid;
        }

        @Nullable
        @Override
        public com.vaadin.v7.ui.Field<?> createField(Object itemId, Object propertyId) {
            Column column = dataGrid.getColumnByPropertyId(propertyId);
            if (column != null && !column.isEditable()) {
                return null;
            }

            //noinspection unchecked
            Entity entity = dataGrid.getDatasource().getItem(itemId);
            Datasource fieldDatasource = dataGrid.createItemDatasource(entity);
            String fieldPropertyId = String.valueOf(propertyId);

            Field columnComponent = column != null && column.getEditorFieldGenerator() != null
                    ? column.getEditorFieldGenerator().createField(fieldDatasource, fieldPropertyId)
                    : fieldFactory.createField(fieldDatasource, fieldPropertyId);
            columnComponent.setParent(dataGrid);
            columnComponent.setFrame(dataGrid.getFrame());

//            return createCustomField(columnComponent);
            return null;
        }

        protected CustomField createCustomField(final Field columnComponent) {
            if (!(columnComponent instanceof Buffered)) {
                throw new IllegalArgumentException("Editor field must implement " +
                        "com.haulmont.cuba.gui.components.Component.Buffered");
            }

            AbstractField<?> content = (AbstractField<?>) WebComponentsHelper.getComposition(columnComponent);

//            CustomField wrapper = new DataGridEditorCustomField(columnComponent) {
//                @Override
//                protected Component initContent() {
//                    return content;
//                }
//            };
//
            //noinspection unchecked
//            wrapper.setConverter(new ObjectToObjectConverter());
//            wrapper.setFocusDelegate(content);
//
//            wrapper.setReadOnly(content.isReadOnly());
//            wrapper.setRequired(content.isRequired());
//            wrapper.setRequiredError(content.getRequiredError());

//            columnComponent.addValueChangeListener(event -> wrapper.markAsDirty());
//
//            return wrapper;
            return null;
        }
    }*/

    // TODO: gg, implement
    protected static abstract class DataGridEditorCustomField extends CustomField {

        protected Field columnComponent;

        public DataGridEditorCustomField(Field columnComponent) {
            this.columnComponent = columnComponent;
        }

        @Override
        protected AbstractField<?> getContent() {
            return (AbstractField<?>) super.getContent();
        }

        protected Field getField() {
            return columnComponent;
        }

        //        @Override
//        public Class getType() {
//            return Object.class;
//        }
//
//        @Override
//        protected void setInternalValue(Object newValue) {
//            columnComponent.setValue(newValue);
//        }
//
//        @Override
//        protected Object getInternalValue() {
//            return columnComponent.getValue();
//        }
//
        @Override
        public ErrorMessage getErrorMessage() {
//            try {
//                validate();
//            } catch (Validator.InvalidValueException ignore) {
//            }
            return getContent().getErrorMessage();
        }

//        @Override
//        public boolean isBuffered() {
//            return ((Buffered) columnComponent).isBuffered();
//        }
//
//        @Override
//        public void setBuffered(boolean buffered) {
//            ((Buffered) columnComponent).setBuffered(buffered);
//        }
//
//        @Override
//        public void commit() throws com.vaadin.v7.data.Buffered.SourceException, Validator.InvalidValueException {
//            validate();
//            ((Buffered) columnComponent).commit();
//        }
//
//        @Override
//        public void validate() throws Validator.InvalidValueException {
//            try {
//                columnComponent.validate();
//            } catch (ValidationException e) {
//                throw new Validator.InvalidValueException(e.getDetailsMessage());
//            }
//        }

        //        @Override
//        public void discard() throws com.vaadin.v7.data.Buffered.SourceException {
//            ((Buffered) columnComponent).discard();
//        }
//
//        @Override
//        public boolean isModified() {
//            return ((Buffered) columnComponent).isModified();
//        }
//
        @Override
        public void setWidth(float width, Unit unit) {
            super.setWidth(width, unit);

            if (getContent() != null) {
                if (width < 0) {
                    getContent().setWidthUndefined();
                } else {
                    getContent().setWidth(100, Unit.PERCENTAGE);
                }
            }
        }

        @Override
        public void setHeight(float height, Unit unit) {
            super.setHeight(height, unit);

            if (getContent() != null) {
                if (height < 0) {
                    getContent().setHeightUndefined();
                } else {
                    getContent().setHeight(100, Unit.PERCENTAGE);
                }
            }
        }
    }

    @Override
    public boolean isHeaderVisible() {
        return component.isHeaderVisible();
    }

    @Override
    public void setHeaderVisible(boolean headerVisible) {
        component.setHeaderVisible(headerVisible);
    }

    @Override
    public boolean isContextMenuEnabled() {
        return contextMenu.isEnabled();
    }

    @Override
    public void setContextMenuEnabled(boolean contextMenuEnabled) {
        contextMenu.setEnabled(contextMenuEnabled);
    }

    @Override
    public ColumnResizeMode getColumnResizeMode() {
        // TODO: gg, implement
//        return convertToDataGridColumnResizeMode(component.getColumnResizeMode());
        return null;
    }

    // TODO: gg, move to a helper class
    @Nullable
    protected ColumnResizeMode convertToDataGridColumnResizeMode(com.vaadin.v7.shared.ui.grid.ColumnResizeMode mode) {
        switch (mode) {
            case ANIMATED:
                return ColumnResizeMode.ANIMATED;
            case SIMPLE:
                return ColumnResizeMode.SIMPLE;
        }
        return null;
    }

    @Override
    public void setColumnResizeMode(ColumnResizeMode mode) {
        // TODO: gg, implement
//        component.setColumnResizeMode(convertToGridColumnResizeMode(mode));
    }

    // TODO: gg, move to a helper class
    @Nullable
    protected com.vaadin.v7.shared.ui.grid.ColumnResizeMode convertToGridColumnResizeMode(ColumnResizeMode mode) {
        switch (mode) {
            case ANIMATED:
                return com.vaadin.v7.shared.ui.grid.ColumnResizeMode.ANIMATED;
            case SIMPLE:
                return com.vaadin.v7.shared.ui.grid.ColumnResizeMode.SIMPLE;
        }
        return null;
    }

    @Override
    public SelectionMode getSelectionMode() {
        return selectionMode;
    }

    @Override
    public void setSelectionMode(SelectionMode selectionMode) {
        // TODO: gg, implement
        this.selectionMode = selectionMode;
        switch (selectionMode) {
            case SINGLE:
//                component.setSelectionModel(new CubaSingleSelectionModel());
                break;
            case MULTI:
//                component.setSelectionModel(new CubaMultiSelectionModel());
                break;
            case MULTI_CHECK:
//                component.setSelectionModel(new CubaMultiCheckSelectionModel());
                break;
            case NONE:
                component.setSelectionMode(Grid.SelectionMode.NONE);
                break;
        }
    }

    @Override
    public boolean isMultiSelect() {
        return SelectionMode.MULTI.equals(selectionMode)
                || SelectionMode.MULTI_CHECK.equals(selectionMode);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public E getSingleSelected() {
//        final Collection selected = component.getSelectedRows();
//        return selected == null || selected.isEmpty() ?
//                null : (E) datasource.getItem(selected.iterator().next());
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<E> getSelected() {
        // TODO: gg, implement
//        Collection itemIds = component.getSelectedRows();
//
//        if (itemIds != null) {
//            Set res = new LinkedHashSet<>();
//            for (Object id : itemIds) {
//                Entity item = datasource.getItem(id);
//                if (item != null) {
//                    res.add(item);
//                }
//            }
//            return res;
//        } else {
//            return Collections.emptySet();
//        }
        return Collections.emptySet();
    }

    @Override
    public void setSelected(@Nullable E item) {
        if (getSelectionMode().equals(SelectionMode.NONE)) {
            return;
        }

        if (item == null) {
            component.deselectAll();
        } else {
            setSelected(Collections.singletonList(item));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setSelected(Collection<E> items) {
        // TODO: gg, implement
        List itemIds = new ArrayList();
        for (Entity item : items) {
//            if (!datasource.containsItem(item.getId())) {
//                throw new IllegalStateException("Datasource doesn't contain items");
//            }
            itemIds.add(item.getId());
        }
        setSelectedIds(itemIds);
    }

    protected void setSelectedIds(Collection<Object> itemIds) {
        // TODO: gg, implement
        switch (selectionMode) {
            case SINGLE:
                if (itemIds.size() > 0) {
                    Object itemId = itemIds.iterator().next();
//                    ((Grid.SelectionModel.Single) component.getSelectionModel()).select(itemId);
                } else {
                    component.deselectAll();
                }
                break;
            case MULTI:
            case MULTI_CHECK:
                component.deselectAll();
//                ((Grid.SelectionModel.Multi) component.getSelectionModel()).select(itemIds);
                break;
        }
    }

    @Override
    public void selectAll() {
        // TODO: gg, implement
        if (isMultiSelect()) {
//            ((Grid.SelectionModel.Multi) component.getSelectionModel()).selectAll();
        }
    }

    @Override
    public void sort(String columnId, SortDirection direction) {
        // TODO: gg, implement
        ColumnImpl column = (ColumnImpl) getColumnNN(columnId);
//        component.sort(column.getColumnPropertyId(), convertToGridSortDirection(direction));
    }

    @Override
    public List<SortOrder> getSortOrder() {
        // TODO: gg, implement
//        return convertToDataGridSortOrder(component.getSortOrder());
        return Collections.emptyList();
    }

    // TODO: gg, implement
    protected List<SortOrder> convertToDataGridSortOrder(List<com.vaadin.v7.data.sort.SortOrder> gridSortOrder) {
//        if (CollectionUtils.isEmpty(gridSortOrder)) {
        return Collections.emptyList();
//        }

//        return gridSortOrder.stream()
//                .map(sortOrder -> {
//                    Column column = getColumnByPropertyId(sortOrder.getPropertyId());
//                    return new SortOrder(column != null ? column.getId() : null,
//                            convertToDataGridSortDirection(sortOrder.getDirection()));
//                })
//                .collect(Collectors.toList());
    }

    @Override
    public void addAction(Action action) {
        int index = findActionById(actionList, action.getId());
        if (index < 0) {
            index = actionList.size();
        }

        addAction(action, index);
    }

    @Override
    public void addAction(Action action, int index) {
        checkNotNullArgument(action, "Action must be non null");

        int oldIndex = findActionById(actionList, action.getId());
        if (oldIndex >= 0) {
            removeAction(actionList.get(oldIndex));
            if (index > oldIndex) {
                index--;
            }
        }
        // TODO: gg, implement
        if (StringUtils.isNotEmpty(action.getCaption())) {
            ActionMenuItemWrapper menuItemWrapper = createContextMenuItem(action);
            menuItemWrapper.setAction(action);
            contextMenuItems.add(menuItemWrapper);
        }

        actionList.add(index, action);
//        shortcutsDelegate.addAction(null, action);
        attachAction(action);
        actionsPermissions.apply(action);
    }

    protected ActionMenuItemWrapper createContextMenuItem(Action action) {
        MenuItem menuItem = contextMenu.addItem(action.getCaption(), null);
        menuItem.setStyleName("c-cm-item");

        return new ActionMenuItemWrapper(menuItem, showIconsForPopupMenuActions) {
            @Override
            public void performAction(Action action) {
                action.actionPerform(WebDataGrid.this);
            }
        };
    }

    protected void attachAction(Action action) {
        if (action instanceof Action.HasTarget) {
            ((Action.HasTarget) action).setTarget(this);
        }

        action.refreshState();
    }

    @Override
    public void removeAction(@Nullable Action action) {
        if (actionList.remove(action)) {
            ActionMenuItemWrapper menuItemWrapper = null;
            for (ActionMenuItemWrapper menuItem : contextMenuItems) {
                if (menuItem.getAction() == action) {
                    menuItemWrapper = menuItem;
                    break;
                }
            }

            if (menuItemWrapper != null) {
                menuItemWrapper.setAction(null);
//                contextMenu.removeItem(menuItemWrapper.getMenuItem());
            }

//            shortcutsDelegate.removeAction(action);
        }
    }

    @Override
    public void removeAction(@Nullable String id) {
        Action action = getAction(id);
        if (action != null) {
            removeAction(action);
        }
    }

    @Override
    public void removeAllActions() {
        for (Action action : actionList.toArray(new Action[actionList.size()])) {
            removeAction(action);
        }
    }

    @Override
    public Collection<Action> getActions() {
        return Collections.unmodifiableCollection(actionList);
    }

    @Nullable
    @Override
    public Action getAction(String id) {
        for (Action action : getActions()) {
            if (Objects.equals(action.getId(), id)) {
                return action;
            }
        }
        return null;
    }

    @Override
    public ActionsPermissions getActionsPermissions() {
        return actionsPermissions;
    }

    protected DataGridIndexedCollectionDsWrapper createContainerDatasource(CollectionDatasource.Indexed datasource,
                                                                           Collection<MetaPropertyPath> columns,
                                                                           CollectionDsListenersWrapper collectionDsListenersWrapper) {
        return datasource instanceof CollectionDatasource.Sortable
                ? new SortableDataGridDsWrapper(datasource, columns, collectionDsListenersWrapper)
                : new DataGridDsWrapper(datasource, columns, collectionDsListenersWrapper);
    }

    protected List<Column> getInitialVisibleColumns() {
//        MetaClass metaClass = datasource.getMetaClass();
        return columnsOrder.stream()
                .filter(column -> {
                    MetaPropertyPath propertyPath = column.getPropertyPath();
//                    return propertyPath == null
//                            || security.isEntityAttrReadPermitted(metaClass, propertyPath.toString());
                    return false;
                })
                .collect(Collectors.toList());
    }

    protected List<MetaPropertyPath> getPropertyColumns() {
//        MetaClass metaClass = datasource.getMetaClass();
        return columnsOrder.stream()
                .filter(column -> {
                    MetaPropertyPath propertyPath = column.getPropertyPath();
//                    return propertyPath != null
//                            && security.isEntityAttrReadPermitted(metaClass, propertyPath.toString());
                    return false;
                })
                .map(Column::getPropertyPath)
                .collect(Collectors.toList());
    }

    @Override
    public Component getComposition() {
        return componentComposition;
    }

    @Override
    public int getFrozenColumnCount() {
        return component.getFrozenColumnCount();
    }

    @Override
    public void setFrozenColumnCount(int numberOfColumns) {
        component.setFrozenColumnCount(numberOfColumns);
    }

    @Override
    public void scrollTo(E item) {
        scrollTo(item, ScrollDestination.ANY);
    }

    @Override
    public void scrollTo(E item, ScrollDestination destination) {
        Preconditions.checkNotNullArgument(item);
//        if (!getContainerDataSource().getItemIds().contains(item.getId())) {
//            throw new IllegalArgumentException("Unable to find item in DataGrid");
//        }
//
//        component.scrollTo(item.getId(), convertToGridScrollDestination(destination));
    }

    @Nullable
    protected com.vaadin.v7.shared.ui.grid.ScrollDestination convertToGridScrollDestination(
            ScrollDestination destination) {
        switch (destination) {
            case ANY:
                return com.vaadin.v7.shared.ui.grid.ScrollDestination.ANY;
            case START:
                return com.vaadin.v7.shared.ui.grid.ScrollDestination.START;
            case MIDDLE:
                return com.vaadin.v7.shared.ui.grid.ScrollDestination.MIDDLE;
            case END:
                return com.vaadin.v7.shared.ui.grid.ScrollDestination.END;
        }
        return null;
    }

    @Override
    public void scrollToStart() {
        component.scrollToStart();
    }

    @Override
    public void scrollToEnd() {
        component.scrollToEnd();
    }

    @Override
    public void repaint() {
//        component.repaint();
    }

    protected boolean canBeSorted(@Nullable DataGridSource<E> dataGridSource) {
        // TODO: gg, implement
//        return dataGridSource instanceof DataGridSource.Sortable;
        return false;
    }

    @Override
    public void setDebugId(String id) {
        super.setDebugId(id);

        if (id != null) {
            componentComposition.setId(AppUI.getCurrent().getTestIdManager().getTestId(id + "_composition"));
        }
    }

    @Override
    public void setId(String id) {
        super.setId(id);

        if (id != null && AppUI.getCurrent().isTestMode()) {
            componentComposition.setCubaId(id + "_composition");
        }
    }

    @Override
    public void setStyleName(String name) {
        super.setStyleName(name);

        internalStyles.forEach(internalStyle ->
                componentComposition.addStyleName(internalStyle));
    }

    @Override
    public ButtonsPanel getButtonsPanel() {
        return buttonsPanel;
    }

    @Override
    public void setButtonsPanel(ButtonsPanel panel) {
        if (buttonsPanel != null && topPanel != null) {
            topPanel.removeComponent(WebComponentsHelper.unwrap(buttonsPanel));
            buttonsPanel.setParent(null);
        }
        buttonsPanel = panel;
        if (panel != null) {
            if (panel.getParent() != null && panel.getParent() != this) {
                throw new IllegalStateException("Component already has parent");
            }

            if (topPanel == null) {
                topPanel = createTopPanel();
                topPanel.setWidth("100%");
                componentComposition.addComponentAsFirst(topPanel);
            }
            topPanel.addComponent(WebComponentsHelper.unwrap(panel));
            if (panel instanceof VisibilityChangeNotifier) {
                ((VisibilityChangeNotifier) panel).addVisibilityChangeListener(event ->
                        updateCompositionStylesTopPanelVisible()
                );
            }
            panel.setParent(this);
        }

        updateCompositionStylesTopPanelVisible();
    }

    @Override
    public RowsCount getRowsCount() {
        return rowsCount;
    }

    @Override
    public void setRowsCount(RowsCount rowsCount) {
        if (this.rowsCount != null && topPanel != null) {
            topPanel.removeComponent(WebComponentsHelper.unwrap(this.rowsCount));
            this.rowsCount.setParent(null);
        }
        this.rowsCount = rowsCount;
        if (rowsCount != null) {
            if (rowsCount.getParent() != null && rowsCount.getParent() != this) {
                throw new IllegalStateException("Component already has parent");
            }

            if (topPanel == null) {
                topPanel = createTopPanel();
                topPanel.setWidth("100%");
                componentComposition.addComponentAsFirst(topPanel);
            }
            com.vaadin.ui.Component rc = WebComponentsHelper.unwrap(rowsCount);
            topPanel.addComponent(rc);
            topPanel.setExpandRatio(rc, 1);
            topPanel.setComponentAlignment(rc, com.vaadin.ui.Alignment.BOTTOM_RIGHT);

            if (rowsCount instanceof VisibilityChangeNotifier) {
                ((VisibilityChangeNotifier) rowsCount).addVisibilityChangeListener(event ->
                        updateCompositionStylesTopPanelVisible()
                );
            }
        }

        updateCompositionStylesTopPanelVisible();
    }

    protected HorizontalLayout createTopPanel() {
        HorizontalLayout topPanel = new HorizontalLayout();
        topPanel.setMargin(false);
        topPanel.setSpacing(false);
        topPanel.setStyleName("c-data-grid-top");
        return topPanel;
    }

    // if buttons panel becomes hidden we need to set top panel height to 0
    protected void updateCompositionStylesTopPanelVisible() {
        if (topPanel != null) {
            boolean hasChildren = topPanel.getComponentCount() > 0;
            boolean anyChildVisible = false;
            for (Component childComponent : topPanel) {
                if (childComponent.isVisible()) {
                    anyChildVisible = true;
                    break;
                }
            }
            boolean topPanelVisible = hasChildren && anyChildVisible;

            if (!topPanelVisible) {
                componentComposition.removeStyleName(HAS_TOP_PANEL_STYLE_NAME);

                internalStyles.remove(HAS_TOP_PANEL_STYLE_NAME);
            } else {
                componentComposition.addStyleName(HAS_TOP_PANEL_STYLE_NAME);

                if (!internalStyles.contains(HAS_TOP_PANEL_STYLE_NAME)) {
                    internalStyles.add(HAS_TOP_PANEL_STYLE_NAME);
                }
            }
        }
    }

    @Override
    public Action getEnterPressAction() {
        return enterPressAction;
    }

    @Override
    public void setEnterPressAction(Action action) {
        enterPressAction = action;
    }

    @Override
    public Action getItemClickAction() {
        return itemClickAction;
    }

    @Override
    public void setItemClickAction(Action action) {
        if (itemClickAction != null) {
            removeAction(itemClickAction);
        }
        itemClickAction = action;
        if (!getActions().contains(action)) {
            addAction(action);
        }
    }

    @Override
    public void applySettings(Element element) {
        if (!isSettingsEnabled()) {
            return;
        }

        final Element columnsElem = element.element("columns");
        if (columnsElem != null) {
            List<Column> modelColumns = getVisibleColumns();
            List<String> modelIds = modelColumns.stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList());

            List<String> loadedIds = Dom4j.elements(columnsElem, "columns").stream()
                    .map(colElem -> colElem.attributeValue("id"))
                    .collect(Collectors.toList());

            if (CollectionUtils.isEqualCollection(modelIds, loadedIds)) {
                applyColumnSettings(element, modelColumns);
            }
        }
    }

    protected void applyColumnSettings(Element element, Collection<Column> oldColumns) {
        final Element columnsElem = element.element("columns");

        List<Column> newColumns = new ArrayList<>();

        // add columns from saved settings
        for (Element colElem : Dom4j.elements(columnsElem, "columns")) {
            for (Column column : oldColumns) {
                if (column.getId().equals(colElem.attributeValue("id"))) {
                    newColumns.add(column);

                    String width = colElem.attributeValue("width");
                    if (width != null) {
                        column.setWidth(Double.parseDouble(width));
                    } else {
                        column.setWidthAuto();
                    }

                    String collapsed = colElem.attributeValue("collapsed");
                    if (collapsed != null && component.isColumnReorderingAllowed()) {
                        column.setCollapsed(Boolean.parseBoolean(collapsed));
                    }

                    break;
                }
            }
        }

        // add columns not saved in settings (perhaps new)
        for (Column column : oldColumns) {
            if (!newColumns.contains(column)) {
                newColumns.add(column);
            }
        }

        // if the data grid contains only one column, always show it
        if (newColumns.size() == 1) {
            newColumns.get(0).setCollapsed(false);
        }

        // TODO: gg, implement
        /*List<String> properties = newColumns.stream()
                .map(Column::getId)
                .collect(Collectors.toList());
        // We don't save settings for columns hidden by security permissions,
        // so we need to return them back to they initial positions
        columnsOrder = restoreColumnsOrder(newColumns);
        component.setColumnOrder(properties.toArray(new String[0]));

        if (isSortable()) {
            // apply sorting
            component.clearSortOrder();
            String sortPropertyName = columnsElem.attributeValue("sortProperty");
            if (!StringUtils.isEmpty(sortPropertyName)) {
                EntityDataGridSource<E> entityDataGridSource = getEntityDataGridSource();
                if (entityDataGridSource != null) {
                    MetaPropertyPath sortProperty = entityDataGridSource.getEntityMetaClass()
                            .getPropertyPath(sortPropertyName);
                    if (sortProperty != null && properties.contains(sortProperty)) {

                        String sortDirection = columnsElem.attributeValue("sortDirection");
                        if (StringUtils.isNotEmpty(sortDirection)) {
                            List<com.vaadin.v7.data.sort.SortOrder> sortOrders = Collections.singletonList(
                                    new com.vaadin.v7.data.sort.SortOrder(sortProperty,
                                            com.vaadin.shared.data.sort.SortDirection.valueOf(sortDirection)));
                            component.setSortOrder(sortOrders);
                        }
                    }
                }
            }
        }*/
    }

    @Override
    public boolean saveSettings(Element element) {
        if (!isSettingsEnabled()) {
            return false;
        }

        Element columnsElem = element.element("columns");
        if (columnsElem != null) {
            element.remove(columnsElem);
        }
        columnsElem = element.addElement("columns");

        List<Column> visibleColumns = getVisibleColumns();
        for (Column column : visibleColumns) {
            Element colElem = columnsElem.addElement("columns");
            colElem.addAttribute("id", column.toString());

            double width = column.getWidth();
            if (width > -1) {
                colElem.addAttribute("width", String.valueOf(width));
            }

            colElem.addAttribute("collapsed", Boolean.toString(column.isCollapsed()));
        }

//        List<com.vaadin.v7.data.sort.SortOrder> sortOrders = component.getSortOrder();
//        if (!sortOrders.isEmpty()) {
//            com.vaadin.v7.data.sort.SortOrder sortOrder = sortOrders.get(0);
//            columnsElem.addAttribute("sortProperty", sortOrder.getPropertyId().toString());
//            columnsElem.addAttribute("sortDirection", sortOrder.getDirection().toString());
//        }

        return true;
    }

    @Nullable
    protected Column getColumnByGridColumn(Grid.Column gridColumn) {
        for (Column column : getColumns()) {
            if (((ColumnImpl) column).getGridColumn() == gridColumn) {
                return column;
            }
        }
        return null;
    }

    @Nullable
    protected Column getColumnById(Object id) {
        for (Column column : getColumns()) {
            String columnId = column.getId();
            if (Objects.equals(columnId, id)) {
                return column;
            }
        }
        return null;
    }

    /*@Nullable
    protected Column getColumnByPropertyId(Object propertyId) {
        for (Column column : getColumns()) {
            Object columnPropertyId = ((ColumnImpl) column).getColumnPropertyId();
            if (Objects.equals(columnPropertyId, propertyId)) {
                return column;
            }
        }
        return null;
    }*/

    @Override
    public boolean isSettingsEnabled() {
        return settingsEnabled;
    }

    @Override
    public void setSettingsEnabled(boolean settingsEnabled) {
        this.settingsEnabled = settingsEnabled;
    }

    @Override
    public void addRowStyleProvider(RowStyleProvider<? super E> styleProvider) {
        if (this.rowStyleProviders == null) {
            this.rowStyleProviders = new LinkedList<>();
        }

        if (!this.rowStyleProviders.contains(styleProvider)) {
            this.rowStyleProviders.add(styleProvider);

//            component.repaint();
        }
    }

    @Override
    public void removeRowStyleProvider(RowStyleProvider<? super E> styleProvider) {
        if (this.rowStyleProviders != null) {
            if (this.rowStyleProviders.remove(styleProvider)) {
//                component.repaint();
            }
        }
    }

    @Override
    public void addCellStyleProvider(CellStyleProvider<? super E> styleProvider) {
        if (this.cellStyleProviders == null) {
            this.cellStyleProviders = new LinkedList<>();
        }

        if (!this.cellStyleProviders.contains(styleProvider)) {
            this.cellStyleProviders.add(styleProvider);

//            component.repaint();
        }
    }

    @Override
    public void removeCellStyleProvider(CellStyleProvider<? super E> styleProvider) {
        if (this.cellStyleProviders != null) {
            if (this.cellStyleProviders.remove(styleProvider)) {
//                component.repaint();
            }
        }
    }

    @Override
    public CellDescriptionProvider getCellDescriptionProvider() {
        return cellDescriptionProvider;
    }

    @Override
    public void setCellDescriptionProvider(CellDescriptionProvider<E> provider) {
        this.cellDescriptionProvider = provider;

        if (provider != null) {
//            component.setCellDescriptionGenerator(createCellDescriptionGenerator());
        } else {
//            component.setCellDescriptionGenerator(null);
        }
    }

//    protected Grid.CellDescriptionGenerator createCellDescriptionGenerator() {
//        return cell -> {
//            //noinspection unchecked
//            E item = (E) datasource.getItem(cell.getItemId());
//            Column column = getColumnByPropertyId(cell.getPropertyId());
//            if (column == null) {
//                throw new RuntimeException("Column not found for propertyId: " + cell.getPropertyId());
//            }
//            return cellDescriptionProvider.getDescription(item, column.getId());
//        };
//    }

    @Override
    public RowDescriptionProvider getRowDescriptionProvider() {
        return rowDescriptionProvider;
    }

    @Override
    public void setRowDescriptionProvider(RowDescriptionProvider<E> provider) {
        this.rowDescriptionProvider = provider;

        if (provider != null) {
//            component.setRowDescriptionGenerator(createRowDescriptionGenerator());
        } else {
//            component.setRowDescriptionGenerator(null);
        }
    }

//    protected Grid.RowDescriptionGenerator createRowDescriptionGenerator() {
//        return row -> {
//            //noinspection unchecked
//            E item = (E) datasource.getItem(row.getItemId());
//
//            return rowDescriptionProvider.getDescription(item);
//        };
//    }

    @Override
    public Column addGeneratedColumn(String columnId, ColumnGenerator<E, ?> generator) {
        return addGeneratedColumn(columnId, generator, columnsOrder.size());
    }

    @Override
    public Column addGeneratedColumn(String columnId, ColumnGenerator<E, ?> generator, int index) {
        checkNotNullArgument(columnId, "columnId is null");
        checkNotNullArgument(generator, "generator is null for column id '%s'", columnId);

        Column existingColumn = getColumn(columnId);
        if (existingColumn != null) {
            index = columnsOrder.indexOf(existingColumn);
            removeColumn(existingColumn);
        }

//        containerWrapper.addGeneratedProperty(columnId, new PropertyValueGenerator<Object>() {
//            @Override
//            public Object getValue(Item item, Object itemId, Object propertyId) {
//                //noinspection unchecked
//                ColumnGeneratorEvent<E> event = new ColumnGeneratorEvent<>(WebDataGrid.this,
//                        (E) datasource.getItem(itemId), propertyId.toString());
//
//                return generator.getValue(event);
//            }
//
//            @Override
//            public Class<Object> getType() {
//                //noinspection unchecked
//                return (Class<Object>) generator.getType();
//            }
//        });

        ColumnImpl column = new ColumnImpl(columnId, generator.getType(), this);
        if (existingColumn != null) {
            copyColumnProperties(column, existingColumn);
        } else {
            column.setCaption(columnId);
        }
        column.setGenerated(true);

        columns.put(column.getId(), column);
        columnsOrder.add(index, column);
        columnGenerators.put(column.getId(), generator);

//        Grid.Column gridColumn = component.getColumn(column.getColumnPropertyId());
//        if (gridColumn != null) {
//            setupGridColumnProperties(gridColumn, column);
//        }
//
//        component.setColumnOrder(getColumnPropertyIds());

        return column;
    }

    @Override
    public ColumnGenerator<E, ?> getColumnGenerator(String columnId) {
        return columnGenerators.get(columnId);
    }

    protected void copyColumnProperties(Column column, Column existingColumn) {
        column.setCaption(existingColumn.getCaption());
        column.setVisible(existingColumn.isVisible());
        column.setCollapsed(existingColumn.isCollapsed());
        column.setCollapsible(existingColumn.isCollapsible());
        column.setCollapsingToggleCaption(existingColumn.getCollapsingToggleCaption());
        column.setMinimumWidth(existingColumn.getMinimumWidth());
        column.setMaximumWidth(existingColumn.getMaximumWidth());
        column.setWidth(existingColumn.getWidth());
        column.setExpandRatio(existingColumn.getExpandRatio());
        column.setResizable(existingColumn.isResizable());
        column.setFormatter(existingColumn.getFormatter());
    }

    @Override
    public <T extends Renderer> T createRenderer(Class<T> type) {
        Class<? extends Renderer> rendererClass = rendererClasses.get(type);
        if (rendererClass == null) {
            throw new IllegalStateException(String.format("Can't find renderer class for '%s'", type.getTypeName()));
        }

        try {
            return type.cast(rendererClass.newInstance());
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(String.format("Error creating the '%s' renderer instance",
                    type.getTypeName()), e);
        }
    }

    @Override
    public void addColumnCollapsingChangeListener(ColumnCollapsingChangeListener listener) {
        getEventRouter().addListener(ColumnCollapsingChangeListener.class, listener);

//        if (columnCollapsingChangeListener == null) {
//            columnCollapsingChangeListener = (Grid.ColumnVisibilityChangeListener) e -> {
//                if (e.isUserOriginated()) {
//                    ColumnCollapsingChangeEvent event = new ColumnCollapsingChangeEvent(WebDataGrid.this,
//                            getColumnByGridColumn(e.getColumn()), e.isHidden());
//                    getEventRouter().fireEvent(ColumnCollapsingChangeListener.class,
//                            ColumnCollapsingChangeListener::columnCollapsingChanged, event);
//                }
//            };
//            component.addColumnVisibilityChangeListener(columnCollapsingChangeListener);
//        }
    }

    @Override
    public void removeColumnCollapsingChangeListener(ColumnCollapsingChangeListener listener) {
        getEventRouter().removeListener(ColumnCollapsingChangeListener.class, listener);

//        if (!getEventRouter().hasListeners(ColumnCollapsingChangeListener.class)) {
//            component.removeColumnVisibilityChangeListener(columnCollapsingChangeListener);
//            columnCollapsingChangeListener = null;
//        }
    }

    @Override
    public void addColumnReorderListener(ColumnReorderListener listener) {
        getEventRouter().addListener(ColumnReorderListener.class, listener);
    }

    @Override
    public void removeColumnReorderListener(ColumnReorderListener listener) {
        getEventRouter().removeListener(ColumnReorderListener.class, listener);
    }

    @Override
    public void addColumnResizeListener(ColumnResizeListener listener) {
        getEventRouter().addListener(ColumnResizeListener.class, listener);

//        if (columnResizeListener == null) {
//            columnResizeListener = (Grid.ColumnResizeListener) e -> {
//                if (e.isUserOriginated()) {
//                    ColumnResizeEvent event =
//                            new ColumnResizeEvent(WebDataGrid.this, getColumnByGridColumn(e.getColumn()));
//                    getEventRouter().fireEvent(ColumnResizeListener.class, ColumnResizeListener::columnResized, event);
//                }
//            };
//            component.addColumnResizeListener(columnResizeListener);
//        }
    }

    @Override
    public void removeColumnResizeListener(ColumnResizeListener listener) {
        getEventRouter().removeListener(ColumnResizeListener.class, listener);

//        if (!getEventRouter().hasListeners(ColumnResizeListener.class)) {
//            component.removeColumnResizeListener(columnResizeListener);
//            columnResizeListener = null;
//        }
    }

    @Override
    public void addSortListener(SortListener listener) {
        getEventRouter().addListener(SortListener.class, listener);

//        if (sortListener == null) {
//            sortListener = (com.vaadin.v7.event.SortEvent.SortListener) e -> {
//                if (e.isUserOriginated()) {
//                    List<SortOrder> sortOrders = convertToDataGridSortOrder(e.getSortOrder());
//
//                    SortEvent event = new SortEvent(WebDataGrid.this, sortOrders);
//                    getEventRouter().fireEvent(SortListener.class, SortListener::sorted, event);
//                }
//            };
//            component.addSortListener(sortListener);
//        }
    }

    protected SortDirection convertToDataGridSortDirection(com.vaadin.shared.data.sort.SortDirection sortDirection) {
        switch (sortDirection) {
            case ASCENDING:
                return SortDirection.ASCENDING;
            case DESCENDING:
                return SortDirection.DESCENDING;
            default:
                throw new UnsupportedOperationException("Unsupported SortDirection");
        }
    }

    protected com.vaadin.shared.data.sort.SortDirection convertToGridSortDirection(SortDirection sortDirection) {
        switch (sortDirection) {
            case ASCENDING:
                return com.vaadin.shared.data.sort.SortDirection.ASCENDING;
            case DESCENDING:
                return com.vaadin.shared.data.sort.SortDirection.DESCENDING;
            default:
                throw new UnsupportedOperationException("Unsupported SortDirection");
        }
    }

    @Override
    public void removeSortListener(SortListener listener) {
        getEventRouter().removeListener(SortListener.class, listener);

        if (!getEventRouter().hasListeners(SortListener.class)) {
//            component.removeSortListener(sortListener);
//            sortListener = null;
        }
    }

    @Override
    public void addContextClickListener(ContextClickListener listener) {
        getEventRouter().addListener(ContextClickListener.class, listener);

//        if (contextClickListener == null) {
//            contextClickListener = e -> {
//                MouseEventDetails mouseEventDetails = WebWrapperUtils.toMouseEventDetails(e);
//
//                ContextClickEvent event = new ContextClickEvent(WebDataGrid.this, mouseEventDetails);
//                getEventRouter().fireEvent(ContextClickListener.class, ContextClickListener::onContextClick, event);
//            };
//            component.addContextClickListener(contextClickListener);
//        }
    }

    @Override
    public void removeContextClickListener(ContextClickListener listener) {
        getEventRouter().removeListener(ContextClickListener.class, listener);

//        if (!getEventRouter().hasListeners(ContextClickListener.class)) {
//            component.removeContextClickListener(contextClickListener);
//            contextClickListener = null;
//        }
    }

    @Override
    public void addItemClickListener(ItemClickListener<E> listener) {
        getEventRouter().addListener(ItemClickListener.class, listener);
    }

    @Override
    public void removeItemClickListener(ItemClickListener<E> listener) {
        getEventRouter().removeListener(ItemClickListener.class, listener);
    }

    @Override
    public HeaderRow getHeaderRow(int index) {
//        return getHeaderRowByGridRow(component.getHeaderRow(index));
        return null;
    }

//    @Nullable
//    protected HeaderRow getHeaderRowByGridRow(Grid.HeaderRow gridRow) {
//        for (HeaderRow headerRow : headerRows) {
//            if (((HeaderRowImpl) headerRow).getGridRow() == gridRow) {
//                return headerRow;
//            }
//        }
//        return null;
//    }

    @Override
    public HeaderRow appendHeaderRow() {
//        Grid.HeaderRow gridRow = component.appendHeaderRow();
//        return addHeaderRowInternal(gridRow);
        return null;
    }

    @Override
    public HeaderRow prependHeaderRow() {
//        Grid.HeaderRow gridRow = component.prependHeaderRow();
//        return addHeaderRowInternal(gridRow);
        return null;
    }

    @Override
    public HeaderRow addHeaderRowAt(int index) {
//        Grid.HeaderRow gridRow = component.addHeaderRowAt(index);
//        return addHeaderRowInternal(gridRow);
        return null;
    }

//    protected HeaderRow addHeaderRowInternal(Grid.HeaderRow gridRow) {
//        HeaderRowImpl rowImpl = new HeaderRowImpl(this, gridRow);
//        headerRows.add(rowImpl);
//        return rowImpl;
//    }

    @Override
    public void removeHeaderRow(HeaderRow headerRow) {
        if (headerRow == null || !headerRows.contains(headerRow)) {
            return;
        }

//        component.removeHeaderRow(((HeaderRowImpl) headerRow).getGridRow());
        headerRows.remove(headerRow);
    }

    @Override
    public void removeHeaderRow(int index) {
        HeaderRow headerRow = getHeaderRow(index);
        removeHeaderRow(headerRow);
    }

    @Override
    public HeaderRow getDefaultHeaderRow() {
//        return getHeaderRowByGridRow(component.getDefaultHeaderRow());
        return null;
    }

    @Override
    public void setDefaultHeaderRow(HeaderRow headerRow) {
//        component.setDefaultHeaderRow(((HeaderRowImpl) headerRow).getGridRow());
    }

    @Override
    public int getHeaderRowCount() {
        return component.getHeaderRowCount();
    }

    @Override
    public FooterRow getFooterRow(int index) {
//        return getFooterRowByGridRow(component.getFooterRow(index));
        return null;
    }

//    @Nullable
//    protected FooterRow getFooterRowByGridRow(Grid.FooterRow gridRow) {
//        for (FooterRow footerRow : footerRows) {
//            if (((FooterRowImpl) footerRow).getGridRow() == gridRow) {
//                return footerRow;
//            }
//        }
//        return null;
//    }

    @Override
    public FooterRow appendFooterRow() {
//        Grid.FooterRow gridRow = component.appendFooterRow();
//        return addFooterRowInternal(gridRow);
        return null;
    }

    @Override
    public FooterRow prependFooterRow() {
//        Grid.FooterRow gridRow = component.prependFooterRow();
//        return addFooterRowInternal(gridRow);
        return null;
    }

    @Override
    public FooterRow addFooterRowAt(int index) {
//        Grid.FooterRow gridRow = component.addFooterRowAt(index);
//        return addFooterRowInternal(gridRow);
        return null;
    }

//    protected FooterRow addFooterRowInternal(Grid.FooterRow gridRow) {
//        FooterRowImpl rowImpl = new FooterRowImpl(this, gridRow);
//        footerRows.add(rowImpl);
//        return rowImpl;
//    }

    @Override
    public void removeFooterRow(FooterRow footerRow) {
        if (footerRow == null || !footerRows.contains(footerRow)) {
            return;
        }

//        component.removeFooterRow(((FooterRowImpl) footerRow).getGridRow());
        footerRows.remove(footerRow);
    }

    @Override
    public void removeFooterRow(int index) {
        FooterRow footerRow = getFooterRow(index);
        removeFooterRow(footerRow);
    }

    @Override
    public int getFooterRowCount() {
        return component.getFooterRowCount();
    }

    @Override
    public void addSelectionListener(SelectionListener<E> listener) {
        getEventRouter().addListener(SelectionListener.class, listener);
    }

    protected List<E> getItemsByIds(Set itemIds) {
        //noinspection unchecked
//        return (List<E>) itemIds.stream()
//                .map(itemId -> datasource.getItem(itemId))
//                .collect(Collectors.toList());
        return null;
    }

    @Override
    public void removeSelectionListener(SelectionListener<E> listener) {
        getEventRouter().removeListener(SelectionListener.class, listener);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    protected String getGeneratedRowStyle(Object itemId) {
        if (rowStyleProviders == null) {
            return null;
        }

//        Entity item = datasource.getItem(itemId);
        StringBuilder joinedStyle = null;
//        for (RowStyleProvider styleProvider : rowStyleProviders) {
//            String styleName = styleProvider.getStyleName(item);
//            if (styleName != null) {
//                if (joinedStyle == null) {
//                    joinedStyle = new StringBuilder(styleName);
//                } else {
//                    joinedStyle.append(" ").append(styleName);
//                }
//            }
//        }

        return joinedStyle != null ? joinedStyle.toString() : null;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    protected String getGeneratedCellStyle(Object itemId, Object propertyId) {
        if (cellStyleProviders == null) {
            return null;
        }

//        Entity item = datasource.getItem(itemId);
        StringBuilder joinedStyle = null;
//        for (CellStyleProvider styleProvider : cellStyleProviders) {
//            Column column = getColumnByPropertyId(propertyId);
//            if (column == null) {
//                throw new RuntimeException("Column not found for propertyId: " + propertyId);
//            }
//            String styleName = styleProvider.getStyleName(item, column.getId());
//            if (styleName != null) {
//                if (joinedStyle == null) {
//                    joinedStyle = new StringBuilder(styleName);
//                } else {
//                    joinedStyle.append(" ").append(styleName);
//                }
//            }
//        }

        return joinedStyle != null ? joinedStyle.toString() : null;
    }

    @Nullable
    @Override
    public DetailsGenerator<E> getDetailsGenerator() {
        return detailsGenerator;
    }

    @Override
    public void setDetailsGenerator(DetailsGenerator<E> detailsGenerator) {
        this.detailsGenerator = detailsGenerator;

        if (detailsGenerator != null) {
//            component.setDetailsGenerator(createDetailsGenerator());
        } else {
//            component.setDetailsGenerator(Grid.DetailsGenerator.NULL);
        }
    }

//    protected Grid.DetailsGenerator createDetailsGenerator() {
//        return (Grid.DetailsGenerator) rowReference -> {
    // noinspection unchecked
//            E item = (E) datasource.getItem(rowReference.getItemId());
//            com.haulmont.cuba.gui.components.Component component = detailsGenerator.getDetails(item);
//            return component.unwrapComposition(Component.class);
//        };
//    }

    @Override
    public boolean isDetailsVisible(Entity entity) {
//        return component.isDetailsVisible(entity.getId());
        return false;
    }

    @Override
    public void setDetailsVisible(Entity entity, boolean visible) {
//        component.setDetailsVisible(entity.getId(), visible);
    }

    @Override
    public int getTabIndex() {
        return component.getTabIndex();
    }

    @Override
    public void setTabIndex(int tabIndex) {
        component.setTabIndex(tabIndex);
    }

    @Override
    public void addLookupValueChangeListener(LookupSelectionChangeListener listener) {
        getEventRouter().addListener(LookupSelectionChangeListener.class, listener);
    }

    @Override
    public void removeLookupValueChangeListener(LookupSelectionChangeListener listener) {
        getEventRouter().removeListener(LookupSelectionChangeListener.class, listener);
    }

    protected class DataGridDsWrapper extends DataGridIndexedCollectionDsWrapper {

        public DataGridDsWrapper(CollectionDatasource.Indexed datasource, Collection<MetaPropertyPath> properties,
                                 CollectionDsListenersWrapper collectionDsListenersWrapper) {
            super(datasource, properties, true, collectionDsListenersWrapper);
        }

        @Override
        protected void createProperties(View view, MetaClass metaClass) {
            if (columns.isEmpty()) {
                super.createProperties(view, metaClass);
            } else {
                columns.values().forEach(column ->
                        properties.add(column.getPropertyPath()));
            }
        }
    }

    protected class SortableDataGridDsWrapper
            extends SortableDataGridIndexedCollectionDsWrapper {

        public SortableDataGridDsWrapper(CollectionDatasource.Indexed datasource,
                                         Collection<MetaPropertyPath> properties,
                                         CollectionDsListenersWrapper collectionDsListenersWrapper) {
            super(datasource, properties, true, collectionDsListenersWrapper);
        }

        @Override
        protected void createProperties(View view, MetaClass metaClass) {
            if (columns.isEmpty()) {
                super.createProperties(view, metaClass);
            } else {
                columns.values().forEach(column ->
                        properties.add(column.getPropertyPath()));
            }
        }

        @Override
        public void sort(Object[] propertyId, boolean[] ascending) {
            // FIXME: gg, workaround to prevent exception from datasource
            if (propertyId.length == 1) {
                super.sort(propertyId, ascending);
            }
        }

        @Override
        public Collection getSortableContainerPropertyIds() {
            Collection<?> ids = new ArrayList<>(super.getSortableContainerPropertyIds());
            for (Column column : getColumns()) {
                if (!column.isSortable()) {
                    ids.remove(column.getPropertyPath());
                }
            }
            return ids;
        }
    }

//    protected class RowStyleGeneratorAdapter implements Grid.RowStyleGenerator {
//        @Override
//        public String getStyle(Grid.RowReference row) {
//            return getGeneratedRowStyle(row.getItemId());
//        }
//    }

//    protected class CellStyleGeneratorAdapter implements Grid.CellStyleGenerator {
//        @Override
//        public String getStyle(Grid.CellReference cell) {
//            return getGeneratedCellStyle(cell.getItemId(), cell.getPropertyId());
//        }
//    }

    public static abstract class AbstractRenderer<T> implements RendererWrapper<T> {
        protected com.vaadin.v7.ui.renderers.Renderer<T> renderer;
        protected WebDataGrid dataGrid;
        protected String nullRepresentation;

        protected AbstractRenderer() {
        }

        protected AbstractRenderer(String nullRepresentation) {
            this.nullRepresentation = nullRepresentation;
        }

        @Override
        public com.vaadin.v7.ui.renderers.Renderer<T> getImplementation() {
            if (renderer == null) {
                renderer = createImplementation();
            }
            return renderer;
        }

        protected abstract com.vaadin.v7.ui.renderers.Renderer<T> createImplementation();

        public com.vaadin.v7.data.util.converter.Converter<? extends T, ?> getConverter() {
            // Some renderers need specific converter to be set at the same time
            // (see com.vaadin.ui.Grid.Column.setRenderer(Renderer<T>, Converter<? extends T,?>)).
            // Default `null` means do not use any converters
            return null;
        }

        @Override
        public void resetImplementation() {
            renderer = null;
        }

        protected WebDataGrid getDataGrid() {
            return dataGrid;
        }

        protected void setDataGrid(WebDataGrid dataGrid) {
            this.dataGrid = dataGrid;
        }

        protected String getNullRepresentation() {
            return nullRepresentation;
        }

        protected void setNullRepresentation(String nullRepresentation) {
            checkRendererNotSet();
            this.nullRepresentation = nullRepresentation;
        }

        protected Column getColumnByGridColumn(Grid.Column column) {
            return dataGrid.getColumnByGridColumn(column);
        }

        protected void checkRendererNotSet() {
            if (renderer != null) {
                throw new IllegalStateException("Renderer parameters cannot be changed after it is set to a column");
            }
        }
    }

    protected static class ColumnImpl<E> implements Column<E> {

        protected String id;
        protected MetaPropertyPath propertyPath;

        protected String caption;
        protected String collapsingToggleCaption;
        protected double width;
        protected double minWidth;
        protected double maxWidth;
        protected int expandRatio;
        protected boolean collapsed;
        protected boolean visible;
        protected boolean collapsible;
        protected boolean sortable;
        protected boolean resizable;
        protected boolean editable;
        protected boolean generated;
        protected Formatter formatter;

        protected AbstractRenderer renderer;
        protected Converter converter;

        protected Class type;
        protected Element element;

        protected WebDataGrid owner;
        protected Grid.Column gridColumn;

        protected ColumnEditorFieldGenerator fieldGenerator;

        public ColumnImpl(String id, @Nullable MetaPropertyPath propertyPath, WebDataGrid owner) {
            this(id, propertyPath, propertyPath != null ? propertyPath.getRangeJavaClass() : String.class, owner);
        }

        public ColumnImpl(String id, Class type, WebDataGrid owner) {
            this(id, null, type, owner);
        }

        protected ColumnImpl(String id, @Nullable MetaPropertyPath propertyPath, Class type, WebDataGrid owner) {
            this.id = id;
            this.propertyPath = propertyPath;
            this.type = type;
            this.owner = owner;

            setupDefaults();
        }

        protected void setupDefaults() {
            resizable = true;
            editable = true;
            generated = false;
            // the generated properties are not normally sortable,
            // but require special handling to enable sorting.
            // for now sorting for generated properties is disabled
            sortable = propertyPath != null && owner.isSortable();
            collapsible = owner.isColumnsCollapsingAllowed();
            collapsed = false;

            visible = true;

            width = -1;
            maxWidth = -1;
            minWidth = 10;
            expandRatio = -1;

            ThemeConstants theme = App.getInstance().getThemeConstants();
            width = theme.getInt("cuba.web.DataGrid.defaultColumnWidth");
            maxWidth = theme.getInt("cuba.web.DataGrid.defaultColumnMaxWidth");
            minWidth = theme.getInt("cuba.web.DataGrid.defaultColumnMinWidth");
            expandRatio = theme.getInt("cuba.web.DataGrid.defaultColumnExpandRatio");
        }

        @Override
        public String getId() {
            if (gridColumn != null) {
                return gridColumn.getId();
            }
            return id;
        }

        // TODO: gg, do we need this?
        public void setId(String id) {
            this.id = id;
            if (gridColumn != null) {
                gridColumn.setId(id);
            }
        }

        @Nullable
        @Override
        public MetaPropertyPath getPropertyPath() {
            return propertyPath;
        }

//        public Object getColumnPropertyId() {
//            return propertyPath != null ? propertyPath : id;
//        }

        @Override
        public String getCaption() {
            if (gridColumn != null) {
                return gridColumn.getCaption();
            }
            return caption;
        }

        @Override
        public void setCaption(String caption) {
            this.caption = caption;
            if (gridColumn != null) {
                gridColumn.setCaption(caption);
            }
        }

        @Override
        public String getCollapsingToggleCaption() {
            if (gridColumn != null) {
                return gridColumn.getHidingToggleCaption();
            }
            return collapsingToggleCaption;
        }

        @Override
        public void setCollapsingToggleCaption(String collapsingToggleCaption) {
            this.collapsingToggleCaption = collapsingToggleCaption;
            if (gridColumn != null) {
                gridColumn.setHidingToggleCaption(collapsingToggleCaption);
            }
        }

        @Override
        public double getWidth() {
            if (gridColumn != null) {
                return gridColumn.getWidth();
            }
            return width;
        }

        @Override
        public void setWidth(double width) {
            this.width = width;
            if (gridColumn != null) {
                gridColumn.setWidth(width);
            }
        }

        @Override
        public boolean isWidthAuto() {
            if (gridColumn != null) {
                return gridColumn.isWidthUndefined();
            }
            return width < 0;
        }

        @Override
        public void setWidthAuto() {
            if (!isWidthAuto()) {
                width = -1;
                if (gridColumn != null) {
                    gridColumn.setWidthUndefined();
                }
            }
        }

        @Override
        public int getExpandRatio() {
            if (gridColumn != null) {
                return gridColumn.getExpandRatio();
            }
            return expandRatio;
        }

        @Override
        public void setExpandRatio(int expandRatio) {
            this.expandRatio = expandRatio;
            if (gridColumn != null) {
                gridColumn.setExpandRatio(expandRatio);
            }
        }

        @Override
        public void clearExpandRatio() {
            setExpandRatio(-1);
        }

        @Override
        public double getMinimumWidth() {
            if (gridColumn != null) {
                return gridColumn.getMinimumWidth();
            }
            return minWidth;
        }

        @Override
        public void setMinimumWidth(double pixels) {
            this.minWidth = pixels;
            if (gridColumn != null) {
                gridColumn.setMinimumWidth(pixels);
            }
        }

        @Override
        public double getMaximumWidth() {
            if (gridColumn != null) {
                return gridColumn.getMaximumWidth();
            }
            return maxWidth;
        }

        @Override
        public void setMaximumWidth(double pixels) {
            this.maxWidth = pixels;
            if (gridColumn != null) {
                gridColumn.setMaximumWidth(pixels);
            }
        }

        @Override
        public boolean isVisible() {
            return visible;
        }

        @Override
        public void setVisible(boolean visible) {
            if (this.visible != visible) {
                this.visible = visible;

                if (visible) {
//                    owner.addContainerProperty(this);

                    // TODO: gg, implement
                    Grid grid = (Grid) owner.getComponent();
                    // addColumn.addColumn?
//                    Grid.Column gridColumn = grid.getColumn(getColumnPropertyId());
//                    if (gridColumn != null) {
//                        owner.setupGridColumnProperties(gridColumn, this);
//                    }
//
//                    grid.setColumnOrder(owner.getColumnPropertyIds());
                } else {
//                    owner.removeContainerProperty(this);
                    setGridColumn(null);
                }
            }
        }

        @Override
        public boolean isCollapsed() {
            if (gridColumn != null) {
                return gridColumn.isHidden();
            }
            return collapsed;
        }

        @Override
        public void setCollapsed(boolean collapsed) {
            this.collapsed = collapsed;
            if (gridColumn != null) {
                gridColumn.setHidden(collapsed);
            }
        }

        @Override
        public boolean isCollapsible() {
            if (gridColumn != null) {
                return gridColumn.isHidable();
            }
            return collapsible;
        }

        @Override
        public void setCollapsible(boolean collapsible) {
            this.collapsible = collapsible;
            updateCollapsible();
        }

        public void updateCollapsible() {
            if (gridColumn != null) {
                gridColumn.setHidable(collapsible && owner.isColumnsCollapsingAllowed());
            }
        }

        @Override
        public boolean isSortable() {
            return sortable;
        }

        @Override
        public void setSortable(boolean sortable) {
            this.sortable = sortable;
            updateSortable();
        }

        public void updateSortable() {
            if (gridColumn != null) {
                gridColumn.setSortable(this.sortable && owner.isSortable());
            }
        }

        @Override
        public boolean isResizable() {
            if (gridColumn != null) {
                return gridColumn.isResizable();
            }
            return resizable;
        }

        @Override
        public void setResizable(boolean resizable) {
            this.resizable = resizable;
            if (gridColumn != null) {
                gridColumn.setResizable(resizable);
            }
        }

        @Override
        public Formatter getFormatter() {
            return formatter;
        }

        @Override
        public void setFormatter(Formatter formatter) {
            this.formatter = formatter;
            if (gridColumn != null) {
                // TODO: gg, implement
                if (formatter != null) {
//                    com.vaadin.v7.data.util.converter.Converter converter = gridColumn.getConverter();
//                    if (converter instanceof FormatterBasedConverter) {
//                        ((FormatterBasedConverter) converter).setFormatter(formatter);
//                    } else {
//                        gridColumn.setConverter(new FormatterBasedConverter(formatter));
//                    }
                } else {
//                    if (converter != null) {
//                        gridColumn.setConverter(createConverterWrapper(converter));
//                    } else {
//                        owner.setDefaultConverter(gridColumn, getMetaProperty(), type);
//                    }
                }
                owner.repaint();
            }
        }

        @Nullable
        protected MetaProperty getMetaProperty() {
            return getPropertyPath() != null
                    ? getPropertyPath().getMetaProperty()
                    : null;
        }

        @Override
        public Element getXmlDescriptor() {
            return element;
        }

        @Override
        public void setXmlDescriptor(Element element) {
            this.element = element;
        }

        @Override
        public Renderer getRenderer() {
            return renderer;
        }

        @Override
        public void setRenderer(Renderer renderer) {
            if (renderer == null && this.renderer != null) {
                this.renderer.resetImplementation();
                this.renderer.setDataGrid(null);
            }

            this.renderer = (AbstractRenderer<?>) renderer;
            if (gridColumn != null) {
                // TODO: gg, implement
                if (this.renderer != null) {
                    this.renderer.setDataGrid(owner);
                    if (this.renderer.getConverter() != null) {
                        //noinspection unchecked
//                        gridColumn.setRenderer(this.renderer.getImplementation(), this.renderer.getConverter());
                    } else {
//                        gridColumn.setRenderer(this.renderer.getImplementation());
                    }
                } else {
                    owner.setDefaultRenderer(gridColumn, getMetaProperty(), type);
                }
                owner.repaint();
            }
        }

        @Override
        public Converter<?, ?> getConverter() {
            return converter;
        }

        @Override
        public void setConverter(Converter<?, ?> converter) {
            this.converter = converter;
            if (gridColumn != null) {
                // TODO: gg, implement
//                gridColumn.setConverter(converter != null ? createConverterWrapper(converter) : null);
                owner.repaint();
            }
        }

        // TODO: gg, implement
        protected com.vaadin.v7.data.util.converter.Converter<?, ?> createConverterWrapper(final Converter converter) {
            return new com.vaadin.v7.data.util.converter.Converter<Object, Object>() {
                @SuppressWarnings("unchecked")
                @Override
                public Object convertToModel(Object value, Class<?> targetType, Locale locale)
                        throws ConversionException {
                    return converter.convertToModel(value, targetType, locale);
                }

                @SuppressWarnings("unchecked")
                @Override
                public Object convertToPresentation(Object value, Class<?> targetType, Locale locale)
                        throws ConversionException {
                    return converter.convertToPresentation(value, targetType, locale);
                }

                @SuppressWarnings("unchecked")
                @Override
                public Class<Object> getModelType() {
                    return converter.getModelType();
                }

                @SuppressWarnings("unchecked")
                @Override
                public Class<Object> getPresentationType() {
                    return converter.getPresentationType();
                }
            };
        }

        // TODO: gg, do we need this?
        @Nullable
        @Override
        public Class getType() {
            return type;
        }

        public boolean isGenerated() {
            return generated;
        }

        public void setGenerated(boolean generated) {
            this.generated = generated;
        }

        @Override
        public boolean isEditable() {
            if (gridColumn != null) {
                return gridColumn.isEditable();
            }
            return isColumnShouldBeEditable();
        }

        protected boolean isColumnShouldBeEditable() {
            return editable
                    && (!generated || fieldGenerator != null)
                    && (!isRepresentsCollection() || fieldGenerator != null)
                    && isEditingPermitted();
        }

        protected boolean isRepresentsCollection() {
            if (propertyPath != null) {
                MetaProperty metaProperty = propertyPath.getMetaProperty();
                Class<?> javaType = metaProperty.getJavaType();
                return Collection.class.isAssignableFrom(javaType);
            }
            return false;
        }

        protected boolean isEditingPermitted() {
            if (propertyPath != null) {
                MetaClass metaClass = propertyPath.getMetaClass();
                return owner.security.isEntityAttrUpdatePermitted(metaClass, propertyPath.toString());
            }
            return true;
        }

        @Override
        public void setEditable(boolean editable) {
            this.editable = editable;
            updateEditable();
        }

        protected void updateEditable() {
            if (gridColumn != null) {
                gridColumn.setEditable(isColumnShouldBeEditable());
            }
        }

        @Override
        public ColumnEditorFieldGenerator getEditorFieldGenerator() {
            return fieldGenerator;
        }

        @Override
        public void setEditorFieldGenerator(ColumnEditorFieldGenerator fieldFactory) {
            this.fieldGenerator = fieldFactory;
            updateEditable();
        }

        public Grid.Column getGridColumn() {
            return gridColumn;
        }

        public void setGridColumn(Grid.Column gridColumn) {
            AppUI current = AppUI.getCurrent();
            if (gridColumn == null && current != null && current.isTestMode()) {
                owner.removeColumnId(this.gridColumn);
            }

            this.gridColumn = gridColumn;
        }

        @Override
        public DataGrid getOwner() {
            return owner;
        }

        @Override
        public void setOwner(DataGrid owner) {
            this.owner = (WebDataGrid) owner;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            //noinspection unchecked
            ColumnImpl column = (ColumnImpl) o;

            return id.equals(column.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public String toString() {
            return id == null ? super.toString() : id;
        }
    }

    protected abstract static class AbstractStaticRowImp<T extends StaticCell> implements StaticRow<T> {

        protected WebDataGrid dataGrid;
//        protected Grid.StaticSection.StaticRow<?> gridRow;

//        public AbstractStaticRowImp(WebDataGrid dataGrid, Grid.StaticSection.StaticRow<?> gridRow) {
//            this.dataGrid = dataGrid;
//            this.gridRow = gridRow;
//        }

        @Override
        public String getStyleName() {
//            return gridRow.getStyleName();
            return null;
        }

        @Override
        public void setStyleName(String styleName) {
//            gridRow.setStyleName(styleName);
        }

        // TODO: gg, implement
        @Override
        public T join(String... columnIds) {
            /*Object[] propertyIds = new Object[columnIds.length];
            for (int i = 0; i < columnIds.length; i++) {
                ColumnImpl column = (ColumnImpl) dataGrid.getColumnNN(columnIds[i]);
                propertyIds[i] = column.getColumnPropertyId();
            }

            T cell = joinInternal(propertyIds);

            // FIXME: gg, workaround for https://github.com/vaadin/framework/issues/8512
            switch (cell.getCellType()) {
                case HTML:
                    cell.setHtml(cell.getHtml());
                    break;
                case TEXT:
                    cell.setText(cell.getText());
                    break;
                case COMPONENT:
                    cell.setComponent(cell.getComponent());
                    break;
            }

            return cell;*/
            return null;
        }

        protected abstract T joinInternal(Object[] propertyIds);

        // TODO: gg, implement
        @Override
        public T getCell(String columnId) {
//            ColumnImpl column = (ColumnImpl) dataGrid.getColumnNN(columnId);
//            return getCellInternal(column.getColumnPropertyId());
            return null;
        }

        protected abstract T getCellInternal(Object propertyId);

//        public Grid.StaticSection.StaticRow<?> getGridRow() {
//            return gridRow;
//        }
    }

    protected static class HeaderRowImpl extends AbstractStaticRowImp<HeaderCell> implements HeaderRow {

//        public HeaderRowImpl(WebDataGrid dataGrid, Grid.HeaderRow gridRow) {
//            super(dataGrid, gridRow);
//        }

//        @Override
//        public Grid.HeaderRow getGridRow() {
//            return (Grid.HeaderRow) super.getGridRow();
//        }

        @Override
        protected HeaderCell getCellInternal(Object propertyId) {
//            Grid.HeaderCell gridCell = getGridRow().getCell(propertyId);
//            return new HeaderCellImpl(this, gridCell);
            return null;
        }

        @Override
        protected HeaderCell joinInternal(Object[] propertyIds) {
//            Grid.HeaderCell gridCell = getGridRow().join(propertyIds);
//            return new HeaderCellImpl(this, gridCell);
            return null;
        }
    }

//    protected static class FooterRowImpl extends AbstractStaticRowImp<FooterCell> implements FooterRow {
//
//        public FooterRowImpl(WebDataGrid dataGrid, Grid.FooterRow gridRow) {
//            super(dataGrid, gridRow);
//        }
//
//        @Override
//        public Grid.FooterRow getGridRow() {
//            return (Grid.FooterRow) super.getGridRow();
//        }
//
//        @Override
//        protected FooterCell getCellInternal(Object propertyId) {
//            Grid.FooterCell gridCell = getGridRow().getCell(propertyId);
//            return new FooterCellImpl(this, gridCell);
//        }
//
//        @Override
//        protected FooterCell joinInternal(Object[] propertyIds) {
//            Grid.FooterCell gridCell = getGridRow().join(propertyIds);
//            return new FooterCellImpl(this, gridCell);
//        }
//    }

    protected abstract static class AbstractStaticCellImpl implements StaticCell {

        protected StaticRow<?> row;
        protected com.haulmont.cuba.gui.components.Component component;

        public AbstractStaticCellImpl(StaticRow<?> row) {
            this.row = row;
        }

        @Override
        public StaticRow<?> getRow() {
            return row;
        }

        @Override
        public com.haulmont.cuba.gui.components.Component getComponent() {
            return component;
        }

        @Override
        public void setComponent(com.haulmont.cuba.gui.components.Component component) {
            this.component = component;
        }
    }

//    protected static class HeaderCellImpl extends AbstractStaticCellImpl implements HeaderCell {

//        protected Grid.HeaderCell gridCell;

//        public HeaderCellImpl(StaticRow<?> row, Grid.HeaderCell gridCell) {
//            super(row);
//            this.gridCell = gridCell;
//        }

//        @Override
//        public String getStyleName() {
//            return gridCell.getStyleName();
//        }
//
//        @Override
//        public void setStyleName(String styleName) {
//            gridCell.setStyleName(styleName);
//        }
//
//        @Override
//        public DataGridStaticCellType getCellType() {
//            return WebWrapperUtils.toDataGridStaticCellType(gridCell.getCellType());
//        }
//
//        @Override
//        public void setComponent(com.haulmont.cuba.gui.components.Component component) {
//            super.setComponent(component);
//            gridCell.setComponent(component.unwrap(Component.class));
//        }
//
//        @Override
//        public String getHtml() {
//            return gridCell.getHtml();
//        }
//
//        @Override
//        public void setHtml(String html) {
//            gridCell.setHtml(html);
//        }
//
//        @Override
//        public String getText() {
//            return gridCell.getText();
//        }
//
//        @Override
//        public void setText(String text) {
//            gridCell.setText(text);
//        }
//    }

//    protected static class FooterCellImpl extends AbstractStaticCellImpl implements FooterCell {

//        protected Grid.FooterCell gridCell;

//        public FooterCellImpl(StaticRow<?> row, Grid.FooterCell gridCell) {
//            super(row);
//            this.gridCell = gridCell;
//        }

//        @Override
//        public String getStyleName() {
//            return gridCell.getStyleName();
//        }
//
//        @Override
//        public void setStyleName(String styleName) {
//            gridCell.setStyleName(styleName);
//        }
//
//        @Override
//        public DataGridStaticCellType getCellType() {
//            return WebWrapperUtils.toDataGridStaticCellType(gridCell.getCellType());
//        }
//
//        @Override
//        public void setComponent(com.haulmont.cuba.gui.components.Component component) {
//            super.setComponent(component);
//            gridCell.setComponent(component.unwrap(Component.class));
//        }
//
//        @Override
//        public String getHtml() {
//            return gridCell.getHtml();
//        }
//
//        @Override
//        public void setHtml(String html) {
//            gridCell.setHtml(html);
//        }
//
//        @Override
//        public String getText() {
//            return gridCell.getText();
//        }
//
//        @Override
//        public void setText(String text) {
//            gridCell.setText(text);
//        }
//    }

    public static class ActionMenuItemWrapper {
        protected MenuItem menuItem;
        protected Action action;

        protected boolean showIconsForPopupMenuActions;

        protected PropertyChangeListener actionPropertyChangeListener;

        public ActionMenuItemWrapper(MenuItem menuItem, boolean showIconsForPopupMenuActions) {
            this.menuItem = menuItem;
            this.showIconsForPopupMenuActions = showIconsForPopupMenuActions;

            this.menuItem.setCommand((Menu.Command) selectedItem -> {
                if (action != null) {
                    performAction(action);
                }
            });
        }

        public void performAction(Action action) {
            action.actionPerform(null);
        }

        public MenuItem getMenuItem() {
            return menuItem;
        }

        public Action getAction() {
            return action;
        }

        public void setAction(Action action) {
            if (this.action != action) {
                if (this.action != null) {
                    this.action.removePropertyChangeListener(actionPropertyChangeListener);
                }

                this.action = action;

                if (action != null) {
                    String caption = action.getCaption();
                    if (!StringUtils.isEmpty(caption)) {
                        setCaption(caption);
                    }

                    menuItem.setEnabled(action.isEnabled());
                    menuItem.setVisible(action.isVisible());

                    if (action.getIcon() != null) {
                        setIcon(action.getIcon());
                    }

                    actionPropertyChangeListener = evt -> {
                        if (Action.PROP_ICON.equals(evt.getPropertyName())) {
                            setIcon(ActionMenuItemWrapper.this.action.getIcon());
                        } else if (Action.PROP_CAPTION.equals(evt.getPropertyName())) {
                            setCaption(ActionMenuItemWrapper.this.action.getCaption());
                        } else if (Action.PROP_ENABLED.equals(evt.getPropertyName())) {
                            setEnabled(ActionMenuItemWrapper.this.action.isEnabled());
                        } else if (Action.PROP_VISIBLE.equals(evt.getPropertyName())) {
                            setVisible(ActionMenuItemWrapper.this.action.isVisible());
                        }
                    };
                    action.addPropertyChangeListener(actionPropertyChangeListener);
                }
            }
        }

        public void setEnabled(boolean enabled) {
            menuItem.setEnabled(enabled);
        }

        public void setVisible(boolean visible) {
            menuItem.setVisible(visible);
        }

        public void setIcon(String icon) {
            if (showIconsForPopupMenuActions) {
                if (!StringUtils.isEmpty(icon)) {
                    menuItem.setIcon(AppBeans.get(IconResolver.class).getIconResource(icon));
                } else {
                    menuItem.setIcon(null);
                }
            }
        }

        public void setCaption(String caption) {
            if (action.getShortcutCombination() != null) {
                StringBuilder sb = new StringBuilder();
                sb.append(caption);
                if (action.getShortcutCombination() != null) {
                    sb.append(" (").append(action.getShortcutCombination().format()).append(")");
                }
                caption = sb.toString();
            }

            menuItem.setText(caption);
        }
    }

    protected static class GridComposition extends CssLayout {
        protected Grid grid;

        public Grid getGrid() {
            return grid;
        }

        public void setGrid(Grid grid) {
            this.grid = grid;
        }

        @Override
        public void setHeight(float height, Unit unit) {
            super.setHeight(height, unit);

            if (getHeight() < 0) {
                grid.setHeightUndefined();
                grid.setHeightMode(HeightMode.UNDEFINED);
            } else {
                grid.setHeight(100, Unit.PERCENTAGE);
                grid.setHeightMode(HeightMode.CSS);
            }
        }

        @Override
        public void setWidth(float width, Unit unit) {
            super.setWidth(width, unit);

            if (getWidth() < 0) {
                grid.setWidthUndefined();
            } else {
                grid.setWidth(100, Unit.PERCENTAGE);
            }
        }
    }

    public class CollectionDsListenersWrapper implements
            Datasource.ItemChangeListener,
            Datasource.ItemPropertyChangeListener,
            Datasource.StateChangeListener,
            CollectionDatasource.CollectionChangeListener {

        protected WeakItemChangeListener weakItemChangeListener;
        protected WeakItemPropertyChangeListener weakItemPropertyChangeListener;
        protected WeakStateChangeListener weakStateChangeListener;
        protected WeakCollectionChangeListener weakCollectionChangeListener;

        private EventRouter eventRouter;

        /**
         * Use EventRouter for listeners instead of fields with listeners List.
         *
         * @return lazily initialized {@link EventRouter} instance.
         * @see EventRouter
         */
        protected EventRouter getEventRouter() {
            if (eventRouter == null) {
                eventRouter = new EventRouter();
            }
            return eventRouter;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void collectionChanged(CollectionDatasource.CollectionChangeEvent e) {
            // #PL-2035, reload selection from ds
//            Collection<Object> selectedItemIds = component.getSelectedRows();
//            if (selectedItemIds == null) {
//                selectedItemIds = Collections.emptySet();
//            }

            //noinspection unchecked
//            Set<Object> newSelection = selectedItemIds.stream()
//                    .filter(entityId -> e.getDs().containsItem(entityId))
//                    .collect(Collectors.toSet());
//
//            if (e.getDs().getState() == Datasource.State.VALID && e.getDs().getItem() != null) {
//                newSelection.add(e.getDs().getItem().getId());
//            }
//
//            if (newSelection.isEmpty()) {
//                setSelected((E) null);
//            } else {
//                setSelectedIds(newSelection);
//            }

//            for (Action action : getActions()) {
//                action.refreshState();
//            }

            if (getEventRouter().hasListeners(CollectionDatasource.CollectionChangeListener.class)) {
                getEventRouter().fireEvent(CollectionDatasource.CollectionChangeListener.class,
                        CollectionDatasource.CollectionChangeListener::collectionChanged, e);
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public void itemChanged(Datasource.ItemChangeEvent e) {
            for (Action action : getActions()) {
                action.refreshState();
            }

            if (getEventRouter().hasListeners(Datasource.ItemChangeListener.class)) {
                getEventRouter().fireEvent(Datasource.ItemChangeListener.class,
                        Datasource.ItemChangeListener::itemChanged, e);
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public void itemPropertyChanged(Datasource.ItemPropertyChangeEvent e) {
            for (Action action : getActions()) {
                action.refreshState();
            }

            if (getEventRouter().hasListeners(Datasource.ItemPropertyChangeListener.class)) {
                getEventRouter().fireEvent(Datasource.ItemPropertyChangeListener.class,
                        Datasource.ItemPropertyChangeListener::itemPropertyChanged, e);
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public void stateChanged(Datasource.StateChangeEvent e) {
            for (Action action : getActions()) {
                action.refreshState();
            }

            if (getEventRouter().hasListeners(Datasource.StateChangeListener.class)) {
                getEventRouter().fireEvent(Datasource.StateChangeListener.class,
                        Datasource.StateChangeListener::stateChanged, e);
            }
        }

        public void addCollectionChangeListener(CollectionDatasource.CollectionChangeListener listener) {
            getEventRouter().addListener(CollectionDatasource.CollectionChangeListener.class, listener);
        }

        public void removeCollectionChangeListener(CollectionDatasource.CollectionChangeListener listener) {
            getEventRouter().removeListener(CollectionDatasource.CollectionChangeListener.class, listener);
        }

        public void addItemChangeListener(Datasource.ItemChangeListener listener) {
            getEventRouter().addListener(Datasource.ItemChangeListener.class, listener);
        }

        public void removeItemChangeListener(Datasource.ItemChangeListener listener) {
            getEventRouter().removeListener(Datasource.ItemChangeListener.class, listener);
        }

        public void addItemPropertyChangeListener(Datasource.ItemPropertyChangeListener listener) {
            getEventRouter().addListener(Datasource.ItemPropertyChangeListener.class, listener);
        }

        public void removeItemPropertyChangeListener(Datasource.ItemPropertyChangeListener listener) {
            getEventRouter().removeListener(Datasource.ItemPropertyChangeListener.class, listener);
        }

        public void addStateChangeListener(Datasource.StateChangeListener listener) {
            getEventRouter().addListener(Datasource.StateChangeListener.class, listener);
        }

        public void removeStateChangeListener(Datasource.StateChangeListener listener) {
            getEventRouter().removeListener(Datasource.StateChangeListener.class, listener);
        }

        @SuppressWarnings("unchecked")
        public void bind(CollectionDatasource ds) {
            weakItemChangeListener = new WeakItemChangeListener(ds, this);
            ds.addItemChangeListener(weakItemChangeListener);

            weakItemPropertyChangeListener = new WeakItemPropertyChangeListener(ds, this);
            ds.addItemPropertyChangeListener(weakItemPropertyChangeListener);

            weakStateChangeListener = new WeakStateChangeListener(ds, this);
            ds.addStateChangeListener(weakStateChangeListener);

            weakCollectionChangeListener = new WeakCollectionChangeListener(ds, this);
            ds.addCollectionChangeListener(weakCollectionChangeListener);
        }

        @SuppressWarnings("unchecked")
        public void unbind(CollectionDatasource ds) {
            ds.removeItemChangeListener(weakItemChangeListener);
            weakItemChangeListener = null;

            ds.removeItemPropertyChangeListener(weakItemPropertyChangeListener);
            weakItemPropertyChangeListener = null;

            ds.removeStateChangeListener(weakStateChangeListener);
            weakStateChangeListener = null;

            ds.removeCollectionChangeListener(weakCollectionChangeListener);
            weakCollectionChangeListener = null;
        }
    }
}