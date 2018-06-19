package com.haulmont.cuba.gui.components.data;

import java.util.stream.Stream;

/**
 * todo JavaDoc
 *
 * @param <I>
 */
public interface DataGridSource<I> {

    BindingState getState();

    Object getItemId(I item);

    Stream<I> getItems();

    int size();

    interface Ordered<T> extends DataGridSource<T> {
        Object nextItemId(Object itemId);

        Object prevItemId(Object itemId);

        Object firstItemId();

        Object lastItemId();

        boolean isFirstId(Object itemId);

        boolean isLastId(Object itemId);
    }

    interface Sortable<T> extends Ordered<T> {
        void sort(Object[] propertyId, boolean[] ascending);

        void resetSortOrder();
    }
}
