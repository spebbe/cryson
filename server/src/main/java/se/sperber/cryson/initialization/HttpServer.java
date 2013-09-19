/*
  Cryson
  
  Copyright 2011-2012 Björn Sperber (cryson@sperber.se)
  
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  
  http://www.apache.org/licenses/LICENSE-2.0
  
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/

package se.sperber.cryson.initialization;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.log.Log;
import org.mortbay.management.MBeanContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import se.sperber.cryson.spring.JettySpringHelper;

import java.lang.management.ManagementFactory;

@Component("crysonHttpServer")
@Profile("cryson_httpserver")
public class HttpServer {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpServer.class);
  private static final String SERVICE_PACKAGE = "se.sperber.cryson.service";

  private static final int DEFAULT_HEADER_BUFFER_SIZE = 16384;

  @Autowired
  private JettySpringHelper jettySpringHelper;

  @Value("${cryson.httpserver.port}") private int port;
  @Value("${cryson.httpserver.cryson.context_path}") private String crysonContextPath;
  @Value("${cryson.httpserver.file.context_path}") private String fileContextPath;
  @Value("${cryson.httpserver.file.root_path}") private String fileRootPath;
  @Value("${cryson.httpserver.user.service_context_path}") private String userServiceContextPath;
  @Value("${cryson.httpserver.user.service_package}") private String userServicePackage;
  @Value("${cryson.security.enabled}") private Boolean securityEnabled;
  
  private Server server;
  private Connector connector;
  private Context crysonContext;
  private MBeanContainer mbeanContainer;


  public void setupJetty() {
    server = new Server();
    connector = new SocketConnector();
    connector.setHeaderBufferSize(DEFAULT_HEADER_BUFFER_SIZE);
    connector.setPort(port);
    server.addConnector(connector);
    server.setSendServerVersion(false);
    crysonContext = new Context(server, "/", Context.SESSIONS);
    jettySpringHelper.addFileServlet(crysonContext, fileContextPath, fileRootPath);
    jettySpringHelper.addJerseyServlet(crysonContext, crysonContextPath, SERVICE_PACKAGE);
    jettySpringHelper.addJerseyServlet(crysonContext, userServiceContextPath, userServicePackage);
    if (securityEnabled) {
      jettySpringHelper.addSecurityFilter(crysonContext, "/*");
    }
    setupJettyJMX();
  }

  private void setupJettyJMX() {
    mbeanContainer=new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
    server.getContainer().addEventListener(mbeanContainer);
    mbeanContainer.addBean(server);
    mbeanContainer.addBean(connector);
    mbeanContainer.addBean(Log.getLog());
  }

  public void teardownJetty() {
    try {
      teardownJettyJmx();
    } catch(Exception e) {
      LOGGER.error("Unexpected error while stopping jetty", e);
    }
  }

  private void teardownJettyJmx() {
    mbeanContainer.removeBean(server);
    mbeanContainer.removeBean(connector);
    mbeanContainer.removeBean(Log.getLog());

    // mbeans for handlers and context are implicitly added, but need to be explicitly removed...
    Handler[] handlers = server.getHandlers();
    if (handlers != null) {
      for (Handler handler : handlers) {
        mbeanContainer.removeBean(handler);
      }
    }
    mbeanContainer.removeBean(crysonContext);
  }

  public void startServer() throws Exception {
    setupJetty();
    server.start();
  }

  public void joinServer() throws Exception {
    server.join();
  }

  public void stopServer() throws Exception {
    teardownJetty();
    server.stop();
  }

}
