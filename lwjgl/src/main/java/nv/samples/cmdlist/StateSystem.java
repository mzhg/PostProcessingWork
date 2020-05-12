package nv.samples.cmdlist;

public final class StateSystem {

    public final int[] bindings = new int[16];
    public int alpha;

    public int stencil_front_func;
    public int stencil_front_mask;
    public int stencil_back_func;
    public int stencil_back_mask;
}
