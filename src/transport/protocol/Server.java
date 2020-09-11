/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package transport.protocol;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;
import static transport.protocol.Networks.mode;

/**
 *
 * @author Rowan Salem
 */
public class Server extends Networks {

    private static volatile boolean working;
    private DatagramSocket serverSocket;
    private LinkedList<SocketAddress> clients;
    private LinkedList<ServerChild> serverChild;
    private float PLP;
    private long seedValue;
    private int serverPortNumber;
    private int serverWindowSize;
    private String serverPath;

    public Server(String serverPath, int portNumber, float PLP, long seedValue, int windowSize) {
        working = true;
        clients = new LinkedList<SocketAddress>();
        serverChild = new LinkedList<ServerChild>();
        this.serverPortNumber = portNumber;
        this.serverWindowSize = windowSize;
        this.seedValue = seedValue;
        this.serverPath = serverPath;
        this.PLP = PLP;
        try {
            serverSocket = new DatagramSocket(portNumber);
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                working = false;
                try {
                    serverSocket.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    public static void closeUdpServers() {
        Server.working = false;
        Thread.currentThread().interrupt();
    }

    public void run() {
        System.out.println("Server Started");
        while (Server.working) {
            byte[] receiveData = new byte[512];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            try {
                serverSocket.receive(receivePacket);
                ServerChild responder = ServeMultiClients(receivePacket);
                responder.handleMessage(receivePacket);
            } catch (Exception ex) {
                ex.printStackTrace();
                break;
            }
        }
        if (serverSocket != null) {
            serverSocket.close();
        }
    }

    private ServerChild ServeMultiClients(DatagramPacket packet) {
        int index;
        if ((index = clients.indexOf(packet.getSocketAddress())) != -1) {
            return serverChild.get(index);
        } else {
            System.out.println("New Responder Created");
            clients.add(packet.getSocketAddress());
            ServerChild responder = new ServerChild(serverPath, Integer.toString(packet.getPort()), serverSocket, PLP, seedValue, packet);
            serverChild.add(responder);
            return responder;
        }
    }
///////////////////////////////////////////////////////////////////////////////////////////////////

    public class ServerChild {

        private DatagramSocket datagramSocket;
        private DatagramPacket datagramPacket;
        private InetAddress IPAddress;
        private int port;
        //       private String name;
        private PacketsManager packetsManager;
        private LinkedList<Integer> receivedAkns;
        private LinkedList<Packet> receivedPackets;
        private float PLP;
        private long seedValue;
        int numberOfDropped;
        private LinkedList<Integer> droppedPackets;
        private String name;
        private String serverPath;
        private ArrayList<Packet> packets = new ArrayList<>();
        private int packetSize = 512;
        int goBackNFail = 0;
        int goBackNFailLast = 0;

        public ServerChild(String serverPath, String name, DatagramSocket datagramSocket, float PLP, long seedValue, DatagramPacket packet) {
            this.datagramSocket = datagramSocket;
            this.datagramPacket = packet;
            this.PLP = PLP;
            this.seedValue = seedValue;
            this.name = name;
            this.serverPath = serverPath;
            this.IPAddress = datagramPacket.getAddress();
            this.port = datagramPacket.getPort();
            this.droppedPackets = new LinkedList<>();
            receivedPackets = new LinkedList<Packet>();
            receivedAkns = new LinkedList<Integer>();

        }

        private void sendFileAKN(int fileLength) throws Exception {
            Packet packet = new Packet(Packet.PacketType.FileAKN, null, fileLength);
            byte[] packetBytes = packet.getPacketAsBytes();
            final DatagramPacket sendPacket = new DatagramPacket(packetBytes, packetBytes.length,
                    IPAddress, port);
            System.out.println("Responder " + name + "  sent  file request AKN: " + fileLength + ", " + packetsManager.packetCount);
            try {
                datagramSocket.send(sendPacket);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        private void sendPacket(byte[] data, final int sequenceNumber) throws Exception {
            if (sequenceNumber == goBackNFailLast) {
                goBackNFail = 0;
            }
            if (working && !receivedAkns.contains(sequenceNumber)) {
                Packet packet = new Packet(Packet.PacketType.DataPacket, data, sequenceNumber);
                //   System.out.println("window high="+windowHigh);
                byte[] packetBytes = packet.getPacketAsBytes();
                final DatagramPacket sendPacket = new DatagramPacket(packetBytes,
                        packetBytes.length, IPAddress, port);
                if (!droppedPackets.contains(packet.getSequenceNumber())) {
                                                 System.out.println("Responder " + name + "  sent DataPacket: " + data.length + ", " + sequenceNumber);
                    try {
                        datagramSocket.send(sendPacket);

                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                } else if (droppedPackets.contains(packet.getSequenceNumber()) && packet.getSequenceNumber() % 2 != 0) {
                                                 System.out.println("Responder " + name + "  sent DataPacket: " + data.length + ", " + sequenceNumber);
                    byte[] corruptedPacketBytes = packet.getCorruptedPacket();
                    final DatagramPacket sendCorruptedPacket = new DatagramPacket(corruptedPacketBytes, corruptedPacketBytes.length, IPAddress, port);
                    try {

                        datagramSocket.send(sendCorruptedPacket);

                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    droppedPackets.remove(droppedPackets.indexOf(packet.getSequenceNumber()));
                } else {
                    droppedPackets.remove(droppedPackets.indexOf(packet.getSequenceNumber()));

                }

                if (Networks.mode != Mode.GoBackN) {
                    PacketTimer timer = new PacketTimer(sequenceNumber);
                    PacketTimerTask action = new PacketTimerTask(sequenceNumber) {
                        public void run() {
                            int timerSequenceNumber = this.getSequenceNumber();
                            if (!receivedAkns.contains(timerSequenceNumber)) {
                                try {
                                    System.out.println("Failed To Send: " + timerSequenceNumber);
                                    sendPacket(packetsManager.getPacketAsBytes(timerSequenceNumber),
                                            timerSequenceNumber);
                                    this.cancel();
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }
                    };
                    timer.schedule(action, PacketsManager.WaitTime);
                    packetsManager.timers.addLast(timer);
                }
            } else {
                System.out.println("Server Closed");
            }
        }

        private synchronized void addGoBackNTimer(int sequenceNumber) {
            if (Networks.mode == Mode.GoBackN && sequenceNumber == packetsManager.windowLow) {
                PacketTimer timer = new PacketTimer(sequenceNumber);
                PacketTimerTask action = new PacketTimerTask(sequenceNumber) {
                    public void run() {
                        int timerSequenceNumber = this.getSequenceNumber();

                        if (!receivedAkns.contains(timerSequenceNumber)) {
                            try {
                                System.out.println("Failed To Send Window From: "
                                        + timerSequenceNumber);
                                if(Networks.mode==Mode.GoBackN)
                                {  goBackNFail = 1;
                                goBackNFailLast = timerSequenceNumber + serverWindowSize;}
                                int low = packetsManager.windowLow;
                                int high = packetsManager.windowHigh;
                                addGoBackNTimer(low);
                                for (int i = low; i <= high; ++i) {
                                    //  System.out.println("failed to send window from :"+timerSequenceNumber+"\n and send :"+i);
                                    sendPacket(packetsManager.getPacketAsBytes(i), i);
                                }
                                this.cancel();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                };
                timer.schedule(action, PacketsManager.WaitTime);
                packetsManager.timers.addLast(timer);
            }
        }

        private synchronized void sendNextPacket(int aknNumber) throws Exception {
            if (Networks.mode != Mode.GoBackN) {
                if (aknNumber == packetsManager.windowLow) {
                    while (packetsManager.isNextExist() && receivedAkns.contains(
                            packetsManager.windowLow)) {
                        ++packetsManager.windowLow;
                        ++packetsManager.windowHigh;
                        sendPacket(packetsManager.getPacketAsBytes(packetsManager.windowHigh),
                                packetsManager.windowHigh);
                    }
                }
   System.out.println("Low: " + packetsManager.windowLow + ", High: "
                        + packetsManager.windowHigh);
            } else if (Networks.mode == Mode.GoBackN) {

                packetsManager.windowLow = aknNumber + 1;
                addGoBackNTimer(packetsManager.windowLow);
                while (packetsManager.windowHigh < (packetsManager.packetCount - 1)
                        && packetsManager.windowHigh < (packetsManager.windowLow
                        + PacketsManager.windowSize - 1)) {
                    ++packetsManager.windowHigh;
                    // System.out.println("gwa el send next");
                    if (goBackNFail == 0) {
                        sendPacket(packetsManager.getPacketAsBytes(packetsManager.windowHigh),
                                packetsManager.windowHigh);
                    }
                }
                System.out.println("Low: " + packetsManager.windowLow + ", High: "
                        + packetsManager.windowHigh);
            }
        }

        private synchronized void deleteTimer(int aknNumber) {
            for (int i = 0; i < packetsManager.timers.size(); ++i) {
                if (packetsManager.timers.get(i).getSequenceNumber() == aknNumber) {
                    packetsManager.timers.remove(i).cancel();
                }
            }
        }

        protected void handleMessage(DatagramPacket datagramPacket) throws Exception {
            Packet packet = new Packet(datagramPacket.getData());
            String message = "Responder " + name + " received ";
            Packet.PacketType packetType = packet.getPacketType();
            if (packetType == Packet.PacketType.FileRequest) {
                ///////////////////////////////////////////////////////////////////////////////////
                receivedPackets = new LinkedList<Packet>();
                receivedAkns = new LinkedList<Integer>();
                System.out.println(message + "File Request: " + packet.getRequestedFile());
                //fileIntoPackets(packet.getRequestedFile());
                packetsManager.setWindowSize(serverWindowSize);
                packetsManager = new PacketsManager(packet.getRequestedFile());
                receivedPackets.add(packet);
                sendFileAKN(packetsManager.overAllSize);
                numberOfDropped = (int) Math.ceil(packetsManager.packetCount * PLP);
                System.out.println("Number of dropped packets:" + numberOfDropped);
                Random r = new Random(seedValue);
                for (int i = 0; i < numberOfDropped; i++) {
                    int x = r.nextInt(packetsManager.packetCount);
                    if (droppedPackets.contains(x)) {
                        i--;
                    } else {
                        droppedPackets.add(x);
                        //            System.out.println("drop: " + x);
                    }
                }
                if (Networks.mode == Mode.GoBackN) {
                    addGoBackNTimer(packetsManager.windowLow);
                }
                for (int i = 0; i <= packetsManager.windowHigh; ++i) {
                    sendPacket(packetsManager.getPacketAsBytes(i), i);
                }
            } else if (packetType == Packet.PacketType.AKN) {
                int aknNumber = packet.getAknNumber();
                receivedPackets.add(packet);
                if (Networks.mode != Mode.GoBackN) {
                    deleteTimer(aknNumber);
                    if (aknNumber >= packetsManager.windowLow && aknNumber
                            <= packetsManager.windowHigh && !receivedAkns.contains(aknNumber)) {
                        receivedAkns.add(aknNumber);
                        Thread.sleep(3);
                        System.out.println(message + "AKN : " + aknNumber);
                        sendNextPacket(aknNumber);
                    }
                } else if (mode == Mode.GoBackN) {
                    System.out.println(message + "AKN : " + aknNumber);
                    if (aknNumber >= packetsManager.windowLow) {
                        for (int i = packetsManager.windowLow; i <= aknNumber; ++i) {
                            receivedAkns.add(i);
                        }
                        deleteTimer(packetsManager.windowLow);
                        sendNextPacket(aknNumber);
                    }
                }
                ///////////////////////////////////////////////////////////////////////////////////
            }
        }

    }

}
