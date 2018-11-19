package jet.opengl.demos.nvidia.volumelight;

import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.Macro;

final class ComputeLightLUTProgram extends BaseVLProgram{

	private ComputeLightLUTDesc desc;
	private final int[] textureIndexs = new int[4];
	
	public ComputeLightLUTProgram(ContextImp_OpenGL context, ComputeLightLUTDesc desc, String debugName) {
		super(context);
		
		System.err.println("ComputeLightLUTProgram initlized!");
		
		this.desc = desc;
		Macro[] lightMacros = {
			new Macro("LIGHTMODE_OMNI", RenderVolumeDesc.LIGHTMODE_OMNI),	
			new Macro("LIGHTMODE_SPOTLIGHT", RenderVolumeDesc.LIGHTMODE_SPOTLIGHT),
			new Macro("LIGHTMODE", desc.lightMode),
			
			new Macro("ATTENUATIONMODE_NONE", RenderVolumeDesc.ATTENUATIONMODE_NONE),	
			new Macro("ATTENUATIONMODE_POLYNOMIAL", RenderVolumeDesc.ATTENUATIONMODE_POLYNOMIAL),
			new Macro("ATTENUATIONMODE_INV_POLYNOMIAL", RenderVolumeDesc.ATTENUATIONMODE_INV_POLYNOMIAL),
			new Macro("ATTENUATIONMODE", desc.attenuationMode),
			
			new Macro("COMPUTEPASS_CALCULATE", RenderVolumeDesc.COMPUTEPASS_CALCULATE),	
			new Macro("COMPUTEPASS_SUM", RenderVolumeDesc.COMPUTEPASS_SUM),
			new Macro("COMPUTEPASS", desc.computePass),
		};
		
		compileComputeProgram("ComputeLightLUT_CS.frag", lightMacros);
		initUniformData();
		
		String[] textureNames = {
				"tPhaseLUT", "tLightLUT_P", "tLightLUT_S1","tLightLUT_S2"
		};
		
		int unit = 0;
		for(int i = 0; i < textureNames.length; i++){
			textureIndexs[i] = gl.glGetUniformLocation(m_programId, textureNames[i]);
			if(textureIndexs[i] >= 0){
				gl.glProgramUniform1i(m_programId, textureIndexs[i], unit);
				
				if(debugName != null){
					System.out.println(debugName + " bind " + textureNames[i] + " to unit " + unit);
				}
				
				unit ++;
			}
		}
	}
	
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
	protected Object getParameter() {
		return desc;
	}

}
