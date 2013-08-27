package se.sperber.cryson.service.exceptionmappers;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.apache.log4j.Logger;

import se.sperber.cryson.exception.CrysonException;
import se.sperber.cryson.initialization.Application;
import se.sperber.cryson.serialization.CrysonSerializer;

public abstract class CrysonExceptionMapperBase<E extends Throwable> implements ExceptionMapper<E> {

  protected static final Logger logger = Logger.getLogger(CrysonExceptionMapperBase.class);

  private static CrysonSerializer crysonSerializer = Application.get(CrysonSerializer.class);

  @Override
  public Response toResponse(E e) {
    logger.error("Error", e);
    return toResponseInternal(e);
  }
  
  protected abstract Response toResponseInternal(E e);
  
  protected Response translateCrysonException(CrysonException e) {
    String serializedMessage = crysonSerializer.serializeWithoutAugmentation(e.getSerializableMessage());
    return Response.status(e.getStatusCode()).entity(serializedMessage).build();
  }
  
  protected String buildJsonMessage(String message) {
    Map<String, Serializable> messageObject = new HashMap<String, Serializable>();
    if (message == null || message.equals("")) {
      messageObject.put("message", "Unclassified error");
    }
    else {
      messageObject.put("message", message);
    }
    return crysonSerializer.serializeWithoutAugmentation(messageObject);
  }
}
