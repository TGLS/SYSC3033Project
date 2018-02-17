package client;

import java.io.BufferedReader;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.*;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.util.ArrayList;

import apps.OutOfDiskSpaceException;
import apps.TFTPCommons;

/**
 * This class handles all the client side functionality.
 */

public class Client {
	private DatagramPacket receivePacket;
	private byte[] receiveData;
	private DatagramSocket sendReceiveSocket;
	private DatagramPacket sendPacket;
	private byte[] sendData;
	private InetAddress destinationAddress;
	private int destinationPort;
	private Boolean verbose;

	public Client(String destinationIP, int destinationPort, Boolean verbose) {
		this.verbose = verbose;
		this.destinationPort = destinationPort;
		try {
			destinationAddress = InetAddress.getByName(destinationIP);
			sendReceiveSocket = new DatagramSocket();
		} catch (UnknownHostException e) {
			// Print a stack trace and exit.
			e.printStackTrace();
			System.exit(1);
		} catch (SocketException e) {
			// Print a stack trace and exit.
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void loop() {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			try {
				System.out.print("Enter command: ");
				interpretCommand(br.readLine());
				
			} catch (IOException e) {
				// Output an error and repeat.
				System.out.println("Input Error!");
			}
		}
	}
	
	public void interpretCommand(String command) {
		try {
			if (command.split(" ")[0].equals("read")) {
				read(command.split(" ")[1], "octet");
			} else if (command.split(" ")[0].equals("write")) {
				write(command.split(" ")[1], "octet");
			} else if (command.equals("quiet")) {
				verbose = false;
			} else if (command.equals("shutdown")) {
				System.out.println("Shutting down client.");
				System.exit(0);
			} else if (command.equals("verbose")) {
				verbose = true;
			} else if (command.equals("help")) {
				System.out.println("Valid commands are:");
				System.out.println("verbose");
				System.out.println("quiet");
				System.out.println("read [filename]");
				System.out.println("write [filename]");
				System.out.println("shutdown");
			} else {
				System.out.println("Invalid command.");
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			System.out.println("Invalid command.");
		}
	}

	public void read(String fileName, String mode) {
		OutputStream stream = null;
		try {
			stream = TFTPCommons.createFile(fileName, true);
		} catch (OutOfDiskSpaceException e) {
			// Inform the user the read failed to be created because the disk is full.
			System.out.println("The disk is full. This operation has been cancelled.");
			return;
		} catch (IOException e) {
			// Inform the user there was an access violation.
			System.out.println("There was an access violation. This operation has been cancelled.");
			return;
		}
		
		formRequest(true, fileName, mode);
		// No printing if it isn't verbose.
		if (verbose) {
			printRequest();
		}
		sendRequest();
		boolean received = TFTPCommons.receiveFile(stream, sendReceiveSocket, verbose);
		// Close the stream.
		try {
			stream.close();
		} catch (IOException e) {
			// Print a stack trace and exit.
			e.printStackTrace();
		}
	}
	
	public void write(String fileName, String mode) {
		// We can't write a file that doesn't exist, so let's check for it.
		InputStream stream = null;
		try {
			stream = Files.newInputStream(Paths.get(fileName)); 
		} catch (NoSuchFileException e) {
			System.out.println("File not found. This operation has been cancelled.");
			return;
		} catch (IOException e) {
			// Inform the user there was an access violation.
			System.out.println("There was an access violation. This operation has been cancelled.");
			return;
		}
		
		formRequest(false, fileName, mode);
		// No printing if it isn't verbose.
		if (verbose) {
			printRequest();
		}
		sendRequest();
		
		// Wait until we receive the proper Acknowledge
		while (true) {
			receiveResponse();
			// No printing if it isn't verbose.
			if (verbose) {
				printResponse();
			}
			
			// Check if it's an Error packet.
			if ((receiveData[0] == 0) & (receiveData[1] == 5)) {
				// Print the error message if we're not verbose
				if (!verbose) {
					TFTPCommons.printErrorMessage(receiveData, receivePacket.getLength());
				}
				
				
				// Close the stream. 
				try {
					stream.close();
				} catch (IOException e) {
					// Print a stack trace and stop
					e.printStackTrace();
				}
				
				// Stop; we won't be receiving any more packets after this.
				return;
			}
			
			// Check if it's an acknowledge and it's block zero
			if (receivePacket.getLength() == 4) {
				if ((receiveData[0] == 0) & (receiveData[1] == 4) &
					(receiveData[2] == 0) &
					(receiveData[3] == 0)) {
					break;
				}
			}
		}
		
		// Start sending data packets
		TFTPCommons.sendFile(stream, sendReceiveSocket, verbose,
				receivePacket.getAddress(), receivePacket.getPort());
		
		// Close the stream.
		try {
			stream.close();
		} catch (IOException e) {
			// Print a stack trace and stop
			e.printStackTrace();
		}
	}

	private void receiveResponse() {
		// In this function, we create a new packet to store and incoming
		// response,
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
	}

	private void printResponse() {
		TFTPCommons.printMessage(false, receiveData, receivePacket.getLength());
	}

	private void sendRequest() {
		// Here, we're going to create a new socket (the notional sendSocket)
		// Send the response packet, and then close the socket.
		try {
			sendReceiveSocket.send(sendPacket);
		} catch (IOException e) {
			e.printStackTrace();
			sendReceiveSocket.close();
			System.exit(1);
		}
	}

	private void formRequest(Boolean read, String fileName, String mode) {
		// To create the request, we create an arrayList of bytes,
		// then add all the bytes to it. Then we convert it to a regular array,
		// then build a packet.
		ArrayList<Byte> buffer = new ArrayList<Byte>();
		byte[] stringBuffer;

		// All requests begin with a 0.
		buffer.add((byte) 0);

		// If the request is a read, the next byte's a 1,
		// otherwise it's a write, so the next byte's a 2
		if (read) {
			buffer.add((byte) 1);
		} else {
			buffer.add((byte) 2);
		}

		// Next we add the filename.
		stringBuffer = fileName.getBytes();
		for (byte b : stringBuffer) {
			buffer.add(b);
		}

		// Now we need a zero
		buffer.add((byte) 0);

		// Next we add the mode.
		stringBuffer = mode.getBytes();
		for (byte b : stringBuffer) {
			buffer.add(b);
		}

		// And the final zero
		buffer.add((byte) 0);

		// Now we make an array
		// We make an array of the same size as the buffer, and then we copy the
		// old bytes over.
		// n is used to keep track of the index.
		sendData = new byte[buffer.size()];
		int n = 0;
		for (Byte b : buffer) {
			sendData[n] = b;
			n++;
		}

		// And now we build the packet
		sendPacket = new DatagramPacket(sendData, sendData.length, destinationAddress, destinationPort);
	}

	private void printRequest() {
		TFTPCommons.printMessage(true, sendData, sendPacket.getLength());
	}
}