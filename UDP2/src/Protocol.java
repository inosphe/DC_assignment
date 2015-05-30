import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by inosphe on 15. 5. 16..
 */
public abstract class Protocol {
    static public final int ARQ_TYPE_NOARQ = 0;
    static public final int ARQ_TYPE_STOP_N_WAIT = 1;
    static public final int ARQ_TYPE_GO_BACK_N = 2;


    public List<ProtocolEventListener> eventListeners = new ArrayList<ProtocolEventListener>();;

    public boolean openServer(){
        try{
            if(connection.StartServer() == false)
            	return false;
            return Init();
        }
        catch (Exception e){
            system.PrintError(e.toString());
            return false;
        }
    }
    public boolean connectToServer(){
        try{
            if(connection.ConnectToServer() == false)
            	return false;
            
            return Init();
        }
        catch (Exception e){
            system.PrintError(e.toString());
            return false;
        }
    }

    protected boolean Init(){return true;}

    protected boolean waiting = false;
    public boolean IsWaiting(){
        return waiting;
    }

    private int arqType = ARQ_TYPE_NOARQ;
    public void SetARQType(int _arqType){
        arqType = _arqType;
        if(arqType == ARQ_TYPE_STOP_N_WAIT){
            SetWindowSize(2);
        }
        else if(arqType == ARQ_TYPE_GO_BACK_N){
            SetWindowSize(8);
        }
        else{
            arqType = ARQ_TYPE_NOARQ;
            SetWindowSize(2);
        }
    }

    public void MonitorARQType(){
        switch(arqType){
            case ARQ_TYPE_STOP_N_WAIT:
                system.Monitor("ARQ Type - STOP_N_WAIT");
                break;
            case ARQ_TYPE_GO_BACK_N:
                system.Monitor("ARQ Type - GO_BACK_N");
                break;
            case ARQ_TYPE_NOARQ:
                system.Monitor("ARQ Type - NO_ARQ");
                break;
        }
    }
    public int GetARQType(){
        return arqType;
    }

