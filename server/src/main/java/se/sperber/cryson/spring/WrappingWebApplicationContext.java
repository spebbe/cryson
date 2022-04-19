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
  public void publishEvent(Object event) {
    wrappedContext.publishEvent(event);
  }

  @Override
  public BeanFactory getParentBeanFactory() {
    return wrappedContext.getParentBeanFactory();
  }

  @Override
  public boolean containsLocalBean(String name) {
    return wrappedContext.containsLocalBean(name);
  }

  @Override
  public boolean containsBeanDefinition(String beanName) {
    return wrappedContext.containsBeanDefinition(beanName);
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
  public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType, boolean allowEagerInit) {
    return wrappedContext.getBeanProvider(requiredType, allowEagerInit);
  }

  @Override
  public <T> ObjectProvider<T> getBeanProvider(ResolvableType resolvableType, boolean allowEagerInit) {
    return wrappedContext.getBeanProvider(resolvableType, allowEagerInit);
  }

  @Override
  public String[] getBeanNamesForType(ResolvableType resolvableType) {
    return wrappedContext.getBeanNamesForType(resolvableType);
  }

  @Override
  public String[] getBeanNamesForType(ResolvableType resolvableType, boolean includeNonSingletons, boolean allowEagerInit) {
    return wrappedContext.getBeanNamesForType(resolvableType, includeNonSingletons, allowEagerInit);
  }

  @Override
  public String[] getBeanNamesForType(Class<?> type) {
    return wrappedContext.getBeanNamesForType(type);
  }

  @Override
  public String[] getBeanNamesForType(Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
    return wrappedContext.getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
  }

  @Override
  public <T> Map<String, T> getBeansOfType(Class<T> type) throws BeansException {
    return wrappedContext.getBeansOfType(type);
  }

  @Override
  public <T> Map<String, T> getBeansOfType(Class<T> type, boolean includeNonSingletons, boolean allowEagerInit) throws BeansException {
    return wrappedContext.getBeansOfType(type, includeNonSingletons, allowEagerInit);
  }

  @Override
  public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
    return wrappedContext.getBeanNamesForAnnotation(annotationType);
  }

  @Override
  public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) throws BeansException {
    return wrappedContext.getBeansWithAnnotation(annotationType);
  }

  @Override
  public <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType) throws NoSuchBeanDefinitionException {
    return wrappedContext.findAnnotationOnBean(beanName, annotationType);
  }

  @Override
  public <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {
    return wrappedContext.findAnnotationOnBean(beanName, annotationType, allowFactoryBeanInit);
  }

  @Override
  public Object getBean(String name) throws BeansException {
    return wrappedContext.getBean(name);
  }

  @Override
  public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
    return wrappedContext.getBean(name, requiredType);
  }

  @Override
  public <T> T getBean(Class<T> type) throws BeansException {
    return getBean(type);
  }

  @Override
  public Object getBean(String name, Object... args) throws BeansException {
    return wrappedContext.getBean(name, args);
  }

  @Override
  public <T> T getBean(Class<T> requiredType, Object... args) throws BeansException {
    return wrappedContext.getBean(requiredType, args);
  }

  @Override
  public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType) {
    return wrappedContext.getBeanProvider(requiredType);
  }

  @Override
  public <T> ObjectProvider<T> getBeanProvider(ResolvableType resolvableType) {
    return wrappedContext.getBeanProvider(resolvableType);
  }

  @Override
  public boolean containsBean(String name) {
    return wrappedContext.containsBean(name);
  }

  @Override
  public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
    return wrappedContext.isSingleton(name);
  }

  @Override
  public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
    return wrappedContext.isPrototype(name);
  }

  @Override
  public boolean isTypeMatch(String name, ResolvableType resolvableType) throws NoSuchBeanDefinitionException {
    return wrappedContext.isTypeMatch(name, resolvableType);
  }

  @Override
  public boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
    return wrappedContext.isTypeMatch(name, typeToMatch);
  }

  @Override
  public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
    return wrappedContext.getType(name);
  }

  @Override
  public Class<?> getType(String name, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {
    return wrappedContext.getType(name, allowFactoryBeanInit);
  }

  @Override
  public String[] getAliases(String name) {
    return wrappedContext.getAliases(name);
  }

  @Override
  public Environment getEnvironment() {
    return wrappedContext.getEnvironment();
  }

  @Override
  public String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
    return wrappedContext.getMessage(code, args, defaultMessage, locale);
  }

  @Override
  public String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException {
    return wrappedContext.getMessage(code, args, locale);
  }

  @Override
  public String getMessage(MessageSourceResolvable messageSourceResolvable, Locale locale) throws NoSuchMessageException {
    return wrappedContext.getMessage(messageSourceResolvable, locale);
  }

  @Override
  public Resource[] getResources(String locationPattern) throws IOException {
    return wrappedContext.getResources(locationPattern);
  }

  @Override
  public Resource getResource(String location) {
    return wrappedContext.getResource(location);
  }

  @Override
  public ClassLoader getClassLoader() {
    return wrappedContext.getClassLoader();
  }
}
