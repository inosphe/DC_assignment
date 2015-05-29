import javax.swing.*;
import java.awt.*;

/**
 * Created by inosphe on 15. 5. 22..
 */



public class SceneState extends JPanel implements ProtocolEventListener{
    protected int type;
    protected ChatSystem system;
    public SceneState(ChatSystem _system, int _type){
        system = _system;
        type = _type;
    }

    public void OnEnter(){

    }

    public void OnExit(){

    }

    public void OnBlocked(ProtocolEvent evt){}
    public void OnReceived(ReceiveEvent evt){}
    public void OnConnectionEstablished(ProtocolEvent evt){}
    public void OnConnectionLost(ProtocolEvent evt){}

    public void Print(String str){}
    public void Monitor(String str){}
}
