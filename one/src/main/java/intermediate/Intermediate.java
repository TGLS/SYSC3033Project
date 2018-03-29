package intermediate;

import java.math.BigInteger;
import java.util.Arrays;
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
				System.exit(0);
				
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
				
				int MODE;
				//ensure that packet type is valid
				if(valInt(commandParts[1])) {
					MODE = Integer.parseInt(commandParts[1]);
				}else {
					MODE = 99;
				}
				
				int len = commandParts.length;
				
				switch(MODE) {
				
				case 0:
					if(!(len ==2)) {
						System.out.println("normal Operation must only contain  mode 0");
						valid = false;
					}else {
						mode = commandParts[1];
					}
					break;
					
				case 1:
					// Drop a packet format :  mode 1 [ack][data] packet#
					if(len == 4) {
						mode = commandParts[1];
						// validate the packet type
						if(validPacket(commandParts[2])) {
							packetType = commandParts[2];
							//validate the packet number 
							if(valInt(commandParts[3])) {
								packetNumber = Integer.parseInt(commandParts[3]);
							}else {
								System.out.println("Invalid packet number");
								valid = false;
							}
						}else {
							valid = false; 
						}
					}else {
						System.out.println("drop packet must have 4 componants:   mode 1 [ack][data] packet# ");
						valid = false; 
					}
	
					
					break;
				
				case 2:
					if(len == 5) {
						mode = commandParts[1];
						// validate the packet type
						if(validPacket(commandParts[2])) {
							packetType = commandParts[2];
							//validate the packet number 
							if(valInt(commandParts[3])) {
								packetNumber = Integer.parseInt(commandParts[3]);
								//validate the Delay 
								if(valInt(commandParts[4])) {
									delay = Integer.parseInt(commandParts[4]);
								}else {
									valid = false; 
									System.out.println("there is an error in your mode formating, please try again ensure your DELAY is valid");
								}
							}else {
								System.out.println("Invalid packet number");
								valid = false;
							}
						}else {
							valid = false; 
						}
					}else {
						System.out.println("loose packet must have 5 componants:  mode 2 [ack][data] packet# delay  ");
						valid = false; 
					}
					break;
				case 3:
					if(len == 5) {
						mode = commandParts[1];
						// validate the packet type
						if(validPacket(commandParts[2])) {
							packetType = commandParts[2];
							//validate the packet number 
							if(valInt(commandParts[3])) {
								packetNumber = Integer.parseInt(commandParts[3]);
								//validate the Delay number 
								if(valInt(commandParts[4])) {
									delay = Integer.parseInt(commandParts[4]);
								}else {
									valid = false; 
									System.out.println("there is an error in your mode formating, please try again ensure your DELAY is valid");
								}
							}else {
								System.out.println("Invalid packet number");
								valid = false;
							}
						}else {
							valid = false; 
						}
					}else {
						System.out.println("loose packet must have 5 componants:  mode 3 [ack][data] packet# delay  ");
						valid = false; 
					}
					break;
				case 4:
					//"Bad opcode:  mode 4 [ack][data][wrq][rrq] packet# opcode
					if(len == 5) {
						mode = commandParts[1];
						// validate the packet type
						if(validPacket(commandParts[2])) {
							packetType = commandParts[2];
							//validate the packet number 
							if(valInt(commandParts[3])) {
								packetNumber = Integer.parseInt(commandParts[3]);
								//validate the opcode 
								if(valInt(commandParts[4]) && commandParts[4].length() ==2) {
									
									int[] opc = new int[2]; 
									opc[0] =Integer.parseInt(commandParts[4].split("")[0]);
									opc[1] =Integer.parseInt(commandParts[4].split("")[1]);
									
									IntermediateControl.opcode = opc;
									
									System.out.println("We have set the opcode to " + opc[0] + " " + opc[1]);
									
									
									
								}else {
									System.out.println("Invalid opcode");
									valid =false; 
								}
								
								
							}else {
								System.out.println("Invalid packet number");
								valid = false;
							}
						}else {
							valid = false; 
						}
					}else {
						System.out.println("mode 4 [ack][data][wrq][rrq] packet# opcode ");
						valid = false; 
					}

					break;
				case 5:
					// Bad mode: mode 5 TransferMode 
					if(len == 3) {
						mode = commandParts[1];
						// automatically needs to be rrq or wrq 
						packetType = "rrq";
						IntermediateControl.newMode = commandParts[2];
						System.out.println("The Transfer mode will be set to : " + IntermediateControl.newMode);

		
					}else {
						System.out.println("drop packet must have 3 componants:   mode 5 TransferMode");
						valid = false; 
					}
					
					
					break;
					
				case 6:
					// Bad counter format :  mode 6 [ack][data] packet# counter
					if(len == 6) {
						mode = commandParts[1];
						// validate the packet type
						if(validPacket(commandParts[2]) && (!commandParts[2].equals("wrq")) && (!commandParts[2].equals("rrq"))) {
							packetType = commandParts[2];
							//validate the packet number 
							if(valInt(commandParts[3])) {
								packetNumber = Integer.parseInt(commandParts[3]);
								// now we need to add in the counter
								if((commandParts[4].equals("0")  || commandParts[4].equals("1")) && (commandParts[5].equals("0") || commandParts[5].equals("1"))) {
									
									byte[] Counter = new byte[2]; 
									
									Counter[0] = (byte) Integer.parseInt(commandParts[4]);
									Counter[1] = (byte) Integer.parseInt(commandParts[5]);
									
									
									
									IntermediateControl.Counter =Counter;
									
									System.out.println("The Counter will be set to: " + IntermediateControl.Counter[0] + " " + IntermediateControl.Counter[1]);
								}else {
									System.out.println("Invalid counter must be [0][1] [0][1] ");
									valid = false;
								}
							}else {
								System.out.println("Invalid packet number");
								valid = false;
							}
						}else {
							System.out.println("mode 6 can only be used on ack or data packets");
							valid = false; 
						}
					}else {
						System.out.println("Bad counter format 6 componants:    mode 6 [ack][data] packet# [0][1] [0][1]  ");
						valid = false; 
					}
					break;
				case 7:
					// invalid TID :  mode 7 [ack][data] packet#
					if(len == 4) {
						mode = commandParts[1];
						// validate the packet type
						if(validPacket(commandParts[2])) {
							packetType = commandParts[2];
							//validate the packet number 
							if(valInt(commandParts[3])) {
								packetNumber = Integer.parseInt(commandParts[3]);
							}else {
								System.out.println("Invalid packet number");
								valid = false;
							}
						}else {
							valid = false; 
						}
					}else {
						System.out.println("TFTP TID error must have 4 componants:   mode 7 [ack][data] packet# ");
						valid = false; 
					}
					break;
				default:
					System.out.println("there is an error in your mode formating, please try again ensure your ERROR_TYPE is valid");
					valid = false;
					break;
				}
				
				if(valid) {
					IntermediateControl.mode = mode; 
					IntermediateControl.packetType = packetType;
					IntermediateControl.packetNumber = packetNumber; 
					IntermediateControl.delay = delay;
					System.out.println("You have set Mode: " + mode + " Packet Type: " + packetType + " Packet# " +  packetNumber + " Delay: " + delay);
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
				+ "To drop a Packet:      mode 1 [ack][data] packet# \n"
				+ "To Delay a Packet:     mode 2 [ack][data] packet# delay \n"
				+ "To duplicate a packet: mode 3 [ack][data] packet# delay\n"
				+ "Bad opcode:            mode 4 [ack][data][wrq][rrq] packet# Opcode\n"
				+ "Bad mode:              mode 5 TransferMode \n"
				+ "Bad counter:           mode 6 [ack][data] packet# [0][1] [0][1] \n"
				+ "TFTP TID error:        mode 7 [ack][data] packet# \n"
				+ "\n");
		
	}
	
	private boolean validPacket(String packet) {
		if(packet.equals("ack") || packet.equals("data") ||packet.equals("wrq") || packet.equals("rrq")) {
			return true;
		}else {
			System.out.println("Not a valid packet type");
			return false;
		}
	}
	private boolean valInt(String item) {
		if(item.matches("[0-9]+")){
			return true;
		}else {
			return false;
		}
	}
	
	
}