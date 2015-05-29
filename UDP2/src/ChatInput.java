import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by inosphe on 15. 5. 16..
 */
public class ChatInput extends JPanel{
    protected JTextField textField;
    Protocol protocol;
    public ChatInput(ActionListener al){
        textField = new JTextField(20);
        textField.addActionListener(al);

        add(textField);
    }
    public void SetEnabled(boolean enabled){
        textField.setEnabled(enabled);
        if(enabled){
            textField.requestFocus();
        }
    }

}
