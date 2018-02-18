/*  Cameron Padua
    Peter Van

    File Name: FileTable.java

    Description: This class is meant to act as a set of file table entries.
    Each instance of this class is one file descriptor. This class will
    create new file table entries when they don't exist and will remove them
    when freed.

  Assumptions:

 */

import java.util.Vector;

public class FileTable {

    private Vector<FileTableEntry> table;// the actual entity of this file table
    private Directory dir;        // the root directory

    /* Constructor
        Will create a file table given a directory reference to find inodes
         numbers when needed.
     */
    public FileTable(Directory directory) { // constructor
        table = new Vector<FileTableEntry>();// instantiate a file
                                                // (structure) table
        dir = directory;           // receive a reference to the Director
    }                             // from the file system

    /*falloc
        This function is responsible for creating fileTableEntries when they
        do not exist in the filetable. When they do exist, this function will
        make sure that no other thread can access files that are being
        written to. When no thread has access to a file, this method can
        create and lock the file until the thread is done.
     */
    public synchronized FileTableEntry falloc(String filename, String mode) {
        // allocate a new file (structure) table entry for this file name
        // allocate/retrieve and register the corresponding inode using dir
        Inode iNodeHolder = null;
        short inodeNumber = -1;
        while (true) {
            if (filename.equals("/")) {
                inodeNumber = (short) 0;
            } else {
                inodeNumber = dir.namei(filename);
            }

            if (inodeNumber >= 0) { //valid iNode Number
                iNodeHolder = new Inode(inodeNumber);
                if (mode.equals("r")) { //read
                    if (iNodeHolder.flag == 2 || iNodeHolder.flag == 1 ||
                             iNodeHolder.flag == 0) {
                        iNodeHolder.flag = 2;
                        break;
                    } else if (iNodeHolder.flag == 3) { //someone is writing to this file
                        try {
                            wait();
                        } catch (Exception e) {
                        }
                    }
                } else { //write
                    if (iNodeHolder.flag == 1 || iNodeHolder.flag == 0) {
                        iNodeHolder.flag = 3;
                        break;

                        // if the flag is read or write, wait until they finish
                    } else {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                        }
                    }
                }
            } else if (!mode.equals("r")) {   //not a valid iNode Number
                //create the iNode in the directory
                inodeNumber = dir.ialloc(filename);
                iNodeHolder = new Inode(inodeNumber);
                iNodeHolder.flag = 3;
                break;
            } else {  //cannot read from a file that does not exist
                return null;
            }
            // increment this inode's count
            // immediately write back this inode to the disk
            // return a reference to this file (structure) table entry
        }
        iNodeHolder.count++;
        iNodeHolder.toDisk(inodeNumber);
        FileTableEntry entry = new FileTableEntry(iNodeHolder, inodeNumber, mode);
        table.addElement(entry);
        return entry;
    }

    /*ffree
        This function is responsible for closing a file opened by a thread
        and removing it from the table of open files. This function will wake
         up any threads waiting for reads or writes.
     */
    public synchronized boolean ffree(FileTableEntry e) {
        // receive a file table entry reference
        Inode inodeHolder = new Inode(e.iNumber);
        // save the corresponding inode to the disk
        // return true if this file table entry found in my table
        if (table.remove(e)) {
            if (inodeHolder.flag == 2) {
                if (inodeHolder.count == 1) {
                    // free this file table entry.
                    notify();
                    inodeHolder.flag = 1;
                }
            } else if (inodeHolder.flag == 3) {
                inodeHolder.flag = 1;
                notifyAll();
            }
            inodeHolder.count--;
            inodeHolder.toDisk(e.iNumber);

            return true;
        }
        return false;
    }

    public synchronized boolean fempty() {
        return table.isEmpty();  // return if table is empty
    }
}
