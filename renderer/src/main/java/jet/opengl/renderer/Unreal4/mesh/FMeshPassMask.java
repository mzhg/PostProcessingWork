package jet.opengl.renderer.Unreal4.mesh;

/** Mesh pass mask - stores one bit per mesh pass. */
public class FMeshPassMask {

    public void Set(EMeshPass Pass) { Data |= (1 << Pass.ordinal()); }
    public boolean Get(EMeshPass Pass)  { return (Data & (1 << Pass.ordinal())) != 0; }

    public void AppendTo(FMeshPassMask Mask) { Mask.Data |= Data; }
    public void Reset() { Data = 0; }
    public boolean IsEmpty() { return Data == 0; }

    private int Data;
}
