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
	protected int remainedRetryCount = 0;

	public List<ProtocolEventListener> eventListeners = new ArrayList<ProtocolEventListener>();;

	public boolean openServer() {
		try {
			if (connection.StartServer() == false)
				return false;
			return Init();
		} catch (Exception e) {
			system.PrintError(e.toString());
			return false;
		}
	}

	public boolean connectToServer() {
		try {
			if (connection.ConnectToServer() == false)
				return false;

			return Init();
		} catch (Exception e) {
			system.PrintError(e.toString());
			return false;
		}
	}

	protected boolean Init() {
		remainedRetryCount = system.timeout_cnt;
		return true;
	}

	protected boolean waiting = false;

	public boolean IsWaiting() {
		return waiting;
	}

	private ARQBase arq = null;

	public void SetARQType(int arqType) {
		switch (arqType) {
		case ARQ_TYPE_STOP_N_WAIT:
			arq = new StopAndWaitARQ(this);
			break;

		case ARQ_TYPE_GO_BACK_N:
			arq = new GoBackNARQ(this, 8);
			break;

		default:
			arq = null;
			break;
		}
	}

	public void MonitorARQType() {
		if (arq == null) {
			system.Monitor("ARQ Type - NO_ARQ");
		} else {
			system.Monitor("ARQ Type - " + arq.ToString());
		}
	}

	public int GetARQType() {
		if (arq == null) {
			return ARQ_TYPE_NOARQ;
		} else {
			return arq.GetType();
		}
	}

	public boolean IsConnectionEstablsished() {
		return connection.connectionEstablished;
	}

	public void AddEventListener(ProtocolEventListener el) {
		eventListeners.add(el);
	}

	public void removeEventListener(ProtocolEventListener el) {
		eventListeners.remove(el);
	}

	public void SetWating(boolean _waiting) {
		waiting = _waiting;
		ProtocolEvent evt = new ProtocolEvent(this);
		for (ProtocolEventListener el : eventListeners) {
			el.OnBlocked(evt);
		}
	}

	public int GetLastAckSeq() {
		if (arq == null)
			return -1;

		return arq.GetLastAckSeq();
	}

	protected int NextSendSeqNo() {
		if (arq == null)
			return 0;

		return arq.GetNextSendSeqNo();
	}

	public boolean usePiggyBacking = false;

	ChatSystem system;
	Connection connection;

	public Connection GetConnection() {
		return connection;
	}

	public Protocol(ChatSystem _system, Connection _connection) {
		system = _system;
		connection = _connection;
		connection.protocol = this;
	}

	public void OnReceive(String str) {
		ReceiveEvent evt = new ReceiveEvent(str);
		for (ProtocolEventListener el : eventListeners) {
			el.OnReceived(evt);
		}
	}

	public void OnConnectionEstablished() {
		connection.connectionEstablished = true;
		waiting = false;
		ProtocolEvent evt = new ProtocolEvent(this);
		for (ProtocolEventListener el : eventListeners) {
			el.OnConnectionEstablished(evt);
		}
	}

	public void OnConnectionLost() {
		system.Alert("Connection Lost.");
		connection.connectionEstablished = false;
		ProtocolEvent evt = new ProtocolEvent(this);
		for (ProtocolEventListener el : eventListeners) {
			el.OnConnectionLost(evt);
		}
	}

	public boolean Send(String str) {
		if (UpdateBlockedStatus()) {
			int seqNo = NextSendSeqNo();
			if (seqNo < 0)
				return false;
			else{
		        system.Print("> " + str);
				return Send(BuildSendFrame(str, seqNo, arq.GetLastReceivedSeq()), 0);
			}
		} else
			return false;
	}

	public void OnTimeOuted() {
		assert (arq != null);

		if (GetRetryCount() < 0) {
			system.Monitor("Request Failed.\n");
			system.Monitor("fail");
			// sleep(2500);
			OnConnectionLost();
			
		}
		
		
		ResendFrom(arq.GetLastAckSeq());
		
		UpdateBlockedStatus();
	}

	public boolean UpdateBlockedStatus() {
		if (arq.IsBlocked()) {
			SetWating(true);
			return false;
		}

		SetWating(false);
		return true;
	}

	protected boolean Send(Frame sendFrame, int count) {
		assert(sendFrame.type == Frame.TYPE_DATA);
		if(arq != null){
			arq.SetBuffer(sendFrame);
			arq.SetTimeout(system.timeout, true);
		}

		system.Monitor("Send - " + sendFrame.ToString());
		return connection.Send(sendFrame.byteArray, count);
	}

	public void RunReceive() {
		ProcessReceivedFrame(BuildReceiveFrame(Receive()));
	}

	protected byte[] Receive() {
		system.Monitor("Waiting...");
		return connection.Receive();
	}

	protected void ProcessReceivedFrame(Frame frame) {
		if (frame == null)
			return;
		system.Monitor("Received - " + frame.ToString());

		Random random = new Random();
		int randomValue = random.nextInt(100);
		if (system.loss_percentage > randomValue) {
			system.Monitor("Frame is forced to loss [Test featrue]) | threshold("
					+ system.loss_percentage + "), value(" + randomValue + ")");
			return;
		}

		if (frame.type == Frame.TYPE_ACK || frame.type == Frame.TYPE_NACK
				|| usePiggyBacking) {
			OnAck(frame.sendSeq);
		} else {
			if (arq != null) {
				if (!arq.IsValidSeq(frame.sendSeq)) {
					system.Monitor("Invalid Sequence Number | expected("
							+ arq.GetExpectedSeqNumberString() + "), actual("
							+ frame.sendSeq + ")");
					//SendAck(false);
				} else if (!frame.crcValidated) {
					system.Monitor("CRC check failed.\n");
					arq.SendAck(frame, false);
				} else {
					arq.SendAck(frame, true);
					OnReceive(frame);
				}
			} else {
				OnReceive(frame);
			}
		}
	}

	protected void OnReceive(Frame frame) {
		system.Print("< " + frame.data);
	}
	
	public void ResendFrom(int seqNo){
		if(arq != null){
			if(GetRetryCount() >= 0){
				if(arq.ResendFrom(seqNo)){
					SetRetryCount(GetRetryCount()-1);
				}
			}
			
			if(GetRetryCount()>=0){
				system.Monitor("* Retry (" + (system.timeout_cnt-remainedRetryCount) + "/" + system.timeout_cnt + ")\n");
			}
		}
	}

	public void SendRaw(Frame f){
		connection.Send(f.byteArray, system.repeat_count);
	}
	
	protected void SetRetryCount(int count){
		remainedRetryCount = count;
	}
	
	protected int GetRetryCount(){
		return remainedRetryCount;
	}

	protected boolean OnAck(int seqNo) {
		if (arq == null)
			return true;

		arq.SetTimeout(system.timeout, false);

		if (arq.ProcessAck(seqNo)) {
			SetRetryCount(system.timeout_cnt);
		}
		else{
			SetRetryCount(GetRetryCount()-1);
		}

		if (!arq.IsBufferEmpty()) {
			arq.SetTimeout(system.timeout, true);
		}
		
		UpdateBlockedStatus();

		return true;
	}

	public boolean IsBlocked() {
		if (arq == null) {
			return false;
		}

		return arq.IsBlocked();
	}
	
	public boolean IsWaitingSending(){
		if(arq == null)
			return false;
		
		return arq.IsWaitingSending();
	}

	public void Print(String str) {
		system.Print(str);
	}

	abstract protected Frame BuildReceiveFrame(byte[] bytes);

	abstract protected Frame BuildSendFrame(String str, int reqSeqNo,
			int ackSeqNo);

	abstract public Frame BuildAckFrame(boolean accepted, int reqSeqNo,
			int ackSeqNo);

	public void Clear() {
		if (connection != null)
			connection.Close();
		if(arq!=null){
			arq.SetTimeout(0, false);
		}
	}

	public void Monitor(String str) {
		system.Monitor(str);
	}
}
