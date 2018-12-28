package jet.opengl.demos.gpupro.rvi;

final class SURFACE {
    DX11_RENDER_TARGET renderTarget;
    RENDER_TARGET_CONFIG renderTargetConfig;
    renderOrders renderOrder;
    DX11_VERTEX_BUFFER vertexBuffer;
    DX11_INDEX_BUFFER indexBuffer;
    CAMERA camera;
    int primitiveType;
    int firstIndex;
    int numElements;
    MATERIAL material;
    DX11_TEXTURE colorTexture;
    DX11_TEXTURE normalTexture;
    DX11_TEXTURE specularTexture;
    DX11_TEXTURE[] customTextures = new DX11_TEXTURE[NUM_CUSTOM_TEXURES];
    ILIGHT *light;
    DX11_RASTERIZER_STATE *rasterizerState;
    DX11_DEPTH_STENCIL_STATE *depthStencilState;
    DX11_BLEND_STATE *blendState;
    DX11_UNIFORM_BUFFER *customUB;
    DX11_STRUCTURED_BUFFER *customSBs[NUM_CUSTOM_STRUCTURED_BUFFERS];
    DX11_SHADER *shader;
    renderModes renderMode;
    int numInstances;
    int numThreadGroupsX;
    int numThreadGroupsY;
    int numThreadGroupsZ;

    private int ID;
}
