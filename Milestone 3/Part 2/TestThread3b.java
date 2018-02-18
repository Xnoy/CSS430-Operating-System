/*******************************************************************************
 * @class TestThread3b.java
 *
 * @description This class was built to simulate read and writes onto the
 * DISK. This is done by going through all 1000 blocks of the disk, writing
 * and then reading from that block. Once the block is done, it will cout
 * that it has finished
 *
 *
 * @author Cameron Padua
 * @date May 7, 2017
 *
 * Limitations:
 *
 * Assumptions:
 *
 ******************************************************************************/
public class TestThread3b extends Thread {
    private byte[] data;

    public TestThread3b() {
    }

    public void run() {
        data = new byte[512];
        for (int i = 0; i < 1000; i++) {
            SysLib.rawwrite(i, data);
            SysLib.rawread(i, data);
        }
        SysLib.cout("done read write\n");
        SysLib.exit();
    }
}
