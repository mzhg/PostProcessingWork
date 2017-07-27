package jet.opengl.demos.nvidia.waves.samples;

import java.io.IOException;

import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL40;

import jet.util.check.GLError;
import jet.util.opengl.shader.GLSLProgram;
import jet.util.opengl.shader.libs.SimpleProgram;
import jet.util.opengl.shader.loader.ShaderLoader;

abstract class IsCoreBaseProgram extends SimpleProgram{

	IsUniformData uniformData;
	IsUniformTextures textureUniforms;
	/* sub-class must fill this array. */
//	TextureSampler[] textureSamplers;
	
	public IsCoreBaseProgram(String debug_name, String vertfile, String ctrlfile, String tessfile, String fragfile) {
		CharSequence vert = null;
		CharSequence ctrl = null;
		CharSequence tess = null;
		CharSequence frag = null;
		
		try {
			vert = ShaderLoader.loadShaderFile(vertfile, false);
			ctrl = ShaderLoader.loadShaderFile(ctrlfile, false);
			tess = ShaderLoader.loadShaderFile(tessfile, false);
			frag = ShaderLoader.loadShaderFile(fragfile, false);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		GLSLProgram program = new GLSLProgram();
		GLSLProgram.ShaderSourceItem items[] = new GLSLProgram.ShaderSourceItem[4];
		items[0] = new GLSLProgram.ShaderSourceItem(vert, GL20.GL_VERTEX_SHADER);
		items[1] = new GLSLProgram.ShaderSourceItem(ctrl, GL40.GL_TESS_CONTROL_SHADER);
		items[2] = new GLSLProgram.ShaderSourceItem(tess, GL40.GL_TESS_EVALUATION_SHADER);
		items[3] = new GLSLProgram.ShaderSourceItem(frag, GL20.GL_FRAGMENT_SHADER);
		
		program.setSourceFromStrings(items);
		programId = program.getProgram();
		
		GLError.checkError();
		GL20.glUseProgram(programId);
		textureUniforms = new IsUniformTextures(debug_name, programId);
		uniformData = new IsUniformData(debug_name, programId);
		GL20.glUseProgram(0);
	}
	
	public void enable(IsParameters params, TextureSampler[] samplers) {
		super.enable();
		
		GLError.checkError();
		textureUniforms.bindTextures(samplers);
		GLError.checkError();
		uniformData.setParameters(params);
		GLError.checkError();
	}
	
	@Override
	public void disable() {
		super.disable();
		
		textureUniforms.unbindTextures();
	}
}
