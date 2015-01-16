package se.sperber.cryson.repository;

import org.hamcrest.Matcher;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Criterion;
import org.hibernate.transform.ResultTransformer;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import se.sperber.cryson.serialization.ReflectionHelper;
import se.sperber.cryson.serialization.UnauthorizedEntity;
import se.sperber.cryson.testutil.CrysonTestEntity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class CrysonRepositoryTest {

  public static class WhenRequestingMultipleRestrictedEntities {

    @Test
    public void shouldIncludeUnauthorizedEntities() {
      CrysonTestEntity authorizedEntity = new CrysonTestEntity(1L);
      CrysonTestEntity unauthorizedEntity = new CrysonTestEntity(2L);
      unauthorizedEntity.setShouldBeReadable(false);

      Criteria criteria = givenCriteria();
      when(criteria.list()).thenReturn(Arrays.asList(authorizedEntity, unauthorizedEntity));

      CrysonRepository crysonRepository = givenCrysonRepositoryWithCriteriaAndEntities(criteria, authorizedEntity, unauthorizedEntity);

      List expectedResult = Arrays.asList(authorizedEntity, new UnauthorizedEntity("CrysonTestEntity", 2L));
      assertThat(crysonRepository.findByIds(CrysonTestEntity.class.getName(), Arrays.asList(1L, 2L), new HashSet<String>()), is(expectedResult));
    }

  }

  public static class WhenRequestingASingleRestrictedEntity {

    @Test
    public void shouldReturnAnUnauthorizedEntity() {
      CrysonTestEntity unauthorizedEntity = new CrysonTestEntity(1L);
      unauthorizedEntity.setShouldBeReadable(false);

      Criteria criteria = givenCriteria();
      when(criteria.uniqueResult()).thenReturn(unauthorizedEntity);

      CrysonRepository crysonRepository = givenCrysonRepositoryWithCriteriaAndEntities(criteria, unauthorizedEntity);

      Object expectedResult = new UnauthorizedEntity("CrysonTestEntity", 1L);
      assertThat(crysonRepository.findById(CrysonTestEntity.class.getName(), 1L, new HashSet<String>()), is(expectedResult));
    }

    @Test
    public void shouldReturnAnEntity() {
      CrysonTestEntity authorizedEntity = new CrysonTestEntity(1L);
      authorizedEntity.setShouldBeReadable(true);

      Criteria criteria = givenCriteria();
      when(criteria.uniqueResult()).thenReturn(authorizedEntity);

      CrysonRepository crysonRepository = givenCrysonRepositoryWithCriteriaAndEntities(criteria, authorizedEntity);

      assertThat(crysonRepository.findById(CrysonTestEntity.class.getName(), 1L, new HashSet<String>()), is((Object)authorizedEntity));
    }

  }

  private static Criteria givenCriteria() {
    Criteria criteria = mock(Criteria.class);
    when(criteria.add(any(Criterion.class))).thenReturn(criteria);
    when(criteria.setResultTransformer(any(ResultTransformer.class))).thenReturn(criteria);
    when(criteria.setCacheable(anyBoolean())).thenReturn(criteria);
    return criteria;
  }

  private static CrysonRepository givenCrysonRepositoryWithCriteriaAndEntities(Criteria criteria, CrysonTestEntity... entities) {
    Session session = mock(Session.class);
    when(session.createCriteria(anyString())).thenReturn(criteria);

    SessionFactory sessionFactory = mock(SessionFactory.class);
    when(sessionFactory.getCurrentSession()).thenReturn(session);

    ReflectionHelper reflectionHelper = mock(ReflectionHelper.class);
    for (CrysonTestEntity entity : entities) {
      when(reflectionHelper.getPrimaryKey(entity)).thenReturn(entity.getId());
    }

    CrysonRepository crysonRepository = new CrysonRepository();
    crysonRepository.setSessionFactory(sessionFactory);
    crysonRepository.setReflectionHelper(reflectionHelper);

    return crysonRepository;
  }

}
