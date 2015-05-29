/**
 * Created by inosphe on 15. 5. 23..
 */
public abstract class Connection {
    public Protocol protocol = null;
    public boolean connectionEstablished = false;

    abstract public boolean StartServer() throws Exception;
    abstract public boolean ConnectToServer() throws Exception;

    abstract public boolean Send(byte[] data, int count);
    abstract public boolean Resend();
    abstract public byte[] Receive();
}
