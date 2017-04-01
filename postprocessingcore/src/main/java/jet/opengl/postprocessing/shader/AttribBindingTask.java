package jet.opengl.postprocessing.shader;

import java.util.function.IntConsumer;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;

public class AttribBindingTask implements IntConsumer{

	AttribBinder[] attribBinders;
	
	public AttribBindingTask(AttribBinder...attribBinders) {
		this.attribBinders = attribBinders;
	}
	
	@Override
	public void accept(int program) {
		if(attribBinders != null){
			GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
			for(AttribBinder ab : attribBinders){
				gl.glBindAttribLocation(program, ab.index, ab.attributeName);
			}
		}
	}

}
