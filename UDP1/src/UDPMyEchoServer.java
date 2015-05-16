/**
 * Created by inosphe on 15. 5. 10..
 */
import java.net.*;
import java.io.*;
public class UDPMyEchoServer {
    final int MAX_BUFFER = 512;
    public static void main(String[ ] args)
    {
        int arg_port = Integer.parseInt(args[0]);
        UDPMyEchoServer udp_server = new UDPMyEchoServer();
        udp_server.work(arg_port);
    }
    void work(int arg_port){
        int port = arg_port;
        try{
            /*
                UDP Socket 생성 (UDP Server Socket)
             */
            DatagramSocket Dsocket = new DatagramSocket(port);
            System.out.println("Running the UDP Echo Server");

            InetAddress inetLocalAddr = InetAddress.getLocalHost();
            System.out.println(inetLocalAddr.getHostName() + "("+inetLocalAddr.getHostAddress()+":"+port+")");

            while(true){
                //UDP Packet 생성(UDP server Socket으로부터 데이터 수신을 위한 UDP packet 생성)
                DatagramPacket packet = new DatagramPacket(new byte[MAX_BUFFER], MAX_BUFFER);

                //UDP Server Socket에서 UDP Packet을 받기 위한 대기
                Dsocket.receive(packet);

                InetAddress inetaddr_recv = packet.getAddress();
                System.out.println("recv from | " + inetaddr_recv.getHostName() + "(" + inetaddr_recv.getHostAddress() + ":"+packet.getPort()+")");

                //Echo 를 위한 송신 UDP packet 생성 (client의 address와 port를 이용)
                DatagramPacket send_packet = new DatagramPacket(packet.getData(), packet.getLength(), packet.getAddress(), packet.getPort());
                //받은 데이터를 재송신
                Dsocket.send(send_packet);
            }
        }catch(IOException e){
            System.out.println(e);
        }
    }
}
