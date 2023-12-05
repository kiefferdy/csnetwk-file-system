import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private JFrame frame;
    private JTextArea logArea;
    private JButton startServerButton;
    private ServerSocket serverSocket;
    private boolean isServerRunning = false;

    public Server() {
        frame = new JFrame("Server");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setLayout(new BorderLayout());

        logArea = new JTextArea();
        logArea.setEditable(false);
        startServerButton = new JButton("Start Server");

        startServerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isServerRunning) {
                    startServer();
                } else {
                    stopServer();
                }
            }
        });

        frame.add(new JScrollPane(logArea), BorderLayout.CENTER);
        frame.add(startServerButton, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    public void notifyClientDisconnected(Socket socket) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("Client disconnected: " + serverSocket.getInetAddress().getHostAddress() + "\n");
        });
    }

    private void startServer() {
        try {
            serverSocket = new ServerSocket(5000);
            isServerRunning = true;
            startServerButton.setText("Stop Server");
            logArea.append("Server started on port 5000\n");
            
            Thread serverThread = new Thread(() -> {
                try {
                    while (!serverSocket.isClosed()) {
                        Socket socket = serverSocket.accept();
                        logArea.append("New client connected: " + socket.getInetAddress().getHostAddress() + "\n");

                        // Instantiate ClientHandler for each connected client
                        new Thread(new ClientHandler(socket, this)).start();
                    }
                } catch (IOException e) {
                    logArea.append("Error accepting client connection: " + e.getMessage() + "\n");
                }
            });
            serverThread.start();

        } catch (IOException e) {
            logArea.append("Error starting server: " + e.getMessage() + "\n");
        }
    }

    private void stopServer() {
        try {
            if (!serverSocket.isClosed()) {
                serverSocket.close();
            }
            isServerRunning = false;
            startServerButton.setText("Start Server");
            logArea.append("Server stopped.\n");
        } catch (IOException e) {
            logArea.append("Error stopping server: " + e.getMessage() + "\n");
        }
    }

    public void updateLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new Server();
            }
        });
    }
}