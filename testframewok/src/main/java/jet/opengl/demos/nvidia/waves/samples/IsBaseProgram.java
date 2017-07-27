package jet.opengl.demos.nvidia.waves.samples;

import java.io.IOException;

import org.lwjgl.opengl.GL20;

import jet.opengl.examples.tools.program.TextureUnitProgram;
import jet.util.check.GLError;
import jet.util.opengl.shader.GLSLProgram;
import jet.util.opengl.shader.libs.SimpleProgram;
import jet.util.opengl.shader.loader.ShaderLoader;

/** Base program for island demo. */
/*public*/ abstract class IsBaseProgram extends SimpleProgram implements TextureUnitProgram{
	
	public IsBaseProgram(String fragfile, String textureName) {
		this(null, fragfile, textureName);
	}
	
	public IsBaseProgram(String vertfile, String fragfile, String textureName) {
		CharSequence vert = null;
		CharSequence frag = null;
		
		try {
			if(vertfile == null)
				vert = ShaderLoader.loadShaderFile("fullscreen.vert", true);
			else
				vert = ShaderLoader.loadShaderFile(vertfile, false);
			frag = ShaderLoader.loadShaderFile(fragfile, false);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		GLSLProgram program = new GLSLProgram();
		program.setSourceFromStrings(vert, frag, true);
		programId = program.getProgram();
		
		GLError.checkError();
		GL20.glUseProgram(programId);
		applyTexture(programId, textureName);
		initShader();
		GL20.glUseProgram(0);
	}
	
	void initShader(){}
}
