/*******************************************************************************
 * @class Cache.java
 *
 * @description This class is meant to act like a Cache between the disk and
 * the CPU for the ThreadOS. It will use the Second chance algorithim to find
 * a victim in the cache when it is full. If a victim is not found within the
 * first loop, it will choose the first block after the loop.
 *
 *
 * @author Cameron Padua
 * @date May 21, 2017
 *
 * Limitations:
 *
 * Assumptions: The SCA will choose the first block/victim after the first
 * loop per dimpsey's remark of looping twice or once, he does not care.
 *
 ******************************************************************************/

import java.io.*;

public class Cache {
    CacheBlock[] cache;
    private int cacheSize;
    private int cacheBlockSize;
    private int victim;

    //--------------------Default Constructor----------------------------------
    /*This constructor will take in the size of the cache blocks and the
	amount of cacheblocks and set the global variables. In addition, this
	method will create an array of CacheBlocks and then loop through to
	intialize those blocks.
	 */
    public Cache(int blockSize, int cacheBlocks) {
        cacheSize = cacheBlocks;
        cacheBlockSize = blockSize;
        victim = cacheBlocks - 1;

        cache = new CacheBlock[cacheSize];
        for (int i = 0; i < cacheSize; i++) {
            cache[i] = new CacheBlock(blockSize);
        }
    }

    //-----------------------------read----------------------------------
	/*This method is used to read data from the cache (or disk if not in the
	cache) into the byte buffer passed in from the user. They also pass in a
	blockId of the block they want. If that block is not in the cache, the
	method will either read a block in from the disk to an empty cache
	location or find a victim, write to the disk, then read from the disk to
	the cache. After, it will read the cache to the buffer.
	 */
    public synchronized boolean read(int blockId, byte buffer[]) {
        if ((blockId >= 0)) {    //valid block id
            int location = inCache(blockId);
            if (location != -1) {    //in cache? Yes
                readFromCacheBlock(location, blockId, buffer);
                return true;
            }
            location = inCache(-1); //find empty, if there is
            if (location != -1) { //an empty spot in cache
                SysLib.rawread(blockId, cache[location].data);
                readFromCacheBlock(location, blockId, buffer);
                return true;
            } else {    //no space in cache, find victim
                writeToDisk(findVictim());
                SysLib.rawread(blockId, cache[victim].data);
                readFromCacheBlock(victim, blockId, buffer);
                return true;
            }
        } else {    //not a valid location
            return false;
        }
    }

    //-----------------------------write----------------------------------
	/*This method will write from a provide byte buffer into a cache location
	. It does this by checking if the blockId they want is already in the
	cache. If it is, it will write to that block and make it dirty. Otherwise
	 it will write to an empty cache location if available. If no cache
	 locations are avaiable, it will find a victim and write that to disk.
	 Then it will write to that cacheblock making it dirty.
	 */
    public synchronized boolean write(int blockId, byte buffer[]) {
        if ((blockId >= 0)) {//valid id
            int location = inCache(blockId);
            if (location != -1) {
                writeToCacheBlock(location, blockId, buffer);
                return true;
            }

            location = inCache(-1);
            if (location != -1) {    //an empty cache blcoks
                writeToCacheBlock(location, blockId, buffer);
                return true;
            } else {    //no empty cache blocks
                writeToDisk(findVictim());
                writeToCacheBlock(victim, blockId, buffer);
                return true;

            }
        } else {    //not valid id
            return false;
        }
    }

    //-----------------------------sync----------------------------------
	/*This method is responsible for writing all dirty cache blocks to the
	disk and making them not dirty. It loops through all blocks and attempts
	to write them to disk. The helper method checks if they are dirty. Then
	we sync the disk
	 */
    public synchronized void sync() {
        for (int i = 0; i < cacheSize; i++) {
            writeToDisk(i);
        }
        SysLib.sync();
    }

    public synchronized void flush() {
        for (int i = 0; i < cacheSize; i++) {
            cache[i].blockFrame = -1;
            cache[i].referenceBit = false;
	    cache[i].dirtyBit = false;
        }
        SysLib.sync();
    }

    //-----------------------------updateCacheBlcok--------------------------
	/*This method is really simple, it will take in a location in the cache,
	and change that cacheBlock's frame and reference bit to a new frame and
	true.
	 */
    private void updateCacheBlock(int location, int frame, boolean boolVal) {
        cache[location].blockFrame = frame;
        cache[location].referenceBit = boolVal;
    }

    //-----------------------------readFromCacheBlock--------------------------
	/*This method is reponsible fro copying from the cache to the buffer and
	will update the cacheblock to say that it was referenced. It will also
	reset the frame id.
	 */
    private void readFromCacheBlock(int location, int blockId, byte[] buffer) {
        System.arraycopy(cache[location].data, 0, buffer, 0, cacheBlockSize);
        updateCacheBlock(location, blockId, true);
    }

    private void writeToCacheBlock(int location, int blockId, byte[] buffer) {
        System.arraycopy(buffer, 0, cache[location].data, 0, cacheBlockSize);
        cache[location].dirtyBit = true;
        updateCacheBlock(location, blockId, true);
    }

    //------------------------writeToDisk-----------------------------
	/*This method is reponsible for writing cache blocks to the disk.
	However, it will only write cacheblocks that are dirty (modified) and are
	 not empty frames. After it has been written to the disk, it is no longer
	  dirty because the disk and cache are the same.
	 */
    public void writeToDisk(int location) {
        if (cache[location].dirtyBit && cache[location].blockFrame != -1) {
            SysLib.rawwrite(cache[location].blockFrame, cache[location]
                    .data);
            cache[location].dirtyBit = false;
        }
    }

    //------------------------inCache-----------------------------
	/*This method is reponsible for looping through the entire cache to see
	if a specific block id is in the cache. If it is, return the location in
	the cache, else return -1.
	 */
    private int inCache(int target) {
        for (int i = 0; i < cacheSize; i++) {
            if (cache[i].blockFrame == target) {
                return i;
            }
        }
        return -1;
    }

    //------------------------findVictim-----------------------------
	/*This method is reponsible for finding a victim cache to write to the
	disk when the cache is full. Ideally it will write a cache that has not
	been referenced and is not dirty. However if the program loops once, it
	will choose the first not referenced block. If it is dirty, it will be
	written to the disk by another method. Once it finds the victim, it
	returns the cache block loaction in the cache
	 */
    public int findVictim() {
        int startingVictim = victim;
        int loopCount = 0;
        while (true) {        //infinite loop
            victim = ((++victim) % cacheSize);
            if (startingVictim == victim) {    //increament Loop Count if the
                // current victim is the same as the starting victim
                loopCount++;
            }
            if (cache[victim].referenceBit == false && cache[victim].dirtyBit
                    == false) {    //dirty = 0, reference = 0
                return victim;
            } else if ((cache[victim].referenceBit == true)) { // dirty = ?
                // reference = 1
                cache[victim].referenceBit = false;
            } else if (cache[victim].referenceBit == false && cache[victim]
                    .dirtyBit == true && loopCount == 1) { //loopCount = 1,
                // dirty = ? and reference = 0
                return victim;
            }

        }
    }

}
