package nv.visualFX.blast.lowlevel;

/**
 * Struct to hold chunk indices and bond index for sorting<p></p>

 Utility struct used by NvBlastCreateAsset in order to arrange bond data in a lookup table, and also to easily identify redundant input.<p></p>
 * Created by mazhen'gui on 2017/9/9.
 */

public class BondSortData {
    public static final int SIZE = 4 * 3;
    public int	m_c0;
    public int	m_c1;
    public int	m_b;

    public BondSortData(int c0, int c1, int b) {
        this.m_c0 = c0;
        this.m_c1 = c1;
        this.m_b = b;
    }
}
