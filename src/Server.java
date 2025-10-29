import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private int port = 5000;
    private String content = "";
    private boolean isLocked = false;
    private int lockedBy = -1;
    private long lockExpiryTime = 0;
    private Map<Integer, ClientHandler> clients = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> lockCheckerTask; // <-- handle to scheduled lock checker

    public static void main(String[] args) {
        new Server().startServer();
    }

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket);
                handler.start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Called by the scheduled task
    private synchronized void checkLockExpiry() {
        if (isLocked && System.currentTimeMillis() > lockExpiryTime) {
            System.out.println("Lock expired for client " + lockedBy);
            isLocked = false;
            lockedBy = -1;

            // Cancel the scheduled task since lock is gone
            if (lockCheckerTask != null) {
                lockCheckerTask.cancel(false);
                lockCheckerTask = null;
            }
        }
    }

    private synchronized String handleRead() {
        return !isLocked ? content : "READ_FAILED";
    }

    private synchronized boolean handleLock(int clientId) {
        if (!isLocked) {
            isLocked = true;
            lockedBy = clientId;
            lockExpiryTime = System.currentTimeMillis() + 30_000; // 30s

            // Start lock expiry timer
            if (lockCheckerTask == null || lockCheckerTask.isCancelled()) {
                lockCheckerTask = scheduler.scheduleAtFixedRate(
                    this::checkLockExpiry, 1, 1, TimeUnit.SECONDS
                );
            }
            return true;
        }
        return false;
    }

    private synchronized boolean handleRenew(int clientId) {
        if (isLocked && lockedBy == clientId) {
            lockExpiryTime = System.currentTimeMillis() + 30_000; // extend 30s
            return true;
        }
        return false;
    }

    private synchronized String handleWrite(int clientId, String newContent) {
        if (isLocked && lockedBy == clientId) {
            content = newContent;        
            return "WRITE_SUCCESS";
        }
        return "WRITE_FAILED";
    }
    
    private synchronized boolean handleRelease(int clientId) {
    	if (isLocked && lockedBy == clientId) {
            isLocked = false;
            lockedBy = -1;
            // Stop lock expiry timer since lock is released
            if (lockCheckerTask != null) {
                lockCheckerTask.cancel(false);
                lockCheckerTask = null;
            }
            return true;
        }
    	System.out.println("[DEBUG] Lock released by client " + clientId);
        return false;
    }

    private class ClientHandler extends Thread {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private int clientId = -1;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void sendMessage(String msg) {
            out.println(msg);
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // First message from client should be its ID
                clientId = Integer.parseInt(in.readLine());
                clients.put(clientId, this);
                System.out.println("Client " + clientId + " connected.");

                String line;
                while ((line = in.readLine()) != null) {
                    String[] parts = line.split(" ", 2);
                    String cmd = parts[0];

                    switch (cmd) {
                        case "READ":
                            sendMessage(handleRead());
                            break;
                        case "LOCK":
                            boolean granted = handleLock(clientId);
                            sendMessage(granted ? "LOCK_GRANTED " + content : "LOCK_DENIED");
                            break;
                        case "RENEW":
                            boolean renewed = handleRenew(clientId);
                            sendMessage(renewed ? "LOCK_RENEWED" : "LOCK_RENEW_FAILED");
                            break;
                        case "WRITE":
                            if (parts.length > 1) {
                                sendMessage(handleWrite(clientId, parts[1]));
                            }
                            break;
                        case "RELEASE":
                        	boolean released = handleRelease(clientId);
                        	sendMessage(released ? "RELEASE_SUCCESS" : "RELEASE_FAILED");
                        	break;
                        default:
                            sendMessage("UNKNOWN_COMMAND");
                    }
                }

            } catch (IOException e) {
                System.out.println("Client " + clientId + " disconnected.");
            } finally {
                try {
                    clients.remove(clientId);
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
