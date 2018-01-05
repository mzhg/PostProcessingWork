package jet.opengl.demos.intel.va;

/**
 * Created by mazhen'gui on 2017/11/18.
 */

public abstract class VaRenderingGlobals extends VaRenderingModuleImpl implements VaDirectXNotifyTarget{
    public static final int            c_shaderDebugFloatOutputCount = VaShaderDefine.SHADERGLOBAL_DEBUG_FLOAT_OUTPUT_COUNT;
    public static final int            c_shaderDebugOutputSyncDelay  = 4;

    protected float[]  m_shaderDebugFloats;

    protected final VaTexture[]  m_shaderDebugOutputGPUTextures = new VaTexture[c_shaderDebugOutputSyncDelay];
    protected final VaTexture[]  m_shaderDebugOutputCPUTextures = new VaTexture[c_shaderDebugOutputSyncDelay];

    protected int                m_frameIndex;

    protected VaRenderingGlobals(){}

    @Override
    public void OnDeviceCreated() {
        /*for( int i = 0; i < c_shaderDebugOutputSyncDelay; i++ )
        {
            m_shaderDebugOutputGPUTextures[i] = VaTexture.Create1D( VaTexture.R32_FLOAT, c_shaderDebugFloatOutputCount, 1, 1,
                    *//*vaTextureBindSupportFlags::UnorderedAccess*//*VaTexture.BSF_UnorderedAccess , *//*vaTextureAccessFlags::None*//*0, null );
            m_shaderDebugOutputCPUTextures[i] = VaTexture.Create1D( VaTexture.R32_FLOAT, c_shaderDebugFloatOutputCount, 1, 1,
                    *//*vaTextureBindSupportFlags::None*//* 0, *//*vaTextureAccessFlags::CPURead*//*0, null );
        }*/

        for( int i = 0; i < c_shaderDebugFloatOutputCount; i++ )
        {
//            m_shaderDebugFloats[i] = 0.0f;
        }

        m_frameIndex = 0;
    }

    // array size is c_shaderDebugFloatOutputCount
    public  float[]  GetShaderDebugFloatOutput( ) { return m_shaderDebugFloats; }
    public int GetShaderDebugFloatOutput( float[] fltArray, int offset ){
//        fltArray = m_shaderDebugFloats; fltArrayCount = SHADERGLOBAL_DEBUG_FLOAT_OUTPUT_COUNT;

        System.arraycopy(m_shaderDebugFloats, 0, fltArray, 0, Math.min(fltArray.length - offset, m_shaderDebugFloats.length));
        return m_shaderDebugFloats.length;
    }


    public void Tick( float deltaTime ){
        m_frameIndex ++;
    }
    public int  GetFrameIndex( )        { return m_frameIndex; }

    public abstract void                SetAPIGlobals(VaDrawContext drawContext );

    // if calling more than once per frame make sure c_shaderDebugOutputSyncDelay is big enough to avoid stalls
    public abstract void                UpdateDebugOutputFloats( VaDrawContext drawContext );

    protected void                        MarkAPIGlobalsUpdated( VaDrawContext drawContext )                                    { drawContext.renderingGlobalsUpdated = true; }
}
