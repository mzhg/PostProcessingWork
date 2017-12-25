package jet.opengl.demos.nvidia.hbaoplus;

import org.lwjgl.util.vector.ReadableVector4f;

final class PerPassConstantBuffers {

	private final RandomTexture m_RandomTexture = new RandomTexture();
	private final PerPassConstantBuffer[] m_CBs = new PerPassConstantBuffer[16];
	
	public PerPassConstantBuffers(){
		for(int i = 0; i < m_CBs.length; i++){
			m_CBs[i] = new PerPassConstantBuffer();
		}
	}
	
	private ReadableVector4f getJitterVector(int JitterX, int JitterY)
    {
        // To match the reference D3D11 implementation
        JitterY = 3 - JitterY;
        return m_RandomTexture.getJitter(JitterY * 4 + JitterX);
    }
	
	void create(/*const GFSDK_SSAO_GLFunctions& GL*/)
    {
        for (int SliceIndex = 0; SliceIndex < m_CBs.length; ++SliceIndex)
        {
        	if(m_CBs[SliceIndex] == null)
        		m_CBs[SliceIndex] = new PerPassConstantBuffer();
        	
//            m_CBs[SliceIndex].create(/*GL*/);

            int JitterX = SliceIndex % 4;
            int JitterY = SliceIndex / 4;

            m_CBs[SliceIndex].setOffset(JitterX, JitterY);
            m_CBs[SliceIndex].setJitter(getJitterVector(JitterX, JitterY));
            m_CBs[SliceIndex].setSliceIndex(SliceIndex);
            m_CBs[SliceIndex].updateBuffer(/*GL*/);
        }
    }
    void release(/*const GFSDK_SSAO_GLFunctions& GL*/)
    {
        for (int PassIndex = 0; PassIndex < m_CBs.length; ++PassIndex)
        {
        	if(m_CBs[PassIndex] == null)
        		continue;
        	
            m_CBs[PassIndex].release(/*GL*/);
        }
    }

    PerPassConstantBuffer getCB(int passIndex){ return m_CBs[passIndex]; }

    int getBufferId(int PassIndex)
    {
//        ASSERT(PassIndex < SIZEOF_ARRAY(m_CBs));
        return m_CBs[PassIndex].getBufferId();
    }
    int GetBindingPoint()
    {
        return m_CBs[0].getBindingPoint();
    }
    void unbind(/*const GFSDK_SSAO_GLFunctions& GL*/)
    {
        m_CBs[0].unbind(/*GL*/);
    }
}
