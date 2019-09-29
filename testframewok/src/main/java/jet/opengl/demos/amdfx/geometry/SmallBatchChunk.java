package jet.opengl.demos.amdfx.geometry;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.nio.ByteBuffer;
import java.util.List;

import jet.opengl.demos.intel.cput.ID3D11InputLayout;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 One small batch chunk can accept multiple draw requests. Draw requests are
 split into small batches of TRIANGLES_PER_SMALL_BATCH each. A draw request always
 occupies consecutive slots. A draw request may be split if it does not fit
 entirely into this small batch.<p></p>

 The filter then processes all small batches in this chunk in one go, and renders
 them by using one indirect draw call per original draw request.
 */
class SmallBatchChunk {

    private BufferGL smallBatchDataBuffer_;
    private BufferGL smallBatchDataSRV_;  // ID3D11ShaderResourceView
    private BufferGL filteredIndexBuffer_;
    private BufferGL filteredIndexUAV_;    // ID3D11UnorderedAccessView
    private BufferGL indirectArgumentsBuffer_;
    private BufferGL indirectArgumentsUAV_;
    private BufferGL drawCallBuffer_;
    private BufferGL drawCallSRV_;   // ID3D11ShaderResourceView

    private List<BufferGL> drawCallConstantBuffers_;
    private BufferGL drawCallConstantBufferMerged_;

    private BufferGL instanceIdBuffer_;

    private final SmallBatchData[] smallBatchDataBackingStore_ = new SmallBatchData[SmallBatchMergeConstants.BATCH_COUNT];
    private final DrawCallArguments[] drawCallBackingStore_ = new DrawCallArguments[SmallBatchMergeConstants.BATCH_COUNT];

    private int currentBatchCount_;
    private int currentDrawCallCount_;
    private int faceCount_;

    private boolean useMultiIndirectDraw_;
//    private AGSContext agsContext_;

    private GLFuncProvider gl;

    SmallBatchChunk (/*ID3D11Device *device, bool emulateMultiDraw, AGSContext* agsContext*/ boolean emulateMultiDraw)
        /*: smallBatchDataBackingStore_ (SmallBatchMergeConstants::BATCH_COUNT)
        , drawCallBackingStore_ (SmallBatchMergeConstants::BATCH_COUNT)
        , agsContext_ (agsContext)
        , currentBatchCount_ (0)
        , currentDrawCallCount_ (0)
        , faceCount_ (0)
        , useMultiIndirectDraw_ (!emulateMultiDraw)*/
    {
        for(int i = 0; i < smallBatchDataBackingStore_.length; i++)
            smallBatchDataBackingStore_[i] = new SmallBatchData();

        for(int i = 0; i < drawCallBackingStore_.length; i++)
            drawCallBackingStore_[i] = new DrawCallArguments();

        gl = GLFuncProviderFactory.getGLFuncProvider();
        useMultiIndirectDraw_ = !emulateMultiDraw;
        CreateFilteredIndexBuffer (/*device*/);
        CreateSmallBatchDataBuffer (/*device*/);
        CreateIndirectDrawArgumentsBuffer (/*device*/);
        CreateDrawCallArgumentsBuffer (/*device*/);

        CreateInstanceIdBuffer (/*device*/);
    }

