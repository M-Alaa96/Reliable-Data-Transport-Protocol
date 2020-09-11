/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package transport.protocol;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;

/**
 *
 * @author Rowan Salem
 */
public class PacketsManager {
private static String serverPath;
    protected static int WaitTime;// timer in ms
    protected static int packetSize;// 3yzeno 512 bs howa 1024
    public static int windowSize;
    private static double dropRate;// drop rate of packet in precentage
    protected int overAllSize;
    protected int packetCount;// number of packets
    protected int windowHigh; // window high 2idy el yemeen
    protected int windowLow;
    protected LinkedList<PacketTimer> timers;// to handle timeout
    private byte[][] packetsBytes; // kaza packet kol packet fi row w el data fi el coloumns


         public PacketsManager(String filePath) throws IOException {
        Path path = Paths.get(serverPath.trim(),filePath.trim());
        byte[] allData = Files.readAllBytes(path);
        overAllSize = allData.length;
        int dataPacketHeaderSize = 18;
        int dataPacketDatSize = packetSize - dataPacketHeaderSize;
        packetCount = (int) Math.ceil(((double) overAllSize) / dataPacketDatSize);
        packetsBytes = new byte[packetCount][];
        int i;
        for(i = 0; i < packetCount - 1; ++i) {
            packetsBytes[i] = Arrays.copyOfRange(allData, i * dataPacketDatSize,
                    (i + 1) * dataPacketDatSize);
        }
        packetsBytes[i] = Arrays.copyOfRange(allData, i * dataPacketDatSize, overAllSize);
        if(Networks.mode == Networks.Mode.StopAndWait) {
            windowSize = 1;
        } else if(windowSize == 0) {
            windowSize = (int) Math.ceil(packetCount / 10);
        }
        System.out.println("packetCount="+packetCount);
        windowLow = 0;
        windowHigh = windowSize - 1;
        timers = new LinkedList<PacketTimer>();
    }

    public static void setServerPath(String serverPath) {
        PacketsManager.serverPath = serverPath;
    }

    public static void setWaitTime(int waitTime) {
        PacketsManager.WaitTime = waitTime;
    }

    public static void setDropRate(double dropRate) {
        PacketsManager.dropRate = dropRate;
    }

    public static void setPacketSize(int packetSize) {
        PacketsManager.packetSize = packetSize;
    }

    public static void setWindowSize(int windowSize) {
        PacketsManager.windowSize = windowSize;
    }

    protected boolean isNextExist() {
        return !((windowHigh == packetCount - 1));
    }

    protected byte[] getPacketAsBytes(int index) {
        return packetsBytes[index];
    }

    public int getOverAllSize() {
        return overAllSize;
    }

    public static int getWindowSize() {
        return windowSize;
    }

}
