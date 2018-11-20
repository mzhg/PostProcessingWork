package jet.opengl.demos.nvidia.volumelight;

import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.util.Pair;

final class RenderVolumeProgram extends BaseVLProgram{

	private RenderVolumeDesc desc;
	private final int[] textureIndexs = new int[6];
	private final int[] textureUnits  = new int[6];
	public RenderVolumeProgram(ContextImp_OpenGL context, RenderVolumeDesc desc) {
		super(context);
		
		this.desc = desc;
		compileProgram();
		
		textureIndexs[0] = gl.glGetUniformLocation(m_programId, "tShadowMap");
		textureIndexs[1] = gl.glGetUniformLocation(m_programId, "tPhaseLUT");
		textureIndexs[2] = gl.glGetUniformLocation(m_programId, "tLightLUT_P");
		textureIndexs[3] = gl.glGetUniformLocation(m_programId, "tLightLUT_S1");
		textureIndexs[4] = gl.glGetUniformLocation(m_programId, "tLightLUT_S2");
		textureIndexs[5] = gl.glGetUniformLocation(m_programId, "tSceneDepth");
	
		int unit = 0;
		for(int i = 0; i < textureIndexs.length; i++){
			int idx = textureIndexs[i];
			if(idx >= 0){
				textureUnits[i] = unit;
				gl.glProgramUniform1i(m_programId, idx, unit++);
			}
		}
	}
	
	public void setShadowMap(int target, int textureID, int sampler){
		if(textureIndexs[0] >= 0){
			bindTexture(target, textureID, sampler, textureUnits[0]);
		}
	}
	
	public void setPhaseLUT(int textureID){
		if(textureIndexs[1] >= 0){
			bindTexture(GLenum.GL_TEXTURE_2D, textureID, context.samplerLinear, textureUnits[1]);
		}
	}
	
	public void setLightLUT_P(int textureID){
		if(textureIndexs[2] >= 0){
			bindTexture(GLenum.GL_TEXTURE_2D, textureID, context.samplerLinear, textureUnits[2]);
		}
	}
	
	public void setLightLUT_S1(int textureID){
		if(textureIndexs[3] >= 0){
			bindTexture(GLenum.GL_TEXTURE_2D, textureID, context.samplerLinear, textureUnits[3]);
		}
	}
	
	public void setLightLUT_S2(int textureID){
		if(textureIndexs[4] >= 0){
			bindTexture(GLenum.GL_TEXTURE_2D, textureID, context.samplerLinear, textureUnits[4]);
		}
	}
	
	public void setSceneDepth(int target, int textureID){
		if(textureIndexs[5] >= 0){
			bindTexture(target, textureID, context.samplerLinear, textureUnits[5]);
		}
	}
	
	@Deprecated
	public void enable(int[] textureIDs) {
		super.enable();
		textureLocation = 0;
		for(int i = 0; i < textureIndexs.length; i++){
			int idx = textureIndexs[i];
			if(idx >= 0){
				/*GL13.glActiveTexture(GL13.GL_TEXTURE0 + textureLocation);
				GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureIDs[textureLocation]);
				GL33.glBindSampler(textureLocation, context.samplerLinear);*/

				bindTexture(GLenum.GL_TEXTURE_2D, textureIDs[textureLocation], context.samplerLinear, textureLocation);
				textureLocation ++;
			}
		}
	}
	
	public void tagLocation() {
		textureLocation = 0;
		for(int i = 0; i < textureIndexs.length; i++){
			if(textureIndexs[i] >= 0){
				textureLocation++;
			}
		}
	}
	
	@Override
	protected Pair<String, Macro[]> getVSShader() {
		if(desc.useQuadVS)
			return super.getVSShader();
		
		String filename = "RenderVolume_VS.vert";
		
		Macro[] vs_macros = {
				new Macro("MESHMODE_FRUSTUM_GRID", 1),
				new Macro("MESHMODE_FRUSTUM_BASE", 2),
				new Macro("MESHMODE_FRUSTUM_CAP", 3),
				new Macro("MESHMODE_OMNI_VOLUME", 4),
				new Macro("MESHMODE_GEOMETRY", 5),
				new Macro("MESHMODE", desc.meshMode)
		};
		
		return new Pair<>(filename, vs_macros);
	}
	
