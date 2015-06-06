
public class StopAndWaitARQ extends GoBackNARQ{
	public StopAndWaitARQ(Protocol protocol){
		super(protocol, 2);
	}
	
	@Override
	public String ToString(){
		return "Stop-And-Wait"; 
	}
	
	@Override
    public int GetType(){
    	return Protocol.ARQ_TYPE_STOP_N_WAIT;
    }
}
