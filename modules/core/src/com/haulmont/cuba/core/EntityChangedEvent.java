package com.haulmont.cuba.core;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.ExtendedEntities;
import com.haulmont.cuba.core.sys.persistence.EntityAttributeChanges;
import org.springframework.context.ApplicationEvent;
import org.springframework.core.ResolvableType;
import org.springframework.core.ResolvableTypeProvider;

public class EntityChangedEvent<E extends Entity> extends ApplicationEvent implements ResolvableTypeProvider {

    public enum Type {
        CREATED,
        UPDATED,
        DELETED
    }

    private E entity;
    private Type type;
    private EntityAttributeChanges changes;

    public EntityChangedEvent(Object source, E entity, Type type, EntityAttributeChanges changes) {
        super(source);
        this.entity = entity;
        this.type = type;
        this.changes = changes;
    }

    public E getEntity() {
        return entity;
    }

    public Type getType() {
        return type;
    }

    public EntityAttributeChanges getChanges() {
        return changes;
    }

    @Override
    public ResolvableType getResolvableType() {
        ExtendedEntities extendedEntities = AppBeans.get(ExtendedEntities.NAME);
        MetaClass metaClass = extendedEntities.getOriginalOrThisMetaClass(getEntity().getMetaClass());
        return ResolvableType.forClassWithGenerics(getClass(), ResolvableType.forClass(metaClass.getJavaClass()));
    }
}
