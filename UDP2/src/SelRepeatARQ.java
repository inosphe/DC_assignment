import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.Timer;


public class SelRepeatARQ extends ARQBase {
	int send_start;
	int send_next_seq;
	
	int receive_window_start_seq;
	Frame[] receive_buffer;
	Timer[] timers;
	int[] retryCount;
	
	public SelRepeatARQ(Protocol protocol, int _windowSize){
		super(protocol, _windowSize);
		
		send_start = 1;
		send_next_seq = 1;
		
		receive_window_start_seq = 1;
		
		receive_buffer = new Frame[_windowSize];
		for(int i=0; i<_windowSize; ++i){
			receive_buffer[i] = null;
		}
		
		timers = new Timer[_windowSize];
		for(int i=0; i<_windowSize; ++i){
			int seqNo = i;
			timers[i] = new Timer(protocol.system.timeout, new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					OnTimeOuted(seqNo);
					
				}
			});
			timers[i].setRepeats(false);
		}
		
		retryCount = new int[_windowSize];
		for(int i=0; i<_windowSize; ++i){
			retryCount[i] = protocol.system.timeout_cnt;
		}
	}
	
	private int GetSendingWindowSize(){
		return GetWindowSize()/2;
	}
	
	@Override
	public String ToString() {
		return "Selective Repeat";
	}

	@Override
	public boolean IsBlocked() {
		lock();
		boolean ret = IsSendingWindowFull();
		unlock();
		return ret;		
	}
	
	private boolean IsSendingWindowFull(){
		int displacement = send_next_seq - send_start + 1;
		if(displacement<0)
			displacement += GetWindowSize();
		
		return displacement >= GetSendingWindowSize();
	}

	@Override
	public boolean IsWaitingSending() {
		lock();
		boolean ret = IsSendingWindowFull();
		unlock();
		return ret;	
	}

	@Override
	public int GetNextSendSeqNo() {
		lock();
		int seq = send_next_seq;
		send_next_seq = (send_next_seq+1)%GetWindowSize();
        System.out.println("NextSendSeqNo | " + seq + " -> " + send_next_seq);
        unlock();
        return seq;
	}
	
	private boolean IsValidAck(int ackNo){
		int displacement = ackNo - send_start;
		if(displacement < 0)
			displacement += GetWindowSize();
		
		int displacement2 = send_start + GetSendingWindowSize() - ackNo;
		if(displacement2 < 0)
			displacement2 += GetWindowSize();
		
		if(displacement + displacement2 == GetSendingWindowSize()){
			Frame f = GetFrame(ackNo);
			return f!=null;
		}
		
		return false;
	}

	private boolean ProcessAck(int ackNo) {
		protocol.Monitor("ProcessAck | seqNo(" + ackNo + "), send_start(" + send_start + ")");
		
		if(IsValidAck(ackNo)){
			SetBuffer(ackNo, null);
			retryCount[ackNo] = protocol.system.timeout_cnt;
			
			int begin = send_start;
			int end = send_next_seq;
			
			for(int i=begin; i!=end; i=(i+1)%GetWindowSize()){
				if(GetFrame(i) == null){
					send_start = (send_start+1)%GetWindowSize(); 
				}
			}
			
			protocol.UpdateSendLock();
			protocol.UpdateBlockedStatus();
			
			return true;
		}
		else{
			protocol.Monitor("Error occured | invalid ackNo(" + ackNo + ") | Dont resend anything");
			return false;
		}
	}

	 private boolean Resend(int seqNo) {
		 Frame f = GetFrame(seqNo);
		 if(f == null)
			 return false;
		 
		 if(retryCount[seqNo]>=0){
			 protocol.Send(f, 1);
			 --retryCount[seqNo];
		 }
		 
				 
		 if(retryCount[seqNo]>=0){
            protocol.Monitor("* Retry seqNo("+seqNo+"), send_start("+send_start+") | (" + (protocol.system.timeout_cnt-retryCount[seqNo]) + "/" + protocol.system.timeout_cnt + ")\n");
		 }
		 		 
		 return true;
	}


	@Override
	public boolean IsValidSeq(int seqNo) {
		int displacement = seqNo - receive_window_start_seq;
		if(displacement < 0)
			displacement += GetWindowSize();
		
		int displacement2 = receive_window_start_seq + GetSendingWindowSize() - seqNo;
		if(displacement2 < 0)
			displacement2 += GetWindowSize();
		
		return displacement + displacement2 == GetSendingWindowSize();
	}

	@Override
	public int GetType() {
		// TODO Auto-generated method stub
		return Protocol.ARQ_TYPE_SEL_REPEAT;
	}

	@Override
	public String GetExpectedSeqNumberString() {
		String str = "";
		
		int end = (receive_window_start_seq+GetSendingWindowSize())%GetWindowSize();
		for(int i=receive_window_start_seq; i!=end; i=(i+1)%GetWindowSize()){
			if(str.length() > 0)
				str += ", ";
			str += i;
		}
		return str;
	}

//	private boolean IsBufferEmpty() {
//		for(int i=receive_window_start_seq; i!= receive_window_start_seq+GetWindowSize(); i=(i+1)%GetWindowSize()){
//			Frame f = GetFrame(i);
//			if(f!=null)
//				return false;
//		}
//		
//		return false;
//	}

	@Override
	public void SendAck(Frame frame, boolean accepted) {
		if(accepted){
			int ack = frame.sendSeq;
	    	int seq = ack; 

			protocol.Monitor("Send Ack | accepted(" + accepted + "), seqNo(" + seq
					+ ")");
			
			protocol.Delay();
			protocol.SendRaw(protocol.BuildAckFrame(accepted, seq, ack));
		}
	}

	@Override
	public int GetLastReceivedSeq() {
		return 0;
	}
	
    private void OnTimeOuted(int seqNo){
    	if (retryCount[seqNo] < 0) {
    		protocol.Monitor("Request Failed.\n");
    		protocol.Monitor("fail");
            // sleep(2500);
    		protocol.OnConnectionLost();
        }
        else{
            Resend(seqNo);
        }
    }
	 
	 @Override
	 public void OnSend(Frame frame){
		 timers[frame.sendSeq].start();
	 }
	 
	 @Override
	 public void OnAck(int ackNo){
        timers[ackNo].stop();
        ProcessAck(ackNo);
	 }
	 
	 @Override
	 public void OnReceive(Frame frame){
		 receive_buffer[frame.sendSeq] = frame;
		 
		 int begin = receive_window_start_seq;
		 int end = (receive_window_start_seq+GetSendingWindowSize())%GetWindowSize();
		 for(int i=begin; i!=end; i=(i+1)%GetWindowSize()){
			 if(i==receive_window_start_seq && receive_buffer[i]!=null){
				 receive_window_start_seq = (receive_window_start_seq+1)%GetWindowSize();
				 receive_buffer[i] = null;
				 protocol.OnReceive(frame);
			 }
		 }
	 }
	 
	 

}
