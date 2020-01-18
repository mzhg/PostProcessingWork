layout(early_fragment_tests) in;

layout(binding = 1) writeonly buffer OccluderVisible
{
    uint VisibleBuffer[];
};

in flat int occludeeID;
void main()
{
    // restrict, coherent, and
    //volatile
    // TODO When a object cover the full screen, writing into the Visible buffer could serosely harm the performance. downsampling the depth buffer could opmize this issue.
    // or try other ways. e.g testing the lines rather than triangles.
    VisibleBuffer[occludeeID] = 1;
//    atomicCompSwap(VisibleBuffer[occludeeID], 0, 1);
}