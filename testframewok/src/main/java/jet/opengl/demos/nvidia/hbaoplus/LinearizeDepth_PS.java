package jet.opengl.demos.nvidia.hbaoplus;

final class LinearizeDepth_PS extends CopyDepth_PS{

	private int m_GlobalUniformBlock = -1;

	@Override
	void create(CharSequence FragmentShaderSource) {
		super.create(FragmentShaderSource);
		
		m_GlobalUniformBlock    = getUniformBlockIndex(/*GL,*/ "GlobalConstantBuffer");
		gl.glUniformBlockBinding(getProgram(), m_GlobalUniformBlock, BaseConstantBuffer.BINDING_POINT_GLOBAL_UBO);
	}
}
