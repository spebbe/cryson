package se.sperber.cryson.service;


import com.sun.jersey.api.container.MappableContainerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Service
@Provider
public class CrysonAccessDeniedExceptionMapper implements ExceptionMapper<AccessDeniedException> {

  private static final Logger LOGGER = LoggerFactory.getLogger(CrysonAccessDeniedExceptionMapper.class);

  @Override
  public Response toResponse(AccessDeniedException e) {

    LOGGER.debug("Spring security threw a "+e.getClass().getName(), e);
    throw new MappableContainerException(e);
  }
}
