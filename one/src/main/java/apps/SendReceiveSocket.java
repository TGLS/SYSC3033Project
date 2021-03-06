package apps;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
 * This class is a fairly transparent wrapper for DatagramSocket.
 * Major differences are that receive takes a verbose argument,
 * and it will attempt to resend on timeout (locked at 1000 ms)
 */


public class SendReceiveSocket {
	private DatagramSocket socket;
	private DatagramPacket previousPacket;
	
	public SendReceiveSocket() {
		try {
			socket = new DatagramSocket();
			socket.setSoTimeout(5000);
			previousPacket = null;
		} catch (SocketException e) {
			// Print a stack trace and exit.
			e.printStackTrace();
			socket.close();
			System.exit(1);
		}
	}

	public void receive(DatagramPacket receivePacket, boolean verbose) throws IOException {
		int counter = 24;
		// Until we receive a packet 
		while (true) {
			try {
				// Attempt to receive a packet.
				socket.receive(receivePacket);
				// After we successfully received a packet, break
				break;
			} catch (SocketTimeoutException e) {
				// Count down to zero. If at zero, stop retransmitting and raise the timeout.
				counter--;
				if (counter == 0) {
					throw e;
				}
				// Retransmit the last sent packet, unless it's an ACK or we haven't sent a packet yet.
				if (previousPacket != null) {
					if (verbose) {
						TFTPCommons.printMessage(true, previousPacket.getData(), previousPacket.getLength());
					}
					socket.send(previousPacket);
				}
			}
		}
	}

	public void close() {
		// Close the socket
		socket.close();
	}

	public void send(DatagramPacket sendPacket) throws IOException {
		// Send the datagram packet
		socket.send(sendPacket);
		
		// Check whether the datagram packet is an ACK or not.
		if ((sendPacket.getData()[0] == 0) & (sendPacket.getData()[1] == 4)) {
			// Clear the previousPacket
			previousPacket = null;
		} else if ((sendPacket.getData()[0] == 0) & (sendPacket.getData()[1] == 5)) {
			// Do nothing. We are either sending a bad TID error or a terminal error
		} else  {
			// Keep a reference of the previous packet handy.
			previousPacket = sendPacket;
		}
	}
}
