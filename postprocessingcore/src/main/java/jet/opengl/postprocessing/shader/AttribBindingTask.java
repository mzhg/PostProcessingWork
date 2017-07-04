package jet.opengl.postprocessing.shader;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;

public class AttribBindingTask implements ProgramLinkTask{

	AttribBinder[] attribBinders;
	
	public AttribBindingTask(AttribBinder...attribBinders) {
		this.attribBinders = attribBinders;
	}
	
	@Override
	public void invoke(int program) {
		if(attribBinders != null){
			GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
			for(AttribBinder ab : attribBinders){
				gl.glBindAttribLocation(program, ab.index, ab.attributeName);
			}
		}
	}

}
