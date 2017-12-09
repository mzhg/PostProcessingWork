package jet.opengl.demos.nvidia.hbaoplus;

import org.lwjgl.util.vector.ReadableVector4f;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.util.CacheBuffer;

final class PerPassConstantBuffer extends BaseConstantBuffer{

	final PerPassConstantStruct m_Data = new PerPassConstantStruct();
	
	public PerPassConstantBuffer() {
		super(PerPassConstantStruct.SIZE, BINDING_POINT_PER_PASS_UBO);
	}
	
	void setOffset(int OffsetX, int OffsetY)
    {
        m_Data.f2Offset.x = OffsetX + 0.5f;
        m_Data.f2Offset.y = OffsetY + 0.5f;
    }

    void setJitter(ReadableVector4f Jitter)
    {
        m_Data.f4Jitter.set(Jitter);
    }

    void setSliceIndex(int SliceIndex)
    {
        m_Data.fSliceIndex = SliceIndex;
        m_Data.uSliceIndex = SliceIndex;
    }
    
    void updateBuffer(){
    	ByteBuffer buf = CacheBuffer.getCachedByteBuffer(PerPassConstantStruct.SIZE);
    	m_Data.store(buf);
    	buf.flip();
    	
    	updateCB(buf);
    }
}
