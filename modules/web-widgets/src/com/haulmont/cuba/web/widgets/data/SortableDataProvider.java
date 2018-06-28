package com.haulmont.cuba.web.widgets.data;

import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.QuerySortOrder;
import com.vaadin.server.SerializablePredicate;

import java.util.List;

public interface SortableDataProvider<T> extends DataProvider<T, SerializablePredicate<T>> {

    void sort(List<QuerySortOrder> sortOrders);
}
