/**
 * Created by inosphe on 15. 5. 23..
 */
public abstract class Connection {
    public Protocol protocol = null;
    public boolean connectionEstablished = false;
    private boolean isServer = false;
    public boolean IsServer(){
        return isServer;
    }

    abstract public boolean StartServer() throws Exception;
    abstract public boolean ConnectToServer() throws Exception;

    abstract public boolean Send(byte[] data, int count);
    abstract public byte[] Receive();
    
    protected boolean _StartServer(){
    	isServer = true;
        connectionEstablished = false;
        
        return true;
    }
    
    protected void _ConnectToServer(){
    	isServer = false;
        connectionEstablished = true;
    }
    
    public boolean Close(){return true;}
}
