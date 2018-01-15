package jet.opengl.demos.intel.cput;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgramPipeline;
import jet.opengl.postprocessing.shader.GLSLUtil;
import jet.opengl.postprocessing.shader.ProgramResources;
import jet.opengl.postprocessing.shader.ShaderProgram;
import jet.opengl.postprocessing.shader.UniformBlockProperties;
import jet.opengl.postprocessing.shader.UniformBlockType;
import jet.opengl.postprocessing.shader.UniformProperty;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.util.LogUtil;

/**
 * Created by mazhen'gui on 2017/11/13.
 */

public final class CPUTMaterialDX11 extends CPUTMaterial{
    static Object mpLastVertexShader;
    static Object mpLastPixelShader;
    static Object mpLastComputeShader;
    static Object mpLastGeometryShader;
    static Object mpLastHullShader;
    static Object mpLastDomainShader;

    static Object[] mpLastShaderViews = new Object[CPUT_MATERIAL_MAX_TEXTURE_SLOTS];

    static Object[] mpLastShaderConstantBuffers = new Object[CPUT_MATERIAL_MAX_CONSTANT_BUFFER_SLOTS];

    static Object[] mpLastComputeShaderUAVs = new Object[CPUT_MATERIAL_MAX_UAV_SLOTS];

    static Object mpLastRenderStateBlock;

    static int   mCurrentSubMaterialIndex;

    // TODO: move texture to base class.  All APIs have textures.
    ShaderProgram     mpPixelShader;
    ShaderProgram    mpComputeShader; // TODO: Do Compute Shaders belong in material?
    ShaderProgram     mpVertexShader;
    ShaderProgram   mpGeometryShader;
    ShaderProgram       mpHullShader;
    ShaderProgram     mpDomainShader;

    int                      mSubMaterialCount;
    CPUTMaterialDX11       []mpSubMaterials;

    GLSLProgramPipeline mProgram;

    final CPUTShaderParameters     mShaderParameters = new CPUTShaderParameters();
    private GLFuncProvider gl;

    public CPUTMaterialDX11(){
    }
    
