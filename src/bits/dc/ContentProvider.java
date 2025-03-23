package bits.dc;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.IntStream;

public class ContentProvider {

    static String filePath = new File("").getAbsolutePath() + "\\ContentProviderList.txt";
    static List<String> nodeList;

    static {
        try {
            nodeList = Files.readAllLines(Path.of(filePath));
        } catch (IOException e) {
            System.out.println("Exception occurred while processing the node file. " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        try {


            Scanner scanner = new Scanner(System.in);

            int contentProviderID = getContentProviderID(scanner, nodeList.size());

            int hasToken = Integer.parseInt(nodeList.get(contentProviderID).split(" ")[3]);
            ClientProviderSite localSite = new ClientProviderSite(nodeList, contentProviderID, hasToken);

            MessageHandler listenToBroadcast = new MessageHandler(localSite, Integer.parseInt(nodeList.get(contentProviderID).split(" ")[0]));
            listenToBroadcast.start();

            String fileName = "";
            String fileContent = "";

            while (!fileName.equalsIgnoreCase("quit")) {
                System.out.println("Kindly enter the file name: ");
                fileName = scanner.nextLine();
                System.out.println("Kindly enter the file content: ");
                fileContent = scanner.nextLine();

                System.out.println("Content Provider -" + contentProviderID + " is trying to enter Critical Section");

                if (localSite.token == 1) {
                    executeAndExitCriticalSection(localSite, contentProviderID, nodeList, fileName, fileContent);
                } else {
                    requestAndExitCriticalSection(localSite, contentProviderID, nodeList, fileName, fileContent);
                }
            }
        } catch (Exception e) {
            System.out.println("Exception occurred while processing the main method. " + e.getMessage());
        }
    }

    private static int getContentProviderID(Scanner scanner, int nodeListSize) {
        int contentProviderID;
        while (true) {
            System.out.print("Enter Content Provider ID from (1 to " + nodeListSize + "): ");
            if (scanner.hasNextInt()) {
                contentProviderID = scanner.nextInt();
                if (contentProviderID >= 1 && contentProviderID <= nodeListSize) {
                    break;
                } else {
                    System.out.println("Invalid input! Please enter Content Provider ID from (1 to " + nodeListSize + "): ");
                }
            } else {
                System.out.println("Invalid input! Please enter Content Provider ID from (1 to " + nodeListSize + "): ");
                scanner.next();
            }
        }
        return contentProviderID;
    }

    private static void executeAndExitCriticalSection(ClientProviderSite localSite, int contentProviderID, List<String> nodeList, String fileName, String fileContent) throws InterruptedException {
        executeCriticalSection(localSite, contentProviderID, fileName, fileContent);
        exitCS(localSite, nodeList, contentProviderID);
    }

    private static void requestAndExitCriticalSection(ClientProviderSite localSite, int contentProviderID, List<String> nodeList, String fileName, String fileContent) throws InterruptedException {
        requestAndExecuteCriticalSection(localSite, contentProviderID, fileName, fileContent);
        exitCS(localSite, nodeList, contentProviderID);
    }

    private static void executeCriticalSection(ClientProviderSite localSite, int contentProviderID, String fileName, String fileContent) throws InterruptedException {
        localSite.processingCS = 1;
        System.out.println("Content Provider -" + contentProviderID + " has token. Executing in the Critical Section.....");
        writeToFile(fileName, fileContent);
        Thread.sleep(10000);
        localSite.processingCS = 0;
        System.out.println("Content Provider -" + contentProviderID + " is exiting Critical Section.");
    }

    private static void writeToFile(String fileName, String fileContent) {
        String[] serverKeys = nodeList.getFirst().split(" ");
        try (Socket socket = new Socket(serverKeys[1], Integer.parseInt(serverKeys[2]));
             OutputStream outputStream = socket.getOutputStream();
             DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
             InputStream inputStream = socket.getInputStream();
             DataInputStream dataInputStream = new DataInputStream(inputStream)) {

            // Send the input message to the server
            dataOutputStream.writeUTF(fileName);
            dataOutputStream.writeUTF(fileContent);

            // Read and print the server response
            String serverResponse = dataInputStream.readUTF();
            System.out.println("Server response: " + serverResponse);

        } catch (IOException e) {
            System.out.println(" Exception occurred while processing the executeCriticalSection method. " + e.getMessage());
        }
    }

    private static void requestAndExecuteCriticalSection(ClientProviderSite localSite, int contentProviderID, String fileName, String fileContent) throws InterruptedException {
        System.out.println("Content Provider -" + contentProviderID + " doesn't have token. So Content Provider -" + contentProviderID + " is requesting token");
        localSite.requestCriticalSection();
        System.out.println("Content Provider -" + contentProviderID + " is waiting for token.");
        localSite.processingCS = 1;

        while (localSite.token == 0) {
            Thread.sleep(2000);
        }

        System.out.println("Content Provider -" + contentProviderID + " has received token. Executing in Critical Section.....");
        writeToFile(fileName, fileContent);
        Thread.sleep(10000);
        localSite.processingCS = 0;
        System.out.println("Content Provider -" + contentProviderID + " is exiting Critical Section.");
    }

    public static void exitCS(ClientProviderSite localSite, List<String> nodeList, int siteNumber) {

        localSite.LN[siteNumber - 1] = localSite.RN[siteNumber - 1];

        // Send updated LN value to all sites
        String message = "ln," + siteNumber + "," + localSite.LN[siteNumber - 1];

        IntStream.range(1, nodeList.size() + 1)
                .filter(i -> i != siteNumber)
                .forEach(i -> {
                    String[] contentProviderKey = nodeList.get(i).split(" ");
                    try (Socket socket = new Socket(contentProviderKey[1], Integer.parseInt(contentProviderKey[2]));
                         OutputStream outputStream = socket.getOutputStream();
                         OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
                         BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter)) {
                        bufferedWriter.write(message);
                        bufferedWriter.flush();
                    } catch (IOException e) {
                        System.out.println(" Exception occurred while processing the exitCS method. " + e.getMessage());
                    }
                });

        IntStream.range(1, nodeList.size() + 1)
                .filter(i -> localSite.RN[i] == localSite.LN[i] + 1)
                .filter(i -> !localSite.tokenQueue.contains(i + 1))
                .forEach(i -> localSite.tokenQueue.add(i + 1));

        if (!localSite.tokenQueue.isEmpty()) {
            localSite.sendToken(localSite.tokenQueue.poll());
        }
    }

}

class ClientProviderSite {
    List<String> nodeList = new ArrayList<>();
    int siteNumber = 0;
    int token = 0;
    int processingCS = 0;
    Queue<Integer> tokenQueue = new LinkedList<>();
    int[] RN;
    int[] LN;

