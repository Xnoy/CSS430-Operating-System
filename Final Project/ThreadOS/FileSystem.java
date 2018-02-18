/*  Cameron Padua
    Peter Van

    FileSystem.java
    This method is responsible for any type of file manipulation on the disk.
     It can read, write, format, delete, open, and close files.

 */
public class FileSystem {
    private final int SEEK_SET = 0;
    private SuperBlock superBlock;
    private Directory directory;
    private FileTable fileTable;

    public FileSystem(int diskBlocks) {
        superBlock = new SuperBlock(diskBlocks);
        directory = new Directory(superBlock.totalInodes);
        fileTable = new FileTable(directory);
        //construct Directory from disk
        FileTableEntry directoryFile = open("/", "r");
        if (directoryFile.inode.length > 0) {
            // There is already data in the directory
            // We must read, and copy it to fsDirectory
            byte[] directoryData = new byte[directoryFile.inode.length];
            loadDirectory(directoryFile, directoryData);
            directory.bytes2directory(directoryData);
        }
        synchronized (directoryFile) {
            // decrease the number of users
            directoryFile.count--;

            if (directoryFile.count <= 0) {
                fileTable.ffree(directoryFile);
            }
        }
    }

    /*sync
        This method will directory to the disk and sync the super block
     */
    void sync() {
        TCB tempTCB = Kernel.getThreadTcb();
        byte[] tempData = directory.directory2bytes();
        //open root directory with write access
        FileTableEntry root = open("/", "w");
        //write directory to root
        int FD = tempTCB.getFD(root);
        write(FD, directory.directory2bytes());
        //close root directory
        close(FD);
        //sync superblock
        superBlock.sync();
    }

    //The parameter specifies the max num of files to be created
    //(num of inodes to be allocated. Constructs file System.
    int format(int files) {
        superBlock.format(files);
        directory = new Directory(files);
        fileTable = new FileTable(directory);
        return 0;
    }

    // opens the file in a given mode. The file is created if it doesn't exist in mode w,w+, or a
    //if the file descriptor table for the thread is full then return error.
    FileTableEntry open(String fileName, String mode) {
        FileTableEntry fileEntry = fileTable.falloc(fileName, mode);
        return fileEntry;
    }

    int loadDirectory(FileTableEntry fileEntry, byte buffer[]) {
        int size = buffer.length;
        int bufferOffset = 0;
        int remainingBlockSize = 0;


        //check write or append status
        if ((fileEntry.mode == "w") || (fileEntry.mode == "a")) {
            return -1;
        }

        synchronized (fileEntry) {
            while (fileEntry.seekPtr < fileEntry.inode.length && (size > 0)) {
                int currentBlock = fileEntry.inode.findTargetLocationBlock(fileEntry.seekPtr);
                if (currentBlock == -1) {
                    break;
                }
                byte[] data = new byte[512];
                SysLib.rawread(currentBlock, data);
                int dataOffset = fileEntry.seekPtr % 512;
                int blocksLeft = 512 - remainingBlockSize;
                int fileRemaining = fileEntry.inode.length - fileEntry.seekPtr;
                if (blocksLeft < fileRemaining) {
                    remainingBlockSize = blocksLeft;
                } else {
                    remainingBlockSize = fileRemaining;
                }
                if (remainingBlockSize > size) {
                    remainingBlockSize = size;
                }
                System.arraycopy(data, dataOffset, buffer, bufferOffset, remainingBlockSize);
                bufferOffset += remainingBlockSize;
                fileEntry.seekPtr += remainingBlockSize;
                size -= remainingBlockSize;
            }

            return bufferOffset;
        }
    }

    //returns bytes that have been read
    int read(int fd, byte buffer[]) {
        int size = buffer.length;
        int bufferOffset = 0;
        int remainingBlockSize = 0;

        TCB tempTCB = Kernel.getThreadTcb();
        FileTableEntry fileEntry = tempTCB.getFtEnt(fd);

        //check write or append status
        if ((fileEntry.mode == "w") || (fileEntry.mode == "a")) {
            return -1;
        }

        synchronized (fileEntry) {
            while (fileEntry.seekPtr < fsize(fd) && (size > 0)) {
                int currentBlock = fileEntry.inode.findTargetLocationBlock(fileEntry.seekPtr);
                if (currentBlock == -1) {
                    break;
                }
                byte[] data = new byte[512];
                SysLib.rawread(currentBlock, data);
                int dataOffset = fileEntry.seekPtr % 512;
                int blocksLeft = 512 - remainingBlockSize;
                int fileRemaining = fsize(fd) - fileEntry.seekPtr;
                if (blocksLeft < fileRemaining) {
                    remainingBlockSize = blocksLeft;
                } else {
                    remainingBlockSize = fileRemaining;
                }
                if (remainingBlockSize > size) {
                    remainingBlockSize = size;
                }
                System.arraycopy(data, dataOffset, buffer, bufferOffset, remainingBlockSize);
                bufferOffset += remainingBlockSize;
                fileEntry.seekPtr += remainingBlockSize;
                size -= remainingBlockSize;
            }

            return bufferOffset;
        }
    }

