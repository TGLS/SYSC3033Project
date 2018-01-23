package assignment.one;

public class Server {
	Thread receiveThread;
	
	
	public static void main(String[] args) throws Exception {
		Server server = new Server();
		server.receiveThread.start();
	}
	
	public Server() {
		receiveThread = new Thread(new RThread(),"receiveThread");
	}
}