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
			printMenue();
			
			System.out.println("Please Enter Command >: ");
			String commandIn = s.nextLine();
			String[] commandParts = commandIn.split(" ");
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
			}else if(commandParts[0].equals("mode") && commandParts.length >=2 ){
				// now we need to validate the string format modes 
				String mode = "";
				String packetType =""; 
				int packetNumber =0;
				int delay = 0; 
				boolean valid = true;
				//ensure that packet type is valid
				if(commandParts[1].equals("0")|commandParts[1].equals("1")|commandParts[1].equals("2")|commandParts[1].equals("3")) {
					mode = commandParts[1]; 
					if((commandParts[1].equals("1") || commandParts[1].equals("2") || commandParts[1].equals("3")) && commandParts.length >2) {
						if( commandParts[2].equals("ack") || commandParts[2].equals("data")) {		
							//Set the packet type to look for 
							packetType = commandParts[2];
							try {
								//modes 1,2,3 all need packet number 
								if(commandParts[3].matches("[0-9]+")) {
									//set the packet number
									packetNumber = Integer.parseInt(commandParts[3]);
									
									// if mode is 3 or 4 need a length of 5
									if(commandParts.length == 5){
										if(commandParts[3].matches("[0-9]+")) {
											delay = Integer.parseInt(commandParts[4]);
										}else {
											valid = false; 
											System.out.println("there is an error in your mode formating, please try again ensure your DELAY is valid");
										}
											
									}else {
										if(!commandParts[1].equals("1")) {
											valid = false; 
											System.out.println("there is an error in your mode formating, please try again ensure your Packet_Number is valid");
										}

									}
									
								}else {
									
									System.out.println("there is an error in your mode formating, please try again ensure your Packet_Number is valid");
									valid = false;
								}
	
							}catch(Exception exp) {
								System.out.println("there is an error in your mode formating, please try again ensure your Packet_Number and DELAY are valid");
								valid =false;
							}
						}else {
							System.out.println("there is an error in your mode formating, please try again ensure your PACKET_TYPE is valid");
							valid =false;
							
						}
					}else {
						// allow mode = 0 to make it to the end
						if(!commandParts[1].equals("0")) {
							valid = false; 
							System.out.println("there is an error in your mode formating, please try again ensure your MODE is valid");
						}
					}

					if(valid) {
						IntermediateControl.mode = mode; 
						IntermediateControl.packetType = packetType;
						IntermediateControl.packetNumber = packetNumber; 
						IntermediateControl.delay = delay;
						System.out.println("You have set Mode  : " + mode + " Packet Type: " + packetType + " Packet# " +  packetNumber + " Delay: " + delay);
		
					}
					
				}else {
					System.out.println("there is an error in your mode formating, please try again ensure your ERROR_TYPE is valid");
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
	
	private void printMenue() {
		
		System.out.print("\nPlease Enter one of the following commands \n"
				+ "verbose \n"
				+ "quiet \n"
				+ "Error Packet format: mode Error_Type Packet_Type [Packet_Number] [Delay] \n"
				+ "For normal Operation:  mode 0\n"
				+ "To loose a Packet:     mode 1 [ack][data] packet# \n"
				+ "To Delay a Packet:     mode 2 [ack][data] packet# delay \n"
				+ "To duplicate a packet: mode 3 [ack][data] packet# delay\n"
				+ "\n");
		
	}
	
}