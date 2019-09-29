package jet.opengl.demos.amdfx.geometry;

import org.lwjgl.util.vector.Matrix4f;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import jet.opengl.demos.intel.cput.D3D11_INPUT_ELEMENT_DESC;
import jet.opengl.demos.intel.cput.ID3D11InputLayout;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.LogUtil;

final class GeometryFX_OpaqueFilterDesc {
    static final int SMALL_BATCH_CHUNK_COUNT = 16;

    private boolean emulateMultiDrawIndirect_;
//    AGSContext* agsContext_;

//    std::vector<std::unique_ptr<GeometryFX_Filter::Handle>> handles_;
    private MeshHandle[] handles_;

    private IMeshManager meshManager_;
    private BufferGL[] drawCallConstantBuffers_;
    private int currentDrawCall_;
    private int maxDrawCallCount_;

    private final List<DrawCommand> drawCommands_ = new ArrayList<>();

//    ID3D11DeviceContext *deviceContext_;
    private FilterContext filterContext_;

    private int pipelineQuery_;
//    ID3D11Device *device_;

    private final FrameConstantBuffer frameConstantBufferBackingStore_ = new FrameConstantBuffer();
    private BufferGL frameConstantBuffer_;

    private GLSLProgram filterComputeShader_;

    private final List<SmallBatchChunk> smallBatchChunks_ = new ArrayList<>();

    private GLSLProgram clearDrawIndirectArgumentsComputeShader_;

    private BufferGL indirectArgumentsBuffer_;
    private BufferGL indirectArgumentsBufferPristine_;
    private BufferGL indirectArgumentsUAV_;

    private ID3D11InputLayout depthOnlyLayout_;
    private GLSLProgram depthOnlyVertexShader_;
    private ID3D11InputLayout depthOnlyLayoutMID_;
    private GLSLProgram depthOnlyVertexShaderMID_;
    private GLFuncProvider gl;

    GeometryFX_OpaqueFilterDesc(GeometryFX_FilterDesc createInfo)
        /*: device_(createInfo.pDevice)
        , maxDrawCallCount_(createInfo.maximumDrawCallCount)
        , emulateMultiDrawIndirect_(false)
        , currentDrawCall_(0)
        , deviceContext_(nullptr)*/
    {
        gl = GLFuncProviderFactory.getGLFuncProvider();
        maxDrawCallCount_ = createInfo.maximumDrawCallCount;
        CreateQueries();
        CreateConstantBuffers();
        CreateShaders();

        meshManager_ = IMeshManager.CreateGlobalMeshManager();
//        agsContext_ = nullptr;

        /*if (agsInit(&agsContext_, nullptr, nullptr) == AGS_SUCCESS)
        {
            unsigned int supportedExtensions = 0;
            agsDriverExtensions_Init(agsContext_, device_, &supportedExtensions);

            if ((supportedExtensions & AGS_EXTENSION_MULTIDRAWINDIRECT) == 0)
            {
                OutputDebugString(TEXT("AGS initialized but multi draw extension not supported"));
                agsDriverExtensions_DeInit(agsContext_);
                agsDeInit(agsContext_);
                agsContext_ = nullptr;
            }
        }*/

        if (createInfo.emulateMultiIndirectDraw)
        {
            LogUtil.i(LogUtil.LogType.DEFAULT, "Multi draw extension supported but ignored");
            emulateMultiDrawIndirect_ = true;
        }
    }

