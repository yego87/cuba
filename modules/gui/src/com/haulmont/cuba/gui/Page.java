/*
 * Copyright (c) 2008-2018 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.haulmont.cuba.gui;

import com.haulmont.cuba.gui.screen.Screen;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * Registers an annotated class as screen that corresponds the given relative path.
 * Annotated class must by a direct or indirect subclass of {@link Screen}.
 * <p>
 *
 * The {@code path} property should be a relative path coming after app context.
 * <p>
 *
 * <strong>Example:</strong> {@code @Page(path = "help") }
 * <p>
 *
 * The {@code publicPage} property enables to define whether annotated screen is public and doesn't require
 * authorization or not.
 *
 * @see Screen
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Page {

    String VALUE_ATTRIBUTE = "value";
    String PATH_ATTRIBUTE = "path";
    String PUBLIC_PAGE_ATTRIBUTE = "publicPage";

    @AliasFor(PATH_ATTRIBUTE)
    String value() default "";

    @AliasFor(VALUE_ATTRIBUTE)
    String path() default "";

    boolean publicPage() default false;
}
