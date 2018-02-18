/*******************************************************************************
 * @class Test3.java
 *
 * @description This class will run two different classes a specified amount
 * of time that the user specifies. TestThread3a and TestThread3b will run
 * each in their own loop. After, two different loops will wait for the
 * termination of each of these TestThreads. Finally the Test will exit
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
import java.util.Date;
import java.util.*;
class Test3 extends Thread {
    	private int pairExecutions;
        private long startTime;
	private long completionTime; 

    public Test3(String[] args) {
        pairExecutions = Integer.parseInt(args[0]);
    }

    public void run() {
        startTime = new Date( ).getTime( );

	String[] compArgs = SysLib.stringToArgs("TestThread3a");
        String[] readWriteArgs = SysLib.stringToArgs("TestThread3b");
	
	//pair execution loop
        for (int i = 0; i < pairExecutions; i++) {
            SysLib.exec(compArgs);
	    SysLib.exec(readWriteArgs);
        }

	//wait and join for executed pairs
        for (int i = 0; i < pairExecutions; i++) {
            SysLib.join();

        }
        for (int i = 0; i < pairExecutions; i++) {
            SysLib.join();

        }
	completionTime = new Date( ).getTime( );
	SysLib.cout("total time = " + (completionTime - startTime) + " msec \n");
        SysLib.exit();
    }
}
