package processes; 

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Random;

public class Server {
  
    private static int lastRecievedSequence = -1; 

    public static void main ( String[] args ) {
        try {
          
          // p2mpserver port# file-name p
            
            int portNumber = Integer.parseInt(args[0]);
            String fileName = args[1]; 
            double lossProbability = Double.parseDouble(args[2]); 
            
            
            final DatagramSocket socket = new DatagramSocket( portNumber );
            final InetAddress address = InetAddress.getByName( "localhost" );
            System.out.println(address);
            final byte[] b = new byte[1000];
            final OutputStream os = new FileOutputStream( fileName );

            Random r = new Random(); 
            boolean running = true;
            while ( running ) {
                final DatagramPacket packet = new DatagramPacket( b, b.length );
                socket.receive( packet );
                
                InetAddress clientAddress = packet.getAddress(); 
                int clientPort = packet.getPort(); 
                byte[] data = packet.getData();  
                int sequence = fromByteArray( Arrays.copyOfRange(data, 0, 4) );
                
                // simulate lost packet 
                double testForLoss = r.nextDouble();
                if(testForLoss <= lossProbability) { 
                  System.out.println("Packet loss, sequence number = " + (lastRecievedSequence+1));
                  continue; 
                }
                System.out.println("sequence: " + sequence);
                
                
                // TODO: checksum not working yet 
                // store checksum field then set to 0 for calculation 
                byte[] clientSum = new byte[2]; 
                clientSum[0] = data[4]; 
                clientSum[1] = data[5]; 
                
                data[4] = 0x00; 
                data[5] = 0x00; 
                
                // calculate checksum 
                char sum = 0; 
                for(int i = 0; i < data.length / 2; i++) { 
                  char piece = byteArrayToChar( Arrays.copyOfRange(data, i, i+2) );
                  sum += piece; 
                }
                sum = (char) ~sum; 
                
                // compare our checksum to client's header
                // if incorrect, do nothing and restart while loop 
                byte[] ourSum = charToByteArray(sum); 
                
//                if(clientSum[0] != ourSum[0] || clientSum[1] != ourSum[1]) { 
//                  
//                  System.out.println("clientSum[0]: " + clientSum[0]);
//                  System.out.println("ourSum[0]: " + ourSum[0]);
//                  System.out.println("clientSum[1]: " + clientSum[1]);
//                  System.out.println("ourSum[0]: " + ourSum[1]);
//                  
//                  continue; 
//                }
                

                
                byte[] ackB = new byte[8];
                byte[] ackSeq;
                
                // in sequence, sends ACK + write to file  
                if(sequence == lastRecievedSequence + 1) { 
                   
                  ackSeq = intToByteArray(++lastRecievedSequence);
                  
                  os.write( Arrays.copyOfRange( data, 8, packet.getLength() ) );
                 
                }
                // out of sequence, send ACK for last recieved packet 
                else { 
                  ackSeq = intToByteArray(lastRecievedSequence); 
                }
                
                // sequence number 
                ackB[0] = ackSeq[0];
                ackB[1] = ackSeq[1];
                ackB[2] = ackSeq[2];
                ackB[3] = ackSeq[3];
                
                // empty field 
                ackB[4] = 0x00; 
                ackB[5] = 0x00; 
                
                // indicates ACK packet 
                ackB[6] = (byte) 0xAA;
                ackB[7] = (byte) 0xAA;
                
                final DatagramPacket ackPacket = new DatagramPacket( ackB, 8, clientAddress, clientPort );
                socket.send( ackPacket );

                
            }
            os.close();
            socket.close();

        }
        catch ( final IOException ioe ) {
            System.out.println( "Error " + ioe.getMessage() );
        }
    }
    
    static int fromByteArray(byte[] bytes) {
     return bytes[0] << 24 | (bytes[1] & 0xFF) << 16 | (bytes[2] & 0xFF) << 8 | (bytes[3] & 0xFF);
    }
    
    public static final byte[] intToByteArray(int value) {
      return new byte[] {
          (byte)(value >>> 24),
          (byte)(value >>> 16),
          (byte)(value >>> 8),
          (byte)value};
    }
    
    public static byte[] charToByteArray(char thisChar) {
      char[] chars = new char[1]; 
      chars[0] = thisChar; 
      CharBuffer charBuffer = CharBuffer.wrap(chars);
      ByteBuffer byteBuffer = Charset.forName("UTF-8").encode(charBuffer);
      byte[] bytes = Arrays.copyOfRange(byteBuffer.array(),
                byteBuffer.position(), byteBuffer.limit());
      Arrays.fill(byteBuffer.array(), (byte) 0); 
      return bytes;
    }
    
    public static char byteArrayToChar(byte[] bytes) { 
      ByteBuffer byteBuffer = ByteBuffer.wrap(bytes); 
      CharBuffer charBuffer = Charset.forName("UTF-8").decode(byteBuffer);
      return charBuffer.get(0); 
    }

}