    //This method will write a buffer to a specified file using the file
    // descriptor. It checks that the file is not null or is in read mode.
    // This method will return the amount of bytes written to the disk.
    int write(int fd, byte buffer[]) {
        TCB tempTCB = Kernel.getThreadTcb();
        FileTableEntry fileEntry = tempTCB.getFtEnt(fd);
        if (fileEntry == null || fileEntry.mode == "r") {
            return -1;
        }
        int bufferSize = buffer.length;
        int bytesWritten = 0;
        int blockSize = 512;

        synchronized (fileEntry) {
            while (bufferSize > 0) {
                int location = fileEntry.inode.findTargetLocationBlock(fileEntry.seekPtr);

                if (location == -1) {
                    location = fileEntry.inode.whereToIndex(fileEntry.seekPtr, superBlock);
                }

                //writing contents to the location
                byte tempBlock[] = new byte[blockSize];
                SysLib.rawread(location, tempBlock);

                int offset = fileEntry.seekPtr % blockSize;
                int diff = blockSize - offset;

                if (diff > bufferSize) {
                    System.arraycopy(buffer, bytesWritten, tempBlock, offset, bufferSize);
                    SysLib.rawwrite(location, tempBlock);

                    fileEntry.seekPtr += bufferSize;
                    bytesWritten += bufferSize;
                    bufferSize = 0;
                } else {
                    System.arraycopy(buffer, bytesWritten, tempBlock, offset, diff);
                    SysLib.rawwrite(location, tempBlock);

                    fileEntry.seekPtr += diff;
                    bytesWritten += diff;
                    bufferSize -= diff;
                }
            }

            // update inode length if seekPtr larger
            if (fileEntry.seekPtr > fileEntry.inode.length) {
                fileEntry.inode.length = fileEntry.seekPtr;
            }
            fileEntry.inode.toDisk(fileEntry.iNumber);

            return bytesWritten;
        }
    }

    //Updates the seek pointer to a specified location or adding to the
    // current seekptr or adding to the end of the file.
    int seek(int fd, int offset, int whence) {
        TCB tempTCB = Kernel.getThreadTcb();
        FileTableEntry fileEntry = tempTCB.getFtEnt(fd);
        synchronized (fileEntry) {
            switch (whence) {
                //set to offset
                case 0:
                    fileEntry.seekPtr = offset;
                    break;
                //add to seekptr
                case 1:
                    fileEntry.seekPtr += offset;
                    break;
                //add to end with offset
                case 2:
                    fileEntry.seekPtr = fileEntry.inode.length + offset;
                    break;

                default:
                    return -1;
            }
            if (fileEntry.seekPtr < 0) {
                fileEntry.seekPtr = 0;
            }
            if (fileEntry.seekPtr > fileEntry.inode.length) {
                fileEntry.seekPtr = fileEntry.inode.length;
            }
        }
        return fileEntry.seekPtr;
    }


    // Closes the file corresponding to fd, commits all file transactions on this file,
    // and unregisters fd from the user file descriptor table of the calling thread's TCB. 
    // The return value is 0 in success, otherwise -1.

    int close(int fd) {
        TCB tempTCB = Kernel.getThreadTcb();
        FileTableEntry fileEntry = tempTCB.getFtEnt(fd);
        synchronized (fileEntry) {
            // decrease the number of users
            fileEntry.count--;
            if (fileEntry.count <= 0) {
                fileTable.ffree(fileEntry);
                return 0;
            }
            return -1;
        }
    }

    // Deletes the file specified by fileName. All blocks used by file are freed. If the file is currently open,
    // it is not deleted and the operation returns a -1. If successfully deleted a 0 is returned.
    int delete(String fileName) {
        FileTableEntry target = open(fileName, "w");
        TCB tempTCB = Kernel.getThreadTcb();
        int FD = tempTCB.getFd(target);
        if (directory.ifree(target.iNumber) && close(FD) <= 0) {
            tempTCB.returnFd(FD);
            return 0;
        } else {
            return -1;
        }
    }

    // Returns the size in bytes of the file indicated by fd
    int fsize(int fd) {
        TCB tempTCB = Kernel.getThreadTcb();
        FileTableEntry fileEntry = tempTCB.getFtEnt(fd);
        synchronized (fileEntry) {
            return fileEntry.inode.length;
        }
    }
}
