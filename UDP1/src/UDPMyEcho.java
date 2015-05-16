/**
 * Created by inosphe on 15. 5. 10..
 */
import java.net.*;
import java.io.*;
public class UDPMyEcho {
    final static int MAXBUFFER = 512;
    public static void main(String[ ] args)
    {
        if (args.length != 2) {System.out.println("사용법: java UDPMyEcho localhost port"); System.exit(0);}

        int arg_port = Integer.parseInt(args[1]);
        UDPMyEcho udp_client = new UDPMyEcho();
        udp_client.work(args[0], arg_port);
    }
    void work(String host_name, int arg_port){
        int port = arg_port;
        try{
            InetAddress inetaddr = InetAddress.getByName(host_name);
            DatagramSocket socket = new DatagramSocket();
            DatagramPacket send_packet, recv_packet;
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

            byte send_buffer[];     //송신용 buffer
            byte recv_buffer[] = new byte[MAXBUFFER];       //echo 수신용 buffer

            while(true){

                System.out.print("Input Data : ");
                String data = br.readLine();
                if(data.length() == 0)      //입력이 없으면 종료.
                    break;
                send_buffer = data.getBytes();

                //입력으로부터 데이터 송신용 datagram packet 생성 | 서버의 inetaddr, port 포함
                send_packet = new DatagramPacket(send_buffer, send_buffer.length, inetaddr, port);
                socket.send(send_packet);

                //에코 데이터 수신
                recv_packet = new DatagramPacket(recv_buffer, MAXBUFFER);
                socket.receive(recv_packet);

                InetAddress inetaddr_recv = recv_packet.getAddress();   //수신한 패킷의 주소정보 추출
                String result = new String(recv_buffer, 0, recv_packet.getLength());    //수신한 패킷의 텍스트 내용 추출
                //화면 출력
                System.out.println("Echo | " + inetaddr_recv.getHostName() + "(" + inetaddr_recv.getHostAddress() + ":"+recv_packet.getPort()+") : " + result);
            }
        }
        catch(UnknownHostException ex){
            System.out.println("Error in the host address");
        }
        catch(IOException e){
            System.out.println(e);
        }
    }
}
