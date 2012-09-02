package se.sperber.cryson.examples.advancedcrysondiary.initialization;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;
import se.sperber.cryson.CrysonServer;

import java.io.IOException;

public class Spring {

  private static AnnotationConfigApplicationContext context = null;

  public static void initialize() throws IOException {
    context = new AnnotationConfigApplicationContext();
    ConfigurableEnvironment environment = context.getEnvironment();
    environment.addActiveProfile("cryson_logging");
    PropertyPlaceholderConfigurer configurer = new PropertyPlaceholderConfigurer();
    configurer.setLocations(new Resource[]{context.getResource("advancedcrysondiary.properties"), context.getResource("cryson.properties")});
    context.addBeanFactoryPostProcessor(configurer);
    context.scan(Spring.class.getPackage().getName(), CrysonServer.class.getPackage().getName());
    context.refresh();
  }

}
