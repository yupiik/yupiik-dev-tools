/*
 * Copyright (c) 2022 - Yupiik SAS - https://www.yupiik.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.yupiik.tools.dev.configuration;

import io.yupiik.batch.runtime.configuration.Binder;
import io.yupiik.batch.runtime.configuration.Param;
import io.yupiik.batch.runtime.configuration.Prefix;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

@Prefix("server")
@ApplicationScoped
public class ServerConfiguration {
    @Param(name = "server-tomcat-compression", description = "Compression attribute on the connector, if on, accept-encoding=gzip is supported.")
    private String compression = "on";

    @Param(name = "server-tomcat-sendfile", description = "`sendfileSize` value (in kilobytes), if a static file is served and is higher than this size then the compression is ignored.")
    private int sendfileSize = 4096; // 4M

    @Param(name = "server-tomcat-port", description = "Tomcat port.")
    private int port = 8080;

    @Param(name = "server-tomcat-accessLogPattern", description = "Tomcat access log pattern.")
    private String accessLogPattern = "host=\"%h\" user=\"%{PharmaPlus-User}r\" time=\"%t\" req=\"%r\" status=\"%s\" byteSent=\"%b\" useragent=\"%{User-Agent}i\" duration=\"%{ms}Tms\"";

    @Param(name = "server-tomcat-resources", description = "A folder served by Tomcat")
    private ResourceConfiguration resources = new ResourceConfiguration();

    @Param(name = "server-health-key", description = "If different from `none`, it is the key checked calling `/health` endpoint.")
    private String healthKey = "none";

    @PostConstruct
    private void init() {
        Binder.bindPrefixed(this);
    }

    public String getCompression() {
        return compression;
    }

    public int getSendfileSize() {
        return sendfileSize;
    }

    public ResourceConfiguration getResources() {
        return resources;
    }

    public String getHealthKey() {
        return healthKey;
    }

    public int getPort() {
        return port;
    }

    public String getAccessLogPattern() {
        return accessLogPattern;
    }

    public static class ResourceConfiguration {
        @Param(description = "If set, the folder is served through HTTP.")
        private String docBase;

        @Param(description = "Should web resource be cached, mainly useful when docBase is set.")
        private boolean webResourcesCached = true;

        public String getDocBase() {
            return docBase;
        }

        public boolean isWebResourcesCached() {
            return webResourcesCached;
        }
    }
}
