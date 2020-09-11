/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package transport.protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Rowan Salem
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    private static Map<String, Client> clients;
    private static Map<String, Long> timers;
    private static Map<String, Long> elapsed;
    private static int clientsWaiting;
    private static String serverIP = "";
    private static int serverPortNumber = 0;
    private static int clientPortNumber = 0;
    private static int maxServerWindowsize = 0;
    private static int clientWindowsize = 0;
    private static String fileName = "";
    private static long seedValue = 0;
    private static float PLP = 0;

    public static void main(String[] args) throws Exception {

        clients = new HashMap<String, Client>();
        timers = new HashMap<String, Long>();
        elapsed = new HashMap<String, Long>();
        clientsWaiting = 0;
                Networks.setMode(Networks.Mode.GoBackN );
        ReadServerFile();
    PacketsManager.setServerPath("/Users/RS/Desktop/Server/");
          PacketsManager.setPacketSize(512);
        PacketsManager.setDropRate(PLP);
        PacketsManager.setWaitTime(500);
        Server server = new Server("/Users/RS/Desktop/Server/", serverPortNumber, PLP, seedValue, maxServerWindowsize);
        server.start();

        ReadClientFile("InputFileClient1.Inform.txt");
        String client1Name = new String(Integer.toString(clientPortNumber));
        Client client1 = new Client(client1Name, "/Users/RS/Desktop/Client1/", clientPortNumber, serverPortNumber, clientWindowsize);
        timers.put(client1Name, System.currentTimeMillis());
        client1.start();
        client1.requestFile(fileName);
        clients.put(client1Name, client1);
        ++clientsWaiting;

    /*   ReadClientFile("InputFileClient2.Inform.txt");
        String client2Name = new String(Integer.toString(clientPortNumber));
        Client client2 = new Client(client2Name, "/Users/RS/Desktop/Client2/", clientPortNumber, serverPortNumber, clientWindowsize);
        timers.put(client2Name, System.currentTimeMillis());
        client2.start();
        client2.requestFile(fileName);
        clients.put(client2Name, client2);
        ++clientsWaiting;
        
               ReadClientFile("InputFileClient3.Inform.txt");
        String client3Name = new String(Integer.toString(clientPortNumber));
        Client client3 = new Client(client3Name, "/Users/RS/Desktop/Client3/", clientPortNumber, serverPortNumber, clientWindowsize);
        timers.put(client3Name, System.currentTimeMillis());
        client3.start();
        client3.requestFile(fileName);
        clients.put(client3Name, client3);
        ++clientsWaiting;*/
/*
           ReadClientFile("InputFileClient4.Inform.txt");
        String client4Name = new String(Integer.toString(clientPortNumber));
        Client client4 = new Client(client4Name, "/Users/RS/Desktop/Client4/", clientPortNumber, serverPortNumber, clientWindowsize);
        timers.put(client4Name, System.currentTimeMillis());
        client4.start();
        client4.requestFile(fileName);
        clients.put(client4Name, client4);
        ++clientsWaiting;
        
           ReadClientFile("InputFileClient5.Inform.txt");
        String client5Name = new String(Integer.toString(clientPortNumber));
        Client client5= new Client(client5Name, "/Users/RS/Desktop/Client5/", clientPortNumber, serverPortNumber, clientWindowsize);
        timers.put(client5Name, System.currentTimeMillis());
        client5.start();
        client5.requestFile(fileName);
        clients.put(client5Name, client5);
        ++clientsWaiting;
        */
        
    }

    public static void ReadServerFile() throws FileNotFoundException {
        try {
            FileInputStream stream = new FileInputStream("InputFileServer.Inform.txt");
            DataInputStream in = new DataInputStream(stream);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            serverPortNumber = Integer.parseInt(reader.readLine());
            maxServerWindowsize = Integer.parseInt(reader.readLine());
            seedValue = Integer.parseInt(reader.readLine());
            PLP = Float.parseFloat(reader.readLine());

            in.close();
        } catch (Exception e) {
            System.err.println("Error " + e.getMessage());
        }

    }

    public static void ReadClientFile(String clientFile) throws FileNotFoundException {
        try {
            FileInputStream stream = new FileInputStream(clientFile);
            DataInputStream in = new DataInputStream(stream);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            serverIP = reader.readLine();
            serverPortNumber = Integer.parseInt(reader.readLine());
            clientPortNumber = Integer.parseInt(reader.readLine());
            fileName = reader.readLine();
            clientWindowsize = Integer.parseInt(reader.readLine());
            in.close();
        } catch (Exception e) {
            System.err.println("Error " + e.getMessage());
        }

    }

    public static void notifyFinished(String name) throws InterruptedException {
        long lEndTime = System.currentTimeMillis();
        long difference = lEndTime - timers.get(name);
        elapsed.put(name, difference);
        clients.get(name).close();
        --clientsWaiting;

        if (clientsWaiting == 0) {
            Server.closeUdpServers();
            System.out.println("\n");
            String starsLine = "";
            for (int i = 0; i < 50; ++i) {
                starsLine += '*';
            }

            for (String key : elapsed.keySet()) {
                System.out.println("packet counter" + clients.get(key).getPacketCounter());
                float throughput = ((float) clients.get(key).getPacketCounter() * 512) / (float) elapsed.get(key);
                String line2 = "  " + key + " FINISHED with Elapsed milliseconds: "
                        + elapsed.get(key);
                String line3 = "  " + key + " FINISHED with throughput: "
                        + throughput + "   bytes/millisecond";
                System.out.println(starsLine);
                System.out.println(line2);
                System.out.println(line3);
                System.out.println(starsLine + "\n");
            }
            System.exit(0);
        }
    }

}
