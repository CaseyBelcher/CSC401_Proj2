import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Client {
    
    private static int MSS = 100;

    public static void main ( String[] args ) {
        try {
            final DatagramSocket socket = new DatagramSocket( );
            final InetAddress address = InetAddress.getByName( "localhost" );
            byte[] b = new byte[MSS];
            final InputStream is = new FileInputStream( "fileToRead.txt" );

            int readBytes = 0;
            b[0] = 't';
            b[1] = 't';
            b[2] = 't';
            b[3] = 't';
            b[4] = 't';
            b[5] = 't';
            b[6] = 't';
            b[7] = 't';
            while ( ( readBytes = is.read( b, 8, MSS - 8 ) ) != -1 ) {
                final DatagramPacket packet = new DatagramPacket( b, readBytes + 8, address, 7735 );
                socket.send( packet );
                System.out.println(readBytes + 8);
            }
            is.close();
            socket.close();

        }
        catch ( final IOException ioe ) {
            System.out.println( "Error " + ioe.getMessage() );
        }
    }

}