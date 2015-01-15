package se.sperber.cryson.examples.advancedcrysondiary.initialization;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AllowSymLinkAliasChecker;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import se.sperber.cryson.examples.advancedcrysondiary.web_service.DiaryService;
import se.sperber.cryson.service.CrysonFrontendService;
import se.sperber.cryson.spring.JettySpringHelper;

import javax.annotation.PostConstruct;

@Component
public class HttpServer {

  @Autowired
  private JettySpringHelper jettySpringHelper;

  @Value("${httpserver.port}") private int port;
  @Value("${httpserver.file.root_path}") private String fileRootPath;

  @PostConstruct
  public void startServer() throws Exception {
    Server server = new Server(port);
    ServletContextHandler context = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS);
    jettySpringHelper.addFileServlet(context, "/*", fileRootPath);
    jettySpringHelper.addJerseyServlet(context, "/cryson/*", CrysonFrontendService.class.getPackage().getName());
    jettySpringHelper.addJerseyServlet(context, "/services/*", DiaryService.class.getPackage().getName());
    jettySpringHelper.addSecurityFilter(context, "/*");
    context.addAliasCheck(new AllowSymLinkAliasChecker());

    server.start();
  }

}
