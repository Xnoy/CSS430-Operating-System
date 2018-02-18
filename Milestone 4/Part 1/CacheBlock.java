/*******************************************************************************
 * @class CacheBlock.java
 *
 * @description This class is a very simple object class. It will emulate a
 * location in the cache that has a reference bit, dirty bit, frame #, and
 * the data in the cache itself. It has a default constructor to specify the
 * sizae of the cache block.
 *
 *
 * @author Cameron Padua
 * @date May 21, 2017
 *
 * Limitations:
 *
 * Assumptions:
 *
 ******************************************************************************/
public class CacheBlock {

	byte[] data;
	int blockFrame;
	boolean referenceBit;
	boolean dirtyBit;
	
	public CacheBlock(int size) {
		data = new byte[size];
		blockFrame = -1;
		referenceBit = false;
		dirtyBit = false;
	}
}
