package jet.opengl.demos.gpupro.culling;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.util.BoundingBox;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.Recti;

final class LinkedListOITRenderer extends TransparencyRenderer{

    // Defines the number of tiles to divide the screen dimensions by
    // Tile mode is used for linked list method only, and is disabled by default
    // (thus tile size is 1x1, i.e. fullscreen)
    private static final int NUMBER_OF_TILES_X  = 3;
    private static final int NUMBER_OF_TILES_Y  = 2;

    int mScreenWidth;
    int mScreenHeight;

    private final BoundingBox mTransparencyMeshAABBWorld = new BoundingBox();
    private final Recti mTransparencyMeshViewport = new Recti();

    private BufferGL g_pStartOffsetBuffer;
    private BufferGL g_pFragmentAndLinkStructuredBuffer;
    private BufferGL g_pTileCoordinatesCB;
    private BufferGL g_pTransparentMeshCB;

    private GLSLProgram g_pStoreFragmentsPS_LinkedList;
    private GLSLProgram g_pRenderFragmentsPS_LinkedList;

    private Runnable g_pDepthTestEnabledNoDepthWritesStencilWriteIncrementDSS;

    private Runnable g_pDepthTestDisabledStencilTestLessDSS;

    private Renderer mSceneRenderer;

    @Override
    protected void onCreate() {
        super.onCreate();

        g_pTileCoordinatesCB = new BufferGL();
        g_pTileCoordinatesCB.initlize(GLenum.GL_UNIFORM_BUFFER, Vector4f.SIZE * 2, null, GLenum.GL_DYNAMIC_DRAW);

        g_pTransparentMeshCB = new BufferGL();
        g_pTransparentMeshCB.initlize(GLenum.GL_UNIFORM_BUFFER, PER_MESH_CONSTANT_BUFFER_STRUCT.SIZE, null, GLenum.GL_DYNAMIC_DRAW);

        final String defulatVS = "gpupro/Culling/shaders/ShadingVS.vert";
        final String root = "gpupro/OIT/shaders/";
        g_pStoreFragmentsPS_LinkedList = GLSLProgram.createProgram(defulatVS, root + "PS_StoreFragments.frag", null);
        g_pStoreFragmentsPS_LinkedList.setName("StoreFragments");

        g_pRenderFragmentsPS_LinkedList = GLSLProgram.createProgram("shader_libs/PostProcessingDefaultScreenSpaceVS.vert", root + "PS_RenderFragments.frag", null);
        g_pRenderFragmentsPS_LinkedList.setName("RenderFragments");

        g_pDepthTestEnabledNoDepthWritesStencilWriteIncrementDSS = ()->
        {
            gl.glEnable(GLenum.GL_DEPTH_TEST);
            gl.glDepthFunc(GLenum.GL_LESS);
            gl.glDepthMask(false);

            gl.glEnable(GLenum.GL_STENCIL_TEST);
            gl.glStencilFunc(GLenum.GL_ALWAYS, 1, 0xFF);
            gl.glStencilMask(0xFF);
            gl.glStencilOp(GLenum.GL_KEEP, GLenum.GL_KEEP, GLenum.GL_INCR);
        };

        g_pDepthTestDisabledStencilTestLessDSS = ()->
        {
            gl.glDisable(GLenum.GL_DEPTH_TEST);

            gl.glEnable(GLenum.GL_STENCIL_TEST);
            gl.glStencilFunc(GLenum.GL_LESS, 0, 0);
            gl.glStencilMask(0xFF);
            gl.glStencilOp(GLenum.GL_KEEP, GLenum.GL_KEEP, GLenum.GL_KEEP);
        };
    }

    @Override
    final OITType getType() {
        return OITType.LinkedList;
    }

