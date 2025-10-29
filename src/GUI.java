import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class GUI extends JFrame implements ActionListener {
	
    Client[] clients;
    //File content Area
    JTextArea fileContentArea;
    
    //Text Editor Area
    JTextArea editorArea;

    //Log Area 
    JTextArea logArea;
    
    //Client components
    JButton[] readButtons = new JButton[4];
    JButton[] requestLockButtons = new JButton[4];
    JButton[] renewButtons = new JButton[4];
    JButton[] writeButtons = new JButton[4];
    JButton[] releaseButtons = new JButton[4];
    JButton[] crashButtons = new JButton[4];

    public GUI(Client[] clients) {
    	//Client Initialization
        this.clients = clients;
        //Frame Setup
        setTitle("Fault Tolerant File System Simulation");
        setSize(1200, 800);
        setLayout(new BorderLayout(10, 10));

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));


        //File State Panel
        JPanel fileStatePanel = new JPanel(new BorderLayout());
        fileStatePanel.setBorder(BorderFactory.createTitledBorder("File State (Latest Update)"));
        fileContentArea = new JTextArea("", 4, 20);
        fileContentArea.setEditable(false);
        fileStatePanel.add(new JScrollPane(fileContentArea), BorderLayout.CENTER);
        
        //Editor Panel
        JPanel editorPanel = new JPanel(new BorderLayout());
        editorPanel.setBorder(BorderFactory.createTitledBorder("Client Editor"));
        editorArea = new JTextArea("", 4, 20);
        editorArea.setEditable(false);
        editorPanel.add(new JScrollPane(editorArea), BorderLayout.CENTER);
        
        //Log Area Panel
        JPanel logAreaPanel = new JPanel(new BorderLayout());
        logAreaPanel.setBorder(BorderFactory.createTitledBorder("Logs"));
        logArea = new JTextArea("", 4, 20);
        logArea.setEditable(false);
        logAreaPanel.add(new JScrollPane(logArea), BorderLayout.CENTER);

        //Clients Panel
        JPanel clientsGridPanel = new JPanel(new GridLayout(1, 4, 10, 10));
        clientsGridPanel.setBorder(BorderFactory.createTitledBorder("Clients"));

        for (int i = 0; i < 4; i++) {
            JPanel clientPanel = new JPanel(new BorderLayout(5, 5));
            clientPanel.setBorder(BorderFactory.createTitledBorder("Client " + (i + 1)));


            JPanel buttonPanel = new JPanel(new GridLayout(4, 1, 5, 5));
            readButtons[i] = new JButton("Read File");
            requestLockButtons[i] = new JButton("Request Lock");
            renewButtons[i] = new JButton("Renew Lock");
            renewButtons[i].setEnabled(false);
            writeButtons[i] = new JButton("Write");
            writeButtons[i].setEnabled(false);
            releaseButtons[i] = new JButton("Release");
            releaseButtons[i].setEnabled(false);
            crashButtons[i] = new JButton("Simulate Crash");

            buttonPanel.add(readButtons[i]);
            buttonPanel.add(requestLockButtons[i]);
            buttonPanel.add(renewButtons[i]);
            buttonPanel.add(writeButtons[i]);
            buttonPanel.add(releaseButtons[i]);
            buttonPanel.add(crashButtons[i]);

            readButtons[i].addActionListener(this);
            requestLockButtons[i].addActionListener(this);
            renewButtons[i].addActionListener(this);
            writeButtons[i].addActionListener(this);
            releaseButtons[i].addActionListener(this);
            crashButtons[i].addActionListener(this);

            clientPanel.add(buttonPanel, BorderLayout.SOUTH);
            clientsGridPanel.add(clientPanel);
        }

        //Add Panels to the Main Panel
        mainPanel.add(fileStatePanel);
        mainPanel.add(editorPanel);
        mainPanel.add(logAreaPanel);
        mainPanel.add(clientsGridPanel);
        add(mainPanel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();

        //Check for client button clicks
        for (int i = 0; i < 4; i++) {
            String clientName = "Client" + (i + 1);
            
            //Read Button : add log, read request, response from server
            if (source == readButtons[i]) {
            	logArea.append(clientName + " request to read the file.\n");
                String resp = clients[i].readRequest();
                logArea.append(resp + "\n");
                if(resp.equals("READ_SUCCESS")) {
                	fileContentArea.setText(clients[i].content);
                }
            } 
            //Request Lock Button: add log, request for lock, if granted enable write button.
            else if (source == requestLockButtons[i]) {
                logArea.append(clientName + " requested a lock.\n");
                boolean granted = clients[i].requestLock();
                if(granted) {
                	logArea.append(clientName + " was granted lock.\n");
                	editorArea.setText(clients[i].content);
                	editorArea.setEditable(true);
                	requestLockButtons[i].setEnabled(false);
                	releaseButtons[i].setEnabled(true);
                } else {
                		logArea.append(clientName + " was denied lock.\n");
                }
                renewButtons[i].setEnabled(granted);
                writeButtons[i].setEnabled(granted);
            } 
            //Renew Button
            else if(source == renewButtons[i]){
            	boolean renewed = clients[i].renewLock();
            	if(renewed) {
            		logArea.append(clientName + " lock was renewed.\n");
            	} else {
            		logArea.append(clientName + " lock renew failed.\n");
            	}
            	
            }
            //Write Button: Write the new content to server file
            else if (source == writeButtons[i]) {
            	String newContent = editorArea.getText();
            	boolean written = clients[i].writeRequest(newContent);
            	if(written) {
            		logArea.append(clientName + " wrote successfully.\n");
            		fileContentArea.setText(newContent);
            	}
            	else {
            		logArea.append(clientName + " write request failed.\n");
            		editorArea.setEditable(false);
                    writeButtons[i].setEnabled(false);
                    renewButtons[i].setEnabled(false);
                    releaseButtons[i].setEnabled(false);
                    requestLockButtons[i].setEnabled(true);
            	}
            } 
            //Release Button: Release the Lock
            else if (source == releaseButtons[i]) {
            	boolean released = clients[i].releaseLock();
            	if(released) {
            		logArea.append(clientName + " released the lock.\n");
            	}
            	else {
            		logArea.append(clientName + " release failed beacause it is not the owner of lock.\n");
            	}
            	editorArea.setText("");
            	editorArea.setEditable(false);
            	writeButtons[i].setEnabled(false);
            	releaseButtons[i].setEnabled(false);
            	renewButtons[i].setEnabled(false);
            	requestLockButtons[i].setEnabled(true);
            }
            // Simulate Crash : Toggle the online status
            else if (source == crashButtons[i]) {
            	clients[i].isOnline = !clients[i].isOnline;
            	if(clients[i].isOnline) {
            		readButtons[i].setEnabled(true);
            		requestLockButtons[i].setEnabled(true);
            		logArea.append(clientName + " recovered!\n");
            		crashButtons[i].setText("Simulate Crash");
            	} else {
            		readButtons[i].setEnabled(false);
            		requestLockButtons[i].setEnabled(false);
            		renewButtons[i].setEnabled(false);
            		writeButtons[i].setEnabled(false);
            		releaseButtons[i].setEnabled(false);
            		logArea.append(clientName + " crashed!\n");
            		crashButtons[i].setText("Recover");
            	}
            }
        }
    }
}