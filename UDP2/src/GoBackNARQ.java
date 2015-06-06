
public class GoBackNARQ extends ARQBase{
	
    private int sendeSeqNo = 1;
    private int receiveSeqNo = 1;
    private int ackReceived = 1;
    
    
    public GoBackNARQ(Protocol _protocol, int _windowSize){
    	super(_protocol, _windowSize);
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
	
	public boolean ProcessAck(int ackNo){
		int displacement = ackNo - ackReceived;
        if(displacement<0)
            displacement += GetWindowSize();

        protocol.Monitor("OnAck | seqNo(" + ackNo + "), ackReceived(" + ackReceived + "), displacement(" + displacement + ")");

        int _ackReceived = ackReceived;
        ackReceived = ackNo;

        if((GetWindowSize()>2 && displacement >= GetWindowSize()-1) || displacement==0) {
            protocol.Monitor("Error occured | Resend from (" + ackNo + ")");
            protocol.ResendFrom(ackReceived);
            return false;
        }
        else{
            System.out.println("clearbuffer " + _ackReceived + ", " + ackReceived);
            ClearBuffer(_ackReceived, ackReceived);
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
        return ret;
	}
	   

    private void incrementReceiveSeqNo(){
    	int before = receiveSeqNo;
        receiveSeqNo = (receiveSeqNo+1)%GetWindowSize();
        System.out.println("incrementReceiveSeqNo | " + before + " -> " + receiveSeqNo);
    }
    
    @Override
    protected void OnTimeOuted(){
    	protocol.OnTimeOuted();
    	ReleaseTimeOuted();
    }
    
    @Override
    public boolean ResendFrom(int seqNo){
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
    
    @Override
    public int GetLastAckSeq(){
        return ackReceived;
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
    
    @Override
    public boolean IsBufferEmpty(){
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
}