    @Override
    protected void onResize(int width, int height) {
        mScreenWidth = width;
        mScreenHeight = height;

        int uTileWidth  = width;
        int uTileHeight = height;

        // Tiling mode only supported for linked list method
        uTileWidth  = ( width + (NUMBER_OF_TILES_X-1) ) / NUMBER_OF_TILES_X;
        uTileHeight = ( height + (NUMBER_OF_TILES_Y-1) ) / NUMBER_OF_TILES_Y;

        // Create Start Offset buffer
        /*D3D11_BUFFER_DESC OffsetBufferDesc;
        OffsetBufferDesc.BindFlags           = D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_UNORDERED_ACCESS;
        OffsetBufferDesc.ByteWidth           = uTileWidth * uTileHeight * sizeof(UINT);
        OffsetBufferDesc.MiscFlags           = D3D11_RESOURCE_MISC_BUFFER_ALLOW_RAW_VIEWS;
        OffsetBufferDesc.Usage               = D3D11_USAGE_DEFAULT;
        OffsetBufferDesc.CPUAccessFlags      = 0;
        OffsetBufferDesc.StructureByteStride = 0;
        V_RETURN( pd3dDevice->CreateBuffer(&OffsetBufferDesc, NULL, &g_pStartOffsetBuffer) );*/

        if(g_pStartOffsetBuffer == null || g_pStartOffsetBuffer.getBufferSize() != uTileWidth * uTileHeight * 4){
            SAFE_RELEASE(g_pStartOffsetBuffer);
            g_pStartOffsetBuffer = new BufferGL();
            g_pStartOffsetBuffer.initlize(GLenum.GL_SHADER_STORAGE_BUFFER, uTileWidth * uTileHeight * 4, null, GLenum.GL_STREAM_COPY);
        }

        if(g_pFragmentAndLinkStructuredBuffer == null || g_pFragmentAndLinkStructuredBuffer.getBufferSize() != uTileWidth * uTileHeight * 12){
            SAFE_RELEASE(g_pFragmentAndLinkStructuredBuffer);
            g_pFragmentAndLinkStructuredBuffer = new BufferGL();
            g_pFragmentAndLinkStructuredBuffer.initlize(GLenum.GL_SHADER_STORAGE_BUFFER, uTileWidth * uTileHeight * 12, null, GLenum.GL_STREAM_COPY);
        }
    }

