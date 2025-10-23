
public class Main {
	public static void main(String[] args) {
		Client[] clients =  new Client[4];
		for(int i = 0; i < clients.length; i++) {
			clients[i] = new Client(i+1, "localhost", 5000);
		}
        new GUI(clients);
    }
}