    @Override
    public void LoadMaterial(String fileName, String modelSuffix, String meshSuffix) throws IOException {
//        CPUTResult result = CPUT_SUCCESS;

        mMaterialName = fileName;

        // Open/parse the file
        CPUTConfigFile file = new CPUTConfigFile();
        /*result =*/ file.LoadFile(fileName);
//        if(CPUTFAILED(result))
//        {
//            return result;
//        }

        // Make a local copy of all the parameters
        CPUTConfigBlock pBlock = file.GetBlock(0);
//        ASSERT( pBlock, _L("Error getting parameter block") );
        mConfigBlock = pBlock;

        // get necessary device and AssetLibrary pointers
//        ID3D11Device *pD3dDevice = CPUT_DX11::GetDevice();
        CPUTAssetLibraryDX11 pAssetLibrary = (CPUTAssetLibraryDX11)CPUTAssetLibrary.GetAssetLibrary();

        if( mConfigBlock.GetValueByName("MultiMaterial" ).ValueAsBool() )
        {
//            std::vector<cString> items;
            ArrayList<String> items = new ArrayList<>();

            int i = 0;
            while( true )
            {
                CPUTConfigEntry pValue = mConfigBlock.GetValueByName(  String.format( "Material%d", i ));
                if( pValue.IsValid() )
                {
                    items.add( pValue.ValueAsString() );
                }
                else
                {
                    break;
                }
                i++;
            }

            mSubMaterialCount = items.size();

            if( mSubMaterialCount > 0 )
            {
                mpSubMaterials = new CPUTMaterialDX11 [mSubMaterialCount];
                for( i = 0; i < mSubMaterialCount; i++ )
                {
                    mpSubMaterials[i] = (CPUTMaterialDX11)((CPUTAssetLibraryDX11)CPUTAssetLibrary.GetAssetLibrary()).GetMaterial( items.get(i), false, modelSuffix, meshSuffix );
                    if( mpSubMaterials[i].IsMultiMaterial() )
                    {
                        throw new IllegalArgumentException("Multi-material cannot have a sub material that is also a multi-material (hierarchy not supported at the moment)");
                    }
                }
            }
        }
        else
        {
            mSubMaterialCount = 0;
        }

        // TODO:  The following code is very repetitive.  Consider generalizing so we can call a function instead.
        // see if there are any pixel/vertex/geo shaders to load
        CPUTConfigEntry pValue;

        CPUTConfigEntry pEntryPointName, pProfileName;
        pValue   = mConfigBlock.GetValueByName("VertexShaderFile");
        if( pValue.IsValid() )
        {
//            ASSERT( !IsMultiMaterial(), L"Multi-material VertexShaderFile not yet supported" );
            if(IsMultiMaterial()){
                throw new IllegalArgumentException("Multi-material VertexShaderFile not yet supported");
            }

            pEntryPointName = mConfigBlock.GetValueByName("VertexShaderMain");
            pProfileName    = mConfigBlock.GetValueByName("VertexShaderProfile");
            mpVertexShader = pAssetLibrary.GetVertexShader(
                    pValue.ValueAsString(),
//                    pD3dDevice,
                    pEntryPointName.ValueAsString(),
                    pProfileName.ValueAsString(),
                    false
            );

            ReadShaderSamplersAndTextures( mpVertexShader/*->GetBlob()*/, mShaderParameters);
        }

        // load and store the pixel shader if it was specified
        pValue  = mConfigBlock.GetValueByName("PixelShaderFile");
        if( pValue.IsValid() )
        {
//            ASSERT( !IsMultiMaterial(), L"Multi-material PixelShaderFile not yet supported" );
            if(IsMultiMaterial()){
                throw new IllegalArgumentException("Multi-material PixelShaderFile not yet supported");
            }

            pEntryPointName = mConfigBlock.GetValueByName("PixelShaderMain");
            pProfileName    = mConfigBlock.GetValueByName("PixelShaderProfile");
            mpPixelShader = pAssetLibrary.GetPixelShader(
                    pValue.ValueAsString(),
//                    pD3dDevice,
                    pEntryPointName.ValueAsString(),
                    pProfileName.ValueAsString(),
                    false
            );
            ReadShaderSamplersAndTextures( mpPixelShader/*->GetBlob()*/, mShaderParameters );
        }

        // load and store the compute shader if it was specified
        pValue = mConfigBlock.GetValueByName("ComputeShaderFile");
        if( pValue.IsValid() )
        {
//            ASSERT( !IsMultiMaterial(), L"Multi-material ComputeShaderFile not yet supported" );
            if(IsMultiMaterial()){
                throw new IllegalArgumentException("Multi-material ComputeShaderFile not yet supported");
            }

            pEntryPointName = mConfigBlock.GetValueByName("ComputeShaderMain");
            pProfileName = mConfigBlock.GetValueByName("ComputeShaderProfile");
            mpComputeShader = pAssetLibrary.GetComputeShader(
                    pValue.ValueAsString(),
//                    pD3dDevice,
                    pEntryPointName.ValueAsString(),
                    pProfileName.ValueAsString(),
                    false
            );
            ReadShaderSamplersAndTextures( mpComputeShader/*->GetBlob()*/, mShaderParameters );
        }

        // load and store the geometry shader if it was specified
        pValue = mConfigBlock.GetValueByName("GeometryShaderFile");
        if( pValue.IsValid() )
        {
//            ASSERT( !IsMultiMaterial(), L"Multi-material GeometryShaderFile not yet supported" );
            if(IsMultiMaterial()){
                throw new IllegalArgumentException("Multi-material GeometryShaderFile not yet supported");
            }

            pEntryPointName = mConfigBlock.GetValueByName("GeometryShaderMain");
            pProfileName = mConfigBlock.GetValueByName("GeometryShaderProfile");
            mpGeometryShader = pAssetLibrary.GetGeometryShader(
                    pValue.ValueAsString(),
//                    pD3dDevice,
                    pEntryPointName.ValueAsString(),
                    pProfileName.ValueAsString(),
                    false
            );
            ReadShaderSamplersAndTextures( mpGeometryShader/*->GetBlob()*/, mShaderParameters );
        }

        // load and store the hull shader if it was specified
        pValue = mConfigBlock.GetValueByName("HullShaderFile");
        if( pValue.IsValid() )
        {
//            ASSERT( !IsMultiMaterial(), L"Multi-material HullShaderFile not yet supported" );
            if(IsMultiMaterial()){
                throw new IllegalArgumentException("Multi-material HullShaderFile not yet supported");
            }

            pEntryPointName = mConfigBlock.GetValueByName("HullShaderMain");
            pProfileName = mConfigBlock.GetValueByName("HullShaderProfile");
            mpHullShader = pAssetLibrary.GetHullShader(
                    pValue.ValueAsString(),
//                    pD3dDevice,
                    pEntryPointName.ValueAsString(),
                    pProfileName.ValueAsString(),
                    false
            );
            ReadShaderSamplersAndTextures( mpHullShader/*->GetBlob()*/, mShaderParameters );
        }

        // load and store the domain shader if it was specified
        pValue = mConfigBlock.GetValueByName("DomainShaderFile");
        if( pValue.IsValid() )
        {
//            ASSERT( !IsMultiMaterial(), L"Multi-material DomainShaderFile not yet supported" );
            if(IsMultiMaterial()){
                throw new IllegalArgumentException("Multi-material DomainShaderFile not yet supported");
            }

            pEntryPointName = mConfigBlock.GetValueByName("DomainShaderMain");
            pProfileName = mConfigBlock.GetValueByName("DomainShaderProfile");
            mpDomainShader = pAssetLibrary.GetDomainShader(
                    pValue.ValueAsString(),
//                    pD3dDevice,
                    pEntryPointName.ValueAsString(),
                    pProfileName.ValueAsString(),
                    false
            );
            ReadShaderSamplersAndTextures( mpDomainShader/*->GetBlob()*/, mShaderParameters );
        }

        // load and store the render state file if it was specified
        pValue = mConfigBlock.GetValueByName("RenderStateFile");
        if( pValue.IsValid() )
        {
//            ASSERT( !IsMultiMaterial(), L"Multi-material RenderStateFile not yet supported" );
            if(IsMultiMaterial()){
                throw new IllegalArgumentException("Multi-material RenderStateFile not yet supported");
            }

//            mpRenderStateBlock = pAssetLibrary->GetRenderStateBlock(pValue->ValueAsString());  TODO
        }

        if( IsMultiMaterial() )
        {
            // no bindings supported for Multi-material yet
            return /*result*/;
        }

//        OUTPUT_BINDING_DEBUG_INFO( (_L("Bindings for : ") + mMaterialName + _L("\n")).c_str() );
        LogUtil.i(LogUtil.LogType.DEFAULT, "Bindings for : " + mMaterialName);
        /*String pShaderTypeNameList[] = {
                ("Pixel shader"),
                ("Compute shader"),
                ("Vertex shader"),
                ("Geometry shader"),
                ("Hull shader"),
                ("Domain shader"),
        };
        cString *pShaderTypeName = pShaderTypeNameList;
        void *pShaderList[] = {
            mpPixelShader,
                    mpComputeShader,
                    mpVertexShader,
                    mpGeometryShader,
                    mpHullShader,
                    mpDomainShader
        };
        void **pShader = pShaderList;*/

        // For each of the shader stages, bind shaders and buffers
        /*for( CPUTShaderParameters **pCur = mpShaderParametersList; *pCur; pCur++ ) // Bind textures and buffersfor each shader stage
        {
            // Clear the bindings to reduce "resource still bound" errors, and to avoid leaving garbage pointers between valid pointers.
            // We bind resources as arrays.  We bind from the min required bind slot to the max-required bind slot.
            // Any slots in between will get bound.  It isn't clear if D3D will simply ignore them, or not.
            // But, they could be garbage, or valid resources from a previous binding.
            memset( pCur.mppBindViews,           0, sizeof(pCur.mppBindViews) );
            memset( pCur.mppBindUAVs,            0, sizeof(pCur.mppBindUAVs) );
            memset( pCur.mppBindConstantBuffers, 0, sizeof(pCur.mppBindConstantBuffers) );

            if( !*pShader++ )
            {
                pShaderTypeName++; // Increment the name pointer to remain coherent.
                continue;          // This shader not bound.  Don't waste time binding to it.
            }

            OUTPUT_BINDING_DEBUG_INFO( (*(pShaderTypeName++)  + _L("\n")).c_str() );

            BindTextures(        **pCur, modelSuffix, meshSuffix );
            BindBuffers(         **pCur, modelSuffix, meshSuffix );
            BindUAVs(            **pCur, modelSuffix, meshSuffix );
            BindConstantBuffers( **pCur, modelSuffix, meshSuffix );

            OUTPUT_BINDING_DEBUG_INFO( _L("\n") );
        }*/

        BindTextures(        mShaderParameters, modelSuffix, meshSuffix );
        BindBuffers(         mShaderParameters, modelSuffix, meshSuffix );
        BindUAVs(            mShaderParameters, modelSuffix, meshSuffix );
        BindConstantBuffers( mShaderParameters, modelSuffix, meshSuffix );

        return /*result*/;
    }

