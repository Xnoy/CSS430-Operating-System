/*******************************************************************************
 * @class SyncQueue.java
 *
 * @description SyncQueue will create an array of QueueNode objects
 * upon instantiation. Once created, SyncQueue has two methods for adding and
 * removing threads from the QueueNodes. Those are enqueueAndSleep and
 * dequeueAndWakeUp; they use QueueNode sleep and wakeup as need to store
 * the threads inside the SyncQueue array's QueueNode.
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
public class SyncQueue {
    private final int CONDMAX_DEFAULT = 10;
    private QueueNode[] queue;
    /*-------------------------Default Constructor-------------------
        This constuctor will build a syncqueue of size 10
     */
    public SyncQueue() {
        intializeQueue(CONDMAX_DEFAULT);
    }
    /*------------------- Constructor-------------------
        This constuctor will build a syncqueue of a size specified by the user
     */
    public SyncQueue(int condMax) {
        intializeQueue(condMax);
    }
    /*-------------------------initializeQueue-------------------
        This method is responsible for creating the QueueNode in each array
        location of the SyncQueue. It accepts the length of the SyncQueue for
         the loop and how big to make the array for SyncQueue
     */
    private void intializeQueue(int condMax) {
        queue = new QueueNode[condMax];
        for (int i = 0; i < condMax; i++) {
            queue[i] = new QueueNode();
        }
    }
    /*-------------------------enqueueAndSleep-------------------
       This method will make sure that the thread being passed in is within
       the size of the SyncQueue array. Next it will set that location to
       sleep/wait for a notify
     */
    public int enqueueAndSleep(int condition) {
        if (condition >= 0 && condition < queue.length) {
            return queue[condition].sleep();
        } else {
            return -1;
        }
    }
    /*-------------------------dequeueAndWakeup-------------------
        This method is reponsible for waking up/notifying a thread to wakeup.
         One method accepts the tid and the condition to wakeup. The other
         defaults to waking up tid 0 and calling the other method with the
         condition that it was passed
       */
    public void dequeueAndWakeup(int condition, int tid) {
        if (condition >= 0 && condition < queue.length) {
            queue[condition].wakeup(tid);
        }
    }

    public void dequeueAndWakeup(int condition) {
        dequeueAndWakeup(condition, 0);
    }
}
