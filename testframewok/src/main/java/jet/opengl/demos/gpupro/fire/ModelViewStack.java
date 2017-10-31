package jet.opengl.demos.gpupro.fire;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

public final class ModelViewStack {

	Matrix4f[] stack;
	
	final Matrix4f previouse = new Matrix4f();   // The previouse saved matrix.
	final Matrix4f mat = new Matrix4f();         // The combined matrix.
	
	Matrix4f current;
	boolean currentIsDirty;
	
	int count;
	
	public ModelViewStack() {
		this(8);
	}
	
	public ModelViewStack(int size) {
		if(size < 1)
			throw new IllegalArgumentException("size at least 1.");
		
		stack = new Matrix4f[size];
		
		current = stack[0] = new Matrix4f();
		count = 1;
	}
	
	public void pushMatrix(){
		if(count == stack.length){
			throw new RuntimeException("OpenGL ModelView Matrix stack overflow!!!");
		}
		
		if(stack[count] == null)
			stack[count] = new Matrix4f();
		
		savePreviouseMatrix();
		
		current = stack[count];
		currentIsDirty = true;
		
		count++;
	}
	
	protected void updateMat(){
		if(currentIsDirty){
			Matrix4f.mul(previouse, current, mat);
			currentIsDirty = false;
		}
	}
	
	protected void savePreviouseMatrix(){
		if(currentIsDirty){
			Matrix4f.mul(previouse, current, previouse);
		}else{
			previouse.load(mat);
		}
	}

	/** Read-only */
	public Matrix4f getTotalMatrix(){
		updateMat();

		return mat;
	}

	/** Read-only */
	public Matrix4f getCurrentMatrix(){
		return current;
	}
	
	public void popMatrix(){
		if(count == 0){
			throw new RuntimeException("OpenGL ModelView Matrix stack overflow!!!");
		}
		
		count--;
		current = stack[count];
		
		previouse.setIdentity();
		// Re-calculate the previouse matrix.
		// This is not an efficent way and have performance issue.
		for(int i = 0; i < count; i++){
			Matrix4f.mul(previouse, stack[i], previouse);
		}
		
		currentIsDirty = true;
	}
	
	public void loadIdentity(){
		current.setIdentity();
		
		currentIsDirty = true;
	}
	
	public void loadMatrix(Matrix4f mat){
		current.load(mat);
		currentIsDirty = true;
	}
	
	public void loadTransposeMatrix(Matrix4f mat){
		current.load(mat);
		current.transpose();
		currentIsDirty = true;
	}

	public void loadTransposeMatrix(float[] mat, int offset){
		current.loadTranspose(mat, offset);
		currentIsDirty = true;
	}
	
	public void multMatrix(Matrix4f mat){
		Matrix4f.mul(current, mat, current);
		currentIsDirty = true;
	}
	
	public void multTransposeMatrix(Matrix4f mat){
		mat.transpose();
		Matrix4f.mul(current, mat, current);
		mat.transpose();
		currentIsDirty = true;
	}
	
	public void translate(float x, float y, float z){
		current.translate(x, y, z);
		
		currentIsDirty = true;
	}
	
	public void rotate(float angle, float x, float y, float z){
		current.rotate((float)Math.toRadians(angle), x, y, z);
		
		currentIsDirty = true;
	}
	
	public void scale(float x, float y, float z){
		current.scale(x, y, z);
		
		currentIsDirty = true;
	}
	
	public void lookAt(Vector3f position, Vector3f target, Vector3f up){
		Matrix4f tmp = Matrix4f.lookAt(position, target, up, null);
		
		multMatrix(tmp);
	}
}
