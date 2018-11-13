package se.sperber.cryson.spring;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Locale;
import java.util.Map;

public class WrappingWebApplicationContext implements WebApplicationContext {

  private final ServletContext servletContext;
  private final ApplicationContext wrappedContext;

  public WrappingWebApplicationContext(ServletContext servletContext, ApplicationContext wrappedContext) {
    this.servletContext = servletContext;
    this.wrappedContext = wrappedContext;
  }

  @Override
  public ServletContext getServletContext() {
    return servletContext;
  }

  @Override
  public String getId() {
    return wrappedContext.getId();
  }

  @Override
  public String getApplicationName() {
    return wrappedContext.getApplicationName();
  }

  @Override
  public String getDisplayName() {
    return wrappedContext.getDisplayName();
  }

  @Override
  public long getStartupDate() {
    return wrappedContext.getStartupDate();
  }

  @Override
  public ApplicationContext getParent() {
    return wrappedContext.getParent();
  }

  @Override
  public AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
    return wrappedContext.getAutowireCapableBeanFactory();
  }

  @Override
  public void publishEvent(ApplicationEvent applicationEvent) {
    wrappedContext.publishEvent(applicationEvent);
  }

  @Override
  public void publishEvent(Object o) {

  }

  @Override
  public BeanFactory getParentBeanFactory() {
    return wrappedContext.getParentBeanFactory();
  }

  @Override
  public boolean containsLocalBean(String s) {
    return wrappedContext.containsLocalBean(s);
  }

  @Override
  public boolean containsBeanDefinition(String s) {
    return wrappedContext.containsBeanDefinition(s);
  }

  @Override
  public int getBeanDefinitionCount() {
    return wrappedContext.getBeanDefinitionCount();
  }

  @Override
  public String[] getBeanDefinitionNames() {
    return wrappedContext.getBeanDefinitionNames();
  }

  @Override
  public String[] getBeanNamesForType(ResolvableType resolvableType) {
    return new String[0];
  }

  @Override
  public String[] getBeanNamesForType(Class<?> aClass) {
    return wrappedContext.getBeanNamesForType(aClass);
  }

  @Override
  public String[] getBeanNamesForType(Class<?> aClass, boolean b, boolean b1) {
    return wrappedContext.getBeanNamesForType(aClass, b, b1);
  }

  @Override
  public <T> Map<String, T> getBeansOfType(Class<T> aClass) throws BeansException {
    return wrappedContext.getBeansOfType(aClass);
  }

  @Override
  public <T> Map<String, T> getBeansOfType(Class<T> aClass, boolean b, boolean b1) throws BeansException {
    return wrappedContext.getBeansOfType(aClass, b, b1);
  }

  @Override
  public String[] getBeanNamesForAnnotation(Class<? extends Annotation> aClass) {
    return wrappedContext.getBeanNamesForAnnotation(aClass);
  }

  @Override
  public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> aClass) throws BeansException {
    return null;
  }

  @Override
  public <A extends Annotation> A findAnnotationOnBean(String s, Class<A> aClass) throws NoSuchBeanDefinitionException {
    return wrappedContext.findAnnotationOnBean(s, aClass);
  }

  @Override
  public Object getBean(String s) throws BeansException {
    return wrappedContext.getBean(s);
  }

  @Override
  public <T> T getBean(String s, Class<T> aClass) throws BeansException {
    return wrappedContext.getBean(s, aClass);
  }

  @Override
  public <T> T getBean(Class<T> aClass) throws BeansException {
    return getBean(aClass);
  }

  @Override
  public Object getBean(String s, Object... objects) throws BeansException {
    return wrappedContext.getBean(s, objects);
  }

  @Override
  public <T> T getBean(Class<T> aClass, Object... objects) throws BeansException {
    return wrappedContext.getBean(aClass, objects);
  }

  @Override
  public <T> ObjectProvider<T> getBeanProvider(Class<T> aClass) {
    return null;
  }

  @Override
  public <T> ObjectProvider<T> getBeanProvider(ResolvableType resolvableType) {
    return null;
  }

  @Override
  public boolean containsBean(String s) {
    return wrappedContext.containsBean(s);
  }

  @Override
  public boolean isSingleton(String s) throws NoSuchBeanDefinitionException {
    return wrappedContext.isSingleton(s);
  }

  @Override
  public boolean isPrototype(String s) throws NoSuchBeanDefinitionException {
    return wrappedContext.isPrototype(s);
  }

  @Override
  public boolean isTypeMatch(String s, ResolvableType resolvableType) throws NoSuchBeanDefinitionException {
    return false;
  }

  @Override
  public boolean isTypeMatch(String s, Class<?> aClass) throws NoSuchBeanDefinitionException {
    return wrappedContext.isTypeMatch(s, aClass);
  }

  @Override
  public Class<?> getType(String s) throws NoSuchBeanDefinitionException {
    return wrappedContext.getType(s);
  }

  @Override
  public String[] getAliases(String s) {
    return wrappedContext.getAliases(s);
  }

  @Override
  public Environment getEnvironment() {
    return wrappedContext.getEnvironment();
  }

  @Override
  public String getMessage(String s, Object[] objects, String s1, Locale locale) {
    return wrappedContext.getMessage(s, objects, s1, locale);
  }

  @Override
  public String getMessage(String s, Object[] objects, Locale locale) throws NoSuchMessageException {
    return wrappedContext.getMessage(s, objects, locale);
  }

  @Override
  public String getMessage(MessageSourceResolvable messageSourceResolvable, Locale locale) throws NoSuchMessageException {
    return wrappedContext.getMessage(messageSourceResolvable, locale);
  }

  @Override
  public Resource[] getResources(String s) throws IOException {
    return wrappedContext.getResources(s);
  }

  @Override
  public Resource getResource(String s) {
    return wrappedContext.getResource(s);
  }

  @Override
  public ClassLoader getClassLoader() {
    return wrappedContext.getClassLoader();
  }
}
