# Reliable-Data-Transport-Protocol
Suppose you’ve a file and you want to send this file from one side to the other(server to
client), you will need to split the file into chunks of data of fixed length and add the data of
one chunk to a UDP datagram packet in the data field of the packet. Three methods
for RDT is implemented in this repo:
# 1. Stop-and-Wait
The server sends a single datagram, and blocks until an acknowledgment from the client
is received (or until a timeout expires).
# 2. Selective repeat
At any given time, the server is allowed to have up to N datagrams that have not yet been
acknowledged by the client. The server has to be able to buffer up to N datagrams, since it should
be able to retransmit them until they get acknowledged by the client.Moreover,the server has
to associate a timer with each datagram transmitted,so that it can retransmit a datagram if it is
not acknowledged in time.
# 3. GoBackN
In a Go-Back-N (GBN) protocol, the sender is allowed to transmit multiple packets
(when available) without waiting for an acknowledgment, but is constrained to have no
more than some maximum allowable number, N, of unacknowledged packets in the
pipeline.

# Work flow between server and client
The main steps are:
1. The client sends a datagram to the server giving the filename forthe transfer.This send needs
to be backed up by a timeout in case the datagram is lost.
2. The server forks off a child process to handle the client.
3. The server (child) creates a UDP socket to handle file transfer to the client.
4. Server sends its first datagram,the server uses some random number generator random()
function to decide with probability p if the datagram would be passed to the method send()or
just ignore sending it.
5. Whenever a datagram arrives, an ACK is sent out by the client to the server.
6. If you choose to discard the package and not to send itfrom the serverthe timer will end at
the server waiting for the ACK that it will never come from the client (sincethepacketwasn’tsent
to it) and the packet will be resent again from the server.
7. Update the window, and make sure to order the datagrams at the client side.
8. Repeat those steps till the whole file is sent and no other datagrams remains.
9. Close the connection.

# Arguments for the client
The client is to be provided with an inputfileclient.infrom which it reads the following
information, in the order shown, one item per line :
* IP address of server.
* Well-known port number of server.
* Port number of client.
* Filename to be transferred (should be a large file).
* Initial receiving sliding-window size (in datagram units).

# Arguments for the server
provide the server with an inputfileserver.infrom which it reads the following
information, in the order shown, one item per line :
* Well-known port number for server.
* Maximum sending sliding-window size (in datagram units).
* Random generator seedvalue.
* Probability p of datagram loss (real number in the range [ 0.0 , 1.0 ]).
