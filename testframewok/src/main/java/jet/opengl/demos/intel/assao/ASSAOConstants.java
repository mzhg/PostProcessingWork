package jet.opengl.demos.intel.assao;

import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector2i;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

final class ASSAOConstants implements Readable{
	
	static final int SIZE = 240/*Util.sizeof(new ASSAOConstants())*/;

	// .zw == 1.0 / ViewportSize.xy
	final Vector2f          ViewportPixelSize = new Vector2f();
	// .zw == 1.0 / ViewportHalfSize.xy
	final Vector2f          HalfViewportPixelSize = new Vector2f();          

	final Vector2f          DepthUnpackConsts = new Vector2f();
	final Vector2f          CameraTanHalfFOV = new Vector2f();

	final Vector2f          NDCToViewMul = new Vector2f();
	final Vector2f          NDCToViewAdd = new Vector2f();

	final Vector2i          PerPassFullResCoordOffset = new Vector2i();
    final Vector2f          PerPassFullResUVOffset = new Vector2f();

    final Vector2f          Viewport2xPixelSize = new Vector2f();
 // Viewport2xPixelSize * 0.25 (for fusing add+mul into mad)
    final Vector2f          Viewport2xPixelSize_x_025 = new Vector2f();

    float                   EffectRadius;                           // world (viewspace) maximum size of the shadow
    float                   EffectShadowStrength;                   // global strength of the effect (0 - 5)
    float                   EffectShadowPow;
    float                   EffectShadowClamp;

    float                   EffectFadeOutMul;                       // effect fade out from distance (ex. 25)
    float                   EffectFadeOutAdd;                       // effect fade out to distance   (ex. 100)
    float                   EffectHorizonAngleThreshold;            // limit errors on slopes and caused by insufficient geometry tessellation (0.05 to 0.5)
    float                   EffectSamplingRadiusNearLimitRec;       // if viewspace pixel closer than this, don't enlarge shadow sampling radius anymore (makes no sense to grow beyond some distance, not enough samples to cover everything, so just limit the shadow growth; could be SSAOSettingsFadeOutFrom * 0.1 or less)

    float                   DepthPrecisionOffsetMod;
    float                   NegRecEffectRadius;                     // -1.0 / EffectRadius
    float                   LoadCounterAvgDiv;                      // 1.0 / ( halfDepthMip[SSAO_DEPTH_MIP_LEVELS-1].sizeX * halfDepthMip[SSAO_DEPTH_MIP_LEVELS-1].sizeY )
    float                   AdaptiveSampleCountLimit;

    float                   InvSharpness;
    int                     PassIndex;
    final Vector2f          QuarterResPixelSize = new Vector2f();   // used for importance map only

    final Vector4f[]        PatternRotScaleMatrices = new Vector4f[5];

    float                   NormalsUnpackMul;
    float                   NormalsUnpackAdd;
    float                   DetailAOStrength;
    float                   Dummy0;
    
    ASSAOConstants(){
//    	Util.initArray(PatternRotScaleMatrices);
		for(int i = 0; i < PatternRotScaleMatrices.length; i++){
			PatternRotScaleMatrices[i] = new Vector4f();
		}
    }
    
    /*public static void main(String[] args) {
//		CodeGen.genStoreBytebuffer(ASSAOConstants.class);
    	System.out.println(new UniformCodeGen(new Class<?>[] {ASSAOConstants.class}, "g_ASSAOConsts", new UniformNameFilter() {
			public String filter(String uniformName) {
				return uniformName;
			}
		}));
	}*/

	@Override
	public ByteBuffer store(ByteBuffer buf) {
		ViewportPixelSize.store(buf);
		HalfViewportPixelSize.store(buf);
		DepthUnpackConsts.store(buf);
		CameraTanHalfFOV.store(buf);
		NDCToViewMul.store(buf);
		NDCToViewAdd.store(buf);
//		PerPassFullResCoordOffset.store(buf);
		buf.putInt(PerPassFullResCoordOffset.x);
		buf.putInt(PerPassFullResCoordOffset.y);
		PerPassFullResUVOffset.store(buf);
		Viewport2xPixelSize.store(buf);
		Viewport2xPixelSize_x_025.store(buf);
		buf.putFloat(EffectRadius);
		buf.putFloat(EffectShadowStrength);
		buf.putFloat(EffectShadowPow);
		buf.putFloat(EffectShadowClamp);
		buf.putFloat(EffectFadeOutMul);
		buf.putFloat(EffectFadeOutAdd);
		buf.putFloat(EffectHorizonAngleThreshold);
		buf.putFloat(EffectSamplingRadiusNearLimitRec);
		buf.putFloat(DepthPrecisionOffsetMod);
		buf.putFloat(NegRecEffectRadius);
		buf.putFloat(LoadCounterAvgDiv);
		buf.putFloat(AdaptiveSampleCountLimit);
		buf.putFloat(InvSharpness);
		buf.putInt(PassIndex);
		QuarterResPixelSize.store(buf);
		for(int i = 0; i < PatternRotScaleMatrices.length; i++)
			PatternRotScaleMatrices[i].store(buf);
		buf.putFloat(NormalsUnpackMul);
		buf.putFloat(NormalsUnpackAdd);
		buf.putFloat(DetailAOStrength);
		buf.putFloat(Dummy0);
		
		return buf;
	}
}
