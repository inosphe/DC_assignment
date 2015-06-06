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

	public Lock lock = new ReentrantLock();
	
	public Lock lockSend = new ReentrantLock();
	public final Condition condSend = lockSend.newCondition();
	
	public Lock lockReceive = new ReentrantLock();
	public final Condition condBlock = lockReceive.newCondition();
	SenderThread senderThread;
	ReceiverThread receiverThread;

	int mode = 0;

	public ProtocolThreadBase(ChatSystem system, Connection connection) {
		super(system, connection);

		senderThread = new SenderThread(this);
		receiverThread = new ReceiverThread(this);
	}

	@Override
	public void Clear() {
		super.Clear();

		receiverThread.interrupt();
		senderThread.interrupt();

	}

	@Override
	protected boolean Init() {
		super.Init();
		
		receiverThread.start();
		senderThread.start();
		return true;
	}
	
	public boolean Send(String str) {
		if (UpdateBlockedStatus()) {
			int seqNo = NextSendSeqNo();
			if (seqNo < 0)
				return false;
			else{
				boolean ret = true;
				lockSend.lock();
				try {
					senderThread.Send(BuildSendFrame(str, seqNo, GetARQ().GetLastReceivedSeq()));
					system.Print("> " + str);
					ret = true;
				}
				catch(Exception e){
					e.printStackTrace();
					ret = false;
				}
				finally {
					condSend.signal();
					lockSend.unlock();
				}
				return ret;
			}
		} else
			return false;
	}

	@Override
	protected boolean Send(Frame sendFrame, int count) {
		lockSend.lock();
		boolean ret = super.Send(sendFrame, count);
		lockSend.unlock();
		return ret;
	}

	@Override
	public boolean IsBlocked() {
		lock.lock();
		lockReceive.lock();
		boolean ret = super.IsBlocked();
		lockReceive.unlock();
		lock.unlock();
		return ret;
	}
	

	@Override
	protected boolean OnAck(int seqNo) {
		boolean ret = super.OnAck(seqNo);
		lock.lock();
		lockReceive.lock();
		condBlock.signal();
		lockReceive.unlock();
		lock.unlock();
		return ret;
	}
	
	@Override
	public void SetWating(boolean _waiting) {
		lock.lock();
		super.SetWating(_waiting);
		lock.unlock();		
	}
	
	@Override
	public boolean IsWaitingSending(){
		lock.lock();
		lockSend.lock();
		boolean ret = super.IsWaitingSending();
		lockSend.unlock();
		lock.unlock();
		return ret;
	}

	@Override
	public void UpdateSendLock(){
		lockSend.lock();
		condSend.signal();
		lockSend.unlock();
	};
}