    private void initlizeGL(){
        gl = GLFuncProviderFactory.getGLFuncProvider();
        mProgram = new GLSLProgramPipeline();
    }

    @Override
    public void ReleaseTexturesAndBuffers(boolean recurseSubMaterials) {
        if( IsMultiMaterial() )
        {
            if( recurseSubMaterials )
            {
                for( int i = 0; i < mSubMaterialCount; i++ )
                    mpSubMaterials[i].ReleaseTexturesAndBuffers(false);
            }

            return;
        }

//        for( CPUTShaderParameters **pCur = mpShaderParametersList; *pCur; pCur++ )
        {
            for( int ii=0; ii<CPUT_MATERIAL_MAX_TEXTURE_SLOTS; ii++ )
            {
                SAFE_RELEASE((Disposeable) mShaderParameters.mppBindViews[ii]);
            }
            for( int ii=0; ii<CPUT_MATERIAL_MAX_UAV_SLOTS; ii++ )
            {
                SAFE_RELEASE(mShaderParameters.mppBindUAVs[ii]);
            }
            for( int ii=0; ii<CPUT_MATERIAL_MAX_UAV_SLOTS; ii++ )
            {
                SAFE_RELEASE(mShaderParameters.mppBindConstantBuffers[ii]);
            }

            Arrays.fill(mShaderParameters.mppBindViews, null);
            Arrays.fill(mShaderParameters.mppBindUAVs, null);
            Arrays.fill(mShaderParameters.mppBindConstantBuffers, null);
        }
    }

