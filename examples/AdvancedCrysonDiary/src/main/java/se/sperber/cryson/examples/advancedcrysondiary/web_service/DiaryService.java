package se.sperber.cryson.examples.advancedcrysondiary.web_service;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Service
@Path("/diary")
@Produces("application/json")
@PreAuthorize("isAuthenticated()")
public class DiaryService {

  @GET
  @Path("login")
  public Response getLoginName() {
    return Response.ok("\"" + SecurityContextHolder.getContext().getAuthentication().getName() + "\"").build();
  }

}
