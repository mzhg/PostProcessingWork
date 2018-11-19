package jet.opengl.demos.nvidia.volumelight;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
import org.lwjgl.util.vector.Writable;

import jet.opengl.postprocessing.buffer.BufferGL;

final class PerVolumeCB implements Writable{

	// c0+4
    final Matrix4f mLightToWorld = new Matrix4f();
    // c4
    float fLightFalloffAngle;
    float fLightFalloffPower;
    float fGridSectionSize;
    float fLightToEyeDepth;
    // c5
    float fLightZNear;
    float fLightZFar;
//    float pad[2];
    // c6
    final Vector4f vAttenuationFactors = new Vector4f();
    // c7+16
//    NvMat44 mLightProj[4];
    final Matrix4f[] mLightProj = new Matrix4f[4];
    // c23+16
//    NvMat44 mLightProj_Inv[4];
    final Matrix4f[] mLightProj_Inv = new Matrix4f[4];
    // c39
    final Vector3f vLightDir = new Vector3f();
    float fDepthBias;
    // c40
    final Vector3f vLightPos = new Vector3f();
    int uMeshResolution;
    // c41
    final Vector3f vLightIntensity = new Vector3f();
    float fTargetRaySize;
    // c42+4
    final Vector4f[] vElementOffsetAndScale = new Vector4f[4];
    // c46
    final Vector4f vShadowMapDim = new Vector4f();
    // c47+4
    // Only first index of each "row" is used.
    // (Need to do this because HLSL can only stride arrays by full offset)
//    uint32_t uElementIndex[4][4];
    final int[] uElementIndex = new int[16];
    
    /*void store(UniformBlockData uniforms){
    	if(uniforms == null)
    		return;
    	
    	uniforms.set("mLightToWorld", mLightToWorld);
    	uniforms.set("fLightFalloffAngle", fLightFalloffAngle);
    	uniforms.set("fLightFalloffPower", fLightFalloffPower);
    	uniforms.set("fGridSectionSize", fGridSectionSize);
    	uniforms.set("fLightToEyeDepth", fLightToEyeDepth);
    	uniforms.set("fLightZNear", fLightZNear);
    	uniforms.set("fLightZFar", fLightZFar);
    	uniforms.set("vAttenuationFactors", vAttenuationFactors);
    	uniforms.set("mLightProj", mLightProj);
    	uniforms.set("mLightProj_Inv", mLightProj_Inv);
    	uniforms.set("vLightDir", vLightDir);
    	uniforms.set("fDepthBias", fDepthBias);
    	uniforms.set("vLightPos", vLightPos);
    	uniforms.set("uMeshResolution", uMeshResolution);
    	uniforms.set("vLightIntensity", vLightIntensity);
    	uniforms.set("fTargetRaySize", fTargetRaySize);
    	uniforms.set("vElementOffsetAndScale", vElementOffsetAndScale);
    	uniforms.set("vShadowMapDim", vShadowMapDim);
    	uniforms.set("uElementIndex", uElementIndex);
    }*/

    void store(BufferGL uniforms){
        throw new UnsupportedOperationException();
    }
    
    @Override
	public Writable load(ByteBuffer buf) {
		mLightToWorld.load(buf);  mLightToWorld.transpose();
		
		fLightFalloffAngle = buf.getFloat();
        fLightFalloffPower = buf.getFloat();
        fGridSectionSize = buf.getFloat();
        fLightToEyeDepth = buf.getFloat();
        // c5
        fLightZNear = buf.getFloat();
        fLightZFar = buf.getFloat();
//        float pad[2];
        buf.getFloat();
        buf.getFloat();
        
        vAttenuationFactors.load(buf);
        for(int i = 0; i < 4; i++){
        	mLightProj[i].load(buf);
        	mLightProj[i].transpose();
        }
        
        for(int i = 0; i < 4; i++){
        	mLightProj_Inv[i].load(buf);
        	mLightProj_Inv[i].transpose();
        }
        
        vLightDir.load(buf);
        fDepthBias = buf.getFloat();
        
        vLightPos.load(buf);
        uMeshResolution = buf.getInt();
        
        vLightIntensity.load(buf);
        fTargetRaySize = buf.getFloat();
        
        for(int i = 0; i < 4; i++){
        	vElementOffsetAndScale[i].load(buf);
        }
        
        vShadowMapDim.load(buf);
        
        for(int i = 0; i < uElementIndex.length; i++){
        	uElementIndex[i] = buf.getInt();
        }
		
		return null;
	}
    

    public PerVolumeCB() {
    	for(int i = 0; i < mLightProj.length; i++) mLightProj[i] = new Matrix4f();
    	for(int i = 0; i < mLightProj_Inv.length; i++) mLightProj_Inv[i] = new Matrix4f();
    	for(int i = 0; i < vElementOffsetAndScale.length; i++) vElementOffsetAndScale[i] = new Vector4f();
	}
    
    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	sb.append("PerVolumeCB:\n");
    	sb.append("mLightToWorld = ").append(mLightToWorld).append('\n');
    	sb.append("fLightFalloffAngle = ").append(fLightFalloffAngle).append('\n');
    	sb.append("fLightFalloffPower = ").append(fLightFalloffPower).append('\n');
    	sb.append("fGridSectionSize = ").append(fGridSectionSize).append('\n');
    	sb.append("fLightToEyeDepth = ").append(fLightToEyeDepth).append('\n');
    	sb.append("fLightZNear = ").append(fLightZNear).append('\n');
    	sb.append("fLightZFar = ").append(fLightZFar).append('\n');
    	sb.append("vAttenuationFactors = ").append(vAttenuationFactors).append('\n');
    	sb.append("mLightProj = ").append(Arrays.toString(mLightProj)).append('\n');
    	sb.append("mLightProj_Inv = ").append(Arrays.toString(mLightProj_Inv)).append('\n');
    	sb.append("vLightDir = ").append(vLightDir).append('\n');
    	sb.append("fDepthBias = ").append(fDepthBias).append('\n');
    	sb.append("vLightPos = ").append(vLightPos).append('\n');
    	sb.append("uMeshResolution = ").append(uMeshResolution).append('\n');
    	sb.append("vLightIntensity = ").append(vLightIntensity).append('\n');
    	sb.append("fTargetRaySize = ").append(fTargetRaySize).append('\n');
    	sb.append("vElementOffsetAndScale = ").append(Arrays.toString(vElementOffsetAndScale)).append('\n');
    	sb.append("vShadowMapDim = ").append(vShadowMapDim).append('\n');
    	sb.append("uElementIndex = ").append(uElementIndex).append('\n');
    	return sb.toString();
    }
}
