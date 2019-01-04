package jet.opengl.demos.gpupro.rvi;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.ReadableVector4f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.RecycledPool;

final class DX11_RENDERER implements Disposeable, ICONST{

    // list of all render-target configs
    private final List<RENDER_TARGET_CONFIG> renderTargetConfigs = new ArrayList<>();

    // list of all render-targets
    private final List<DX11_RENDER_TARGET> renderTargets = new ArrayList<>();

    // list of all vertex-buffers
//    private List<DX11_VERTEX_BUFFER*> vertexBuffers;

    // list of all index-buffers
//    LIST<DX11_INDEX_BUFFER*> indexBuffers;

    // list of all uniform-buffers
//    LIST<DX11_UNIFORM_BUFFER*> uniformBuffers;

    // list of all structured buffers
    private List<DX11_STRUCTURED_BUFFER> structuredBuffers = new ArrayList<>();

    // list of all cameras
    private List<CAMERA> cameras = new ArrayList<>();

    // list of all dynamic lights
    private List<ILIGHT> lights = new ArrayList<>();

    // list of all dynamically created meshes
//    LIST<MESH*> meshes;

    // list of all post-processors
    private List<IPOST_PROCESSOR> postProcessors = new ArrayList<>();

    // list of all per frame passed surfaces
    private List<SURFACE> surfaces = new ArrayList<>();

    // render-states, frequently used by post-processors
    Runnable noneCullRS;
    Runnable noDepthTestDSS;
    Runnable defaultBS;

    // helper variables
    SURFACE lastSurface;
    boolean frameCleared;

    private GLFuncProvider gl;

    // DirectX 11 objects
//    ID3D11Device *device;
//    ID3D11DeviceContext *deviceContext;
//    IDXGISwapChain *swapChain;

    private static DX11_RENDERER instance;

    static DX11_RENDERER getInstance(){
        if(instance == null)
            instance = new DX11_RENDERER();

        return instance;
    }

    boolean Create() {
        gl = GLFuncProviderFactory.getGLFuncProvider();

        if(!CreateDefaultObjects())
            return false;

        // pre-allocate some surfaces, to prevent initial stutters
//        surfaces.Resize(256);

        return true;
    }

    boolean CreateDefaultObjects()
    {
        // create frequently used samplers

        // LINEAR_SAMPLER
        /*SAMPLER_DESC samplerDesc;
        if(!CreateSampler(samplerDesc))
            return false;

        // TRILINEAR_SAMPLER
        samplerDesc.filter = MIN_MAG_MIP_LINEAR_FILTER;
        samplerDesc.adressU = REPEAT_TEX_ADDRESS;
        samplerDesc.adressV = REPEAT_TEX_ADDRESS;
        samplerDesc.adressW = REPEAT_TEX_ADDRESS;
        if(!CreateSampler(samplerDesc))
            return false;

        // SHADOW_MAP_SAMPLER
        samplerDesc.filter = COMP_MIN_MAG_LINEAR_FILTER;
        samplerDesc.adressU = CLAMP_TEX_ADRESS;
        samplerDesc.adressV = CLAMP_TEX_ADRESS;
        samplerDesc.adressW = CLAMP_TEX_ADRESS;
        samplerDesc.compareFunc = LEQUAL_COMP_FUNC;
        if(!CreateSampler(samplerDesc))
            return false;


        // create frequently used render-targets

        // BACK_BUFFER_RT
        if(!CreateBackBufferRT())
            return false;*/

        final int TEX_FORMAT_RGBA16F = GLenum.GL_RGBA16F;
        final int TEX_FORMAT_DEPTH24 = GLenum.GL_DEPTH_COMPONENT24;
        // GBUFFER_RT
        // 1. frameBufferTextures[0]:
        //    accumulation buffer
        // 2. frameBufferTextures[1]:
        //    RGB-channel: albedo, Alpha-channel: specular intensity
        // 3. frameBufferTextures[2]:
        //    RGB-channel: normal, Alpha-channel: depth
        if(CreateRenderTarget(SCREEN_WIDTH,SCREEN_HEIGHT,1,TEX_FORMAT_RGBA16F,true,3, 0) == null)
            return false;

        // SHADOW_MAP_RT
        if(CreateRenderTarget(1024,1024,1,TEX_FORMAT_DEPTH24,true,0,SamplerUtils.getDepthComparisonSampler()) == null)
            return false;

        // create render-states, frequently used by post-processors
        /*RASTERIZER_DESC rasterDesc;
        noneCullRS = CreateRasterizerState(rasterDesc);
        if(!noneCullRS)
            return false;*/
        noneCullRS = ()->
        {
            gl.glDisable(GLenum.GL_CULL_FACE);
            gl.glColorMask(true, true, true, true);
        };

        /*DEPTH_STENCIL_DESC depthStencilDesc;
        depthStencilDesc.depthTest = false;
        depthStencilDesc.depthMask = false;
        noDepthTestDSS = CreateDepthStencilState(depthStencilDesc);
        if(!noDepthTestDSS)
            return false;*/
        noDepthTestDSS = ()->
        {
            gl.glDisable(GLenum.GL_DEPTH_TEST);
            gl.glDisable(GLenum.GL_STENCIL_TEST);
        };

        /*BLEND_DESC blendDesc;
        defaultBS = CreateBlendState(blendDesc);
        if(!defaultBS)
            return false;*/
        defaultBS = ()->
        {
            gl.glDisable(GLenum.GL_BLEND);
        };

        // create frequently used cameras

        // MAIN_CAMERA
        if(CreateCamera(80.0f,0.2f,5000.0f) == null)
            return false;


        // create frequently used meshes

        // SCREEN_QUAD_MESH
        if(!CreateScreenQuadMesh())
            return false;

        // UNIT_SPHERE_MESH
        if(!CreateUnitSphere())
            return false;


        return true;
    }

