package apps;

import client.Client;
import intermediate.Intermediate;
import server.Server;
/**
 * This short class has three functions:
 * 1) Determine whether the program has been called for Client, Server or Intermediate
 * 2) Make the correct method calls for each
 * 3) Check options and apply them. 
 */
public class App 
{
	// To coordinate the size of the data buffer used by each component of the system,
	// We're moving the max_buffer constant here.
	public final static int max_buffer = 516;
	
    public static void main( String[] args )
    {
    	boolean verbose = false;
    	//
    	for (String s : args) {
    		if ((s.equals("-v")) | (s.equals("-verbose"))) {
    			verbose = true;
    		}
    		
    		if ((s.equals("-q")) | (s.equals("-quiet"))) {
    			verbose = false;
    		}
    	}
    	try {
    		if (args[0].equals("server")) {
        		Server s = new Server(Integer.parseInt(args[1]));
        	
        	} else if (args[0].equals("intermediate")) {
        		Intermediate i = new Intermediate(Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]));
        		i.loop();
        	} else if (args[0].equals("client")) {
        		Client c = new Client(args[1], Integer.parseInt(args[2]), verbose);
        		c.loop();
        		
        	} else {
        		// If we get here, we have a bad argument.
        		incorrectArgumentMessage();
        	}
    	} catch (ArrayIndexOutOfBoundsException e) {
    		incorrectArgumentMessage();
    	}
    }
    
    private static void incorrectArgumentMessage() {
    	System.out.println("You must provide arguments of one of the following forms:");
    	System.out.println("server [source port]");
    	System.out.println("client [destination ip] [destination port]");
    	System.out.println("intermediate [source port] [destination ip] [destination port]");
    	System.out.println("[Note that the destination ip and port must be the ip");
    	System.out.println("and port of a server or intermediate process.]");
    	System.out.println("You may provide any number of the following options afterwards:");
    	System.out.println(" -v, -verbose: Enables verbose output.");
    	System.out.println(" -q, -quiet: Disables verbose output (default).");
    	System.out.println("Later options are take precedence over earlier options.");
    }
}