    private void setShaderResources(){
        mProgram.setVS(mpVertexShader);
        mProgram.setTC(mpHullShader);
        mProgram.setTE(mpDomainShader);
        mProgram.setGS(mpGeometryShader);
        mProgram.setPS(mpPixelShader);
        mProgram.setCS(mpComputeShader);

        if(mShaderParameters.mTextureCount > 0){
            for( int ii=0; ii < mShaderParameters.mTextureCount; ii++ )
            {
                int bindPoint = mShaderParameters.mpTextureParameterBindPoint[ii];
                if(mpLastShaderViews[bindPoint] != mShaderParameters.mppBindViews[bindPoint] )
                {
                    /*mpLast##SHADER##ShaderViews[bindPoint] = m##SHADER##ShaderParameters.mppBindViews[bindPoint];*/
                    TextureGL shaderResource = (TextureGL) mShaderParameters.mppBindViews[bindPoint];
                    gl.glActiveTexture(GLenum.GL_TEXTURE0);
                    gl.glBindTexture(shaderResource.getTarget(), shaderResource.getTexture());
                    mpLastShaderViews[bindPoint] = shaderResource;
                }
            }
        }

        if(mShaderParameters.mConstantBufferCount > 0){
            for( int ii=0; ii < mShaderParameters.mConstantBufferCount; ii++ ){
                int bindPoint = mShaderParameters.mpConstantBufferParameterBindPoint[ii];
                if(mpLastShaderConstantBuffers[bindPoint] != mShaderParameters.mppBindConstantBuffers[bindPoint]){
                    BufferGL constantBuffer = mShaderParameters.mppBindConstantBuffers[bindPoint];

                    if(constantBuffer != null) {
                        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, bindPoint, constantBuffer.getBuffer());
                    }else{
                        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, bindPoint, 0);
                    }
                    mpLastShaderConstantBuffers[bindPoint] = constantBuffer;
                }
            }
        }
    }

    @Override
    public void RebindTexturesAndBuffers(boolean recurseSubMaterials) {
        if( IsMultiMaterial() )
        {
            if( recurseSubMaterials )
            {
                for( int i = 0; i < mSubMaterialCount; i++ )
                    mpSubMaterials[i].RebindTexturesAndBuffers(recurseSubMaterials);
            }

            return;
        }

//        for( CPUTShaderParameters **pCur = mpShaderParametersList; *pCur; pCur++ ) // Rebind textures for each shader stage
        {
            CPUTShaderParameters pCur = mShaderParameters;
            for( int ii=0; ii<(pCur).mTextureCount; ii++ )
            {
                if( pCur.mpTexture[ii]  == null)
                {
                    continue;
                }
                int bindPoint = pCur.mpTextureParameterBindPoint[ii];
//                SAFE_RELEASE(pCur.mppBindViews[bindPoint]);
                pCur.mppBindViews[bindPoint] = ((CPUTTextureDX11)pCur.mpTexture[ii]).GetShaderResourceView();
//                pCur.mppBindViews[bindPoint]->AddRef();
            }
            for( int ii=0; ii<pCur.mBufferCount; ii++ )
            {
                int bindPoint = pCur.mpBufferParameterBindPoint[ii];
                /*SAFE_RELEASE(pCur.mppBindViews[bindPoint]);
                pCur.mppBindViews[bindPoint] = ((CPUTBufferDX11)pCur.mpBuffer[ii]).GetShaderResourceView();
                pCur.mppBindViews[bindPoint]->AddRef();*/
            }
            for( int ii=0; ii<pCur.mUAVCount; ii++ )
            {
                int bindPoint = pCur.mpUAVParameterBindPoint[ii];
                /*SAFE_RELEASE(pCur.mppBindUAVs[bindPoint]);
                pCur.mppBindUAVs[bindPoint] = ((CPUTBufferDX11*)pCur.mpUAV[ii])->GetUnorderedAccessView();
                pCur.mppBindUAVs[bindPoint]->AddRef();*/
            }
            for( int ii=0; ii<pCur.mConstantBufferCount; ii++ )
            {
                int bindPoint = pCur.mpConstantBufferParameterBindPoint[ii];
                /*SAFE_RELEASE(pCur.mppBindConstantBuffers[bindPoint]);
                pCur.mppBindConstantBuffers[bindPoint] = ((CPUTBufferDX11*)pCur.mpConstantBuffer[ii])->GetNativeBuffer();
                pCur.mppBindConstantBuffers[bindPoint]->AddRef();*/
            }
        }
    }

    @Override
    public boolean MaterialRequiresPerModelPayload() {
        if( IsMultiMaterial() )
        {
            for( int i = 0; i < mSubMaterialCount; i++ )
                if( mpSubMaterials[i].MaterialRequiresPerModelPayload() )
                return true;
            return false;
        }
        else
        {
            return
                (mpPixelShader!=null    && ShaderRequiresPerModelPayload(mpPixelShader, mConfigBlock))  ||
                (mpComputeShader!=null  && ShaderRequiresPerModelPayload(mpComputeShader, mConfigBlock))  ||
                (mpVertexShader!=null   && ShaderRequiresPerModelPayload(mpVertexShader, mConfigBlock))  ||
                (mpGeometryShader!=null && ShaderRequiresPerModelPayload(mpGeometryShader, mConfigBlock))  ||
                (mpHullShader!=null     && ShaderRequiresPerModelPayload(mpHullShader, mConfigBlock))  ||
                (mpDomainShader!=null   && ShaderRequiresPerModelPayload(mpDomainShader, mConfigBlock));
        }
    }

    private boolean ShaderRequiresPerModelPayload(ShaderProgram shader,  CPUTConfigBlock properties){
        /*ID3D11ShaderReflection *pReflector = NULL;  TODO

        D3DReflect( mpBlob->GetBufferPointer(), mpBlob->GetBufferSize(), IID_ID3D11ShaderReflection, (void**)&pReflector);
        // Walk through the shader input bind descriptors.
        // If any of them begin with '@', then we need a unique material per model (i.e., we need to clone the material).
        int ii=0;
        D3D11_SHADER_INPUT_BIND_DESC desc;
        HRESULT hr = pReflector->GetResourceBindingDesc( ii++, &desc );
        while( SUCCEEDED(hr) )
        {
            cString tagName = s2ws(desc.Name);
            CPUTConfigEntry *pValue = properties.GetValueByName(tagName);
            if( !pValue->IsValid() )
            {
                // We didn't find our property in the file.  Is it in the global config block?
                pValue = CPUTMaterial::mGlobalProperties.GetValueByName(tagName);
            }
            cString boundName = pValue->ValueAsString();
            if( (boundName.length() > 0) && ((boundName[0] == '@') || (boundName[0] == '#')) )
            {
                return true;
            }
            hr = pReflector->GetResourceBindingDesc( ii++, &desc );
        }*/

        throw new UnsupportedOperationException();
    }

    @Override
    public CPUTMaterial CloneMaterial(String absolutePathAndFilename, String modelSuffix, String meshSuffix) {
        return null;
    }

    public ShaderProgram   GetVertexShader()   { return GetCurrentSubMaterial().mpVertexShader; }
    public ShaderProgram    GetPixelShader()    { return GetCurrentSubMaterial().mpPixelShader; }
    public ShaderProgram GetGeometryShader() { return GetCurrentSubMaterial().mpGeometryShader; }
    public ShaderProgram  GetComputeShader()  { return GetCurrentSubMaterial().mpComputeShader; }
    public ShaderProgram   GetDomainShader()   { return GetCurrentSubMaterial().mpDomainShader; }
    public ShaderProgram     GetHullShader()     { return GetCurrentSubMaterial().mpHullShader; }

    public boolean                        IsMultiMaterial()                    { return mSubMaterialCount > 0; }
    public static void                 GlobalSetCurrentSubMaterial( int index )    { mCurrentSubMaterialIndex = index; }
    public CPUTMaterialDX11   GetCurrentSubMaterial(){
        if( !IsMultiMaterial() )
            return this;
        if( (mCurrentSubMaterialIndex >= 0) && (mCurrentSubMaterialIndex < mSubMaterialCount) )
            return mpSubMaterials[mCurrentSubMaterialIndex];
        throw new IllegalArgumentException("Material not a IsMultiMaterial or mCurrentSubMaterialIndex index invalid");
    }

    /**
     * Tells material to set the current render state to match the properties, textures,
     *  shaders, state, etc that this material represents
     * @param renderParams
     */
    @Override
    public void SetRenderStates( CPUTRenderParameters renderParams ){
        gl.glUseProgram(0);
        mProgram.enable();
        setShaderResources();

        // Only the compute shader may have UAVs to bind.
        // Note that pixel shaders can too, but DX requires setting those when setting RTV(s).
        for( int ii=0; ii<mShaderParameters.mUAVCount; ii++ )
        {
            int bindPoint = mShaderParameters.mpUAVParameterBindPoint[ii];
            if(mpLastComputeShaderUAVs[ii] != mShaderParameters.mppBindUAVs[bindPoint] )
            {
                TextureGL unorderedView = mShaderParameters.mppBindUAVs[bindPoint];
                mpLastComputeShaderUAVs[ii] = unorderedView;

                if(unorderedView != null){
                    gl.glBindImageTexture(bindPoint, unorderedView.getTexture(), 0, false, 0, GLenum.GL_READ_WRITE, unorderedView.getFormat());
                }else{
                    gl.glBindImageTexture(bindPoint, 0, 0, false, 0, GLenum.GL_READ_WRITE, GLenum.GL_RGBA8);
                }
            }
        }

        // Set the render state block if it changed
        /*if( mpLastRenderStateBlock != mpRenderStateBlock )
        {
            mpLastRenderStateBlock = mpRenderStateBlock;
            if( mpRenderStateBlock )
            {
                // We know we have a DX11 class.  Does this correctly bypass the virtual?
                // Should we move it to the DX11 class.
                ((CPUTRenderStateBlockDX11*)mpRenderStateBlock)->SetRenderStates(renderParams);
            }
            else
            {
                CPUTRenderStateBlock::GetDefaultRenderStateBlock()->SetRenderStates(renderParams);
            }
        }*/
    }

