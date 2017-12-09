package jet.opengl.demos.nvidia.hbaoplus;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;

import java.nio.ByteBuffer;

final class GlobalConstantStruct {
	
	static final int SIZE = 14 * 16;

	final int[] u4BuildVersion = new int[4];					// 1
	final Vector2f f2InvQuarterResolution = new Vector2f();
	final Vector2f f2InvFullResolution = new Vector2f();		// 2
	
	final Vector2f f2UVToViewA = new Vector2f();
	final Vector2f f2UVToViewB = new Vector2f();				// 3
	
	float fRadiusToScreen;
	float fR2;
	float fNegInvR2;
	float fNDotVBias;											// 4
	
	float fSmallScaleAOAmount;
	float fLargeScaleAOAmount;
	float fPowExponent;
	int iUnused;												// 5
	
	float fBlurViewDepth0;
	float fBlurViewDepth1;
	float fBlurSharpness0;
	float fBlurSharpness1;										// 6
	
	float fLinearizeDepthA;
	float fLinearizeDepthB;
	float fInverseDepthRangeA;
	float fInverseDepthRangeB;									// 7
	
	final Vector2f f2InputViewportTopLeft = new Vector2f();
	float fViewDepthThresholdNegInv;
	float fViewDepthThresholdSharpness;							// 8
	
	float fBackgroundAORadiusPixels;
	float fForegroundAORadiusPixels;
	int   iDebugNormalComponent;
	float pad;													// 9
	
	final Matrix4f f44NormalMatrix = new Matrix4f();			// 13
	float fNormalDecodeScale;
	float fNormalDecodeBias;									// 14
	
	void store(ByteBuffer buf){
		for(int i = 0; i < u4BuildVersion.length; i++)
			buf.putInt(u4BuildVersion[i]);
		f2InvQuarterResolution.store(buf);
		f2InvFullResolution.store(buf);
		f2UVToViewA.store(buf);
		f2UVToViewB.store(buf);
		buf.putFloat(fRadiusToScreen);
		buf.putFloat(fR2);
		buf.putFloat(fNegInvR2);
		buf.putFloat(fNDotVBias);
		buf.putFloat(fSmallScaleAOAmount);
		buf.putFloat(fLargeScaleAOAmount);
		buf.putFloat(fPowExponent);
		buf.putInt(iUnused);
		buf.putFloat(fBlurViewDepth0);
		buf.putFloat(fBlurViewDepth1);
		buf.putFloat(fBlurSharpness0);
		buf.putFloat(fBlurSharpness1);
		buf.putFloat(fLinearizeDepthA);
		buf.putFloat(fLinearizeDepthB);
		buf.putFloat(fInverseDepthRangeA);
		buf.putFloat(fInverseDepthRangeB);
		f2InputViewportTopLeft.store(buf);
		buf.putFloat(fViewDepthThresholdNegInv);
		buf.putFloat(fViewDepthThresholdSharpness);
		buf.putFloat(fBackgroundAORadiusPixels);
		buf.putFloat(fForegroundAORadiusPixels);
		buf.putInt(iDebugNormalComponent);
		buf.putFloat(pad);
		f44NormalMatrix.store(buf);
		buf.putFloat(fNormalDecodeScale);
		buf.putFloat(fNormalDecodeBias);
		buf.putFloat(0).putFloat(0);  // padding 
	}
}
