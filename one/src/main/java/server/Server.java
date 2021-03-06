package server;

import java.util.Scanner;

/**
 * This short class has two functions:
 * 1) Start up the CLI for the Server.
 * 2) Start the Server Receive Thread. 
 */

public class Server {

	private Thread recieveThread;
	private Scanner s ;
	
	public Server(int sourcePort, Boolean verbose) {
		// This method will start the server receive thread and then proceed to 
		// Run the Server CLI: 
		
		//Initialize the control settings
		ServerControl.serverStop = false;
		ServerControl.verboseMode = verbose;
		
		//First start the server receive thread  
		recieveThread = new Thread(new ServerReceiveThread(sourcePort),"recieveThread");
		recieveThread.start(); 
		
		s = new Scanner(System.in);
		
		//Run The Servers Command Line interface
		serverInterface();
		//Shutdown after the interface closes
		shutdown();
		
	}
	
	private void serverInterface() {
		//This method runs the Servers Command Line interface 
		
		//Create a scanner to take input from the user
		
		
		//run the interface until the shutdown command is received
		while (true) {
		
			System.out.println("Please Enter Command >: ");
			String commandIn = s.nextLine();
			commandIn = commandIn.toLowerCase();
			
			commandIn.replaceAll("\\s", "");
			
			//if shut down is entered, signal the shutdown.
			if(commandIn.equals("shutdown")) {
				ServerControl.serverStop = true;
				System.out.println("The Shut Down will now commence");
				System.exit(0);
				
			//if verbose is entered, signal verbose mode.
			}else if(commandIn.equals("verbose")) {
				ServerControl.verboseMode = true;
			
			//if quiet is entered, exit verbose mode
			}else if(commandIn.equals("quiet")) {
				ServerControl.verboseMode = false;
				
			//Not a valid Command  
			}else {
				System.out.println("Not a Valid Command Please try again");
			}
	
			
		}
		
	}
	
	
	private void shutdown() {
		
		//close the scanner 
		s.close();
		
		//terminate the program
		System.exit(0);
		
	}
	
	
}