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

package se.sperber.cryson.exception;

import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class CrysonException extends RuntimeException {

  public CrysonException(String message, Throwable cause) {
    super(message, cause);
  }

  public Map<String, Serializable> getSerializableMessage() {
    Map<String, Serializable> message = new HashMap<String, Serializable>();
    message.put("message", getMessage());
    return message;
  }

  public int getStatusCode() {
    return Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
  }

}
