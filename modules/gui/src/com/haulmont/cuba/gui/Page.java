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
 * Registers an annotated class as corresponding to some route. It means that that screen should be opened when URL ends
 * with specified route and that URL should be changed according to the route when the screen is opened.
 * <p>
 *
 * <pre>
 *     Example:
 *
 *     Screen annotated with {@code @Page("help")} corresponds to "/app/{rootRoute}/help",
 *     where {rootRoute} equals to currently opened root screen.
 *
 *     This screen will be opened when URL changes from "/app/{rootRoute}" to "/app/{rootRoute}/help" and URL will be
 *     changed in the same way when the screen is opened.
 * </pre>
 * <p>
 *
 * Required parent screen that should be opened to form a route can be specified with "parent" property. If this
 * property is set but parent screen isn't opened annotated screen route will not be applied.
 * <p><br/>
 *
 * URL squashing can be used if the "parentPrefix" property is specified. Annotated screen route will be merged with
 * a route configured in property value screen if a parent is currently opened.
 * <p>
 *
 * <pre>
 *     Example. Let two screens exist:
 * {@code
 *
 * @Page("orders")
 * public class OrderBrowse { ... }
 *
 * @Page(route = "orders/edit", parentPrefix = OrderBrowse.class)
 * public class OrderEdit { ... }
 * }
 *
 *     When OrderEdit screen is opened after OrderBrowse screen resulting address will be
 *     "app/{rootRoute/users/users/edit}.
 *
 *     It allows to specify clear routes for each screen and avoid repeats in URL.
 * </pre>
 *
 * Annotated class must be a direct or indirect subclass of {@link Screen}.
 * <p>
 *
 * @see Screen
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Page {

    String VALUE_ATTRIBUTE = "value";
    String ROUTE_ATTRIBUTE = "route";
    String PARENT_ATTRIBUTE = "parent";

    @AliasFor(ROUTE_ATTRIBUTE)
    String value() default "";

    @AliasFor(VALUE_ATTRIBUTE)
    String route() default "";

    Class<? extends Screen> parent() default Screen.class;

    Class<? extends Screen> parentPrefix() default Screen.class;
}
