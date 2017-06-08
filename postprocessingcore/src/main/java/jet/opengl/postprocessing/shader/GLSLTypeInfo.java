package jet.opengl.postprocessing.shader;

public class GLSLTypeInfo {

	public static final int TYPE_INT = 1;
	public static final int TYPE_FLOAT = 2;
	public static final int TYPE_DOUBLE = 3;
	public static final int TYPE_BOOL = 4;
	
	/** The name of type. (i.e FLOAT, VEC2, MAT4...) */
	public final String name;
	/** The correspond java type. */
	public final Class<?> clazz;
	/** One of {@link #TYPE_BOOL}, {@link #TYPE_DOUBLE}, {@link #TYPE_FLOAT}, {@link #TYPE_INT} */
	public final int dataType;
	/** The memory size in bytes of the type. */
	public final int size;
	
	GLSLTypeInfo(String name, Class<?> clazz, int dataType, int size) {
		this.name = name;
		this.clazz = clazz;
		this.dataType = dataType;
		this.size = size;
	}
	public boolean isSampler(){ return name != null && name.contains("sampler");}
	public boolean isImage(){ return name != null && name.contains("image");}
	

}
