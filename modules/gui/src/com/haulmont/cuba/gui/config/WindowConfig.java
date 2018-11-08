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
package com.haulmont.cuba.gui.config;

import com.google.common.base.Strings;
import com.haulmont.bali.util.Dom4j;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.Resources;
import com.haulmont.cuba.core.global.Scripting;
import com.haulmont.cuba.core.sys.AppContext;
import com.haulmont.cuba.gui.NoSuchRouteException;
import com.haulmont.cuba.gui.NoSuchScreenException;
import com.haulmont.cuba.gui.Page;
import com.haulmont.cuba.gui.components.AbstractFrame;
import com.haulmont.cuba.gui.components.Window;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.gui.sys.AnnotationScanMetadataReaderFactory;
import com.haulmont.cuba.gui.sys.PageDefinition;
import com.haulmont.cuba.gui.sys.UiControllerDefinition;
import com.haulmont.cuba.gui.sys.UiControllersConfiguration;
import com.haulmont.cuba.gui.sys.UiDescriptorUtils;
import com.haulmont.cuba.gui.xml.layout.ScreenXmlLoader;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringTokenizer;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GenericUI class holding information about all registered in <code>screens.xml</code> screens.
 */
@Component(WindowConfig.NAME)
public class WindowConfig {

    public static final String NAME = "cuba_WindowConfig";

    public static final String WINDOW_CONFIG_XML_PROP = "cuba.windowConfig";

    public static final Pattern ENTITY_SCREEN_PATTERN = Pattern.compile("([A-Za-z0-9]+[$_][A-Z][_A-Za-z0-9]*)\\..+");

    private final Logger log = LoggerFactory.getLogger(WindowConfig.class);

    protected Map<String, WindowInfo> screens = new HashMap<>();
    protected Map<String, String> routes = new HashMap<>();

    protected Map<Class, WindowInfo> primaryEditors = new HashMap<>();
    protected Map<Class, WindowInfo> primaryLookups = new HashMap<>();

    @Autowired(required = false)
    protected List<UiControllersConfiguration> configurations = Collections.emptyList();

    @Inject
    protected Resources resources;
    @Inject
    protected Scripting scripting;
    @Inject
    protected Metadata metadata;
    @Inject
    protected ScreenXmlLoader screenXmlLoader;

    @Inject
    protected ApplicationContext applicationContext;
    @Inject
    protected AnnotationScanMetadataReaderFactory metadataReaderFactory;

    protected volatile boolean initialized;

    protected ReadWriteLock lock = new ReentrantReadWriteLock();

    protected WindowAttributesProvider windowAttributesProvider = new WindowAttributesProvider() {
        @Override
        public WindowInfo.Type getType(WindowInfo windowInfo) {
            return extractWindowInfoType(windowInfo);
        }

        @Nullable
        @Override
        public String getTemplate(WindowInfo windowInfo) {
            return extractWindowTemplate(windowInfo);
        }

        @Override
        public boolean isMultiOpen(WindowInfo windowInfo) {
            return extractMultiOpen(windowInfo);
        }

        @Nonnull
        @Override
        public Class<? extends FrameOwner> getControllerClass(WindowInfo windowInfo) {
            return extractControllerClass(windowInfo);
        }
    };

    protected MetadataReaderFactory getMetadataReaderFactory() {
        return metadataReaderFactory;
    }

    protected ResourceLoader getResourceLoader() {
        return applicationContext;
    }

