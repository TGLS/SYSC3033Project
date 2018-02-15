package intermediate;

import java.util.Scanner;

/**
 * This short class has two functions:
 * 1) Start up the CLI for the Intermediate.
 * 2) Start the Intermediate Receive Thread. 
 */

public class Intermediate {
	private Scanner s ;
	private Thread errorSimRecieveThread;
	
	
	public Intermediate(int sourcePort, String destinationIP, int destinationPort, Boolean verbose) {
		// Create a DatagramSocket for reception with the port number you were given.
		// Surrounded with try-catch because creating a new socket might fail.
		
		IntermediateControl.IntermediateStop = false;
		IntermediateControl.verboseMode = verbose;
		
		//First start the server receive thread  
		errorSimRecieveThread = new Thread(new ErrorSimRecieveThread(sourcePort,destinationIP,destinationPort),"ErrorSimRecieveThread");
		errorSimRecieveThread.start(); 
		
		s= new Scanner(System.in);
		
		//Run The Servers Command Line interface
		intermediateInterface();
		//Shutdown after the interface closes
		shutdown();
		
		
	}
	

	
	
	

	private void intermediateInterface() {
		//This method runs the Servers Command Line interface 
		
		//Create a scanner to take input from the user
		
		
		//run the interface until the shutdown command is received
		while (true) {
			System.out.println("Please Enter Command >: ");
			String commandIn = s.nextLine();
			commandIn.replaceAll("\\s", "");
			
			commandIn = commandIn.toLowerCase();
			
			//if shut down is entered, signal the shutdown.
			if(commandIn.equals("shutdown")) {
				IntermediateControl.IntermediateStop = true;
				System.out.println("The Shut Down will now commence");
				break;
				
			//if verbose is entered, signal verbose mode.
			}else if(commandIn.equals("verbose")) {
				IntermediateControl.verboseMode = true;
				
			//if quiet is entered, exit verbose mode.
			}else if(commandIn.equals("quiet")) {
				IntermediateControl.verboseMode = false;
				
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