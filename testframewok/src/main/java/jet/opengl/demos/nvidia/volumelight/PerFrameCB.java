package jet.opengl.demos.nvidia.volumelight;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
import org.lwjgl.util.vector.Writable;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.util.CommonUtil;

final class PerFrameCB implements Writable{

	final Matrix4f mProj = new Matrix4f();
	final Matrix4f mViewProj = new Matrix4f();
	final Matrix4f mViewProj_Inv = new Matrix4f();
	
	final Vector2f vOutputViewportSize = new Vector2f();
	final Vector2f vOutputViewportSize_Inv = new Vector2f();
	
	final Vector2f vViewportSize = new Vector2f();
	final Vector2f vViewportSize_Inv = new Vector2f();
	
	final Vector3f vEyePosition = new Vector3f();
	
	final Vector2f vJitterOffset = new Vector2f();
	float fZNear;
    float fZFar;
    
    final Vector3f vScatterPower = new Vector3f();
    int uNumPhaseTerms;
    
    final Vector3f vSigmaExtinction = new Vector3f();
    
    final int[][] uPhaseFunc = new int[VLConstant.MAX_PHASE_TERMS][4];
    final Vector4f[] vPhaseParams = new Vector4f[VLConstant.MAX_PHASE_TERMS];
    
    public PerFrameCB() {
    	for(int i = 0;i < vPhaseParams.length; i++){
			vPhaseParams[i] = new Vector4f();
		}
	}
    
    /*void store(UniformBlockData uniforms){
    	if(uniforms == null)
    		return;
    	
    	uniforms.set("mProj", mProj);
    	uniforms.set("mViewProj", mViewProj);
    	uniforms.set("mViewProj_Inv", mViewProj_Inv);
    	uniforms.set("vOutputViewportSize", vOutputViewportSize);
    	uniforms.set("vOutputViewportSize_Inv", vOutputViewportSize_Inv);
    	uniforms.set("vViewportSize", vViewportSize);
    	uniforms.set("vViewportSize_Inv", vViewportSize_Inv);
    	uniforms.set("vEyePosition", vEyePosition);
    	uniforms.set("vJitterOffset", vJitterOffset);
    	uniforms.set("fZNear", fZNear);
    	uniforms.set("fZFar", fZFar);
    	uniforms.set("vScatterPower", vScatterPower);
    	uniforms.set("uNumPhaseTerms", uNumPhaseTerms);
    	uniforms.set("vSigmaExtinction", vSigmaExtinction);
//    	uniforms.set("uPhaseFunc", uPhaseFunc);
    	uniforms.set("vPhaseParams", vPhaseParams);
    }*/

	void store(BufferGL unfiorms){
//		throw new UnsupportedOperationException();
	}
    
	@Override
	public PerFrameCB load(ByteBuffer buf) {
		mProj.load(buf);  mProj.transpose();
		mViewProj.load(buf);  mViewProj.transpose();
		mViewProj_Inv.load(buf); mViewProj_Inv.transpose();
		vOutputViewportSize.load(buf);
		vOutputViewportSize_Inv.load(buf);
		
		vViewportSize.load(buf);
		vViewportSize_Inv.load(buf);
		
		vEyePosition.load(buf);
		buf.getFloat();
		
		vJitterOffset.load(buf);
		fZNear = buf.getFloat();
		fZFar  = buf.getFloat();
		
		vScatterPower.load(buf);
		uNumPhaseTerms = buf.getInt();
		
		vSigmaExtinction.load(buf);
		buf.getFloat();
		
		for(int i = 0; i < uPhaseFunc.length; i++){
			for(int j = 0; j < 4; j++){
				uPhaseFunc[i][j] = buf.getInt();
			}
		}
		
		for(int i = 0; i < vPhaseParams.length; i++){
			vPhaseParams[i].load(buf);
		}
		return this;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("PerFrameCB:\n");
		sb.append("mProj = ").append(mProj).append('\n');
		sb.append("mViewProj = ").append(mViewProj).append('\n');
		sb.append("mViewProj_Inv = ").append(mViewProj_Inv).append('\n');
		sb.append("vOutputViewportSize = ").append(vOutputViewportSize).append('\n');
		sb.append("vOutputViewportSize_Inv = ").append(vOutputViewportSize_Inv).append('\n');
		sb.append("vViewportSize = ").append(vViewportSize).append('\n');
		sb.append("vViewportSize_Inv = ").append(vViewportSize_Inv).append('\n');
		sb.append("vEyePosition = ").append(vEyePosition).append('\n');
		sb.append("vJitterOffset = ").append(vJitterOffset).append('\n');
		sb.append("fZNear = ").append(fZNear).append('\n');
		sb.append("fZFar = ").append(fZFar).append('\n');
		sb.append("vScatterPower = ").append(vScatterPower).append('\n');
		sb.append("uNumPhaseTerms = ").append(uNumPhaseTerms).append('\n');
		sb.append("vSigmaExtinction = ").append(vSigmaExtinction).append('\n');
		sb.append("uPhaseFunc = ").append(CommonUtil.toString(uPhaseFunc)).append('\n');
		sb.append("vPhaseParams = ").append(Arrays.toString(vPhaseParams)).append('\n');
		return sb.toString();
	}
}
