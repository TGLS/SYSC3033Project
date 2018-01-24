package assignment.one;

public class Server {
	Thread receiveThread;

	public Server() {
		receiveThread = new Thread(new RThread(),"receiveThread");
		receiveThread.start();
	}
}