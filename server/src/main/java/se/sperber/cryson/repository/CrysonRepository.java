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

package se.sperber.cryson.repository;

import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.DistinctRootEntityResultTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.stereotype.Repository;
import se.sperber.cryson.exception.CrysonValidationFailedException;
import se.sperber.cryson.security.Restrictable;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import javax.ws.rs.core.MultivaluedMap;
import java.util.List;
import java.util.Set;

@Repository
public class CrysonRepository {

  @Autowired
  private SessionFactory sessionFactory;

  @Value("${cryson.validation.enabled}")
  private boolean validationsEnabled;

  private ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();

  @PostAuthorize("hasPermission(returnObject, 'read')")
  public Object findById(String entityClassName, Long id, Set<String> associationsToFetch) {
    Criteria criteria = sessionFactory.getCurrentSession()
            .createCriteria(entityClassName)
            .add(Restrictions.eq("id", id))
            .setResultTransformer(DistinctRootEntityResultTransformer.INSTANCE)
            .setCacheable(true);

    setFetchModeForAssociations(criteria, associationsToFetch);

    return criteria.uniqueResult();
  }

  @PostFilter("hasPermission(filterObject, 'read')")
  public List findByIds(String entityClassName, List<Long> ids, Set<String> associationsToFetch) {
    Criteria criteria = sessionFactory.getCurrentSession()
            .createCriteria(entityClassName)
            .add(Restrictions.in("id", ids))
            .setResultTransformer(DistinctRootEntityResultTransformer.INSTANCE)
            .setCacheable(true);

    setFetchModeForAssociations(criteria, associationsToFetch);

    return criteria.list();
  }

  @PostFilter("hasPermission(filterObject, 'read')")
  public List findByExample(String entityClassName, Object exampleEntity, Set<String> associationsToFetch) {
    Criteria criteria = sessionFactory.getCurrentSession()
            .createCriteria(entityClassName)
            .add(Example.create(exampleEntity).enableLike(MatchMode.EXACT))
            .setResultTransformer(DistinctRootEntityResultTransformer.INSTANCE)
            .setCacheable(true);

    setFetchModeForAssociations(criteria, associationsToFetch);

    return criteria.list();
  }

  @PostFilter("hasPermission(filterObject, 'read')")
  public List findAll(String entityClassName, Set<String> associationsToFetch) {
    Criteria criteria = sessionFactory.getCurrentSession()
            .createCriteria(entityClassName)
            .setResultTransformer(DistinctRootEntityResultTransformer.INSTANCE)
            .setCacheable(true);

    setFetchModeForAssociations(criteria, associationsToFetch);

    return criteria.list();
  }

  @PostFilter("hasPermission(filterObject, 'read')")
  public List<Object> findByNamedQuery(String queryName, MultivaluedMap<String,String> queryParameters) {
    Query query = sessionFactory.getCurrentSession().getNamedQuery(queryName)
            .setResultTransformer(DistinctRootEntityResultTransformer.INSTANCE)
            .setCacheable(true);

    for(String parameterName : queryParameters.keySet()) {
      query.setParameter(parameterName, queryParameters.getFirst(parameterName));
    }

    return query.list();
  }

  @PostAuthorize("hasPermission(#entity, 'write')")
  public void persist(Object entity) {
    if (validationsEnabled) {
      throwConstraintViolations(validatorFactory.getValidator().validate(entity));
    }
    sessionFactory.getCurrentSession().save(entity);
    sessionFactory.getCurrentSession().flush();
    if (entity instanceof Restrictable) {
      sessionFactory.getCurrentSession().refresh(entity);
    }
  }

  @PostAuthorize("hasPermission(#entity, 'read')")
  public void refresh(Object entity) {
    sessionFactory.getCurrentSession().refresh(entity);
  }

  @PostAuthorize("hasPermission(#entity, 'write')")
  public void update(Object entity) {
    if (validationsEnabled) {
      throwConstraintViolations(validatorFactory.getValidator().validate(entity));
    }
    sessionFactory.getCurrentSession().update(entity);
    if (entity instanceof Restrictable) {
      sessionFactory.getCurrentSession().flush();
      sessionFactory.getCurrentSession().refresh(entity);
    }
  }

  @PostAuthorize("hasPermission(#entity, 'write')")
  public void delete(Object entity) {
    sessionFactory.getCurrentSession().delete(entity); // TODO: Authorization will be performed with placeholder associations...
  }

  @PostAuthorize("hasPermission(returnObject, 'read') and hasPermission(returnObject, 'write')")
  public Object ensureReadableAndWritable(String entityClassName, long id) {
    Criteria criteria = sessionFactory.getCurrentSession()
            .createCriteria(entityClassName)
            .add(Restrictions.eq("id", id))
            .setResultTransformer(DistinctRootEntityResultTransformer.INSTANCE)
            .setCacheable(true);

    return criteria.uniqueResult();
  }

  private void throwConstraintViolations(Set<ConstraintViolation<Object>> constraintViolations) {
    if (constraintViolations.size() > 0) {
      StringBuilder violationMessages = new StringBuilder();
      for(ConstraintViolation<Object> constraintViolation : constraintViolations) {
        violationMessages.append(constraintViolation.getRootBeanClass().getSimpleName()).append(" ");
        violationMessages.append(constraintViolation.getPropertyPath()).append(" ");
        violationMessages.append(constraintViolation.getMessage()).append("\n");
      }
      throw new CrysonValidationFailedException(violationMessages.toString(), constraintViolations);
    }
  }

  private void setFetchModeForAssociations(Criteria criteria, Set<String> associationsToFetch) {
    for(String associationToFetch : associationsToFetch) {
      criteria.setFetchMode(associationToFetch, FetchMode.JOIN);
    }
  }

}
