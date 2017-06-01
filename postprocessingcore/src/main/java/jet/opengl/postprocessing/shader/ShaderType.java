package jet.opengl.postprocessing.shader;

import jet.opengl.postprocessing.common.GLenum;

public enum ShaderType {
	
	VERTEX(GLenum.GL_VERTEX_SHADER),
	FRAGMENT(GLenum.GL_FRAGMENT_SHADER),
	GEOMETRY(GLenum.GL_GEOMETRY_SHADER),
	TESS_CONTROL(GLenum.GL_TESS_CONTROL_SHADER),
	TESS_EVAL(GLenum.GL_TESS_EVALUATION_SHADER),
	COMPUTE(GLenum.GL_COMPUTE_SHADER);

	public final int shader;
	
	private ShaderType(int shader) {
		this.shader = shader;
	}

	public static ShaderType wrap(int shader){
		ShaderType[] items = ShaderType.values();
		for(ShaderType type : items){
			if(type.shader == shader)
				return type;
		}

		return null;
	}
}
