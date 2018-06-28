package com.haulmont.cuba.web.gui.components.datagrid;

import com.haulmont.cuba.gui.components.data.DataGridSource;
import com.haulmont.cuba.web.widgets.data.SortableDataProvider;
import com.vaadin.data.provider.QuerySortOrder;
import com.vaadin.shared.data.sort.SortDirection;

import java.util.ArrayList;
import java.util.List;

public class SortableDataGridDataProvider<T>
        extends DataGridDataProvider<T>
        implements SortableDataProvider<T> {

    public SortableDataGridDataProvider(DataGridSource.Sortable<T> dataGridSource,
                                        DataGridSourceEventsDelegate<T> dataEventsDelegate) {
        super(dataGridSource, dataEventsDelegate);
    }

    public DataGridSource.Sortable<T> getSortableDataGridSource() {
        return (DataGridSource.Sortable<T>) dataGridSource;
    }

    @Override
    public void sort(List<QuerySortOrder> sortOrders) {
        /*List<String> sortColumns = new ArrayList<>();
        List<Boolean> ascendingFlags = new ArrayList<>();
        for (QuerySortOrder sortOrder : sortOrders) {
            sortColumns.add(sortOrder.getSorted());
            ascendingFlags.add(SortDirection.ASCENDING.equals(sortOrder.getDirection()));
        }*/

//        getSortableDataGridSource().sort(sortColumns, ascendingFlags);
    }
}