    boolean CreateScreenQuadMesh()
    {
        // Create mesh with only 2 vertices, the geometry shader will generate the actual full-screen quad.
        /*VERTEX_ELEMENT_DESC vertexLayout[2] = { POSITION_ELEMENT,R32G32B32_FLOAT_EF,0,
            TEXCOORDS_ELEMENT,R32G32_FLOAT_EF,3};
        MESH *screenQuadMesh = CreateMesh(LINES_PRIMITIVE,vertexLayout,2,false,2,0);
        if(!screenQuadMesh)
            return false;

        QUAD_VERTEX screenQuadVertices[2];
        screenQuadVertices[0].position.Set(-1.0f,-1.0f,0.0f);
        screenQuadVertices[0].texCoords.Set(0.0f,1.0f);
        screenQuadVertices[1].position.Set(1.0f,1.0f,0.0f);
        screenQuadVertices[1].texCoords.Set(1.0f,0.0f);
        screenQuadMesh->vertexBuffer->AddVertices(2,(float*)screenQuadVertices);
        screenQuadMesh->vertexBuffer->Update();*/

        return true;
    }

    boolean CreateUnitSphere()
    {
        // Create low tessellated unit sphere, used for example for rendering deferred point-lights.
        /*int tesselation = 18;
        int numSphereVertices = 2+(((tesselation/2)-2)*(tesselation/2)*4);
        int numSphereIndices = ((tesselation)*6)+(((tesselation/2)-2)*(tesselation/2)*12);
        VERTEX_ELEMENT_DESC vertexLayout[1] = { POSITION_ELEMENT,R32G32B32_FLOAT_EF,0 };
        MESH *sphereMesh = CreateMesh(TRIANGLES_PRIMITIVE,vertexLayout,1,false,numSphereVertices,numSphereIndices);
        if(!sphereMesh)
            return false;

        VECTOR3D *sphereVertices = new VECTOR3D[numSphereVertices];
        if(!sphereVertices)
            return false;
        int *sphereIndices = new int[numSphereIndices];
        if(!sphereIndices)
        {
            SAFE_DELETE_ARRAY(sphereVertices);
            return false;
        }

        // create vertices
        int vertexIndex = 0;
        VECTOR3D vertex;
        VECTOR4D theta;
        sphereVertices[vertexIndex++].Set(0.0f,-1.0f,0.0f);
        for(int i=1;i<tesselation/2-1;i++)
        {
            theta.x = ((i*TWOPI)/tesselation)-PIDIV2;
            float sinThetaX = sin(theta.x);
            float cosThetaX = cos(theta.x);
            theta.y = (((i+1)*TWOPI)/tesselation)-PIDIV2;
            float sinThetaY = sin(theta.y);
            float cosThetaY = cos(theta.y);
            for(int j=0;j<tesselation;j+=2)
            {
                theta.z = (j*TWOPI)/tesselation;
                float sinThetaZ = sin(theta.z);
                float cosThetaZ = cos(theta.z);
                theta.w = ((j+1)*TWOPI)/tesselation;
                float sinThetaW = sin(theta.w);
                float cosThetaW = cos(theta.w);
                vertex.x = cosThetaX*cosThetaZ;
                vertex.y = sinThetaX;
                vertex.z = cosThetaX*sinThetaZ;
                sphereVertices[vertexIndex++] = vertex;
                vertex.x = cosThetaY*cosThetaZ;
                vertex.y = sinThetaY;
                vertex.z = cosThetaY*sinThetaZ;
                sphereVertices[vertexIndex++] = vertex;
                vertex.x = cosThetaY*cosThetaW;
                vertex.y = sinThetaY;
                vertex.z = cosThetaY*sinThetaW;
                sphereVertices[vertexIndex++] = vertex;
                vertex.x = cosThetaX*cosThetaW;
                vertex.y = sinThetaX;
                vertex.z = cosThetaX*sinThetaW;
                sphereVertices[vertexIndex++] = vertex;
            }
        }
        sphereVertices[vertexIndex++].Set(0.0f,1.0f,0.0f);

        // create lower cap indices
        int index = 0;
        vertexIndex = 1;
        for(int i=0;i<tesselation;i++)
        {
            sphereIndices[index++] = 0;
            sphereIndices[index++] = vertexIndex;
            if((i % 2)==0)
                vertexIndex += 3;
            else
                vertexIndex += 1;
            sphereIndices[index++] = (i<(tesselation-1)) ? vertexIndex : 1;
        }

        // create middle sphere indices
        vertexIndex = 1;
        for(int i=1;i<tesselation/2-1;i++)
        {
            int startIndex = vertexIndex;
            for(int j=0;j<tesselation/2;j++)
            {
                sphereIndices[index++] = vertexIndex++;
                sphereIndices[index++] = vertexIndex++;
                sphereIndices[index++] = vertexIndex;
                sphereIndices[index++] = vertexIndex++;
                sphereIndices[index++] = vertexIndex++;
                sphereIndices[index++] = vertexIndex-4;
                int nextIndex = (j==(tesselation/2-1)) ? startIndex : vertexIndex;
                sphereIndices[index++] = vertexIndex-1;
                sphereIndices[index++] = vertexIndex-2;
                sphereIndices[index++] = nextIndex+1;
                sphereIndices[index++] = nextIndex+1;
                sphereIndices[index++] = nextIndex;
                sphereIndices[index++] = vertexIndex-1;
            }
        }

        // create upper cap indices
        int lastIndex = vertexIndex;
        vertexIndex -= 2;
        for(int i=0;i<tesselation;i++)
        {
            sphereIndices[index++] = lastIndex;
            sphereIndices[index++] = vertexIndex;
            if((i % 2)==0)
                vertexIndex -= 1;
            else
                vertexIndex -= 3;
            sphereIndices[index++] = (i<(tesselation-1)) ? vertexIndex : (lastIndex-2);
        }

        sphereMesh->vertexBuffer->AddVertices(numSphereVertices,(float*)sphereVertices);
        sphereMesh->vertexBuffer->Update();
        sphereMesh->indexBuffer->AddIndices(numSphereIndices,sphereIndices);
        sphereMesh->indexBuffer->Update();

        SAFE_DELETE_ARRAY(sphereVertices);
        SAFE_DELETE_ARRAY(sphereIndices);*/

        return true;
    }

