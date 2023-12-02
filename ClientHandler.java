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
        synchronized (registeredUsers) {
            if (clientCommand.startsWith("/register ")) {
                String handle = clientCommand.substring(10).trim();
                if (!registeredUsers.contains(handle)) {
                    registeredUsers.add(handle);
                    writer.println("Handle registered: " + handle);
                } else {
                    writer.println("Error: Handle already exists");
                }
            }
        }

        if (clientCommand.startsWith("/store ")) {
            String filename = clientCommand.substring(7); // Extract filename
            writer.println("START"); // Signal to start file transfer
            receiveFile(filename);
            writer.println("END"); // Signal end of file transfer
        }

        if (clientCommand.equals("/dir")) {
            File dir = new File("."); // Current directory
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

        // ... other command handling logic ...
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
        try (FileOutputStream fileOut = new FileOutputStream(filename);
             DataInputStream dataIn = new DataInputStream(socket.getInputStream())) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = dataIn.read(buffer)) != -1) {
                fileOut.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            System.out.println("Error receiving file: " + e.getMessage());
        }
    }
}