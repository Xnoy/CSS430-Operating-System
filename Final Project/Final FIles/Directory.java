/* Cameron Padua
    Peter Van

    File: Directory.java
    This class is responsible keeping tack of all files that are currently in
     use. This is accomplished by have array of the lengths of the names and
     the names themeslves. There are a variety of functions to help add,
     remove, and find if a file exists in the directory.
 */
public class Directory {
    private static int maxChars = 30; // max characters of each file name

    // Directory entries
    private int fsize[];        // each element stores a different file size.
    private char fnames[][];    // each element stores a different file name.

    public Directory(int maxInumber) { // directory constructor
        fsize = new int[maxInumber];     // maxInumber = max files
        for (int i = 0; i < maxInumber; i++)
            fsize[i] = 0;                 // all file size initialized to 0
        fnames = new char[maxInumber][maxChars];
        String root = "/";                // entry(inode) 0 is "/"
        fsize[0] = root.length();        // fsize[0] is the size of "/".
        root.getChars(0, fsize[0], fnames[0], 0); // fnames[0] includes "/"
    }

    // loads data from disk to directory
    public int bytes2directory(byte data[]) {
        // assumes data[] received directory information from disk
        // initializes the Directory instance with this data[]
        int offset = 0;
        for (int i = 0; i < fsize.length; i++) {
            fsize[i] = SysLib.bytes2int(data, offset);
            offset += 4;
        }
        for (int i = 0; i < fsize.length; i++) {
            String str = new String(data, offset, 60);
            str.getChars(0, fsize[i], fnames[i], 0);
            offset += 60;
        }
        return 0;
    }

    // saves data from directory to disk
    public byte[] directory2bytes() {
        // converts and return Directory information into a plain byte array
        // this byte array will be written back to disk
        // note: only meaningfull directory information should be converted
        // into bytes.
        byte directory[] = new byte[(60 + 4) * fsize.length];
        int offset = 0;
        for (int i = 0; i < fsize.length; i++) {
            SysLib.int2bytes(fsize[i], directory, offset);
            offset += 4;
        }
        for (int i = 0; i < fsize.length; i++) {
            String str = new String(fnames[i], 0, fsize[i]);
            byte[] byteForm = str.getBytes();
            System.arraycopy(byteForm, 0, directory, offset, byteForm.length);
            offset += 60;
        }
        return directory;
    }

    // allocates an inode number to a file with the given filename
    public short ialloc(String filename) {
        // filename is the one of a file to be created.
        // allocates a new inode number for this filename
        for (int i = 0; i < fsize.length; i++) {
            if (fsize[i] == 0) {
                fsize[i] = filename.length();
                filename.getChars(0, fsize[i], fnames[i], 0);
                return (short) i;
            }
        }
        return -1;
    }

    // frees an inode number based on the given parameter
    public boolean ifree(short iNumber) {
        // deallocates this inumber (inode number)
        // the corresponding file will be deleted.
        if (iNumber > 0 && iNumber < fsize.length) {
            fsize[iNumber] = 0;
            return true;
        }
        return false;
    }

    // returns inode number associated the file name
    public short namei(String filename) {
        for (int i = 0; i < fsize.length; i++) {
            if (filename.length() == fsize[i]) {
                String tmp = new String(fnames[i], 0, fsize[i]);
                if (filename.equals(tmp)) return (short) i;
            }
        }
        return -1;
    }

    //get file size
    public int getfsize(int index) {
        return fsize[index];
    }

    public int getNumOfFiles() {
        return fsize.length;
    }
}
