package com.haulmont.cuba.web.gui.components.datagrid;

import com.haulmont.bali.events.Subscription;
import com.haulmont.cuba.gui.components.data.BindingState;
import com.haulmont.cuba.gui.components.data.DataGridSource;
import com.vaadin.data.provider.AbstractDataProvider;
import com.vaadin.data.provider.DataChangeEvent;
import com.vaadin.data.provider.Query;
import com.vaadin.server.SerializablePredicate;

import java.util.stream.Stream;

public class DataGridDataProvider<I> extends AbstractDataProvider<I, SerializablePredicate<I>> {

    protected DataGridSource<I> dataGridSource;
    protected DataGridSourceEventsDelegate<I> dataEventsDelegate;

    protected Subscription itemSetChangeSubscription;
    protected Subscription valueChangeSubscription;
    protected Subscription stateChangeSubscription;
    protected Subscription selectedItemChangeSubscription;

    public DataGridDataProvider(DataGridSource<I> dataGridSource,
                                DataGridSourceEventsDelegate<I> dataEventsDelegate) {
        this.dataGridSource = dataGridSource;
        this.dataEventsDelegate = dataEventsDelegate;

        this.itemSetChangeSubscription =
                this.dataGridSource.addItemSetChangeListener(this::datasourceItemSetChanged);
        this.valueChangeSubscription =
                this.dataGridSource.addValueChangeListener(this::datasourceValueChanged);
        this.stateChangeSubscription =
                this.dataGridSource.addStateChangeListener(this::datasourceStateChanged);
        this.selectedItemChangeSubscription =
                this.dataGridSource.addSelectedItemChangeListener(this::datasourceSelectedItemChanged);
    }

    public void unbind() {
        if (itemSetChangeSubscription != null) {
            this.itemSetChangeSubscription.remove();
            this.itemSetChangeSubscription = null;
        }

        if (valueChangeSubscription != null) {
            this.valueChangeSubscription.remove();
            this.valueChangeSubscription = null;
        }

        if (stateChangeSubscription != null) {
            this.stateChangeSubscription.remove();
            this.stateChangeSubscription = null;
        }

        if (selectedItemChangeSubscription != null) {
            this.selectedItemChangeSubscription.remove();
            this.selectedItemChangeSubscription = null;
        }
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

    protected void datasourceItemSetChanged(DataGridSource.ItemSetChangeEvent<I> event) {
        fireEvent(new DataChangeEvent<>(this));

        dataEventsDelegate.dataGridSourceItemSetChanged(event);
    }

    protected void datasourceValueChanged(DataGridSource.ValueChangeEvent<I> event) {
        // TODO: gg, probably need to fire DataChangeEvent/DataRefreshEvent
        dataEventsDelegate.dataGridSourcePropertyValueChanged(event);
    }

    protected void datasourceStateChanged(DataGridSource.StateChangeEvent<I> event) {
        dataEventsDelegate.dataGridSourceStateChanged(event);
    }

    protected void datasourceSelectedItemChanged(DataGridSource.SelectedItemChangeEvent<I> event) {
        dataEventsDelegate.dataGridSourceSelectedItemChanged(event);
    }
}
