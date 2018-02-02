Thompson Stubbs (100975033)
2018-01-20

Contents
1) How to run
2) Known Issues

1) How to run
From where you stored the Increment1.jar, you can start the program by opening a command line interface there, and running the command "java -jar Assignment1.jar [arguments]". On Windows, the process is further simplified by using the batch program "Increment1.bat", which runs the program running a server, an intermediate host, and multiple clients on the same machine with the ports specified by the specification. It starts each in verbose mode.

In order to run the whole system, you must run "Increment1.jar" one time with each of the
following arguments:
server
intermediate
client

You may provide any number of the following options afterwards:
-v, -verbose: Enables verbose output.
-q, -quiet: Disables verbose output (default).
-in_port=[port]: Sets the port incoming messages will be received by. Default for server is 69, for intermeditate is 23.
-out_port=[port]: Sets the port outgoing messages will be sent to. Default for client is 23, for intermeditate is 69.
-ip=[ip]: Sets the ip outgoing messages will be sent to. Default for client and intermediate is 127.0.0.1.
Later options are take precedence over earlier options.

2) Known Issues
The program will not accept read/write requests with no file name or mode, but the client won't send those messages anyway.

Writing to the same file on the server from two different clients can cause conflicts.

You can read from a file that is being written to, if you read after the write begins.

Reading a file that doesn't exist server-side will cause a crash, as will writing a file that doesn't exist client-side.

It's impossible to send commands to the system while verbose is outputting data. 