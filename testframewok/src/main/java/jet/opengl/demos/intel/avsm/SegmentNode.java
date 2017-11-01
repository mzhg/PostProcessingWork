package jet.opengl.demos.intel.avsm;

/**
 * Created by mazhen'gui on 2017/11/1.
 */

final class SegmentNode {
    static final int SIZE = 5 * 4;
    int         next;
    float       depth0;
    float       depth1;
    float       trans;
    float       sortKey;
}
