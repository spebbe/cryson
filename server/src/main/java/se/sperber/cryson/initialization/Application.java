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

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

public class Application {

  private static AnnotationConfigApplicationContext context = null;
  
  private static Properties properties = null;

  public static <T> T get(Class<T> type) {
    if (context == null) {
      throw new IllegalStateException("Application not initialized");
    }

    return context.getBean(type);
  }

  public static <T> Collection<T> getAll(Class<T> type) {
    if (context == null) {
      throw new IllegalStateException("Application not initialized");
    }

    return context.getBeansOfType(type).values();
  }

  public static void initialize(String[] basePackages, String[] propertyFiles) throws IOException {
    if (context != null) {
      throw new IllegalStateException("Application already initialized");
    }

    context = new AnnotationConfigApplicationContext();
    resolveProperties(propertyFiles);
    configureProfiles();
    String[] beanPackages = resolveBeanPackages(basePackages);
    resolveBeans(beanPackages);
    context.refresh();
  }

  private static void configureProfiles() {
    ConfigurableEnvironment environment = context.getEnvironment();
    if ("true".equals(getProperty("cryson.httpserver.enabled"))) {
      environment.addActiveProfile("cryson_httpserver");
    }
    if ("true".equals(getProperty("cryson.database.enabled"))) {
      environment.addActiveProfile("cryson_database");
    }
    if ("true".equals(getProperty("cryson.security.enabled"))) {
      environment.addActiveProfile("cryson_security");
    }
    if ("true".equals(getProperty("cryson.logging.enabled"))) {
      environment.addActiveProfile("cryson_logging");
    }
  }

  public static String getProperty(String propertyName) {
    if (properties == null) {
      throw new IllegalStateException("Properties not initialized");
    }

    return properties.getProperty(propertyName);
  }

  public static void shutdown() {
    if (context == null) {
      throw new IllegalStateException("Application not initialized");
    }

    context.close();
    context.stop();
    context.destroy();
    context = null;
  }


  private static String[] resolveBeanPackages(String[] basePackages) {
    List<String> beanPackages = new ArrayList<String>();
    for(int ix = 0;ix < basePackages.length;ix++) {
      beanPackages.add(basePackages[ix]);
    }
    String beanPackagesFromPropertiesString = getProperty("cryson.extra.spring.bean.packages");
    if (beanPackagesFromPropertiesString != null && beanPackagesFromPropertiesString.length() > 0) {
      String[] beanPackagesFromProperties = beanPackagesFromPropertiesString.split(",");
      for(int ix = 0;ix < beanPackagesFromProperties.length;ix++) {
        String beanPackageFromProperties = beanPackagesFromProperties[ix];
        if (beanPackageFromProperties != null && beanPackageFromProperties.length() > 0) {
          beanPackages.add(beanPackagesFromProperties[ix]);
        }
      }
    }
    return beanPackages.toArray(new String[]{});
  }

  private static void resolveProperties(String[] propertyFiles) throws IOException {
    properties = new Properties();
    if ((propertyFiles != null) && (propertyFiles.length > 0)) {
      Resource[] propertyResources = getPropertyResources(propertyFiles);
      
      for(Resource propertyResource : propertyResources) {
        properties.load(propertyResource.getInputStream());
      }
      
      PropertyPlaceholderConfigurer configurer = new PropertyPlaceholderConfigurer();
      configurer.setLocations(propertyResources);
      context.addBeanFactoryPostProcessor(configurer);
    }
  }

  private static Resource[] getPropertyResources(String[] propertyFiles) {
    Resource[] result = new Resource[propertyFiles.length];
    for(int ix = 0;ix < propertyFiles.length;ix++) {
      result[ix] = context.getResource(propertyFiles[ix]);
    }
    return result;
  }

  private static void resolveBeans(String[] basePackages) {
    if ((basePackages != null) && (basePackages.length > 0)) {
      context.scan(basePackages);
    }
  }

  public static AnnotationConfigApplicationContext getContext() {
    return context;
  }
}
