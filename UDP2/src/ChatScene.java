import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by inosphe on 15. 5. 22..
 */
public class ChatScene extends SceneState implements ActionListener {

    ChatInput input;
    ChatOutput output;
    ChatOutput output2;

    public ChatScene(ChatSystem _system, int type){
        super(_system, type);
        input = new ChatInput(this);
        input.SetEnabled(false);
        output = new ChatOutput();
        output.setPreferredSize(new Dimension(500, 300));
        output2 = new ChatOutput();
        output2.setPreferredSize(new Dimension(500, 300));

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.add(output);
        panel.add(input);

        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 1;
        add(panel, c);


        JPanel panel2 = new JPanel();
        panel2.setLayout(new BoxLayout(panel2, BoxLayout.PAGE_AXIS));
        panel2.add(output2);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 0;
        add(panel2, c);
    }

    public void OnBlocked(ProtocolEvent evt)
    {
        Protocol protocol = system.GetProtocol();
        input.SetEnabled(!protocol.IsWaiting() && protocol.IsConnectionEstablsished());
    }
    public void OnReceived(ReceiveEvent evt)
    {
        output.AddText(">" + evt.GetString());
    }
    public void OnConnectionEstablished(ProtocolEvent evt){
        Protocol protocol = system.GetProtocol();
        System.out.println("connection established.");
        //tf_addr_client.setText(protocol.GetAddress().getHostAddress());
        //tf_port_client.setText(protocol.GetPort() + "");
        input.SetEnabled(!protocol.IsWaiting());
        output.AddText("Connected\n");
    }

    public void OnConnectionLost(ProtocolEvent evt) {
        Protocol protocol = system.GetProtocol();
        input.SetEnabled(!protocol.IsWaiting() && protocol.IsConnectionEstablsished());
        system.ClearProtocol();
        //system.SetState(ChatSystem.STATE_ID_CONNECTION);
    }

    public void actionPerformed(ActionEvent evt) {
        JTextField textField = (JTextField)evt.getSource();
        String text = textField.getText();
        System.out.println(text);
        textField.setText("");
        system.Send(text + "\n");
    }

    public void Print(String str){
        output.AddText(str);
    }

    @Override
    public void OnEnter(){
        output.Clear();
        system.MonitorConfiguration();
    }

    @Override
    public void Monitor(String str){
        output2.AddText("* " + str+"\n");
    }
}
