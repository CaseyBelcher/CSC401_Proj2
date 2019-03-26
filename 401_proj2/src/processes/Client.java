package processes; 

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.ByteBuffer;

import java.nio.charset.CharsetDecoder; 


public class Client {
    
    private static int MSS;

    public static void main ( String[] args ) {
        
        //initialize command line args
        int numServers = Integer.parseInt( args[0] );
        String[] servers = new String[numServers];
        for(int i = 1; i < 1 + numServers; i++) {
            servers[i - 1] = args[i];
        }
        int portNumber = Integer.parseInt( args[1 + numServers] );
        String fileName = args[2 + numServers];
        MSS = Integer.parseInt( args[3 + numServers] );
        
        try {
            final DatagramSocket socket = new DatagramSocket( );
            final InetAddress address = InetAddress.getByName( servers[0] );
            byte[] b = new byte[MSS];            
            String fileName2 = new File( "" ).getAbsolutePath();
            fileName2 = fileName2.concat( "/src/processes/" + fileName );            
            final InputStream is = new FileInputStream( fileName2 );
            int readBytes = 0;
            int seqNum = 0;
            while ( ( readBytes = is.read( b, 8, MSS - 8 ) ) != -1 ) {
                
                //sequence number
                byte[] bytes = intToByteArray(seqNum);
                b[0] = bytes[0];
                b[1] = bytes[1];
                b[2] = bytes[2];
                b[3] = bytes[3];
                
                //checksum
                b[4] = 0x00;
                b[5] = 0x00;
                
                
                //indicates data packet
                b[6] = 0x55;
                b[7] = 0x55;
                
                
                // checksum: 
                // break down packet into 16 bit / 2 byte / 1 char sections
                // convert to char so you can add them 
                // sum them, then one's complement the sum => checksum value 
                
                char sum = 0; 
                for(int i = 0; i < MSS / 2; i++) { 
                  char piece = byteArrayToChar( Arrays.copyOfRange(b, i, i+2) );
                  sum += piece; 
                }
                
                // complement 
                sum = (char) ~sum; 
                 
                byte[] checksumbytes = charToByteArray(sum); 
                b[4] = checksumbytes[0]; 
                b[5] = checksumbytes[1]; 
                
                
                
                DatagramPacket packet = new DatagramPacket( b, readBytes + 8, address, 7735 );
                socket.send( packet );
                System.out.println(readBytes + 8);
                
                
                // wait for all ACKs 
                int acksRecieved = 0; 
                byte[] ackBuffer = new byte[8]; 
                while(acksRecieved < numServers) { 
                  packet = new DatagramPacket(ackBuffer, ackBuffer.length); 
                  socket.receive(packet);
                  
                  int ackSequence = fromByteArray( Arrays.copyOfRange(packet.getData(), 0, 4) );
                  System.out.println("ACKed sequence: " + ackSequence);
                  
                  if(ackSequence == seqNum) { 
                    acksRecieved++; 
                  }
                }
                
                seqNum++; 
                
                
            }
            is.close();
            socket.close();

        }
        catch ( final IOException ioe ) {
            System.out.println( "Error " + ioe.getMessage() );
        }
    }
    
    public static final byte[] intToByteArray(int value) {
        return new byte[] {
            (byte)(value >>> 24),
            (byte)(value >>> 16),
            (byte)(value >>> 8),
            (byte)value};
    }
    
    public static int fromByteArray(byte[] bytes) {
      return bytes[0] << 24 | (bytes[1] & 0xFF) << 16 | (bytes[2] & 0xFF) << 8 | (bytes[3] & 0xFF);
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