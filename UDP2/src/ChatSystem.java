import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class ChatSystem extends JFrame{
    static public final int STATE_ID_CONNECTION = 0;
    static public final int STATE_ID_CHAT = 1;

    public final int PROTOCOL_TYPE_UDP = 0;
    public final int PROTOCOL_TYPE_HDLC = 1;
    public final int PROTOCOL_TYPE_HDLC_SERIAL = 2;

    private Protocol protocol;

    SceneState states[] = new SceneState[5];
    private SceneState current_state = null;


    public int timeout = 100;
    public int timeout_cnt = 10;
    public int delay = 0;
    public int loss_percentage = 0;
    public int repeat_count = 1;


    public ChatSystem() {
        initProtocol();
        initUI();
        initStates();

        SetState(STATE_ID_CONNECTION);
    }

    private void initStates(){
        states[STATE_ID_CONNECTION] = new ConnectionScene(this, STATE_ID_CONNECTION);
        states[STATE_ID_CHAT] = new ChatScene(this, STATE_ID_CHAT);
    }

    private void initProtocol(){

    }

    private void initUI() {
        pack();
        setTitle("UDP Chat");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    public void MonitorConfiguration(){
        Monitor("Delay("+delay+"), Loss_Percentage("+loss_percentage+"), RepeatCount("+repeat_count+")");
        if(delay>0){
            Monitor("Ack packet will be delayed " + delay + "ms.");
        }
        if(repeat_count>1){
            Monitor("All packet(DATA/ACK/NCK) will be sended " + repeat_count + " times.");
        }
        if(loss_percentage>0){
            Monitor("All received packet will be lossed by " + loss_percentage + "%.");
        }
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



    public void CreateServer(int protocolType, int arqType, int port){
        protocol = CreateProtocolInstance(protocolType);
        protocol.SetARQType(arqType);
        ConnectionUDP connection = (ConnectionUDP)protocol.GetConnection();
        connection.open_port = port;
        protocol.openServer();


        SetState(STATE_ID_CHAT);
        protocol.MonitorARQType();

        try {
            Print("Server Started (" + InetAddress.getLocalHost().getHostAddress() + ":" + port + ")\n");
            Monitor("Server Started (" + InetAddress.getLocalHost().getHostAddress() + ":" + port + ")\n");
        } catch (UnknownHostException ex) {
            Monitor(ex.toString());
        }
    }

    public void CreateClient(int protocolType, int arqType, InetAddress server_addr, int server_port){
        protocol = CreateProtocolInstance(protocolType);
        protocol.SetARQType(arqType);
        ConnectionUDP connection = (ConnectionUDP)protocol.GetConnection();
        connection.target_addr = server_addr;
        connection.target_port = server_port;
        protocol.connectToServer();


        SetState(STATE_ID_CHAT);
        protocol.MonitorARQType();

        Send("try to connect\n");
        Print("try to connect (" + server_addr.getHostAddress() + ":" + server_port + ")\n");
        Monitor("try to connect (" + server_addr.getHostAddress() + ":" + server_port + ")\n");
    }

    public void PrintError(String str){
        if(current_state != null){
            current_state.Print(str);
        }
    }

    public void Print(String str){
        if(current_state != null){
            current_state.Print(str);
        }
    }

    public void Send(String str){
        protocol.Send(str);
    }

    public void SetState(int i){
        if(current_state != null){
            current_state.OnExit();
            remove(current_state);
            if(protocol!=null)
                protocol.removeEventListener(current_state);
            current_state.setVisible(false);
        }

        current_state = states[i];
        if(current_state != null){
            add(current_state, BorderLayout.NORTH);
            if(protocol!=null)
                protocol.AddEventListener(current_state);
            current_state.setVisible(true);
            current_state.OnEnter();
        }

        validate();
        repaint();
        pack();
    }

    public Protocol GetProtocol(){
        return protocol;
    }

    public Protocol CreateProtocolInstance(int type){
        switch(type){
            case PROTOCOL_TYPE_HDLC:
                return new ProtocolHDLC(this, new ConnectionUDP());
            default:
                return null;
        }
    }

    public void ClearProtocol(){
        protocol.Clear(); protocol = null;
    }

    public void Monitor(String str){
        if(current_state != null){
            current_state.Monitor(str);
        }
    }
}