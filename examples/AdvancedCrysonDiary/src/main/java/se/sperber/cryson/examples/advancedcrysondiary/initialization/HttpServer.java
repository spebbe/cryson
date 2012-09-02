package se.sperber.cryson.examples.advancedcrysondiary.initialization;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
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
    Context context = new Context(server, "/", Context.SESSIONS);
    jettySpringHelper.addFileServlet(context, "/*", fileRootPath);
    jettySpringHelper.addJerseyServlet(context, "/cryson/*", CrysonFrontendService.class.getPackage().getName());
    jettySpringHelper.addJerseyServlet(context, "/services/*", DiaryService.class.getPackage().getName());
    jettySpringHelper.addSecurityFilter(context, "/*");

    server.start();
  }

}
