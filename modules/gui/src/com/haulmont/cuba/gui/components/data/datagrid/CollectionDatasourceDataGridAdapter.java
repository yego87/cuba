package com.haulmont.cuba.gui.components.data.datagrid;

import com.haulmont.bali.events.EventPublisher;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.MetadataTools;
import com.haulmont.cuba.gui.components.data.BindingState;
import com.haulmont.cuba.gui.components.data.EntityDataGridSource;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.data.impl.CollectionDsHelper;

import java.util.Collection;
import java.util.stream.Stream;

public class CollectionDatasourceDataGridAdapter<E extends Entity<K>, K> implements EntityDataGridSource<E> {

    protected CollectionDatasource<E, K> datasource;
    protected EventPublisher events = new EventPublisher();

    protected BindingState state = BindingState.INACTIVE;

    public CollectionDatasourceDataGridAdapter(CollectionDatasource<E, K> datasource) {
        this.datasource = datasource;

        CollectionDsHelper.autoRefreshInvalid(datasource, true);

        if (datasource.getState() == Datasource.State.VALID) {
            setState(BindingState.ACTIVE);
        }
    }

    public CollectionDatasource<E, K> getDatasource() {
        return datasource;
    }

    @Override
    public MetaClass getEntityMetaClass() {
        return datasource.getMetaClass();
    }

    @Override
    public Collection<MetaPropertyPath> getAutowiredProperties() {
        MetadataTools metadataTools = AppBeans.get(MetadataTools.class);

        return datasource.getView() != null
                // if a view is specified - use view properties
                ? metadataTools.getViewPropertyPaths(datasource.getView(), datasource.getMetaClass())
                // otherwise use all properties from meta-class
                : metadataTools.getPropertyPaths(datasource.getMetaClass());
    }

    @Override
    public BindingState getState() {
        return state;
    }

    public void setState(BindingState state) {
        if (this.state != state) {
            this.state = state;

            // TODO: gg,
//            events.publish(TableSource.StateChangeEvent.class, new TableSource.StateChangeEvent<>(this, state));
        }
    }

    @Override
    public Object getItemId(E item) {
        return item.getId();
    }

    @Override
    public Stream<E> getItems() {
        return datasource.getItems().stream();
    }

    @Override
    public int size() {
        return datasource.size();
    }
}
