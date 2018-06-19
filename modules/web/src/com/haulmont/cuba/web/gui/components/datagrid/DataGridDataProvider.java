package com.haulmont.cuba.web.gui.components.datagrid;

import com.haulmont.cuba.gui.components.data.BindingState;
import com.haulmont.cuba.gui.components.data.DataGridSource;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.DataProviderListener;
import com.vaadin.data.provider.Query;
import com.vaadin.server.SerializablePredicate;
import com.vaadin.shared.Registration;

import java.util.stream.Stream;

public class DataGridDataProvider<I> implements DataProvider<I, SerializablePredicate<I>> {

    protected DataGridSource<I> dataGridSource;

    public DataGridDataProvider(DataGridSource<I> dataGridSource) {
        this.dataGridSource = dataGridSource;
    }

    public void unbind() {
//        if (itemSetChangeSubscription != null) {
//            this.itemSetChangeSubscription.remove();
//            this.itemSetChangeSubscription = null;
//        }
//        if (valueChangeSubscription != null) {
//            this.valueChangeSubscription.remove();
//            this.valueChangeSubscription = null;
//        }
//        if (stateChangeSubscription != null) {
//            this.stateChangeSubscription.remove();
//            this.stateChangeSubscription = null;
//        }
//        if (selectedItemChangeSubscription != null) {
//            this.selectedItemChangeSubscription.remove();
//            this.selectedItemChangeSubscription = null;
//        }
//        wrappersPool.clear();
//        itemsCache.clear();
    }

    public DataGridSource<I> getDataGridSource() {
        return dataGridSource;
    }

    @Override
    public Object getId(I item) {
        return dataGridSource.getItemId(item);
    }

    @Override
    public boolean isInMemory() {
        return true;
    }

    @Override
    public int size(Query<I, SerializablePredicate<I>> query) {
        // TODO: gg, query?
        if (dataGridSource.getState() == BindingState.INACTIVE) {
            return 0;
        }

        return dataGridSource.size();
    }

    @Override
    public Stream<I> fetch(Query<I, SerializablePredicate<I>> query) {
        // TODO: gg, query?
        if (dataGridSource.getState() == BindingState.INACTIVE) {
            return Stream.empty();
        }

        return dataGridSource.getItems();
    }

    @Override
    public void refreshItem(I item) {
        // Use a datasource instead
        throw new UnsupportedOperationException();
    }

    @Override
    public void refreshAll() {
        // Use a datasource instead
        throw new UnsupportedOperationException();
    }

    @Override
    public Registration addDataProviderListener(DataProviderListener<I> listener) {
        return null;
    }
}
