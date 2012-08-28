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

package se.sperber.cryson;

import se.sperber.cryson.initialization.Application;
import se.sperber.cryson.initialization.HttpServer;

import java.io.IOException;

public class CrysonServer {

  private static final String[] BASE_PACKAGES = new String[]{"se.sperber.cryson"};
  private static final String[] DEFAULT_PROPERTY_FILES = new String[]{"cryson.properties"};

  public void join() throws Exception {
    Application.get(HttpServer.class).joinServer();
  }

  // for jsvc/commons-daemon

  public void init(String[] args) throws IOException {
    String[] propertyFiles;
    if (args.length > 0) {
      propertyFiles = new String[args.length];
      for (int i=0; i<args.length; i++) {
        propertyFiles[i] = args[i];
      }
    } else {
      propertyFiles = DEFAULT_PROPERTY_FILES;
    }
    Application.initialize(BASE_PACKAGES, propertyFiles);
  }

  public void start() throws Exception {
    Application.get(HttpServer.class).startServer();
  }

  public void stop() throws Exception {
    Application.get(HttpServer.class).stopServer();
  }

  public void destroy() {
    Application.shutdown();
  }

}
