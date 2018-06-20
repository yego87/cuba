package com.haulmont.cuba.gui.components.data;

import com.haulmont.bali.events.Subscription;

import javax.annotation.Nullable;
import java.util.EventObject;
import java.util.function.Consumer;
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

    boolean containsItem(I item);

    int size();

    @Nullable
    I getSelectedItem();
    void setSelectedItem(@Nullable I item);

    Subscription addStateChangeListener(Consumer<StateChangeEvent<I>> listener);
    Subscription addValueChangeListener(Consumer<ValueChangeEvent<I>> listener);
    Subscription addItemSetChangeListener(Consumer<ItemSetChangeEvent<I>> listener);
    Subscription addSelectedItemChangeListener(Consumer<SelectedItemChangeEvent<I>> listener);

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
