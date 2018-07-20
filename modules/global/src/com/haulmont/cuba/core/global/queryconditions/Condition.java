package com.haulmont.cuba.core.global.queryconditions;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

public interface Condition extends Serializable, Cloneable {

    Collection<String> getParameters();

    @Nullable
    Condition actualize(Set<String> actualParameters);
}
