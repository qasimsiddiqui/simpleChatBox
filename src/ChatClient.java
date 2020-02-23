import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

/**
 * A simple Swing-based client for the chat server. Graphically it is a frame
 * with a text field for entering messages and a textarea to see the whole
 * dialog.
 *
 * The client follows the following Chat Protocol. When the server sends
 * "SUBMITNAME" the client replies with the desired screen name. The server will
 * keep sending "SUBMITNAME" requests as long as the client submits screen names
 * that are already in use. When the server sends a line beginning with
 * "NAMEACCEPTED" the client is now allowed to start sending the server
 * arbitrary strings to be broadcast to all chatters connected to the server.
 * When the server sends a line beginning with "MESSAGE" then all characters
 * following this string should be displayed in its message area.
 */
public class ChatClient extends JFrame{

    String serverAddress;
    Scanner in;
    PrintWriter out;

    JPanel panel;
    JTextField textField = new JTextField(40);
    JTextArea messageArea = new JTextArea(16, 40);
    GridBagLayout GBLayout = new GridBagLayout();

    /**
     * Constructs the client by laying out the GUI and registering a listener with
     * the textfield so that pressing Return in the listener sends the textfield
     * contents to the server. Note however that the textfield is initially NOT
     * editable, and only becomes editable AFTER the client receives the
     * NAMEACCEPTED message from the server.
     */
    public ChatClient(String serverAddress) {
        this.serverAddress = serverAddress;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationByPlatform(true);
        setSize(300, 300);
        setResizable(false);
        panel = new JPanel(GBLayout);
        setContentPane(panel);
        panel.setBackground(Color.white);
        panel.setBorder(new EmptyBorder(5,5,5,5));

        messageArea.setEditable(false);
        addComponent(new JScrollPane(messageArea),0,0,2,1,new Insets(5,5,5,5),1,1,GridBagConstraints.BOTH,GridBagConstraints.CENTER);
        JLabel messageLabel = new JLabel("Message: ");
        addComponent(messageLabel,1,0,1,1,new Insets(5,5,5,2),0,0,GridBagConstraints.CENTER,GridBagConstraints.CENTER);
        addComponent(textField,1,1,1,1,new Insets(5,1,5,5),0,0,GridBagConstraints.CENTER,GridBagConstraints.CENTER);

        this.pack();

        // Send on enter then clear to prepare for next message
        textField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                out.println(textField.getText());
                textField.setText("");
            }
        });
    }

    public String getName() {
        return JOptionPane.showInputDialog(this, "Choose a screen name:", "Screen name selection",
                JOptionPane.PLAIN_MESSAGE);
    }

    private void run() {
        try {
            var socket = new Socket(serverAddress, 59001);
            in = new Scanner(socket.getInputStream());
            out = new PrintWriter(socket.getOutputStream(), true);

            while (in.hasNextLine()) {
                var line = in.nextLine();
                if (line.startsWith("SUBMITNAME")) {
                    out.println(getName());
                } else if (line.startsWith("NAMEACCEPTED")) {
                    this.setTitle("Chatter - " + line.substring(13));
                    textField.setEditable(true);
                } else if (line.startsWith("MESSAGE")) {
                    messageArea.append(line.substring(8) + "\n");
                }
            }
        }catch (Exception ex){
            System.err.println(ex + "\t" + ex.getMessage());
            JOptionPane.showMessageDialog(this,ex + "\t" + ex.getMessage());
        }
        finally {
            this.setVisible(false);
            this.dispose();
        }
    }

    public static void main(String[] args) {
        var client = new ChatClient("127.0.0.1");
        client.setVisible(true);
        client.run();
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