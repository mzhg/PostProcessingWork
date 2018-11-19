package jet.opengl.demos.nvidia.volumelight;

import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.util.Pair;

final class ResolvePogram extends BaseVLProgram{

	private boolean isMSAA;
	
	private final int[] textureIndexs = new int[2];
	public ResolvePogram(ContextImp_OpenGL context, boolean isMSAA, boolean debug) {
		super(context);
		
		this.isMSAA = isMSAA;
		
		compileProgram();
		
		String[] textureNames = {
				"tGodraysBuffer", "tGodraysDepth",
		};
		
		int unit = 0;
		for(int i = 0; i < textureNames.length; i++){
			textureIndexs[i] = gl.glGetUniformLocation(m_programId, textureNames[i]);
			if(textureIndexs[i] >= 0){
				gl.glProgramUniform1i(m_programId, textureIndexs[i], unit);
				
				if(debug){
					System.out.println("ResolvePogram bind " + textureNames[i] + " to unit " + unit);
				}
				
				unit ++;
			}
		}
	}
	
	public void enable(int[] textureIDs) {
		super.enable();
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
	
	@Override
	protected Pair<String, Macro[]> getPSShader() {
		return new Pair<>("Resolve_PS.frag", isMSAA ? sampleModeMSAA : sampleModeSingle);
	}

	@Override
	protected Object getParameter() {
		return "Resolve_" + (isMSAA ? 2 : 1);
	}

}
