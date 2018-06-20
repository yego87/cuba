package com.haulmont.cuba.gui.components.data.datagrid;

import com.google.common.base.Preconditions;
import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.gui.components.data.DataGridSource;
import com.haulmont.cuba.gui.data.CollectionDatasource;

public class SortableCollectionDatasourceDataGridAdapter<E extends Entity<K>, K>
        extends CollectionDatasourceDataGridAdapter<E, K> {

    public SortableCollectionDatasourceDataGridAdapter(CollectionDatasource<E, K> datasource) {
        super(datasource);
    }

    @SuppressWarnings("unchecked")
    protected CollectionDatasource.Sortable<E, K> getSortableDatasource() {
        return (CollectionDatasource.Sortable<E, K>) datasource;
    }

    /*@Override
    public Object nextItemId(Object itemId) {
        return getSortableDatasource().nextItemId((K) itemId);
    }

    @Override
    public Object prevItemId(Object itemId) {
        return getSortableDatasource().prevItemId((K) itemId);
    }

    @Override
    public Object firstItemId() {
        return getSortableDatasource().firstItemId();
    }

    @Override
    public Object lastItemId() {
        return getSortableDatasource().lastItemId();
    }

    @Override
    public boolean isFirstId(Object itemId) {
        return getSortableDatasource().isFirstId((K) itemId);
    }

    @Override
    public boolean isLastId(Object itemId) {
        return getSortableDatasource().isLastId((K) itemId);
    }

    @Override
    public void sort(Object[] propertyIds, boolean[] ascendingFlags) {
        // table support sort only by one property
        Preconditions.checkArgument(propertyIds.length == 1);

        MetaPropertyPath propertyPath = (MetaPropertyPath) propertyIds[0];
        boolean ascending = ascendingFlags[0];

        CollectionDatasource.Sortable.SortInfo<MetaPropertyPath> info = new CollectionDatasource.Sortable.SortInfo<>();
        info.setPropertyPath(propertyPath);
        info.setOrder(ascending ? CollectionDatasource.Sortable.Order.ASC : CollectionDatasource.Sortable.Order.DESC);

        getSortableDatasource().sort(new CollectionDatasource.Sortable.SortInfo[] {info});
    }

    @Override
    public void resetSortOrder() {
        getSortableDatasource().resetSortOrder();
    }*/
}
