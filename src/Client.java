import java.io.*;
import java.net.*;

public class Client {
    private int id;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    boolean isOnline = true;
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
        	return resp;
        content = resp;
        return "READ_SUCCESS";
    }

    public boolean requestLock() {
        out.println("LOCK");
        String[] resp = waitForResponse().split(" ",2);
        if(resp[0].equals("LOCK_GRANTED")) {
        	if(resp.length > 1)
        		content = resp[1];
        	return true;
        }
        return false;   
    }

    public boolean renewLock() {
        out.println("RENEW");
        return waitForResponse().equals("LOCK_RENEWED");
    }

    public boolean writeRequest(String newContent) {
        out.println("WRITE " + newContent);
        if(waitForResponse().equals("WRITE_SUCCESS")) {
        	content = newContent;
        	return true;
        }
        return false;
    }

    public boolean releaseLock() {
    	out.println("RELEASE");
    	return waitForResponse().equals("RELEASE_SUCCESS");
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
