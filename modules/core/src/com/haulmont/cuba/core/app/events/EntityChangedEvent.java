package com.haulmont.cuba.core.app.events;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.ExtendedEntities;
import com.haulmont.cuba.core.global.Metadata;
import org.springframework.context.ApplicationEvent;
import org.springframework.core.ResolvableType;
import org.springframework.core.ResolvableTypeProvider;

public class EntityChangedEvent<E extends Entity<K>, K> extends ApplicationEvent implements ResolvableTypeProvider {

    public enum Type {
        CREATED,
        UPDATED,
        DELETED
    }

    private Class<E> entityClass;
    private K entityId;
    private Type type;
    private AttributeChanges changes;

    public EntityChangedEvent(Object source, Class<E> entityClass, K entityId, Type type, AttributeChanges changes) {
        super(source);
        this.entityClass = entityClass;
        this.entityId = entityId;
        this.type = type;
        this.changes = changes;
    }

    public Class<E> getEntityClass() {
        return entityClass;
    }

    public K getEntityId() {
        return entityId;
    }

    public Type getType() {
        return type;
    }

    public AttributeChanges getChanges() {
        return changes;
    }

    @Override
    public ResolvableType getResolvableType() {
        Metadata metadata = AppBeans.get(Metadata.NAME);
        ExtendedEntities extendedEntities = metadata.getExtendedEntities();
        MetaClass metaClass = extendedEntities.getOriginalOrThisMetaClass(metadata.getClassNN(entityClass));
        MetaProperty pkProperty = metadata.getTools().getPrimaryKeyProperty(metaClass);
        if (pkProperty == null) {
            throw new IllegalStateException("Unable to send EntityChangedEvent for " + metaClass + " because it has no primary key");
        }
        return ResolvableType.forClassWithGenerics(getClass(),
                ResolvableType.forClass(metaClass.getJavaClass()),
                ResolvableType.forClass(pkProperty.getJavaType()));
    }
}
