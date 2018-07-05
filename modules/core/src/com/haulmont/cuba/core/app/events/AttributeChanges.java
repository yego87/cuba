package com.haulmont.cuba.core.app.events;

import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.contracts.Id;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AttributeChanges {

    private Set<Change> changes;
    private Map<String, AttributeChanges> embeddedChanges;

    public AttributeChanges(Set<Change> changes, Map<String, AttributeChanges> embeddedChanges) {
        this.changes = changes;
        this.embeddedChanges = embeddedChanges;
    }

    /**
     * @return changed attributes names for current entity
     */
    public Set<String> getOwnAttributes() {
        return changes.stream().map(change -> change.name).collect(Collectors.toSet());
    }

    /**
     * @return changed attributes names for current entity and all embedded entities
     */
    public Set<String> getAttributes() {
        Set<String> attributes = new HashSet<>();
        for (Change change : changes) {
            AttributeChanges nestedChanges = embeddedChanges.get(change.name);
            if (nestedChanges == null) {
                attributes.add(change.name);
            } else {
                for (String attribute : nestedChanges.getAttributes()) {
                    attributes.add(String.format("%s.%s", change.name, attribute));
                }
            }
        }
        return attributes;
    }

    public boolean isChanged(String attributeName) {
        for (Change change : changes) {
            if (change.name.equals(attributeName))
                return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getOldValue(String attributeName) {
        String[] properties = attributeName.split("\\.");
        if (properties.length == 1) {
            for (Change change : changes) {
                if (change.name.equals(attributeName))
                    return (T) change.oldValue;
            }
        } else {
            AttributeChanges nestedChanges = embeddedChanges.get(properties[0]);
            if (nestedChanges != null) {
                return nestedChanges.getOldValue(attributeName.substring(attributeName.indexOf(".") + 1));
            }
        }
        return null;
    }

    @Nullable
    public <E extends Entity<K>, K> Id<E, K> getOldReferenceId(String attributeName) {
        return getOldValue(attributeName);
    }

    public static class Change {

        public final String name;
        public final Object oldValue;

        public Change(String name, Object oldValue) {
            this.name = name;
            this.oldValue = oldValue;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Change that = (Change) o;

            return name.equals(that.name);

        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }
}
