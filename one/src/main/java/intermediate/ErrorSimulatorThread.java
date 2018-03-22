package intermediate;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import apps.TFTPCommons;

/**
 * This class has one function:
 * Forward packets from server to client and vice versa.
 */

public class ErrorSimulatorThread implements Runnable{
	private DatagramPacket receivePacket;
	private byte[] receiveData;
	private DatagramSocket sendReceiveSocket;
	private DatagramPacket sendPacket;
	private byte[] sendData;
	private InetAddress serverAddress;
	private int serverPort;
	private InetAddress clientAddress;
	private int clientPort;
	private Boolean firstContact = true; 
	
	private Boolean duplicatePacket = false, delayPacket = false, losePacket = false, illegalTFTPOpcode = false,illegalTFTPCounter = false ,illegalTFTPMode = false, unknownTID = false ;
	
	
	// packet counters for all packet types simulation
	private int ackCounter =0;  
//  private int wrqCounter =0; Dont think I'll need these but I'll ask the TA tomorrow will only need them if run over multiple transfers
//	private int rrqCounter =0;
	private int dataCounter =0; // starting at -1 as rrq/wrq is read as a data packet
	
	public ErrorSimulatorThread(DatagramPacket receivePacket, String destinationIP, int destinationPort) {
		try {
			sendReceiveSocket = new DatagramSocket();
			serverAddress = InetAddress.getByName(destinationIP);
		} catch (SocketException e) {
		// Print a stack trace and exit.
			e.printStackTrace();
			System.exit(1);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		clientAddress = receivePacket.getAddress();
		this.receivePacket = receivePacket;
		this.receiveData = receivePacket.getData();
		clientPort = receivePacket.getPort();
		serverPort = destinationPort;
		
	}
	
	public void run() {
		byte[] lastBlock = null;
		while(true) {
			
			if(IntermediateControl.verboseMode) {
				printRequest();
			}
			formSendPacket();
			
			
			// Check to see if we need to modify this packet 
		
			
			if (receiveData[1] == 4) {
				ackCounter++;
			}
			
			//if we get a regular data packet increase the counter 
			if (receiveData[1] ==3) {
				dataCounter ++;
			}
			
			isError();
			
			sendPacket();
			
			
			
			// If we receive a non-full length packet,
			if ((receivePacket.getLength() < TFTPCommons.max_buffer) & (receivePacket.getLength() >= 4)) {
				// And it's a Data packet
				if ((receiveData[0] == 0) & (receiveData[1] == 3)) {
					// Set lastBlock properly.
					lastBlock = new byte[] {receiveData[2], receiveData[3]};
					//increase the data counter
					
				}
			}
			
		
			
			// If we receive an acknowledge packet
			if (receivePacket.getLength() ==  4) {
				if ((receiveData[0] == 0) & (receiveData[1] == 4)) {
					// And it matches block number with the previous value
					if (lastBlock != null) {
						if ((receiveData[2] == lastBlock[0]) & (receiveData[3] == lastBlock[1])) {
							// Kill the thread.
							break;
						}
					}
				}
			
			}
			
			// If we receive a error packet
			if ((receiveData[0] == 0) & (receiveData[1] == 5)) {
				// Kill the thread. Error packets are Terminal
				break;
			}
			
			
			receivePacket(); 
		}
		while(!IntermediateControl.canClose) {
			
		}
			sendReceiveSocket.close();
		
	}

	
	private void printRequest() {
		TFTPCommons.printMessage(false, receiveData, receivePacket.getLength());
	}
	
	private void reprintRequest() {
		TFTPCommons.printMessage(true, sendData, sendPacket.getLength());
	}
	
	// This should receive a packet gracefully  
	// Determine if its going to the client or server
	// and send appropriatly 
	private void receivePacket() {
	
		// In this function, we create a new packet to store and incoming response,
		// and store the incoming response.
		
		// Create a byte array for the incoming packet.
		receiveData = new byte[TFTPCommons.max_buffer];
		
		// Create a packet for the incoming packet.
		receivePacket = new DatagramPacket(receiveData, receiveData.length);
		
		// Receive a message from the reception socket.
		// Surrounded with try-catch because receiving a message might fail.
		try {
			sendReceiveSocket.receive(receivePacket);
		} catch (IOException e) {
			// Print a stack trace, close the socket, and exit.
			e.printStackTrace();
			sendReceiveSocket.close();
			System.exit(1);
		}
		if(firstContact) {
			serverAddress = receivePacket.getAddress();
			serverPort = receivePacket.getPort();
			firstContact = false;
		}
		
	}
	
	// this should be generic ie should be able to send to either the server or client depending on the packet that comes in
	private void formSendPacket() {
		
		// To create the request, we have the sendData point to the receiveData
		
		int length = receivePacket.getLength();
		
		if(illegalTFTPOpcode) {
			// Modify the send packet with invalid opcode
			sendData[0] = (byte) IntermediateControl.opcode[0];
			sendData[1] = (byte) IntermediateControl.opcode[1];
			
			System.out.println("opcod changed to " + sendData[0] + " " + sendData[1]);
			
		}
		
		
		if(illegalTFTPCounter) {
			// Modify the send packet with invalid opcode
			sendData[2] = (byte) IntermediateControl.Counter[0];
			sendData[3] = (byte) IntermediateControl.Counter[1];
			System.out.println("Counter  changed to " + sendData[2] + " " + sendData[3]);
			
		}
		
		
		
		if(illegalTFTPMode) {
			// in this case we need to modify the mode 
			// lets work back from the end and look for 0 byte 			
			byte[] stringBuffer;
			int count;
			// look for the 0 byte seperating the file name and mode 
			for(count = 2; count <receiveData.length; count++) {
				
				if(receiveData[count] ==0) {
					// we found the 0 lets break 
					break;
				}
			}
			
			//now copy everyhting except the mode into new array
			ArrayList<Byte> buffer = new ArrayList<Byte>();
			for(int i = 0; i<count;i++) {
				buffer.add(receiveData[i]);
			}
			//add the 0 byte back in 
			buffer.add((byte) 0);
			
			// now add the new mode 
			stringBuffer = IntermediateControl.newMode.getBytes();
			for (byte b : stringBuffer) {
				buffer.add(b);
			}
			buffer.add((byte) 0);
			
			
			// put it all into send data
			sendData = new byte[buffer.size()];
			int n = 0;
			for (Byte b : buffer) {
				sendData[n] = b;
				n++;
			}
			
			length = sendData.length;
			
			
		}else { 
			sendData = receiveData;
		}
		
		//sendData = receiveData;
		
		
		// if the receivePacket address is the client send to the sever 
		if(receivePacket.getAddress().equals(clientAddress) && receivePacket.getPort() == clientPort) {
			sendPacket = new DatagramPacket(sendData, length, serverAddress,serverPort);	
			
		}else {
			// if not send to the client 
			sendPacket = new DatagramPacket(sendData,  length, clientAddress, clientPort);
		}
	}
	
	private void sendPacket() {
		// Here, we're going to use the sendReceiveSocket to send the 
		try {
			 if(duplicatePacket) {
				 
				 if(IntermediateControl.verboseMode) {
						reprintRequest();
				}
				sendReceiveSocket.send(sendPacket);
				
				if(IntermediateControl.delay !=0) {
					 Thread delayThread = new Thread(new ErrorSimDelayThread(IntermediateControl.delay, sendPacket, sendReceiveSocket));
					 delayThread.start();
				}else {
					if(IntermediateControl.verboseMode) {
						reprintRequest();
					}
					sendReceiveSocket.send(sendPacket);
				}
				
				duplicatePacket = false; 
				
			 }else if(delayPacket) {
				 // Delay the Packet 	
				 System.out.println("A Packet has been Delayed");
				 //Create a delay thread to delay the packet
				 Thread delayThread = new Thread(new ErrorSimDelayThread(IntermediateControl.delay, sendPacket, sendReceiveSocket));
				 delayThread.start();
				 delayPacket = false; 
				 
			 }else if(losePacket) {
				 // don't do anything
				 System.out.println("A packet has been dropped!");
				 losePacket = false; 
			 }else if (illegalTFTPCounter) {
				 
				 System.out.println("Changing the Counter!");
				 
				 formSendPacket();
				 if(IntermediateControl.verboseMode) {
						reprintRequest();
				}
				 
				 illegalTFTPCounter = false;
				 
			
			 }else if (illegalTFTPMode) {
				 
				 System.out.println("Changing the Transfer Mode!");
				 formSendPacket();
				 illegalTFTPMode = false;
				 if(IntermediateControl.verboseMode) {
						reprintRequest();
				}
				 // for now this is normal mode 
				 sendReceiveSocket.send(sendPacket);	
				 
			 }else if (illegalTFTPOpcode) {
				 
				 // here we need to edit the opcode of the packet 
				 System.out.println("Changing the Opcode!");
				 
				 formSendPacket();
				 
				 illegalTFTPOpcode= false; 
				 if(IntermediateControl.verboseMode) {
						reprintRequest();
				}
				 // for now this is normal mode 
				 sendReceiveSocket.send(sendPacket);	
				 
				 
			 }else if (unknownTID) {
				 // THis will simulate an illegal Tid being sent to the server/client 
					System.out.println("Simulating an unknown TID");
				 // create an error socket 
				 DatagramSocket errorSocket = new DatagramSocket();
				 
				if(IntermediateControl.verboseMode) {
					reprintRequest();
				}
				
				 //send the packet using the new socket 
				 errorSocket.send(sendPacket);
			 
				 //wait to receive the error message
				 byte[] receiveDataError = new byte[TFTPCommons.max_buffer];
					
					// Create a packet for the incoming packet.
				DatagramPacket receivePacketError = new DatagramPacket(receiveDataError, receiveDataError.length);
					
					// Receive a message from the reception socket.
					// Surrounded with try-catch because receiving a message might fail.
				try {
					System.out.println("Waiting for response");
					errorSocket.receive(receivePacketError);
				} catch (IOException e) {
					// Print a stack trace, close the socket, and exit.
					e.printStackTrace();
					errorSocket.close();
					System.exit(1);
				}
				errorSocket.close();
				System.out.println("Closing the error socket");
				// for now just print out the message on the packet should be the error code 
				if(IntermediateControl.verboseMode) {
					TFTPCommons.printMessage(false, receiveDataError, receivePacketError.getLength());
				}
				unknownTID = false;
				
				
				// After this point the client will timeout 
			 
			 }else {
				 if(IntermediateControl.verboseMode) {
						reprintRequest();
				}
				 // for now this is normal mode 
				 sendReceiveSocket.send(sendPacket);	 
			 }
		} catch (IOException e) {
			// Print a stack trace, close all sockets and exit.
			e.printStackTrace();
			sendReceiveSocket.close();
			System.exit(1);
		}
	}

	public void isError() {
		//The purpose of this class is to watch the counters and the input data to ensure that we 
		//Modify the correct packet
		
		//If mode is 0 no need to continue
		if(!IntermediateControl.mode.equals("0")) {	
			//Need to determine if were at the right packet // still need to add cases for wrq and rrq
			if((IntermediateControl.packetType.equals("ack") && ackCounter == IntermediateControl.packetNumber) 
			||((IntermediateControl.packetType.equals("data") && dataCounter == IntermediateControl.packetNumber))
			||((IntermediateControl.packetType.equals("wrq")|IntermediateControl.packetType.equals("rrq")) && (ackCounter==0 && dataCounter ==0))){
				
				if(IntermediateControl.mode.equals("1")) {
					//This is the drop packet case 
					losePacket = true;
				}	
				if (IntermediateControl.mode.equals("2")) {
					//Delay a packet 
					delayPacket = true;
					IntermediateControl.canClose =false;
				}
				if (IntermediateControl.mode.equals("3")) {
					//Duplicate a packet
					System.out.println("Creating Duplicate Packets");
					duplicatePacket = true;
					IntermediateControl.canClose =false;
				}
				if (IntermediateControl.mode.equals("4")) {
					// this will simulate Illegal TFTP operation.
					//junk the op code 
					//junk the counter for datas/acks
					//junk the mode for requests 
					// choose the packet same as before 
					
					illegalTFTPOpcode = true; 
					
					
					
				}
				if (IntermediateControl.mode.equals("5")) {
					// this will simulate Illegal TFTP operation.
					//junk the op code 
					//junk the counter for datas/acks
					//junk the mode for requests 
					// choose the packet same as before 
					illegalTFTPMode = true; 
					
					
					
				}
				if (IntermediateControl.mode.equals("6")) {
					// this will simulate Illegal TFTP operation.
					//junk the op code 
					//junk the counter for datas/acks
					//junk the mode for requests 
					// choose the packet same as before 
					
					illegalTFTPCounter = true; 
					
					
				}
				if (IntermediateControl.mode.equals("7")) { // works for now need to test
					//This will simulate Unknown transfer ID.
					// change source port
					// choose the packet same as before 
				
					// create a new socket send the packet to the server 
					// wait of the error response 
					// print the response 
					// close the socket 
					
					unknownTID = true;
					
					
					
					
				}
				
				IntermediateControl.packetType = "";
				IntermediateControl.mode = "0";
				
			}
		}
		
	}

	
	
	
}
