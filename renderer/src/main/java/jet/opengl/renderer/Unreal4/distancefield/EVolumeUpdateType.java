package jet.opengl.renderer.Unreal4.distancefield;

public interface EVolumeUpdateType {
    int
    VUT_MeshDistanceFields = 1,
    VUT_Heightfields = 2,
    VUT_All = VUT_MeshDistanceFields | VUT_Heightfields;
}
