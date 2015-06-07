
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

public class GoBackNARQ extends ARQBase{
	
    private int sendeSeqNo = 1;
    private int receiveSeqNo = 1;
    private int ackReceived = 1;


    protected int remainedRetryCount = 0;

    private Timer timer;
    private boolean isTimeOuted = false;
    private boolean isTimerRunning = false;
    
    
    public GoBackNARQ(Protocol _protocol, int _windowSize){
    	super(_protocol, _windowSize);

        timer = new Timer(protocol.system.timeout, new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                lock();
                isTimerRunning = false;
                isTimeOuted = true;
                
                OnTimeOuted();
                unlock();
            }
        });

        timer.setRepeats(false);
        

        remainedRetryCount = protocol.system.timeout_cnt;

    }


    
    private boolean IsTimerRunning(){
        return isTimerRunning;
    }
    
    private boolean IsTimeOuted(){
        return isTimeOuted;
    }


    
    private void ReleaseTimeOuted(){
        isTimeOuted = false;
        protocol.UpdateSendLock();
    }
	
	@Override
	public String ToString(){
		return "Go-Back-N"; 
	}
	
	@Override
	public boolean IsBlocked(){
		return IsSendingWindowFull();
	}
	
	@Override
	public boolean IsWaitingSending(){
		return IsSendingWindowFull() || IsTimeOuted();
	}
	
	@Override
	public int GetNextSendSeqNo(){
		lock();
		int seq = sendeSeqNo;
        sendeSeqNo = (sendeSeqNo+1)%GetWindowSize();
        System.out.println("NextSendSeqNo | " + seq + " -> " + sendeSeqNo);
        unlock();
        return seq;
	}
	
	private boolean ProcessAck(int ackNo){
		int displacement = ackNo - ackReceived;
        if(displacement<0)
            displacement += GetWindowSize();

        protocol.Monitor("ProcessAck | seqNo(" + ackNo + "), ackReceived(" + ackReceived + "), displacement(" + displacement + ")");

        int _ackReceived = ackReceived;
        ackReceived = ackNo;

        if((GetWindowSize()>2 && displacement >= GetWindowSize()-1) || displacement==0) {
            protocol.Monitor("Error occured | Resend from (" + ackNo + ")");
            ResendFrom(ackReceived);

            --remainedRetryCount;
            return false;
        }
        else{
            ClearBuffer(_ackReceived, ackReceived);

            remainedRetryCount = protocol.system.timeout_cnt;
            return true;
        }
	}
	
	private boolean IsSendingWindowFull(){
		lock();
		int displacement = sendeSeqNo - ackReceived;
        if(displacement<0)
            displacement += GetWindowSize();
        boolean ret = displacement >= GetWindowSize()-1;
        unlock();
        protocol.Monitor("isfull : " + ret);
        return ret;
	}
	   

    private void incrementReceiveSeqNo(){
    	int before = receiveSeqNo;
        receiveSeqNo = (receiveSeqNo+1)%GetWindowSize();
        System.out.println("incrementReceiveSeqNo | " + before + " -> " + receiveSeqNo);
    }
    
    private void OnTimeOuted(){
        if (remainedRetryCount < 0) {
            protocol.Monitor("Request Failed.\n");
            protocol.Monitor("fail");
            // sleep(2500);
            protocol.OnConnectionLost();
        }
        else{
            ResendFrom(ackReceived);
        }
        ReleaseTimeOuted();
    }

    private void ResendFrom(int seqNo){
        if(remainedRetryCount >= 0){
            if(__ResendFrom(seqNo)){
                --remainedRetryCount;

                if(remainedRetryCount>=0){
                    protocol.Monitor("* Retry (" + (protocol.system.timeout_cnt-remainedRetryCount) + "/" + protocol.system.timeout_cnt + ")\n");
                }
            }
        }
    }
    
    private boolean __ResendFrom(int seqNo){
    	boolean ret = false;
    	protocol.Monitor("resend("+seqNo+ ", " + sendeSeqNo + ")");
    	for(int i=seqNo; i!=sendeSeqNo; i=(i+1)%GetWindowSize()){
    		System.out.println("("+i+")");
    		Frame f = GetFrame(i);
    		if(f == null){	//not sended yet
    			System.out.println("not yet");
    			break;
    		}
    			
    		System.out.println("resend : " + f.ToString());
    		protocol.Send(f, 1);
    		ret = true;
    	}
        return ret;
    }
    
    //ack to sender
    private int GetAckSeqNo(boolean accepted){
    	if(accepted){
            incrementReceiveSeqNo();
        }
    	
    	return receiveSeqNo;
    }

    @Override
    public int GetLastReceivedSeq(){
    	return receiveSeqNo;
    }
    
    public boolean IsValidSeq(int seqNo){
    	return seqNo == receiveSeqNo;
    }
    
    
    @Override
    public int GetType(){
    	return Protocol.ARQ_TYPE_GO_BACK_N;
    }

    @Override
	public String GetExpectedSeqNumberString(){
    	return receiveSeqNo+"";
    }
    
    private boolean IsBufferEmpty(){
    	return ackReceived == sendeSeqNo;
    }
    
    @Override
    public void SendAck(Frame frame, boolean accepted){
    	int ack = frame.sendSeq;
    	int seq = GetAckSeqNo(accepted);    	 

		protocol.Monitor("Send Ack | accepted(" + accepted + "), seqNo(" + seq
				+ ")");
		if (protocol.system.delay > 0) {
			protocol.system.Monitor("Delay applied | sleep(" + protocol.system.delay + ")");
			try {
				Thread.sleep(protocol.system.delay);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		protocol.SendRaw(protocol.BuildAckFrame(accepted, seq, ack));
    }

    @Override
    public void OnAck(int ackNo){
        SetTimeout(false);
        ProcessAck(ackNo);
        if (!IsBufferEmpty()) {
            SetTimeout(true);
        }
    }

    @Override
    public void Clear(){
        SetTimeout(false);
    }

    @Override
    public void OnSend(Frame frame){
        SetTimeout(true);
    }


    public void SetTimeout(boolean start){
        timer.stop();
        if(start){
            isTimerRunning = true;
            timer.start();
        }
        else{
            isTimeOuted = false;
        }
    }

    @Override
    public void OnReceive(Frame frame){
        protocol.OnReceive(frame);
    }
}

