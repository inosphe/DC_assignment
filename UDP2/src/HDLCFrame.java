import java.nio.ByteBuffer;
import java.util.zip.CRC32;

import javax.swing.JOptionPane;

/**
 * Created by inosphe on 15. 5. 23..
 */
public class HDLCFrame {
    static public int FLAG_TYPE_DATA = 0x0001;
    static public int FLAG_TYPE_ACK = 0x0002;
    static public int FLAG_TYPE_NAK = 0x0003;

    final static public int ARQ_TYPE_NOARQ = 0x0000;
    final static public int ARQ_TYPE_STOP_N_WAIT = 0x0001;
    final static public int ARQ_TYPE_GO_BACK_N = 0x0002;
    final static public int ARQ_TYPE_SEL_REPEAT = 0x0003;

    public int type = FLAG_TYPE_DATA;
    public int arqType = ARQ_TYPE_NOARQ;
    public byte[] data = null;

    public int seqNumber = 0;
    public int ackNumber = 0;

    public boolean crcValidated = true;


    public HDLCFrame(int _type){
        type = _type;
    }

    public HDLCFrame(byte[] bytes){
        System.out.println("HDLCFrame - begin");
        ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
        buffer.put(bytes);
        buffer.flip();
        seqNumber = buffer.get();
        ackNumber = buffer.get();
        byte flag = buffer.get();
        arqType = (byte)(flag & 0xF0) >> 4;
        type = (byte)(flag & 0x0F);
        int length = buffer.get();

        if(type == FLAG_TYPE_DATA){
            int crc = buffer.getInt();
            data = new byte[length];
            buffer.get(data, 0, length);
            
            switch(Option.ReceiveCRC){
            case Option.CRC_TYPE_NO:
            	crcValidated = true;
            	break;
            case Option.CRC_TYPE_DATA_ONLY:
            	crcValidated = CheckDataCRC(crc, data, length);
            	break;
            case Option.CRC_TYPE_WHOLE:
            	crcValidated = CheckWholeCRC(crc, bytes, length+8);
            	break;	
            }
            
            System.out.println("crcValidated : " + crcValidated);
        }
        else{
            crcValidated = true;
        }

        if(data != null)
            System.out.println(new String(data));

        System.out.println("HDLCFrame - end");
    }
    
    static public int GetARQType(byte flag){
    	return (flag & 0xF0) >> 4;
    }
    
    static public int GetType(byte flag){
    	return (flag & 0x0F);
    }


    public Frame BuildFrame(boolean buildByteArray){
        int len = 0;
        if(data != null){
            len = data.length;
        }
        Frame f = new Frame();
        f.ackSeq = ackNumber;
        if(buildByteArray) {
            f.byteArray = GetByteArray();
        }
        f.crcValidated = crcValidated;
        f.sendSeq = seqNumber;
        if(len>0)
            f.data = new String(data);
        else
            f.data = "";

        
        if(type == FLAG_TYPE_DATA){
        	f.type = Frame.TYPE_DATA;
        }
        else if(type == FLAG_TYPE_ACK){
        	f.type = Frame.TYPE_ACK;
        }
        else if(type == FLAG_TYPE_NAK){
        	f.type = Frame.TYPE_NACK;
        }
        else{
        	JOptionPane.showMessageDialog(null, "invalid flag("+type+")", "Alert", JOptionPane.WARNING_MESSAGE);
        }
        
        return f;
    }

    public byte[] GetByteArray(){
        int dataLen = 0;
        if(data != null)
            dataLen = data.length;
        int totalLength = 4 + dataLen;
        if(data != null){
        	totalLength += 4;
        }
        ByteBuffer buffer = ByteBuffer.allocate(totalLength);
        buffer.put((byte) (seqNumber&0xFF));
        buffer.put((byte) (ackNumber&0xFF));
        byte flag = (byte)(arqType <<4 | type);
        buffer.put(flag);
        buffer.put((byte) dataLen);
        
        if(data != null){
        	buffer.putInt(0);
        	buffer.put(data);
        	int crc = 0;
        	switch(Option.SendCRC){
        		case Option.CRC_TYPE_WHOLE:
        		crc = GetCRC(buffer.array(), dataLen+8);
        		break;
        		
        		case Option.CRC_TYPE_DATA_ONLY:
        		crc = GetCRC(data, data.length);
        		break;
        		
        		case Option.CRC_TYPE_NO:
        		default:
        		break;		
        	}
        		
            buffer.putInt(4, crc);
        }            
        
        buffer.flip();
        return buffer.array();
    }

    public int GetCRC(byte[] bytes, int length){
        CRC32 crc32 = new CRC32();
        crc32.update(bytes, 0, length);
        return (int)crc32.getValue();
    }

    private boolean CheckWholeCRC(int crc, byte[] bytes, int length){
        ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
        buffer.put(bytes);
        buffer.putInt(4, 0);
        buffer.flip();

        int crc2 = GetCRC(buffer.array(), length);
        System.out.printf("CRC : %x, %x\n", crc, crc2);
        if(crc == crc2){
            System.out.printf("Same\n");
        }
        return crc == crc2;
    }
    
    private boolean CheckDataCRC(int crc, byte[] bytes, int length){
    	ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
        buffer.put(bytes);
        buffer.flip();

        int crc2 = GetCRC(buffer.array(), length);
        System.out.printf("CRC : %x, %x\n", crc, crc2);
        if(crc == crc2){
            System.out.printf("Same\n");
        }
        return crc == crc2;
    }
}
