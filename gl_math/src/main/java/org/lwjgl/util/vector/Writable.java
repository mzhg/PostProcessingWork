package org.lwjgl.util.vector;

import java.nio.ByteBuffer;

public interface Writable {

	/**
	 * Load this vector from a ByteBuffer
	 * @param buf The buffer to load it from, at the current position
	 * @return this
	 */
	Writable load(ByteBuffer buf);
}