    /**
     If true is returned, then remainder has been filled and must be
     re-submitted. Otherwise, the whole request has been handled by this small
     batch.
     */
    boolean AddRequest (final DrawCommand request, final DrawCommand remainder,
                     FilterContext filterContext)
    {
        if (currentDrawCallCount_ == SmallBatchMergeConstants.BATCH_COUNT)
        {
            remainder.set(request);
            return true;
        }

        assert (request.firstTriangle >= 0);

        int firstTriangle = request.firstTriangle;
        final int firstCluster = firstTriangle / SmallBatchMergeConstants.BATCH_SIZE;
        int currentCluster = firstCluster;
        int lastTriangle = firstTriangle;

        final int filteredIndexBufferStartOffset =
            currentBatchCount_ * SmallBatchMergeConstants.BATCH_SIZE * 3 * /*sizeof (int)*/4;

        final int firstBatch = currentBatchCount_;

        // We move the eye position into object space, so we don't have to
        // transform the cone into world space all the time
        // This matrix inversion will happen once every 2^16 triangles on
        // average; and saves us transforming the cone every 256 triangles
//        const auto eye = DirectX::XMVector4Transform (filterContext.eye, XMMatrixInverse (nullptr, request.dcb.world));
        Matrix4f worldInv = Matrix4f.invert(request.dcb.world, null);
        Vector3f eye = Matrix4f.transformVector(worldInv, filterContext.eye, null);

        // Try to assign batches until we run out of batches or geometry
        for (int i = currentBatchCount_; i < SmallBatchMergeConstants.BATCH_COUNT; ++i)
        {
            lastTriangle = Math.min (
                firstTriangle + SmallBatchMergeConstants.BATCH_SIZE, request.mesh.faceCount);

            assert (currentCluster < request.mesh.clusters.length);
            StaticMesh.Cluster clusterInfo = request.mesh.clusters[currentCluster];
            ++currentCluster;

            boolean cullCluster = false;

            if (((filterContext.options.enabledFilters & GeometryFX_Filter.GeometryFX_ClusterFilterBackface) != 0) && clusterInfo.valid)
            {
//                const auto testVec = DirectX::XMVector3Normalize (DirectX::XMVectorSubtract (eye, clusterInfo.coneCenter));
                Vector3f testVec = Vector3f.sub(eye, clusterInfo.coneCenter, null);
                testVec.normalise();
                // Check if we're inside the cone
                if (Vector3f.dot(testVec, clusterInfo.coneAxis) > clusterInfo.coneAngleCosine)
                {
                    cullCluster = true;
                }
            }

            if (!cullCluster)
            {
                SmallBatchData smallBatchData = smallBatchDataBackingStore_[currentBatchCount_];

                smallBatchData.drawIndex = currentDrawCallCount_;
                smallBatchData.faceCount = lastTriangle - firstTriangle;

                // Offset relative to the start of the mesh
                smallBatchData.indexOffset = firstTriangle * 3 * /*sizeof (int)*/4;
                smallBatchData.outputIndexOffset = filteredIndexBufferStartOffset;
                smallBatchData.meshIndex = request.dcb.meshIndex;
                smallBatchData.drawBatchStart = firstBatch;

                faceCount_ += smallBatchData.faceCount;

                ++currentBatchCount_;
            }

            firstTriangle += SmallBatchMergeConstants.BATCH_SIZE;

            if (lastTriangle == request.mesh.faceCount)
            {
                break;
            }
        }

        if (filterContext.options.statistics != null)
        {
            filterContext.options.statistics.clustersProcessed +=
                currentCluster - firstCluster;

            filterContext.options.statistics.clustersRendered +=
                currentBatchCount_ - firstBatch;

            filterContext.options.statistics.clustersCulled +=
                filterContext.options.statistics.clustersProcessed - filterContext.options.statistics.clustersRendered;
        }

        if (currentBatchCount_ > firstBatch)
        {
            drawCallBackingStore_[currentDrawCallCount_] = request.dcb;
            ++currentDrawCallCount_;
        }

        // Check if the draw command fit into this call, if not, create a remainder
        if (lastTriangle < request.mesh.faceCount)
        {
            remainder.set(request);
            assert (lastTriangle >= 0);
            remainder.firstTriangle = lastTriangle;

            return true;
        }
        else
        {
            return false;
        }
    }

