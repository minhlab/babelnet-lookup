package spinoza.blookup;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.eclipse.jetty.server.Server;

public class BabelNetServer {

    private static final int DEFAULT_PORT = 9000;
    
    private Server httpServer;
    
    public BabelNetServer(int port) throws Exception {
        httpServer = new Server(port);
        httpServer.setHandler(new BabelNetRequestHandler());
    }

    public void start() throws Exception {
        httpServer.start();
    }

    public void stop() throws Exception {
        httpServer.stop();
    }

    public void join() throws InterruptedException {
        httpServer.join();
    }

    public static void main(String[] args) throws Exception
    {
        CommandLine cmd = parseOptions(args);
        int port = DEFAULT_PORT;
        if (cmd.hasOption("p")) {
            port = Integer.parseInt(cmd.getOptionValue("p"));
        }
        BabelNetServer server = new BabelNetServer(port);
        server.start();
        server.join();
    }
    
    private static CommandLine parseOptions(String[] args) {
        Options options = new Options();
        options.addOption("p", "port", true, "Port to listen to");
        try {
            return new PosixParser().parse(options, args);
        } catch (ParseException e) {
            new HelpFormatter().printHelp("server [options]", options);
            System.exit(1);
            return null;
        }
    }

}
