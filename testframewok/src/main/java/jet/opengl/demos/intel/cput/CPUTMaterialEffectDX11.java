package jet.opengl.demos.intel.cput;

import org.lwjgl.util.vector.Vector4f;

import java.io.IOException;
import java.util.Arrays;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.AttribBinder;
import jet.opengl.postprocessing.shader.GLSLUtil;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.shader.ProgramResources;
import jet.opengl.postprocessing.shader.ShaderProgram;
import jet.opengl.postprocessing.shader.UniformBlockProperties;
import jet.opengl.postprocessing.shader.UniformBlockType;
import jet.opengl.postprocessing.shader.UniformProperty;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.util.LogUtil;

public class CPUTMaterialEffectDX11 extends CPUTMaterialEffect {
    static Object mpLastVertexShader;
    static Object mpLastPixelShader;
    static Object mpLastComputeShader;
    static Object mpLastGeometryShader;
    static Object mpLastHullShader;
    static Object mpLastDomainShader;

    static Object[] mpLastShaderViews = new Object[CPUTMaterial.CPUT_MATERIAL_MAX_TEXTURE_SLOTS];
    static Object[] mpLastShaderConstantBuffers = new Object[CPUTMaterial.CPUT_MATERIAL_MAX_CONSTANT_BUFFER_SLOTS];
    static Object[] mpLastComputeShaderUAVs = new Object[CPUTMaterial.CPUT_MATERIAL_MAX_UAV_SLOTS];

    static Object mpLastRenderStateBlock;

    static int   mCurrentSubMaterialIndex;

    // TODO: move texture to base class.  All APIs have textures.
    ShaderProgram    mpPixelShader;
    ShaderProgram    mpComputeShader; // TODO: Do Compute Shaders belong in material?
    ShaderProgram    mpVertexShader;
    ShaderProgram    mpGeometryShader;
    ShaderProgram    mpHullShader;
    ShaderProgram    mpDomainShader;
    boolean          mRequiresPerModelPayload;

    final CPUTShaderParameters     mShaderParameters = new CPUTShaderParameters();
    private GLFuncProvider gl;

    public ShaderProgram   GetVertexShader()   { return mpVertexShader; }
    public ShaderProgram    GetPixelShader()    { return mpPixelShader; }
    public ShaderProgram GetGeometryShader() { return mpGeometryShader; }
    public ShaderProgram  GetComputeShader()  { return mpComputeShader; }
    public ShaderProgram   GetDomainShader()   { return mpDomainShader; }
    public ShaderProgram     GetHullShader()     { return mpHullShader; }

    @Override
    public void dispose() {
        SAFE_RELEASE(mpPixelShader);
        SAFE_RELEASE(mpComputeShader);
        SAFE_RELEASE(mpVertexShader);
        SAFE_RELEASE(mpGeometryShader);
        SAFE_RELEASE(mpHullShader);
        SAFE_RELEASE(mpDomainShader);
        SAFE_RELEASE(mpRenderStateBlock);
    }

    @Override
    public void ReleaseTexturesAndBuffers() {
        for( int ii=0; ii<CPUTMaterial.CPUT_MATERIAL_MAX_TEXTURE_SLOTS; ii++ )
        {
            SAFE_RELEASE((Disposeable) mShaderParameters.mppBindViews[ii]);
        }
        for( int ii=0; ii<CPUTMaterial.CPUT_MATERIAL_MAX_UAV_SLOTS; ii++ )
        {
            SAFE_RELEASE(mShaderParameters.mppBindUAVs[ii]);
        }
        for( int ii=0; ii<CPUTMaterial.CPUT_MATERIAL_MAX_UAV_SLOTS; ii++ )
        {
            SAFE_RELEASE(mShaderParameters.mppBindConstantBuffers[ii]);
        }

        Arrays.fill(mShaderParameters.mppBindViews, null);
        Arrays.fill(mShaderParameters.mppBindUAVs, null);
        Arrays.fill(mShaderParameters.mppBindConstantBuffers, null);
    }