    ClientProviderSite(List<String> nodeList, int siteNumber, int hasToken) {
        this.siteNumber = siteNumber;
        this.token = hasToken;
        this.nodeList.addAll(nodeList);

        RN = new int[nodeList.size()];
        LN = new int[nodeList.size()];
        for (int i = 1; i <= nodeList.size(); i++) {
            RN[i] = 0;
            LN[i] = 0;
        }
    }

    void updateLN(int thisSite, int value) {
        LN[thisSite - 1] = value;
    }

    void requestCriticalSection() {
        RN[siteNumber - 1]++;
        String message = "request," + siteNumber + "," + RN[siteNumber - 1];
        System.out.println("Broadcasting request to other " + (nodeList.size() - 2) + " Content Providers : ");


        IntStream.range(1, nodeList.size() + 1)
                .filter(i -> i != siteNumber)
                .forEach(i -> {
                    String[] contentProviderKeys = nodeList.get(i).split(" ");
                    try (Socket socket = new Socket(contentProviderKeys[1], Integer.parseInt(contentProviderKeys[2]));
                         OutputStream outputStream = socket.getOutputStream();
                         OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
                         BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter)) {
                        System.out.println("Broadcasting to the Content Provider " + siteNumber + " with port :" + socket.getPort());
                        bufferedWriter.write(message);
                    } catch (Exception e) {
                        System.out.println(" Exception occurred while processing the requestCriticalSection method. " + e.getMessage());
                    }
                });

    }

    void processCriticalSectionReq(int siteNumber, int sequenceNumber) {
        if (RN[siteNumber - 1] < sequenceNumber) {
            RN[siteNumber - 1] = sequenceNumber;
        }

        if (processingCS == 0 && token == 1) {
            sendToken(siteNumber);

        } else {
            tokenQueue.add(siteNumber);
        }

    }

    void sendToken(int site) {

        if (this.token == 1) {
            if (RN[site - 1] == LN[site - 1] + 1) {
                System.out.println("Sending token to Content provider " + site);
                String[] contentProviderKeys = nodeList.get(site).split(" ");
                try (Socket socket = new Socket(contentProviderKeys[1], Integer.parseInt(contentProviderKeys[2]));
                     OutputStream outputStream = socket.getOutputStream();
                     OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
                     BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter)) {

                    StringBuilder message = new StringBuilder("token");
                    for (int i = 0; i < tokenQueue.size(); i++) {
                        message.append(",").append(tokenQueue.poll());
                    }

                    bufferedWriter.write(message.toString());
                    bufferedWriter.flush();
                    this.token = 0;
                } catch (Exception e) {
                    System.out.println("Exception occurred in sendToken. " + e.getMessage());
                }
            }
        }

    }
}

class MessageHandler extends Thread {

    int port;
    ClientProviderSite localSite;

    public MessageHandler(ClientProviderSite thisSite, int port) {
        this.port = port;
        this.localSite = thisSite;
    }

    public void run() {

        while (true) {
            try (ServerSocket serverSocket = new ServerSocket(port);
                 Socket socket = serverSocket.accept()) {
                new MessageProcessor(socket, localSite).start();
            } catch (Exception e) {
                System.out.println("Exception occurred in MessageHandler while message broadcast. " + e.getMessage());
            }
        }


    }

}

class MessageProcessor extends Thread {

    Socket socket;
    ClientProviderSite localSite;

    public MessageProcessor(Socket socket, ClientProviderSite site) {
        this.socket = socket;
        this.localSite = site;
    }

    public void run() {

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            String command = bufferedReader.readLine();

            if (command != null) {
                String[] message = command.split(",");


                if (command.startsWith("request")) {
                    int siteNumber = Integer.parseInt(message[1]);
                    int seqNumber = Integer.parseInt(message[2]);
                    System.out.println("Content Provider -" + siteNumber + " has requested for Critical Section");
                    localSite.processCriticalSectionReq(siteNumber, seqNumber);
                }

                if (command.startsWith("token")) {
                    localSite.tokenQueue.clear();
                    for (int i = 1; i < message.length; i++) {
                        localSite.tokenQueue.add(Integer.parseInt(message[i]));
                    }
                    localSite.token = 1;
                }

                if (command.startsWith("ln")) {
                    int siteNumber = Integer.parseInt(message[1]);
                    int seqNumber = Integer.parseInt(message[2]);
                    System.out.println("Content Provider -" + siteNumber + " has left the Critical Section");
                    localSite.updateLN(siteNumber, seqNumber);
                }
            }

        } catch (Exception e) {
            System.out.println("Exception occurred in MessageProcessor. " + e.getMessage());
        }
    }
}