    void Render (GLSLProgram computeClearShader,
                 GLSLProgram filterShader, GLSLProgram vertexShader,
                 ID3D11InputLayout layout,
                 BufferGL vertexData, BufferGL indexData,
                 BufferGL meshConstantData, BufferGL globalVertexBuffer,
                 BufferGL perFrameConstantBuffer)
    {
        GLCheck.checkError();
        ClearIndirectArgsBuffer (/*context,*/ computeClearShader);
        UpdateDrawCallAndSmallBatchBuffers (/*context*/);
        Filter (/*context,*/ filterShader, vertexData, indexData, meshConstantData,
                perFrameConstantBuffer);

//        context->VSSetShader (vertexShader, nullptr, 0);

//        context->IASetIndexBuffer (filteredIndexBuffer_.Get (), DXGI_FORMAT_R32_UINT, 0);
//        context->IASetPrimitiveTopology (D3D11_PRIMITIVE_TOPOLOGY_TRIANGLELIST);

        vertexShader.enable();

        // Binding the vertex data
        gl.glBindVertexArray(0);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, globalVertexBuffer.getBuffer());
        gl.glEnableVertexAttribArray(0);
        gl.glVertexAttribPointer(0, 3, GLenum.GL_FLOAT, false, 0, 0);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, instanceIdBuffer_.getBuffer());
        gl.glEnableVertexAttribArray(1);
        gl.glVertexAttribPointer(1, 1, GLenum.GL_UNSIGNED_INT, false, 0, 0);

        // Binding the index data
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, filteredIndexBuffer_.getBuffer());

        // Binding the drawConstat
        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 6, drawCallSRV_.getBuffer());

        // TODO missing the DrawCallConstantBuffer buffer here.

        // Indirect buffer
        gl.glBindBuffer(GLenum.GL_DRAW_INDIRECT_BUFFER, indirectArgumentsBuffer_.getBuffer());

        for (int i = 0; i < currentDrawCallCount_; ++i)
        {
//            context->DrawIndexedInstancedIndirect (
//                    indirectArgumentsBuffer_.Get (), sizeof (IndirectArguments) * i);
            gl.glDrawElementsIndirect(GLenum.GL_TRIANGLES, GLenum.GL_UNSIGNED_INT, IndirectArguments.SIZE * i);
        }

        /*ID3D11Buffer *iaVBs[] = { globalVertexBuffer, instanceIdBuffer_.Get () };
        UINT vbOffsets[] = { 0, 0 };
        UINT vbStrides[] = { sizeof (float) * 3, sizeof (int) };
        context->IASetVertexBuffers (0, 2, iaVBs, vbStrides, vbOffsets);

        ID3D11ShaderResourceView *srvs[] = { drawCallSRV_.Get () };

        context->VSSetShaderResources (3, 1, srvs);

        if (agsContext_ && useMultiIndirectDraw_)
        {
            agsDriverExtensions_MultiDrawIndexedInstancedIndirect (agsContext_,
                    currentDrawCallCount_,
                    indirectArgumentsBuffer_.Get (), 0, sizeof (IndirectArguments));
        }
        else
        {
            for (int i = 0; i < currentDrawCallCount_; ++i)
            {
                context->DrawIndexedInstancedIndirect (
                        indirectArgumentsBuffer_.Get (), sizeof (IndirectArguments) * i);
            }
        }

        context->IASetIndexBuffer (nullptr, DXGI_FORMAT_R32_UINT, 0);*/

        if(GeometryFX_Filter.isPrintLog())
            vertexShader.printPrograminfo();

        Reset ();
    }

    int GetFaceCount ()
    {
        return faceCount_;
    }

    private void Filter (/*ID3D11DeviceContext *context,*/ GLSLProgram filterShader,
                       BufferGL vertexData, BufferGL indexData,
                       BufferGL meshConstantData, BufferGL perFrameConstantBuffer) {
        /*ID3D11ShaderResourceView *csSRVs[] = { vertexData, indexData, meshConstantData, drawCallSRV_.Get (), smallBatchDataSRV_.Get () };
        context->CSSetShaderResources (0, 5, csSRVs);

        UINT initialCounts[] = { 0, 0 };
        ID3D11UnorderedAccessView *csUAVs[] = { filteredIndexUAV_.Get (), indirectArgumentsUAV_.Get () };
        context->CSSetUnorderedAccessViews (0, 2, csUAVs, initialCounts);

        ID3D11Buffer *csCBs[] = { perFrameConstantBuffer };
        context->CSSetConstantBuffers (1, 1, csCBs);

        context->CSSetShader (filterShader, nullptr, 0);

        context->Dispatch (currentBatchCount_, 1, 1);

        csUAVs[0] = nullptr;
        csUAVs[1] = nullptr;
        context->CSSetUnorderedAccessViews (0, 2, csUAVs, initialCounts);*/

        // Read only Shader Resource Views
        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 4, vertexData.getBuffer());
        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 8, indexData.getBuffer());
        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 5, meshConstantData.getBuffer());
        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 6, drawCallSRV_.getBuffer());
        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 7, smallBatchDataSRV_.getBuffer());
        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 1, perFrameConstantBuffer.getBuffer());

        // Unordered Shader Resource Views
        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 2, filteredIndexUAV_.getBuffer());
        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 3, indirectArgumentsUAV_.getBuffer());

        filterShader.enable();

        gl.glDispatchCompute(currentBatchCount_, 1, 1);

        //Unbind the unordered shader resource views
        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 2, 0);
        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 3, 0);

        if(GeometryFX_Filter.isPrintLog())
            filterShader.printPrograminfo();
    }

    private void UpdateDrawCallAndSmallBatchBuffers (/*ID3D11DeviceContext *context*/) {
        /*D3D11_MAPPED_SUBRESOURCE mapping;
        context->Map (smallBatchDataBuffer_.Get (), 0, D3D11_MAP_WRITE_DISCARD, 0, &mapping);

        ::memcpy (mapping.pData, smallBatchDataBackingStore_.data (),
            sizeof (SmallBatchData) * smallBatchDataBackingStore_.size ());

        context->Unmap (smallBatchDataBuffer_.Get (), 0);

        context->Map (drawCallBuffer_.Get (), 0, D3D11_MAP_WRITE_DISCARD, 0, &mapping);

        ::memcpy (mapping.pData, drawCallBackingStore_.data (),
            sizeof (DrawCallArguments) * drawCallBackingStore_.size ());

        context->Unmap (drawCallBuffer_.Get (), 0);*/

        ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(smallBatchDataBackingStore_.length * SmallBatchData.SIZE);
        for(int i = 0; i < smallBatchDataBackingStore_.length; i++)
            smallBatchDataBackingStore_[i].store(buffer);
        buffer.flip();
        smallBatchDataBuffer_.update(0, buffer);

        buffer = CacheBuffer.getCachedByteBuffer(drawCallBackingStore_.length * DrawCallArguments.SIZE);
        for(int i = 0; i < drawCallBackingStore_.length; i++)
            drawCallBackingStore_[i].store(buffer);
        buffer.flip();
        drawCallBuffer_.update(0, buffer);
    }

    void CreateFilteredIndexBuffer (/*ID3D11Device *device*/) {
        /*D3D11_BUFFER_DESC filteredIndexBufferDesc = {};
        filteredIndexBufferDesc.BindFlags = D3D11_BIND_INDEX_BUFFER | D3D11_BIND_UNORDERED_ACCESS;
        filteredIndexBufferDesc.ByteWidth = SmallBatchMergeConstants::BATCH_COUNT *
                SmallBatchMergeConstants::BATCH_SIZE *
                (sizeof (int) * 3);
        filteredIndexBufferDesc.CPUAccessFlags = 0;
        filteredIndexBufferDesc.MiscFlags = 0;
        filteredIndexBufferDesc.Usage = D3D11_USAGE_DEFAULT;

        device->CreateBuffer (&filteredIndexBufferDesc, nullptr, &filteredIndexBuffer_);
        SetDebugName (filteredIndexBuffer_.Get (), "[AMD GeometryFX Filtering] Filtered index buffer [%p]", this);*/

        filteredIndexBuffer_ = new BufferGL();
        filteredIndexBuffer_.initlize(GLenum.GL_UNIFORM_BUFFER, SmallBatchMergeConstants.BATCH_COUNT *
                SmallBatchMergeConstants.BATCH_SIZE * (/*sizeof (int)*/4 * 3), null, GLenum.GL_STREAM_COPY);
        filteredIndexBuffer_.setName("[AMD GeometryFX Filtering] Filtered index buffer");

        /*D3D11_UNORDERED_ACCESS_VIEW_DESC fibUav = {};
        fibUav.Buffer.FirstElement = 0;
        fibUav.Buffer.Flags = 0;
        fibUav.Buffer.NumElements =
                SmallBatchMergeConstants::BATCH_COUNT * SmallBatchMergeConstants::BATCH_SIZE * 3;
        fibUav.Format = DXGI_FORMAT_R32_UINT;
        fibUav.ViewDimension = D3D11_UAV_DIMENSION_BUFFER;

        device->CreateUnorderedAccessView (filteredIndexBuffer_.Get (), &fibUav, &filteredIndexUAV_);
        SetDebugName (filteredIndexUAV_.Get (), "[AMD GeometryFX Filtering] Filtered index buffer UAV [%p]", this);*/
        filteredIndexBuffer_.createTextureBuffer(GLenum.GL_R32UI);
        filteredIndexUAV_ = filteredIndexBuffer_;
    }

    void CreateSmallBatchDataBuffer (/*ID3D11Device *device*/)
    {
        /*D3D11_BUFFER_DESC smallBatchDataBufferDesc;
        smallBatchDataBufferDesc.BindFlags = D3D11_BIND_SHADER_RESOURCE;
        smallBatchDataBufferDesc.ByteWidth =
                SmallBatchMergeConstants::BATCH_COUNT * sizeof (SmallBatchData);
        smallBatchDataBufferDesc.MiscFlags = D3D11_RESOURCE_MISC_BUFFER_STRUCTURED;
        smallBatchDataBufferDesc.StructureByteStride = sizeof (SmallBatchData);
        smallBatchDataBufferDesc.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE;
        smallBatchDataBufferDesc.Usage = D3D11_USAGE_DYNAMIC;

        device->CreateBuffer (&smallBatchDataBufferDesc, nullptr, &smallBatchDataBuffer_);
        SetDebugName (smallBatchDataBuffer_.Get (), "[AMD GeometryFX Filtering] Batch data buffer [%p]", this);*/

        smallBatchDataBuffer_ = new BufferGL();
        smallBatchDataBuffer_.initlize(GLenum.GL_UNIFORM_BUFFER, SmallBatchMergeConstants.BATCH_COUNT * SmallBatchData.SIZE, null, GLenum.GL_STREAM_COPY);
        smallBatchDataBuffer_.setName("[AMD GeometryFX Filtering] Batch data buffer");

//        D3D11_SHADER_RESOURCE_VIEW_DESC smallBatchDataSRVDesc;
//        smallBatchDataSRVDesc.Buffer.FirstElement = 0;
//        smallBatchDataSRVDesc.Buffer.NumElements = SmallBatchMergeConstants::BATCH_COUNT;
//        smallBatchDataSRVDesc.Format = DXGI_FORMAT_UNKNOWN;
//        smallBatchDataSRVDesc.ViewDimension = D3D11_SRV_DIMENSION_BUFFER;
//
//        device->CreateShaderResourceView (
//                smallBatchDataBuffer_.Get (), &smallBatchDataSRVDesc, &smallBatchDataSRV_);
//        SetDebugName (smallBatchDataSRV_.Get (), "[AMD GeometryFX Filtering] Batch data buffer SRV [%p]", this);

        smallBatchDataSRV_ = smallBatchDataBuffer_;
    }

    void CreateIndirectDrawArgumentsBuffer (/*ID3D11Device *device*/)
    {
        /*D3D11_BUFFER_DESC indirectArgumentsBufferDesc;
        indirectArgumentsBufferDesc.BindFlags = D3D11_BIND_UNORDERED_ACCESS;
        indirectArgumentsBufferDesc.MiscFlags = D3D11_RESOURCE_MISC_DRAWINDIRECT_ARGS;
        indirectArgumentsBufferDesc.ByteWidth =
                sizeof (IndirectArguments) * SmallBatchMergeConstants::BATCH_COUNT;
        indirectArgumentsBufferDesc.StructureByteStride = sizeof (IndirectArguments);
        indirectArgumentsBufferDesc.CPUAccessFlags = 0;
        indirectArgumentsBufferDesc.Usage = D3D11_USAGE_DEFAULT;

        std::vector<IndirectArguments> indirectArgs (SmallBatchMergeConstants::BATCH_COUNT);

        for (int i = 0; i < SmallBatchMergeConstants::BATCH_COUNT; ++i)
        {
            IndirectArguments::Init (indirectArgs[i]);
        }

        D3D11_SUBRESOURCE_DATA indirectArgumentsBufferData;
        indirectArgumentsBufferData.pSysMem = indirectArgs.data ();
        indirectArgumentsBufferData.SysMemPitch =
                static_cast<UINT>(sizeof (IndirectArguments) * indirectArgs.size ());
        indirectArgumentsBufferData.SysMemSlicePitch = indirectArgumentsBufferData.SysMemPitch;

        device->CreateBuffer (
                &indirectArgumentsBufferDesc, &indirectArgumentsBufferData, &indirectArgumentsBuffer_);

        SetDebugName (indirectArgumentsBuffer_.Get (), "[AMD GeometryFX Filtering] Indirect arguments buffer [%p]", this);*/
        indirectArgumentsBuffer_ = new BufferGL();
        indirectArgumentsBuffer_.initlize(GLenum.GL_DRAW_INDIRECT_BUFFER, SmallBatchMergeConstants.BATCH_COUNT * IndirectArguments.SIZE, null, GLenum.GL_DYNAMIC_DRAW);

        /*D3D11_UNORDERED_ACCESS_VIEW_DESC indirectArgsUAVDesc = {};
        indirectArgsUAVDesc.Buffer.FirstElement = 0;
        indirectArgsUAVDesc.Buffer.NumElements = SmallBatchMergeConstants::BATCH_COUNT * 5;
        indirectArgsUAVDesc.Format = DXGI_FORMAT_R32_UINT;
        indirectArgsUAVDesc.ViewDimension = D3D11_UAV_DIMENSION_BUFFER;

        device->CreateUnorderedAccessView (
                indirectArgumentsBuffer_.Get (), &indirectArgsUAVDesc, &indirectArgumentsUAV_);
        SetDebugName (indirectArgumentsUAV_.Get (), "[AMD GeometryFX Filtering] Indirect arguments buffer UAV [%p]", this);*/
        indirectArgumentsUAV_ = indirectArgumentsBuffer_;
    }

    void CreateDrawCallArgumentsBuffer (/*ID3D11Device *device*/)
    {
        /*D3D11_BUFFER_DESC drawCallBufferDesc;
        drawCallBufferDesc.BindFlags = D3D11_BIND_SHADER_RESOURCE;
        drawCallBufferDesc.MiscFlags = D3D11_RESOURCE_MISC_BUFFER_STRUCTURED;
        drawCallBufferDesc.ByteWidth =
                sizeof (DrawCallArguments) * SmallBatchMergeConstants::BATCH_COUNT;
        drawCallBufferDesc.StructureByteStride = sizeof (DrawCallArguments);
        drawCallBufferDesc.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE;
        drawCallBufferDesc.Usage = D3D11_USAGE_DYNAMIC;
        device->CreateBuffer (&drawCallBufferDesc, nullptr, &drawCallBuffer_);
        SetDebugName (drawCallBuffer_.Get (), "[AMD GeometryFX Filtering] Draw arguments buffer [%p]", this);*/

        drawCallBuffer_ = new BufferGL();
        drawCallBuffer_.initlize(GLenum.GL_UNIFORM_BUFFER, DrawCallArguments.SIZE * SmallBatchMergeConstants.BATCH_COUNT, null, GLenum.GL_DYNAMIC_DRAW);
        drawCallBuffer_.setName("[AMD GeometryFX Filtering] Draw arguments buffer");

        /*D3D11_SHADER_RESOURCE_VIEW_DESC drawCallSRVDesc;
        drawCallSRVDesc.Buffer.FirstElement = 0;
        drawCallSRVDesc.Buffer.NumElements = SmallBatchMergeConstants::BATCH_COUNT;
        drawCallSRVDesc.Format = DXGI_FORMAT_UNKNOWN;
        drawCallSRVDesc.ViewDimension = D3D11_SRV_DIMENSION_BUFFER;

        device->CreateShaderResourceView (drawCallBuffer_.Get (), &drawCallSRVDesc, &drawCallSRV_);
        SetDebugName (drawCallSRV_.Get (), "[AMD GeometryFX Filtering] Draw arguments buffer SRV [%p]", this);*/

        drawCallSRV_ = drawCallBuffer_;
    }

    /**
     The instance ID buffer is our workaround for not having gl_DrawID in D3D.
     The buffer simply contains 0, 1, 2, 3 ..., and is bound with a per-instance
     rate of 1.
     */
    void CreateInstanceIdBuffer (/*ID3D11Device *device*/)
    {
        /*D3D11_BUFFER_DESC instanceIdBufferDesc = {};
        instanceIdBufferDesc.BindFlags = D3D11_BIND_VERTEX_BUFFER;
        instanceIdBufferDesc.ByteWidth = sizeof (int) * SmallBatchMergeConstants::BATCH_COUNT;
        instanceIdBufferDesc.StructureByteStride = sizeof (int);
        instanceIdBufferDesc.Usage = D3D11_USAGE_IMMUTABLE;

        std::vector<int> ids (SmallBatchMergeConstants::BATCH_COUNT);
        std::iota (ids.begin (), ids.end (), 0);

        D3D11_SUBRESOURCE_DATA data;
        data.pSysMem = ids.data ();
        data.SysMemPitch = instanceIdBufferDesc.ByteWidth;
        data.SysMemSlicePitch = data.SysMemPitch;

        device->CreateBuffer (&instanceIdBufferDesc, &data, &instanceIdBuffer_);
        SetDebugName (instanceIdBuffer_.Get (), "[AMD GeometryFX Filtering] Instance ID buffer [%p]", this);*/

        instanceIdBuffer_ = new BufferGL();
        instanceIdBuffer_.initlize(GLenum.GL_UNIFORM_BUFFER, SmallBatchMergeConstants.BATCH_COUNT * 4, null, GLenum.GL_DYNAMIC_DRAW);
    }

    void Reset ()
    {
        currentBatchCount_ = 0;
        currentDrawCallCount_ = 0;
        faceCount_ = 0;
    }

    void ClearIndirectArgsBuffer (
            /*ID3D11DeviceContext *context, */GLSLProgram computeClearShader)
    {
        /*ID3D11UnorderedAccessView *uavViews[] = { indirectArgumentsUAV_.Get () };
        UINT initialCounts[] = { 0 };
        context->CSSetUnorderedAccessViews (1, 1, uavViews, initialCounts);
        context->CSSetShader (computeClearShader, nullptr, 0);
        context->Dispatch (currentBatchCount_, 1, 1);

        uavViews[0] = nullptr;

        context->CSSetUnorderedAccessViews (0, 1, uavViews, initialCounts);*/

        GLCheck.checkError();
        computeClearShader.enable();
        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 3, indirectArgumentsUAV_.getBuffer());

        gl.glDispatchCompute(currentBatchCount_, 1, 1);
        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 3, 0);
        if(GeometryFX_Filter.isPrintLog()){
            computeClearShader.printPrograminfo();
        }
    }
}
