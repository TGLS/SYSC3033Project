package intermediate;

import java.util.Scanner;


public class Intermediate {
	private Scanner s ;
	private Thread errorSimRecieveThread;
	private String shutDown = "shutdown"; 
	private String verbose = "verbose";
	
	
	public Intermediate(int sourcePort, String destinationIP, int destinationPort, boolean verbose) {
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
			
			//if shut down is entered, signal the shutdown.
			if(commandIn.equals(shutDown)) {
				IntermediateControl.IntermediateStop = true;
				System.out.println("The Shut Down will now commence");
				break;
				
			//if verbose is entered, signal verbose mode.
			}else if(commandIn.equals(verbose)) {
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
			
			//Wait for the receive thread to complete before exiting this 
			//ensures that all threads will be complete before we finish.
			try {
				errorSimRecieveThread.join();
			} catch (InterruptedException e) {
				System.out.println("There was an error shutting down the threads");
				e.printStackTrace();
				System.exit(1);
			}
			
			//Notify the user of the completion
			System.out.println("All the threads have completed goodbye ... ");
			
			//terminate the program
			System.exit(0);
			
	}
	

	
}