    @Override
    public void RebindTexturesAndBuffers() {
        CPUTShaderParameters pCur = mShaderParameters;
        for( int ii=0; ii<(pCur).mTextureCount; ii++ )
        {
            if( pCur.mpTexture[ii]  != null)
            {
                int bindPoint = pCur.mpTextureParameters.get(ii).index;
//                SAFE_RELEASE(pCur.mppBindViews[bindPoint]);
                pCur.mppBindViews[bindPoint] = ((CPUTTextureDX11)pCur.mpTexture[ii]).GetShaderResourceView();
//                pCur.mppBindViews[bindPoint]->AddRef();
            }

        }
        for( int ii=0; ii<pCur.mBufferCount; ii++ )
        {
            int bindPoint = pCur.mpBufferParameters.get(ii).index;
                /*SAFE_RELEASE(pCur.mppBindViews[bindPoint]);
                pCur.mppBindViews[bindPoint] = ((CPUTBufferDX11)pCur.mpBuffer[ii]).GetShaderResourceView();
                pCur.mppBindViews[bindPoint]->AddRef();*/
        }
        for( int ii=0; ii<pCur.mUAVCount; ii++ )
        {
            int bindPoint = pCur.mpUAVParameters.get(ii).index;
                /*SAFE_RELEASE(pCur.mppBindUAVs[bindPoint]);
                pCur.mppBindUAVs[bindPoint] = ((CPUTBufferDX11*)pCur.mpUAV[ii])->GetUnorderedAccessView();
                pCur.mppBindUAVs[bindPoint]->AddRef();*/
        }
        for( int ii=0; ii<pCur.mConstantBufferCount; ii++ )
        {
            int bindPoint = pCur.mpConstantBufferParameters.get(ii).index;
                /*SAFE_RELEASE(pCur.mppBindConstantBuffers[bindPoint]);
                pCur.mppBindConstantBuffers[bindPoint] = ((CPUTBufferDX11*)pCur.mpConstantBuffer[ii])->GetNativeBuffer();
                pCur.mppBindConstantBuffers[bindPoint]->AddRef();*/
        }
    }

