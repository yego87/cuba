package com.haulmont.cuba.web.gui.components.datagrid;

import com.haulmont.cuba.gui.components.data.DataGridSource;

// TODO: gg, implement sortable
public class SortableDataGridDataProvider<I> extends DataGridDataProvider<I> {

    public SortableDataGridDataProvider(DataGridSource<I> dataGridSource,
                                        DataGridSourceEventsDelegate<I> dataEventsDelegate) {
        super(dataGridSource, dataEventsDelegate);
    }
}
