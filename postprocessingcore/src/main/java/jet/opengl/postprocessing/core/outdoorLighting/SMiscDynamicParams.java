package jet.opengl.postprocessing.core.outdoorLighting;

import org.lwjgl.util.vector.Vector2f;

import java.nio.ByteBuffer;

final class SMiscDynamicParams implements org.lwjgl.util.vector.Readable{

	static final int SIZE = 3 * 16;
	
	float fMaxStepsAlongRay;   // Maximum number of steps during ray tracing
    float fCascadeInd;
    final Vector2f f2WQ = new Vector2f(); // Used when pre-computing inscattering look-up table

    int uiDepthSlice;
    float fElapsedTime;
    float f2Dummy0, f2Dummy1;
    
    int ui4SrcMinMaxLevelXOffset;
    int ui4SrcMinMaxLevelYOffset;
    int ui4DstMinMaxLevelXOffset;
    int ui4DstMinMaxLevelYOffset;
    
    public ByteBuffer store(ByteBuffer buf){
    	buf.putFloat(fMaxStepsAlongRay);
    	buf.putFloat(fCascadeInd);
    	f2WQ.store(buf);
    	buf.putInt(uiDepthSlice);
    	buf.putFloat(fElapsedTime);
    	buf.putFloat(f2Dummy0);
    	buf.putFloat(f2Dummy1);
    	buf.putInt(ui4SrcMinMaxLevelXOffset);
    	buf.putInt(ui4SrcMinMaxLevelYOffset);
    	buf.putInt(ui4DstMinMaxLevelXOffset);
    	buf.putInt(ui4DstMinMaxLevelYOffset);
    	
    	return buf;
    }
}
