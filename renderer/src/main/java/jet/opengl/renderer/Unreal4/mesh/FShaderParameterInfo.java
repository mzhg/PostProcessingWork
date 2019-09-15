package jet.opengl.renderer.Unreal4.mesh;

import jet.opengl.renderer.Unreal4.utils.Unsigned;

public class FShaderParameterInfo {
    @Unsigned
    public short BaseIndex;
    @Unsigned
    public short Size;

    public FShaderParameterInfo() {}

    public FShaderParameterInfo(short InBaseIndex, short InSize)
    {
        BaseIndex = InBaseIndex;
        Size = InSize;
//        checkf(BaseIndex == InBaseIndex && Size == InSize, TEXT("Tweak FShaderParameterInfo type sizes"));
    }
}
