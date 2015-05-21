import java.util.EventListener;

/**
 * Created by inosphe on 15. 5. 17..
 */
public interface ProtocolEventListener extends EventListener {
    public void OnBlocked(ProtocolEvent evt);
    public void OnReceived(ReceiveEvent evt);
    public void OnConnectionEstablished(ProtocolEvent evt);
    public void OnConnectionLost(ProtocolEvent evt);
}
