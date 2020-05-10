package nv.samples.cmdlist;

final class StateSystem {

    final int[] bindings = new int[16];
    int alpha;

    int stencil_front_func;
    int stencil_front_mask;
    int stencil_back_func;
    int stencil_back_mask;
}
