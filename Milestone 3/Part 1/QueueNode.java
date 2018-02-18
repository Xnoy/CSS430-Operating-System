/*******************************************************************************
 * @class TestThread3a.java
 *
 * @description QueueNode is responsible for creating a Vector that will hold
 * an integer representing the Thread ID. With the sleep and wakeup, it can put
 * threads to sleep and wake them up while in the critical section.
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
import java.util.Vector;

public class QueueNode {
    private Vector threads;
    /*-------------------------Default Constructor-------------------
        This constuctor will set threads to a new Vector
     */
    public QueueNode() {
        threads = new Vector();
    }
    /*-------------------Sleep-----------------
        This method will check the size of the threads vector, if there is
        nothing inside, it will wait. Then remove that item from the vector.
     */
    public synchronized int sleep() {
        if (threads.size() == 0) {
            try {
                wait();
            } catch (InterruptedException localInterruptedException) {
            }
            return (int) threads.remove(0);
        }
        return -1;


    }
    /*-------------------wakeup-----------------
        This method is responsible for adding a thread to the vector and
        notifying the vector that it has to wakeup.
       */
    public synchronized void wakeup(int tid) {
        threads.add(tid);
        notify();
    }
}

