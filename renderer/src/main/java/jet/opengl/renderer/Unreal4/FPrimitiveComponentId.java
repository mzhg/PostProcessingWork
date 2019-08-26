package jet.opengl.renderer.Unreal4;

/**
 * Class used to identify UPrimitiveComponents on the rendering thread without having to pass the pointer around,
 * Which would make it easy for people to access game thread variables from the rendering thread.
 */
public class FPrimitiveComponentId {
    public int PrimIDValue;

    public boolean IsValid()
    {
        return PrimIDValue > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FPrimitiveComponentId that = (FPrimitiveComponentId) o;
        return PrimIDValue == that.PrimIDValue;
    }

    @Override
    public int hashCode() {
        return PrimIDValue;
    }
}
