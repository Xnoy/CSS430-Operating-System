/*******************************************************************************
 * @class TestThread3a.java
 *
 * @description This class was built to simulate computations that will
 * stress the CPU. It does this by doing adding the running sum of a formula
 * that loops until 121234320.
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
public class TestThread3a extends Thread {
    public TestThread3a() {
    }

    public void run() {
        double runningSum = 0.0;
        for (int i = 1; i < 121234320; i++) {
            runningSum += Math.pow(Math.sqrt(Math.exp(i*i) * Math.sqrt(i*7.0)), 5.0);

        }
        SysLib.cout("done comp\n");
        SysLib.exit();
    }
}
