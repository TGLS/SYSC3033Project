package assignment.one;

/**
 * This short class has two functions: 1) Determine whether the program has been
 * called for Client, Server or Intermediate 2) Make the correct method calls
 * for each
 */

public class App2 {

	public static final String ipAddress = "localhost";
	public static final int sourceIP = 24;
	public static final int destIP = 69;

	public static void main(String[] args) {

		Thread server = new Thread(new Runnable() {
			public void run() {
				new Server();
			}
		});

		Thread intermediate = new Thread(new Runnable() {
			public void run() {
				Intermediate i = new Intermediate(sourceIP, destIP);
				i.loop();
			}
		});

		Thread client = new Thread(new Runnable() {
			public void run() {
				Client c = new Client( sourceIP);
				c.send(false, "Thompson", "Octet");
				c.send(true, "sKLFasjflksajf", "ocTET");
				c.send(false, "Delicious", "Netascii");
				c.send(true, "🍡AMA🍡AMA🍡", "netASCII");
				c.send(false, "A miserible pile of secrets", "netAscii");
				c.send(true, "Germany", "NetAscii");
				c.send(false, "irreg1672.jpg", "oCtEt");
				c.send(true, "darths1602.jpg", "OcTeT");
				c.send(false, "Windows.iso", "Netascii");
				c.send(true, "d3d9.dll", "netASCII");
				c.send(true,
						"This file name is almost certainly, perhaps with over 99% percent odds,"
								+ "far too long to be accepted by the server,"
								+ "because it will randomly be truncated part way through",
						"netASCII");
			}
		});
		try {
			server.start();
			intermediate.start();
			client.start();
		} catch (Exception e) {
			e.printStackTrace();
			incorrectArgumentMessage();
		}
	}

	private static void incorrectArgumentMessage() {
		System.out.println("You must provide arguments of one of the following forms:");
		System.out.println("server [source port]");
		System.out.println("client [destination ip] [destination port]");
		System.out.println("intermediate [source port] [destination ip] [destination port]");
		System.out.println("[Note that the destination ip and port must be the ip");
		System.out.println("and port of a server or intermediate process.]");
	}
}