    DX11_RENDER_TARGET CreateBackBufferRT()
    {
        DX11_RENDER_TARGET backBuffer = new DX11_RENDER_TARGET();
        backBuffer.CreateBackBuffer();
        renderTargets.add(backBuffer);
        return backBuffer;
    }

    <T extends IPOST_PROCESSOR> T CreatePostProcessor(T postProcessor)
    {
        if(!postProcessor.Create())
        {
            return null;
        }
        postProcessors.add(postProcessor);
        return postProcessor;
    }

    /*DX11_UNIFORM_BUFFER* DX11_RENDERER::CreateUniformBuffer(uniformBufferBP bindingPoint,const UNIFORM_LIST &uniformList)
    {
        DX11_UNIFORM_BUFFER *uniformBuffer = new DX11_UNIFORM_BUFFER;
        if(!uniformBuffer)
            return NULL;
        if(!uniformBuffer->Create(bindingPoint,uniformList))
        {
            SAFE_DELETE(uniformBuffer);
            return NULL;
        }
        uniformBuffers.AddElement(&uniformBuffer);
        return uniformBuffer;
    }

    DX11_STRUCTURED_BUFFER* DX11_RENDERER::CreateStructuredBuffer(int bindingPoint,int elementCount,int elementSize)
    {
        DX11_STRUCTURED_BUFFER *structuredBuffer = new DX11_STRUCTURED_BUFFER;
        if(!structuredBuffer)
            return NULL;
        if(!structuredBuffer->Create(bindingPoint,elementCount,elementSize))
        {
            SAFE_DELETE(structuredBuffer);
            return NULL;
        }
        structuredBuffers.AddElement(&structuredBuffer);
        return structuredBuffer;
    }*/

