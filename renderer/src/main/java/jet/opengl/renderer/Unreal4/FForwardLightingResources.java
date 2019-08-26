package jet.opengl.renderer.Unreal4;

import jet.opengl.postprocessing.buffer.BufferGL;

public class FForwardLightingResources {
    public final FForwardLightData ForwardLightData = new FForwardLightData(UE4Engine.MAX_CASCADE);

    public final TextureBuffer ForwardLocalLightBuffer = new TextureBuffer();

    public final TextureBuffer NumCulledLightsGrid = new TextureBuffer();

    public final TextureBuffer CulledLightDataGrid = new TextureBuffer();

    public BufferGL ForwardLightDataUniformBuffer;
}
