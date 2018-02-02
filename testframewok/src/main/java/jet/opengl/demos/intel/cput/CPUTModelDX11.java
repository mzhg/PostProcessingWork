package jet.opengl.demos.intel.cput;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;
import java.nio.ByteBuffer;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/11/13.
 */

public class CPUTModelDX11 extends CPUTModel {
    private static final Vector3f g_DefaultLightDir = new Vector3f(0.7f, -0.5f, -0.05f);
    static {
        g_DefaultLightDir.normalise();
    }
    protected BufferGL      mpModelConstantBuffer;
    private CPUTModelConstantBuffer constantBuffer = new CPUTModelConstantBuffer();

    /**
     * Load the set file definition of this object.<ul>
     * <li> 1. Parse the block of name/parent/transform info for model block
     * <li> 2. Load the model's binary payload (i.e., the meshes)
     * <li> 3. Assert the # of meshes matches # of materials
     * <li> 4. Load each mesh's material
     </ul>
     * @param pBlock
     * @param pMasterModel
     * @return
     * @throws IOException
     */
    @Override
    public int LoadModel(CPUTConfigBlock pBlock, CPUTModel pMasterModel) throws IOException {
        /*CPUTResult result = CPUT_SUCCESS;*/
        CPUTAssetLibraryDX11 pAssetLibrary = (CPUTAssetLibraryDX11)CPUTAssetLibrary.GetAssetLibrary();
        final String modelSuffix = /*ptoc(this)*/"CPUTModelDX11";

        // set the model's name
        mName = pBlock.GetValueByName("name").ValueAsString();
        mName = mName + ".mdl";

        // resolve the full path name
        String modelLocation;
        String resolvedPathAndFile;
        modelLocation = ((CPUTAssetLibraryDX11)CPUTAssetLibrary.GetAssetLibrary()).GetModelDirectory();
        modelLocation = modelLocation+mName;
        /*CPUTOSServices::GetOSServices()->ResolveAbsolutePathAndFilename(modelLocation, &resolvedPathAndFile);*/
        resolvedPathAndFile = modelLocation;

        // Get the parent ID.  Note: the caller will use this to set the parent.
        int parentID = -1;

        try {
            parentID = pBlock.GetValueByName(_L("parent")).ValueAsInt();
        } catch (NumberFormatException e) {
            System.err.println("Error occured when loading the model: " + resolvedPathAndFile);
            e.printStackTrace();
        }

        LoadParentMatrixFromParameterBlock( pBlock );

        // Get the bounding box information
//        float3 center(0.0f), half(0.0f);
        float[] center = new float[3];
        float[] half = new float[3];
        pBlock.GetValueByName(_L("BoundingBoxCenter")).ValueAsFloatArray(center, 0,3);
        pBlock.GetValueByName(_L("BoundingBoxHalf")).ValueAsFloatArray(half, 0,3);
        mBoundingBoxCenterObjectSpace.load(center, 0);
        mBoundingBoxHalfObjectSpace.load(half, 0);

        // the # of meshes in the binary file better match the number of meshes in the .set file definition
        mMeshCount = pBlock.GetValueByName(_L("meshcount")).ValueAsInt();
        mpMesh     = new CPUTMesh[mMeshCount];
        mpMaterial = new CPUTMaterial[mMeshCount];
        /*memset( mpMaterial, 0, mMeshCount * sizeof(CPUTMaterial*) );*/

        // get the material names, load them, and match them up with each mesh
        String materialName;
//        char pNumber[4];
        String materialValueName;

        CPUTModelDX11 pMasterModelDX = (CPUTModelDX11)pMasterModel;

        for(int ii=0; ii<mMeshCount; ii++)
        {
            if(pMasterModelDX != null)
            {
                // Reference the master model's mesh.  Don't create a new one.
                mpMesh[ii] = pMasterModelDX.mpMesh[ii];
//                mpMesh[ii]->AddRef();
            }
            else
            {
                mpMesh[ii] = new CPUTMeshDX11();
            }
        }
        if( pMasterModelDX  == null)
        {
            // Not a clone/instance.  So, load the model's binary payload (i.e., vertex and index buffers)
            // TODO: Change to use GetModel()
            LoadModelPayload(resolvedPathAndFile);
            /*ASSERT( CPUTSUCCESS(result), _L("Failed loading model") );*/
        }
        // Create the model constant buffer.
        /*HRESULT hr;
        D3D11_BUFFER_DESC bd = {0};
        bd.ByteWidth = sizeof(CPUTModelConstantBuffer);
        bd.BindFlags = D3D11_BIND_CONSTANT_BUFFER;
        bd.Usage = D3D11_USAGE_DYNAMIC;
        bd.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE;
        hr = (CPUT_DX11::GetDevice())->CreateBuffer( &bd, NULL, &mpModelConstantBuffer );
        ASSERT( !FAILED( hr ), _L("Error creating constant buffer.") );
        CPUTSetDebugName( mpModelConstantBuffer, _L("Model Constant buffer") );
        cString pModelSuffix = ptoc(this);
        cString name = _L("#cbPerModelValues") + pModelSuffix;
        CPUTBufferDX11 *pBuffer = new CPUTBufferDX11(name, mpModelConstantBuffer);
        pAssetLibrary->AddConstantBuffer( name, pBuffer );
        pBuffer->Release(); // We're done with it.  We added it to the library.  Release our reference.*/

        String name = "#cbPerModelValues" + modelSuffix;

        CPUTBufferDX11 pBuffer = (CPUTBufferDX11) pAssetLibrary.GetConstantBuffer(name);
        if(pBuffer == null){
            mpModelConstantBuffer = new BufferGL();
            mpModelConstantBuffer.initlize(GLenum.GL_UNIFORM_BUFFER, CPUTModelConstantBuffer.SIZE, null, GLenum.GL_STREAM_DRAW);
            mpModelConstantBuffer.unbind();
            mpModelConstantBuffer.setName("Model Constant buffer");
            pBuffer = new CPUTBufferDX11("Model Constant buffer", mpModelConstantBuffer);

            pAssetLibrary.AddConstantBuffer( name, pBuffer );
        }else{
            mpModelConstantBuffer = pBuffer.GetNativeBuffer();
        }

        String assetSetDirectoryName = pAssetLibrary.GetAssetSetDirectoryName();
        String modelDirectory        = pAssetLibrary.GetModelDirectory();
        String materialDirectory     = pAssetLibrary.GetMaterialDirectory();
        String textureDirectory      = pAssetLibrary.GetTextureDirectory();
        String shaderDirectory       = pAssetLibrary.GetShaderDirectory();
        String fontDirectory         = pAssetLibrary.GetFontDirectory();
        String up2MediaDirName       = assetSetDirectoryName + _L("..\\");
        pAssetLibrary.SetMediaDirectoryName( up2MediaDirName );
        mpShadowCastMaterial = pAssetLibrary.GetMaterial( _L("shadowCast"), false, modelSuffix, "" );
        pAssetLibrary.SetAssetSetDirectoryName( assetSetDirectoryName );
        pAssetLibrary.SetModelDirectoryName( modelDirectory );
        pAssetLibrary.SetMaterialDirectoryName( materialDirectory );
        pAssetLibrary.SetTextureDirectoryName( textureDirectory );
        pAssetLibrary.SetShaderDirectoryName( shaderDirectory );
        pAssetLibrary.SetFontDirectoryName( fontDirectory );

        for(int ii=0; ii<mMeshCount; ii++)
        {
            // get the right material number ('material0', 'material1', 'material2', etc)
            /*materialValueName = _L("material");
            _itoa_s(ii, pNumber, 4, 10);
            materialValueName.append(s2ws(pNumber));*/
            materialValueName = "material" + ii;
            materialName = pBlock.GetValueByName(materialValueName).ValueAsString();

            // Get/load material for this mesh
            String meshSuffix  = Integer.toString(ii);
            CPUTMaterialDX11 pMaterial = (CPUTMaterialDX11)pAssetLibrary.GetMaterial(materialName, false, modelSuffix, meshSuffix);
            /*ASSERT( pMaterial, _L("Couldn't find material.") );*/
            if(pMaterial == null)
                throw new IllegalArgumentException("Couldn't find material.");

            // set the material on this mesh
            // TODO: Model owns the materials.  That allows different models to share meshes (aka instancing) that have different materials
            SetMaterial(ii, pMaterial);

            // Release the extra refcount we're holding from the GetMaterial operation earlier
            // now the asset library, and this model have the only refcounts on that material
            /*pMaterial.dispose();*/

            // Create two ID3D11InputLayout objects, one for each material.
            mpMesh[ii].BindVertexShaderLayout( mpMaterial[ii], mpShadowCastMaterial);
            // mpShadowCastMaterial->Release()
        }
        return parentID;
    }

