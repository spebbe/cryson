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

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;
import se.sperber.cryson.initialization.Application;

public class CrysonDelegatingFilterProxy extends DelegatingFilterProxy {

  private DefaultListableBeanFactory defaultListableBeanFactory;

  public CrysonDelegatingFilterProxy() {
    super();
    defaultListableBeanFactory = Application.getContext().getDefaultListableBeanFactory();
  }

  public CrysonDelegatingFilterProxy(DefaultListableBeanFactory defaultListableBeanFactory) {
    super();
    this.defaultListableBeanFactory = defaultListableBeanFactory;
  }

  @Override
  protected WebApplicationContext findWebApplicationContext() {
    return new GenericWebApplicationContext(defaultListableBeanFactory);
  }

  @Override
  protected String getTargetBeanName() {
    return "springSecurityFilterChain";
  }

}