    public boolean IsConnectionEstablsished(){
        return connection.connectionEstablished;
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

    private int windowSize = 2;
    private int sendeSeqNo = 0;
    private int receiveSeqNo = 0;
    private int ackReceived = 0;
    private Frame[] sendBuffer = new Frame[windowSize];

    public int GetLastAckSeq(){
        return ackReceived;
    }

    private void SetWindowSize(int size) {
        windowSize = size;
        sendBuffer = new Frame[windowSize];
    }

    protected int NextSendSeqNo(){
        int seq = sendeSeqNo;
        sendeSeqNo = (sendeSeqNo+1)%windowSize;
        System.out.println("NextSendSeqNo | " + seq + " -> " + sendeSeqNo);
        return seq;
    }


    public boolean usePiggyBacking = false;


    ChatSystem system;
    Connection connection;

    public Connection GetConnection(){
        return connection;
    }

    public Protocol(ChatSystem _system, Connection _connection){
        system = _system;
        connection = _connection;
        connection.protocol = this;
    }

    public void OnReceive(String str){
        ReceiveEvent evt = new ReceiveEvent(str);
        for(ProtocolEventListener el : eventListeners){
            el.OnReceived(evt);
        }
    }



    public void OnConnectionEstablished(){
        connection.connectionEstablished = true;
        waiting = false;
        ProtocolEvent evt = new ProtocolEvent(this);
        for(ProtocolEventListener el : eventListeners){
            el.OnConnectionEstablished(evt);
        }
    }

    public void OnConnectionLost(){
        system.Alert("Connection Lost.");
        connection.connectionEstablished = false;
        ProtocolEvent evt = new ProtocolEvent(this);
        for(ProtocolEventListener el : eventListeners){
            el.OnConnectionLost(evt);
        }
    }

    public boolean Send(String str){
        return Send(BuildSendFrame(str, NextSendSeqNo(), 0), 0);
    }

    protected boolean Send(Frame sendFrame, int count){
        sendBuffer[sendFrame.sendSeq] = sendFrame;

        system.Monitor("Send - " + sendFrame.ToString());
        return connection.Send(sendFrame.byteArray, count);
    }
    public boolean ResendFrom(int seqNo){
        for(int i=seqNo; i!=sendeSeqNo; i=(i+1)%windowSize)
            Send(sendBuffer[i], 1);

        return true;
    }

    public void RunReceive(){
        ProcessReceivedFrame(BuildReceiveFrame(Receive()));
    }

    protected byte[] Receive(){
        system.Monitor("Waiting...");
        return connection.Receive();
    }

    private void incrementReceiveSeqNo(){
    	int before = receiveSeqNo;
        receiveSeqNo = (receiveSeqNo+1)%windowSize;
        System.out.println("incrementReceiveSeqNo | " + before + " -> " + receiveSeqNo);
    }

    private boolean IsFutureSequence(int seqNo){
        int displacement = receiveSeqNo - seqNo;
        if(displacement<0)
            displacement += windowSize;
        return windowSize>2 && displacement>=windowSize-1;

    }

    protected void ProcessReceivedFrame(Frame frame){
    	if(frame == null)
    		return;
        system.Monitor("Received - " + frame.ToString());

        Random random = new Random();
        int randomValue = random.nextInt(100);
        if(system.loss_percentage>randomValue){
            system.Monitor("Frame is forced to loss [Test featrue]) | threshold("+system.loss_percentage+"), value("+randomValue+")");
            return;
        }

        if(frame.type == Frame.TYPE_ACK || frame.type == Frame.TYPE_NACK || usePiggyBacking){
            OnAck(frame.ackSeq);
        }
        else if(arqType != ARQ_TYPE_NOARQ){
            if(frame.sendSeq != receiveSeqNo){
                if(arqType != ARQ_TYPE_NOARQ) {
                    system.Monitor("Invalid Sequence Number | expected(" + receiveSeqNo + "), actual(" + frame.sendSeq + ")");
                    SendAck(false);
                }
            }
            else if(!frame.crcValidated){
                system.Monitor("CRC check failed.\n");
                if(arqType != ARQ_TYPE_NOARQ) {
                    SendAck(false);
                }
            }
            else {
                SendAck(true);
                OnReceive(frame);
            }
        }
        else{
            OnReceive(frame);
        }
    }

    private void SendAck(boolean accepted){
        if(accepted){
            incrementReceiveSeqNo();
        }
        system.Monitor("Send Ack | accepted("+accepted+"), seqNo("+receiveSeqNo+")");
        if(system.delay>0){
            system.Monitor("Delay applied | sleep("+system.delay+")");
            try{
                Thread.sleep(system.delay);
            }
            catch(InterruptedException e){
            	e.printStackTrace();
            }
        }
        connection.Send(BuildAckFrame(accepted, 0, receiveSeqNo).byteArray, system.repeat_count);
    }

    protected void OnReceive(Frame frame){
        system.Print("< " + frame.data);
    }

    protected boolean OnAck(int seqNo) {
        if(arqType == ARQ_TYPE_NOARQ)
            return true;

        int displacement = seqNo - ackReceived;
        if(displacement<0)
            displacement += windowSize;

        system.Monitor("OnAck | seqNo(" + seqNo + "), ackReceived(" + ackReceived + "), displacement(" + displacement + ")");

        ackReceived = seqNo;

        if((windowSize>2 && displacement >= windowSize-1) || displacement==0) {
            system.Monitor("Error occured | Resend from (" + seqNo + ")");
            ResendFrom(ackReceived);
        }
        else{
            ClearBuffer(seqNo);
        }

        return true;
    }
    public boolean IsNeededToWaitAck(){
        if(arqType == ARQ_TYPE_NOARQ)
            return false;

        int displacement = sendeSeqNo - ackReceived;
        if(displacement<0)
            displacement += windowSize;
        return displacement >= windowSize-1;
    }

    public void Print(String str){
        system.Print(str);
    }

    abstract protected Frame BuildReceiveFrame(byte[] bytes);
    abstract protected Frame BuildSendFrame(String str, int reqSeqNo, int ackSeqNo);
    abstract protected Frame BuildAckFrame(boolean accepted, int reqSeqNo, int ackSeqNo);

    private void ClearBuffer(int ackSeqNo){
        int i = receiveSeqNo;
        while(i!=ackSeqNo){
            sendBuffer[i] = null;

            i = (i+1)%windowSize;
        }
    }

    public void Clear(){
    	if(connection!=null)
    		connection.Close();
    }
}
