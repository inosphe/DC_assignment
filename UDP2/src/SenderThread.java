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
    public Queue<Frame> sendBuffer = new LinkedList<Frame>();
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
                while(sendBuffer.isEmpty() == true){
                    protocol.condSend.await();
                }

                Frame sendFrame = sendBuffer.element();
                sendBuffer.poll();

                int count = protocol.system.timeout_cnt;
                protocol.Send(sendFrame, protocol.system.repeat_count);
                while (count >= 0){
                    if(count>=0 && protocol.IsNeededToWaitAck()){
                        protocol.SetWating(true);
                        protocol.lockReceive.lock();
                        protocol.condAck.await(protocol.system.timeout, TimeUnit.MILLISECONDS);
                        protocol.lockReceive.unlock();
                    }

                    if(protocol.IsNeededToWaitAck()){
                        count--;
                        if(count >= 0){
                            if(count<protocol.system.timeout_cnt)
                                protocol.system.Monitor("* Retry (" + (protocol.system.timeout_cnt - count) + "/" + protocol.system.timeout_cnt+")\n");

                            protocol.ResendFrom(protocol.GetLastAckSeq());
                        }
                    }
                    else{
                        protocol.SetWating(false);
                        break;
                    }
                }

                if(count<0){
                    protocol.system.Monitor("Request Failed.\n");
                    sleep(2500);
                    protocol.OnConnectionLost();
                }

                protocol.lockSend.unlock();
            }
            catch(InterruptedException e){
                protocol.system.Monitor(e.toString());
            }
        }
    }
}
