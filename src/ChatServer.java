import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.metal.MetalBorders;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.HashSet;
import java.util.Scanner;
import java.util.concurrent.Executors;

import static javax.swing.UIManager.getInstalledLookAndFeels;
import static javax.swing.UIManager.setLookAndFeel;

/**
 * A multithreaded chat room server. When a client connects the server requests
 * a screen name by sending the client the text "SUBMITNAME", and keeps
 * requesting a name until a unique one is received. After a client submits a
 * unique name, the server acknowledges with "NAMEACCEPTED". Then all messages
 * from that client will be broadcast to all other clients that have submitted a
 * unique screen name. The broadcast messages are prefixed with "MESSAGE".
 *
 * This is just a teaching example so it can be enhanced in many ways, e.g.,
 * better logging. Another is to accept a lot of fun commands, like Slack.
 */
public class ChatServer extends JFrame{

    // All client names, so we can check for duplicates upon registration.
    private static Set<String> names = new HashSet<>();

    // The set of all the print writers for all the clients, used for broadcast.
    private static Set<PrintWriter> writers = new HashSet<>();

    private JPanel panel;
    private GridBagLayout GBLayout;
    private ServerHandlerThread serverThread = new ServerHandlerThread();
    JTextArea activeUsersTextArea = new JTextArea(20,15);
    JTextArea serverLogTextArea = new JTextArea(20,30);
    JButton StartServerBtn = new JButton("Start Server");


    public static void main(String[] args) {

        try{
            for (UIManager.LookAndFeelInfo info : getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    setLookAndFeel(info.getClassName());
                    break;
                }
            }
        }
        catch (Exception e){
            System.err.println(e);
        }

        try {
            ChatServer frame = new ChatServer("Chat Server");
            frame.setVisible(true);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public class ServerHandlerThread extends Thread {

        @Override
        public void run() {
            System.out.println("The chat server is running...");
            var pool = Executors.newFixedThreadPool(500);
            try (var listener = new ServerSocket(59001)) {
                while (true) {
                    pool.execute(new Handler(listener.accept()));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }


    /**
     * The client handler task.
     */
    private class Handler implements Runnable {
        private String name;
        private Socket socket;
        private Scanner in;
        private PrintWriter out;

        /**
         * Constructs a handler thread, squirreling away the socket. All the interesting
         * work is done in the run method. Remember the constructor is called from the
         * server's main method, so this has to be as short as possible.
         */
        public Handler(Socket socket) {
            this.socket = socket;
        }

        /**
         * Services this thread's client by repeatedly requesting a screen name until a
         * unique one has been submitted, then acknowledges the name and registers the
         * output stream for the client in a global set, then repeatedly gets inputs and
         * broadcasts them.
         */
        public void run() {
            try {
                in = new Scanner(socket.getInputStream());
                out = new PrintWriter(socket.getOutputStream(), true);

                // Keep requesting a name until we get a unique one.
                while (true) {
                    out.println("SUBMITNAME");
                    name = in.nextLine();
                    if (name == null) {
                        return;
                    }
                    synchronized (names) {
                        if (!name.isBlank() && !names.contains(name)) {
                            names.add(name);
                            break;
                        }
                    }
                }

                // Now that a successful name has been chosen, add the socket's print writer
                // to the set of all writers so this client can receive broadcast messages.
                // But BEFORE THAT, let everyone else know that the new person has joined!
                out.println("NAMEACCEPTED " + name);
                for (PrintWriter writer : writers) {
                    writer.println("MESSAGE " + name + " has joined");
                }
                serverLogTextArea.append(name + " has joined\n");
                activeUsersTextArea.append(name + "\n");
                writers.add(out);

                // Accept messages from this client and broadcast them.
                while (true) {
                    String input = in.nextLine();
                    if (input.toLowerCase().startsWith("/quit")) {
                        return;
                    }
                    for (PrintWriter writer : writers) {
                        writer.println("MESSAGE " + name + ": " + input);
                    }
                    serverLogTextArea.append(name + " has sent a message...\n");
                }
            } catch (Exception e) {
                System.err.println(e);
            } finally {
                if (out != null) {
                    writers.remove(out);
                }
                if (name != null) {
                    System.out.println(name + " is leaving");

                    names.remove(name);
                    for (PrintWriter writer : writers) {
                        writer.println("MESSAGE " + name + " has left");
                    }
                    serverLogTextArea.append(name + " has left\n");
                    activeUsersTextArea.setText("");
                    for(String name : names){
                        activeUsersTextArea.append(name + "\n");
                    }
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    System.err.println(e);
                }
            }
        }
    }

    public ChatServer(String title){
            setTitle(title);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLocationByPlatform(false);
            setSize(400, 500);
            setResizable(false);
            GBLayout = new GridBagLayout();
            panel = new JPanel(GBLayout);
            setContentPane(panel);
            panel.setBackground(Color.white);
            panel.setBorder(new EmptyBorder(5,5,5,5));

            addComponent(StartServerBtn,0,0,2,1,new Insets(5,5,5,5),0,0,GridBagConstraints.CENTER,GridBagConstraints.CENTER);
            StartServerBtn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    try{
                        serverThread.start();
                        StartServerBtn.setEnabled(false);
                    }
                    catch (Exception ex){
                        System.err.println(ex + ex.getMessage());
                    }
                }
            });
            JLabel activeUserLabel = new JLabel("Active Users: ");
            activeUserLabel.setHorizontalAlignment(SwingConstants.CENTER);
            activeUserLabel.setBorder(new LineBorder(Color.lightGray,5,true));
            addComponent(activeUserLabel,1,0,1,1,new Insets(5,5,1,5),0,0,GridBagConstraints.BOTH,GridBagConstraints.CENTER);
            activeUsersTextArea.setEditable(false);
            addComponent(new JScrollPane(activeUsersTextArea),2,0,1,2,new Insets(1,5,5,5),1,1,GridBagConstraints.BOTH,GridBagConstraints.CENTER);

            JLabel serverLogLabel = new JLabel("Server Log: ");
            serverLogLabel.setHorizontalAlignment(SwingConstants.CENTER);
            serverLogLabel.setBorder(new LineBorder(Color.lightGray,5,true));
            addComponent(serverLogLabel,1,1,1,1,new Insets(5,5,1,5),0,0,GridBagConstraints.BOTH,GridBagConstraints.CENTER);
            serverLogTextArea.setEditable(false);
            addComponent(new JScrollPane(serverLogTextArea),2,1,1,2,new Insets(1,5,5,5),1,1,GridBagConstraints.BOTH,GridBagConstraints.CENTER);

            this.pack();
        }

        private void addComponent(Component component, int row, int column, int width, int height,
                                  Insets insets, double weightx, double weighty, int fill, int anchor){
            GridBagConstraints constraints = new GridBagConstraints();

            constraints.gridy = row;     //row to be placed in
            constraints.gridx = column;     //column to be placed in
            constraints.gridwidth = width;
            constraints.gridheight = height;
            constraints.insets = insets;
            constraints.weightx = weightx;
            constraints.weighty = weighty;
            constraints.fill = fill;
            constraints.anchor = anchor;

            panel.add(component, constraints);
        }
}
