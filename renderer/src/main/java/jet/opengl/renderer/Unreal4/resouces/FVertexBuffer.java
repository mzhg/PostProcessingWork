package jet.opengl.renderer.Unreal4.resouces;

import jet.opengl.postprocessing.buffer.BufferGL;

public class FVertexBuffer extends FRenderResource{

    public BufferGL VertexBufferRHI = new BufferGL();

    // FRenderResource interface.
    @Override
    public void ReleaseRHI()
    {
        VertexBufferRHI.dispose();
    }

    @Override
    public  String GetFriendlyName()  { return ("FVertexBuffer"); }
}
