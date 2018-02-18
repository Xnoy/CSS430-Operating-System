/*File Name: SuperBlock.java

  Description: This class is meant to be the first block in the disk. It represents the total number of inodes, blocks,
  and the next free block for writing. I has the ability to write the superblock to disk, format the superblock, find a
  free block, and return a block back to the free list.

  Assumptions:

 */
public class SuperBlock {
    public int totalBlocks;
    public int totalInodes;
    public int freeList;

    /*  Constructor
        This method is responsible for getting the superblock from the disk and checking if the values are valid. It
        does this by comparing the values read from the disk to the int passed in representing the total number of block
        in the disk. If they are not the same, the superblock will be formated
     */
    public SuperBlock(int diskSize) {
        byte[] superBlock = new byte[512];
        try {
            SysLib.rawread(0, superBlock);

            totalBlocks = SysLib.bytes2int(superBlock, 0);
            totalInodes = SysLib.bytes2int(superBlock, 4);
            freeList = SysLib.bytes2int(superBlock, 8);

            if (totalBlocks != diskSize || totalInodes <= 0 || freeList < 2) {
                totalBlocks = diskSize;
                format(64);
            }
        } catch (NullPointerException e) {
            totalBlocks = diskSize;
            format(64);
        }
    }

    /*sync
        This method is responsible for writing the values of the superblock to the disk. This can happen during an
        change in the values in the superblock.
     */
    public void sync() {
        byte[] data = new byte[512];

        SysLib.int2bytes(totalBlocks, data, 0);
        SysLib.int2bytes(totalInodes, data, 4);
        SysLib.int2bytes(freeList, data, 8);

        SysLib.rawwrite(0, data);
    }

    /*  findFreeBlock
        This method will find the next free block in the freeList queue and reset the freeList int to the next item in
        the freelist queue. If there is no value in the freelist queue, it will return -1, otherwise it will return the
        block number of the freeblock
     */
    public int findFreeBlock() {
        if (freeList < totalBlocks && freeList > 0) {
            byte[] temp = new byte[512];
            SysLib.rawread(freeList, temp);

            int tempNum = freeList;

            freeList = SysLib.bytes2int(temp, 0);

            return tempNum;
        }
        return -1;
    }

    /*returnBlockToFreeList
        As the name implies, this method will return an block no longer in use to the freeList queue. It will walk to
        the end of the freeList queue, reading the first value for a -1 (meaning the end of the list) and then creating
        the new block for use and updating the next/last block number in the previous block.
     */
    public boolean returnBlockToFreeList(int blockNum) {
        if (blockNum > 0 && blockNum < totalBlocks) {
            int currentFree = freeList;
            int tempNext = 0;

            byte[] next = new byte[512];
            byte[] newBlock = new byte[512];

            //clean block
            for (int i = 0; i < 512; i++) {
                newBlock[i] = 0;
            }
            SysLib.int2bytes(-1, newBlock, 0);

            //until the end of freeList queue
            while (currentFree != -1) {
                SysLib.rawread(currentFree, next);
                tempNext = SysLib.bytes2int(next, 0);

                //end of the free list
                if (tempNext == -1) {
                    SysLib.int2bytes(blockNum, next, 0);
                    SysLib.rawwrite(currentFree, next);
                    SysLib.rawwrite(blockNum, newBlock);

                    return true;
                }
                currentFree = tempNext;
            }
        }
        return false;
    }

    /*  format
        This method will format the entire disk reseting values to their default. It will walk the entire disk setting
        the block number and the bytes to 0. Lastly, it will create a new superblock with default values and write to
        disk.
     */
    public void format(int numberOfBlocks) {
        if (numberOfBlocks < 0) {
            numberOfBlocks = 64;
        }
        totalInodes = numberOfBlocks;
        Inode temp = null;

        //resetting inodes
        for (int i = 0; i < totalInodes; i++) {
            temp = new Inode();
            temp.flag = 0;
            temp.toDisk((short) i);
        }
        freeList = (totalInodes / 16) + 2; //freelist location in bytes

        byte[] emptyBlock = null;

        //cleaning blocks with blocknumbers
        for (int i = freeList; i < 1000 - 1; i++) {
            emptyBlock = new byte[512];

            for (int j = 0; j < 512; j++) {
                emptyBlock[j] = 0;
            }

            SysLib.int2bytes(i + 1, emptyBlock, 0);
            SysLib.rawwrite(i, emptyBlock);
        }

        //free data
        emptyBlock = new byte[512];
        for (int j = 0; j < 512; j++) {
            emptyBlock[j] = 0;
        }
        SysLib.int2bytes(-1, emptyBlock, 0);
        SysLib.rawwrite(1000 - 1, emptyBlock);

        //superblock replacemnent
        byte[] replacement = new byte[512];
        // set to default values
        sync();

    }
}
