import java.io.*;
import java.net.*;

public class Client {
    private int id;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    String content;

    public Client(int id, String serverIP, int port) {
        this.id = id;
        try {
            socket = new Socket(serverIP, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            // Send client ID to server
            out.println(this.id);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public String readRequest() {
        out.println("READ");
        String resp = waitForResponse();
        if(resp.equals("READ_FAILED"))
        	return "READ_FAILED";
        content = resp;
        return "READ_SUCCESS" + content;
    }

    public boolean requestLock() {
        out.println("LOCK");
        return waitForResponse().equals("LOCK_GRANTED");
    }

    public boolean renewLock() {
        out.println("RENEW");
        return waitForResponse().equals("LOCK_RENEWED");
    }

    public boolean writeRequest(String newContent) {
        out.println("WRITE " + newContent);
        return waitForResponse().equals("WRITE_SUCCESSFUL");
    }

    public boolean releaseLock() {
    	out.println("RELEASE");
    	return waitForResponse().equals("RELEASE_SUCCESSFUL");
    }
    
    private String waitForResponse() {
        try {
            return in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
