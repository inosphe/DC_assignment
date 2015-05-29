import com.sun.corba.se.impl.orbutil.concurrent.CondVar;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by inosphe on 15. 5. 17..
 */
public abstract class ProtocolThreadBase extends Protocol {


    public Lock lockSend = new ReentrantLock();
    public final Condition condSend = lockSend.newCondition();
    public Lock lockReceive = new ReentrantLock();
    public final Condition condAck = lockReceive.newCondition();
    SenderThread senderThread;
    ReceiverThread receiverThread;


    int mode = 0;

    public ProtocolThreadBase(ChatSystem system, Connection connection){
        super(system, connection);

        senderThread = new SenderThread(this);
        receiverThread = new ReceiverThread(this);
    }

    public void Clear(){
        receiverThread.interrupt();
        senderThread.interrupt();

    }



    public boolean close(){
        return true;
    }

    @Override
    protected boolean Init(){
        receiverThread.start();
        senderThread.start();
        return true;
    }

    @Override
    public boolean Send(String str){
        if(IsNeededToWaitAck())
            return false;

        lockSend.lock();
        try{
            senderThread.Send(BuildSendFrame(str, NextSendSeqNo(), 0));
        }
        finally{
            condSend.signal();
            lockSend.unlock();
        }
        return true;
    }

    @Override
    protected boolean Send(Frame sendFrame, int count){
        lockSend.lock();
        boolean ret = super.Send(sendFrame, count);
        lockSend.unlock();
        return ret;
    }

    @Override
    public boolean IsNeededToWaitAck(){
        lockReceive.lock();
        boolean ret = super.IsNeededToWaitAck();
        lockReceive.unlock();
        return ret;
    }

    @Override
    public void OnReceive(Frame frame){
        lockReceive.lock();
        super.OnReceive(frame);
        lockReceive.unlock();
    }

    @Override
    protected boolean OnAck(int seqNo){
        boolean ret = super.OnAck(seqNo);
        lockReceive.lock();
        condAck.signal();
        lockReceive.unlock();
        return ret;
    }

}