    CAMERA CreateCamera(float fovy,float nearClipDistance,float farClipDistance)
    {
        CAMERA camera = new CAMERA();
        if(!camera.Init(fovy,nearClipDistance,farClipDistance))
        {
//            SAFE_DELETE(camera);
//            return NULL;
        }
        cameras.add(camera);
        return camera;
    }

    DX11_RENDER_TARGET GetRenderTarget(int ID)
    {
        if((ID<0)||(ID>=renderTargets.size()))
            return null;
        return renderTargets.get(ID);
    }

    DX11_RENDER_TARGET CreateRenderTarget(int width,int height,int depth,int format,boolean depthStencil,int numColorBuffers,
                                                          int sampler)
    {
        DX11_RENDER_TARGET renderTarget = new DX11_RENDER_TARGET();
        if(!renderTarget.Create(width,height,depth,format,depthStencil,numColorBuffers,sampler))
        {
//            SAFE_DELETE(renderTarget);
//            return NULL;
        }
        renderTargets.add(renderTarget);
        return renderTarget;
    }

    DX11_STRUCTURED_BUFFER CreateStructuredBuffer(int bindingPoint,int elementCount,int elementSize)
    {
        DX11_STRUCTURED_BUFFER structuredBuffer = new DX11_STRUCTURED_BUFFER();
//        if(!structuredBuffer)
//            return NULL;
        if(!structuredBuffer.Create(bindingPoint,elementCount,elementSize))
        {
//            SAFE_DELETE(structuredBuffer);
//            return NULL;
            throw new IllegalStateException();
        }
        structuredBuffers.add(structuredBuffer);
        return structuredBuffer;
    }

    RENDER_TARGET_CONFIG CreateRenderTargetConfig(RT_CONFIG_DESC desc)
    {
        for(int i=0;i<renderTargetConfigs.size();i++)
        {
            if(renderTargetConfigs.get(i).GetDesc().equals(desc))
            return renderTargetConfigs.get(i);
        }
        RENDER_TARGET_CONFIG renderTargetConfig = new RENDER_TARGET_CONFIG();
//        if(!renderTargetConfig)
//            return NULL;
        if(!renderTargetConfig.Create(desc))
        {
//            SAFE_DELETE(renderTargetConfig);
//            return NULL;
        }
        renderTargetConfigs.add(renderTargetConfig);
        return renderTargetConfig;
    }

