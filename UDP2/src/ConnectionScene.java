import javax.comm.CommPortIdentifier;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * Created by inosphe on 15. 5. 22..
 */
public class ConnectionScene extends SceneState {
    private JTextField tf_port_server;
    private JTextField tf_addr_client, tf_port_client;
    private JTextField tf_timeout, tf_timeout_count;
    private JButton btn_server, btn_client;
    private JComboBox combo_protocolType, combo_ARQType, combo_Serial;
    private JTextField tf_delay, tf_loss_percentage, tf_repeat_count;

    private int selectedProtocolType = 1;
    private int selectedARQType = 1;
    private CommPortIdentifier selectedPortId = null;


    public ConnectionScene(ChatSystem _system, int type){
        super(_system, type);
        this.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel panel_server = new JPanel();
        tf_port_server = new JTextField(5);
        tf_port_server.setText("9999");
        btn_server = new JButton();
        btn_server.setText("Start Server");
        btn_server.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int port = Integer.parseInt(tf_port_server.getText());
                
                switch(selectedProtocolType){
                case ChatSystem.PROTOCOL_TYPE_UDP:
                case ChatSystem.PROTOCOL_TYPE_HDLC:
                	system.CreateServer(selectedProtocolType, selectedARQType, port);
                	break;
                case ChatSystem.PROTOCOL_TYPE_HDLC_SERIAL:
                	if(selectedPortId != null)
                		system.CreateServer(selectedProtocolType, selectedARQType, selectedPortId);
                	break;
                }
                //EnableButtons(false);
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
                    InetAddress server_addr = InetAddress.getByName(tf_addr_client.getText());
                    int server_port = Integer.parseInt(tf_port_client.getText());
                    
                    switch(selectedProtocolType){
                    case ChatSystem.PROTOCOL_TYPE_UDP:
                    case ChatSystem.PROTOCOL_TYPE_HDLC:
                    	system.CreateClient(selectedProtocolType, selectedARQType, server_addr, server_port);
                    	break;
                    case ChatSystem.PROTOCOL_TYPE_HDLC_SERIAL:
                    	if(selectedPortId != null)
                    		system.CreateClient(selectedProtocolType, selectedARQType, selectedPortId);
                    	break;
                    }
                    
                    //EnableButtons(false);

                } catch (UnknownHostException ex) {
                	ex.printStackTrace();
                }
            }
        });
        panel_client.add(tf_addr_client);
        panel_client.add(tf_port_client);
        panel_client.add(btn_client);

        combo_ARQType = new JComboBox();
        combo_ARQType.addItem("No ARQ");
        combo_ARQType.addItem("Stop&Wait ARQ");
        combo_ARQType.addItem("Go-Back-N ARQ");
        combo_ARQType.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectedARQType = combo_ARQType.getSelectedIndex();
            }
        });
        combo_ARQType.setSelectedIndex(1);
        
        combo_Serial = new JComboBox<>();
        ArrayList<CommPortIdentifier> ports= SerialHelper.GetSerialPorts();
        for(CommPortIdentifier c : ports){
        	combo_Serial.addItem(c.getName());
        }
        combo_Serial.addActionListener(new ActionListener(){
        	@Override
        	public void actionPerformed(ActionEvent e){
        		selectedPortId = ports.get(combo_Serial.getSelectedIndex());
        	}
        });
        if(combo_Serial.getItemCount()>0){
        	combo_Serial.setSelectedIndex(0);
        }
        
        combo_protocolType = new JComboBox();
        combo_protocolType.addItem("NoFrame(UDP)");
        combo_protocolType.addItem("Frame(UDP)");
        combo_protocolType.addItem("Frame(Serial)");
        combo_protocolType.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectedProtocolType = combo_protocolType.getSelectedIndex();
                
                combo_Serial.setEnabled(selectedProtocolType == ChatSystem.PROTOCOL_TYPE_HDLC_SERIAL);
            }
        });
        combo_protocolType.setSelectedIndex(1);

        tf_timeout = new JTextField(5);
        tf_timeout.setText("250");
        ;
        tf_timeout.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    system.timeout = Integer.parseInt(tf_timeout.getText());
                    btn_server.setEnabled(system.timeout >= 0);
                    btn_client.setEnabled(system.timeout >= 0);
                } catch (NumberFormatException ex) {
                    btn_server.setEnabled(false);
                    btn_client.setEnabled(false);
                    ex.printStackTrace();
                }
            }
        });

        tf_timeout_count = new JTextField(3);
        tf_timeout_count.setText("10");
        ;
        tf_timeout_count.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    system.timeout_cnt = Integer.parseInt(tf_timeout_count.getText());
                    btn_server.setEnabled(system.timeout_cnt>=0);
                    btn_client.setEnabled(system.timeout_cnt>=0);
                } catch (NumberFormatException ex) {
                    btn_server.setEnabled(false);
                    btn_client.setEnabled(false);
                    ex.printStackTrace();
                }
            }
        });


        tf_loss_percentage = new JTextField(3);
        tf_loss_percentage.setText("0");
        ;
        tf_loss_percentage.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    system.loss_percentage = Integer.parseInt(tf_loss_percentage.getText());
                    btn_server.setEnabled(system.loss_percentage >= 0 && system.loss_percentage<=100);
                    btn_client.setEnabled(system.loss_percentage >= 0 && system.loss_percentage<=100);
                } catch (NumberFormatException ex) {
                    btn_server.setEnabled(false);
                    btn_client.setEnabled(false);
                    ex.printStackTrace();
                }
            }
        });

        tf_delay = new JTextField(6);
        tf_delay.setText("0");
        ;
        tf_delay.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    system.delay = Integer.parseInt(tf_delay.getText());
                    btn_server.setEnabled(system.delay >= 0);
                    btn_client.setEnabled(system.delay >= 0);
                } catch (NumberFormatException ex) {
                    btn_server.setEnabled(false);
                    btn_client.setEnabled(false);
                    ex.printStackTrace();
                }
            }
        });

        tf_repeat_count = new JTextField(5);
        tf_repeat_count.setText("1");
        ;
        tf_repeat_count.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    system.repeat_count = Integer.parseInt(tf_repeat_count.getText());
                    btn_server.setEnabled(system.repeat_count >= 1);
                    btn_client.setEnabled(system.repeat_count >= 1);
                } catch (NumberFormatException ex) {
                    btn_server.setEnabled(false);
                    btn_client.setEnabled(false);
                    ex.printStackTrace();
                }
            }
        });



        this.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 2;
        c.gridx = 0;
        c.gridy = 0;
        this.add(panel_client, c);
        c.gridx = 0;
        c.gridy = 1;
        this.add(panel_server, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 2;
        this.add(combo_protocolType, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 2;
        this.add(combo_ARQType, c);
        
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 3;
        this.add(combo_Serial, c);
        
        
        
        

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(0, 2));
        panel.setBorder(BorderFactory.createTitledBorder("timeout"));
        panel.add(new JLabel("timeout"));
        panel.add(tf_timeout);
        panel.add(new JLabel("timeout count"));
        panel.add(tf_timeout_count);

        JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayout(0, 2));
        panel2.setBorder(BorderFactory.createTitledBorder("network"));

        panel2.add(new JLabel("ack delay"));
        panel2.add(tf_delay);

        panel2.add(new JLabel("loss(%)"));
        panel2.add(tf_loss_percentage);

        panel2.add(new JLabel("repeat"));
        panel2.add(tf_repeat_count);
        

        JPanel panelDetail = new JPanel();
        panelDetail.setLayout(new GridLayout(0,2));
        panelDetail.setBorder(BorderFactory.createTitledBorder("detail"));
        panelDetail.add(panel);
        panelDetail.add(panel2);
        
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 3;
        c.gridx = 0;
        c.gridy = 4;
        this.add(panelDetail, c);
        

    }

    public void OnEnter(){
        EnableButtons(true);
    }


    public void EnableButtons(boolean enabled){
        btn_client.setEnabled(enabled);
        btn_server.setEnabled(enabled);
        tf_addr_client.setEnabled(enabled);
        tf_port_client.setEnabled(enabled);
        tf_port_server.setEnabled(enabled);
    }
}
