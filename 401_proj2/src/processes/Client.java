import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

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
            final InputStream is = new FileInputStream( fileName );

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
                final DatagramPacket packet = new DatagramPacket( b, readBytes + 8, address, 7735 );
                socket.send( packet );
                System.out.println(readBytes + 8);
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

}