    ILIGHT GetLight(int index)
    {
        if((index<0)||(index>=lights.size()))
            return null;
        return lights.get(index);
    }

    int GetNumLights()
    {
        return lights.size();
    }

    void SetupPostProcessSurface(SURFACE surface)
    {
//        MESH* screenQuadMesh = GetMesh(SCREEN_QUAD_MESH_ID);
        surface.vertexBuffer = null; //screenQuadMesh->vertexBuffer;
        surface.primitiveType = GLenum.GL_TRIANGLES; // screenQuadMesh->primitiveType;
        surface.firstIndex = 0;
        surface.numElements = 3; //screenQuadMesh->vertexBuffer->GetVertexCount();
        surface.rasterizerState = noneCullRS;
        surface.depthStencilState = noDepthTestDSS;
        surface.blendState = defaultBS;
        surface.renderMode = RenderMode.NON_INDEXED_RM;
    }

    void AddSurface(SURFACE surface)
    {
//        int index = surfaces.add(1,surface);
//        surfaces[index].ID = index;
        int index = surfaces.size();
        surfaces.add(surface);
        surface.ID = index;
    }

    IPOST_PROCESSOR GetPostProcessor(String name)
    {
        if(name == null)
            return null;
        for(int i=0;i<postProcessors.size();i++)
        {
//            if(strcmp(name,postProcessors[i]->GetName())==0)
//            return postProcessors[i];

            if(postProcessors.get(i).GetName().endsWith(name)){
                return postProcessors.get(i);
            }
        }
        return null;
    }

    CAMERA GetCamera(int ID) {
        if ((ID < 0) || (ID >= cameras.size()))
            return null;
        return cameras.get(ID);
    }

    POINT_LIGHT CreatePointLight(ReadableVector3f position, float radius, ReadableVector4f color, float multiplier)
    {
        POINT_LIGHT pointLight = new POINT_LIGHT();
        if(!pointLight.Create(position,radius,color,multiplier))
        {
//            SAFE_DELETE(pointLight);
            return null;
        }
        lights.add(pointLight);
        return pointLight;
    }

    DIRECTIONAL_LIGHT CreateDirectionalLight(ReadableVector3f direction, ReadableVector4f color,float multiplier)
    {
        DIRECTIONAL_LIGHT directionalLight = new DIRECTIONAL_LIGHT();
        if(!directionalLight.Create(direction,color,multiplier))
        {
//            SAFE_DELETE(directionalLight);
            return null;
        }
        lights.add(directionalLight);
        return directionalLight;
    }

    void ClearFrame() {
        surfaces.clear();
        for(int i=0;i<renderTargets.size();i++)
            renderTargets.get(i).Reset();
        frameCleared = true;
    }

    void UpdateLights() {
        for(int i=0;i<lights.size();i++)
        {
            ILIGHT light = lights.get(i);
            if(light.IsActive())
                light.Update();
        }
    }

    void ExecutePostProcessors() {
        for(int i=0;i<postProcessors.size();i++)
        {
            if(postProcessors.get(i).IsActive())
                postProcessors.get(i).AddSurfaces();
        }
    }

