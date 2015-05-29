/**
 * Created by inosphe on 15. 5. 23..
 */
public class Frame {
    final static public int TYPE_NONE = -1;
    final static public int TYPE_DATA = 0;
    final static public int TYPE_ACK = 1;
    final static public int TYPE_NACK = 2;

    int sendSeq = -1;
    int ackSeq = -1;
    String data = "";
    int type = TYPE_NONE;
    byte[] byteArray = null;
    boolean crcValidated = true;

    public String ToString(){
        String typeStr = "Invalid";
        switch(type){
            case TYPE_NONE: typeStr = "NONE"; break;
            case TYPE_DATA: typeStr = "DATA"; break;
            case TYPE_ACK: typeStr = "ACK"; break;
            case TYPE_NACK: typeStr = "NCK"; break;
        }

        return "Frame | Type("+typeStr+") Seq(" + sendSeq + "), Ack("+ackSeq+"), Data("+data+"), CRC passed("+crcValidated+")";
    }
}
