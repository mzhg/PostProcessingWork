package jet.opengl.demos.demos.scenes.outdoor;

import java.io.IOException;

import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.Macro;

public class HemisphereProgram extends GLSLProgram{

	public HemisphereProgram(String shaderPath, Macro[] macros) {
		try {
			setSourceFromFiles(shaderPath + "HemisphereVS.vert", shaderPath + "HemispherePS.frag", macros);
		} catch (IOException e) {
			e.printStackTrace();
		}

		int g_tex2DAmbientSkylight = gl.glGetUniformLocation(getProgram(), "g_tex2DAmbientSkylight");
		int g_tex2DMtrlMap = gl.glGetUniformLocation(getProgram(), "g_tex2DMtrlMap");
		int g_tex2DNormalMap = gl.glGetUniformLocation(getProgram(), "g_tex2DNormalMap");
		int g_tex2DOccludedNetDensityToAtmTop = gl.glGetUniformLocation(getProgram(), "g_tex2DOccludedNetDensityToAtmTop");
		int g_tex2DShadowMap = gl.glGetUniformLocation(getProgram(), "g_tex2DShadowMap");
//		int g_tex2DTileNormalMaps0 = GL20.glGetUniformLocation(getProgram(), "g_tex2DTileNormalMaps[0]");
//		int g_tex2DTileTextures0 = GL20.glGetUniformLocation(getProgram(), "g_tex2DTileTextures[0]");

		gl.glProgramUniform1i(getProgram(), g_tex2DAmbientSkylight, 0);
		gl.glProgramUniform1i(getProgram(), g_tex2DMtrlMap, 1);
		gl.glProgramUniform1i(getProgram(), g_tex2DNormalMap, 2);
		gl.glProgramUniform1i(getProgram(), g_tex2DOccludedNetDensityToAtmTop, 3);
		gl.glProgramUniform1i(getProgram(), g_tex2DShadowMap, 4);
//		GL41.glProgramUniform1i(getProgram(), g_tex2DTileNormalMaps0, 5);
//		GL41.glProgramUniform1i(getProgram(), g_tex2DTileTextures0, 6);
		
		for(int i=0;i < 5; i++){
			String tileNormalName = String.format("g_tex2DTileNormalMaps[%d]", i);
			String tileTextureName = String.format("g_tex2DTileTextures[%d]", i);
			
			int location = gl.glGetUniformLocation(getProgram(), tileNormalName);
			if(location >= 0){
				gl.glProgramUniform1i(getProgram(), location, getTileNormalMapUnit(i));
			}
			
			location = gl.glGetUniformLocation(getProgram(), tileTextureName);
			if(location >= 0){
				gl.glProgramUniform1i(getProgram(), location, getTileTextureUnit(i));
			}
		}
	}
	
	public void unbindTextures(){
		for(int i =  15; i>= 0; i--){
			int target = (i == 4) ? GLenum.GL_TEXTURE_2D_ARRAY : GLenum.GL_TEXTURE_2D;
			gl.glActiveTexture(GLenum.GL_TEXTURE0 + i);
			gl.glBindTexture(target, 0);
			gl.glBindSampler(i, 0);
		}
	}
	
	public int getAmbientSkylightUnit() { return 0;}
	public int getMtrlMapUnit() { return 1;}
	public int getNormalMapUnit() { return 2;}
	public int getOccludedNetDensityToAtmTopUnit() { return 3;}
	public int getShadowMapUnit() { return 4;}
	public int getTileNormalMapUnit(int index) { return 5 + index;}
	public int getTileTextureUnit(int index) { return 10 + index;}
}