    @Override
    void renderScene(Scene scene) {
        // Tile size
        int uTileWidth  = ( ( mScreenWidth  + (NUMBER_OF_TILES_X-1) ) / NUMBER_OF_TILES_X );
        int uTileHeight = ( ( mScreenHeight + (NUMBER_OF_TILES_Y-1) ) / NUMBER_OF_TILES_Y );

        final Matrix4f viewProj = CacheBuffer.getCachedMatrix();
        Matrix4f.mul(scene.mProj, scene.mView, viewProj);
        computeTransformedBoundingBoxExtents(scene, viewProj);

        int nNumTilesVisible = 0;
        final int left = mTransparencyMeshViewport.x;
        final int right = mTransparencyMeshViewport.x + mTransparencyMeshViewport.width;
        final int top = mTransparencyMeshViewport.y;
        final int bottom = mTransparencyMeshViewport.y + mTransparencyMeshViewport.height;

        for (int nTileStartPositionX = left; nTileStartPositionX < right; nTileStartPositionX += uTileWidth)
        {
            int nTileEndPositionX = Math.min(nTileStartPositionX + uTileWidth, mScreenWidth - 1);
            for (int nTileStartPositionY = top; nTileStartPositionY < bottom; nTileStartPositionY += uTileHeight)
            {
                int nTileEndPositionY = Math.min(nTileStartPositionY + uTileHeight, mScreenHeight - 1);

                // Skip tiles that are outside the render area
                // This is only required if the tile size is a fixed size independent of resolution
                //if ( (nTileX*uTileWidth>g_fScreenWidth) || (nTileY*uTileHeight>g_fScreenHeight) ) continue;

                //
                // LINKED LIST Step 1: Store fragments into UAVs
                //

                // Clear start offset buffer to -1
//                final int dwClearDataMinusOne[1] = { 0xFFFFFFFF };
//                pd3dImmediateContext->ClearUnorderedAccessViewUint(g_pStartOffsetBufferUAV, dwClearDataMinusOne);
                gl.glClearNamedBufferData(g_pStartOffsetBuffer.getBuffer(), GLenum.GL_R32I, GLenum.GL_RED_INTEGER, GLenum.GL_INT, CacheBuffer.wrap(-1,-1,-1,-1));

                // Set scissor rect for current tile
                /*D3D11_RECT ScissorRect;
                ScissorRect.top    = (LONG)nTileStartPositionY;
                ScissorRect.left   = (LONG)nTileStartPositionX;
                ScissorRect.bottom = (LONG)nTileEndPositionY;
                ScissorRect.right  = (LONG)nTileEndPositionX;
                pd3dImmediateContext->RSSetScissorRects(1, &ScissorRect);*/
                gl.glScissor(nTileStartPositionX, nTileStartPositionY,nTileEndPositionX-nTileStartPositionX+1,nTileEndPositionY - nTileStartPositionY+1);

                // Update tile coordinates CB
                /*D3D11_MAPPED_SUBRESOURCE MappedSubResource;
                pd3dImmediateContext->Map(g_pTileCoordinatesCB, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedSubResource );
                ((TILE_COORDINATES_CONSTANT_BUFFER_STRUCT *)MappedSubResource.pData)->vRectangleCoordinates =
                    D3DXVECTOR4( (float)ScissorRect.left, (float)ScissorRect.top, (float)ScissorRect.right, (float)ScissorRect.bottom);
                ((TILE_COORDINATES_CONSTANT_BUFFER_STRUCT *)MappedSubResource.pData)->vTileSize =
                    D3DXVECTOR4( (float)uTileWidth, (float)uTileHeight, 0, 0);
                pd3dImmediateContext->Unmap(g_pTileCoordinatesCB, 0);*/
                ByteBuffer bytes = CacheBuffer.getCachedByteBuffer(Vector4f.SIZE * 2);
                bytes.putFloat(nTileStartPositionX);
                bytes.putFloat(nTileStartPositionY);
                bytes.putFloat(nTileEndPositionX-nTileStartPositionX+1);
                bytes.putFloat(nTileEndPositionY - nTileStartPositionY+1);
                bytes.putFloat(uTileWidth);
                bytes.putFloat(uTileHeight);
                bytes.putFloat(0);
                bytes.putFloat(0);
                bytes.flip();
                g_pTileCoordinatesCB.update(0, bytes);

                // Set render target, depth buffer and UAVs
                /*pRTV[0] = NULL;
                pUAV[0] = g_pStartOffsetBufferUAV;
                pUAV[1] = g_pFragmentAndLinkStructuredBufferUAV;
                pUAV[2] = NULL;
                pUAV[3] = NULL;
                pd3dImmediateContext->OMSetRenderTargetsAndUnorderedAccessViews(1, pRTV, g_pDepthStencilTextureDSV,
                        1, 4, pUAV, pUAVCounters );*/
                gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 0, g_pStartOffsetBuffer.getBuffer());
                gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 1, g_pFragmentAndLinkStructuredBuffer.getBuffer());

                // Set shaders
               /* pd3dImmediateContext->VSSetShader( g_pMainVS, NULL, 0 );
                pd3dImmediateContext->PSSetShader( g_pStoreFragmentsPS_LinkedList, NULL, 0 );*/
                g_pStoreFragmentsPS_LinkedList.enable();

                // Set stencil buffer to increment for each fragment
//                pd3dImmediateContext->OMSetDepthStencilState(g_pDepthTestEnabledNoDepthWritesStencilWriteIncrementDSS, 0x00);
                g_pDepthTestEnabledNoDepthWritesStencilWriteIncrementDSS.run();

                // Disable color writes
//                pd3dImmediateContext->OMSetBlendState(g_pColorWritesOff, 0, 0xffffffff);
                gl.glColorMask(false, false, false, false);

                // Bind the mesh constant buffer at slot 1 for all stages
                /*pBuffers[0] = g_pTransparentMeshCB;
                pBuffers[1] = g_pTileCoordinatesCB;
                pd3dImmediateContext->VSSetConstantBuffers( 1, 1, pBuffers );
                pd3dImmediateContext->PSSetConstantBuffers( 1, 2, pBuffers );
                pd3dImmediateContext->CSSetConstantBuffers( 1, 2, pBuffers );*/
                gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, g_pTransparentMeshCB.getBuffer());
                gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 1, g_pTransparentMeshCB.getBuffer());

                // Calculate off-centered projection matrix corresponding to current tile
                /*D3DXMATRIX mTileProjection;
                CalculateOffCenteredProjectionMatrixFrom2DRectangle(&mTileProjection, g_fScreenWidth, g_fScreenHeight,
                        g_Camera.GetNearClip(), g_Camera.GetFarClip(),
                        D3DX_PI/4, ScissorRect);

                // Calculate combined WVP transformation matrix for current tile
                D3DXMATRIX TransMatrixTile = mWorld * (*g_Camera.GetViewMatrix()) * mTileProjection;

                // Set bounding box status: this updates the FrameInfluenceOffset member
                // of all sub-meshes with visibility info
                PerformBoundingBoxCheck(&g_TransparentMesh[g_nCurrentModel], &TransMatrixTile);

                // Debug info
                if (g_TransparentMesh[g_nCurrentModel].GetMesh(0)->FrameInfluenceOffset>0) g_nNumTilesVisible++;*/

                // Render transparent model with bounding box testing on
