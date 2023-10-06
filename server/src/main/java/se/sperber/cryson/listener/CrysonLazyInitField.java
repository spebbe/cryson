package se.sperber.cryson.listener;

import java.util.Set;

public interface CrysonLazyInitField {
  Set<Long> getPrimaryKeys(String fieldName);
}