    protected WindowInfo.Type extractWindowInfoType(WindowInfo windowInfo) {
        Class<? extends FrameOwner> controllerClass = extractControllerClass(windowInfo);

        if (Screen.class.isAssignableFrom(controllerClass)) {
            return WindowInfo.Type.SCREEN;
        }

        if (ScreenFragment.class.isAssignableFrom(controllerClass)) {
            return WindowInfo.Type.FRAGMENT;
        }

        throw new IllegalStateException("Unknown type of screen " + windowInfo.getId());
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    protected Class<? extends FrameOwner> extractControllerClass(WindowInfo windowInfo) {
        if (windowInfo.getDescriptor() != null) {
            String className = windowInfo.getDescriptor().attributeValue("class");

            if (Strings.isNullOrEmpty(className)) {
                Element screenXml = screenXmlLoader.load(windowInfo.getTemplate(),
                        windowInfo.getId(), Collections.emptyMap());
                className = screenXml.attributeValue("class");
            }

            if (Strings.isNullOrEmpty(className)) {
                // fallback for legacy frames
                return AbstractFrame.class;
            }

            return (Class<? extends FrameOwner>) scripting.loadClassNN(className);
        }

        if (windowInfo.getControllerClassName() != null) {
            return loadDefinedScreenClass(windowInfo.getControllerClassName());
        }

        throw new IllegalStateException("Neither screen class nor descriptor is set for WindowInfo");
    }

    protected boolean extractMultiOpen(WindowInfo windowInfo) {
        if (windowInfo.getDescriptor() != null) {
            return Boolean.parseBoolean(windowInfo.getDescriptor().attributeValue("multipleOpen"));
        }

        if (windowInfo.getControllerClassName() != null) {
            Class<? extends FrameOwner> screenClass = loadDefinedScreenClass(windowInfo.getControllerClassName());

            UiController uiController = screenClass.getAnnotation(UiController.class);
            if (uiController == null) {
                // default is false
                return false;
            }
            // todo multi open
            return false;
        }

        throw new IllegalStateException("Neither screen class nor descriptor is set for WindowInfo");
    }

    @SuppressWarnings("unchecked")
    @Nullable
    protected String extractWindowTemplate(WindowInfo windowInfo) {
        if (windowInfo.getDescriptor() != null) {
            return windowInfo.getDescriptor().attributeValue("template");
        }

        if (windowInfo.getControllerClassName() != null) {
            Class<? extends FrameOwner> screenClass = loadDefinedScreenClass(windowInfo.getControllerClassName());

            UiDescriptor annotation = screenClass.getAnnotation(UiDescriptor.class);
            if (annotation == null) {
                return null;
            }
            String template = UiDescriptorUtils.getInferredTemplate(annotation, screenClass);
            if (!template.startsWith("/")) {
                String packageName = UiControllerUtils.getPackage(screenClass);
                if (StringUtils.isNotEmpty(packageName)) {
                    String relativePath = packageName.replace('.', '/');
                    template = "/" + relativePath + "/" + template;
                }
            }

            return template;
        }

        throw new IllegalStateException("Neither screen class nor descriptor is set for WindowInfo");
    }

    @SuppressWarnings("unchecked")
    protected Class<? extends FrameOwner> loadDefinedScreenClass(String className) {
        return (Class<? extends FrameOwner>) scripting.loadClassNN(className);
    }

    protected void checkInitialized() {
        if (!initialized) {
            lock.readLock().unlock();
            lock.writeLock().lock();
            try {
                if (!initialized) {
                    init();
                    initialized = true;
                }
            } finally {
                lock.readLock().lock();
                lock.writeLock().unlock();
            }
        }
    }

    protected void init() {
        long startTime = System.currentTimeMillis();

        screens.clear();
        primaryEditors.clear();
        primaryLookups.clear();

        loadScreenConfigurations();
        loadScreensXml();

        log.info("WindowConfig initialized in {} ms", System.currentTimeMillis() - startTime);
    }

    protected void loadScreenConfigurations() {
        for (UiControllersConfiguration provider : configurations) {
            List<UiControllerDefinition> uiControllers = provider.getUiControllers();

            for (UiControllerDefinition definition : uiControllers) {
                WindowInfo windowInfo = new WindowInfo(definition.getId(), windowAttributesProvider,
                        definition.getControllerClass(), definition.getPageDefinition());
                registerScreen(definition.getId(), windowInfo);
            }
        }
    }

    protected void loadScreensXml() {
        String configName = AppContext.getProperty(WINDOW_CONFIG_XML_PROP);
        StringTokenizer tokenizer = new StringTokenizer(configName);
        for (String location : tokenizer.getTokenArray()) {
            Resource resource = resources.getResource(location);
            if (resource.exists()) {
                try (InputStream stream = resource.getInputStream()) {
                    loadConfig(Dom4j.readDocument(stream).getRootElement());
                } catch (IOException e) {
                    throw new RuntimeException("Unable to read window config from " + location, e);
                }
            } else {
                log.warn("Resource {} not found, ignore it", location);
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected void loadConfig(Element rootElem) {
        for (Element element : rootElem.elements("include")) {
            String fileName = element.attributeValue("file");
            if (!StringUtils.isBlank(fileName)) {
                String incXml = resources.getResourceAsString(fileName);
                if (incXml == null) {
                    log.warn("File {} not found, ignore it", fileName);
                    continue;
                }
                loadConfig(Dom4j.readDocument(incXml).getRootElement());
            }
        }
        for (Element element : rootElem.elements("screen")) {
            String id = element.attributeValue("id");
            if (StringUtils.isBlank(id)) {
                log.warn("Invalid window config: 'id' attribute not defined");
                continue;
            }

            PageDefinition pageDef = loadPageDefinition(element);

            WindowInfo windowInfo = new WindowInfo(id, windowAttributesProvider, element, pageDef);
            registerScreen(id, windowInfo);
        }
    }

    protected PageDefinition loadPageDefinition(Element screenRegistration) {
        String templateAttr = screenRegistration.attributeValue("template");
        if (templateAttr == null || templateAttr.isEmpty()) {
            return null;
        }

        Element screenTemplate = screenXmlLoader.load(
                templateAttr,
                screenRegistration.attributeValue("id"),
                Collections.emptyMap());

        String screenControllerFqn = screenTemplate.attributeValue("class");
        if (screenControllerFqn == null || screenControllerFqn.isEmpty()) {
            return null;
        }

        Map<String, Object> pageAnnotation = loadClassMetadata(screenControllerFqn)
                .getAnnotationMetadata()
                .getAnnotationAttributes(Page.class.getName());

        if (pageAnnotation == null) {
            return null;
        }

        String pathAttr = (String) pageAnnotation.get(Page.ROUTE_ATTRIBUTE);
        //noinspection unchecked
        Class<? extends Screen> parentAttr = (Class<? extends Screen>) pageAnnotation.get(Page.PARENT_ATTRIBUTE);

        return new PageDefinition(pathAttr, parentAttr);
    }

    protected void registerScreen(String id, WindowInfo windowInfo) {
        String controllerClassName = windowInfo.getControllerClassName();
        if (controllerClassName != null) {
            MetadataReader classMetadata = loadClassMetadata(controllerClassName);
            AnnotationMetadata annotationMetadata = classMetadata.getAnnotationMetadata();

            registerPrimaryEditor(windowInfo, annotationMetadata);
            registerPrimaryLookup(windowInfo, annotationMetadata);
        }

        screens.put(id, windowInfo);

        PageDefinition pageDef = windowInfo.getPageDefinition();
        if (pageDef != null) {
            routes.put(pageDef.getRoute(), id);
        }
    }

    protected void registerPrimaryEditor(WindowInfo windowInfo, AnnotationMetadata annotationMetadata) {
        Map<String, Object> primaryEditorAnnotation =
                annotationMetadata.getAnnotationAttributes(PrimaryEditorScreen.class.getName());
        if (primaryEditorAnnotation != null) {
            Class entityClass = (Class) primaryEditorAnnotation.get("value");
            if (entityClass != null) {
                MetaClass metaClass = metadata.getClass(entityClass);
                MetaClass originalMetaClass = metadata.getExtendedEntities().getOriginalOrThisMetaClass(metaClass);
                primaryEditors.put(originalMetaClass.getJavaClass(), windowInfo);
            }
        }
    }

    protected void registerPrimaryLookup(WindowInfo windowInfo, AnnotationMetadata annotationMetadata) {
        Map<String, Object> primaryEditorAnnotation =
                annotationMetadata.getAnnotationAttributes(PrimaryLookupScreen.class.getName());
        if (primaryEditorAnnotation != null) {
            Class entityClass = (Class) primaryEditorAnnotation.get("value");
            if (entityClass != null) {
                MetaClass metaClass = metadata.getClass(entityClass);
                MetaClass originalMetaClass = metadata.getExtendedEntities().getOriginalOrThisMetaClass(metaClass);
                primaryLookups.put(originalMetaClass.getJavaClass(), windowInfo);
            }
        }
    }

    protected MetadataReader loadClassMetadata(String className) {
        Resource resource = getResourceLoader().getResource("/" + className.replace(".", "/") + ".class");
        if (!resource.isReadable()) {
            throw new RuntimeException(String.format("Resource %s is not readable for class %s", resource, className));
        }
        try {
            return getMetadataReaderFactory().getMetadataReader(resource);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read resource " + resource, e);
        }
    }

    /**
     * Make the config to reload screens on next request.
     */
    public void reset() {
        initialized = false;
    }

    /**
     * Get screen information by screen ID.
     *
     * @param id screen ID as set up in <code>screens.xml</code>
     * @return screen's registration information or null if not found
     */
    @Nullable
    public WindowInfo findWindowInfo(String id) {
        lock.readLock().lock();
        try {
            checkInitialized();

            WindowInfo windowInfo = screens.get(id);
            if (windowInfo == null) {
                Matcher matcher = ENTITY_SCREEN_PATTERN.matcher(id);
                if (matcher.matches()) {
                    MetaClass metaClass = metadata.getClass(matcher.group(1));
                    if (metaClass == null)
                        return null;
                    MetaClass originalMetaClass = metadata.getExtendedEntities().getOriginalMetaClass(metaClass);
                    if (originalMetaClass != null) {
                        String originalId = new StringBuilder(id)
                                .replace(matcher.start(1), matcher.end(1), originalMetaClass.getName()).toString();
                        windowInfo = screens.get(originalId);
                    }
                }
            }
            return windowInfo;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get screen information by screen ID.
     *
     * @param id screen ID as set up in <code>screens.xml</code>
     * @return screen's registration information
     * @throws NoSuchScreenException if the screen with specified ID is not registered
     */
    public WindowInfo getWindowInfo(String id) {
        WindowInfo windowInfo = findWindowInfo(id);
        if (windowInfo == null) {
            throw new NoSuchScreenException(id);
        }
        return windowInfo;
    }

    /**
     * Get screen information by route.
     *
     * @param route route
     * @return screen's registration information or null if not found
     */
    @Nullable
    public WindowInfo findWindowInfoByRoute(String route) {
        String screenId = routes.get(route);
        return screenId != null
                ? findWindowInfo(screenId)
                : null;
    }

    /**
     * Get screen information by route.
     *
     * @param route route
     * @return screen's registration information
     * @throws NoSuchScreenException if the screen with specified ID is not registered
     */
    public WindowInfo getWindowInfoByRoute(String route) {
        WindowInfo windowInfo = findWindowInfoByRoute(route);
        if (windowInfo == null) {
            throw new NoSuchRouteException(route);
        }
        return windowInfo;
    }

    /**
     * @return true if the configuration contains a screen with provided ID
     */
    public boolean hasWindow(String id) {
        return findWindowInfo(id) != null;
    }

    /**
     * All registered screens
     */
    public Collection<WindowInfo> getWindows() {
        lock.readLock().lock();
        try {
            checkInitialized();
            return screens.values();
        } finally {
            lock.readLock().unlock();
        }
    }

    public String getMetaClassScreenId(MetaClass metaClass, String suffix) {
        MetaClass screenMetaClass = metaClass;
        MetaClass originalMetaClass = metadata.getExtendedEntities().getOriginalMetaClass(metaClass);
        if (originalMetaClass != null) {
            screenMetaClass = originalMetaClass;
        }

        return screenMetaClass.getName() + suffix;
    }

    public String getBrowseScreenId(MetaClass metaClass) {
        return getMetaClassScreenId(metaClass, Window.BROWSE_WINDOW_SUFFIX);
    }

    public String getLookupScreenId(MetaClass metaClass) {
        MetaClass originalMetaClass = metadata.getExtendedEntities().getOriginalOrThisMetaClass(metaClass);
        WindowInfo windowInfo = primaryLookups.get(originalMetaClass.getJavaClass());
        if (windowInfo != null) {
            return windowInfo.getId();
        }

        return getMetaClassScreenId(metaClass, Window.LOOKUP_WINDOW_SUFFIX);
    }

    public String getEditorScreenId(MetaClass metaClass) {
        MetaClass originalMetaClass = metadata.getExtendedEntities().getOriginalOrThisMetaClass(metaClass);
        WindowInfo windowInfo = primaryEditors.get(originalMetaClass.getJavaClass());
        if (windowInfo != null) {
            return windowInfo.getId();
        }

        return getMetaClassScreenId(metaClass, Window.EDITOR_WINDOW_SUFFIX);
    }

    public WindowInfo getEditorScreen(Entity entity) {
        MetaClass metaClass = entity.getMetaClass();
        MetaClass originalMetaClass = metadata.getExtendedEntities().getOriginalOrThisMetaClass(metaClass);
        WindowInfo windowInfo = primaryEditors.get(originalMetaClass.getJavaClass());
        if (windowInfo != null) {
            return windowInfo;
        }

        String editorScreenId = getEditorScreenId(metaClass);
        return getWindowInfo(editorScreenId);
    }

    /**
     * Get available lookup screen by class of entity
     *
     * @param entityClass entity class
     * @return id of lookup screen
     * @throws NoSuchScreenException if the screen with specified ID is not registered
     */
    public WindowInfo getLookupScreen(Class<? extends Entity> entityClass) {
        MetaClass metaClass = metadata.getSession().getClass(entityClass);

        MetaClass originalMetaClass = metadata.getExtendedEntities().getOriginalOrThisMetaClass(metaClass);
        WindowInfo windowInfo = primaryLookups.get(originalMetaClass.getJavaClass());
        if (windowInfo != null) {
            return windowInfo;
        }

        String lookupScreenId = getAvailableLookupScreenId(metaClass);
        return getWindowInfo(lookupScreenId);
    }

    public String getAvailableLookupScreenId(MetaClass metaClass) {
        String id = getLookupScreenId(metaClass);
        if (!hasWindow(id)) {
            id = getBrowseScreenId(metaClass);
        }
        return id;
    }
}