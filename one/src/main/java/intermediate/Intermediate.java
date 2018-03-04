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
				System.out.println("You have entered Verbose Mode");
				
				
				
			//if quiet is entered, exit verbose mode.
			}else if(commandIn.equals("quiet")) {
				IntermediateControl.verboseMode = false;
				System.out.println("You have entered quiet Mode");
				
				
			// here we wan to be able to enter error modes	
			//format of command:  mode [00][01][02][03][04]	Packet_type packet_number;
			}else if(commandIn.contains("mode")){
				// now we need to validate the string format modes 
				
				String mode = "";
				String packetType =""; 
				int packetNumber ;
				int specification; 
				
				String[] commandParts = commandIn.split(" ");
				//ensure mode is the first item 
				if(!(commandParts[0].equals("mode"))){
					System.out.println("there is an error in your mode formating please try again mode must be at the fron of the command");
					// print the proper formatting 
				}else {
				//ensure that packet type is valid
					if(commandParts[1].equals("00")|commandParts[1].equals("01")|commandParts[1].equals("02")|commandParts[1].equals("03")) {
						mode = commandParts[1]; 
						if(commandParts[2].equals("ack")| commandParts[2].equals("data")|commandParts[2].equals("rrq")|commandParts[2].equals("wrq")){
							packetType = commandParts[2];
							// this will be the packet number  
							if((commandParts[3].matches("[0-9]+"))){
								packetNumber = Integer.parseInt(commandParts[2]); 
								
								// now need to validate the transferData example delay, number of duplications ... ect
								if((commandParts[4].matches("[0-9]+"))){
									specification = Integer.parseInt(commandParts[4]);
									// if we get to here we have a valid imput 
									IntermediateControl.mode = mode; 
									IntermediateControl.packetType = packetType;
									IntermediateControl.packetNumber = packetNumber; 
									IntermediateControl.specification = specification;
									
								}else {
									System.out.println("there is an error in your mode formating please try again ensure your SPECIFICATION is valid");
								}
								
								
							}else {
								System.out.println("there is an error in your mode formating please try again ensure your PACKET_NUMBER is valid");
							}
						}else {
							System.out.println("there is an error in your mode formating please try again ensure your PACKET_TYPE is valid");
						}
					}
				}	
			//Not a valid Command  
			}else {
				System.out.println("Not a Valid Command Please try again");
				
				//print out the valid commands
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