package jet.opengl.postprocessing.shader;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UniformProperty {

	public String name;
	public int size;  // The array size
	public int type;
	public int location;  // only valid for the single unfiorms
	public Object value;
	public boolean isBelongBlock;

	public int offset; // only for GL_BUFFER_VARIABLE
	public int arrayStride;
	public int matrixStride;
	public boolean isRowMajor;
	
	@Override
	public String toString() {
//		return "Uniform [name=" + name + ", size=" + size + ", type=" + type + ", location=" + location
//				+ ", value=" + value + "]";
		return "Uniform [" + GLSLUtil.getGLSLTypeName(type) + " name=" + name + ", location=" + location + ", value = " + toString(value) + "]";
	}
	
	private static String toString(Object o){
		if(o == null)
			return "null";
		
		Class<?> clazz = o.getClass();
		if(clazz.isArray()){
			List<Object> results = new ArrayList<>();
			flatArray(o, results);
			Object[] array = results.toArray();
			return Arrays.toString(array);
		}else{
			return o.toString();
		}
	}
	
	private static void flatArray(Object array, List<Object> results){
		Class<?> clazz = array.getClass();
		if(!clazz.isArray()){
			results.add(array);
			return;
		}
		
		int length = Array.getLength(array);
		for(int i = 0; i < length; i++){
			flatArray(Array.get(array, i), results);
		}
	}
}
