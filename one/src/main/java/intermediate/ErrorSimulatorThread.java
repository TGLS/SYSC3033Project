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
	
	// packet counters for error simulation
	//private int ackCounter =0;
	//private int wrqCounter =0;
	//private int rrqCounter =0;
	//private int dataCounter =0;
	
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
			
			// need to put in the checks for the different error modes 
			
			if(IntermediateControl.mode =="00") {
				// normal operation don't do anything 
				
			}
			if(IntermediateControl.mode =="01") {
				//loose a packet 
				
				// lets get somecounters going and if the counters match the paraaters dont send 
				
			}
			if(IntermediateControl.mode =="02") {
				
				//delay packet 
				//create a separate thread that will delay by a specified amount
				// want the separate so that it will not block
				
				
				
			}
			if(IntermediateControl.mode =="03") {
				//duplicate a packet 
				// when we get to the desired packet resend it again
				
				
			}
			
			
			
			
			
			
			
			
			
			System.out.println("beginning of the loop");
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
		System.out.println("Problem");
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
			System.out.println("First Contact!");
			serverAddress = receivePacket.getAddress();
			serverPort = receivePacket.getPort();
			firstContact = false;
		}
		System.out.println("Recieved a packet 2 !");
		
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
			sendReceiveSocket.send(sendPacket);
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
	
	
}
