
package alda;

import java.util.Scanner;

public class AldaRepl {

  private AldaServer server;

  private Scanner s;

  public AldaRepl(AldaServer server) {
    this.server = server;
    s = new Scanner(System.in);
  }

  public void run() {
    while (true) {
      System.out.print("> ");
      System.out.flush();
      String input = s.nextLine();
      System.out.println(input);
      try {
        server.play(input, "(tempo! 500) piano: c d", null, null);
      } catch (Throwable e) {
        server.error(e.getMessage());
        // if (globalOpts.verbose) {
        //   System.out.println();
        //   e.printStackTrace();
        // }
        System.exit(1);
      }

    }
  }
}