//                RenderModel(pd3dImmediateContext, &g_TransparentMesh[g_nCurrentModel], true);  todo RenderScenes
                gl.glColorMask(true, true, true, true);
                //
                // LINKED LIST Step 2: Sorting and displaying pass
                //

                /*if (g_bMSAAResolveDuringSort && g_MSAASampleDesc.Count>1) todo We didn't support the MSAA
                {
                    // Set render target to back buffer directly since we will also resolve on the fly
                    //pRTV[0] = DXUTGetD3D11RenderTargetView();

                    // Set render target to resolved render target since we will also resolve on the fly
                    pRTV[0] = g_pMainRenderTargetTextureResolvedRTV;
                    pUAV[0] = NULL;
                    pUAV[1] = NULL;
                    pUAV[2] = NULL;
                    pUAV[3] = NULL;
                    pd3dImmediateContext->OMSetRenderTargetsAndUnorderedAccessViews(1, pRTV, NULL, 1, 4,
                            pUAV, pUAVCounters);

                    // No depthstencil test since no depth buffer is bound
                    pd3dImmediateContext->OMSetDepthStencilState(g_pDepthTestDisabledDSS, 0x00);

                    // Set shaders
                    pd3dImmediateContext->VSSetShader( g_pVSPassThrough, NULL, 0 );
                    pd3dImmediateContext->PSSetShader( g_pRenderFragmentsWithResolvePS_LinkedList, NULL, 0 );

                    // Set shader resources
                    pSRV[0] = g_pStartOffsetBufferSRV;
                    pSRV[1] = g_pFragmentAndLinkStructuredBufferSRV;
                    pSRV[2] = NULL;
                    pSRV[3] = g_pCopyOfMainRenderTargetTextureSRV;
                    pd3dImmediateContext->PSSetShaderResources(0, 4, pSRV);

                    // Disable blending (background color comes from a copy
                    // of the main render target bound as texture)
                    pd3dImmediateContext->OMSetBlendState(g_pBlendStateNoBlend, 0, 0xffffffff);

                    // Set up fullscreen quad rendering
                    stride = sizeof( SIMPLEVERTEX );
                    offset = 0;
                    pd3dImmediateContext->IASetVertexBuffers( 0, 1, &g_pFullscreenQuadVB, &stride, &offset );
                    pd3dImmediateContext->IASetPrimitiveTopology( D3D11_PRIMITIVE_TOPOLOGY_TRIANGLESTRIP);
                    pd3dImmediateContext->IASetInputLayout( g_pSimpleVertexLayout );

                    // Draw fullscreen quad
                    pd3dImmediateContext->Draw( 4, 0);
                }
                else*/
                {
                    // Set render target, depth buffer and UAVs (no UAVs are used during the resolve pass)
                    /*pRTV[0] = g_pMainRenderTargetTextureRTV;
                    pUAV[0] = NULL;
                    pUAV[1] = NULL;
                    pUAV[2] = NULL;
                    pUAV[3] = NULL;
                    pd3dImmediateContext->OMSetRenderTargetsAndUnorderedAccessViews(1, pRTV, g_pDepthStencilTextureDSV,
                            1, 4, pUAV, pUAVCounters);*/
                    mSceneRenderer.setOutputRenderTaget();

                    // Set stencil pass to pass if stencil value is above 0
//                    pd3dImmediateContext->OMSetDepthStencilState(g_pDepthTestDisabledStencilTestLessDSS, 0x00);
                    g_pDepthTestDisabledStencilTestLessDSS.run();

                    // Set shaders
                    /*pd3dImmediateContext->VSSetShader( g_pVSPassThrough, NULL, 0 );
                    pd3dImmediateContext->PSSetShader( g_pRenderFragmentsPS_LinkedList, NULL, 0 );*/

                    g_pRenderFragmentsPS_LinkedList.enable();

                    // Set shader resources
                    /*pSRV[0] = g_pStartOffsetBufferSRV;  todo binding Shader Resource Views
                    pSRV[1] = g_pFragmentAndLinkStructuredBufferSRV;
                    pSRV[2] = NULL;
                    pSRV[3] = g_pCopyOfMainRenderTargetTextureSRV;
                    pd3dImmediateContext->PSSetShaderResources(0, 4, pSRV);*/

                    // Disable blending (background color comes from a copy of the back buffer bound as texture)
//                    pd3dImmediateContext->OMSetBlendState(g_pBlendStateNoBlend, 0, 0xffffffff);
                    gl.glDisable(GLenum.GL_BLEND);


                    // Set up fullscreen quad rendering
                    /*stride = sizeof( SIMPLEVERTEX );
                    offset = 0;
                    pd3dImmediateContext->IASetVertexBuffers( 0, 1, &g_pFullscreenQuadVB, &stride, &offset );
                    pd3dImmediateContext->IASetPrimitiveTopology( D3D11_PRIMITIVE_TOPOLOGY_TRIANGLESTRIP);
                    pd3dImmediateContext->IASetInputLayout( g_pSimpleVertexLayout );

                    // Draw fullscreen quad
                    pd3dImmediateContext->Draw( 4, 0);*/

                    gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);  // Render Quad
                }

                // To avoid debug error messages