    /**
     * Tells material to set the current render state to match the properties, textures,
     *  shaders, state, etc that this material represents
     * @param renderParams
     */
    @Override
    public void SetRenderStates( CPUTRenderParameters renderParams ){
        CPUTRenderParametersDX context = (CPUTRenderParametersDX)renderParams;

        context.BeginRender();
        setShaderResources(context);

        // Only the compute shader may have UAVs to bind.
        // Note that pixel shaders can too, but DX requires setting those when setting RTV(s).
        for( int ii=0; ii<mShaderParameters.mUAVCount; ii++ )
        {
            int bindPoint = mShaderParameters.mpUAVParameters.get(ii).index;
//            if(mpLastComputeShaderUAVs[ii] != mShaderParameters.mppBindUAVs[bindPoint] )
            {
                TextureGL unorderedView = mShaderParameters.mppBindUAVs[bindPoint];
//                mpLastComputeShaderUAVs[ii] = unorderedView;

                if(unorderedView != null){
                    gl.glBindImageTexture(bindPoint, unorderedView.getTexture(), 0, false, 0, GLenum.GL_READ_WRITE, unorderedView.getFormat());
                }else{
                    gl.glBindImageTexture(bindPoint, 0, 0, false, 0, GLenum.GL_READ_WRITE, GLenum.GL_RGBA8);
                }
            }
        }

        // Set the render state block if it changed  TODO
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

    boolean MaterialUsesTessellationShaders()
    {
        return mpHullShader != null;
    }

    @Override
    public boolean MaterialRequiresPerModelPayload() {
        if( mRequiresPerModelPayload == false )
        {
            mRequiresPerModelPayload =
                    (mpPixelShader!=null    &&  ShaderRequiresPerModelPayload(mpPixelShader, mConfigBlock))  ||
                    (mpComputeShader!=null  &&  ShaderRequiresPerModelPayload(mpComputeShader,mConfigBlock))  ||
                    (mpVertexShader!=null   &&  ShaderRequiresPerModelPayload(mpVertexShader, mConfigBlock))  ||
                    (mpGeometryShader!=null &&  ShaderRequiresPerModelPayload(mpGeometryShader, mConfigBlock))  ||
                    (mpHullShader!=null     &&  ShaderRequiresPerModelPayload(mpHullShader, mConfigBlock))  ||
                    (mpDomainShader!=null   &&  ShaderRequiresPerModelPayload(mpDomainShader, mConfigBlock));
        }
        return mRequiresPerModelPayload;
    }

    private static boolean ShaderRequiresPerModelPayload(ShaderProgram shader, CPUTConfigBlock properties){
        ProgramResources resources = GLSLUtil.getProgramResources(shader.getProgram());

        if(resources.active_uniform_properties != null){
            for(int i = 0; i < resources.active_uniform_properties.length; i++){
                UniformProperty uniform = resources.active_uniform_properties[i];
                String tagName = uniform.name;
                CPUTConfigEntry pValue = properties.GetValueByName(tagName);
                if( !pValue.IsValid() )
                {
                    // We didn't find our property in the file.  Is it in the global config block?
                    pValue = CPUTMaterial.mGlobalProperties.GetValueByName(tagName);
                }

                String boundName = pValue.ValueAsString();
                if( (boundName.length() > 0) && ((boundName.charAt(0) == '@') || (boundName.charAt(0) == '#')) )
                {
                    return true;
                }
            }
        }

        for(UniformBlockProperties uniform : resources.uniformBlockProperties){
            if(uniform.type != UniformBlockType.UNFIORM_BLOCK){
                continue;
            }

            String tagName = uniform.name;
            CPUTConfigEntry pValue = properties.GetValueByName(tagName);
            if( !pValue.IsValid() )
            {
                // We didn't find our property in the file.  Is it in the global config block?
                pValue = CPUTMaterial.mGlobalProperties.GetValueByName(tagName);
            }

            String boundName = pValue.ValueAsString();
            if( (boundName.length() > 0) && ((boundName.charAt(0) == '@') || (boundName.charAt(0) == '#')) )
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public CPUTMaterialEffect CloneMaterialEffect(CPUTModel pModel, int meshIndex) {
        return null;
    }

    @Override
    public void LoadMaterialEffect(String fileName, CPUTModel pModel, int meshIndex, Macro[] pShaderMacros, int externalCount,
                                   String pExternalName, Vector4f[] pExternals, int[] pExternalOffses, int[] pExternalSize)
            throws IOException{

        mMaterialName = fileName;
//        mMaterialNameHash = CPUTComputeHash( mMaterialName );

        // Open/parse the file
        CPUTConfigFile file = new CPUTConfigFile();
        file.LoadFile(fileName);

        // Make a local copy of all the parameters
        CPUTConfigBlock pBlock = file.GetBlock(0);
//        ASSERT( pBlock, _L("Error getting parameter block") );
        if( pBlock  == null)
        {
            throw new NullPointerException("Error getting parameter block");
        }
        mConfigBlock = pBlock;

        // get necessary device and AssetLibrary pointers
//        ID3D11Device *pD3dDevice = CPUT_DX11::GetDevice();
        CPUTAssetLibraryDX11 pAssetLibrary = (CPUTAssetLibraryDX11)CPUTAssetLibrary.GetAssetLibrary();

        // TODO:  The following code is very repetitive.  Consider generalizing so we can call a function instead.
        // see if there are any pixel/vertex/geo shaders to load
        CPUTConfigEntry pValue;


        pValue   = mConfigBlock.GetValueByName("RenderLayer");
        if( pValue.IsValid() )
        {
//            pLayerMap.FindMapEntryByName( (int*)&mLayer, pValue->ValueAsString() );
            mLayer = CPUTLayerType.valueOf(pValue.ValueAsString());
        }

        CPUTConfigEntry pEntryPointName, pProfileName;
        pValue   = mConfigBlock.GetValueByName("VertexShaderFile");
        if( pValue.IsValid() )
        {
            pEntryPointName = mConfigBlock.GetValueByName("VertexShaderMain");
            pProfileName    = mConfigBlock.GetValueByName("VertexShaderProfile");
            mpVertexShader = pAssetLibrary.GetVertexShader(
                    pValue.ValueAsString(),
                    pEntryPointName.ValueAsString(),
                    pProfileName.ValueAsString(),
                    false,
                    pShaderMacros
            );

            ReadShaderSamplersAndTextures( mpVertexShader/*->GetBlob()*/, /*&mVertexShaderParameters*/ mShaderParameters);
        }

        // load and store the pixel shader if it was specified
        pValue  = mConfigBlock.GetValueByName("PixelShaderFile");
        if( pValue.IsValid() )
        {
            pEntryPointName = mConfigBlock.GetValueByName("PixelShaderMain");
            pProfileName    = mConfigBlock.GetValueByName("PixelShaderProfile");
            mpPixelShader = pAssetLibrary.GetPixelShader(
                    pValue.ValueAsString(),
                    pEntryPointName.ValueAsString(),
                    pProfileName.ValueAsString(),
                    false,
                    pShaderMacros
            );
            ReadShaderSamplersAndTextures( mpPixelShader/*->GetBlob()*/, /*&mPixelShaderParameters*/mShaderParameters );
        }

        // load and store the compute shader if it was specified
        pValue = mConfigBlock.GetValueByName("ComputeShaderFile");
        if( pValue.IsValid() )
        {
            pEntryPointName = mConfigBlock.GetValueByName("ComputeShaderMain");
            pProfileName = mConfigBlock.GetValueByName("ComputeShaderProfile");
            mpComputeShader = pAssetLibrary.GetComputeShader(
                    pValue.ValueAsString(),
                    pEntryPointName.ValueAsString(),
                    pProfileName.ValueAsString(),
                    false,
                    pShaderMacros
            );
            ReadShaderSamplersAndTextures( mpComputeShader/*->GetBlob()*/, /*&mComputeShaderParameters*/ mShaderParameters);
        }

        // load and store the geometry shader if it was specified
        pValue = mConfigBlock.GetValueByName("GeometryShaderFile");
        if( pValue.IsValid() )
        {
            pEntryPointName = mConfigBlock.GetValueByName("GeometryShaderMain");
            pProfileName = mConfigBlock.GetValueByName("GeometryShaderProfile");
            mpGeometryShader = pAssetLibrary.GetGeometryShader(
                    pValue.ValueAsString(),
                    pEntryPointName.ValueAsString(),
                    pProfileName.ValueAsString(),
                    false,
                    pShaderMacros
            );
            ReadShaderSamplersAndTextures( mpGeometryShader/*->GetBlob()*/, /*&mGeometryShaderParameters*/ mShaderParameters);
        }

        // load and store the hull shader if it was specified
        pValue = mConfigBlock.GetValueByName("HullShaderFile");
        if( pValue.IsValid() )
        {
            pEntryPointName = mConfigBlock.GetValueByName("HullShaderMain");
            pProfileName = mConfigBlock.GetValueByName("HullShaderProfile");
            mpHullShader = pAssetLibrary.GetHullShader(
                    pValue.ValueAsString(),
                    pEntryPointName.ValueAsString(),
                    pProfileName.ValueAsString(),
                    false,
                    pShaderMacros
            );
            ReadShaderSamplersAndTextures( mpHullShader/*->GetBlob()*/, /*&mHullShaderParameters*/ mShaderParameters);
        }

        // load and store the domain shader if it was specified
        pValue = mConfigBlock.GetValueByName("DomainShaderFile");
        if( pValue.IsValid() )
        {
            pEntryPointName = mConfigBlock.GetValueByName("DomainShaderMain");
            pProfileName = mConfigBlock.GetValueByName("DomainShaderProfile");
            mpDomainShader = pAssetLibrary.GetDomainShader(
                    pValue.ValueAsString(),
                    pEntryPointName.ValueAsString(),
                    pProfileName.ValueAsString(),
                    false,
                    pShaderMacros
            );
            ReadShaderSamplersAndTextures( mpDomainShader/*->GetBlob()*/, /*&mDomainShaderParameters*/ mShaderParameters);
        }

        // load and store the render state file if it was specified
        pValue = mConfigBlock.GetValueByName("RenderStateFile");
        if( pValue.IsValid() )
        {
            mpRenderStateBlock = pAssetLibrary.GetRenderStateBlock(pValue.ValueAsString(), false);
        }

//        OUTPUT_BINDING_DEBUG_INFO( (_L("Bindings for : ") + mMaterialName + _L("\n")).c_str() );
        LogUtil.i(LogUtil.LogType.DEFAULT, ("Bindings for : ") + mMaterialName);

        String pShaderTypeNameList[] = {
                ("Pixel shader"),
                ("Compute shader"),
                ("Vertex shader"),
                ("Geometry shader"),
                ("Hull shader"),
                ("Domain shader"),
        };
//        cString *pShaderTypeName = pShaderTypeNameList;
//
//        void *pShaderList[] = {
//            mpPixelShader,
//                    mpComputeShader,
//                    mpVertexShader,
//                    mpGeometryShader,
//                    mpHullShader,
//                    mpDomainShader
//        };
//        void **pShader = pShaderList;

        // For each of the shader stages, bind shaders and buffers
        /*for( CPUTShaderParameters **pCur = mpShaderParametersList; *pCur; pCur++ ) // Bind textures and buffersfor each shader stage
        {
            // Clear the bindings to reduce "resource still bound" errors, and to avoid leaving garbage pointers between valid pointers.
            // We bind resources as arrays.  We bind from the min required bind slot to the max-required bind slot.
            // Any slots in between will get bound.  It isn't clear if D3D will simply ignore them, or not.
            // But, they could be garbage, or valid resources from a previous binding.
            memset( (*pCur)->mppBindViews,           0, sizeof((*pCur)->mppBindViews) );
            memset( (*pCur)->mppBindUAVs,            0, sizeof((*pCur)->mppBindUAVs) );
            memset( (*pCur)->mppBindConstantBuffers, 0, sizeof((*pCur)->mppBindConstantBuffers) );

            if( !*pShader++ )
            {
                pShaderTypeName++; // Increment the name pointer to remain coherent.
                continue;          // This shader not bound.  Don't waste time binding to it.
            }

            OUTPUT_BINDING_DEBUG_INFO( (*(pShaderTypeName++)  + _L("\n")).c_str() );
            BindTextures(        **pCur, pModel, meshIndex );
            BindBuffers(         **pCur, pModel, meshIndex );
            BindUAVs(            **pCur, pModel, meshIndex );
            BindConstantBuffers( **pCur, pModel, meshIndex );

            OUTPUT_BINDING_DEBUG_INFO( _L("\n") );
        }*/

        LogUtil.i(LogUtil.LogType.DEFAULT, "Bindings for : " + mMaterialName);
        String modelSuffix = pModel.toString();
        String meshSuffix = Integer.toString(meshIndex);

        BindTextures(        mShaderParameters, modelSuffix,  meshSuffix);
        BindBuffers(         mShaderParameters, modelSuffix,  meshSuffix );
        BindUAVs(            mShaderParameters, modelSuffix,  meshSuffix );
        BindConstantBuffers( mShaderParameters, modelSuffix,  meshSuffix );

//        return result;
    }

    protected void ReadShaderSamplersAndTextures(   ShaderProgram shader, CPUTShaderParameters pShaderParameter ){
        ProgramResources resources = GLSLUtil.getProgramResources(shader.getProgram());

        UniformProperty[] samplerUniforms = resources.active_uniform_properties;
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
//                        assert pShaderParameter.mTextureParameterCount>0:"Algorithm error";
//                        pShaderParameter.mTextureParameterCount--;
                    }
                    else
                    {
                        /*pShaderParameter.mpTextureParameterName[textureIndex] = strName;
                        pShaderParameter.mpTextureParameterBindPoint[textureIndex] = (Integer)property.value;
                        textureIndex++;*/
                        pShaderParameter.AddTexture(strName, (Integer)property.value);
                    }
                }else if(typeName.contains("image") || typeName.contains("imageBuffer")){ // unorder resource views
                    if( ignore )
                    {
                        /*assert pShaderParameter.mUAVParameterCount > 0 : "Algorithm error";
                        pShaderParameter.mUAVParameterCount--;*/
                    }
                    else
                    {
                        /*pShaderParameter.mpUAVParameterName[uavIndex] = strName;
                        pShaderParameter.mpUAVParameterBindPoint[uavIndex] = (Integer)property.value;
                        uavIndex++;*/
                        pShaderParameter.AddUnorderResourceView(strName, (Integer)property.value);
                    }
                }
            }
        }

