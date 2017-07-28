package jet.opengl.demos.nvidia.waves.samples;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;

/*public*/ class IsUniformTextures {
	
	static boolean out_print = true;

//	uniform sampler2D g_HeightfieldTexture;     //unit 0
//	uniform sampler2D g_LayerdefTexture;        //unit 1
//	uniform sampler2D g_SandBumpTexture;        //unit 2
//	uniform sampler2D g_RockBumpTexture;        //unit 3
//	uniform sampler2D g_WaterNormalMapTexture;  //unit 4
//	uniform sampler2D g_SandMicroBumpTexture;   //unit 5
//	uniform sampler2D g_RockMicroBumpTexture;   //unit 6
//
//	// RenderHeightFieldPS
//	uniform sampler2D g_SlopeDiffuseTexture;    //unit 7
//	uniform sampler2D g_SandDiffuseTexture;     //unit 8
//	uniform sampler2D g_RockDiffuseTexture;     //unit 9
//	uniform sampler2D g_GrassDiffuseTexture;    //unit 10
//	uniform sampler2DShadow g_DepthTexture;     //unit 11
//	uniform sampler2D g_WaterBumpTexture;       //unit 12
//	uniform sampler2D g_RefractionDepthTextureResolved;   //unit 13
//	uniform sampler2D g_ReflectionTexture;      //unit 14
//	uniform sampler2D g_RefractionTexture;      //unit 15
//	unfiorm sampler2D g_DepthMapTexture;        //unit 17
	
	final int[] textureUniforms = new int[17];
	int textureUnits;
	private GLFuncProvider gl;
	public IsUniformTextures(String debug_name, int program) {
		String[] textureNames = {
			"g_HeightfieldTexture", "g_LayerdefTexture", "g_SandBumpTexture",
			"g_RockBumpTexture", "g_WaterNormalMapTexture", "g_SandMicroBumpTexture",
			"g_RockMicroBumpTexture", "g_SlopeDiffuseTexture", "g_SandDiffuseTexture",
			"g_RockDiffuseTexture", "g_GrassDiffuseTexture", "g_DepthTexture", "g_WaterBumpTexture",
			"g_RefractionDepthTextureResolved", "g_ReflectionTexture", "g_RefractionTexture",
			"g_DepthMapTexture"
		};

		gl= GLFuncProviderFactory.getGLFuncProvider();
		
		if(out_print){
			System.out.println(debug_name + "'s texture uniforms initalizing...");
		}

		gl.glUseProgram(program);
		int index = 0;
		for(int i = 0; i < textureNames.length; i++){
			textureUniforms[i] = gl.glGetUniformLocation(program, textureNames[i]);
			if(textureUniforms[i] >= 0){
				gl.glUniform1i(textureUniforms[i], index);  // setup the default value.
				if(out_print){
					System.out.println(debug_name + " have sampler2D " + textureNames[i] +", and binding unit to " + index);
				}
				
				index++;
			}else if(out_print){
//				System.out.println(debug_name + "don't have sampler2D " + textureNames[i]);
			}
		}
	}
	
	public void bindTextures(TextureSampler[] textures){
		textureUnits = 0;
		for(int i = 0; i < textureUniforms.length; i++){
			int index = textureUniforms[i];
			if(index >= 0){
				int unit = GLenum.GL_TEXTURE0 + textureUnits;
				GLCheck.checkError("texture unit" + textureUnits + " and index = " + i);
				gl.glActiveTexture(unit);
				gl.glBindTexture(GLenum.GL_TEXTURE_2D, textures[textureUnits].textureID);
				gl.glBindSampler(textureUnits, textures[textureUnits].sampler);
				textureUnits++;
			}
		}
	}
	
	public void unbindTextures(){
		while(textureUnits-- > 0){
			gl.glActiveTexture(GLenum.GL_TEXTURE0 + textureUnits);
			gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);
			gl.glBindSampler(textureUnits, 0);
		}
	}
}
