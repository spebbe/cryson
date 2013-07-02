package se.sperber.cryson.serialization;

public class UnauthorizedEntity {

  private Boolean crysonUnauthorized = true;

  private final String crysonEntityClass;

  private final Long id;

  public UnauthorizedEntity(String crysonEntityClass, Long id) {
    this.crysonEntityClass = crysonEntityClass;
    this.id = id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    UnauthorizedEntity that = (UnauthorizedEntity) o;

    if (crysonUnauthorized != null ? !crysonUnauthorized.equals(that.crysonUnauthorized) : that.crysonUnauthorized != null)
      return false;
    if (crysonEntityClass != null ? !crysonEntityClass.equals(that.crysonEntityClass) : that.crysonEntityClass != null) return false;
    if (id != null ? !id.equals(that.id) : that.id != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = crysonUnauthorized != null ? crysonUnauthorized.hashCode() : 0;
    result = 31 * result + (crysonEntityClass != null ? crysonEntityClass.hashCode() : 0);
    result = 31 * result + (id != null ? id.hashCode() : 0);
    return result;
  }
}
