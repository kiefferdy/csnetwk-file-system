    import javax.swing.*;
    import java.awt.*;
    import java.awt.event.ActionEvent;
    import java.awt.event.ActionListener;
    import java.io.*;
    import java.net.Socket;
    import java.util.List;


    public class Client {

        private JFrame frame;
        private JTextField commandInputField;
        private JTextArea responseArea;
        private JButton sendButton;
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String separator = "-------------------------------------------------------------------------------------------\n";


        public Client() {
            initializeGUI();
        }

        private void initializeGUI() {
            frame = new JFrame("Client");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(400, 300);
            frame.setLayout(new BorderLayout());

            commandInputField = new JTextField();
            responseArea = new JTextArea();
            responseArea.setEditable(false);  // Make the responseArea non-editable
            sendButton = new JButton("Send");

            frame.add(commandInputField, BorderLayout.NORTH);
            frame.add(new JScrollPane(responseArea), BorderLayout.CENTER);
            frame.add(sendButton, BorderLayout.SOUTH);

            sendButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    sendCommand();
                }
            });

            frame.setVisible(true);
        }

        private void connectToServer(String hostname, int port) throws IOException {
            socket = new Socket(hostname, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Start a new thread to listen for messages from the server
            new Thread(() -> {
                try {
                    String response;
                    while ((response = in.readLine()) != null) {
                        final String resp = response;
                        SwingUtilities.invokeLater(() -> {
                            responseArea.append(resp + "\n");
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }

        private void sendCommand() {
            String command = commandInputField.getText().trim();

            if (command.startsWith("/join ")) {
                String[] parts = command.split(" ");
                if (parts.length == 3) {
                    try {
                        connectToServer(parts[1], Integer.parseInt(parts[2]));
                        responseArea.append("Connected to server at " + parts[1] + ":" + parts[2] + "\n" + separator);
                    } catch (IOException e) {
                        responseArea.append("Failed to connect to server: " + e.getMessage() + "\n"  + separator);
                    }
                }
            } else if (command.equals("/leave")) {
                try {
                    if (socket != null && !socket.isClosed()) {
                        out.println("/leave"); // Inform the server of the intention to disconnect
                        socket.close();
                        responseArea.append("Disconnected from the server.\n" + separator);
                    }
                } catch (IOException e) {
                    responseArea.append("Error while disconnecting: " + e.getMessage() + "\n" + separator);
                }
            } else if (command.equals("/?")) {
                printHelp();
        } else if (command.equals("/dir")) {
            out.println(command); // Send the /dir command to the server

            SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
                @Override
                protected Void doInBackground() throws Exception {
                    try {
                        String serverResponse;
                        while (!(serverResponse = in.readLine()).equals("END_OF_DIR")) {
                            publish(serverResponse + "\n");
                        }
                    } catch (IOException e) {
                        publish("Error receiving directory listing: " + e.getMessage() + "\n");
                    }
                    return null;
                }

                @Override
                protected void process(List<String> chunks) {
                    for (String message : chunks) {
                        responseArea.append(message);
                    }
                }
            };
            worker.execute();
        } else if (command.startsWith("/store ")) {
                String filename = command.substring(7);
                out.println(command);

                SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        try {
                            String serverResponse = in.readLine();
                            if ("START".equals(serverResponse)) {
                                publish("Starting file transfer for '" + filename + "'...\n");
                                sendFile(filename);
                                serverResponse = in.readLine();
                                if ("END".equals(serverResponse)) {
                                    publish("File transfer completed.\n");
                                }
                            } else {
                                publish("Server response: " + serverResponse + "\n");
                            }
                        } catch (IOException e) {
                            publish("Error in file transfer: " + e.getMessage() + "\n");
                        }
                        return null;
                    }

                    @Override
                    protected void process(List<String> chunks) {
                        for (String message : chunks) {
                            responseArea.append(message);
                        }
                    }
                };
                worker.execute();
            } else if (command.startsWith("/register ")) {
                out.println(command); // Send the /register command to the server
                // You can add additional code here if you want to handle the response from the server
            } else if (command.startsWith("/get ")) {
                String filename = command.substring(5);
                out.println(command);

                SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        try {
                            if ("START".equals(in.readLine())) {
                                receiveFile(filename);
                                if ("END".equals(in.readLine())) {
                                    publish("File received.\n");
                                }
                            }
                        } catch (IOException e) {
                            publish("Error receiving file: " + e.getMessage() + "\n");
                        }
                        return null;
                    }

                    @Override
                    protected void process(List<String> chunks) {
                        for (String message : chunks) {
                            responseArea.append(message);
                        }
                    }
                };
                worker.execute();
            } else {
                    // Check if the command is not empty
                    if (!command.isEmpty()) {
                        JOptionPane.showMessageDialog(frame, 
                            "That is not a valid command. Use '/?' to view valid commands.",
                            "Invalid Command", JOptionPane.ERROR_MESSAGE);
                    }
                }

            commandInputField.setText("");
        }


        private void printHelp() {
            responseArea.append("Available commands: \n");
            responseArea.append("/join <server_ip> <port> - Connect to the server.\n");
            responseArea.append("/leave - Disconnect from the server.\n");
            responseArea.append("/register <handle> - Register your handle.\n");
            responseArea.append("/store <filename> - Send a file.\n");
            responseArea.append("/get <filename> - Fetch a file.\n");
            responseArea.append("/dir - List files in the directory.\n");
            responseArea.append("/? - Show this help message.\n" + separator);
        }

        private void sendFile(String filename) {
            File file = new File(filename);
            if (file.exists()) {
                long fileSize = file.length();
                try (FileInputStream fileIn = new FileInputStream(file);
                    DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream())) {
                    // Send the file size first
                    dataOut.writeLong(fileSize);
                    dataOut.flush();

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fileIn.read(buffer)) != -1) {
                        dataOut.write(buffer, 0, bytesRead);
                    }
                    dataOut.flush(); // Ensure the file data is sent immediately
                    responseArea.append("File sent.\n");
                } catch (IOException e) {
                    responseArea.append("Error sending file: " + e.getMessage() + "\n");
                }
            } else {
                responseArea.append("File not found: " + filename + "\n");
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
                responseArea.append("File received.\n");
            } catch (IOException e) {
                responseArea.append("Error receiving file: " + e.getMessage() + "\n");
            }
        }

        public static void main(String[] args) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    new Client();
                }
            });
        }
    }