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

package com.haulmont.cuba.gui.sys;

import com.haulmont.bali.util.Preconditions;
import com.haulmont.cuba.gui.Page;
import com.haulmont.cuba.gui.screen.UiController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.haulmont.bali.util.Preconditions.checkNotNullArgument;

/**
 * Configuration that performs ClassPath scanning of {@link UiController}s and provides {@link UiControllerDefinition}.
 */
public class UiControllersConfiguration extends AbstractScanConfiguration {

    private static final Logger log = LoggerFactory.getLogger(UiControllersConfiguration.class);

    protected ApplicationContext applicationContext;
    protected MetadataReaderFactory metadataReaderFactory;

    protected List<String> basePackages = Collections.emptyList();
    protected List<UiControllerDefinition> explicitDefinitions = Collections.emptyList();

    public UiControllersConfiguration() {
    }

    @Inject
    protected void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public List<String> getBasePackages() {
        return basePackages;
    }

    public void setBasePackages(List<String> basePackages) {
        Preconditions.checkNotNullArgument(basePackages);

        this.basePackages = basePackages;
    }

    public List<UiControllerDefinition> getExplicitDefinitions() {
        return explicitDefinitions;
    }

    public void setExplicitDefinitions(List<UiControllerDefinition> explicitDefinitions) {
        checkNotNullArgument(explicitDefinitions);

        this.explicitDefinitions = explicitDefinitions;
    }

    public List<UiControllerDefinition> getUiControllers() {
        log.trace("Scanning packages {}", basePackages);

        Stream<UiControllerDefinition> scannedControllersStream = basePackages.stream()
                .flatMap(this::scanPackage)
                .filter(this::isCandidateUiController)
                .map(this::extractControllerDefinition);

        return Stream.concat(scannedControllersStream, explicitDefinitions.stream())
                .collect(Collectors.toList());
    }

    protected UiControllerDefinition extractControllerDefinition(MetadataReader metadataReader) {
        Map<String, Object> uiControllerAnn =
                metadataReader.getAnnotationMetadata().getAnnotationAttributes(UiController.class.getName());

        String idAttr = null;
        String valueAttr = null;
        if (uiControllerAnn != null) {
            idAttr = (String) uiControllerAnn.get(UiController.ID_ATTRIBUTE);
            valueAttr = (String) uiControllerAnn.get(UiController.VALUE_ATTRIBUTE);
        }

        Map<String, Object> pageAnnotation = metadataReader.getAnnotationMetadata().getAnnotationAttributes(Page.class.getName());

        String pathAttr = null;
        boolean publicPageAttr = false;
        if (pageAnnotation != null) {
            pathAttr = (String) pageAnnotation.get(Page.PATH_ATTRIBUTE);
            publicPageAttr = (boolean) pageAnnotation.get(Page.PUBLIC_PAGE_ATTRIBUTE);
        }

        String className = metadataReader.getClassMetadata().getClassName();
        String controllerId = UiDescriptorUtils.getInferredScreenId(idAttr, valueAttr, className);

        return pageAnnotation != null
                ? new UiControllerDefinition(controllerId, className)
                : new UiControllerDefinition(controllerId, className, pathAttr, publicPageAttr);
    }

    protected boolean isCandidateUiController(MetadataReader metadataReader) {
        return metadataReader.getClassMetadata().isConcrete()
                && metadataReader.getAnnotationMetadata().hasAnnotation(UiController.class.getName());
    }

    @Override
    protected MetadataReaderFactory getMetadataReaderFactory() {
        return metadataReaderFactory;
    }

    @Inject
    public void setMetadataReaderFactory(AnnotationScanMetadataReaderFactory metadataReaderFactory) {
        this.metadataReaderFactory = metadataReaderFactory;
    }

    @Override
    protected ResourceLoader getResourceLoader() {
        return applicationContext;
    }

    @Override
    protected Environment getEnvironment() {
        return applicationContext.getEnvironment();
    }
}