        for(int i = 0; i < resources.uniformBlockProperties.size(); i++){
            UniformBlockProperties uniformBlock = resources.uniformBlockProperties.get(i);
            String strName = uniformBlock.name;
            boolean ignore = (strName.length() > 8) && (strName.substring(0, 8).equals("NONCPUT_"));
            if(uniformBlock.type == UniformBlockType.UNFIORM_BLOCK){  // const buffers
                if( ignore )
                {
                    /*assert pShaderParameter.mConstantBufferParameterCount > 0:"Algorithm error";
                    pShaderParameter.mConstantBufferParameterCount--;*/
                }
                else
                {
                    /*pShaderParameter.mpConstantBufferParameterName[constantBufferIndex] = strName;
                    pShaderParameter.mpConstantBufferParameterBindPoint[constantBufferIndex] = uniformBlock.binding;
                    constantBufferIndex++;*/
                    pShaderParameter.AddConstantBuffer(strName, uniformBlock.binding);
                }
            }else if(uniformBlock.type == UniformBlockType.UNIFORM_BUFFER){ // shader storage buffers
                if( ignore )
                {
                    /*assert pShaderParameter.mBufferParameterCount > 0 : "Algorithm error";
                    pShaderParameter.mBufferParameterCount--;*/
                }
                else
                {
                    /*pShaderParameter.mpBufferParameterName[bufferIndex] = strName;
                    pShaderParameter.mpBufferParameterBindPoint[bufferIndex] = uniformBlock.binding;
                    bufferIndex++;*/
                    pShaderParameter.AddBuffer(strName, uniformBlock.binding);
                }
            }
        }
    }

    // TODO: these "Bind*" functions are almost identical, except they use different params.  Can we combine?
    void SetTexture(String SlotName, String TextureName )
    {
        CPUTAssetLibraryDX11 pAssetLibrary = (CPUTAssetLibraryDX11)CPUTAssetLibrary.GetAssetLibrary();
        for(mShaderParameters.mTextureCount=0; mShaderParameters.mTextureCount < mShaderParameters.mpTextureParameters.size(); mShaderParameters.mTextureCount++)
        {
            int textureCount = mShaderParameters.mTextureCount;
            AttribBinder textureParameter = mShaderParameters.mpTextureParameters.get(textureCount);
            String tagName = textureParameter.attributeName /*mShaderParameters.mpTextureParameterName[textureCount]*/;

            if(tagName.equals(SlotName))
            {
                int bindPoint = textureParameter.index /*mShaderParameters.mpTextureParameterBindPoint[textureCount]*/;
                assert bindPoint < CPUTMaterial.CPUT_MATERIAL_MAX_TEXTURE_SLOTS : ("Texture bind point out of range.");

//                SAFE_RELEASE(mShaderParameters.mppBindViews[bindPoint]);
//                SAFE_RELEASE(mShaderParameters.mpTexture[textureCount]);

                try {
                    mShaderParameters.mpTexture[textureCount] = pAssetLibrary.GetTexture( TextureName, false,false);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                assert mShaderParameters.mpTexture[textureCount] != null: ("Failed getting texture ") + TextureName;

                mShaderParameters.mppBindViews[bindPoint] = ((CPUTTextureDX11)mShaderParameters.mpTexture[textureCount]).GetShaderResourceView();
//                mShaderParameters.mppBindViews[bindPoint]->AddRef();
            }
        }
    }

    void SetRenderStateBlock( String BlockName )
    {
        CPUTAssetLibraryDX11 pAssetLibrary = (CPUTAssetLibraryDX11)CPUTAssetLibrary.GetAssetLibrary();

//        mpRenderStateBlock.Release();

        mpRenderStateBlock = pAssetLibrary.GetRenderStateBlock(BlockName, false);
    }

    protected void BindTextures(CPUTShaderParameters params, String modelSuffix, String meshSuffix ) throws IOException{
        if( params.mpTextureParameters.size()  == 0) {return;}
        /*OUTPUT_BINDING_DEBUG_INFO( _L() );*/
        LogUtil.i(LogUtil.LogType.DEFAULT, "Bound Textures");

        CPUTAssetLibraryDX11 pAssetLibrary = (CPUTAssetLibraryDX11)CPUTAssetLibrary.GetAssetLibrary();

        for(params.mTextureCount=0; params.mTextureCount < params.mpTextureParameters.size(); params.mTextureCount++)
        {
            String textureName;
            int textureCount = params.mTextureCount;
            AttribBinder textureBind = params.mpTextureParameters.get(textureCount);
            String tagName = textureBind.attributeName;
            CPUTConfigEntry pValue = mConfigBlock.GetValueByName(tagName);
            if( !pValue.IsValid() )
            {
                // We didn't find our property in the file.  Is it in the global config block?
                pValue = CPUTMaterial.mGlobalProperties.GetValueByName(tagName);
            }
            /*ASSERT( pValue->IsValid(), L"Can't find texture '" + tagName + L"'." ); //  TODO: fix message*/
            if(!pValue.IsValid())
                throw new RuntimeException("Can't find texture '" + tagName + "'.");
            textureName = pValue.ValueAsString();

            int bindPoint = textureBind.index;
            if(bindPoint >= CPUTMaterial.CPUT_MATERIAL_MAX_TEXTURE_SLOTS)
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

//-----------------------------------------------------------------------------
    /*void BindTextures( CPUTShaderParameters params, CPUTModel pModel, int meshIndex )
    {
        CPUTAssetLibraryDX11 pAssetLibrary = (CPUTAssetLibraryDX11)CPUTAssetLibrary.GetAssetLibrary();

        for(params.mTextureCount=0; params.mTextureCount < params.mpTextureParameters.size(); params.mTextureCount++)
        {
            String textureName;
            int textureCount = params.mTextureCount;
            String tagName = params.mpTextureParameters.get(textureCount).attributeName;

            CPUTConfigEntry pValue = mConfigBlock.GetValueByName(tagName);
            if( !pValue.IsValid() )
            {
                // We didn't find our property in the file.  Is it in the global config block?
                pValue = CPUTMaterial.mGlobalProperties.GetValueByName(tagName);
            }
            assert pValue.IsValid():"Can't find texture '" + tagName + "'."; //  TODO: fix message
            textureName = pValue.ValueAsString();
            // If the texture name not specified.  Load default.dds instead
            if( 0 == textureName.length() ) { textureName = "default.dds"; }

            int bindPoint = params.mpTextureParameters.get(textureCount).index;
            assert  bindPoint < CPUTMaterial.CPUT_MATERIAL_MAX_TEXTURE_SLOTS : "Texture bind point out of range.";

            params.mBindViewMin = Math.min( params.mBindViewMin, bindPoint );
            params.mBindViewMax = Math.max( params.mBindViewMax, bindPoint );

            if( textureName.charAt(0) == '@' )
            {
                // This is a per-mesh value.  Add to per-mesh list.
                textureName += ptoc(pModel) + itoc(meshIndex);
            } else if( textureName[0] == '#' )
            {
                // This is a per-mesh value.  Add to per-mesh list.
                textureName += ptoc(pModel);
            }

            // Get the sRGB flag (default to true)
            cString SRGBName = tagName+_L("sRGB");
            CPUTConfigEntry *pSRGBValue = mConfigBlock.GetValueByName(SRGBName);
            bool loadAsSRGB = pSRGBValue->IsValid() ?  loadAsSRGB = pSRGBValue->ValueAsBool() : true;

            if( !params.mpTexture[textureCount] )
            {
                params.mpTexture[textureCount] = pAssetLibrary->GetTexture( textureName, false, loadAsSRGB );
                ASSERT( params.mpTexture[textureCount], _L("Failed getting texture ") + textureName);
            }

            // The shader file (e.g. .fx) can specify the texture bind point (e.g., t0).  Those specifications
            // might not be contiguous, and there might be gaps (bind points without assigned textures)
            // TODO: Warn about missing bind points?
            params.mppBindViews[bindPoint] = ((CPUTTextureDX11*)params.mpTexture[textureCount])->GetShaderResourceView();
            params.mppBindViews[bindPoint]->AddRef();

            OUTPUT_BINDING_DEBUG_INFO( (itoc(bindPoint) + _L(" : ") + params.mpTexture[textureCount]->GetName() + _L("\n")).c_str() );
        }
    }*/

    protected void BindBuffers(         CPUTShaderParameters params, String modelSuffix, String meshSuffix ){

        if( params.mpBufferParameters.size() == 0 ) { return; }
        /*OUTPUT_BINDING_DEBUG_INFO( _L("Bound Buffers") );*/
        LogUtil.i(LogUtil.LogType.DEFAULT, "Bound Buffers");

        CPUTAssetLibraryDX11 pAssetLibrary = (CPUTAssetLibraryDX11)CPUTAssetLibrary.GetAssetLibrary();
        for(params.mBufferCount=0; params.mBufferCount < params.mpBufferParameters.size(); params.mBufferCount++)
        {
            String bufferName;
            int bufferCount = params.mBufferCount;
            AttribBinder bufferBind = params.mpBufferParameters.get(bufferCount);
            String tagName = bufferBind.attributeName;
            {
                CPUTConfigEntry pValue = mConfigBlock.GetValueByName(tagName);
                if( !pValue.IsValid() )
                {
                    // We didn't find our property in the file.  Is it in the global config block?
                    pValue = CPUTMaterial.mGlobalProperties.GetValueByName(tagName);
                }
//                ASSERT( pValue->IsValid(), L"Can't find buffer '" + tagName + L"'." ); //  TODO: fix message
                if(!pValue.IsValid()){
                    throw new RuntimeException("Can't find buffer '" + tagName + "'.");
                }

                bufferName = pValue.ValueAsString();
            }
            int bindPoint = bufferBind.index;
//            ASSERT( bindPoint < CPUT_MATERIAL_MAX_BUFFER_SLOTS, _L("Buffer bind point out of range.") );
            if(bindPoint >= CPUTMaterial.CPUT_MATERIAL_MAX_BUFFER_SLOTS)
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

    protected void BindUAVs(CPUTShaderParameters params, String modelSuffix, String meshSuffix ){
        if( params.mpUAVParameters.size() ==0) { return; }
        LogUtil.i(LogUtil.LogType.DEFAULT, "Bound UAVs");

        CPUTAssetLibraryDX11 pAssetLibrary = (CPUTAssetLibraryDX11)CPUTAssetLibrary.GetAssetLibrary();
        for(params.mUAVCount=0; params.mUAVCount < params.mpUAVParameters.size(); params.mUAVCount++)
        {
            String uavName;
            int uavCount = params.mUAVCount;
            AttribBinder uavBind = params.mpUAVParameters.get(uavCount);
            String tagName = uavBind.attributeName;
            {
                CPUTConfigEntry pValue = mConfigBlock.GetValueByName(tagName);
                if( !pValue.IsValid() )
                {
                    // We didn't find our property in the file.  Is it in the global config block?
                    pValue = CPUTMaterial.mGlobalProperties.GetValueByName(tagName);
                }
                /*ASSERT( pValue->IsValid(), L"Can't find UAV '" + tagName + L"'." ); //  TODO: fix message*/
                if(!pValue.IsValid())
                    throw new RuntimeException("Can't find UAV '" + tagName + "'.");
                uavName = pValue.ValueAsString();
            }
            int bindPoint = uavBind.index;
//            ASSERT( bindPoint < CPUT_MATERIAL_MAX_UAV_SLOTS, _L("UAV bind point out of range.") );
            if(bindPoint >= CPUTMaterial.CPUT_MATERIAL_MAX_UAV_SLOTS){
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
        if( params.mpConstantBufferParameters.size() ==0) { return; }
        LogUtil.i(LogUtil.LogType.DEFAULT, "Bound Constant Buffers");

        CPUTAssetLibraryDX11 pAssetLibrary = (CPUTAssetLibraryDX11)CPUTAssetLibrary.GetAssetLibrary();
        for(params.mConstantBufferCount=0; params.mConstantBufferCount < params.mpConstantBufferParameters.size(); params.mConstantBufferCount++)
        {
            String constantBufferName;
            int constantBufferCount = params.mConstantBufferCount;
            AttribBinder constBufferBind = params.mpConstantBufferParameters.get(constantBufferCount);
            String tagName = constBufferBind.attributeName;
            if(tagName == null){
                throw new NullPointerException();
            }

            {
                CPUTConfigEntry pValue = mConfigBlock.GetValueByName(tagName);
                if( !pValue.IsValid() )
                {
                    // We didn't find our property in the file.  Is it in the global config block?
                    pValue = CPUTMaterial.mGlobalProperties.GetValueByName(tagName);
                }
//                ASSERT( pValue->IsValid(), L"Can't find constant buffer '" + tagName + L"'." ); //  TODO: fix message
                if(!pValue.IsValid()){
                    throw new RuntimeException("Can't find constant buffer '" + tagName + "'.");
                }

                constantBufferName = pValue.ValueAsString();
            }
            int bindPoint = constBufferBind.index;
//            ASSERT( bindPoint < CPUT_MATERIAL_MAX_CONSTANT_BUFFER_SLOTS, _L("Constant buffer bind point out of range.") );
            if(!(bindPoint < CPUTMaterial.CPUT_MATERIAL_MAX_CONSTANT_BUFFER_SLOTS)){
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
            params.mppBindConstantBuffers[bindPoint]   = ((CPUTBufferDX11)params.mpConstantBuffer[constantBufferCount]).GetNativeBuffer();

//            OUTPUT_BINDING_DEBUG_INFO( (itoc(bindPoint) + _L(" : ") + params.mpConstantBuffer[constantBufferCount]->GetName() + _L("\n")).c_str() );
        }
//        OUTPUT_BINDING_DEBUG_INFO( _L("\n") );
    }

    private void setShaderResources(CPUTRenderParametersDX context){
        /*mProgram.setVS(mpVertexShader);
        mProgram.setTC(mpHullShader);
        mProgram.setTE(mpDomainShader);
        mProgram.setGS(mpGeometryShader);
        mProgram.setPS(mpPixelShader);
        mProgram.setCS(mpComputeShader);*/

        context.VSSetShader(mpVertexShader);
        context.PSSetShader(mpPixelShader);

        if(mShaderParameters.mTextureCount > 0){
            for( int ii=0; ii < mShaderParameters.mTextureCount; ii++ )
            {
                int bindPoint = mShaderParameters.mpTextureParameters.get(ii).index;
//                if(mpLastShaderViews[bindPoint] != mShaderParameters.mppBindViews[bindPoint] )
                {
                    /*mpLast##SHADER##ShaderViews[bindPoint] = m##SHADER##ShaderParameters.mppBindViews[bindPoint];*/
                    TextureGL shaderResource = (TextureGL) mShaderParameters.mppBindViews[bindPoint];
                    gl.glActiveTexture(GLenum.GL_TEXTURE0 + bindPoint);
                    gl.glBindTexture(shaderResource.getTarget(), shaderResource.getTexture());
                    mpLastShaderViews[bindPoint] = shaderResource;
                }
            }
        }

        if(mShaderParameters.mConstantBufferCount > 0){
            for( int ii=0; ii < mShaderParameters.mConstantBufferCount; ii++ ){
                int bindPoint = mShaderParameters.mpConstantBufferParameters.get(ii).index;
//                if(mpLastShaderConstantBuffers[bindPoint] != mShaderParameters.mppBindConstantBuffers[bindPoint])
                {
                    BufferGL constantBuffer = mShaderParameters.mppBindConstantBuffers[bindPoint];

                    if(constantBuffer != null) {
                        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, bindPoint, constantBuffer.getBuffer());
                    }else{
                        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, bindPoint, 0);
                    }
//                    mpLastShaderConstantBuffers[bindPoint] = constantBuffer;
                }
            }
        }
    }
}
