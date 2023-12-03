import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileExchangeServer implements Runnable {
    private int port;
    private int maxClients;

    public FileExchangeServer(int port, int maxClients) {
        this.port = port;
        this.maxClients = maxClients;
    }

    @Override
    public void run() {
        ExecutorService clientPool = Executors.newFixedThreadPool(maxClients);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is listening on port " + port);

            while (!Thread.currentThread().isInterrupted()) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected");

                // Handle client in a separate thread managed by the thread pool
                clientPool.execute(new ClientHandler(socket));
            }

        } catch (Exception e) {
            System.out.println("Server exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (!clientPool.isShutdown()) {
                clientPool.shutdown();
            }
        }
    }

    // Main method for standalone running
    public static void main(String[] args) {
        FileExchangeServer server = new FileExchangeServer(5000, 10); // Example port and max clients
        new Thread(server).start();
    }
}