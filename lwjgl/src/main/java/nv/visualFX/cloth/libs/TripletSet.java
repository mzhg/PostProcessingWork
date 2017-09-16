package nv.visualFX.cloth.libs;

/**
 * Created by mazhen'gui on 2017/9/14.
 */

final class TripletSet {
    int mMark = 0xFFFFFFFF; // triplet index
    byte[] mNumReplays = {1,1,1};
    byte[][] mNumConflicts= new byte[3][32];
}
