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
package com.haulmont.cuba.web.log;

import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.annotation.Nullable;
import java.util.Date;

public class LogItem {

    private Date timestamp;
    private LogLevel level;
    private String message;
    private Throwable throwable;

    public LogItem(Date timestamp, LogLevel level, String message, Throwable throwable) {
        this.timestamp = timestamp;
        this.level = level;
        this.message = message;
        this.throwable = throwable;
    }

    public LogLevel getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    public String getStacktrace() {
        return throwable != null ? ExceptionUtils.getStackTrace(throwable) : "";
    }

    @Nullable
    public Throwable getThrowable() {
        return throwable;
    }

    public Date getTimestamp() {
        return timestamp;
    }
}