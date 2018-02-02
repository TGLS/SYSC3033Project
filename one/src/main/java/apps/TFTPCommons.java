package apps;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class contains static functions used by multiple classes to facilitate TFTP.
 */

public class TFTPCommons {
	// To coordinate the size of the data buffer used by each component of the system,
	// We're moving the max_buffer constant here.
	public final static int max_buffer = 516;
	
	public static String extractFileName(byte[] messageData, int dataLength) {
		// Return a blank string if packet doesn't include a file name
		if ((messageData[1] == 1) | (messageData[1] == 2)) {
			// Copy the byte array after the first two bytes
			byte[] buffer = new byte[dataLength];
			for (int n = 2; n < dataLength; n++) {
				buffer[n - 2] = messageData[n];
			}

			// Now we convert the byte array to text, and split at null
			// characters
			String rawText = null;
			try {
				rawText = new String(buffer, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				// Print an error message and quit 
				System.out.println("Bad File Name");
				System.exit(1);
			}
			String[] text = rawText.split("\u0000");

			// Then we print the file name and mode, with labels
			return text[0];
		}
		return "";
	}
	
	public static void printMessage(boolean send, byte[] messageData, int dataLength) {
		// Checksum is the sum of all the bytes, less overflows
		int checksum = 0;
		// First we convert the sent data into strings.
		// While we're doing this, we'll check array index bounds by try-catch.
		try {
			// Note whether the packet is being sent or received.
			if (send) {
				System.out.print("Sending ");
			} else {
				System.out.print("Recieving ");
			}
			// If the second byte is 1, the message is a Read Request.
			// If the second byte is 2, the message is a Write Request.
			// If the second byte is 3, the message is a Data.
			// If the second byte is 4, the message is an Acknowledgement.
			// Anything else we say it's invalid.
			if (messageData[1] == 1) {
				System.out.println("Read Request");
			} else if (messageData[1] == 2) {
				System.out.println("Write Request");
			} else if (messageData[1] == 3) {
				System.out.println("Data");
			} else if (messageData[1] == 4) {
				System.out.println("Acknowledgment");
			} else {
				System.out.println("Invalid Message");
			}
			
			// For read and write requests, extract the filename and mode
			if ((messageData[1] == 1) | (messageData[1] == 2)) {
				// Copy the byte array after the first two bytes
				byte[] buffer = new byte[dataLength];
				for (int n = 2; n < dataLength; n++) {
					buffer[n - 2] = messageData[n];
				}

				// Now we convert the byte array to text, and split at null
				// characters
				String rawText = new String(buffer, "UTF-8");
				String[] text = rawText.split("\u0000");

				// Then we print the file name and mode, with labels
				System.out.println("File Name: " + text[0]);
				System.out.println("Mode: " + text[1]);
			}
			
			// For Data and Acknowledgment Messages
			// Print the block number being sent.
			if ((messageData[1] == 3) | (messageData[1] == 4)) {
				int block_number = ((int)(messageData[2]) & 0xFF) * 256;
				block_number = block_number + ((int)(messageData[3]) & 0xFF);
				System.out.println("Block Number: " + block_number);
			}

		} catch (ArrayIndexOutOfBoundsException e) {
			// If we get an array exception, the message is badly formed
			System.out.println("Error reading message.");
		} catch (UnsupportedEncodingException e) {
			// If we have an unsupported character, we'll just make a note.
			// This doesn't necessarily mean the request is invalid.
			System.out.println("Cannot render filename or mode.");
		}

		System.out.println("");
		
		// Then we print it as hex and compute a checksum.
		// This is accomplished by converting each number to hex,
		// then printing it. Line breaks every 40 numbers.
		for (int n = 0; n < dataLength; n++) {
			String s = String.format("%02X", messageData[n]);
			if (n % 40 == 39) {
				System.out.println(s);
			} else if (n == dataLength - 1) {
				System.out.println(s);
			} else {
				System.out.print(s);
			}
			// Calculate the checksum while printing each byte
			checksum += (int)messageData[n] & 0xFF;

			// Because we validate data after we print the data,
			// we'll check if we're over the max_buffer limit here,
			// to avoid going over array index limitations.
			if (n >= TFTPCommons.max_buffer) {
				break;
			}
		}
		// Print the checksum
		System.out.println("");
		System.out.println("Checksum: " + String.format("%08X", checksum));
	}
	
	public static void incrementBlockCounter(byte[] blockCounter) {
		// This is simple. We take in a 2 byte block counter, and we increment it.
		// We wrap around if necessary.
		if (blockCounter[1] == -1) {
			blockCounter[1] = 0;
			if (blockCounter[0] == -1) {
				blockCounter[0] = 0;
			} else {
				blockCounter[0]++;
			}
		} else {
			blockCounter[1]++;
		}
	}

	public static void receiveFile(String fileName, DatagramSocket sendReceiveSocket, boolean verbose) {
		// First of all, let's create a block counter and some other important variables
		byte[] blockCounter = {0, 1};
		byte[] receiveData;
		byte[] respondData; 
		DatagramPacket receivePacket;
		DatagramPacket respondPacket;
		
		// We're going to use Files to write to the file.
		// So let's get a path to a file.
		Path file = Paths.get(fileName);
		
		// Because it's a new file,  we have to initialize it first.
		// So let's create an empty array.
		byte[] data = {};
		try {
			// Then write it to the file.
			Files.write(file, data);
			
			// Now let's start looping.
			while (true) {
				// First, receive a packet
				// Create a byte array for the incoming packet.
				receiveData = new byte[max_buffer];
					
				// Create a packet for the incoming packet.
				receivePacket = new DatagramPacket(receiveData, receiveData.length);
					
				// Receive a message from the socket.
				// Surrounded with try-catch because receiving a message might fail.
				sendReceiveSocket.receive(receivePacket);
				
				// Print it if we're being verbose
				if (verbose) {
					printMessage(false, receiveData, receivePacket.getLength());
				}
				
				// Check if it's a data packet
				// And whether if its block counter matches
				if (receivePacket.getLength() >= 4) {
					if ((receiveData[0] == 0) & (receiveData[1] == 3) &
						(receiveData[2] == blockCounter[0]) &
						(receiveData[3] == blockCounter[1])) {
						// If it does, append the data block, if any
						if (receiveData.length > 4) {
							Files.write(file, Arrays.copyOfRange(receiveData, 4, receivePacket.getLength()), StandardOpenOption.APPEND);
						}
						
						// Then we respond with an acknowledge.
						respondData = new byte[] {0, 4, blockCounter[0], blockCounter[1]};
						respondPacket = new DatagramPacket(respondData, 4, receivePacket.getAddress(),
								receivePacket.getPort());
						
						// Print response if we're being verbose
						if (verbose) {
							printMessage(true, respondData, respondPacket.getLength());
						}
						
						sendReceiveSocket.send(respondPacket);
						
						// Increment blockCounter
						incrementBlockCounter(blockCounter);
						
						// Then check that whether we're finished.
						if (receivePacket.getLength() < 516) {
							break;
						}
					}
				}
			}
		} catch (IOException e) {
			// Print an error and quit
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void sendFile(String fileName, DatagramSocket sendReceiveSocket, boolean verbose, InetAddress targetAddress, int targetPort) {
		// First of all, let's create a block counter and some other important variables
		byte[] blockCounter = {0, 1};
		ArrayList<Byte> respondDataBuilder;
		byte[] receiveData;
		byte[] respondData; 
		DatagramPacket receivePacket;
		DatagramPacket respondPacket;
		boolean cont = true;
		FileInputStream in = null;
		
		// We're going to use a FileInputStream to read the file.
		try {
			in = new FileInputStream(fileName);
			// Now, let's start looping
			while (cont) {
				// Start building the message
				// Op code, block, then data
				respondDataBuilder = new ArrayList<Byte>();
				respondDataBuilder.add((byte) 0);
				respondDataBuilder.add((byte) 3);
				respondDataBuilder.add(blockCounter[0]);
				respondDataBuilder.add(blockCounter[1]);
				
				// Read in bytes from the file.
				// Break on EOF (-1) and when we reach max packet size.
				// On EOF, we stop the outer loop too.
				while (respondDataBuilder.size() < max_buffer) {
					int temp = in.read();
					if (temp == -1) {
						cont = false;
						break;
					} else {
						respondDataBuilder.add((byte) temp);
					}
				}
				
				// Now we make an array
				// We make an array of the same size as the buffer, and then we copy the
				// old bytes over.
				// n is used to keep track of the index.
				respondData = new byte[respondDataBuilder.size()];
				int n = 0;
				for (Byte b : respondDataBuilder) {
					respondData[n] = b;
					n++;
				}
				
				// Then we create and send the packet
				respondPacket = new DatagramPacket(respondData, respondData.length, targetAddress, targetPort);
				// Print it if we're being verbose
				if (verbose) {
					printMessage(true, respondData, respondPacket.getLength());
				}
				
				sendReceiveSocket.send(respondPacket);
				
				// Now let's wait for the acknowledge
				while (true) {
					// Prepare to receive a packet
					// Create a byte array for the incoming packet.
					receiveData = new byte[max_buffer];
						
					// Create a packet for the incoming packet.
					receivePacket = new DatagramPacket(receiveData, receiveData.length);
						
					// Receive a message from the socket.
					sendReceiveSocket.receive(receivePacket);
					
					// Print it if we're being verbose
					if (verbose) {
						printMessage(false, receiveData, receivePacket.getLength());
					}
					
					// Check if it's an acknowledge packet
					// And whether if its block counter matches
					if (receivePacket.getLength() == 4) {
						if ((receiveData[0] == 0) & (receiveData[1] == 4) &
							(receiveData[2] == blockCounter[0]) &
							(receiveData[3] == blockCounter[1])) {
							// Increment blockCounter
							incrementBlockCounter(blockCounter);
							break;
						}
					}
				}
			}
			
		} catch (FileNotFoundException e) {
			// Print an error and quit
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			// Print an error and quit
			e.printStackTrace();
			System.exit(1);
		} finally {
			try {
				// Close the in stream.
				in.close();
			} catch (IOException e) {
				// Print an error and quit
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
}
