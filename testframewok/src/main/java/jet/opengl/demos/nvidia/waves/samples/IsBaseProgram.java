package jet.opengl.demos.nvidia.waves.samples;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;

/** Base program for island demo. */
/*public*/ abstract class IsBaseProgram extends GLSLProgram{
	
	public IsBaseProgram(String fragfile, String textureName) {
		this("nvidia/WaveWorks/shaders/fullscreen.vert", fragfile, textureName);
	}
	
	public IsBaseProgram(String vertfile, String fragfile, String textureName) {
//		String path="nvidia/WaveWorks/shaders/";
		try {
			setSourceFromFiles(vertfile, fragfile);
		} catch (IOException e) {
			e.printStackTrace();
		}

		enable();
		setTextureUniform(textureName, 0);
	}
}
