/**
 * Created by inosphe on 15. 5. 23..
 */
public class ProtocolHDLC extends ProtocolThreadBase {
    public ProtocolHDLC(ChatSystem system, Connection connection){
        super(system, connection);
    }

    @Override
    protected Frame BuildReceiveFrame(byte[] bytes){
    	if(bytes == null)
    		return null;
        HDLCFrame hdlcFrame = new HDLCFrame(bytes);
        return hdlcFrame.BuildFrame(false);
    }

    @Override
    protected Frame BuildSendFrame(String str, int reqSeqNo, int ackSeqNo){
        HDLCFrame hdlcFrame = new HDLCFrame(HDLCFrame.FLAG_TYPE_DATA);
        switch(GetARQType()){
            case ARQ_TYPE_NOARQ:
                hdlcFrame.arqType = HDLCFrame.ARQ_TYPE_NOARQ;
                break;
            case ARQ_TYPE_STOP_N_WAIT:
                hdlcFrame.arqType = HDLCFrame.ARQ_TYPE_STOP_N_WAIT;
                break;
            case ARQ_TYPE_GO_BACK_N:
                hdlcFrame.arqType = HDLCFrame.ARQ_TYPE_GO_BACK_N;
                break;
        }

        hdlcFrame.ackNumber = ackSeqNo;
        hdlcFrame.seqNumber = reqSeqNo;
        hdlcFrame.data = str.getBytes();
        return hdlcFrame.BuildFrame(true);
    }

    @Override
    public Frame BuildAckFrame(boolean accepted, int reqSeqNo, int ackSeqNo){
        HDLCFrame hdlcFrame;
        if(accepted)
            hdlcFrame = new HDLCFrame(HDLCFrame.FLAG_TYPE_ACK);
        else
            hdlcFrame = new HDLCFrame(HDLCFrame.FLAG_TYPE_NAK);

        switch(GetARQType()){
            case ARQ_TYPE_NOARQ:
                hdlcFrame.arqType = HDLCFrame.ARQ_TYPE_NOARQ;
                break;
            case ARQ_TYPE_STOP_N_WAIT:
                hdlcFrame.arqType = HDLCFrame.ARQ_TYPE_STOP_N_WAIT;
                break;
            case ARQ_TYPE_GO_BACK_N:
                hdlcFrame.arqType = HDLCFrame.ARQ_TYPE_GO_BACK_N;
                break;
        }

        hdlcFrame.ackNumber = ackSeqNo;
        hdlcFrame.seqNumber = reqSeqNo;

        return hdlcFrame.BuildFrame(true);
    }


}