    MeshHandle[] RegisterMeshes(int meshCount, int[] verticesInMesh, int[] indicesInMesh)
    {
        handles_ = new MeshHandle[meshCount];
        for (int i = 0; i < meshCount; ++i)
        {
            handles_[i] = new MeshHandle(i);
        }

        meshManager_.Allocate(/*device_,*/ meshCount, verticesInMesh, indicesInMesh);

        for (int i = 0; i < meshCount; ++i)
        {
            handles_[i].mesh = meshManager_.GetMesh(i);
        }

        CreateIndirectDrawArgumentsBuffer(meshCount, indicesInMesh, verticesInMesh);

        if (maxDrawCallCount_ == -1)
        {
            maxDrawCallCount_ = meshManager_.GetMeshCount();
        }

        CreateDrawCallConstantBuffers();

        for (int i = 0; i < SMALL_BATCH_CHUNK_COUNT; ++i)
        {
            smallBatchChunks_.add(
                    new SmallBatchChunk( emulateMultiDrawIndirect_/*, agsContext_*/));
        }

        /*std::vector<MeshHandle> result;
        result.reserve(handles_.size());
        for (std::vector<std::unique_ptr<Handle>>::iterator it = handles_.begin(),
            end = handles_.end();
        it != end; ++it)
        {
            result.push_back(it->get());
        }
        return result;*/

        return handles_;
    }

    void SetMeshData(MeshHandle handle, float[] vertexData, int[] indexData)
    {
        /*ComPtr<ID3D11DeviceContext> deviceContext;
        device_->GetImmediateContext(&deviceContext);*/

        meshManager_.SetData(/*device_, deviceContext.Get(),*/ handle.index, vertexData, indexData);
    }

    void BeginRender(/*ID3D11DeviceContext *context, const*/ FilterContext filterContext)
    {
//        deviceContext_ = context;
        filterContext_ = filterContext;
        currentDrawCall_ = 0;

        if (filterContext.options.statistics != null) {
            filterContext.options.statistics.reset();
        }

        drawCommands_.clear();

        frameConstantBufferBackingStore_.view.load(filterContext.view);
        frameConstantBufferBackingStore_.projection.load(filterContext.projection);
        frameConstantBufferBackingStore_.height = filterContext.windowHeight;
        frameConstantBufferBackingStore_.width = filterContext.windowWidth;
        frameConstantBufferBackingStore_.cullFlags = filterContext.options.enabledFilters;

        /*D3D11_MAPPED_SUBRESOURCE mapping;
        context->Map(frameConstantBuffer_.Get(), 0, D3D11_MAP_WRITE_DISCARD, 0, &mapping);
        ::memcpy(mapping.pData, &frameConstantBufferBackingStore_,
            sizeof(frameConstantBufferBackingStore_));
        context->Unmap(frameConstantBuffer_.Get(), 0);

        context->PSSetShader(nullptr, NULL, 0);*/

        ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(FrameConstantBuffer.SIZE);
        frameConstantBufferBackingStore_.store(buffer).flip();
        frameConstantBuffer_.update(0, buffer);
    }

    void RenderMeshInstanced(MeshHandle handle, Matrix4f worldMatrice){
        DrawCommand request = new DrawCommand();
        request.mesh = handle.mesh;
        request.dcb.world.load(worldMatrice);
//            request.dcb.worldView = worldMatrices[i] * filterContext_.view;
        Matrix4f.mul(filterContext_.view, worldMatrice, request.dcb.worldView);
        request.dcb.meshIndex = handle.index;
        request.drawCallId = currentDrawCall_;

        if (!filterContext_.options.enableFiltering)
        {
                /*D3D11_MAPPED_SUBRESOURCE mapping;
                deviceContext_->Map(drawCallConstantBuffers_[currentDrawCall_].Get(), 0,
                        D3D11_MAP_WRITE_DISCARD, 0, &mapping);
                ::memcpy(mapping.pData, &request.dcb, sizeof(request.dcb));
                deviceContext_->Unmap(drawCallConstantBuffers_[currentDrawCall_].Get(), 0);*/

            ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(request.dcb.SIZE);
            request.dcb.store(buffer).flip();
            drawCallConstantBuffers_[currentDrawCall_].update(0, buffer);
        }

        drawCommands_.add(request);
        ++currentDrawCall_;
    }

    void RenderMeshInstanced(MeshHandle handle, Matrix4f[] worldMatrices) {
        for (int i = 0; i < worldMatrices.length; ++i)
        {
            RenderMeshInstanced(handle, worldMatrices[i]);
        }
    }

