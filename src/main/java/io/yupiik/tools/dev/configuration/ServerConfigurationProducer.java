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

import io.yupiik.uship.webserver.tomcat.TomcatWebServerConfiguration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.servlets.DefaultServlet;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.valves.AbstractAccessLogValve;

import java.util.List;
import java.util.stream.Stream;

@ApplicationScoped
public class ServerConfigurationProducer {
    @Produces
    @ApplicationScoped
    public TomcatWebServerConfiguration configuration(final ServerConfiguration configuration) {
        final var tomcat = new TomcatWebServerConfiguration();
        tomcat.setPort(configuration.getPort());
        tomcat.setAccessLogPattern(configuration.getAccessLogPattern());
        tomcat.setTomcatCustomizers(List.of(t -> t.getConnector().setProperty("compression", configuration.getCompression())));
        tomcat.setContextCustomizers(List.of(ctx -> customize(ctx, configuration.getResources(), configuration.getSendfileSize())));
        return tomcat;
    }

    private void customize(final StandardContext ctx, final ServerConfiguration.ResourceConfiguration resources, final int sendFileSize) {
        ctx.setParentClassLoader(Thread.currentThread().getContextClassLoader());
        if (resources != null && resources.getDocBase() != null) {
            ctx.setDocBase(resources.getDocBase());
            if (!resources.isWebResourcesCached()) {
                ctx.addLifecycleListener(event -> {
                    if (Lifecycle.CONFIGURE_START_EVENT.equals(event.getType())) {
                        ctx.getResources().setCachingAllowed(resources.isWebResourcesCached());
                    }
                });
            } // else it is the default so don't add a listener for nothing
            final var defaultServlet = Tomcat.addServlet(ctx, "default", DefaultServlet.class.getName());
            defaultServlet.setLoadOnStartup(1);
            defaultServlet.addInitParameter("sendfileSize", Integer.toString(sendFileSize));
            ctx.addServletMappingDecoded("/", "default");
            ctx.addMimeMapping("css", "text/css");
            ctx.addMimeMapping("js", "application/javascript");
            ctx.addMimeMapping("html", "text/html");
            ctx.addMimeMapping("svg", "image/svg+xml");
            ctx.addMimeMapping("png", "image/png");
            ctx.addMimeMapping("gif", "image/gif");
            ctx.addMimeMapping("jpg", "image/jpg");
            ctx.addMimeMapping("jpeg", "image/jpeg");
            ctx.addWelcomeFile("index.html");
        }
        Stream.of(ctx.getPipeline().getValves())
                .filter(AbstractAccessLogValve.class::isInstance)
                .map(AbstractAccessLogValve.class::cast)
                .forEach(v -> v.setCondition("skip-access-log"));
    }
}
