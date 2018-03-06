package server;

import java.nio.file.NoSuchFileException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;

import apps.OutOfDiskSpaceException;
import apps.SendReceiveSocket;
import apps.TFTPCommons;

/**
 * This class has two functions:
 * 1) Respond to the first message
 * 2) Handle steady state communications.
 */

public class ServerResponseThread implements Runnable {
	private SendReceiveSocket sendReceiveSocket;
	private DatagramPacket receivePacket;
	private byte[] receiveData;
	private DatagramPacket sendPacket;
	private byte[] sendData;
	
	public ServerResponseThread(DatagramPacket receivePacket) {
		this.receivePacket = receivePacket;
		this.receiveData = receivePacket.getData();
		this.sendReceiveSocket = new SendReceiveSocket();
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
			// We can't write a file that doesn't exist, so let's check for it.
			InputStream stream = null;
			try {
				stream = Files.newInputStream(Paths.get(TFTPCommons.extractFileName(receiveData, receivePacket.getLength())));
				TFTPCommons.sendFile(stream,sendReceiveSocket, ServerControl.verboseMode,
						receivePacket.getAddress(), receivePacket.getPort());
				// Close the stream.
				stream.close();
			} catch (NoSuchFileException e) {
				// Send a file not found error.
				TFTPCommons.sendError(1,sendReceiveSocket, ServerControl.verboseMode,
						receivePacket.getAddress(), receivePacket.getPort());
			} catch (IOException e) {
				// Send an Access Violation Error and break.
				TFTPCommons.sendError(2,sendReceiveSocket, ServerControl.verboseMode,
						receivePacket.getAddress(), receivePacket.getPort());
				return;
			}
		}
		// If we received a write request,
		// We'll send the first ACK and begin the write sequence.
		else if (receiveData[1] == 2) {
			OutputStream stream = null;
			try {
				stream = TFTPCommons.createFile(TFTPCommons.extractFileName(receiveData, receivePacket.getLength()), false);
			} catch (FileAlreadyExistsException e) {
				// Send a File Already Exists Error and break.
				TFTPCommons.sendError(6,sendReceiveSocket, ServerControl.verboseMode,
						receivePacket.getAddress(), receivePacket.getPort());
				return;
			} catch (OutOfDiskSpaceException e) {
				// Send a Disk Full Error and break.
				TFTPCommons.sendError(3,sendReceiveSocket, ServerControl.verboseMode,
						receivePacket.getAddress(), receivePacket.getPort());
				return;
			} catch (IOException e) {
				// Send an Access Violation Error and break.
				TFTPCommons.sendError(2,sendReceiveSocket, ServerControl.verboseMode,
						receivePacket.getAddress(), receivePacket.getPort());
				return;
			}
			
			formWriteBegin();
			if (ServerControl.verboseMode) {
				printWriteBegin();
			}
			sendWriteBegin();
			
			// Keep the result of whether result worked
			boolean received = TFTPCommons.receiveFile(stream,sendReceiveSocket, ServerControl.verboseMode);
			
			// Close the stream.
			try {
				stream.close();
			} catch (IOException e) {
				// Print a stack trace and stop
				e.printStackTrace();
			}
			
			// receiveFile returns false if it fails. So we'll delete the file
			if (!received) {
				try {
					Files.delete(Paths.get(TFTPCommons.extractFileName(receiveData, receivePacket.getLength())));
				} catch (IOException e) {
					// Do nothing. The file probably got deleted beforehand somehow.
				}
			}
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
	
	private Boolean validateRequest() {
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
