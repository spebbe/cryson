package se.sperber.cryson.serialization;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import org.springframework.stereotype.Component;
import se.sperber.cryson.annotation.CrysonExclude;

@Component
public class CrysonExcludeExclusionStrategy implements ExclusionStrategy {

  @Override
  public boolean shouldSkipField(FieldAttributes fieldAttributes) {
    return fieldAttributes.getAnnotation(CrysonExclude.class) != null;
  }

  @Override
  public boolean shouldSkipClass(Class<?> aClass) {
    return false;
  }
}
