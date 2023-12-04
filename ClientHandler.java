import java.io.*;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ClientHandler extends Thread {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private static final Set<String> registeredUsers = Collections.synchronizedSet(new HashSet<>());
    private boolean isRegistered = false;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            System.out.println("Error setting up streams: " + e.getMessage());
        }
    }

    public void run() {
        try {
            String clientCommand;
            while ((clientCommand = reader.readLine()) != null) {
                handleClientCommand(clientCommand);
            }
        } catch (IOException e) {
            System.out.println("Client handler exception: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Error closing socket: " + e.getMessage());
            }
        }
    }

    private void handleClientCommand(String clientCommand) {
        if (clientCommand.startsWith("/register ")) {
            String handle = clientCommand.substring(10).trim();
            synchronized (registeredUsers) {
                if (!registeredUsers.contains(handle)) {
                    registeredUsers.add(handle);
                    writer.println("Handle registered: " + handle);
                    isRegistered = true; // Set the flag to true on successful registration
                } else {
                    writer.println("Error: Handle already exists");
                }
            }
        } else if (!isRegistered) {
            writer.println("Error: You must register first with /register <handle>");
        } else {
            if (clientCommand.startsWith("/store ")) {
                String filename = clientCommand.substring(7); // Extract filename
                writer.println("START"); // Signal to start file transfer
                writer.flush(); // Ensure the message is sent immediately
                System.out.println("Server: Sending START signal for file transfer."); // Debugging
                receiveFile(filename);
                writer.println("END"); // Signal end of file transfer
                writer.flush(); // Ensure the message is sent immediately
                System.out.println("Server: Sending END signal for file transfer."); // Debugging
            }
            if (clientCommand.equals("/dir")) {
                File dir = new File("./files"); // Current directory
                File[] filesList = dir.listFiles();
                if (filesList != null) {
                    for (File file : filesList) {
                        if (file.isFile()) {
                            writer.println(file.getName());
                        }
                    }
                }
            }
            if (clientCommand.startsWith("/get ")) {
                String filename = clientCommand.substring(5); // Extract filename
                sendFile(filename);
            }
        }
    }

    private void sendFile(String filename) {
        File file = new File(filename);
        if (file.exists()) {
            try (FileInputStream fileIn = new FileInputStream(file);
                 DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream())) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fileIn.read(buffer)) != -1) {
                    dataOut.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                System.out.println("Error sending file: " + e.getMessage());
            }
        } else {
            writer.println("Error: File not found");
        }
    }

    private void receiveFile(String filename) {
        File directory = new File("files");
        if (!directory.exists()) {
            directory.mkdir(); 
        }
    
        File file = new File(directory, filename);
        try (FileOutputStream fileOut = new FileOutputStream(file);
             DataInputStream dataIn = new DataInputStream(socket.getInputStream())) {
            // Read the file size first
            long fileSize = dataIn.readLong();
            long totalBytesRead = 0;
            byte[] buffer = new byte[4096];
            int bytesRead;
            while (totalBytesRead < fileSize && (bytesRead = dataIn.read(buffer)) != -1) {
                fileOut.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
            }
            System.out.println("Server: File received and saved."); // Debugging
        } catch (IOException e) {
            System.out.println("Error receiving file: " + e.getMessage());
        }
    }     
}