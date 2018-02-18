/*******************************************************************************
 * @class Test4.java
 *
 * @description This class is meant to test the cache class and give
 * performace times with and without caching when writing and reading blocks
 * of bytes. It has five modes. The first being Random access mode which will
 * read and write from random locations. Next being Localized access mode
 * which will read and write a small selection multiple times within a cache.
 * Mixed Access mode which will be a 90-10 split of the first two modes.
 * Adversary mode which will make the cache perform poorly. And the last mode
 * will run them all.
 *
 * This class takes in two parameters, the first being enabling the cache and
 * the second being the test case you want.
 * l Test4 [enabled|disabled] [1-5]
 *
 *
 * @author Cameron Padua
 * @date May 21, 2017
 *
 * Limitations:
 *
 * Assumptions: I was unsure if you wanted to enabled caching with -cache or
 * cache, so I made it check for the word enable in the first parameter.
 *
 *
 ******************************************************************************/
import java.util.Date;
import java.util.Random;

public class Test4 extends Thread {
    private boolean useCache;
    private int testCase;
    private Random rand;

    private long writeStart;
    private long writeEnd;
    private long readStart;
    private long readEnd;

    private byte[] writeData;
    private byte[] readData;
//---------------------Default Contructor-------------------------------------
    /*This constructor will read in the two parameters from the user and set
    the global arrays that will store the data that will be written and read.
     */
    public Test4(String[] paramArrayOfString) {
        useCache = (paramArrayOfString[0].contains("enabled"));
        testCase = Integer.parseInt(paramArrayOfString[1]);

        writeData = new byte[512];
        readData = new byte[512];

        rand = new Random();
        rand.nextBytes(writeData);
    }
    //--------------------------- run ---------------------------------
    /*This method will flush the  and switch through the test cases based on
    the user provide second paramter. If the paramer/case does not exist,
    defaults to letting the user know. Then it will sync the disk and exit.
     */
    public void run() {
        SysLib.flush();

        switch (testCase) {
            case 1:
                randomAccesses();
                break;
            case 2:
                localizedAccesses();
                break;
            case 3:
                mixedAccesses();
                break;
            case 4:
                adversaryAccesses();
                break;
            case 5:
                randomAccesses();
                localizedAccesses();
                mixedAccesses();
                adversaryAccesses();
                break;
            default:
                SysLib.cout("not a valid test case\n");
        }
        sync();
        SysLib.exit();
    }
    //--------------------- randomAccesses-------------------------------------
    /*This method will randomly choose blockID that will be either in the
    cache or on the disk. It does this 150 times before computing differences
     between the read and write data and the performace results.
     */
    private void randomAccesses() {
        //random locations computation
        int[] randomBlocks = new int[150];
        for (int i = 0; i < 150; i++) {
            randomBlocks[i] = rand.nextInt(512);
        }
        //writes
        writeStart = new Date().getTime();
        for (int i = 0; i < 150; i++) {
            write(randomBlocks[i], writeData);
        }
        writeEnd = new Date().getTime();
        //reads
        readStart = new Date().getTime();
        for (int i = 0; i < 150; i++) {
            read(randomBlocks[i], readData);
        }
        readEnd = new Date().getTime();

        compareReadWrite();
        calculatePerformance("Random Access");
    }
    //--------------------- localizedAccesses----------------------------------
    /*This method will loop through the same 10 cache locations 150 times
    writing to them. Then it will read from those 10 locations 150 tomes.
    Then computes difference and the performance results.
     */
    private void localizedAccesses() {
        //writes
        writeStart = new Date().getTime();
        for (int i = 0; i < 150; i++) {
            for (int j = 0; j < 10; j++) {
                write(j, writeData);
            }
        }
        writeEnd = new Date().getTime();
        //reads
        readStart = new Date().getTime();
        for (int i = 0; i < 150; i++) {
            for (int j = 0; j < 10; j++) {
                read(j, readData);
            }
        }
        readEnd = new Date().getTime();

        compareReadWrite();
        calculatePerformance("Localized Access");
    }
    //--------------------- mixedAccesses----------------------------------
    /*This method will create an array of blockID that consists of 150
    locations that is made up of a 90 to 10 ratio of local to random accesses
    . Then it will loop 150 times writing and reading data from the cache and
     disk
     */
    private void mixedAccesses() {
        //creates 90-10 mixed accesses
        int[] mixedBlocks = new int[150];
        for (int i = 0; i < 150; i++) {
            if (rand.nextInt(10) == 9) { //random
                mixedBlocks[i] = rand.nextInt(512);
            } else {  //local
                mixedBlocks[i] = rand.nextInt(10);
            }
        }
        //writes
        writeStart = new Date().getTime();
        for (int i = 0; i < 150; i++) {
            write(mixedBlocks[i], writeData);
        }
        writeEnd = new Date().getTime();
        //reads
        readStart = new Date().getTime();
        for (int i = 0; i < 150; i++) {
            read(mixedBlocks[i], readData);
        }
        readEnd = new Date().getTime();

        compareReadWrite();
        calculatePerformance("Mixed Access");
    }
    //--------------------- adversaryAccesses----------------------------------
    /*This method is suppose to make the worst use of the cache as possible.
    In theory this method should make the program fetch about 500 of the
    accesses from the disk because it will never has the same two numbers
    being called. It does 512 writes and reads to and from the disk/cache
    (primarily the disk).
     */
    private void adversaryAccesses() {
        //writes0
        writeStart = new Date().getTime();
        for (int i = 0; i < 512; i++) {
            write(i, writeData);
        }
        writeEnd = new Date().getTime();
        //reads
        readStart = new Date().getTime();
        for (int i = 0; i < 512; i++) {
            read(i, readData);
        }
        readEnd = new Date().getTime();

        compareReadWrite();
        calculatePerformance("Adversary Access");
    }
    //--------------------- write----------------------------------
    /*This method will decide if we are using the cache write or disk write
    based on the user parameter given at instatiation.
     */
    private void write(int blockID, byte[] buffer) {
        if (useCache == true) {
            SysLib.cwrite(blockID, buffer);
        } else {
            SysLib.rawwrite(blockID, buffer);
        }
    }
    //--------------------- read----------------------------------
    /*This method will decide if we are using the cache read or disk read
    based on the user parameter given at instatiation.
     */
    private void read(int blockID, byte[] buffer) {
        if (useCache == true) {
            SysLib.cread(blockID, buffer);
        } else {
            SysLib.rawread(blockID, buffer);
        }
    }
    //--------------------- calculatePerformance-------------------------------
    /*This method will calculate that average write and read of each test
    based on global variable set after each test.
     */
    private void calculatePerformance(String testType) {
        SysLib.cout("Test Type: " + testType + ". ");
        SysLib.cout("(cache enabled?: " + useCache + ") ");
        SysLib.cout("Avg Write(" + ((writeEnd - writeStart) / 150) + " " +
                "msec), ");
        SysLib.cout("Read(" + ((readEnd - readStart) / 150) + " msec)\n");
    }
    //--------------------- compareReadWrite-------------------------------
    /*This method will compare each data item in the write and readData and
    print out if there are any differences.
     */
    private void compareReadWrite() {
        for (int i = 0; i < 150; i++) {
            if (writeData[i] != readData[i]) {
                SysLib.cout("Blocks " + i + " are not the same\n");
            }
        }
    }
}
