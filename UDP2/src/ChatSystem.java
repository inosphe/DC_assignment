import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class ChatSystem extends JFrame implements ProtocolEventListener, ActionListener{

    private Protocol protocol;

    private JTextField tf_port_server;
    private JTextField tf_addr_client, tf_port_client;

    ChatInput input;
    ChatOutput output;

    JButton btn_server, btn_client;

    public ChatSystem() {
        initProtocol();
        initUI();
    }

    private void initProtocol(){

    }

    private void initUI() {
        final ProtocolEventListener el = this;

        JPanel panel_server = new JPanel();
        tf_port_server = new JTextField(5);
        tf_port_server.setText("9999");
        btn_server = new JButton();
        btn_server.setText("Start Server");
        btn_server.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int port = Integer.parseInt(tf_port_server.getText());
                System.out.println(port);
                protocol = new ChatProtocolWithStopAndWait(el);
                protocol.openServer(port);
                try {
                    output.AddText("Server Started (" + InetAddress.getLocalHost().getHostAddress() + ":" + port + ")\n");
                } catch (UnknownHostException ex) {
                    System.out.println(ex);
                }

                DisableButtons();
            }
        });
        panel_server.add(tf_port_server);
        panel_server.add(btn_server);

        JPanel panel_client = new JPanel();
        tf_addr_client = new JTextField(20);
        tf_addr_client.setText("localhost");
        tf_port_client = new JTextField(5);
        tf_port_client.setText("9999");
        btn_client = new JButton();
        btn_client.setText("Connect to");
        btn_client.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int port = Integer.parseInt(tf_port_client.getText());
                    protocol = new ChatProtocolWithStopAndWait(el);
                    System.out.println(tf_addr_client.getText());
                    System.out.println(port);
                    protocol.connectToServer(InetAddress.getByName(tf_addr_client.getText()), port);
                    output.AddText("try to connect (" + tf_addr_client.getText() + ":" + port + ")\n");
                    protocol.Send("try to connect\n");

                    DisableButtons();

                } catch (UnknownHostException ex) {
                    System.out.println(ex);
                }
            }
        });
        panel_client.add(tf_addr_client);
        panel_client.add(tf_port_client);
        panel_client.add(btn_client);

        JPanel panel_connection = new JPanel();
        panel_connection.setLayout(new BorderLayout());
        panel_connection.add(panel_client, BorderLayout.NORTH);
        panel_connection.add(panel_server, BorderLayout.SOUTH);

        add(panel_connection, BorderLayout.NORTH);

        input = new ChatInput(this);
        input.setVisible(true);
        input.SetEnabled(false);
        add(input, BorderLayout.SOUTH);

        output = new ChatOutput();
        output.setVisible(true);
        add(output, BorderLayout.CENTER);

        pack();
        setTitle("UDP Chat");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                ChatSystem ex = new ChatSystem();
                ex.setVisible(true);
            }
        });
    }

    public void OnBlocked(ProtocolEvent evt)
    {
        input.SetEnabled(!protocol.IsWaiting() && protocol.IsConnectionEstablsished());
    }
    public void OnReceived(ReceiveEvent evt)
    {
        output.AddText(">" + evt.GetString());
    }
    public void OnConnectionEstablished(ProtocolEvent evt){
        System.out.println("connection established.");
        tf_addr_client.setText(protocol.GetAddress().getHostAddress());
        tf_port_client.setText(protocol.GetPort() + "");
        input.SetEnabled(!protocol.IsWaiting());
        output.AddText("Connected\n");
    }

    public void OnConnectionLost(ProtocolEvent evt) {
        input.SetEnabled(!protocol.IsWaiting() && protocol.IsConnectionEstablsished());
    }

    public void actionPerformed(ActionEvent evt) {
        JTextField textField = (JTextField)evt.getSource();
        String text = textField.getText();
        System.out.println(text);
        textField.setText("");
        protocol.Send(text + "\n");
    }

    public void DisableButtons(){
        btn_client.setEnabled(false);
        btn_server.setEnabled(false);
        tf_addr_client.setEnabled(false);
        tf_port_client.setEnabled(false);
        tf_port_server.setEnabled(false);
    }
}