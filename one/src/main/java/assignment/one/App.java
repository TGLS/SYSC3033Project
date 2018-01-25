package assignment.one;

import java.net.InetAddress;
import java.util.Scanner;

/**
 * This short class has two functions:
 * 1) Determine whether the program has been called for Client, Server or Intermediate
 * 2) Make the correct method calls for each 
 */
public class App 
{
	InetAddress address;
    public static void main( String[] args )
    {
    	Scanner s = new Scanner(System.in);
    	System.out.println("Would you like to run the server, intermediate, or client");
    	String input = s.nextLine();
 
    	
    	try {
    		if (input.equals("server")) {
        	
           		System.out.println("Which recieve port would you like to use?");
        		int sourcePort = s.nextInt();
        		new Server(sourcePort);
        		
        	
        	} else if (input.equals("intermediate")) {
        		System.out.println("Specify the ip address of the server.");
        		String serverDestinationIP = s.nextLine();
        		System.out.println("Which source port would you like to use?");
        		int sourcePort = s.nextInt();
        		System.out.println("Which destination port would you like to use?");
        		int destinationPort = s.nextInt();
        		new Intermediate(sourcePort, serverDestinationIP, destinationPort).loop();
        	} else if (input.equals("client")) {
        		System.out.println("Specify the ip address of the intermediate.");
        		String destinationIP = s.nextLine();
        		System.out.println("Which destination port would you like to use?");
        		int destinationPort = s.nextInt();
        		Client c = new Client(destinationIP, destinationPort);
        		
        		Client c2 = new Client(destinationIP, destinationPort);
        		
        		
        		
        		c.send(false, "Thompson1", "Octet");
        		c2.send(false, "Thompson2", "Octet");
        		
        		c.send(true, "sKLFasjflksajf1", "ocTET"); 
        		c2.send(true, "sKLFasjflksajf2", "ocTET"); 
        		
        		c.send(false, "Delicious1", "Netascii");
        		c2.send(false, "Delicious2", "Netascii");
        		
        		c.send(true, "🍡AMA🍡AMA🍡1", "netASCII");
        		c2.send(true, "🍡AMA🍡AMA🍡2", "netASCII");
        		
        		c.send(false, "A miserible pile of secrets1", "netAscii");
        		c2.send(false, "A miserible pile of secrets2", "netAscii");
        		
        		c.send(true, "Germany1", "NetAscii");
        		c2.send(true, "Germany2", "NetAscii");
        		
        		c.send(false, "irreg1672.jpg1", "oCtEt");
        		c2.send(false, "irreg1672.jpg2", "oCtEt");
        		
        		c.send(true, "darths1602.jpg1", "OcTeT");
        		c2.send(true, "darths1602.jpg2", "OcTeT");
        		
        		c.send(false, "Windows.iso1", "Netascii");
        		c2.send(false, "Windows.iso2", "Netascii");
        		
        		c.send(true, "d3d9.dll1", "netASCII");
        		c2.send(true, "d3d9.dll2", "netASCII");
        		/*c.send(true, "This file name is almost certainly, perhaps with over 99% percent odds,"
        				+ "far too long to be accepted by the server,"
        				+ "because it will randomly be truncated part way through", "netASCII");
        		*/
        	} else {
        		// If we get here, we have a bad argument.
        		incorrectArgumentMessage();
        	}
    	} catch (ArrayIndexOutOfBoundsException e) {
    		incorrectArgumentMessage();
    	}
    	s.close();
    }
    
    private static void incorrectArgumentMessage() {
    	System.out.println("You must provide arguments of one of the following forms:");
    	System.out.println("server [source port]");
    	System.out.println("client [destination ip] [destination port]");
    	System.out.println("intermediate [source port] [destination ip] [destination port]");
    	System.out.println("[Note that the destination ip and port must be the ip");
    	System.out.println("and port of a server or intermediate process.]");
    }
}