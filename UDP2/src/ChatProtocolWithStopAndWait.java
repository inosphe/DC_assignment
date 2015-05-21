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
public class ChatProtocolWithStopAndWait extends Protocol {


    Lock lockSend = new ReentrantLock();
    public final Condition condSend = lockSend.newCondition();
    Lock lockAck = new ReentrantLock();
    public final Condition condAck = lockAck.newCondition();
    SenderThread senderThread;
    ReceiverThread receiverThread;
    boolean ackReceived = false;

    DatagramSocket Dsocket;
    int mode = 0;

    public ChatProtocolWithStopAndWait(ProtocolEventListener el){
        AddEventListener(el);
    }

    public boolean openServer(int _port){
        mode = 0;
        connectionEstablished = false;
        port = -1;
        addr = null;
        try{
            Dsocket = new DatagramSocket(_port);
            System.out.println("server started");
        }
        catch(SocketException e){
            System.out.println(e);
        }

        Init();
        return true;
    }

    public boolean connectToServer(InetAddress _addr, int _port){
        mode = 1;
        connectionEstablished = true;
        port = _port;
        addr = _addr;

        try{
            Dsocket = new DatagramSocket();
            System.out.println("connected" +  _addr.getAddress() + "" +_port);
        }
        catch(SocketException e){
            System.out.println(e);
        }

        Init();
        return true;
    }

    public boolean close(){
        return true;
    }

    public boolean Init(){
        senderThread = new SenderThread(this);
        senderThread.start();
        receiverThread = new ReceiverThread(this);
        receiverThread.start();
        return true;
    }

    @Override
    public boolean Send(String str){
        if(IsWaiting())
            return false;

        lockSend.lock();
        try{
            senderThread.Send(str);
        }
        finally{
            condSend.signal();
            lockSend.unlock();
        }
        return true;
    }

    private class SenderThread extends Thread{
        public boolean sendReady = false;
        public Queue<String> strBuffer = new LinkedList<String>();
        ChatProtocolWithStopAndWait protocol;
        public SenderThread(ChatProtocolWithStopAndWait _protocol){
            protocol = _protocol;
        }
        public void Send(String str){
            strBuffer.add(str);
        }
        @Override
        public void run(){
            while(true){

                try{
                    lockSend.lock();
                    lockAck.lock();
                    ackReceived = false;
                    while(strBuffer.isEmpty() == true){
                        condSend.await();
                    }

                    protocol.SetWating(true);

                    String str = strBuffer.element();
                    DatagramPacket send_packet = new DatagramPacket(str.getBytes()
                            , str.getBytes().length
                            , protocol.addr
                            , protocol.port);

                    int count = 10;
                    while(count >= 0){
                        Dsocket.send(send_packet);
                        if(count < 10){
                            OnReceive("* Retry (" + (10-count) + "/10)\n");
                        }
                        condAck.await(5000, TimeUnit.MILLISECONDS);
                        if(protocol.ackReceived){
                            break;
                        }
                        else{
                            count--;
                        }
                    }

                    if(count<0){
                        protocol.OnConnectionLost();
                    }

                    protocol.SetWating(false);

                    strBuffer.poll();
                    lockSend.unlock();
                }
                catch(InterruptedException e){
                    System.out.println(e);
                }
                catch(IOException e){
                    System.out.println(e);
                }

            }
        }
    }

    private class ReceiverThread extends Thread{
        final int MAXBUFFER = 512;
        byte recv_buffer[] = new byte[MAXBUFFER];       //echo 수신용 buffer
        DatagramPacket ack_packet;
        public ReceiverThread(ChatProtocolWithStopAndWait protocol){
            String strAck = "##";
            System.out.println(strAck.getBytes());
            ack_packet = new DatagramPacket(strAck.getBytes()
                    , strAck.getBytes().length);
        }
        @Override
        public void run(){
            while(true){
                try{
                    DatagramPacket recv_packet = new DatagramPacket(recv_buffer, MAXBUFFER);
                    Dsocket.receive(recv_packet);

                    if(!connectionEstablished){
                        port = recv_packet.getPort();
                        addr = recv_packet.getAddress();
                        OnConnectionEstablished();
                    }

                    String str = new String(recv_buffer, 0, recv_packet.getLength());
                    System.out.println(str);
                    System.out.println(str.equals("##"));
                    if(str.equals("##")){
                        lockAck.lock();
                        ackReceived = true;
                        condAck.signal();
                        lockAck.unlock();
                    }
                    else{
                        ack_packet.setAddress(addr);
                        ack_packet.setPort(port);
                        Dsocket.send(ack_packet);
                        OnReceive(new String(recv_buffer, 0, recv_packet.getLength()));
                    }
                }
                catch(IOException e){
                }
            }
        }
    }
}
