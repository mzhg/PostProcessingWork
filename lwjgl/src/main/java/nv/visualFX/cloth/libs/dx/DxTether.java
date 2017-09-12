package nv.visualFX.cloth.libs.dx;

/**
 * Created by mazhen'gui on 2017/9/12.
 */

final class DxTether {
    static final int SIZE = 4;
    short mAnchor;
    short mLength;

    public DxTether(short anchor, short length) {
        this.mAnchor = anchor;
        this.mLength = length;
    }
}
