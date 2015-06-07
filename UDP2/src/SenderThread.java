import java.io.IOException;
import java.net.DatagramPacket;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

/**
 * Created by inosphe on 15. 5. 23..
 */

public class SenderThread extends Thread{
    public boolean sendReady = false;
    private Queue<Frame> sendBuffer = new LinkedList<Frame>();
    ProtocolThreadBase protocol;
    public SenderThread(ProtocolThreadBase _protocol){
        protocol = _protocol;
    }
    public void Send(Frame sendFrame){
        sendBuffer.add(sendFrame);
    }
    @Override
    public void run(){
        while(true){

            try{
                protocol.lockSend.lock();
                // || protocol.IsWaitingSending()
                while(sendBuffer.isEmpty() == true){
                	protocol.Monitor("locked");
                    protocol.condSend.await();
                }
                Frame sendFrame = sendBuffer.element();
                sendBuffer.poll();
                protocol.lockSend.unlock();
                
                protocol.Send(sendFrame, protocol.system.repeat_count);
                System.out.println("senderframe-send | " + sendFrame.ToString());
            }
            catch(InterruptedException e){
                e.printStackTrace();
            }
        }
    }
}
