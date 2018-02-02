package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import apps.TFTPCommons;

/**
 * This class has two functions:
 * 1) Respond to the first message
 * 2) Handle steady state communications.
 */

public class ServerResponseThread implements Runnable {
	private DatagramSocket sendReceiveSocket;
	private DatagramPacket receivePacket;
	private byte[] receiveData;
	private DatagramPacket sendPacket;
	private byte[] sendData;
	
	public ServerResponseThread(DatagramPacket receivePacket) {
		this.receivePacket = receivePacket;
		this.receiveData = receivePacket.getData();
		try {
			this.sendReceiveSocket = new DatagramSocket();
		} catch (SocketException e) {
			// Print a stack trace, close all sockets and exit.
			e.printStackTrace();
			sendReceiveSocket.close();
			System.exit(1);
		}
	}
	
	public void run() {
		if (ServerControl.verboseMode) {
			printRequest();
		}
		if (!validateRequest() && ServerControl.verboseMode) {
			System.out.println("Invalid Message!");
			System.exit(1);
		}
		// If we received a read request
		// We'll start sending data packets
		if (receiveData[1] == 1) {
			TFTPCommons.sendFile(TFTPCommons.extractFileName(receiveData, receivePacket.getLength()),sendReceiveSocket, ServerControl.verboseMode,
					receivePacket.getAddress(), receivePacket.getPort());
		}
		// If we received a write request,
		// We'll send the first ACK and begin the write sequence.
		else if (receiveData[1] == 2) {
			formWriteBegin();
			if (ServerControl.verboseMode) {
				printWriteBegin();
			}
			sendWriteBegin();
			
			TFTPCommons.receiveFile(TFTPCommons.extractFileName(receiveData, receivePacket.getLength()),sendReceiveSocket, ServerControl.verboseMode);
		}
	}
	
	private void formWriteBegin() {
		// We send the first data packet for the write request.
		sendData = new byte[4];
		sendData[0] = 0;
		sendData[1] = 4;
		sendData[2] = 0;
		sendData[3] = 0;
		
		// Now that the data has been set up, let's form the packet.
		sendPacket = new DatagramPacket(sendData, 4, receivePacket.getAddress(),
				receivePacket.getPort());	
	}
	
	private void printRequest() {
		TFTPCommons.printMessage(false, receiveData, receivePacket.getLength());
	}
	
	private void printWriteBegin() {
		TFTPCommons.printMessage(true, sendData, sendPacket.getLength());
	}

	private void sendWriteBegin() {
		
		// Here, we're going to create a new socket (the notional sendSocket)
		// Send the response packet, and then close the socket.
		try {
			sendReceiveSocket.send(sendPacket);
		} catch (IOException e) {
			// Print a stack trace, close all sockets and exit.
			e.printStackTrace();
			sendReceiveSocket.close();
			System.exit(1);
		}
	}
	
	private boolean validateRequest() {
		// In this function, we determine whether or not the message we received
		// is valid.
		
		// Current Cell
		// This keeps track of what cell we are going to access next.
		// If it equals length, the message is invalid, likely because it was malformed.
		// It starts at 2 because the first two bytes are checked directly.
		int cell = 2;
		
		// If the length is over max_buffer bytes, the message is too long.
		if (receivePacket.getLength() > TFTPCommons.max_buffer) {
			return false;
		}
		
		// If the first two bytes aren't 0 1 or 0 2, then the message is malformed.
		if ((receiveData[0] != 0) | !((receiveData[1] == 1) | (receiveData[1] == 2))) {
			return false;
		}
		
		// Note which cell we're at now.
		int prev_cell = cell;
		
		// Fast-forward through the text to the first separator 0.
		while (true) {
			// If cell equals length, the message is invalid,
			// likely because it was malformed.
			if (cell == receivePacket.getLength()) {
				return false;
			}
			
			// If this is the first separator 0, break loop and go to next cell
			// Unless the first separator zero is the cell we started at,
			// then the message is invalid
			if (receiveData[cell] == 0) {
				if (cell == prev_cell) {
					return false;
				}
				cell++;
				break;
			} else {
				// Otherwise, go to next cell
				cell++;
			}
		}
		
		// Note which cell we're at now.
		prev_cell = cell;
				
		// Fast-forward through the text to the second separator 0.
		while (true) {
			// If cell equals length, the message is invalid,
			// likely because it was malformed.
			if (cell == receivePacket.getLength()) {
				return false;
			}
			
			// If this is the second separator 0, break loop and go to next cell.
			// Unless the first separator zero is the cell we started at,
			// then the message is invalid
			if (receiveData[cell] == 0) {
				if (cell == prev_cell) {
					return false;
				}
				cell++;
				break;
			} else {
				// Otherwise, go to next cell
				cell++;
			}
		}
		
		// At this point, we should have covered the whole message,
		// Unless it is malformed.
		if (cell == receivePacket.getLength()) {
			return true;
		} else {
			return false;
		}
	}
	
}
