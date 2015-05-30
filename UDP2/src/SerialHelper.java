import java.util.ArrayList;
import java.util.Enumeration;

import javax.comm.CommPortIdentifier;
import javax.comm.PortInUseException;
import javax.comm.SerialPort;


public class SerialHelper {

	static public ArrayList<CommPortIdentifier> GetSerialPorts(){
		ArrayList<CommPortIdentifier> ret = new ArrayList<CommPortIdentifier>();
		
		Enumeration portList = CommPortIdentifier.getPortIdentifiers();
		CommPortIdentifier portId;
		while (portList.hasMoreElements()) {
            portId = (CommPortIdentifier) portList.nextElement();
            if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                ret.add(portId);
            }
        }
		
		return ret;
	}
}
