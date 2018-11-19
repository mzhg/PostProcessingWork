package jet.opengl.demos.nvidia.volumelight;


import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.util.Pair;

final class ComputePhaseLookupProgram extends BaseVLProgram{

	public ComputePhaseLookupProgram(ContextImp_OpenGL context) {
		super(context);
		
		compileProgram();
	}
	
	@Override
	protected Pair<String, Macro[]> getPSShader() {
		return new Pair<>("ComputePhaseLookup_PS.frag", null);
	}

	@Override
	protected Object getParameter() {
		return "ComputePhaseLookup";
	}

}
