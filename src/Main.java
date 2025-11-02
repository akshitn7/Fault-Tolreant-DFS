import java.util.Scanner;
public class Main {
	public static void main(String[] args) {
		Scanner sc = new Scanner(System.in);
		Client[] clients =  new Client[4];
		String serverIP;
		System.out.print("Enter the server IP: ");
		serverIP = sc.next();
		for(int i = 0; i < clients.length; i++) {
			clients[i] = new Client(i+1, serverIP, 5000);
		}
        new GUI(clients);
    }
}
