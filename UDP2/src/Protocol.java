import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by inosphe on 15. 5. 16..
 */
public abstract class Protocol {
    public List<ProtocolEventListener> eventListeners = new ArrayList<ProtocolEventListener>();;

    abstract public boolean openServer(int _port);
    abstract public boolean connectToServer(InetAddress _addr, int _port) throws UnknownHostException;

    protected boolean waiting = false;
    public boolean IsWaiting(){
        return waiting;
    }

    protected InetAddress addr;
    protected int port;
    InetAddress GetAddress(){
        return addr;
    }
    int GetPort(){
        return port;
    }

    protected boolean connectionEstablished = false;
    public boolean IsConnectionEstablsished(){
        return connectionEstablished;
    }

    public void AddEventListener(ProtocolEventListener el){
        eventListeners.add(el);
    }
    public void removeEventListener(ProtocolEventListener el){
        eventListeners.remove(el);
    }
    public void SetWating(boolean _waiting){
        waiting = _waiting;
        ProtocolEvent evt = new ProtocolEvent(this);
        for(ProtocolEventListener el : eventListeners){
            el.OnBlocked(evt);
        }
    }

    public void OnReceive(String str){
        ReceiveEvent evt = new ReceiveEvent(str);
        for(ProtocolEventListener el : eventListeners){
            el.OnReceived(evt);
        }
    }

    public void OnConnectionEstablished(){
        connectionEstablished = true;
        waiting = false;
        ProtocolEvent evt = new ProtocolEvent(this);
        for(ProtocolEventListener el : eventListeners){
            el.OnConnectionEstablished(evt);
        }
    }

    public void OnConnectionLost(){
        connectionEstablished = false;
        ProtocolEvent evt = new ProtocolEvent(this);
        for(ProtocolEventListener el : eventListeners){
            el.OnConnectionLost(evt);
        }
    }

    abstract public boolean Send(String str);
}
