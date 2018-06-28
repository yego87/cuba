package com.haulmont.cuba.gui.components.data;

import com.haulmont.bali.events.Subscription;

import javax.annotation.Nullable;
import java.util.EventObject;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * todo JavaDoc
 *
 * @param <T>
 */
public interface DataGridSource<T> {

    BindingState getState();

    Object getItemId(T item);

    T getItem(@Nullable Object itemId);

    int indexOfItem(T item);

    @Nullable
    T getItemByIndex(int index);

    Stream<T> getItems();

    List<T> getItems(int startIndex, int numberOfItems);

    boolean containsItem(T item);

    int size();

    @Nullable
    T getSelectedItem();
    void setSelectedItem(@Nullable T item);

    Subscription addStateChangeListener(Consumer<StateChangeEvent<T>> listener);
    Subscription addValueChangeListener(Consumer<ValueChangeEvent<T>> listener);
    Subscription addItemSetChangeListener(Consumer<ItemSetChangeEvent<T>> listener);
    Subscription addSelectedItemChangeListener(Consumer<SelectedItemChangeEvent<T>> listener);

    // todo
    class StateChangeEvent<T> extends EventObject {
        protected BindingState state;

        public StateChangeEvent(DataGridSource<T> source, BindingState state) {
            super(source);
            this.state = state;
        }

        @SuppressWarnings("unchecked")
        @Override
        public DataGridSource<T> getSource() {
            return (DataGridSource<T>) super.getSource();
        }

        public BindingState getState() {
            return state;
        }
    }

    // todo
    class ValueChangeEvent<T> extends EventObject {
        private final T prevValue;
        private final T value;

        public ValueChangeEvent(DataGridSource<T> source, T prevValue, T value) {
            super(source);
            this.prevValue = prevValue;
            this.value = value;
        }

        @SuppressWarnings("unchecked")
        @Override
        public DataGridSource<T> getSource() {
            return (DataGridSource<T>) super.getSource();
        }

        public T getPrevValue() {
            return prevValue;
        }

        public T getValue() {
            return value;
        }
    }

    // todo
    class ItemSetChangeEvent<T> extends EventObject {
        public ItemSetChangeEvent(DataGridSource<T> source) {
            super(source);
        }

        @SuppressWarnings("unchecked")
        @Override
        public DataGridSource<T> getSource() {
            return (DataGridSource<T>) super.getSource();
        }
    }

    // todo
    class SelectedItemChangeEvent<T> extends EventObject {
        protected final T selectedItem;

        public SelectedItemChangeEvent(DataGridSource<T> source, T selectedItem) {
            super(source);
            this.selectedItem = selectedItem;
        }

        @SuppressWarnings("unchecked")
        @Override
        public DataGridSource<T> getSource() {
            return (DataGridSource<T>) super.getSource();
        }

        public T getSelectedItem() {
            return selectedItem;
        }
    }
}
