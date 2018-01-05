package jet.opengl.demos.intel.va;

import org.lwjgl.util.vector.Matrix4f;

import java.util.List;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLStateTracker;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.common.RasterizerState;
import jet.opengl.postprocessing.texture.Texture2D;

/**
 * Created by mazhen'gui on 2017/11/21.
 */

final class VaRenderMeshManagerDX11 extends VaRenderMeshManager implements VaDirectXNotifyTarget{
    private VaDirectXConstantsBuffer m_constantsBuffer = new VaDirectXConstantsBuffer(RenderMeshConstants.SIZE);
    private final RenderMeshConstants m_constants = new RenderMeshConstants();
    private final RasterizerState  m_rasterizerState = new RasterizerState();
    private int m_storeageIndex;

    protected VaRenderMeshManagerDX11(VaConstructorParamsBase params){
        VaDirectXCore.helperInitlize(this);
    }

    @Override
    public void setStorageIndex(int index) {
        m_storeageIndex = index;
    }

    @Override
    public int getStorageIndex() {
        return m_storeageIndex;
    }

    @Override
    public void OnDeviceCreated() {
        m_constantsBuffer.Create();
    }

    @Override
    public void OnDeviceDestroyed() {
        m_constantsBuffer.dispose();
    }

    @Override
    public void Draw(VaDrawContext drawContext, VaRenderMeshDrawList list) {
        VaRenderDeviceContextDX11 apiContext = (VaRenderDeviceContextDX11) drawContext.APIContext;
//        ID3D11DeviceContext * dx11Context = apiContext->GetDXImmediateContext( );

        assert( drawContext.GetRenderingGlobalsUpdated( ) );    if( !drawContext.GetRenderingGlobalsUpdated( ) ) return;

        VaSimpleShadowMapDX11 simpleShadowMapDX11 = null;
        if( drawContext.SimpleShadowMap != null )
            simpleShadowMapDX11 = (VaSimpleShadowMapDX11) drawContext.SimpleShadowMap;

        // make sure we're not overwriting anything else
        VaDirectXTools.AssertSetToD3DContextAllShaderTypes( /*dx11Context,*/ ( Texture2D ) null, VaShaderDefine.RENDERMESH_TEXTURE_SLOT0 );
        VaDirectXTools.AssertSetToD3DContextAllShaderTypes( /*dx11Context,*/ ( Texture2D ) null, VaShaderDefine.RENDERMESH_TEXTURE_SLOT1 );
        VaDirectXTools.AssertSetToD3DContextAllShaderTypes( /*dx11Context,*/ ( Texture2D ) null, VaShaderDefine.RENDERMESH_TEXTURE_SLOT2 );
        VaDirectXTools.AssertSetToD3DContextAllShaderTypes( /*dx11Context,*/ ( Texture2D ) null, VaShaderDefine.RENDERMESH_TEXTURE_SLOT3 );
        VaDirectXTools.AssertSetToD3DContextAllShaderTypes( /*dx11Context,*/ ( Texture2D ) null, VaShaderDefine.RENDERMESH_TEXTURE_SLOT4 );
        VaDirectXTools.AssertSetToD3DContextAllShaderTypes( /*dx11Context,*/ ( Texture2D ) null, VaShaderDefine.RENDERMESH_TEXTURE_SLOT5 );
        VaDirectXTools.AssertSetToD3DContextAllShaderTypes( /*dx11Context,*/ ( BufferGL ) null, VaShaderDefine.RENDERMESH_CONSTANTS_BUFFERSLOT );
        VaDirectXTools.AssertSetToD3DContextAllShaderTypes( /*dx11Context,*/ ( BufferGL ) null, VaShaderDefine.RENDERMESHMATERIAL_CONSTANTS_BUFFERSLOT );

        // set our main constant buffer
        m_constantsBuffer.SetToD3DContextAllShaderTypes( /*dx11Context,*/ VaShaderDefine.RENDERMESH_CONSTANTS_BUFFERSLOT );
        final GLStateTracker stateTracker = GLStateTracker.getInstance();

        // Global API states
//        dx11Context->IASetPrimitiveTopology( D3D11_PRIMITIVE_TOPOLOGY_TRIANGLELIST );

//        float blendFactor[4] = { 0, 0, 0, 0 };
        if( drawContext.PassType == VaRenderPassType.ForwardTransparent  )
        {
//            dx11Context->OMSetBlendState( vaDirectXTools::GetBS_AlphaBlend( ), blendFactor, 0xFFFFFFFF );
//            dx11Context->OMSetDepthStencilState( (drawContext.Camera.GetUseReversedZ())?( vaDirectXTools::GetDSS_DepthEnabledG_NoDepthWrite( ) ):( vaDirectXTools::GetDSS_DepthEnabledL_NoDepthWrite( ) ), 0 );
            stateTracker.setBlendState(VaDirectXTools.GetBS_AlphaBlend());
            stateTracker.setDepthStencilState((drawContext.Camera.GetUseReversedZ())?( VaDirectXTools.GetDSS_DepthEnabledG_NoDepthWrite( ) ):( VaDirectXTools.GetDSS_DepthEnabledL_NoDepthWrite( ) ));
        }
        else if( drawContext.PassType == VaRenderPassType.ForwardDebugWireframe )
        {
            stateTracker.setBlendState( VaDirectXTools.GetBS_Opaque( )/*, blendFactor, 0xFFFFFFFF*/ );
            stateTracker.setDepthStencilState( (drawContext.Camera.GetUseReversedZ())?( VaDirectXTools.GetDSS_DepthEnabledGE_NoDepthWrite( ) ):( VaDirectXTools.GetDSS_DepthEnabledLE_NoDepthWrite( ) )/*, 0*/ );
        }
        else // all other
        {
            stateTracker.setBlendState( VaDirectXTools.GetBS_Opaque( )/*, blendFactor, 0xFFFFFFFF*/ );
            stateTracker.setDepthStencilState( (drawContext.Camera.GetUseReversedZ())?( VaDirectXTools.GetDSS_DepthEnabledG_DepthWrite( ) ):( VaDirectXTools.GetDSS_DepthEnabledL_DepthWrite( ) )/*, 0*/ );
        }

        // should sort by mesh, then iterate by meshes -> subparts -> draw entries

        for( int i = 0; i < list.Count(); i++ )
        {
            VaRenderMeshDrawList.Entry  entry = list.get(i);
            VaRenderMesh mesh = entry.Mesh/*.get()*/;

            if( mesh.GetTriangleMesh( ) == null )
            {
                assert( false );
                continue;
            }

            // we can only render our own meshes
            assert( /*static_cast<vaRenderMeshManagerDX11*>( &*/mesh.GetManager()/*)*/ == this );

            List<VaRenderMesh.SubPart> parts = mesh.GetParts();

//            StandardTriangleMeshDX11 * triMeshDX11 = mesh.GetTriangleMesh( )->SafeCast<StandardTriangleMeshDX11 *>();
            VaTriangleMeshDX11 triMeshDX11 = mesh.GetTriangleMesh().SafeCast();
            triMeshDX11.UpdateAndSetToD3DContext( /*dx11Context*/ );

            // update per-instance constants
            {
                //const Tree::Settings & settings = m_tree.GetSettings( );
                RenderMeshConstants consts = m_constants;

                /*consts.World = vaMatrix4x4::FromQuaternion( entry.Rotation );
                consts.World.SetTranslation( entry.Translation );
                consts.World = vaMatrix4x4::Scaling( entry.Scale ) * consts.World;
                consts.Color = entry.Color;*/
                entry.Rotation.toMatrix(consts.World);
                consts.World.setColumn(3, entry.Translation.x, entry.Translation.y, entry.Translation.z, 1);
                consts.World.scale(entry.Scale);
                consts.Color.set(entry.Color);

                if( drawContext.PassType != VaRenderPassType.GenerateShadowmap )
                {
//                    consts.WorldView = consts.World * drawContext.Camera.GetViewMatrix( );
                    Matrix4f.mul(drawContext.Camera.GetViewMatrix( ), consts.World, consts.WorldView);
                    consts.ShadowWorldViewProj /*= vaMatrix4x4::Identity*/.setIdentity();
                }
                else
                {
                    assert( drawContext.SimpleShadowMap != null );
                    /*consts.WorldView = consts.World * drawContext.SimpleShadowMap->GetViewMatrix( );
                    consts.ShadowWorldViewProj = consts.World * drawContext.SimpleShadowMap->GetViewProjMatrix( );*/
                    Matrix4f.mul(drawContext.SimpleShadowMap.GetViewMatrix( ), consts.World, consts.WorldView);
                    Matrix4f.mul(drawContext.SimpleShadowMap.GetViewProjMatrix( ), consts.World, consts.ShadowWorldViewProj);
                }
                consts.Color.set(entry.Color);

                m_constantsBuffer.Update( /*dx11Context,*/ consts );
            }

            // draw subparts!
            for( int subPartIndex = 0; subPartIndex < parts.size(); subPartIndex++ )
            {
//             if( ( (1 << (subPartIndex-1)) & entry.SubPartMask ) == 0 )
//                 continue;

                VaRenderMesh.SubPart subPart = parts.get(subPartIndex);
                assert( (subPart.IndexStart + subPart.IndexCount) <= triMeshDX11.GetIndexCount() );

                VaRenderMaterial material = subPart.Material/*.lock()*/;
                if( material == null )
                    material = VaRenderMaterialManager.GetInstance().GetDefaultMaterial( );

                VaRenderMaterial.MaterialSettings  materialSettings = material.GetSettings();

                RasterizerState rasterizerDesc = m_rasterizerState;
                rasterizerDesc.fillMode                 = (drawContext.PassType == VaRenderPassType.ForwardDebugWireframe)?(GLenum.GL_LINE):( GLenum.GL_FILL );
//                rasterizerDesc.cullMode                 = (materialSettings.FaceCull == vaFaceCull::None)?( D3D11_CULL_NONE ): ( (materialSettings.FaceCull == vaFaceCull::Front)?( D3D11_CULL_FRONT ): ( D3D11_CULL_BACK ) );
                if(materialSettings.FaceCull == VaRenderMaterial.FaceCull_None){
                    rasterizerDesc.cullFaceEnable = false;
                }else{
                    rasterizerDesc.cullFaceEnable = true;
                    rasterizerDesc.cullMode = (materialSettings.FaceCull == VaRenderMaterial.FaceCull_Front)?( GLenum.GL_FRONT ): ( GLenum.GL_BACK );
                }
                rasterizerDesc.frontCounterClockwise    = mesh.GetFrontFaceWindingOrder() == /*vaWindingOrder::CounterClockwise*/VaRenderMesh.WindingOrder_CounterClockwise;
//                rasterizerDesc.depthBias                = 0;        // if( drawContext.PassType == vaRenderPassType::GenerateShadowmap ), these will go to whatever there's in simpleShadowMapDX11
//                rasterizerDesc.depthBiasClamp           = 0;        // if( drawContext.PassType == vaRenderPassType::GenerateShadowmap ), these will go to whatever there's in simpleShadowMapDX11
//                rasterizerDesc.slopeScaledDepthBias     = 0;        // if( drawContext.PassType == vaRenderPassType::GenerateShadowmap ), these will go to whatever there's in simpleShadowMapDX11
//                rasterizerDesc.depthClipEnable          = true;
//                rasterizerDesc.scissorEnable            = false;
//                rasterizerDesc.multisampleEnable        = false;    // this comes from drawContext
//                rasterizerDesc.antialiasedLineEnable    = false;

                if( drawContext.PassType == VaRenderPassType.GenerateShadowmap )
                {
                    assert( false );
                    // update depth slope biases here
                }
//                dx11Context->RSSetState( vaDirectXTools::FindOrCreateRasterizerState( rasterizerDesc ) );
                GLStateTracker.getInstance().setRasterizerState(rasterizerDesc);

                material.UploadToAPIContext( drawContext );

//                dx11Context->DrawIndexed( subPart.IndexCount, subPart.IndexStart, 0 );
                GLFuncProviderFactory.getGLFuncProvider().glDrawElementsBaseVertex(GLenum.GL_TRIANGLES, subPart.IndexCount, GLenum.GL_UNSIGNED_INT, subPart.IndexStart, 0);
            }
        }

        // make sure nothing messed with our constant buffers and nothing uses them after
        VaDirectXTools.AssertSetToD3DContextAllShaderTypes( /*dx11Context,*/ m_constantsBuffer.GetBuffer( ), VaShaderDefine.RENDERMESH_CONSTANTS_BUFFERSLOT );

        // Reset states
        VaDirectXTools.SetToD3DContextAllShaderTypes( /*dx11Context,*/ ( Texture2D )null, VaShaderDefine.RENDERMESH_TEXTURE_SLOT0 );
        VaDirectXTools.SetToD3DContextAllShaderTypes( /*dx11Context,*/ ( Texture2D )null, VaShaderDefine.RENDERMESH_TEXTURE_SLOT1 );
        VaDirectXTools.SetToD3DContextAllShaderTypes( /*dx11Context,*/ ( Texture2D )null, VaShaderDefine.RENDERMESH_TEXTURE_SLOT2 );
        VaDirectXTools.SetToD3DContextAllShaderTypes( /*dx11Context,*/ ( Texture2D )null, VaShaderDefine.RENDERMESH_TEXTURE_SLOT3 );
        VaDirectXTools.SetToD3DContextAllShaderTypes( /*dx11Context,*/ ( Texture2D )null, VaShaderDefine.RENDERMESH_TEXTURE_SLOT4 );
        VaDirectXTools.SetToD3DContextAllShaderTypes( /*dx11Context,*/ ( Texture2D )null, VaShaderDefine.RENDERMESH_TEXTURE_SLOT5 );
        VaDirectXTools.SetToD3DContextAllShaderTypes( /*dx11Context,*/ ( BufferGL ) null, VaShaderDefine.RENDERMESH_CONSTANTS_BUFFERSLOT );
        VaDirectXTools.SetToD3DContextAllShaderTypes( /*dx11Context,*/ ( BufferGL ) null, VaShaderDefine.RENDERMESHMATERIAL_CONSTANTS_BUFFERSLOT );

        /*ID3D11ShaderResourceView * nullTextures[4] = { NULL, NULL, NULL, NULL };
        dx11Context->VSSetShader( NULL, NULL, 0 );
        dx11Context->VSSetShaderResources( 0, _countof( nullTextures ), nullTextures );
        dx11Context->PSSetShader( NULL, NULL, 0 );
        dx11Context->PSSetShaderResources( 0, _countof( nullTextures ), nullTextures );*/
        apiContext.ClearShaders();
    }
}