    void SetRenderStates(SURFACE surface)
    {
        if((surface.rasterizerState!=lastSurface.rasterizerState)||(frameCleared))
        {
            if(surface.rasterizerState != null)
                surface.rasterizerState.run();
            lastSurface.rasterizerState = surface.rasterizerState;
        }

        if((surface.depthStencilState!=lastSurface.depthStencilState)||(frameCleared))
        {
            if(surface.depthStencilState != null)
                surface.depthStencilState.run();
            lastSurface.depthStencilState = surface.depthStencilState;
        }

        if((surface.blendState!=lastSurface.blendState)||(frameCleared))
        {
            if(surface.blendState != null)
                surface.blendState.run();
            lastSurface.blendState = surface.blendState;
        }

        if((surface.renderTarget!=lastSurface.renderTarget)||(surface.renderTargetConfig!=lastSurface.renderTargetConfig)||
                (frameCleared))
        {
            surface.renderTarget.Bind(surface.renderTargetConfig);
            lastSurface.renderTarget = surface.renderTarget;
            lastSurface.renderTargetConfig = surface.renderTargetConfig;
        }

        /*if((surface.vertexBuffer!=lastSurface.vertexBuffer)||(frameCleared))   todo: handle the vertex buffer and indices buffer
        {
            if(surface.vertexBuffer)
                surface.vertexBuffer->Bind();
            lastSurface.vertexBuffer = surface.vertexBuffer;
        }

        if((surface.indexBuffer!=lastSurface.indexBuffer)||(frameCleared))
        {
            if(surface.indexBuffer)
                surface.indexBuffer->Bind();
            lastSurface.indexBuffer = surface.indexBuffer;
        }*/

        if((surface.shader!=lastSurface.shader)||(frameCleared))
        {
            surface.shader.enable();
            lastSurface.shader = surface.shader;
        }

        if((surface.light!=lastSurface.light)||(frameCleared))
        {
            if(surface.light != null)
            {
                if(surface.renderOrder==RenderOrder.SHADOW_RO)
                    surface.renderTarget.Clear(GLenum.GL_DEPTH_BUFFER_BIT);
            }
            lastSurface.light = surface.light;
        }
    }

