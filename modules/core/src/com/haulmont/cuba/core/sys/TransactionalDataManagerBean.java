package com.haulmont.cuba.core.sys;

import com.haulmont.cuba.core.TransactionalDataManager;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.core.global.*;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Component(TransactionalDataManager.NAME)
public class TransactionalDataManagerBean implements TransactionalDataManager {

    @Inject
    private DataManager dataManager;

    @Override
    public <E extends Entity<K>, K> FluentLoader<E, K> load(Class<E> entityClass) {
        return new FluentLoader<>(entityClass, dataManager, true);
    }

    @Override
    public <E extends Entity<K>, K> FluentLoader.ById<E, K> load(Id<E, K> entityId) {
        return new FluentLoader<>(entityId.getEntityClass(), dataManager).id(entityId.getValue());
    }

    @Override
    public FluentValuesLoader loadValues(String queryString) {
        return new FluentValuesLoader(queryString, dataManager, true);
    }

    @Override
    public <T> FluentValueLoader<T> loadValue(String queryString, Class<T> valueClass) {
        return new FluentValueLoader<>(queryString, valueClass, dataManager, true);
    }

    @Override
    public EntitySet save(Entity... entities) {
        CommitContext cc = new CommitContext(entities);
        cc.setJoinTransaction(true);
        return dataManager.commit(cc);
    }

    @Override
    public <E extends Entity> E save(E entity) {
        CommitContext cc = new CommitContext(entity);
        cc.setJoinTransaction(true);
        return dataManager.commit(cc).get(entity);
    }

    @Override
    public <E extends Entity> E save(E entity, @Nullable View view) {
        CommitContext cc = new CommitContext();
        cc.addInstanceToCommit(entity, view);
        cc.setJoinTransaction(true);
        return dataManager.commit(cc).get(entity);
    }

    @Override
    public <E extends Entity> E save(E entity, @Nullable String viewName) {
        CommitContext cc = new CommitContext();
        cc.addInstanceToCommit(entity, viewName);
        cc.setJoinTransaction(true);
        return dataManager.commit(cc).get(entity);
    }

    @Override
    public void remove(Entity entity) {
        CommitContext cc = new CommitContext();
        cc.addInstanceToRemove(entity);
        cc.setJoinTransaction(true);
        dataManager.commit(cc);
    }
}