    void EndRender()
    {
        // Set this up for all vertex shaders
//        ID3D11Buffer *constantBuffers[] = { frameConstantBuffer_.Get() };

//        deviceContext_->VSSetConstantBuffers(1, 1, constantBuffers);
        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 1, frameConstantBuffer_.getBuffer());

        if (filterContext_.options.enableFiltering)
        {
            RenderGeometryChunked(/*deviceContext_,*/ filterContext_);
        }
        else
        {
            RenderGeometryDefault(/*deviceContext_, filterContext_*/);
        }

//        deviceContext_ = nullptr;
    }

    /*void GetBuffersForMesh(MeshHandle handle, ID3D11Buffer **vertexBuffer,
                           int32 *vertexOffset, ID3D11Buffer **indexBuffer, int32 *indexOffset) const
    {
        const auto &mesh = handle->mesh;

        if (vertexBuffer)
        {
            *vertexBuffer = mesh->vertexBuffer.Get();
        }

        if (vertexOffset)
        {
            *vertexOffset = mesh->vertexOffset;
        }

        if (indexBuffer)
        {
            *indexBuffer = mesh->indexBuffer.Get();
        }

        if (indexOffset)
        {
            *indexOffset = mesh->indexOffset;
        }
    }*/

    int GetMeshInfo(MeshHandle handle/*, int32 *indexCount*/)
    {
        /*if (indexCount)
        {
            *indexCount = handle->mesh->indexCount;
        }*/

        return handle.mesh.indexCount;
    }

    void CreateShaders() {
        gl = GLFuncProviderFactory.getGLFuncProvider();
        /*depthOnlyVertexShader_ = ComPtr<ID3D11VertexShader>();
        depthOnlyLayout_ = ComPtr<ID3D11InputLayout>();
        depthOnlyVertexShaderMID_ = ComPtr<ID3D11VertexShader>();
        depthOnlyLayoutMID_ = ComPtr<ID3D11InputLayout>();
        filterComputeShader_ = ComPtr<ID3D11ComputeShader>();
        clearDrawIndirectArgumentsComputeShader_ = ComPtr<ID3D11ComputeShader>();*/

        final String root = "amdfx\\GeometryFX\\shaders\\";
        final int DXGI_FORMAT_R32G32B32_FLOAT = GLenum.GL_RGB32F;
        final int DXGI_FORMAT_R32_UINT = GLenum.GL_R32UI;
        final int D3D11_INPUT_PER_VERTEX_DATA = 0;
        final int D3D11_INPUT_PER_INSTANCE_DATA = 1;
        final D3D11_INPUT_ELEMENT_DESC depthOnlyLayout[] =
        {
            new D3D11_INPUT_ELEMENT_DESC( "POSITION", 0, DXGI_FORMAT_R32G32B32_FLOAT, 0, 0, D3D11_INPUT_PER_VERTEX_DATA, 0 )
        };

        /*CreateShader(device_, (ID3D11DeviceChild **)depthOnlyVertexShader_.GetAddressOf(),
            sizeof(AMD_GeometryFX_DepthOnlyVS), AMD_GeometryFX_DepthOnlyVS, ShaderType::Vertex, &depthOnlyLayout_,
            ARRAYSIZE(depthOnlyLayout), depthOnlyLayout);*/

        depthOnlyVertexShader_ = GLSLProgram.createProgram(root + "DepthOnlyVS.vert", null, null);
        depthOnlyLayout_ = ID3D11InputLayout.createInputLayoutFrom(depthOnlyLayout);

        final D3D11_INPUT_ELEMENT_DESC depthOnlyLayoutMID[] =
        {
            new D3D11_INPUT_ELEMENT_DESC( "POSITION", 0, DXGI_FORMAT_R32G32B32_FLOAT, 0, 0, D3D11_INPUT_PER_VERTEX_DATA, 0 ),
            new D3D11_INPUT_ELEMENT_DESC( "DRAWID", 0, DXGI_FORMAT_R32_UINT, 1, 0, D3D11_INPUT_PER_INSTANCE_DATA, 1 )
        };

        /*CreateShader(device_, (ID3D11DeviceChild **)depthOnlyVertexShaderMID_.GetAddressOf(),
            sizeof(AMD_GeometryFX_DepthOnlyMultiIndirectVS), AMD_GeometryFX_DepthOnlyMultiIndirectVS,
            ShaderType::Vertex, &depthOnlyLayoutMID_, ARRAYSIZE(depthOnlyLayoutMID),
            depthOnlyLayoutMID);*/

        depthOnlyVertexShaderMID_ = GLSLProgram.createProgram(root + "DepthOnlyMultiIndirectVS.vert", null, null);
        depthOnlyLayoutMID_ = ID3D11InputLayout.createInputLayoutFrom(depthOnlyLayoutMID);

        /*CreateShader(device_,
                (ID3D11DeviceChild **)clearDrawIndirectArgumentsComputeShader_.GetAddressOf(),
            sizeof(AMD_GeometryFX_ClearDrawIndirectArgsCS), AMD_GeometryFX_ClearDrawIndirectArgsCS,
            ShaderType::Compute);*/

        clearDrawIndirectArgumentsComputeShader_ = GLSLProgram.createProgram(root + "ClearDrawIndirectArgsCS.comp", null);

        /*CreateShader(device_, (ID3D11DeviceChild **)filterComputeShader_.GetAddressOf(),
            sizeof(AMD_GeometryFX_FilterCS), AMD_GeometryFX_FilterCS, ShaderType::Compute);*/

        filterComputeShader_ = GLSLProgram.createProgram(root + "FilterCS.comp", null);
    }

    private static int RoundToNextMultiple(int value, int multiple) {
        return ((value + multiple - 1) / multiple) * multiple;
    }


    void CreateIndirectDrawArgumentsBuffer(
        final int meshCount, final int[] indicesInMesh, final int[] verticesInMesh )
    {
        /*D3D11_BUFFER_DESC indirectArgsBufferDesc = {};
        indirectArgsBufferDesc.BindFlags = D3D11_BIND_UNORDERED_ACCESS;

        // Round to multiples of 256 so the clear shader doesn't have to test
        // bounds
        indirectArgsBufferDesc.ByteWidth = sizeof(IndirectArguments) * roundedIndirectArgsCount;
        indirectArgsBufferDesc.MiscFlags = D3D11_RESOURCE_MISC_DRAWINDIRECT_ARGS;
        indirectArgsBufferDesc.Usage = D3D11_USAGE_DEFAULT;
        indirectArgsBufferDesc.StructureByteStride = sizeof(IndirectArguments);

        D3D11_SUBRESOURCE_DATA indirectArgsData;
        indirectArgsData.pSysMem = indirectArgs.data();
        indirectArgsData.SysMemPitch =
                static_cast<UINT>(indirectArgs.size() * sizeof(IndirectArguments));
        indirectArgsData.SysMemSlicePitch = indirectArgsData.SysMemPitch;

        device_->CreateBuffer(
                &indirectArgsBufferDesc, &indirectArgsData, &indirectArgumentsBuffer_);
        device_->CreateBuffer(
                &indirectArgsBufferDesc, &indirectArgsData, &indirectArgumentsBufferPristine_);*/

        final int roundedIndirectArgsCount = RoundToNextMultiple(meshCount, 256);
        IndirectArguments[] indirectArgs = new IndirectArguments[roundedIndirectArgsCount];

        for (int i = 0; i < roundedIndirectArgsCount; ++i)
        {
//            IndirectArguments::Init(indirectArgs[i]);
            indirectArgs[i] = new IndirectArguments();
            indirectArgs[i].IndexCountPerInstance = i<meshCount ? indicesInMesh[i] : 0;
        }

        ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(indirectArgs.length * IndirectArguments.SIZE);
        CacheBuffer.put(buffer, indirectArgs);
        buffer.flip();

        indirectArgumentsBuffer_ = new BufferGL();
        indirectArgumentsBuffer_.initlize(GLenum.GL_DRAW_INDIRECT_BUFFER, buffer.remaining(), buffer, GLenum.GL_STATIC_DRAW);

        indirectArgumentsBufferPristine_ = new BufferGL();
        indirectArgumentsBufferPristine_.initlize(GLenum.GL_DRAW_INDIRECT_BUFFER, buffer.remaining(), buffer, GLenum.GL_STATIC_DRAW);

//        SetDebugName(indirectArgumentsBuffer_.Get(), "[AMD GeometryFX Filtering] IndirectArgumentBuffer");
//        SetDebugName(indirectArgumentsBufferPristine_.Get(),
//                "[AMD GeometryFX Filtering] IndirectArgumentBuffer pristine version");

        /*D3D11_UNORDERED_ACCESS_VIEW_DESC indirectArgsUAVDesc = {};
        indirectArgsUAVDesc.Buffer.FirstElement = 0;
        indirectArgsUAVDesc.Buffer.NumElements = static_cast<int>(meshCount * 5);
        indirectArgsUAVDesc.Format = DXGI_FORMAT_R32_UINT;
        indirectArgsUAVDesc.ViewDimension = D3D11_UAV_DIMENSION_BUFFER;

        device_->CreateUnorderedAccessView(
                indirectArgumentsBuffer_.Get(), &indirectArgsUAVDesc, &indirectArgumentsUAV_);*/

        indirectArgumentsUAV_ = indirectArgumentsBuffer_;
    }

    void CreateDrawCallConstantBuffers()
    {
        drawCallConstantBuffers_ = new BufferGL[maxDrawCallCount_];

        for (int i = 0; i < maxDrawCallCount_; ++i)
        {
            /*D3D11_BUFFER_DESC cbDesc;
            ZeroMemory(&cbDesc, sizeof(cbDesc));
            cbDesc.Usage = D3D11_USAGE_DYNAMIC;
            cbDesc.BindFlags = D3D11_BIND_CONSTANT_BUFFER;
            cbDesc.ByteWidth = sizeof(DrawCallArguments);
            cbDesc.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE;
            device_->CreateBuffer(&cbDesc, NULL, &drawCallConstantBuffers_[i]);

            SetDebugName(
                    drawCallConstantBuffers_[i].Get(), "[AMD GeometryFX Filtering] Draw call constant buffer [%d]", i);*/

            drawCallConstantBuffers_[i]  = new BufferGL();
            drawCallConstantBuffers_[i].initlize(GLenum.GL_UNIFORM_BUFFER, DrawCallArguments.SIZE, null, GLenum.GL_DYNAMIC_COPY);
        }
    }

    void CreateQueries()
    {
        /*D3D11_QUERY_DESC queryDesc;
        queryDesc.MiscFlags = 0;
        queryDesc.Query = D3D11_QUERY_PIPELINE_STATISTICS;
        device_->CreateQuery(&queryDesc, &pipelineQuery_);*/

        pipelineQuery_ = gl.glGenQuery();
    }

    void CreateConstantBuffers()
    {
        /*D3D11_BUFFER_DESC cbDesc;
        ZeroMemory(&cbDesc, sizeof(cbDesc));
        cbDesc.Usage = D3D11_USAGE_DYNAMIC;
        cbDesc.BindFlags = D3D11_BIND_CONSTANT_BUFFER;
        cbDesc.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE;
        cbDesc.ByteWidth = sizeof(FrameConstantBuffer);
        device_->CreateBuffer(&cbDesc, NULL, &frameConstantBuffer_);
        SetDebugName(frameConstantBuffer_.Get(), "[AMD GeometryFX Filtering] PerFrameConstantBuffer");*/

        frameConstantBuffer_ = new BufferGL();
        frameConstantBuffer_.initlize(GLenum.GL_UNIFORM_BUFFER, FrameConstantBuffer.SIZE, null, GLenum.GL_DYNAMIC_COPY);
    }

    void ClearIndirectArgsBuffer(/*ID3D11DeviceContext *context*/)
    {
        /*ID3D11UnorderedAccessView *uavViews[] = { indirectArgumentsUAV_.Get() };
        UINT initialCounts[] = { 0 };
        context->CSSetUnorderedAccessViews(1, 1, uavViews, initialCounts);
        context->CSSetShader(clearDrawIndirectArgumentsComputeShader_.Get(), nullptr, 0);
        context->Dispatch(
                static_cast<UINT>(RoundToNextMultiple(meshManager_->GetMeshCount(), 256)), 1, 1);

        uavViews[0] = nullptr;

        context->CSSetUnorderedAccessViews(0, 1, uavViews, initialCounts);*/

        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 3, indirectArgumentsUAV_.getBuffer());
        clearDrawIndirectArgumentsComputeShader_.enable();
        gl.glDispatchCompute(RoundToNextMultiple(meshManager_.GetMeshCount(), 256), 1, 1);
        gl.glMemoryBarrier(GLenum.GL_SHADER_STORAGE_BARRIER_BIT);
        gl.glFlush();

        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 3, 0);
    }

    void RenderGeometryDefault(/*ID3D11DeviceContext *context, FilterContext &  filterContext */)
    {
//        assert(context);
        /*ComPtr<ID3DUserDefinedAnnotation> annotation;
        context->QueryInterface(IID_PPV_ARGS(&annotation)); // QueryInterface can fail with E_NOINTERFACE

        context->IASetInputLayout(depthOnlyLayout_.Get());
        context->VSSetShader(depthOnlyVertexShader_.Get(), NULL, 0);
        context->IASetPrimitiveTopology(D3D11_PRIMITIVE_TOPOLOGY_TRIANGLELIST);*/

        depthOnlyVertexShader_.enable();

        /*if (annotation.Get() != nullptr)
        {
            annotation->BeginEvent(L"Depth pass");
        }*/

        for (DrawCommand it : drawCommands_)
        {
            /*ID3D11Buffer *vertexBuffers[] = { it->mesh->vertexBuffer.Get() };
            UINT strides[] = { sizeof(float) * 3 };
            UINT offsets[] = { static_cast<UINT>(it->mesh->vertexOffset) };
            context->IASetVertexBuffers(0, 1, vertexBuffers, strides, offsets);
            context->IASetIndexBuffer(
                    it->mesh->indexBuffer.Get(), DXGI_FORMAT_R32_UINT, it->mesh->indexOffset);
            ID3D11Buffer *constantBuffers[] = { drawCallConstantBuffers_[it->drawCallId].Get() };
            context->VSSetConstantBuffers(0, 1, constantBuffers);
            context->DrawIndexed(it->mesh->indexCount, 0, 0);*/

            gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, it.mesh.vertexBuffer.getBuffer());
            depthOnlyLayout_.bind();

            gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, it.mesh.indexBuffer.getBuffer());
            gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 1, drawCallConstantBuffers_[it.drawCallId].getBuffer());
            gl.glDrawElements(GLenum.GL_TRIANGLES, it.mesh.indexCount, GLenum.GL_UNSIGNED_INT, 0);

        }

        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);
        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, 0);

        depthOnlyLayout_.unbind();

        /*if (annotation.Get() != nullptr)
        {
            annotation->EndEvent();
        }*/
    }

    void RenderGeometryChunked(/*ID3D11DeviceContext *context,*/ FilterContext filterContext)
    {
        if (drawCommands_.isEmpty())
        {
            return;
        }

//        ComPtr<ID3DUserDefinedAnnotation> annotation;
//        context->QueryInterface(IID_PPV_ARGS(&annotation)); // QueryInterface can fail with E_NOINTERFACE

        int currentSmallBatchChunk = 0;

//        context->IASetInputLayout(depthOnlyLayoutMID_.Get());
        GLSLProgram vertexShader = depthOnlyVertexShaderMID_;

        /*if (annotation.Get() != nullptr)
        {
            annotation->BeginEvent(L"Depth pass");
        }*/

        final DrawCommand current = new DrawCommand();
        final DrawCommand next = new DrawCommand();
        for (DrawCommand it : drawCommands_)
        {
            current.set(it);
            while (smallBatchChunks_.get(currentSmallBatchChunk).AddRequest(current, next, filterContext))
            {
                final int trianglesInBatch =
                    smallBatchChunks_.get(currentSmallBatchChunk).GetFaceCount();

                if (filterContext.options.statistics != null)
                {
                    filterContext.options.statistics.trianglesProcessed += trianglesInBatch;
//                    context.Begin(pipelineQuery_.Get());
                    gl.glBeginQuery(GLenum.GL_PRIMITIVES_GENERATED, pipelineQuery_);
                }

                // Overflow, submit this batch and continue with next one
                smallBatchChunks_.get(currentSmallBatchChunk).Render(//context,
                    clearDrawIndirectArgumentsComputeShader_,
                    filterComputeShader_,
                    vertexShader, depthOnlyLayoutMID_,
                    meshManager_.GetVertexBufferSRV(),
                    meshManager_.GetIndexBufferSRV(), meshManager_.GetMeshConstantsBuffer(),
                    meshManager_.GetVertexBuffer(), frameConstantBuffer_);

                if (filterContext.options.statistics != null)
                {
//                    context->End(pipelineQuery_.Get());
                    gl.glEndQuery(GLenum.GL_PRIMITIVES_GENERATED);

                    /*D3D11_QUERY_DATA_PIPELINE_STATISTICS stats;
                    while (context->GetData(pipelineQuery_.Get(), &stats, sizeof(stats), 0) != S_OK)
                    {
                        Yield();
                    }*/
                    int result;
                    do{
                        result = gl.glGetQueryObjectuiv(pipelineQuery_, GLenum.GL_QUERY_RESULT_AVAILABLE);
                    }while (result == GLenum.GL_FALSE);

                    int samples = gl.glGetQueryObjectuiv(pipelineQuery_, GLenum.GL_QUERY_RESULT);

                    filterContext.options.statistics.trianglesRendered += samples;
                    filterContext.options.statistics.trianglesCulled +=
                        (trianglesInBatch - samples);
                }

                current.set(next);
                currentSmallBatchChunk = (currentSmallBatchChunk + 1) % smallBatchChunks_.size();
            }
        }

        final int trianglesInBatch = smallBatchChunks_.get(currentSmallBatchChunk).GetFaceCount();
        if (filterContext.options.statistics!= null)
        {
//            context->Begin(pipelineQuery_.Get());
            gl.glBeginQuery(GLenum.GL_PRIMITIVES_GENERATED, pipelineQuery_);
            filterContext.options.statistics.trianglesProcessed += trianglesInBatch;
        }

        smallBatchChunks_.get(currentSmallBatchChunk).Render(//context,
            clearDrawIndirectArgumentsComputeShader_,
            filterComputeShader_,
            vertexShader, depthOnlyLayoutMID_, meshManager_.GetVertexBufferSRV(), meshManager_.GetIndexBufferSRV(),
            meshManager_.GetMeshConstantsBuffer(), meshManager_.GetVertexBuffer(),
            frameConstantBuffer_);

        if (filterContext.options.statistics!=null)
        {
//            context->End(pipelineQuery_.Get());
            gl.glEndQuery(GLenum.GL_PRIMITIVES_GENERATED);

            /*D3D11_QUERY_DATA_PIPELINE_STATISTICS stats;
            while (context->GetData(pipelineQuery_.Get(), &stats, sizeof(stats), 0) != S_OK)
            {
                Yield();
            }*/

            int result;
            do{
                result = gl.glGetQueryObjectuiv(pipelineQuery_, GLenum.GL_QUERY_RESULT_AVAILABLE);
            }while (result == GLenum.GL_FALSE);

            int samples = gl.glGetQueryObjectuiv(pipelineQuery_, GLenum.GL_QUERY_RESULT);

            filterContext.options.statistics.trianglesRendered += samples;
            filterContext.options.statistics.trianglesCulled +=
                (trianglesInBatch - samples);
        }

        /*if (annotation.Get() != nullptr)
        {
            annotation->EndEvent();
        }*/
    }
}
