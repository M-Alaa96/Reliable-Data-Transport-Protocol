
package transport.protocol;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import static sun.security.krb5.Confounder.bytes;

public class Client extends Networks {

    private volatile boolean working;
    private InetAddress IPAddress;
    private DatagramSocket clientSocket;
    private byte[] fileArray;
    private byte[] temp;
    private ByteBuffer fileBuffer;
    private LinkedList<Packet> receivedPackets;
    private LinkedList<Integer> receivedSequences;
    private LinkedList<Packet> waitingPackets;
    private LinkedList<Integer> waitingSequences;
    private int currentWriteSequence;
    private int packetSize;
    private int dropAfter;
    private int windowSize;
    private int windowLow;
    private int windowHigh;
    private String fileName;
    private String path;
    private String name;
    private int clientPortNumber;
    private int serverPortNumber;
    List<Byte> byteList = new ArrayList<Byte>();
    int fileAck;
    int fileSize;
    int receivedSize = 0;
    int expectedSequenceNumber = -1;
int packetCounter=0;
    //   FileOutputStream fos = null;
    public Client(String name, String path, int clientPortNumber, int serverPortNumber, int windowSize) throws Exception {
        working = true;
        this.path = path;
        this.name = name;
        this.clientPortNumber = clientPortNumber;
        this.serverPortNumber = serverPortNumber;
        this.windowSize = windowSize;
        this.packetSize = 512;
        receivedPackets = new LinkedList<Packet>();
        clientSocket = new DatagramSocket(clientPortNumber);
        IPAddress = InetAddress.getByName("localhost");
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                working = false;
                try {
                    clientSocket.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    public void close() {
        working = false;
    }

    public void run() {
        while (working) {
            byte[] receiveData = new byte[packetSize];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            try {
                clientSocket.receive(receivePacket);
                readDatagram(receivePacket);
            } catch (Exception ex) {
                System.err.println("??" + name);
                ex.printStackTrace();
                break;
            }
        }
        if (clientSocket != null) {
            clientSocket.close();
        }
    }

    public void requestFile(String fileName) throws Exception {
        this.fileName = fileName;
        currentWriteSequence = 0;
        Packet packet = new Packet(Packet.PacketType.FileRequest, fileName.getBytes());
        receivedSequences = new LinkedList<Integer>();
        waitingPackets = new LinkedList<Packet>();
        waitingSequences = new LinkedList<Integer>();
        fileAck = 0;
        DatagramPacket sendPacket = new DatagramPacket(packet.getPacketAsBytes(),
                packet.getPacketAsBytes().length, IPAddress, serverPortNumber);
        try {
            clientSocket.send(sendPacket);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
            expectedSequenceNumber=0;
        System.err.println("CLIENT " + name + " requested File: " + fileName);
        PacketTimer timer = new PacketTimer(-1);
        PacketTimerTask action = new PacketTimerTask(-1) {
            public void run() {
                if (fileAck == 0) {
                    try {
                        System.err.println("Failed To Send: " + "fileRequesr");
                        requestFile(fileName);
                        this.cancel();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        };
        timer.schedule(action, PacketsManager.WaitTime);
    }

    private void sendAKN(int sequenceNumber) throws Exception {
                System.err.println("CLIENT " + name + " sent AKN: " + sequenceNumber);
        Packet packet = new Packet(Packet.PacketType.AKN, null, sequenceNumber);
        byte[] packetBytes = packet.getPacketAsBytes();
        DatagramPacket sendPacket = new DatagramPacket(packetBytes, packetBytes.length,
                IPAddress, serverPortNumber);
        try {
            clientSocket.send(sendPacket);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void writePacket(Packet packet) throws Exception {
        int sequenceNumber = packet.getSequenceNumber();
        if (sequenceNumber == currentWriteSequence) {
            ++currentWriteSequence;
            fileBuffer.put(packet.getDataBytes());
            receivedSize += packet.getDataBytes().length;
            while (waitingSequences.contains(currentWriteSequence)) {
                int ind = waitingSequences.indexOf(currentWriteSequence);
                fileBuffer.put(waitingPackets.get(ind).getDataBytes());
                receivedSize += waitingPackets.get(ind).getDataBytes().length;
                waitingPackets.remove(ind);
                waitingSequences.remove(ind);
                ++currentWriteSequence;
            }
        } else {
            waitingSequences.add(packet.getSequenceNumber());
            waitingPackets.add(packet);
        }
        if (receivedSize == fileSize) {
            writeFile();
        }
    }

    private void writeFile() throws Exception {
        Files.write(Paths.get(path + fileName), fileBuffer.array());
        Thread.sleep(5);
        System.err.println("Client " + name + "  Wrote file to disk");
        Main.notifyFinished(name);
    }

    private void readDatagram(DatagramPacket datagramPacket) throws Exception {
        Packet packet = new Packet(datagramPacket.getData());
        String message = "CLIENT " + name + "  received ";
        Packet.PacketType packetType = packet.getPacketType();

        if (packetType == Packet.PacketType.FileAKN) {
            fileAck = 1;
            System.err.println(message + "FileAKN: " + fileName);
            fileSize = packet.getFileLength();
            fileArray = new byte[packet.getFileLength()];
            fileBuffer = ByteBuffer.wrap(fileArray);
        } else if (packetType == Packet.PacketType.DataPacket) {
            ///////////////////////////////////////////////////////////////////////////////////////
            int sequenceNumber = packet.getSequenceNumber();
                        packetCounter++;
            if (!packet.isValidPacket()) {
                System.err.println(message + "Corrupted Packet :" + packet.getSequenceNumber());
            }
            if (Networks.mode != Mode.GoBackN && packet.isValidPacket()) {
                receivedPackets.add(packet);
                Thread.sleep(3);
                 System.err.println(message + "DataPacket: " + packet.getDataBytes().length + ", " + packet.getSequenceNumber());
                    sendAKN(sequenceNumber);
                if (!receivedSequences.contains(sequenceNumber)) {
                    receivedSequences.add(sequenceNumber);
                    if (sequenceNumber == windowLow) {
                        while (receivedSequences.contains(windowLow)) {
                            ++windowLow;
                            ++windowHigh;
                        }
                    }                  
                    writePacket(packet);
                }
            } else if (Networks.mode == Mode.GoBackN && packet.isValidPacket()) {
                if (sequenceNumber == windowLow) {
                    System.err.println(message + "DataPacket: " + packet.getDataBytes().length + ", " + packet.getSequenceNumber());
                    receivedPackets.add(packet);                   
                    if(expectedSequenceNumber==packet.getSequenceNumber()){
                    sendAKN(packet.getSequenceNumber());
                    ++windowLow;
                    expectedSequenceNumber++;
                    }
                    if (!receivedSequences.contains(sequenceNumber)) {
                        receivedSequences.add(sequenceNumber);
                        writePacket(packet);
                    }
                } else if (windowLow - 1 > -1) {
                    sendAKN(windowLow - 1);
                }
            }
            ///////////////////////////////////////////////////////////////////////////////////////
        }
    }

    public int getPacketCounter() {
        return packetCounter;
    }

}
