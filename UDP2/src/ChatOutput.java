import javax.swing.*;
import javax.swing.JScrollPane;
import java.awt.*;

/**
 * Created by inosphe on 15. 5. 16..
 */
public class ChatOutput extends JPanel{
    JTextArea textArea;
    public ChatOutput(){
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JScrollPane pane = new JScrollPane();
        textArea = new JTextArea(5, 20);
        pane.getViewport().add(textArea);
        pane.setPreferredSize(new Dimension(250, 200));
        add(pane);
    }
    public void AddText(String str){
        textArea.append(str);
    }

    public void Clear(){
        textArea.setText("");
    }
}
