package jet.opengl.demos.nvidia.volumelight;

import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.util.Pair;

final class ApplyProgram extends BaseVLProgram{

	private ApplyDesc desc;
	private final int[] textureIndexs = new int[3];
	private final int[] textureUnits = new int[3];
	
	private int godrays_sampler;
	public ApplyProgram(ContextImp_OpenGL context, ApplyDesc desc, String debugName) {
		super(context);
		
		this.desc = desc;
		compileProgram();
		
		String[] textureNames = {
				"tSceneDepth", "tGodraysDepth",  "tGodraysBuffer",
		};
		
		int unit = 0;
		for(int i = 0; i < textureNames.length; i++){
			textureIndexs[i] = gl.glGetUniformLocation(m_programId, textureNames[i]);
			if(textureIndexs[i] >= 0){
				gl.glProgramUniform1i(m_programId, textureIndexs[i], unit);
				textureUnits[i] = unit;
				
				if(debugName != null){
					System.out.println(debugName + " bind " + textureNames[i] + " to unit " + unit);
				}
				
				unit ++;
			}
		}
		
		if(desc.upsampleMode == RenderVolumeDesc.UPSAMPLEMODE_BILATERAL){
			godrays_sampler = context.samplerLinear;
		}else{
			godrays_sampler = context.samplerPoint;
		}
	}
	
	public void setSceneDepth(int target, int textureID){
		if(textureIndexs[0] >= 0){
			bindTexture(target, textureID, context.samplerPoint, textureUnits[0]);
		}
	}
	
	public void setGodraysDepth(int target, int textureID){
		if(textureIndexs[1] >= 0){
			bindTexture(target, textureID, context.samplerPoint, textureUnits[1]);
		}
	}
	
	public void setGodraysBuffer(int textureID){
		if(textureIndexs[2] >= 0){
			bindTexture(GLenum.GL_TEXTURE_2D, textureID, godrays_sampler, textureUnits[2]);
		}
	}
	
	@Deprecated
	public void enable(int[] textureIDs) {
		textureLocation = 0;
		super.enable();
		for(int i = 0; i < textureIndexs.length; i++){
			int idx = textureIndexs[i];
			if(idx >= 0){
				gl.glActiveTexture(GLenum.GL_TEXTURE0 + textureLocation);
				gl.glBindTexture(GLenum.GL_TEXTURE_2D, textureIDs[textureLocation]);
				gl.glBindSampler(textureLocation, context.samplerLinear);
				textureLocation ++;
			}
		}
	}
	
	@Override
	protected Pair<String, Macro[]> getPSShader() {
		String filename = "Apply_PS.frag";
		Macro[] ps_macros = {
			new Macro("SAMPLEMODE_SINGLE", RenderVolumeDesc.SAMPLEMODE_SINGLE),
			new Macro("SAMPLEMODE_MSAA", RenderVolumeDesc.SAMPLEMODE_MSAA),
			new Macro("SAMPLEMODE", desc.sampleMode),

			new Macro("UPSAMPLEMODE_POINT", RenderVolumeDesc.UPSAMPLEMODE_POINT),
			new Macro("UPSAMPLEMODE_BILINEAR", RenderVolumeDesc.UPSAMPLEMODE_BILINEAR),
			new Macro("UPSAMPLEMODE_BILATERAL", RenderVolumeDesc.UPSAMPLEMODE_BILATERAL),
			new Macro("UPSAMPLEMODE", desc.upsampleMode),

			new Macro("FOGMODE_NONE", RenderVolumeDesc.FOGMODE_NONE),
			new Macro("FOGMODE_NOSKY", RenderVolumeDesc.FOGMODE_NOSKY),
			new Macro("FOGMODE_FULL", RenderVolumeDesc.FOGMODE_FULL),
			new Macro("FOGMODE", desc.fogMode),
		};
		
		return new Pair<>(filename, ps_macros);
	}

	@Override
	protected Object getParameter() {
		return desc;
	}

}
