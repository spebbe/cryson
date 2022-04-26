package se.sperber.cryson.hibernate;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.NativeQuery;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import se.sperber.cryson.CrysonServer;
import se.sperber.cryson.initialization.Application;
import se.sperber.cryson.testutil.CrysonTestEntity;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class CrysonHibernateTest {

  private static CrysonServer crysonServer;

  private static void cleanDatabase() {
    Session session = Application.get(SessionFactory.class).openSession();
    session.beginTransaction();
    session.createSQLQuery("DELETE FROM CrysonTestChildEntity").executeUpdate();
    session.createSQLQuery("DELETE FROM CrysonTestEntity").executeUpdate();
    session.getTransaction().commit();
    session.close();
  }

  @BeforeClass
  public static void setup() throws Exception {
    crysonServer = new CrysonServer();
    crysonServer.init(new String[]{"cryson-test.properties"});
    crysonServer.start();
    cleanDatabase();
  }

  @AfterClass
  public static void teardown() throws Exception {
    crysonServer.stop();
    crysonServer.destroy();
  }

  @Test
  public void shouldUseZeroBasedJDBCStyleParameters() {
    try (Session session = Application.get(SessionFactory.class).openSession()) {
      CrysonTestEntity testEntity = new CrysonTestEntity(1L);
      testEntity.setName("Crydaughter");
      session.save(testEntity);
      NativeQuery query = session.createSQLQuery("SELECT * FROM CrysonTestEntity WHERE name = ?")
          .setParameter(0, "Crydaughter");
      assertThat(query.getParameter(0).getPosition(), is(0));
      assertThat(query.getParameterValue(0), is("Crydaughter"));
    }
  }
}
