package jet.opengl.postprocessing.shader;

public class AttribProperties {

	public String name;
	public int location;
	public int type;
	public int size;
	@Override
	public String toString() {
		return "Attrib [name=" + name + ", location=" + location  + "]";
	}
}
