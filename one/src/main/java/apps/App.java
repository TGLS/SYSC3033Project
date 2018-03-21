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
	public static void main( String[] args )
    {
    	Boolean verbose = false;
    	String outgoingIP = "127.0.0.1";
    	int incomingPort = -1;
    	int outgoingPort = -1;
    	
    	// Incoming Argument Handling
    	for (String s : args) {
    		s = s.toLowerCase();
    		if ((s.equals("-v")) | (s.equals("-verbose"))) {
    			verbose = true;
    		}
    		
    		if ((s.equals("-q")) | (s.equals("-quiet"))) {
    			verbose = false;
    		}
    		
    		if (s.startsWith("-ip=")) {
    			outgoingIP = s.split("=")[1];
    		}
    		
    		if (s.startsWith("-out_port=")) {
    			outgoingPort = Integer.parseInt(s.split("=")[1]);
    		}
    		
    		if (s.startsWith("-in_port=")) {
    			incomingPort = Integer.parseInt(s.split("=")[1]);
    		}
    	}
    	try {
    		if (args[0].equals("server")) {
    			if (incomingPort == -1) {
    				incomingPort = 69;
    			}
        		Server s = new Server(incomingPort, verbose);
        	
        	} else if (args[0].equals("intermediate")) {
        		if (incomingPort == -1) {
    				incomingPort = 23;
    			}
        		if (outgoingPort == -1) {
        			outgoingPort = 69;
    			}
        		Intermediate i = new Intermediate(incomingPort, outgoingIP, outgoingPort, verbose);
        	} else if (args[0].equals("client")) {
        		if (outgoingPort == -1) {
        			outgoingPort = 23;
    			}
        		Client c = new Client(outgoingIP, outgoingPort, verbose);
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
    	System.out.println("You must provide an argument of one of the following forms:");
    	System.out.println("server");
    	System.out.println("client");
    	System.out.println("intermediate");
    	System.out.println("You may provide any number of the following options afterwards:");
    	System.out.println(" -v, -verbose: Enables verbose output.");
    	System.out.println(" -q, -quiet: Disables verbose output (default).");
    	System.out.println(" -in_port=[port]: Sets the port incoming messages will be received by. Default for server is 69, for intermeditate is 23.");
    	System.out.println(" -out_port=[port]: Sets the port outgoing messages will be sent to. Default for client is 23, for intermeditate is 69.");
    	System.out.println(" -ip=[ip]: Sets the ip outgoing messages will be sent to. Default for client and intermediate is 127.0.0.1.");
    	System.out.println("Later options are take precedence over earlier options.");
    }
}