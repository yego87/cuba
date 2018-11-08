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

package com.haulmont.cuba.web.app.ui.statistics;

import com.haulmont.bali.util.ParamsMap;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.cuba.core.entity.JmxInstance;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.gui.Route;
import com.haulmont.cuba.gui.WindowManager.OpenType;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.GroupDatasource;
import com.haulmont.cuba.web.app.ui.jmxinstance.edit.JmxInstanceEditor;
import com.haulmont.cuba.web.jmx.JmxControlAPI;
import com.haulmont.cuba.web.jmx.JmxControlException;

import javax.inject.Inject;
import java.util.Map;
import java.util.UUID;

@Route("perfstat")
public class StatisticsWindow extends AbstractWindow {

    @Inject
    protected GroupTable<PerformanceParameter> paramsTable;

    @Inject
    protected GroupDatasource<PerformanceParameter, UUID> statisticsDs;

    @Inject
    protected JmxControlAPI jmxControlAPI;

    @Inject
    protected Label<String> localNodeLab;

    @Inject
    protected LookupPickerField<JmxInstance> jmxConnectionField;

    @Inject
    protected CollectionDatasource<JmxInstance, UUID> jmxInstancesDs;

    @Inject
    protected Timer valuesTimer;

    @Inject
    protected Metadata metadata;

    protected JmxInstance localJmxInstance;

    protected MetaClass parameterClass;

    protected int timerDelay = 5000;
    protected long startTime = System.currentTimeMillis();

    @Override
    public void init(Map<String, Object> params) {
        parameterClass = metadata.getClassNN(PerformanceParameter.class);
        initJMXTable();
        setNode(jmxConnectionField.getValue());
        valuesTimer.setDelay(timerDelay);

        paramsTable.setShowItemsCountForGroup(false);
    }

    protected void initJMXTable() {
        localJmxInstance = jmxControlAPI.getLocalInstance();

        jmxInstancesDs.refresh();
        jmxConnectionField.setValue(localJmxInstance);
        jmxConnectionField.setRequired(true);
        jmxConnectionField.addValueChangeListener(e -> {
            try {
                setNode(jmxConnectionField.getValue());
            } catch (JmxControlException ex) {
                JmxInstance jmxInstance = jmxConnectionField.getValue();
                showNotification(messages.getMessage("com.haulmont.cuba.web.app.ui.jmxcontrol",
                        "unableToConnectToInterface"), NotificationType.WARNING);
                if (jmxInstance != localJmxInstance) {
                    jmxConnectionField.setValue(localJmxInstance);
                }
            }
        });

        jmxConnectionField.removeAllActions();

        jmxConnectionField.addAction(new PickerField.LookupAction(jmxConnectionField) {
            @Override
            public void afterCloseLookup(String actionId) {
                jmxInstancesDs.refresh();
            }
        });

        jmxConnectionField.addAction(new BaseAction("actions.Add")
                .withIcon("icons/plus-btn.png")
                .withHandler(event -> {
                    JmxInstanceEditor instanceEditor = (JmxInstanceEditor) openEditor(
                            metadata.create(JmxInstance.class), OpenType.DIALOG);
                    instanceEditor.addCloseWithCommitListener(() -> {
                        jmxInstancesDs.refresh();
                        jmxConnectionField.setValue(instanceEditor.getItem());
                    });
                }));

        localNodeLab.setValue(jmxControlAPI.getLocalNodeName());
    }

    @SuppressWarnings("unused")
    public void onRefresh(Timer timer) {
        statisticsDs.refresh();

        StatisticsDatasource.DurationFormatter formatter = new StatisticsDatasource.DurationFormatter();
        String dur = formatter.apply((double) (System.currentTimeMillis() - startTime));
        paramsTable.setColumnCaption("recentStringValue", formatMessage("recentAverage", dur));
    }

    protected void setNode(JmxInstance currentNode) {
        statisticsDs.clear();
        startTime = System.currentTimeMillis();

        statisticsDs.refresh(ParamsMap.of("node", currentNode,
                                          "refreshPeriod", timerDelay));
        statisticsDs.groupBy(new Object[]{new MetaPropertyPath(parameterClass, parameterClass.getPropertyNN("parameterGroup"))});
        paramsTable.expandAll();
    }

    public void onMonitorThreads() {
        openWindow("threadsMonitoringWindow", OpenType.NEW_TAB,
                ParamsMap.of("node", jmxConnectionField.getValue()));
    }
}