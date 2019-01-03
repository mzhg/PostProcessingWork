package jet.opengl.demos.gpupro.rvi;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.shader.GLSLProgram;

final class SURFACE implements ICONST{
    DX11_RENDER_TARGET renderTarget;
    RENDER_TARGET_CONFIG renderTargetConfig;
    RenderOrder renderOrder;
    DX11_VERTEX_BUFFER vertexBuffer;
    BufferGL indexBuffer;
    CAMERA camera;
    int primitiveType;
    int firstIndex;
    int numElements;
    MATERIAL material;
    DX11_TEXTURE colorTexture;
    DX11_TEXTURE normalTexture;
    DX11_TEXTURE specularTexture;
    DX11_TEXTURE[] customTextures = new DX11_TEXTURE[NUM_CUSTOM_TEXURES];
    ILIGHT light;
    Runnable rasterizerState;
    Runnable depthStencilState;
    Runnable blendState;
    BufferGL customUB;
    DX11_STRUCTURED_BUFFER[] customSBs = new DX11_STRUCTURED_BUFFER[NUM_CUSTOM_STRUCTURED_BUFFERS];
    GLSLProgram shader;
    RenderMode renderMode = RenderMode.INDEXED_RM;
    int numInstances = 1;
    int numThreadGroupsX;
    int numThreadGroupsY;
    int numThreadGroupsZ;

    int ID;

    SURFACE(){}

    SURFACE( SURFACE rhs)
    {
        set(rhs);
    }

    void set(SURFACE surface)
    {
        renderTarget = surface.renderTarget;
        renderTargetConfig = surface.renderTargetConfig;
        renderOrder = surface.renderOrder;
        vertexBuffer = surface.vertexBuffer;
        indexBuffer = surface.indexBuffer;
        camera = surface.camera;
        primitiveType = surface.primitiveType;
        firstIndex = surface.firstIndex;
        numElements = surface.numElements;
        material = surface.material;
        colorTexture = surface.colorTexture;
        normalTexture = surface.normalTexture;
        specularTexture = surface.specularTexture;
        for(int i=0;i<NUM_CUSTOM_TEXURES;i++)
            customTextures[i] = surface.customTextures[i];
        light = surface.light;
        rasterizerState = surface.rasterizerState;
        depthStencilState = surface.depthStencilState;
        blendState = surface.blendState;
        customUB = surface.customUB;
        for(int i=0;i<NUM_CUSTOM_STRUCTURED_BUFFERS;i++)
            customSBs[i] = surface.customSBs[i];
        shader = surface.shader;
        renderMode = surface.renderMode;
        numInstances = surface.numInstances;
        numThreadGroupsX = surface.numThreadGroupsX;
        numThreadGroupsY = surface.numThreadGroupsY;
        numThreadGroupsZ = surface.numThreadGroupsZ;
        ID = surface.ID;
    }
}
