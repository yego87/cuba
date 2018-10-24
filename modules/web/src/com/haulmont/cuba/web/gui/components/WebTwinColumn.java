/*
 * Copyright (c) 2008-2016 Haulmont.
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
 *
 */
package com.haulmont.cuba.web.gui.components;

import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.MetadataTools;
import com.haulmont.cuba.gui.components.CaptionMode;
import com.haulmont.cuba.gui.components.TwinColumn;
import com.haulmont.cuba.gui.components.data.Options;
import com.haulmont.cuba.gui.components.data.meta.EntityValueSource;
import com.haulmont.cuba.gui.components.data.meta.OptionsBinding;
import com.haulmont.cuba.gui.components.data.options.OptionsBinder;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.web.gui.icons.IconResolver;
import com.haulmont.cuba.web.widgets.CubaTwinColSelect;

import javax.inject.Inject;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

public class WebTwinColumn<V> extends WebV8AbstractField<CubaTwinColSelect<V>, Set<V>, V>
        implements TwinColumn<V> {

    protected StyleProvider styleProvider;
    protected OptionsBinding<V> optionsBinding;
    protected Function<? super V, String> optionCaptionProvider;

    protected MetadataTools metadataTools;

    protected IconResolver iconResolver = AppBeans.get(IconResolver.class);

    public WebTwinColumn() {
        component = createComponent();
        component.setItemCaptionGenerator(this::generateItemCaption);
        /*{
            @Override
            public void setPropertyDataSource(Property newDataSource) {
                super.setPropertyDataSource(new PropertyAdapter(newDataSource) {
                    @Override
                    public Object getValue() {
                        final Object o = itemProperty.getValue();
                        return getKeyFromValue(o);
                    }

                    @Override
                    public void setValue(Object newValue) throws ReadOnlyException, Converter.ConversionException {
                        final Object v = getValueFromKey(newValue);
                        itemProperty.setValue(v);
                    }
                });
            }

            @Override
            public Resource getItemIcon(Object itemId) {
                if (styleProvider != null) {
                    @SuppressWarnings({"unchecked"})
                    Entity item = optionsDatasource.getItem(itemId);
                    String resURL = styleProvider.getItemIcon(item, isSelected(itemId));

                    return iconResolver.getIconResource(resURL);
                } else {
                    return null;
                }
            }
        };*/
    }

    protected CubaTwinColSelect<V> createComponent() {
        return new CubaTwinColSelect<>();
    }

    @Inject
    protected void setMetadataTools(MetadataTools metadataTools) {
        this.metadataTools = metadataTools;
    }

    @Override
    public void setOptions(Options<V> options) {
        if (this.optionsBinding != null) {
            this.optionsBinding.unbind();
            this.optionsBinding = null;
        }

        if (options != null) {
            OptionsBinder optionsBinder = beanLocator.get(OptionsBinder.NAME);
            this.optionsBinding = optionsBinder.bind(options, this, this::setItemsToPresentation);
            this.optionsBinding.activate();
        }
    }

    protected void setItemsToPresentation(Stream<V> options) {
        component.setItems(options);
    }

    @Override
    public Options<V> getOptions() {
        return optionsBinding != null ? optionsBinding.getSource() : null;
    }

    @Override
    public void setOptionCaptionProvider(Function<? super V, String> captionProvider) {
        this.optionCaptionProvider = captionProvider;
    }

    protected String generateDefaultItemCaption(V item) {
        if (valueBinding != null && valueBinding.getSource() instanceof EntityValueSource) {
            EntityValueSource entityValueSource = (EntityValueSource) valueBinding.getSource();
            return metadataTools.format(item, entityValueSource.getMetaPropertyPath().getMetaProperty());
        }

        return metadataTools.format(item);
    }

    protected String generateItemCaption(V item) {
        if (item == null) {
            return null;
        }

        if (optionCaptionProvider != null) {
            return optionCaptionProvider.apply(item);
        }

        return generateDefaultItemCaption(item);
    }

    @Override
    public Function<? super V, String> getOptionCaptionProvider() {
        return optionCaptionProvider;
    }

    /*public static class CollectionPropertyWrapper extends PropertyWrapper {

        public CollectionPropertyWrapper(Object item, MetaPropertyPath propertyPath) {
            super(item, propertyPath);
        }

        @Override
        public void setValue(Object newValue) throws ReadOnlyException, Converter.ConversionException {
            Class propertyType = propertyPath.getMetaProperty().getJavaType();
            if (Set.class.isAssignableFrom(propertyType)) {
                if (newValue == null) {
                    newValue = new HashSet();
                } else {
                    if (newValue instanceof Collection) {
                        newValue = new HashSet<>((Collection<?>) newValue);
                    } else {
                        newValue = Collections.singleton(newValue);
                    }
                }
            } else if (List.class.isAssignableFrom(propertyType)) {
                if (newValue == null) {
                    newValue = new ArrayList();
                } else {
                    if (newValue instanceof Collection) {
                        newValue = new ArrayList<>((Collection<?>) newValue);
                    } else {
                        newValue = Collections.singletonList(newValue);
                    }
                }
            }
            super.setValue(newValue);
        }

        @Override
        public Object getValue() {
            Object value = super.getValue();
            if (value instanceof Collection) {
                Class propertyType = propertyPath.getMetaProperty().getJavaType();
                if (Set.class.isAssignableFrom(propertyType)) {
                    value = new HashSet<>((Collection<?>) value);
                } else if (List.class.isAssignableFrom(propertyType)) {
                    value = new LinkedHashSet<>((Collection<?>) value);
                }
            }
            return value;
        }

        @Override
        public Class getType() {
            return Object.class;
        }
    }*/

    @Override
    public V getValue() {
        return super.getValue();
//        if (optionsDatasource != null) {
//            final Object key = super.getValue();
//            return getValueFromKey(key);
//        } else {
//            return wrapAsCollection(super.getValue());
//        }
    }

    @Override
    public void setValue(V value) {
        super.setValue((V) getKeyFromValue(value));
    }

    @Override
    public int getColumns() {
        // doesn't support cause of Vaadin 8
        return 0;
    }

    @Override
    public void setColumns(int columns) {
        // doesn't support cause of Vaadin 8
    }

    @Override
    public int getRows() {
        return component.getRows();
    }

    @Override
    public void setRows(int rows) {
        component.setRows(rows);
    }

    @Override
    public void setDatasource(Datasource datasource, String property) {
        super.setDatasource(datasource, property);
//        component.setConverter(new ObjectToObjectConverter());
    }

    @Override
    public void setStyleProvider(final StyleProvider styleProvider) {
        this.styleProvider = styleProvider;
        if (styleProvider != null) {
//            component.setStyleGenerator((source, itemId, selected) -> {
//                final Entity item = optionsDatasource.getItem(itemId);
//                return styleProvider.getStyleName(item, itemId, component.isSelected(itemId));
//            });
        } else {
            component.setStyleGenerator(null);
        }
    }

    @Override
    public void setAddAllBtnEnabled(boolean enabled) {
        component.setAddAllBtnEnabled(enabled);
    }

    @Override
    public boolean isAddAllBtnEnabled() {
        return component.isAddAllBtnEnabled();
    }

    @Override
    public void setLeftColumnCaption(String leftColumnCaption) {
        component.setLeftColumnCaption(leftColumnCaption);
    }

    @Override
    public String getLeftColumnCaption() {
        return component.getLeftColumnCaption();
    }

    @Override
    public void setRightColumnCaption(String rightColumnCaption) {
        component.setRightColumnCaption(rightColumnCaption);
    }

    @Override
    public String getRightColumnCaption() {
        return component.getRightColumnCaption();
    }

    @Override
    public CaptionMode getCaptionMode() {
        // doesn't support cause of Vaadin 8
        return null;
    }

    @Override
    public void setCaptionMode(CaptionMode captionMode) {
        // doesn't support cause of Vaadin 8
    }

    @Override
    public String getCaptionProperty() {
        // doesn't support cause of Vaadin 8
        return null;
    }

    @Override
    public void setCaptionProperty(String captionProperty) {
        // doesn't support cause of Vaadin 8
    }

/*    protected <T> T getValueFromKey(Object key) {
        if (key instanceof Collection) {
            final Set<Object> set = new LinkedHashSet<>();
            for (Object o : (Collection) key) {
                Object t = getValue(o);
                set.add(t);
            }
            return (T) set;
        } else {
            final Object o = getValue(key);
            return (T) wrapAsCollection(o);
        }
    }

    protected Object getValue(Object o) {
        Object t;
        if (o instanceof Enum) {
            t = o;
        } else if (o instanceof Entity) {
            t = o;
        } else {
            t = optionsDatasource.getItem(o);
        }
        return t;
    }
    */

    protected Object getKeyFromValue(Object value) {
        if (value == null) {
            return null;
        }
        final Set<Object> set = new HashSet<>();
        if (value instanceof Collection) {
            for (Object o : (Collection) value) {
                Object t = getKey(o);
                set.add(t);
            }

        } else {
            getKey(value);
            set.add(getKey(value));
        }
        return set;
    }

    protected Object getKey(Object o) {
        Object t;
        if (o instanceof Entity) {
            t = ((Entity) o).getId();
        } /*else if (o instanceof Enum) {
            t = o;
        }*/ else {
            t = o;
        }
        return t;
    }
}