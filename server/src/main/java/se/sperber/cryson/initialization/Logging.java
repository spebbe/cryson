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

package se.sperber.cryson.initialization;

import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.logging.Handler;
import java.util.logging.LogManager;

@Component
@Profile("cryson_logging")
public class Logging {

	@PostConstruct
	public void routeJULtoSLF4J() {
		Handler[] julHandlers = LogManager.getLogManager().getLogger("").getHandlers();
		for(int ix = 0;ix < julHandlers.length;ix++) {
			Handler handler = julHandlers[ix];
			LogManager.getLogManager().getLogger("").removeHandler(handler);
		}
		SLF4JBridgeHandler.install();
	}

}
