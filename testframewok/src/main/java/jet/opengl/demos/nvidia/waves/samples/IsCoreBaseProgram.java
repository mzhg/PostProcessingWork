package jet.opengl.demos.nvidia.waves.samples;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.ShaderLoader;
import jet.opengl.postprocessing.shader.ShaderSourceItem;
import jet.opengl.postprocessing.shader.ShaderType;

abstract class IsCoreBaseProgram extends GLSLProgram{

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
		
		ShaderSourceItem items[] = new ShaderSourceItem[4];
		items[0] = new ShaderSourceItem(vert, ShaderType.VERTEX);
		items[1] = new ShaderSourceItem(ctrl, ShaderType.TESS_CONTROL);
		items[2] = new ShaderSourceItem(tess, ShaderType.TESS_EVAL);
		items[3] = new ShaderSourceItem(frag, ShaderType.FRAGMENT);
		
		setSourceFromStrings(items);
		textureUniforms = new IsUniformTextures(debug_name, getProgram());
		uniformData = new IsUniformData(debug_name, getProgram());
	}
	
	public void enable(IsParameters params, TextureSampler[] samplers) {
		super.enable();
		
		textureUniforms.bindTextures(samplers);
		uniformData.setParameters(params);
	}
	
	@Override
	public void disable() {
		super.disable();
		
		textureUniforms.unbindTextures();
	}
}