	@Override
	protected Pair<String, Macro[]> getHSShader() {
		if(!desc.includeTesslation)
			return null;
		
		String filename = "RenderVolume_HS.frag";
		
		Macro[] hs_macros = {
				new Macro("SHADOWMAPTYPE_ATLAS", RenderVolumeDesc.SHADOWMAPTYPE_ATLAS),
				new Macro("SHADOWMAPTYPE_ARRAY", RenderVolumeDesc.SHADOWMAPTYPE_ARRAY),
				new Macro   ("SHADOWMAPTYPE", desc.shadowMapType), 
				
				new Macro("CASCADECOUNT_1", RenderVolumeDesc.CASCADECOUNT_1),
				new Macro("CASCADECOUNT_2", RenderVolumeDesc.CASCADECOUNT_2),
				new Macro("CASCADECOUNT_3", RenderVolumeDesc.CASCADECOUNT_3),
				new Macro("CASCADECOUNT_4", RenderVolumeDesc.CASCADECOUNT_4),
				new Macro   ("CASCADECOUNT", desc.cascadeCount), 
				
				new Macro("VOLUMETYPE_FRUSTUM", RenderVolumeDesc.VOLUMETYPE_FRUSTUM),
				new Macro("VOLUMETYPE_PARABOLOID", RenderVolumeDesc.VOLUMETYPE_PARABOLOID),
				new Macro   ("VOLUMETYPE", desc.volumeType),
				
				new Macro("MAXTESSFACTOR_LOW", RenderVolumeDesc.MAXTESSFACTOR_LOW),
				new Macro("MAXTESSFACTOR_MEDIUM", RenderVolumeDesc.MAXTESSFACTOR_MEDIUM),
				new Macro("MAXTESSFACTOR_HIGH", RenderVolumeDesc.MAXTESSFACTOR_HIGH),
				new Macro   ("MAXTESSFACTOR", desc.maxtessfactor), 
		};

		return new Pair<>(filename, hs_macros);
	}
	
	@Override
	protected Pair<String, Macro[]> getDSShader() {
		if(!desc.includeTesslation)
			return null;
		
		String filename = "RenderVolume_DS.frag";
		
		Macro[] ds_macros = {
				new Macro("SHADOWMAPTYPE_ATLAS", RenderVolumeDesc.SHADOWMAPTYPE_ATLAS),
				new Macro("SHADOWMAPTYPE_ARRAY", RenderVolumeDesc.SHADOWMAPTYPE_ARRAY),
				new Macro   ("SHADOWMAPTYPE", desc.shadowMapType), 
				
				new Macro("CASCADECOUNT_1", RenderVolumeDesc.CASCADECOUNT_1),
				new Macro("CASCADECOUNT_2", RenderVolumeDesc.CASCADECOUNT_2),
				new Macro("CASCADECOUNT_3", RenderVolumeDesc.CASCADECOUNT_3),
				new Macro("CASCADECOUNT_4", RenderVolumeDesc.CASCADECOUNT_4),
				new Macro   ("CASCADECOUNT", desc.cascadeCount), 
				
				new Macro("VOLUMETYPE_FRUSTUM", RenderVolumeDesc.VOLUMETYPE_FRUSTUM),
				new Macro("VOLUMETYPE_PARABOLOID", RenderVolumeDesc.VOLUMETYPE_PARABOLOID),
				new Macro   ("VOLUMETYPE", desc.volumeType),
		};

		return new Pair<>(filename, ds_macros);
	}
	
	@Override
	protected Pair<String, Macro[]> getPSShader() {
		if(desc.debugPS){
			return new Pair<>("Debug_PS.frag", null);
		}
		
		String filename = "RenderVolume_PS.frag";
		
		Macro[] ps_macros = {
				new Macro("SAMPLEMODE_SINGLE", RenderVolumeDesc.SAMPLEMODE_SINGLE),
				new Macro("SAMPLEMODE_MSAA", RenderVolumeDesc.SAMPLEMODE_MSAA),
				new Macro   ("SAMPLEMODE", desc.sampleMode), 
				
				new Macro("LIGHTMODE_DIRECTIONAL", RenderVolumeDesc.LIGHTMODE_DIRECTIONAL),
				new Macro("LIGHTMODE_SPOTLIGHT", RenderVolumeDesc.LIGHTMODE_SPOTLIGHT),
				new Macro("LIGHTMODE_OMNI", RenderVolumeDesc.LIGHTMODE_OMNI),
				new Macro   ("LIGHTMODE", desc.lightMode), 
				
				new Macro("PASSMODE_GEOMETRY", RenderVolumeDesc.PASSMODE_GEOMETRY),
				new Macro("PASSMODE_SKY", RenderVolumeDesc.PASSMODE_SKY),
				new Macro("PASSMODE_FINAL", RenderVolumeDesc.PASSMODE_FINAL),
				new Macro   ("PASSMODE", desc.passMode),
				
				new Macro("ATTENUATIONMODE_NONE", RenderVolumeDesc.ATTENUATIONMODE_NONE),
				new Macro("ATTENUATIONMODE_POLYNOMIAL", RenderVolumeDesc.ATTENUATIONMODE_POLYNOMIAL),
				new Macro("ATTENUATIONMODE_INV_POLYNOMIAL", RenderVolumeDesc.ATTENUATIONMODE_INV_POLYNOMIAL),
				new Macro   ("ATTENUATIONMODE", desc.attenuationMode),
				
				new Macro("FALLOFFMODE_NONE", RenderVolumeDesc.FALLOFFMODE_NONE),
				new Macro("FALLOFFMODE_FIXED", RenderVolumeDesc.FALLOFFMODE_FIXED),
				new Macro("FALLOFFMODE_CUSTOM", RenderVolumeDesc.FALLOFFMODE_CUSTOM),
				new Macro   ("FALLOFFMODE", desc.falloffMode), 
		};

		return new Pair<>(filename, ps_macros);
	}
	
	@Override
	protected Object getParameter() {
		return desc;
	}
}
