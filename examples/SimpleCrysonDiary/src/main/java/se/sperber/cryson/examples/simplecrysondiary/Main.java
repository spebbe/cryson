package se.sperber.cryson.examples.simplecrysondiary;

import se.sperber.cryson.CrysonServer;

public class Main {

  public static void main(String[] args) throws Throwable {
    CrysonServer crysonServer = new CrysonServer();
    crysonServer.init(args);
    crysonServer.start();
  }

}
