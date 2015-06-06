
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public abstract class ARQBase {
	private Frame[] sendBuffer;
	private int windowSize = -1;
	Protocol protocol;
	private Lock lock = new ReentrantLock();
	
	public ARQBase(Protocol _protocol, int _windowSize){
		protocol = _protocol;
		windowSize = _windowSize;
		sendBuffer = new Frame[windowSize];
		
		
	}


	public void lock(){
		lock.lock();
	}

	public void unlock(){
		lock.unlock();
	}

	
	public int GetWindowSize(){
		return windowSize;
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
	//received packet
	public abstract boolean IsValidSeq(int seqNo);

	public abstract int GetType();
	
	public abstract String GetExpectedSeqNumberString(); 
	public abstract void SendAck(Frame frame, boolean accepted);
	public abstract int GetLastReceivedSeq();
	public abstract void OnAck(int ackNo);
	public void Clear(){}
	public abstract void OnSend(Frame frame);
	public abstract void OnReceive(Frame frame);
}
