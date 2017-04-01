package org.lwjgl.util.vector;

public enum Axis {

	X(Vector3f.X_AXIS),
	Y(Vector3f.Y_AXIS), 
	Z(Vector3f.Z_AXIS);
	
	final ReadableVector3f vector;
	
	private Axis(ReadableVector3f vector) {
		this.vector = vector;
	}
	
	/** Return the correspond {@link ReadableVector3f}. */
	public ReadableVector3f get() { return vector;}
}
