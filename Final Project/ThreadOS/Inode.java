/*  Cameron Padua
    Peter Van

    Inode.java
    This class will keep track of all data blocks related to a file. It has
    11 direct blocks and 1 indirect block containing 256 pointers to data.
 */
public class Inode {
    private final static int iNodeSize = 32;       // fix to 32 bytes
    private final static int directSize = 11;      // # direct pointers
    private final static int BLOCK_SIZE = 16;
    public int length;                             // file size in bytes
    public short count;                            // # file-table entries pointing to this
    public short flag;                             // 0 = unused, 1 = used, ...
    public short direct[] = new short[directSize]; // direct pointers
    public short indirect;                         // a indirect pointer

    // default constructor
    public Inode() {                                     // a default constructor
        length = 0;
        count = 0;
        flag = 1;
        for (int i = 0; i < directSize; i++)
            direct[i] = -1;
        indirect = -1;
    }

    // constructor that fills in data fields with parameter
    public Inode(short iNumber) {                       // retrieving inode from disk
        // design it by yourself.
        int blockNumber = 1 + iNumber / BLOCK_SIZE;
        byte buffer[] = new byte[512];
        SysLib.rawread(blockNumber, buffer);
        int offset = iNumber % 16 * 32;
        length = SysLib.bytes2int(buffer, offset);
        offset += 4;
        count = SysLib.bytes2short(buffer, offset);
        offset += 2;
        flag = SysLib.bytes2short(buffer, offset);
        offset += 2;
        for (int i = 0; i < directSize; i++) {
            direct[i] = SysLib.bytes2short(buffer, offset);
            offset += 2;
        }
        indirect = SysLib.bytes2short(buffer, offset);
    }

    // Saves inode data to disk
    int toDisk(short iNumber) {                  // save to disk as the i-th inode
        // design it by yourself.
        int blockNumber = 1 + iNumber / BLOCK_SIZE;
        byte buffer[] = new byte[32];
        SysLib.int2bytes(length, buffer, 0);
        SysLib.short2bytes(count, buffer, 4);
        SysLib.short2bytes(flag, buffer, 6);
        for (int i = 0; i < directSize; i++) {
            SysLib.short2bytes(direct[i], buffer, 8 + 2 * i);
        }
        SysLib.short2bytes(indirect, buffer, 30);
        int offset = iNumber % 16 * 32;
        byte data[] = new byte[512];
        SysLib.rawread(blockNumber, data);
        System.arraycopy(buffer, 0, data, offset, 32);
        SysLib.rawwrite(blockNumber, data);
        return 0;
    }

    /*This method will check that all blocks are not being used. If the block
     is being used or it being written to, it will error.

    */
    int whereToIndex(int entry, SuperBlock superBlock) {
        short newLocation = (short) superBlock.findFreeBlock();

        int errorcase = whereToIndexHelper(entry, newLocation);

        if (errorcase == -3) {
            short freeBlock = (short) superBlock.findFreeBlock();

            if (!setIndirectBlockPointer(freeBlock)) {
                return -1;
            }

            if (whereToIndexHelper(entry, newLocation) != 0) {
                return -1;
            }

        } else if (errorcase == -2 || errorcase == -1) {
            return -1;
        }
        return newLocation;
    }

    int whereToIndexHelper(int entry, short offset) {
        int target = entry / 512;

        if (target < directSize) {
            if (direct[target] >= 0) {
                return -1;
            }

            if ((target > 0) && (direct[target - 1] == -1)) {
                return -2;
            }

            direct[target] = offset;
            return 0;
        }

        if (indirect < 0) {
            return -3;
        } else {
            byte[] data = new byte[512];
            SysLib.rawread(indirect, data);

            int blockSpace = (target - directSize) * 2;
            if (SysLib.bytes2short(data, blockSpace) > 0) {
                return -1;
            } else {
                SysLib.short2bytes(offset, data, blockSpace);
                SysLib.rawwrite(indirect, data);
            }
        }
        return 0;
    }

    /*
        This method will return a direct block location if the location is a less
        than 12.
        If it is accessing a indirect block it will return -1.
     */
    int findTargetLocationBlock(int location) {
        int block = location / 512;

        if (block < directSize) {
            return direct[block];
        } else if (indirect == -1) {
            return -1;
        }

        byte[] data = new byte[512];
        SysLib.rawread(indirect, data);

        //location short in byte size
        int blockLocation = (block - directSize) * 2;
        return SysLib.bytes2short(data, blockLocation);
    }

    /*This method will change the indirect block reference if all the direct
    blocks are full and the indirect block is not -1.

     */
    boolean setIndirectBlockPointer(short blockNumber) {
        for (int i = 0; i < directSize; i++) {
            if (direct[i] == -1) {
                return false;
            }
        }

        if (indirect != -1) {
            return false;
        }

        indirect = blockNumber;
        byte[] data = new byte[512];

        for (int i = 0; i < (512 / 2); i++) {
            SysLib.short2bytes((short) -1, data, i * 2);
        }
        SysLib.rawwrite(blockNumber, data);

        return true;

    }
}
