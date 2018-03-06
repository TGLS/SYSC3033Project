package intermediate;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
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
	
	private Boolean duplicatePacket = false, delayPacket = false, losePacket = true;
	
	
	// packet counters for all packet types simulation
	private int ackCounter =0;  
//  private int wrqCounter =0; Dont think I'll need these but I'll ask the TA tomorrow will only need them if run over multiple transfers
//	private int rrqCounter =0;
	private int dataCounter =0;
	
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
			
			if(IntermediateControl.verboseMode) {
				reprintRequest();
			}
			
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
			
			//if we get a regular data packet increase the counter 
			if ((receivePacket.getLength() < TFTPCommons.max_buffer) & (receivePacket.getLength() >= 4)) {
				dataCounter ++;
			}
			
			
			// If we receive an acknowledge packet
			if (receivePacket.getLength() ==  4) {
				ackCounter++;
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
	//	System.out.println("Recieved a packet !" + receivePacket.getAddress().equals(clientAddress));
		if(firstContact) {
			serverAddress = receivePacket.getAddress();
			serverPort = receivePacket.getPort();
			firstContact = false;
		}
		
	}
	
	// this should be generic ie should be able to send to either the server or client depending on the packet that comes in
	private void formSendPacket() {
		
		// To create the request, we have the sendData point to the receiveData
		sendData = receiveData;
		
		// if the receivePacket address is the client send to the sever 
		if(receivePacket.getAddress().equals(clientAddress) && receivePacket.getPort() == clientPort) {
			System.out.println("Sending to the Server !");
			sendPacket = new DatagramPacket(sendData, receivePacket.getLength(), serverAddress,
					serverPort);
			
		}else {
			// if not send to the client 
			sendPacket = new DatagramPacket(sendData, receivePacket.getLength(), clientAddress,
					clientPort);
		}
	}
	
	private void sendPacket() {
		// Here, we're going to use the sendReceiveSocket to send the 
		try {
			 if(duplicatePacket) {
				sendReceiveSocket.send(sendPacket);
				
				if(IntermediateControl.delay !=0) {
					//delay by that amount
				}
				
				sendReceiveSocket.send(sendPacket);
				duplicatePacket = false; 
				
			 }else if(delayPacket) {
				 // Delay the Packet 	
				 System.out.println("Delaying the Packet");
				 //Create a delay thread to delay the packet
				 Thread delayThread = new Thread(new ErrorSimDelayThread(IntermediateControl.delay, sendPacket, this));
				 delayThread.start();
				 delayPacket = false; 
				 
			 }else if(losePacket) {
				 // don't do anything
				 System.out.println("A packet has been lost!");
				 losePacket = false; 
			 }else {
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
	
	public void sendPacket(DatagramPacket sendPacket) {
		// Here, we're going to use the sendReceiveSocket to send the 
		try {
			sendReceiveSocket.send(sendPacket);
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
			||( (IntermediateControl.packetType.equals("data") && dataCounter == IntermediateControl.packetNumber)) ){
			
				if(IntermediateControl.mode.equals("1")) {
					//This is the drop packet case 
					losePacket = true;
				}	
				if (IntermediateControl.mode.equals("2")) {
					//Delay a packet 
					delayPacket = true;
					
				}
				if (IntermediateControl.mode.equals("3")) {
					//Duplicate a packet
					duplicatePacket = true;
				}

			}
		}
		
	}
	
	
	
	
}