//    CPUTMaterial *CloneMaterial( const cString &absolutePathAndFilename, const cString &modelSuffix, const cString &meshSuffix );
    public static void ResetStateTracking()
    {
        mpLastVertexShader = null;
        mpLastPixelShader = null;
        mpLastComputeShader = null;
        mpLastGeometryShader = null;
        mpLastHullShader = null;
        mpLastDomainShader = null;
        mpLastRenderStateBlock = null;
        for(int ii=0; ii<CPUT_MATERIAL_MAX_TEXTURE_SLOTS; ii++ )
        {
            mpLastShaderViews[ii] = null;
        }
        for(int ii=0; ii<CPUT_MATERIAL_MAX_CONSTANT_BUFFER_SLOTS; ii++ )
        {
            mpLastShaderConstantBuffers[ii]   = null;
        }
        for(int ii=0; ii<CPUT_MATERIAL_MAX_UAV_SLOTS; ii++ )
        {
            mpLastComputeShaderUAVs[ii] = null;
        }
    }

    @Override
    public void dispose() {
        for( int i = 0; i < mSubMaterialCount; i++ ) {
            SAFE_RELEASE(mpSubMaterials[i]);
            mpSubMaterials[i] = null;
        }
        SAFE_DELETE_ARRAY(mpSubMaterials);

        // release any shaders
        SAFE_RELEASE(mpPixelShader);
        SAFE_RELEASE(mpComputeShader);
        SAFE_RELEASE(mpVertexShader);
        SAFE_RELEASE(mpGeometryShader);
        SAFE_RELEASE(mpHullShader);
        SAFE_RELEASE(mpDomainShader);
//        SAFE_RELEASE(mpRenderStateBlock);
        mpPixelShader = null;
        mpComputeShader = null;
        mpVertexShader = null;
        mpGeometryShader = null;
        mpHullShader = null;
        mpDomainShader = null;
    }

    protected void ReadShaderSamplersAndTextures(   ShaderProgram shader, CPUTShaderParameters pShaderParameter ){
        ProgramResources resources = GLSLUtil.getProgramResources(shader.getProgram());

        UniformProperty[] samplerUniforms = resources.active_uniform_properties;
        if(samplerUniforms != null){
            for(int i = 0; i < samplerUniforms.length; i++){
                final String typeName = GLSLUtil.getGLSLTypeName(samplerUniforms[i].type);

                if(typeName.contains("sampler")){  // texture
                    pShaderParameter.mTextureParameterCount++;
                }else if(typeName.contains("image") || typeName.contains("imageBuffer")){ // unorder resource views
                    pShaderParameter.mUAVParameterCount ++;
                }
            }
        }

        List<UniformBlockProperties>  uniformBlocks =  resources.uniformBlockProperties;
        for(int i = 0; i < uniformBlocks.size(); i++){
            UniformBlockProperties uniformBlock = uniformBlocks.get(i);
            if(uniformBlock.type == UniformBlockType.UNFIORM_BLOCK){  // const buffers
                pShaderParameter.mConstantBufferParameterCount++;
            }else if(uniformBlock.type == UniformBlockType.UNIFORM_BUFFER){ // shader storage buffers
                pShaderParameter.mBufferParameterCount ++;
            }
        }

        pShaderParameter.mpTextureParameterName              = new String[pShaderParameter.mTextureParameterCount];
        pShaderParameter.mpTextureParameterBindPoint         = new int[   pShaderParameter.mTextureParameterCount];
        pShaderParameter.mpSamplerParameterName              = new String[pShaderParameter.mSamplerParameterCount];
        pShaderParameter.mpSamplerParameterBindPoint         = new int[   pShaderParameter.mSamplerParameterCount];
        pShaderParameter.mpBufferParameterName               = new String[pShaderParameter.mBufferParameterCount];
        pShaderParameter.mpBufferParameterBindPoint          = new int[   pShaderParameter.mBufferParameterCount];
        pShaderParameter.mpUAVParameterName                  = new String[pShaderParameter.mUAVParameterCount];
        pShaderParameter.mpUAVParameterBindPoint             = new int[   pShaderParameter.mUAVParameterCount];
        pShaderParameter.mpConstantBufferParameterName       = new String[pShaderParameter.mConstantBufferParameterCount];
        pShaderParameter.mpConstantBufferParameterBindPoint  = new int[   pShaderParameter.mConstantBufferParameterCount];

        // Start over.  This time, copy the names.
        int ii=0;
        int textureIndex = 0;
        int samplerIndex = 0;
        int bufferIndex = 0;
        int uavIndex = 0;
        int constantBufferIndex = 0;

        if(samplerUniforms != null){
            for(int i = 0; i < samplerUniforms.length; i++){
                UniformProperty property = samplerUniforms[i];
                final String typeName = GLSLUtil.getGLSLTypeName(property.type);

                String strName = property.name;
                boolean ignore = (strName.length() > 8) && (strName.substring(0, 8).equals("NONCPUT_"));
                if(typeName.contains("sampler")){  // texture
                    if( ignore )
                    {
                        assert pShaderParameter.mTextureParameterCount>0:"Algorithm error";
                        pShaderParameter.mTextureParameterCount--;
                    }
                    else
                    {
                        pShaderParameter.mpTextureParameterName[textureIndex] = strName;
                        pShaderParameter.mpTextureParameterBindPoint[textureIndex] = (Integer)property.value;
                        textureIndex++;
                    }
                }else if(typeName.contains("image") || typeName.contains("imageBuffer")){ // unorder resource views
                    if( ignore )
                    {
                        assert pShaderParameter.mUAVParameterCount > 0 : "Algorithm error";
                        pShaderParameter.mUAVParameterCount--;
                    }
                    else
                    {
                        pShaderParameter.mpUAVParameterName[uavIndex] = strName;
                        pShaderParameter.mpUAVParameterBindPoint[uavIndex] = (Integer)property.value;
                        uavIndex++;
                    }
                }
            }
        }

        for(int i = 0; i < uniformBlocks.size(); i++){
            UniformBlockProperties uniformBlock = uniformBlocks.get(i);
            String strName = uniformBlock.name;
            boolean ignore = (strName.length() > 8) && (strName.substring(0, 8).equals("NONCPUT_"));
            if(uniformBlock.type == UniformBlockType.UNFIORM_BLOCK){  // const buffers
                if( ignore )
                {
                    assert pShaderParameter.mConstantBufferParameterCount > 0:"Algorithm error";
                    pShaderParameter.mConstantBufferParameterCount--;
                }
                else
                {
                    pShaderParameter.mpConstantBufferParameterName[constantBufferIndex] = strName;
                    pShaderParameter.mpConstantBufferParameterBindPoint[constantBufferIndex] = uniformBlock.binding;
                    constantBufferIndex++;
                }
            }else if(uniformBlock.type == UniformBlockType.UNIFORM_BUFFER){ // shader storage buffers
                if( ignore )
                {
                    assert pShaderParameter.mBufferParameterCount > 0 : "Algorithm error";
                    pShaderParameter.mBufferParameterCount--;
                }
                else
                {
                    pShaderParameter.mpBufferParameterName[bufferIndex] = strName;
                    pShaderParameter.mpBufferParameterBindPoint[bufferIndex] = uniformBlock.binding;
                    bufferIndex++;
                }
            }
        }
    }

    protected void BindTextures(        CPUTShaderParameters params, String modelSuffix, String meshSuffix ) throws IOException{
        if( IsMultiMaterial() )
        {
            GetCurrentSubMaterial().BindTextures( params, modelSuffix, meshSuffix );
            return;
        }

        if( params.mTextureParameterCount  == 0) {return;}
        /*OUTPUT_BINDING_DEBUG_INFO( _L() );*/
        LogUtil.i(LogUtil.LogType.DEFAULT, "Bound Textures");

        CPUTAssetLibraryDX11 pAssetLibrary = (CPUTAssetLibraryDX11)CPUTAssetLibrary.GetAssetLibrary();

        for(params.mTextureCount=0; params.mTextureCount < params.mTextureParameterCount; params.mTextureCount++)
        {
            String textureName;
            int textureCount = params.mTextureCount;
            String tagName = params.mpTextureParameterName[textureCount];
            CPUTConfigEntry pValue = mConfigBlock.GetValueByName(tagName);
            if( !pValue.IsValid() )
            {
                // We didn't find our property in the file.  Is it in the global config block?
                pValue = mGlobalProperties.GetValueByName(tagName);
            }
            /*ASSERT( pValue->IsValid(), L"Can't find texture '" + tagName + L"'." ); //  TODO: fix message*/
            if(!pValue.IsValid())
                throw new RuntimeException("Can't find texture '" + tagName + "'.");
            textureName = pValue.ValueAsString();

            int bindPoint = params.mpTextureParameterBindPoint[textureCount];
            if(bindPoint >= CPUT_MATERIAL_MAX_TEXTURE_SLOTS)
                throw new RuntimeException("Texture bind point out of range.");

            // If the texture name not specified.  Load default.dds instead
            if( 0 == textureName.length() ) { textureName = ("default.dds"); }

            params.mBindViewMin = Math.min( params.mBindViewMin, bindPoint );
            params.mBindViewMax = Math.max( params.mBindViewMax, bindPoint );

            if( textureName.charAt(0) == '@' )
            {
                // This is a per-mesh value.  Add to per-mesh list.
                textureName += modelSuffix + meshSuffix;
            } else if( textureName.charAt(0) == '#' )
            {
                // This is a per-mesh value.  Add to per-mesh list.
                textureName += modelSuffix;
            }

            // Get the sRGB flag (default to true)
            String SRGBName = tagName+("sRGB");
            CPUTConfigEntry pSRGBValue = mConfigBlock.GetValueByName(SRGBName);
            boolean loadAsSRGB = pSRGBValue.IsValid() ?  loadAsSRGB = pSRGBValue.ValueAsBool() : true;

            if( params.mpTexture[textureCount]  == null)
            {
                params.mpTexture[textureCount] = pAssetLibrary.GetTexture( textureName, false, loadAsSRGB );
//                ASSERT( params.mpTexture[textureCount], _L("Failed getting texture ") + textureName);
                if(params.mpTexture[textureCount] == null){
                    throw new RuntimeException("Failed getting texture " + textureName);
                }
            }

            // The shader file (e.g. .fx) can specify the texture bind point (e.g., t0).  Those specifications
            // might not be contiguous, and there might be gaps (bind points without assigned textures)
            // TODO: Warn about missing bind points?
            params.mppBindViews[bindPoint] = ((CPUTTextureDX11)params.mpTexture[textureCount]).GetShaderResourceView();
            /*params.mppBindViews[bindPoint]->AddRef();*/

//            OUTPUT_BINDING_DEBUG_INFO( (itoc(bindPoint) + _L(" : ") + params.mpTexture[textureCount]->GetName() + _L("\n")).c_str() );
            LogUtil.i(LogUtil.LogType.DEFAULT, bindPoint + " : " + params.mpTexture[textureCount].GetName());
        }
    }

    protected void BindBuffers(         CPUTShaderParameters params, String modelSuffix, String meshSuffix ){
        if( IsMultiMaterial() )
        {
            GetCurrentSubMaterial().BindBuffers( params, modelSuffix, meshSuffix );
            return;
        }

        if( params.mBufferParameterCount == 0 ) { return; }
        /*OUTPUT_BINDING_DEBUG_INFO( _L("Bound Buffers") );*/
        LogUtil.i(LogUtil.LogType.DEFAULT, "Bound Buffers");

        CPUTAssetLibraryDX11 pAssetLibrary = (CPUTAssetLibraryDX11)CPUTAssetLibrary.GetAssetLibrary();
        for(params.mBufferCount=0; params.mBufferCount < params.mBufferParameterCount; params.mBufferCount++)
        {
            String bufferName;
            int bufferCount = params.mBufferCount;
            String tagName = params.mpBufferParameterName[bufferCount];
            {
                CPUTConfigEntry pValue = mConfigBlock.GetValueByName(tagName);
                if( !pValue.IsValid() )
                {
                    // We didn't find our property in the file.  Is it in the global config block?
                    pValue = mGlobalProperties.GetValueByName(tagName);
                }
//                ASSERT( pValue->IsValid(), L"Can't find buffer '" + tagName + L"'." ); //  TODO: fix message
                if(!pValue.IsValid()){
                    throw new RuntimeException("Can't find buffer '" + tagName + "'.");
                }

                bufferName = pValue.ValueAsString();
            }
            int bindPoint = params.mpBufferParameterBindPoint[bufferCount];
//            ASSERT( bindPoint < CPUT_MATERIAL_MAX_BUFFER_SLOTS, _L("Buffer bind point out of range.") );
            if(bindPoint >= CPUT_MATERIAL_MAX_BUFFER_SLOTS)
                throw new RuntimeException("Buffer bind point out of range.");

            params.mBindViewMin = Math.min( params.mBindViewMin, bindPoint );
            params.mBindViewMax = Math.max( params.mBindViewMax, bindPoint );

            if( bufferName.charAt(0) == '@' )
            {
                // This is a per-mesh value.  Add to per-mesh list.
                bufferName += modelSuffix + meshSuffix;
            } else if( bufferName.charAt(0) == '#' )
            {
                // This is a per-mesh value.  Add to per-model list.
                bufferName += modelSuffix;
            }
            if( params.mpBuffer[bufferCount]  == null)
            {
                params.mpBuffer[bufferCount] = pAssetLibrary.GetBuffer( bufferName );
//                ASSERT( params.mpBuffer[bufferCount], _L("Failed getting buffer ") + bufferName);
                if(params.mpBuffer[bufferCount] == null){
                    throw new RuntimeException("Failed getting buffer " + bufferName);
                }
            }

            params.mppBindViews[bindPoint]   = params.mpBuffer[bufferCount]/*((CPUTBufferDX11)params.mpBuffer[bufferCount]).GetShaderResourceView()*/;
            /*if( params.mppBindViews[bindPoint] )  { params.mppBindViews[bindPoint]->AddRef();}*/

//            OUTPUT_BINDING_DEBUG_INFO( (itoc(bindPoint) + _L(" : ") + params.mpBuffer[bufferCount]->GetName() + _L("\n")).c_str() );
        }
//        OUTPUT_BINDING_DEBUG_INFO( _L("\n") );
    }

    protected void BindUAVs(            CPUTShaderParameters params, String modelSuffix, String meshSuffix ){
        if( IsMultiMaterial() )
        {
            GetCurrentSubMaterial().BindUAVs( params, modelSuffix, meshSuffix );
            return;
        }

        if( params.mUAVParameterCount ==0) { return; }
        LogUtil.i(LogUtil.LogType.DEFAULT, "Bound UAVs");

        CPUTAssetLibraryDX11 pAssetLibrary = (CPUTAssetLibraryDX11)CPUTAssetLibrary.GetAssetLibrary();
        for(params.mUAVCount=0; params.mUAVCount < params.mUAVParameterCount; params.mUAVCount++)
        {
            String uavName;
            int uavCount = params.mUAVCount;

            String tagName = params.mpUAVParameterName[uavCount];
            {
                CPUTConfigEntry pValue = mConfigBlock.GetValueByName(tagName);
                if( !pValue.IsValid() )
                {
                    // We didn't find our property in the file.  Is it in the global config block?
                    pValue = mGlobalProperties.GetValueByName(tagName);
                }
                /*ASSERT( pValue->IsValid(), L"Can't find UAV '" + tagName + L"'." ); //  TODO: fix message*/
                if(!pValue.IsValid())
                    throw new RuntimeException("Can't find UAV '" + tagName + "'.");
                uavName = pValue.ValueAsString();
            }
            int bindPoint = params.mpUAVParameterBindPoint[uavCount];
//            ASSERT( bindPoint < CPUT_MATERIAL_MAX_UAV_SLOTS, _L("UAV bind point out of range.") );
            if(bindPoint >= CPUT_MATERIAL_MAX_UAV_SLOTS){
                throw new RuntimeException("UAV bind point out of range.");
            }

            params.mBindUAVMin = Math.min( params.mBindUAVMin, bindPoint );
            params.mBindUAVMax = Math.max( params.mBindUAVMax, bindPoint );

            if( uavName.charAt(0) == '@' )
            {
                // This is a per-mesh value.  Add to per-mesh list.
                uavName += modelSuffix + meshSuffix;
            } else if( uavName.charAt(0) == '#' )
            {
                // This is a per-mesh value.  Add to per-model list.
                uavName += modelSuffix;
            }
            if( params.mpUAV[uavCount] ==null)
            {
                params.mpUAV[uavCount] = pAssetLibrary.GetBuffer( uavName );
//                ASSERT( params.mpUAV[uavCount], _L("Failed getting UAV ") + uavName);
                if(params.mpUAV[uavCount] == null)
                throw new RuntimeException("Failed getting UAV " + uavName);
            }

            // If has UAV, then add to mppBindUAV
            /*params.mppBindUAVs[bindPoint]   = ((CPUTBufferDX11*)params.mpUAV[uavCount])->GetUnorderedAccessView();
            if( params.mppBindUAVs[bindPoint] )  { params.mppBindUAVs[bindPoint]->AddRef();}*/
//            params.mppBindUAVs[bindPoint]   = params.mpUAV[uavCount];  TODO

//            OUTPUT_BINDING_DEBUG_INFO( (itoc(bindPoint) + _L(" : ") + params.mpUAV[uavCount]->GetName() + _L("\n")).c_str() );
        }
//        OUTPUT_BINDING_DEBUG_INFO( _L("\n") );
    }

    protected void BindConstantBuffers( CPUTShaderParameters params, String modelSuffix, String meshSuffix ){
        if( IsMultiMaterial() )
        {
            GetCurrentSubMaterial().BindConstantBuffers( params, modelSuffix, meshSuffix );
            return;
        }

        if( params.mConstantBufferParameterCount ==0) { return; }
        LogUtil.i(LogUtil.LogType.DEFAULT, "Bound Constant Buffers");

        CPUTAssetLibraryDX11 pAssetLibrary = (CPUTAssetLibraryDX11)CPUTAssetLibrary.GetAssetLibrary();
        for(params.mConstantBufferCount=0; params.mConstantBufferCount < params.mConstantBufferParameterCount; params.mConstantBufferCount++)
        {
            String constantBufferName;
            int constantBufferCount = params.mConstantBufferCount;

            String tagName = params.mpConstantBufferParameterName[constantBufferCount];
            {
                CPUTConfigEntry pValue = mConfigBlock.GetValueByName(tagName);
                if( !pValue.IsValid() )
                {
                    // We didn't find our property in the file.  Is it in the global config block?
                    pValue = mGlobalProperties.GetValueByName(tagName);
                }
//                ASSERT( pValue->IsValid(), L"Can't find constant buffer '" + tagName + L"'." ); //  TODO: fix message
                if(!pValue.IsValid()){
                    throw new RuntimeException("Can't find constant buffer '" + tagName + "'.");
                }

                constantBufferName = pValue.ValueAsString();
            }
            int bindPoint = params.mpConstantBufferParameterBindPoint[constantBufferCount];
//            ASSERT( bindPoint < CPUT_MATERIAL_MAX_CONSTANT_BUFFER_SLOTS, _L("Constant buffer bind point out of range.") );
            if(!(bindPoint < CPUT_MATERIAL_MAX_CONSTANT_BUFFER_SLOTS)){
                throw new RuntimeException("Constant buffer bind point out of range.");
            }

            params.mBindConstantBufferMin = Math.min( params.mBindConstantBufferMin, bindPoint );
            params.mBindConstantBufferMax = Math.max( params.mBindConstantBufferMax, bindPoint );

            if( constantBufferName.charAt(0) == '@' )
            {
                constantBufferName += modelSuffix + meshSuffix;
            } else if( constantBufferName.charAt(0) == '#' )
            {
                constantBufferName += modelSuffix;
            }
            if( params.mpConstantBuffer[constantBufferCount] == null )
            {
                constantBufferName = constantBufferName.toLowerCase();
                params.mpConstantBuffer[constantBufferCount] = pAssetLibrary.GetConstantBuffer( constantBufferName );
//                ASSERT( params.mpConstantBuffer[constantBufferCount], _L("Failed getting constant buffer ") + constantBufferName);
                if(params.mpConstantBuffer[constantBufferCount] == null)
                    throw new RuntimeException("Failed getting constant buffer " + constantBufferName);
            }

            // If has constant buffer, then add to mppBindConstantBuffer
//            params.mppBindConstantBuffers[bindPoint]   = ((CPUTBufferDX11*)params.mpConstantBuffer[constantBufferCount])->GetNativeBuffer();  TODO
//            if( params.mppBindConstantBuffers[bindPoint] )  { params.mppBindConstantBuffers[bindPoint]->AddRef();}

//            OUTPUT_BINDING_DEBUG_INFO( (itoc(bindPoint) + _L(" : ") + params.mpConstantBuffer[constantBufferCount]->GetName() + _L("\n")).c_str() );
        }
//        OUTPUT_BINDING_DEBUG_INFO( _L("\n") );
    }
}
