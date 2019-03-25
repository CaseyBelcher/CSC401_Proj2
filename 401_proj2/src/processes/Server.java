import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

public class Server {

    public static void main ( String[] args ) {
        try {
            final DatagramSocket socket = new DatagramSocket( 7735 );
            final InetAddress address = InetAddress.getByName( "localhost" );
            System.out.println(address);
            final byte[] b = new byte[1000];
            final OutputStream os = new FileOutputStream( "fileToPrint.txt" );

            final int readBytes = 0;
            boolean running = true;
            while ( running ) {
                final DatagramPacket packet = new DatagramPacket( b, b.length );
                socket.receive( packet );
                int sequence = fromByteArray( Arrays.copyOfRange(packet.getData(), 0, 4) );
                System.out.println(sequence);
                os.write( Arrays.copyOfRange( packet.getData(), 8, packet.getLength() ) );
                if( b.length == 0) {
                    running = false;
                }
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

}