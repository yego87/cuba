package com.haulmont.cuba.core;

import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.core.global.*;

import javax.annotation.Nullable;

/**
 * Similar to {@link DataManager} but joins an existing transaction.
 * <p>
 * Use this bean in the middleware logic to perform some operations in one transaction, for example:
 * <pre>
 *     {@literal @}Inject
 *      private TransactionalDataManager dataManager;
 *
 *     {@literal @}Transactional
 *      private void saveEntities(Foo foo, Bar bar) {
 *          dataManager.save(foo);
 *          dataManager.save(bar);
 *      }
 * </pre>
 */
public interface TransactionalDataManager {

    String NAME = "cuba_TransactionalDataManager";

    <E extends Entity<K>, K> FluentLoader<E, K> load(Class<E> entityClass);

    <E extends Entity<K>, K> FluentLoader.ById<E, K> load(Id<E, K> entityId);

    FluentValuesLoader loadValues(String queryString);

    <T> FluentValueLoader<T> loadValue(String queryString, Class<T> valueClass);

    EntitySet save(Entity... entities);

    <E extends Entity> E save(E entity);

    <E extends Entity> E save(E entity, @Nullable View view);

    <E extends Entity> E save(E entity, @Nullable String viewName);

    void remove(Entity entity);
}
