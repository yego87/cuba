package com.haulmont.cuba.core.entity.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@MetaAnnotation
public @interface PublishEntityChangedEvents {

    boolean created() default true;
    boolean updated() default true;
    boolean deleted() default true;

    String view() default "";
}
