Thompson Stubbs (100975033)
2018-01-20

Contents
1) How to run
2) Known Issues

1) How to run
From where you stored the Assignment1.jar, you can start the program by opening a command line interface there, and running the command "java -jar Assignment1.jar [arguments]". On Windows, the process is further simplified by using the batch program "Assignment1.bat", which runs the program running a client, a server, and an intermediate host on the same machine with the ports specified by the specification. Alternatively, you can run it through Eclipse, creating a run configuration for each set of arguments.

In order to run the whole system, you must run each of the following arguments.
server [source port]: This starts a server waiting to accept receive and respond to messages from a client or intermediate host. It will wait to receive messages from the port specified.
client [destination ip] [destination port]: This starts a client that will send messages to the a server or intermediate host at the destination ip at the destination port and wait for a response after each message.
intermediate [source port] [destination ip] [destination port]: This starts an intermediate host that will wait to receive messages from a client or intermediate host, and will forward them to a server or another intermediate host.

2) Known Issues
The client should be started after all intermediate hosts and servers are created. This is because if it is started earlier than that, the client will send a message to a server or intermediate host that has not yet started, and will stall forever.

The specification was unclear as to whether no file name or mode would treated as a valid file name or mode (no text is some text after all). The program can handle no file name or mode just fine, but it is not tested with this value.

In the event that the server receives an invalid request, then the server will exit without sending a message to inform the client or intermediates that it has received an invalid request. Thus, they will be waiting forever for a message that will not be received.

The system is not designed for use with multiple clients, so trying to connect multiple clients to the same server or intermediate host will have unusual effects.