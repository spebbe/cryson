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

package se.sperber.cryson.spring;

import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.api.model.AbstractResource;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.core.spi.component.ioc.IoCComponentProvider;
import com.sun.jersey.server.spi.component.ResourceComponentProvider;
import com.sun.jersey.server.spi.component.ResourceComponentProviderFactory;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.DefaultServlet;
import org.mortbay.jetty.servlet.FilterHolder;
import org.mortbay.jetty.servlet.ServletHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class JettySpringHelper {

  @Autowired
  private DefaultListableBeanFactory defaultListableBeanFactory;

  @PostConstruct
  public void initialize() {
    SpringComponentProviderFactory.defaultListableBeanFactory = defaultListableBeanFactory;
  }

  public void addSecurityFilter(Context context, String pathSpec) {
    FilterHolder securityFilterHolder = new FilterHolder(new CrysonDelegatingFilterProxy(defaultListableBeanFactory));
    context.addFilter(securityFilterHolder, pathSpec, Handler.REQUEST | Handler.FORWARD | Handler.INCLUDE);
  }

  public void addJerseyServlet(Context context, String pathSpec, String packageName) {
    ServletHolder servletHolder = new ServletHolder(ServletContainer.class);
    servletHolder.setInitParameter(ServletContainer.RESOURCE_CONFIG_CLASS, PackagesResourceConfig.class.getName());
    servletHolder.setInitParameter(PackagesResourceConfig.PROPERTY_PACKAGES, packageName);
    servletHolder.setInitParameter(ResourceConfig.PROPERTY_DEFAULT_RESOURCE_COMPONENT_PROVIDER_FACTORY_CLASS,
            SpringComponentProviderFactory.class.getName());
    context.addServlet(servletHolder, pathSpec);
  }

  public void addFileServlet(Context context, String pathSpec, String fileRootPath) {
    ServletHolder servletHolder = new ServletHolder(DefaultServlet.class);
    servletHolder.setInitParameter("resourceBase", fileRootPath);
    servletHolder.setInitParameter("aliases", "true");
    context.addServlet(servletHolder, pathSpec);
  }

  // Silly glue for feeding jersey with springletons
  public static class SpringComponentProviderFactory implements ResourceComponentProviderFactory {

    public static DefaultListableBeanFactory defaultListableBeanFactory;

    public ResourceComponentProvider getComponentProvider(final Class<?> c) {
      return new ResourceComponentProvider() {

        public Object getInstance() {
          return defaultListableBeanFactory.getBean(c);
        }

        public void init(AbstractResource abstractResource) {}

        public ComponentScope getScope() {
          return ComponentScope.Singleton;
        }

        public Object getInstance(HttpContext hc) {
          return defaultListableBeanFactory.getBean(c);
        }

        public void destroy() {}

      };
    }

    public ComponentScope getScope(@SuppressWarnings("rawtypes") Class c) {
      return ComponentScope.Singleton;
    }

    public ResourceComponentProvider getComponentProvider(
            IoCComponentProvider icp, Class<?> c) {
      throw new UnsupportedOperationException();
    }

  }

}
