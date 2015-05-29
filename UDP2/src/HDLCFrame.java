import java.nio.ByteBuffer;
import java.util.zip.CRC32;

/**
 * Created by inosphe on 15. 5. 23..
 */
public class HDLCFrame {
    final static public int FLAG_TYPE_DATA = 0x0001;
    final static public int FLAG_TYPE_ACK = 0x0002;
    final static public int FLAG_TYPE_NAK = 0x0003;

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
            crcValidated = CheckCRC(bytes, length+8);
            System.out.println("crcValidated : " + crcValidated);
        }
        else{
            crcValidated = true;
        }

        if(data != null)
            System.out.println(new String(data));

        System.out.println("HDLCFrame - end");
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
            f.crcValidated = CheckCRC(f.byteArray, len+8);
        }
        else{
            f.crcValidated = crcValidated;
        }
        f.sendSeq = seqNumber;
        if(len>0)
            f.data = new String(data);
        else
            f.data = "";

        switch(type){
            case FLAG_TYPE_DATA:
                f.type = Frame.TYPE_DATA;
                break;
            case FLAG_TYPE_ACK:
                f.type = Frame.TYPE_ACK;
                break;
            case FLAG_TYPE_NAK:
                f.type = Frame.TYPE_NACK;
                break;
        }



        return f;
    }

    public byte[] GetByteArray(){
        int dataLen = 0;
        if(data != null)
            dataLen = data.length;
        ByteBuffer buffer = ByteBuffer.allocate(4 + dataLen + 4);
        buffer.put((byte) seqNumber);
        buffer.put((byte) ackNumber);
        byte flag = (byte)(arqType <<4 | type);
        buffer.put(flag);
        buffer.put((byte) dataLen);
        buffer.putInt(0);
        if(data != null)
            buffer.put(data);
        int crc = GetCRC(buffer.array(), dataLen+8);
        buffer.putInt(4, crc);
        buffer.flip();
        return buffer.array();
    }

    public int GetCRC(byte[] bytes, int length){
        CRC32 crc32 = new CRC32();
        crc32.update(bytes, 0, length);
        return (int)crc32.getValue();
    }

    protected boolean CheckCRC(byte[] bytes, int length){
        ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
        buffer.put(bytes);
        int crc = buffer.getInt(4);
        buffer.putInt(4, 0);
        buffer.flip();

        int crc2 = GetCRC(buffer.array(), length);
        System.out.printf("CRC : %x, %x\n", crc, crc2);
        if(crc == crc2){
            System.out.printf("Same\n");
        }
        return crc == crc2;
    }
}
