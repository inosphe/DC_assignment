import java.io.IOException;
import java.net.DatagramPacket;

/**
 * Created by inosphe on 15. 5. 23..
 */

public class ReceiverThread extends Thread{
    ProtocolThreadBase protocol;
    public ReceiverThread(ProtocolThreadBase _protocol){
        protocol = _protocol;
    }
    @Override
    public void run(){
        while(true){
            try{
                protocol.RunReceive();
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
    }
}