    /** Return the mesh at the given index (cast to the GFX api version of CPUTMeshDX11) */
    @Override
    public CPUTMeshDX11  GetMesh(int index){
        return ( 0==mMeshCount || index > mMeshCount) ? null : (CPUTMeshDX11)mpMesh[index];
    }

    /** Set the render state before drawing this object */
    public void SetRenderStates(CPUTRenderParameters renderParams){
        // Should update the constant buffer only when the model moves.
        // But, requires individual, per-model constant buffers
//        ID3D11DeviceContext *pContext  = ((CPUTRenderParametersDX*)&renderParams)->mpContext;

        CPUTModelConstantBuffer pCb=constantBuffer;

        ReadableVector3f gLightDir;
        if(renderParams.mpShadowCamera != null)
        {
            Vector3f vec = new Vector3f();
            renderParams.mpShadowCamera.GetLook(vec);
            gLightDir = vec;
        }
        else
        {
            // default position
            gLightDir = /*normalize( float3(0.7f, -0.5f, -0.05f) )*/g_DefaultLightDir;
        }

        // update parameters of constant buffer
        /*D3D11_MAPPED_SUBRESOURCE mapInfo;
        pContext->Map( mpModelConstantBuffer, 0, D3D11_MAP_WRITE_DISCARD, 0, &mapInfo );*/
        {
            // TODO: remove construction of XMM type
            Matrix4f world = GetWorldMatrix();
//            XMVECTOR    determinant = XMMatrixDeterminant(world);
            /*CPUTCamera *pCamera     = gpSample.GetCamera();*/
            CPUTCamera pCamera  = renderParams.mpCamera;

            /*XMMATRIX    view((float*)pCamera->GetViewMatrix());
            XMMATRIX    projection((float*)pCamera->GetProjectionMatrix());
            float      *pCameraPos = (float*)&pCamera->GetPosition();
            XMVECTOR    cameraPos = XMLoadFloat3(&XMFLOAT3( pCameraPos[0], pCameraPos[1], pCameraPos[2] ));
            pCb = (CPUTModelConstantBuffer*)mapInfo.pData;
            pCb->World               = world;
            pCb->ViewProjection      = view  *projection;
            pCb->WorldViewProjection = world  *pCb->ViewProjection;
            pCb->InverseWorld        = XMMatrixInverse(&determinant, XMMatrixTranspose(world));*/
            pCb.World.load(world);
            pCb.ViewProjection.load(pCamera.GetViewProjMatrix());
            Matrix4f.mul(pCb.ViewProjection, world, pCb.WorldViewProjection);
            Matrix4f.invert(world, pCb.InverseWorld);

            // pCb->LightDirection      = XMVector3Transform(gLightDir, pCb->InverseWorld );
            // pCb->EyePosition         = XMVector3Transform(cameraPos, pCb->InverseWorld );
            // TODO: Tell the lights to set their render states

            /*XMVECTOR lightDirection = XMLoadFloat3(&XMFLOAT3( gLightDir.x, gLightDir.y, gLightDir.z ));
            pCb->LightDirection      = XMVector3Normalize(lightDirection);
            pCb->EyePosition         = cameraPos;
            float *bbCWS = (float*)&mBoundingBoxCenterWorldSpace;
            float *bbHWS = (float*)&mBoundingBoxHalfWorldSpace;
            float *bbCOS = (float*)&mBoundingBoxCenterObjectSpace;
            float *bbHOS = (float*)&mBoundingBoxHalfObjectSpace;
            pCb->BoundingBoxCenterWorldSpace  = XMLoadFloat3(&XMFLOAT3( bbCWS[0], bbCWS[1], bbCWS[2] )); ;
            pCb->BoundingBoxHalfWorldSpace    = XMLoadFloat3(&XMFLOAT3( bbHWS[0], bbHWS[1], bbHWS[2] )); ;
            pCb->BoundingBoxCenterObjectSpace = XMLoadFloat3(&XMFLOAT3( bbCOS[0], bbCOS[1], bbCOS[2] )); ;
            pCb->BoundingBoxHalfObjectSpace   = XMLoadFloat3(&XMFLOAT3( bbHOS[0], bbHOS[1], bbHOS[2] )); ;*/

            if(renderParams.mpShadowCamera != null){
                Matrix4f.decompseRigidMatrix(renderParams.mpShadowCamera.GetViewMatrix(), null, null, null, pCb.LightDirection);
                pCb.LightDirection.scale(-1);
            }else{
                pCb.LightDirection.set(g_DefaultLightDir);
                pCb.LightDirection.normalise();
            }

            pCamera.GetPosition(pCb.EyePosition);
            pCb.BoundingBoxCenterWorldSpace.set(mBoundingBoxCenterWorldSpace);
            pCb.BoundingBoxHalfWorldSpace.set(mBoundingBoxHalfWorldSpace);
            pCb.BoundingBoxCenterObjectSpace.set(mBoundingBoxCenterObjectSpace);
            pCb.BoundingBoxHalfObjectSpace.set(mBoundingBoxHalfObjectSpace);

            // Shadow camera
//            XMMATRIX    shadowView, shadowProjection;
            CPUTCamera pShadowCamera =  renderParams.mpShadowCamera;
            if( pShadowCamera != null)
            {
                /*shadowView = XMMATRIX((float*)pShadowCamera->GetViewMatrix());
                shadowProjection = XMMATRIX((float*)pShadowCamera->GetProjectionMatrix());
                pCb->LightWorldViewProjection = world * shadowView * shadowProjection;*/
                Matrix4f.mul(pShadowCamera.GetViewProjMatrix(), world, pCb.LightWorldViewProjection);
            }
        }
//        pContext->Unmap(mpModelConstantBuffer,0);
        ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(CPUTModelConstantBuffer.SIZE);
        pCb.store(buffer).flip();
        mpModelConstantBuffer.update(0, buffer);
        GLCheck.checkError();
    }

