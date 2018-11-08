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

package com.haulmont.cuba.gui.app.security.session.browse;

import com.haulmont.bali.util.ParamsMap;
import com.haulmont.chile.core.datatypes.Datatypes;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.gui.Page;
import com.haulmont.cuba.gui.WindowManager.OpenType;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.Action.Status;
import com.haulmont.cuba.gui.components.DialogAction.Type;
import com.haulmont.cuba.security.app.UserSessionService;
import com.haulmont.cuba.security.entity.UserSessionEntity;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.*;

@Page("userSessions")
public class SessionBrowser extends AbstractLookup {

    @Inject
    protected UserSessionSource userSessionSource;

    @Inject
    protected UserSessionService uss;

    @Inject
    protected Table<UserSessionEntity> sessionsTable;

    @Inject
    protected UserSessionsDatasource sessionsDs;

    @Inject
    protected Label lastUpdateTsLab;

    @Inject
    protected TextField<String> userLogin;

    @Inject
    protected TextField<String> userName;

    @Inject
    protected TextField<String> userAddress;

    @Inject
    protected TextField<String> userInfo;

    @Named("sessionsTable.refresh")
    protected Action refreshAction;

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        sessionsTable.setTextSelectionEnabled(true);

        sessionsDs.addCollectionChangeListener(e -> {
            String time = Datatypes.getNN(Date.class).format(sessionsDs.getUpdateTs(), userSessionSource.getLocale());
            lastUpdateTsLab.setValue(time);
        });

        addAction(refreshAction);
    }

    public void refresh() {
        Map<String, Object> fieldValues = new HashMap<>();
        String userLoginStr = userLogin.getValue();
        if (!StringUtils.isEmpty(userLoginStr))
            fieldValues.put("userLogin", userLoginStr);
        String userNameStr = userName.getValue();
        if (!StringUtils.isEmpty(userNameStr))
            fieldValues.put("userName", userNameStr);
        String userAddressStr = userAddress.getValue();
        if (!StringUtils.isEmpty(userAddressStr))
            fieldValues.put("userAddress", userAddressStr);
        String userInfoStr = userInfo.getValue();
        if (!StringUtils.isEmpty(userInfoStr))
            fieldValues.put("userInfo", userInfoStr);
        sessionsDs.refresh(fieldValues);
    }

    public void clearTextFields() {
        userLogin.setValue("");
        userName.setValue("");
        userAddress.setValue("");
        userInfo.setValue("");
        refresh();
    }

    public void message() {
        Set<UserSessionEntity> selected = sessionsTable.getSelected();
        Set<UserSessionEntity> all = new HashSet<>(sessionsDs.getItems());

        SessionMessageWindow window = (SessionMessageWindow) openWindow("sessionMessageWindow", OpenType.DIALOG,
                ParamsMap.of("selectedSessions", selected,
                             "allSessions", all));

        window.addCloseListener(actionId -> {
            String result = window.getResult();
            if (!StringUtils.isBlank(result)) {
                showNotification(result, NotificationType.TRAY);
                sessionsTable.focus();
            }
        });
    }

    public void kill() {
        Set<UserSessionEntity> selected = sessionsTable.getSelected();
        if (selected.isEmpty())
            return;

        showOptionDialog(
                messages.getMainMessage("dialogs.Confirmation"),
                messages.getMessage(getClass(), "killConfirm"),
                MessageType.CONFIRMATION,
                new Action[]{
                        new DialogAction(Type.OK)
                                .withHandler(event -> disconnectSession(selected)),
                        new DialogAction(Type.CANCEL, Status.PRIMARY)
                }
        );
    }

    protected void disconnectSession(Set<UserSessionEntity> selected) {
        for (UserSessionEntity session : selected) {
            if (!session.getId().equals(userSessionSource.getUserSession().getId())) {
                uss.killSession(session.getId());
            } else {
                showNotification(getMessage("killUnavailable"), NotificationType.WARNING);
            }
        }
        sessionsTable.getDatasource().refresh();
        sessionsTable.focus();
    }
}