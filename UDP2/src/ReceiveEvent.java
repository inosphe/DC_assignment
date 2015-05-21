import java.util.EventObject;

/**
 * Created by inosphe on 15. 5. 17..
 */
public class ReceiveEvent extends EventObject{
    private String str;
    public ReceiveEvent(Object source) {
        super(source);
        str = (String) source;
    }
    public String GetString(){
        return str;
    }

}