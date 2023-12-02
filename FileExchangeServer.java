import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileExchangeServer {
    public static void main(String[] args) {
        int port = 5000;
        int maxClients = 10; // Maximum number of concurrent client connections
        ExecutorService clientPool = Executors.newFixedThreadPool(maxClients);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is listening on port " + port);

            while (true) {
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
}