    private void SetUniformBuffer(BufferGL uniformBuffer, int index)
    {
        if(uniformBuffer == null)
            return;

        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, index, uniformBuffer.getBuffer());
        /*for(int i=0;i<NUM_SHADER_TYPES;i++)
        {
            if(uniformBufferMasks[uniformBuffer->GetBindingPoint()] & (1<<i))
                uniformBuffer->Bind((shaderTypes)i);
        }*/
    }

    private void SetStructuredBuffer(BufferGL uniformBuffer, int index)
    {
        if(uniformBuffer == null)
            return;

        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, index, uniformBuffer.getBuffer());
        /*for(int i=0;i<NUM_SHADER_TYPES;i++)
        {
            if(uniformBufferMasks[uniformBuffer->GetBindingPoint()] & (1<<i))
                uniformBuffer->Bind((shaderTypes)i);
        }*/
    }

    private void SetTexture(int unit, TextureGL tex){
        gl.glBindTextureUnit(unit, tex.getTexture());
    }

    void SetShaderParams(SURFACE surface)
    {
        // TODO notice the buffer binding index
        // set camera uniform-buffer
        if(surface.camera != null)
            /*surface.shader.*/SetUniformBuffer(surface.camera.GetUniformBuffer(), 0);

        // set light uniform-buffer
        if(surface.light != null)
            /*surface.shader.*/SetUniformBuffer(surface.light.GetUniformBuffer(), 0);

        // set custom uniform-buffer
        if(surface.customUB != null)
            /*surface.shader.*/SetUniformBuffer(surface.customUB, 0);

        // set custom structured buffers
        for(int i=0;i<NUM_CUSTOM_STRUCTURED_BUFFERS;i++)
        {
            if(surface.customSBs[i] != null)
                /*surface.shader.*/SetStructuredBuffer(surface.customSBs[i].GetUnorderdAccessView(), 0);
        }

        // set color texture
        if(surface.colorTexture != null)
            /*surface.shader.*/SetTexture(COLOR_TEX_BP,surface.colorTexture.GetUnorderdAccessView());

        // set normal texture
        if(surface.normalTexture != null)
            /*surface.shader.*/SetTexture(NORMAL_TEX_BP,surface.normalTexture.GetUnorderdAccessView());

        // set specular texture
        if(surface.specularTexture != null)
            /*surface.shader.*/SetTexture(SPECULAR_TEX_BP,surface.specularTexture.GetUnorderdAccessView());

        // set custom textures
        for(int i=0;i<NUM_CUSTOM_TEXURES;i++)
        {
            int bindingPoint = (CUSTOM0_TEX_BP+i);
            if(surface.customTextures[i] != null)
                /*surface.shader.*/SetTexture(bindingPoint,surface.customTextures[i].GetUnorderdAccessView());
        }
    }

    private final Comparator<SURFACE> CompareSurfaces = new Comparator<SURFACE>() {
        @Override
        public int compare(SURFACE sA, SURFACE sB) {
            // interleave shadow-map generation + direct illumination + illumination of voxel-grid
            if(((sA.renderOrder.ordinal()>=RenderOrder.SHADOW_RO.ordinal())&&(sA.renderOrder.ordinal()<=RenderOrder.GRID_ILLUM_RO.ordinal()))&&
                    ((sB.renderOrder.ordinal()>=RenderOrder.SHADOW_RO.ordinal())&&(sB.renderOrder.ordinal()<=RenderOrder.GRID_ILLUM_RO.ordinal())))
            {
                if(sA.light.GetIndex()<sB.light.GetIndex())
                    return -1;
                else if(sA.light.GetIndex()>sB.light.GetIndex())
                    return 1;
            }

            if(sA.renderOrder.ordinal()<sB.renderOrder.ordinal())
                return -1;
            else if(sA.renderOrder.ordinal()>sB.renderOrder.ordinal())
                return 1;
            if(sA.ID<sB.ID)
                return -1;
            else if(sA.ID>sB.ID)
                return 1;
            return 0;
        }
    };

    void DrawSurfaces() {
        ExecutePostProcessors();
        Collections.sort(surfaces, CompareSurfaces);
        for(int i=0;i<surfaces.size();i++)
        {
            SURFACE surface = surfaces.get(i);
            if(i>0)
                frameCleared = false;
            SetRenderStates(surface);
            SetShaderParams(surface);
            switch(surface.renderMode)
            {
                case INDEXED_RM:
                    DrawIndexedElements(surface.primitiveType,surface.numElements,surface.firstIndex,surface.numInstances);
                    break;

                case NON_INDEXED_RM:
                    DrawElements(surface.primitiveType,surface.numElements,surface.firstIndex,surface.numInstances);
                    break;

                case COMPUTE_RM:
                    Dispatch(surface.numThreadGroupsX,surface.numThreadGroupsY,surface.numThreadGroupsZ);
                    break;
            }
        }
//        swapChain->Present(VSYNC_ENABLED,0);
    }

    void DrawIndexedElements(int primitiveType,int numElements,int firstIndex,int numInstances)
    {
//        deviceContext->IASetPrimitiveTopology((D3D11_PRIMITIVE_TOPOLOGY)primitiveType);
        if(numInstances<2)
            gl.glDrawElements(primitiveType, numElements, GLenum.GL_UNSIGNED_INT, 0);
//            deviceContext->DrawIndexed(numElements,firstIndex,0);
        else
//            deviceContext->DrawIndexedInstanced(numElements,numInstances,firstIndex,0,0);
            gl.glDrawElementsInstanced(primitiveType, numElements, GLenum.GL_UNSIGNED_INT, 0,numInstances);

        UnbindShaderResources();
    }

    void DrawElements(int primitiveType,int numElements,int firstIndex,int numInstances)
    {
//        deviceContext->IASetPrimitiveTopology((D3D11_PRIMITIVE_TOPOLOGY)primitiveType);
        if(numInstances<2)
//            deviceContext->Draw(numElements,firstIndex);
            gl.glDrawArrays(primitiveType, 0, numElements);
        else
//            deviceContext->DrawInstanced(numElements,numInstances,firstIndex,0);
            gl.glDrawArraysInstanced(primitiveType, 0,numElements, numInstances);
        UnbindShaderResources();
    }

    void Dispatch(int numThreadGroupsX,int numThreadGroupsY,int numThreadGroupsZ)
    {
//        deviceContext->Dispatch(numThreadGroupsX,numThreadGroupsY,numThreadGroupsZ);
        gl.glDispatchCompute(numThreadGroupsX,numThreadGroupsY,numThreadGroupsZ);
        UnbindShaderResources();

//        ID3D11UnorderedAccessView *unorderedAccessViews[MAX_NUM_COLOR_BUFFERS] =
//                { NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL };
//        deviceContext->CSSetUnorderedAccessViews(0,MAX_NUM_COLOR_BUFFERS,unorderedAccessViews, NULL);
    }

    void UnbindShaderResources(){ }

    @Override
    public void dispose() {

    }
}
