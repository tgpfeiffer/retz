/**
 *    Retz
 *    Copyright (C) 2016 Nautilus Technologies, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package io.github.retz.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.retz.protocol.data.Application;

public class LoadAppRequest extends Request {
    private Application application;

    @JsonCreator
    public LoadAppRequest(@JsonProperty(value = "application", required = true) Application app) {
        this.application = app;
    }

    @JsonGetter("application")
    public Application application() {
        return application;
    }

    @Override
    public String resource() {
        return "/app/" + application.getAppid();
    }

    @Override
    public String method() {
        return PUT;
    }

    @Override
    public boolean hasPayload() {
        return true;
    }

    public static String resourcePattern() {
        return "/app/:name";
    }
}
