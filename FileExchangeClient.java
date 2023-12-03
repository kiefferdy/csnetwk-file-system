import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;


public class FileExchangeClient {
    private Socket socket;
    private PrintWriter serverWriter;
    private BufferedReader serverReader;
    private ServerListener serverListener;


    // Connect to the server
    public void connectToServer(String serverIp, int port, Consumer<String> onResponseReceived, Runnable onConnectionSuccess, Runnable onConnectionFailure) {
        new Thread(() -> {
            try {
                socket = new Socket(serverIp, port);
                serverWriter = new PrintWriter(socket.getOutputStream(), true);

                // Start listening to server responses
                serverListener = new ServerListener(socket, onResponseReceived);
                serverListener.start();

                onConnectionSuccess.run();
            } catch (IOException e) {
                onConnectionFailure.run();
            }
        }).start();
    }

    // Disconnect from the server
    public void disconnectFromServer(Runnable onDisconnection) {
        new Thread(() -> {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
                onDisconnection.run();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // Send a command to the server
    public void sendCommand(String command, Runnable onCommandSent) {
        new Thread(() -> {
            if (serverWriter != null) {
                serverWriter.println(command);
                onCommandSent.run();
            }
        }).start();
    }

    // Read a response from the server
    public void readResponse(Consumer<String> onResponseReceived) {
        new Thread(() -> {
            try {
                String response;
                while ((response = serverReader.readLine()) != null) {
                    onResponseReceived.accept(response);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void sendFile(String filename, Runnable onFileSent) {
        new Thread(() -> {
            try {
                // Implement file sending logic
                File file = new File(filename);
                if (file.exists()) {
                    serverWriter.println("/store " + filename);
                    // Read the server's response (expecting "START")
                    if ("START".equals(serverReader.readLine())) {
                        FileInputStream fileIn = new FileInputStream(file);
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = fileIn.read(buffer)) != -1) {
                            socket.getOutputStream().write(buffer, 0, bytesRead);
                        }
                        socket.getOutputStream().flush();
                        fileIn.close();
                    }
                }
                onFileSent.run();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // Receive a file from the server
    public void getFile(String filename, Runnable onFileReceived) {
        new Thread(() -> {
            try {
                // Implement file receiving logic
                serverWriter.println("/get " + filename);
                // Read the server's response (expecting "START")
                if ("START".equals(serverReader.readLine())) {
                    FileOutputStream fileOut = new FileOutputStream(filename);
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = socket.getInputStream().read(buffer)) != -1) {
                        fileOut.write(buffer, 0, bytesRead);
                    }
                    fileOut.close();
                }
                onFileReceived.run();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
    // ... other methods ...
}