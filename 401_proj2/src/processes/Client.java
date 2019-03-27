package processes; 

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.ByteBuffer;

import java.nio.charset.CharsetDecoder; 


public class Client {
    
    private static int MSS;

    public static void main ( String[] args ) {
        
        long startProgramTime = System.currentTimeMillis(); 
      
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
//            final InetAddress address = InetAddress.getByName( servers[0] );
            byte[] b = new byte[MSS];            
            String fileName2 = new File( "" ).getAbsolutePath();
            fileName2 = fileName2.concat( "/src/processes/" + fileName );            
            final InputStream is = new FileInputStream( fileName2 );
            int readBytes = 0;
            int seqNum = 0;
            
            // initial volley to determine RTT and timeout 
//            byte[] volleyBytes = new byte[4]; 
//            byte[] tempSeqBytes = intToByteArray(-1);
//            volleyBytes[0] = tempSeqBytes[0];
//            volleyBytes[1] = tempSeqBytes[1];
//            volleyBytes[2] = tempSeqBytes[2];
//            volleyBytes[3] = tempSeqBytes[3];
//            
//            InetAddress tempAddress = InetAddress.getByName( servers[0] );
//            DatagramPacket tempPacket = new DatagramPacket( volleyBytes, 4, tempAddress, 7735 );
//           
//            
//            long startTime = System.currentTimeMillis();  
//            socket.send( tempPacket );
//            
//            byte[] volleyReturn = new byte[4]; 
//            
//            DatagramPacket tempPacket2 = new DatagramPacket(volleyReturn, volleyReturn.length); 
//            socket.receive(tempPacket2); 
//            long endTime = System.currentTimeMillis(); 
//            
//            int timeout = (int) (endTime - startTime) * 2; 
            
            
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
                
                
                
                for(int i = 0; i < servers.length; i++) { 
                  InetAddress address = InetAddress.getByName( servers[i] );
                  DatagramPacket packet = new DatagramPacket( b, readBytes + 8, address, 7735 );
                  socket.send( packet );
                }
                socket.setSoTimeout(1);
                System.out.println(readBytes + 8);
                
                
                // wait for all ACKs 
                int acksRecieved = 0; 
                byte[] ackBuffer = new byte[8];
//                HashSet<InetAddress> addressesRemaining = new HashSet<InetAddress>(); 
                ArrayList<InetAddress> addressesRemaining = new ArrayList<InetAddress>(); 
                for(int i = 0; i < servers.length; i++) { 
                  addressesRemaining.add(InetAddress.getByName(servers[i])); 
                }
                
                a: while(!addressesRemaining.isEmpty()) { 
                   try {  
                    DatagramPacket packet = new DatagramPacket(ackBuffer, ackBuffer.length); 
                    socket.receive(packet);
                    
                    int ackSequence = fromByteArray( Arrays.copyOfRange(packet.getData(), 0, 4) );
                    System.out.println("ACKed sequence: " + ackSequence);
                    
                    if(ackSequence == seqNum) { 
                      acksRecieved++; 
                      addressesRemaining.remove(packet.getAddress()); 
                    }
                   
                   }
                   catch(SocketTimeoutException e) { 
                     System.out.println("Timeout, sequence number = " + seqNum);
                     for(int i = 0; i < addressesRemaining.size(); i++) { 
                       DatagramPacket packet = new DatagramPacket( b, readBytes + 8, addressesRemaining.get(i), 7735 );
                       socket.send( packet );
                     }
                     continue a;
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
        
        
        
        long endProgramTime = System.currentTimeMillis(); 
        long runTime = endProgramTime - startProgramTime; 
        
        System.out.println("Runtime in ms: " + runTime);
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