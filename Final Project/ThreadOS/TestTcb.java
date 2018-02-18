import java.util.Date;
final byte[] buf16 = new byte[16];
class TestTcb extends Thread {

    public void run( ) {
	if ( test3( ) ) // write buf[16]
      SysLib.cout("Correct behavior of writing a few bytes.........2\n");
	SysLib.cout( "Test completed\n" );
    	SysLib.exit( );
    }
private boolean test3( ) {
    //.............................................."
	int fd = SysLib.open( "css430", "w+" );    
	SysLib.cout( "3: size = write( fd, buf[16] )...." );
    for ( byte i = 0; i < 16; i++ )
      buf16[i] = i;
    size = SysLib.write( fd, buf16 );

    if ( size != 16 ) {
      SysLib.cout( "size = " + size + " (wrong)\n" );
      return false;
    }
    SysLib.cout( "successfully completed\n" );
    return true;
  }

}
