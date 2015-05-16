/**
 * Created by inosphe on 15. 5. 10..
 */
import java.net.*;
import java.io.*;
public class UDPMyEchoWithTimeout {
    final static int MAXBUFFER = 512;
    final static int TIMEOUT = 1000;
    final static int MAX_TRY_COUNT = 10;
    public static void main(String[ ] args)
    {
        if (args.length != 2) {System.out.println("사용법: java UDPMyEcho localhost port"); System.exit(0);}

        int arg_port = Integer.parseInt(args[1]);
        UDPMyEchoWithTimeout udp_client = new UDPMyEchoWithTimeout();
        udp_client.work(args[0], arg_port);
    }
    void work(String host_name, int arg_port){
        int port = arg_port;
        try{
            InetAddress inetaddr = InetAddress.getByName(host_name);
            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(TIMEOUT);   //소켓 타임아웃 설정
            DatagramPacket send_packet, recv_packet;
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

            byte send_buffer[]; //송신용 buffer
            byte recv_buffer[] = new byte[MAXBUFFER];  //echo 수신용 buffer

            while(true){
                System.out.print("Input Data : ");
                String data = br.readLine();
                if(data.length() == 0)
                    break;
                send_buffer = data.getBytes();
                //데이터 송신
                boolean send = true;
                //입력으로부터 데이터 송신용 datagram packet 생성 | 서버의 inetaddr, port 포함
                send_packet = new DatagramPacket(send_buffer, send_buffer.length, inetaddr, port);
                recv_packet = new DatagramPacket(recv_buffer, MAXBUFFER);
                int send_count = 0; //재송신이 일정 횟수 이상 넘어갔을 때의 처리를 위한 count value
                while(send) {
                    socket.send(send_packet);
                    try {
                        //에코 데이터 수신
                        socket.receive(recv_packet);
                        send = false;

                        InetAddress inetaddr_recv = recv_packet.getAddress();   //수신한 패킷의 주소정보 추출
                        String result = new String(recv_buffer, 0, recv_packet.getLength());    //수신한 패킷의 텍스트 내용 추출
                        //화면 출력
                        System.out.println("Echo | " + inetaddr_recv.getHostName() + "(" + inetaddr_recv.getHostAddress() + ":"+recv_packet.getPort()+") : " + result);
                    } catch (SocketTimeoutException e) {    //타임아웃 되었을 때 exception handling
                        // timeout exception.
                        ++send_count;
                        System.out.println("Timeout reached!!! (" + send_count + "/" + MAX_TRY_COUNT + ")");
                        if(send_count >= MAX_TRY_COUNT){
                            System.out.println("Retransmission reached maximum try(" + MAX_TRY_COUNT + ").");
                            System.out.println("Program will be shutdown.");
                            System.exit(0);
                        }
                    }
                }
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
