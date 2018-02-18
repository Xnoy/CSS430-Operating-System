/*  Cameron Padua
    Peter Van

    FileTableEntry.java
    This contains a pointer to a inode, the inode number, the mode of the
    file, the current location in the file, and how many threads or other are
     using this entry.

 */
public class FileTableEntry {          // Each table entry should have
    public final Inode inode;           //    a reference to its inode
    public final short iNumber;         //    this inode number
    public final String mode;           //    "r", "w", "w+", or "a"
    public int seekPtr;                 //    a file seek pointer
    public int count;                   //    # threads sharing this entry

    public FileTableEntry(Inode i, short inumber, String m) {
        seekPtr = 0;             // the seek pointer is set to the file top
        inode = i;
        iNumber = inumber;
        count = 1;               // at least on thread is using this entry
        mode = m;                // once access mode is set, it never changes
        if (mode.compareTo("a") == 0) // if mode is append,
            seekPtr = inode.length;        // seekPtr points to the end of file
    }
}