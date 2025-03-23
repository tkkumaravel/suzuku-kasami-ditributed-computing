package bits.dc;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

public class Server {
    //private HashMap<String, String> files = new HashMap<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(Integer.parseInt(args[0]))) {
            System.out.println("Server started at port " + Integer.parseInt(args[0]));


            while (true) {
                // Wait for a client connection
                try (Socket clientSocket = serverSocket.accept();
                     InputStream inputStream = clientSocket.getInputStream();
                     DataInputStream dataInputStream = new DataInputStream(inputStream);
                     OutputStream outputStream = clientSocket.getOutputStream();
                     DataOutputStream dataOutputStream = new DataOutputStream(outputStream)) {

                    System.out.println("Client connected: IP- " + clientSocket.getInetAddress() + " Port- " + clientSocket.getPort());

                    // Receive file name and content from the client
                    String filename = dataInputStream.readUTF();
                    String content = dataInputStream.readUTF();

                    // Process the file (e.g., save it to disk, perform operations)
                    System.out.println("Received file: " + filename);
                    // System.out.println("Content: " + content);

                    writeToFile(filename, content);

                    // Send a response back to the client
                    dataOutputStream.writeUTF("File '" + filename + "' received and stored in the server successfully");

                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void write(String filename, String content) {
        System.out.println("Content Provider writing file: " + filename);
        //files.put(filename, content);
        writeToFile(filename, content);
    }

    private static void writeToFile(String fileName, String content) {
        try {
            Files.write(Path.of(fileName), content.getBytes());
            System.out.println("File " + fileName + " written to server.");
        } catch (IOException e) {
            System.err.println("Error writing file: " + fileName + e.getMessage());
        }
    }
}