//                pSRV[0] = NULL;
//                pSRV[1] = NULL;
//                pSRV[2] = NULL;
//                pd3dImmediateContext->PSSetShaderResources(0, 3, pSRV);
            }
        }

        CacheBuffer.free(viewProj);
    }

    private void computeTransformedBoundingBoxExtents(Scene scene, Matrix4f viewProj){
        final int numMeshes = scene.mTransparencyMeshes.size();

        mTransparencyMeshAABBWorld.init();
        for(int meshIdx = 0; meshIdx < numMeshes; meshIdx++){
            mTransparencyMeshAABBWorld.expandBy(scene.mExpandMeshes.get(meshIdx).mAABB);
        }

        BoundingBox.transform(viewProj, mTransparencyMeshAABBWorld, mTransparencyMeshAABBWorld);

        float minX = mTransparencyMeshAABBWorld._min.x * 0.5f + 0.5f;
        float maxX = mTransparencyMeshAABBWorld._max.x * 0.5f + 0.5f;
        float minY = mTransparencyMeshAABBWorld._min.y * 0.5f + 0.5f;
        float maxY = mTransparencyMeshAABBWorld._max.y * 0.5f + 0.5f;
        float minZ = mTransparencyMeshAABBWorld._min.z * 0.5f + 0.5f;
        float maxZ = mTransparencyMeshAABBWorld._max.z * 0.5f + 0.5f;

        if(minX > 1 || maxX < 0 || minY > 1 || maxY < 0 || minZ > 1 || maxZ < 0){
            // The objects culled by the camera.
        }

        if(minZ < 0){
            // The camera inside the object
            // Special case to make bounding box extent cover the whole screen if the bounding
            // box at least partially covers the front clip plane
            mTransparencyMeshViewport.set(0,0, mScreenWidth, mScreenHeight);
        }else{
            mTransparencyMeshViewport.x = (int) (minX * mScreenWidth + 0.5f);
            mTransparencyMeshViewport.y = (int) (minY * mScreenHeight + 0.5f);
            mTransparencyMeshViewport.width = (int) ((maxX - minX) * mScreenWidth + 0.5f);
            mTransparencyMeshViewport.height = (int) ((maxY - minY) * mScreenHeight + 0.5f);
        }
    }

    @Override
    public void dispose() {

    }

    private static final class PER_MESH_CONSTANT_BUFFER_STRUCT{
        static final int SIZE = Matrix4f.SIZE * 2 + Vector4f.SIZE;

        final Matrix4f    mWorld = new Matrix4f();
        final Matrix4f    mWorldViewProjection = new Matrix4f();
        final Vector4f    vMeshColor = new Vector4f();
    }
}