    /** Render - render this model (only) */
    public void Render(CPUTRenderParameters renderParams){
        /*CPUTRenderParametersDX *pParams = (CPUTRenderParametersDX*)&renderParams;*/
        CPUTCamera pCamera = renderParams.mpCamera;

        if( !renderParams.mDrawModels ) { return; }

        // TODO: add world-space bounding box to model so we don't need to do that work every frame
        if( !renderParams.mRenderOnlyVisibleModels || pCamera == null || pCamera.isCenterExtentVisible( mBoundingBoxCenterWorldSpace, mBoundingBoxHalfWorldSpace ) )
        {
            // loop over all meshes in this model and draw them
            for(int ii=0; ii<mMeshCount; ii++)
            {
                CPUTMaterialDX11 pMaterial = (CPUTMaterialDX11)(mpMaterial[ii]);
                pMaterial.SetRenderStates(renderParams);

                // We would like to set the model's render states only once (and then iterate over materials)
                // But, the material resource lists leave holes for per-model resources (e.g., constant buffers)
                // We need to 'fixup' the bound resources.  The material sets some to 0, and the model overwrites them with the correct values.
                SetRenderStates(renderParams);

                // Potentially need to use a different vertex-layout object!
                mpMesh[ii].Draw(renderParams, this);
            }
        }
    }
    public void RenderShadow(CPUTRenderParameters renderParams){
        CPUTCamera pCamera = renderParams.mpCamera;
        if( !renderParams.mDrawModels ) { return; }

        // TODO: add world-space bounding box to model so we don't need to do that work every frame
        if( !renderParams.mRenderOnlyVisibleModels || pCamera == null || pCamera.isCenterExtentVisible( mBoundingBoxCenterWorldSpace, mBoundingBoxHalfWorldSpace ) )
        {
            // loop over all meshes in this model and draw them
            for(int ii=0; ii<mMeshCount; ii++)
            {
                CPUTMaterialDX11 pMaterial = (CPUTMaterialDX11)(mpShadowCastMaterial);
                pMaterial.SetRenderStates(renderParams);

                // We would like to set the model's render states only once (and then iterate over materials)
                // But, the material resource lists leave holes for per-model resources (e.g., constant buffers)
                // We need to 'fixup' the bound resources.  The material sets some to 0, and the model overwrites them with the correct values.
                SetRenderStates(renderParams);

                // Potentially need to use a different vertex-layout object!
                mpMesh[ii].DrawShadow(renderParams, this);
            }
        }
    }
    /** Render this using AVSM buffers and standard shadow map to determine shadow amount */
    public void RenderAVSMShadowed(CPUTRenderParameters renderParams){
        CPUTCamera pCamera = renderParams.mpCamera;
        if( !renderParams.mDrawModels ) { return; }

        // TODO: add world-space bounding box to model so we don't need to do that work every frame
        if( !renderParams.mRenderOnlyVisibleModels || pCamera == null || pCamera.isCenterExtentVisible( mBoundingBoxCenterWorldSpace, mBoundingBoxHalfWorldSpace ) )
        {
            // loop over all meshes in this model and draw them
            for(int ii=0; ii<mMeshCount; ii++)
            {
                CPUTMaterialDX11 pMaterial = (CPUTMaterialDX11)(mpMaterial[ii]);
                pMaterial.SetRenderStates(renderParams);

                // We would like to set the model's render states only once (and then iterate over materials)
                // But, the material resource lists leave holes for per-model resources (e.g., constant buffers)
                // We need to 'fixup' the bound resources.  The material sets some to 0, and the model overwrites them with the correct values.
                SetRenderStates(renderParams);

                // Potentially need to use a different vertex-layout object!
                mpMesh[ii].Draw(renderParams, this);
            }
        }
    }

    public void SetMaterial(int ii, CPUTMaterial pMaterial){
        super.SetMaterial(ii, pMaterial);

        // Can't bind the layout if we haven't loaded the mesh yet.
        CPUTMeshDX11 pMesh = (CPUTMeshDX11)mpMesh[ii];
        D3D11_INPUT_ELEMENT_DESC[] pDesc = pMesh.GetLayoutDescription();
        if( pDesc!=null )
        {
            pMesh.BindVertexShaderLayout(pMaterial, mpMaterial[ii]);
        }
    }

    void          DrawBoundingBox(CPUTRenderParameters renderParams){

    }

    @Override
    public void dispose() {
        SAFE_RELEASE(mpModelConstantBuffer);
    }
}
