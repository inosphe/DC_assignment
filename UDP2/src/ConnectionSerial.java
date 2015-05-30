import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Signature;
import java.util.Enumeration;
import java.util.TooManyListenersException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.comm.*;//32비트

public class ConnectionSerial extends Connection implements SerialPortEventListener {
	
    SerialPort serialPort;
    InputStream inputStream;
    OutputStream outputStream;
    
    final int MAXBUFFER = 512;
    byte[] recv_buffer = new byte[MAXBUFFER];       //echo 수신용 buffer
    byte[] last_received;
    int currentPos = 0;
    
    private Lock lockReceive = new ReentrantLock();
    private final Condition condReceive = lockReceive.newCondition();
    
    private final char FLAG = 126; //01111110
    private final char ESCAPE = '\\';
    
    String portName = "COM6";
    
    public CommPortIdentifier portId = null;
    
	@Override
	public boolean StartServer() throws Exception {
		
		if(StartSerialPort()){
			super._StartServer();
			return true;
		}
		else{
			return false;
		}
	}

	@Override
	public boolean ConnectToServer() throws Exception {
		if(StartSerialPort()){
			super._ConnectToServer();
			return true;
		}
		else{
			return false;
		}
	}
	
	@Override
	public boolean Close(){
		if(serialPort!=null)
			serialPort.close();
		return true;
	}
	
	public void SetSerialPortIdentifier(CommPortIdentifier _portId){
		portId = _portId;
	}
	
	private boolean StartSerialPort(){
		if(portId == null)
			return false;
		
		try{
			return SetPort((SerialPort) portId.open("SerialChatServer", 2000));
		}
	 	catch (PortInUseException e) {
	 		protocol.system.Alert("port open failed.");
	 		e.printStackTrace();
	 		return false;
	 	}
	}
	
	private boolean SetPort(SerialPort _serialPort){
		serialPort = _serialPort;
		
		try {
			inputStream = serialPort.getInputStream();
            outputStream = serialPort.getOutputStream();
        } catch (IOException e) {
        	e.printStackTrace();
        	return false;
        }
		
		try {
			serialPort.addEventListener(this);
		} catch (TooManyListenersException e) {
			e.printStackTrace();
		}
	        
		serialPort.notifyOnDataAvailable(true);
        
        try {
            serialPort.setSerialPortParams(9600,
            	SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
            	SerialPort.PARITY_NONE);
        } catch (UnsupportedCommOperationException e) {
        	e.printStackTrace();
        	return false;
        }
        
		return true;
	}

	@Override
	public boolean Send(byte[] data, int count) {
		int escapeCount = 0;
		for(int i=0; i<data.length; ++i){
			if(data[i] == FLAG || data[i] == ESCAPE){
				escapeCount++;
			}
		}
		
		byte[] send_data;
		if(escapeCount == 0){
			send_data = data;
		}
		else{
			send_data = new byte[data.length + escapeCount];
			for(int i=0, j=0; i<data.length; ++i){
				if(data[i] == FLAG || data[i] == ESCAPE){
					send_data[j++] = ESCAPE;
				}
				
				send_data[j++] = data[i];
			}
		}
		
		try {
			for(int i=0; i<count; ++i) {
                if(count>1){
                    protocol.system.Monitor("Repeat Count ("+(i+1)+"/"+protocol.system.repeat_count+")");
                }
                protocol.system.Monitor("serial sended |  + length("+send_data.length+"), escapeCount("+escapeCount+")");
                outputStream.write(FLAG);
                outputStream.write(send_data);
                outputStream.write(FLAG);
            }
        } catch (IOException e) {
        	e.printStackTrace();
        }
		return true;
	}
	
	@Override
	public byte[] Receive() {
		try{
			lockReceive.lock();
			condReceive.await();
			
			if(!connectionEstablished){
	            protocol.OnConnectionEstablished();
	        }
		}
		catch(Exception e){
			e.printStackTrace();
		}
		finally{
			
		}
		
		return last_received;
	}
	
	public void serialEvent(SerialPortEvent event) {
        // 이벤트의 타입에 따라 switch 문으로 제어.
        switch (event.getEventType()) {
        case SerialPortEvent.BI:
        case SerialPortEvent.OE:
        case SerialPortEvent.FE:
        case SerialPortEvent.PE:
        case SerialPortEvent.CD:
        case SerialPortEvent.CTS:
        case SerialPortEvent.DSR:
        case SerialPortEvent.RI:
        case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
            break;
            
            // 데이터가 도착하면
            case SerialPortEvent.DATA_AVAILABLE:
                int numBytes = 0;

                // 입력 스트림이 사용가능하면, 버퍼로 읽어 들인 후
                // String 객체로 변환하여 출력
                try {
                    while (inputStream.available() > 0) {
                        numBytes = inputStream.read(recv_buffer, currentPos, MAXBUFFER-currentPos);
                        System.out.println("serial received" + numBytes);
                    }
                    currentPos += numBytes;                    
                } catch (IOException e) {
                	e.printStackTrace();
                }
                
                //current only for HDLC frame
                
                final int POS_FLAG = 2;
                final int POS_LENGTH = 3;
                
                int begin = -1;
                int end = -1;
                boolean escapeDetected = false;
                int escapeCount = 0;
                
                for(int i=0; i<currentPos; ++i){
                	byte ch = recv_buffer[i];
                	
                	if(escapeDetected){
                		escapeDetected = false;
                	}
                	else{
                		if(ch == ESCAPE){
                    		escapeDetected = true;
                    		escapeCount++;
                    	}
                    	else{
                    		if(ch == FLAG){
                    			if(begin<0){
                    				begin = i;
                    			}
                    			else if(i == begin+1){
                    				begin = i;
                    			}
                    			else{
                    				end = i;
                    			}
                    		}
                    		
                    		escapeDetected = false;
                    	}
                	}
                }
                
                assert(escapeDetected==false);
                if(begin>=0 && end>begin){
                	int byteLen = end-begin-escapeCount-1;
                	
                	last_received = new byte[byteLen];
                	
                	for(int i=(begin+1), j=0; i<end; ++i){
                		byte ch = recv_buffer[i];
                		if(escapeDetected){
                			last_received[j++] = ch;
                    		escapeDetected = false;
                    	}
                    	else{
                    		if(ch == ESCAPE){
                        		escapeDetected = true;
                        	}
                        	else{
                        		last_received[j++] = ch;
                        		escapeDetected = false;
                        	}
                    	}
                		recv_buffer[i] = 0;
                	}
                	
            		System.arraycopy(recv_buffer, end+1, recv_buffer, 0, currentPos-(end+1));
            		currentPos -= end+1;
            		
            		protocol.system.Monitor("Received via SerialPort | begin("+ begin + "), end(" + end+ "), length("+byteLen+"), escapeCount("+escapeCount+")");
            		
            		lockReceive.lock();
            		condReceive.signal();
            		lockReceive.unlock();
                	
                }
                
                break;
            }
        }
}
