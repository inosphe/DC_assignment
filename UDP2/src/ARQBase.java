import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.Timer;


public abstract class ARQBase {
	private Timer timer;
	private boolean isTimerRunning = false;
	private Frame[] sendBuffer;
	private int windowSize = -1;
	Protocol protocol;
	private boolean isTimeOuted = false;
	private Lock lock = new ReentrantLock();
	
	public ARQBase(Protocol _protocol, int _windowSize){
		protocol = _protocol;
		windowSize = _windowSize;
		sendBuffer = new Frame[windowSize];
		
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
		
	}


	public void lock(){
		lock.lock();
	}

	public void unlock(){
		lock.unlock();
	}

	public void SetTimeout(int delay, boolean start){
		timer.stop();
		if(start){
			isTimerRunning = true;
			timer.setDelay(delay);
			timer.start();
		}
		else{
			isTimeOuted = false;
		}
	}
	
	public int GetWindowSize(){
		return windowSize;
	}
	
	public boolean IsTimerRunning(){
		return isTimerRunning;
	}
	
	public boolean IsTimeOuted(){
		return isTimeOuted;
	}
	
	protected void ClearBuffer(int begin, int end){
		lock.lock();
		protocol.Monitor("ClearBuffer("+begin + ", " + end +")");
        int i = begin;
        while(i!=end){
            sendBuffer[i] = null;

            System.out.println(i + " is cleared");
            i = (i+1)%windowSize;
        }
        lock.unlock();
    }
	
	public Frame GetFrame(int seqNo){
		lock.lock();
		Frame frame = sendBuffer[seqNo];
		lock.unlock();
		return frame;
		
	}
	
	public void ReleaseTimeOuted(){
		isTimeOuted = false;
	}
	
	protected void OnTimeOuted(){}

	
	public void SetBuffer(int i, Frame frame){
		lock.lock();
		sendBuffer[i] = frame;
		lock.unlock();
	}
	public void SetBuffer(Frame frame){
		SetBuffer(frame.sendSeq, frame);		
	}
	
	public abstract String ToString();	
	public abstract boolean IsBlocked();
	public abstract boolean IsWaitingSending();
	public abstract int GetNextSendSeqNo();
	public abstract boolean ProcessAck(int ackNo);
	public abstract boolean ResendFrom(int seqNo);
	//received ack
	public abstract int GetLastAckSeq();

	//received packet
	public abstract boolean IsValidSeq(int seqNo);

	public abstract int GetType();
	
	public abstract String GetExpectedSeqNumberString(); 
	
	public abstract boolean IsBufferEmpty();
	
	public abstract void SendAck(Frame frame, boolean accepted);
	public abstract int GetLastReceivedSeq();
}
