package jet.opengl.demos.intel.cput;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.texture.TextureGL;

/**
 * Created by mazhen'gui on 2017/11/15.
 */

final class CPUTShaderParameters implements Disposeable{
    int                       mTextureCount;
    String[]                  mpTextureParameterName;
    int[]                     mpTextureParameterBindPoint;
    int                       mTextureParameterCount;
    CPUTTexture[]             mpTexture = new CPUTTexture[CPUTMaterial.CPUT_MATERIAL_MAX_TEXTURE_SLOTS];   // samplerXXX
    CPUTBuffer[]              mpBuffer = new CPUTBuffer[CPUTMaterial.CPUT_MATERIAL_MAX_BUFFER_SLOTS];      // uniform buffer or shader storege buffer
    CPUTBuffer[]              mpUAV =new CPUTBuffer[CPUTMaterial.CPUT_MATERIAL_MAX_UAV_SLOTS];                             // imageXXX
    CPUTBuffer[]              mpConstantBuffer = new CPUTBuffer[CPUTMaterial.CPUT_MATERIAL_MAX_CONSTANT_BUFFER_SLOTS];

    String[]                  mpSamplerParameterName;
    int[]                     mpSamplerParameterBindPoint;
    int                       mSamplerParameterCount;

    int                       mBufferCount;
    int                       mBufferParameterCount;
    String[]                  mpBufferParameterName;
    int[]                     mpBufferParameterBindPoint;

    int                       mUAVCount;
    int                       mUAVParameterCount;
    String[]                  mpUAVParameterName;
    int[]                     mpUAVParameterBindPoint;

    int                       mConstantBufferCount;
    int                       mConstantBufferParameterCount;
    String[]                  mpConstantBufferParameterName;
    int[]                     mpConstantBufferParameterBindPoint;

    int                       mBindViewMin = CPUTMaterial.CPUT_MATERIAL_MAX_SRV_SLOTS;
    int                       mBindViewMax;

    int                       mBindUAVMin = CPUTMaterial.CPUT_MATERIAL_MAX_UAV_SLOTS;
    int                       mBindUAVMax;

    int                       mBindConstantBufferMin;
    int                       mBindConstantBufferMax;

    Object[]               mppBindViews = new Object[CPUTMaterial.CPUT_MATERIAL_MAX_SRV_SLOTS];  // It can be textures or buffers.
    TextureGL[]               mppBindUAVs = new TextureGL[CPUTMaterial.CPUT_MATERIAL_MAX_UAV_SLOTS];
    BufferGL[]                mppBindConstantBuffers = new BufferGL[CPUTMaterial.CPUT_MATERIAL_MAX_CONSTANT_BUFFER_SLOTS];


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
