package jet.opengl.renderer.Unreal4.api;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.BlendState;
import jet.opengl.postprocessing.common.DepthStencilState;

public class FMeshPassProcessorRenderState {
    private BlendState BlendState;
    private DepthStencilState DepthStencilState;
    private boolean	DepthStencilAccess;

    private BufferGL
    /*TUniformBufferRef<FViewUniformShaderParameters>*/	ViewUniformBuffer;
    private BufferGL
    /*TUniformBufferRef<FInstancedViewUniformShaderParameters>*/ InstancedViewUniformBuffer;
//    FUniformBufferRHIParamRef		PassUniformBuffer;
    private int							StencilRef;
}
