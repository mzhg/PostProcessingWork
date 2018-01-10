package jet.opengl.demos.intel.va;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jet.opengl.demos.intel.cput.D3D11_INPUT_ELEMENT_DESC;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.util.LogUtil;

/**
 * Created by mazhen'gui on 2017/11/21.
 */

public class VaRenderMaterialManagerDX11 extends VaRenderMaterialManager implements VaDirectXNotifyTarget {
    private final Map< VaRenderMaterialCachedShadersDX11.Key, VaRenderMaterialCachedShadersDX11 > m_cachedShaders = new HashMap<>();
    private int m_storeageIndex;

    protected VaRenderMaterialManagerDX11(){
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

    private static String constructShaderFileName(String fileName, String shaderModel, String entryName){
        String ext;
        if(shaderModel.startsWith("vs")){
            ext = "vert";
        }else if(shaderModel.startsWith("ps")){
            ext = "frag";
        }else{
            throw new IllegalArgumentException("unkowm shaderModel: " + shaderModel);
        }

        return String.format(fileName, entryName, ext);
    }

    public VaRenderMaterialCachedShadersDX11 FindOrCreateShaders( String fileName, boolean alphaTest, String entryVS_PosOnly, String entryPS_DepthOnly,
                                                                  String entryVS_Standard, String entryPS_Forward, String entryPS_Deferred,
                                                                  List<Macro> shaderMacros )
    {
        VaRenderMaterialCachedShadersDX11.Key cacheKey = new VaRenderMaterialCachedShadersDX11.Key
        ( fileName, alphaTest, entryVS_PosOnly, entryPS_DepthOnly, entryVS_Standard, entryPS_Forward, entryPS_Deferred, shaderMacros );

        VaRenderMaterialCachedShadersDX11 it = m_cachedShaders.get( cacheKey );

        // in cache but no longer used by anyone so it was destroyed
        /*if( (it != *//*m_cachedShaders.end()*//* null) && it*//*->second*//*.expired() )
        {
            m_cachedShaders.erase( it );
            it = m_cachedShaders.end();
        }*/

        // not in cache
        if( it == /*m_cachedShaders.end()*/ null )
        {
            VaRenderMaterialCachedShadersDX11 newShaders = new VaRenderMaterialCachedShadersDX11();

            final int D3D11_APPEND_ALIGNED_ELEMENT = 0;
            final int D3D11_INPUT_PER_VERTEX_DATA = 0;

            D3D11_INPUT_ELEMENT_DESC[] inputElements= new D3D11_INPUT_ELEMENT_DESC[6];
            inputElements[0] = ( VaDirectXTools.CD3D11_INPUT_ELEMENT_DESC( "SV_Position", 0, GLenum.GL_RGB32F, 0, D3D11_APPEND_ALIGNED_ELEMENT, D3D11_INPUT_PER_VERTEX_DATA, 0 ) );
            inputElements[1] = ( VaDirectXTools.CD3D11_INPUT_ELEMENT_DESC( "COLOR", 0, GLenum.GL_RGBA8, 0, D3D11_APPEND_ALIGNED_ELEMENT, D3D11_INPUT_PER_VERTEX_DATA, 0 ) );
            inputElements[2] = ( VaDirectXTools.CD3D11_INPUT_ELEMENT_DESC( "NORMAL", 0, GLenum.GL_RGBA32F, 0, D3D11_APPEND_ALIGNED_ELEMENT, D3D11_INPUT_PER_VERTEX_DATA, 0 ) );
            inputElements[3] = ( VaDirectXTools.CD3D11_INPUT_ELEMENT_DESC( "TANGENT", 0, GLenum.GL_RGBA32F, 0, D3D11_APPEND_ALIGNED_ELEMENT, D3D11_INPUT_PER_VERTEX_DATA, 0 ) );
            inputElements[4] = ( VaDirectXTools.CD3D11_INPUT_ELEMENT_DESC( "TEXCOORD", 0, GLenum.GL_RG32F, 0, D3D11_APPEND_ALIGNED_ELEMENT, D3D11_INPUT_PER_VERTEX_DATA, 0 ) );
            inputElements[5] = ( VaDirectXTools.CD3D11_INPUT_ELEMENT_DESC( "TEXCOORD", 1, GLenum.GL_RG32F, 0, D3D11_APPEND_ALIGNED_ELEMENT, D3D11_INPUT_PER_VERTEX_DATA, 0 ) );

            newShaders.VS_PosOnly.CreateShaderAndILFromFile( constructShaderFileName(fileName, "vs_5_0",entryVS_PosOnly), "vs_5_0", entryVS_PosOnly, shaderMacros, inputElements/*, (uint32)inputElements.size( )*/ );
            newShaders.VS_Standard.CreateShaderAndILFromFile( constructShaderFileName(fileName, "vs_5_0",entryVS_Standard),"vs_5_0", entryVS_Standard, shaderMacros, inputElements/*, (uint32)inputElements.size( )*/ );
            if( alphaTest )
                newShaders.PS_DepthOnly.CreateShaderFromFile( constructShaderFileName(fileName,"ps_5_0",entryPS_DepthOnly), "ps_5_0", entryPS_DepthOnly, shaderMacros );
            else
                newShaders.PS_DepthOnly.Clear( );
            newShaders.PS_Forward.CreateShaderFromFile( constructShaderFileName(fileName, "ps_5_0",entryPS_Forward),"ps_5_0", entryPS_Forward, shaderMacros );
            newShaders.PS_Deferred.CreateShaderFromFile( constructShaderFileName(fileName,"ps_5_0",entryPS_Deferred), "ps_5_0", entryPS_Deferred, shaderMacros );

//            m_cachedShaders.insert( std::make_pair( cacheKey, newShaders ) );
            m_cachedShaders.put(cacheKey, newShaders);

            LogUtil.i(LogUtil.LogType.DEFAULT, "Create a VaRenderMaterialCachedShadersDX11, the size of the cache is " + m_cachedShaders.size());

            return newShaders;
        }
        else
        {
//            return it->second.lock();
            return it;
        }
    }
}
