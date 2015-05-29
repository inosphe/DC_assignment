import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * Created by inosphe on 15. 5. 23..
 */
public class ConnectionUDP extends Connection {
    public InetAddress target_addr;
    public int target_port;

    public int open_port;
    private boolean isServer = false;
    public boolean IsServer(){
        return isServer;
    }

    DatagramSocket Dsocket;
    DatagramPacket lastPacket;

    public ConnectionUDP(){
    }

    final int MAXBUFFER = 512;
    byte recv_buffer[] = new byte[MAXBUFFER];       //echo 수신용 buffer

    @Override
    public boolean StartServer(){
        return openServer_UDP();
    }

    @Override
    public boolean ConnectToServer(){
        return connectToServer_UDP();
    }

    public boolean openServer_UDP(){
        isServer = true;
        connectionEstablished = false;
        target_port = -1;
        target_addr = null;
        try{
            Dsocket = new DatagramSocket(open_port);
            System.out.println("server started");
        }
        catch(SocketException e){
            System.out.println(e);
        }

        return true;
    }

    public boolean connectToServer_UDP(){
        isServer = false;
        connectionEstablished = true;


        try{
            Dsocket = new DatagramSocket();
            open_port = Dsocket.getPort();
            System.out.println("connected" + target_addr.getHostAddress() + ":" + target_port);
        }
        catch(SocketException e){
            System.out.println(e);
        }

        return true;
    }

    @Override
    public boolean Send(byte[] data, int count){
        lastPacket = new DatagramPacket(data, data.length, target_addr, target_port);
        try{
            for(int i=0; i<count; ++i) {
                if(count>1){
                    protocol.system.Monitor("Repeat Count ("+(i+1)+"/"+protocol.system.repeat_count+")");
                }
                Dsocket.send(lastPacket);
            }
        }
        catch (IOException e){
            System.out.println(e.toString());
        }
        return true;
    }

    public boolean Resend(){
        try{
            Dsocket.send(lastPacket);
        }
        catch (IOException e){
            System.out.println(e.toString());
        }
        return true;
    }

    public byte[] Receive(){
        DatagramPacket recv_packet = new DatagramPacket(recv_buffer, MAXBUFFER);
        try{
            Dsocket.receive(recv_packet);
        }
        catch (IOException e){
            System.out.println(e.toString());
        }

        if(!connectionEstablished){
            target_port = recv_packet.getPort();
            target_addr = recv_packet.getAddress();
            protocol.OnConnectionEstablished();
        }

        return recv_packet.getData();
    }
}
