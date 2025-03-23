package bits.dc;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class User {
    public static void main(String[] args) throws IOException {
        System.out.println("Hello world!");
        InetAddress serverAddress = InetAddress.getLocalHost();  // Replace with server address

        try (Socket socket = new Socket(serverAddress, Integer.parseInt(args[0]));
             BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
             OutputStream outputStream = socket.getOutputStream();
             DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
             InputStream inputStream = socket.getInputStream();
             DataInputStream dataInputStream = new DataInputStream(inputStream)){

            System.out.println("Enter the file name (type 'exit' to quit): ");

            String fileName;
            while ((fileName = reader.readLine()) != null && !fileName.equalsIgnoreCase("exit")) {

                System.out.println("Enter the file content: ");
                String fileContent = reader.readLine();
                // Send the input message to the server
                dataOutputStream.writeUTF(fileName);
                dataOutputStream.writeUTF(fileContent);

                // Read and print the server response
                String serverResponse = dataInputStream.readUTF();
                System.out.println("Server response: " + serverResponse);
                System.out.println("Enter the file name (type 'exit' to quit): ");

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}