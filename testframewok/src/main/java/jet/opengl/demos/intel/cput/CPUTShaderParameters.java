package jet.opengl.demos.intel.cput;

import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.shader.AttribBinder;
import jet.opengl.postprocessing.texture.TextureGL;

/**
 * Created by mazhen'gui on 2017/11/15.
 */

final class CPUTShaderParameters implements Disposeable{
    int                       mTextureCount;
    final List<AttribBinder>  mpTextureParameters = new ArrayList<>();

    CPUTTexture[]             mpTexture = new CPUTTexture[CPUTMaterial.CPUT_MATERIAL_MAX_TEXTURE_SLOTS];   // samplerXXX
    CPUTBuffer[]              mpBuffer = new CPUTBuffer[CPUTMaterial.CPUT_MATERIAL_MAX_BUFFER_SLOTS];      // uniform buffer or shader storege buffer
    CPUTBuffer[]              mpUAV =new CPUTBuffer[CPUTMaterial.CPUT_MATERIAL_MAX_UAV_SLOTS];                             // imageXXX
    CPUTBuffer[]              mpConstantBuffer = new CPUTBuffer[CPUTMaterial.CPUT_MATERIAL_MAX_CONSTANT_BUFFER_SLOTS];

    String[]                  mpSamplerParameterName;
    int[]                     mpSamplerParameterBindPoint;
    int                       mSamplerParameterCount;

    int                       mBufferCount;
    final List<AttribBinder>  mpBufferParameters = new ArrayList<>();

    int                       mUAVCount;
    final List<AttribBinder>  mpUAVParameters = new ArrayList<>();

    int                       mConstantBufferCount;
    final List<AttribBinder>  mpConstantBufferParameters = new ArrayList<>();

    int                       mBindViewMin = CPUTMaterial.CPUT_MATERIAL_MAX_SRV_SLOTS;
    int                       mBindViewMax;

    int                       mBindUAVMin = CPUTMaterial.CPUT_MATERIAL_MAX_UAV_SLOTS;
    int                       mBindUAVMax;

    int                       mBindConstantBufferMin;
    int                       mBindConstantBufferMax;

    Object[]                  mppBindViews = new Object[CPUTMaterial.CPUT_MATERIAL_MAX_SRV_SLOTS];  // It can be textures or buffers.
    TextureGL[]               mppBindUAVs = new TextureGL[CPUTMaterial.CPUT_MATERIAL_MAX_UAV_SLOTS];
    BufferGL[]                mppBindConstantBuffers = new BufferGL[CPUTMaterial.CPUT_MATERIAL_MAX_CONSTANT_BUFFER_SLOTS];

    public void AddTexture(String name, int binding){
        if(name == null || binding < 0)
            throw new IllegalArgumentException();

        AttribBinder newValue = new AttribBinder(name, binding);

        if(!mpTextureParameters.contains(newValue)){
            mpTextureParameters.add(newValue);
        }
    }

    public void AddBuffer(String name, int binding){
        if(name == null || binding < 0)
            throw new IllegalArgumentException();

        AttribBinder newValue = new AttribBinder(name, binding);
        if(!mpBufferParameters.contains(newValue)){
            mpBufferParameters.add(newValue);
        }
    }

    public void AddConstantBuffer(String name, int binding){
        if(name == null || binding < 0)
            throw new IllegalArgumentException();

        AttribBinder newValue = new AttribBinder(name, binding);
        if(!mpConstantBufferParameters.contains(newValue)){
            mpConstantBufferParameters.add(newValue);
        }
    }

    public void AddUnorderResourceView(String name, int binding){
        if(name == null || binding < 0)
            throw new IllegalArgumentException();

        AttribBinder newValue = new AttribBinder(name, binding);

        if(!mpUAVParameters.contains(newValue)){
            mpUAVParameters.add(newValue);
        }
    }

    @Override
    public void dispose() {
        for(int ii=0; ii<CPUTMaterial.CPUT_MATERIAL_MAX_TEXTURE_SLOTS; ii++)
        {
            SAFE_RELEASE((Disposeable)mppBindViews[ii]);
            SAFE_RELEASE(mpTexture[ii]);
            SAFE_RELEASE(mpBuffer[ii]);

            mppBindViews[ii] = null;
            mpTexture[ii] = null;
            mpBuffer[ii] = null;
        }
        for(int ii=0; ii<CPUTMaterial.CPUT_MATERIAL_MAX_UAV_SLOTS; ii++)
        {
            SAFE_RELEASE(mppBindUAVs[ii]);
            SAFE_RELEASE(mpUAV[ii]);

            mppBindUAVs[ii] = null;
            mpUAV[ii] =null;
        }
        for(int ii=0; ii<CPUTMaterial.CPUT_MATERIAL_MAX_CONSTANT_BUFFER_SLOTS; ii++)
        {
            SAFE_RELEASE(mppBindConstantBuffers[ii]);
            SAFE_RELEASE(mpConstantBuffer[ii]);
            mppBindConstantBuffers[ii] = null;
            mpConstantBuffer[ii] = null;
        }
        /*SAFE_DELETE_ARRAY(mpTextureParameterName);
        SAFE_DELETE_ARRAY(mpTextureParameterBindPoint);
        SAFE_DELETE_ARRAY(mpSamplerParameterName);
        SAFE_DELETE_ARRAY(mpSamplerParameterBindPoint);
        SAFE_DELETE_ARRAY(mpBufferParameterName);
        SAFE_DELETE_ARRAY(mpBufferParameterBindPoint);
        SAFE_DELETE_ARRAY(mpUAVParameterName);
        SAFE_DELETE_ARRAY(mpUAVParameterBindPoint);
        SAFE_DELETE_ARRAY(mpConstantBufferParameterName);
        SAFE_DELETE_ARRAY(mpConstantBufferParameterBindPoint)*/
    }
}
