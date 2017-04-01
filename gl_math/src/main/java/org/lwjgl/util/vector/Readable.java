package org.lwjgl.util.vector;

import java.nio.ByteBuffer;

public interface Readable {

	/**
	 * Store this vector in a ByteBuffer
	 * @param buf The buffer to store it in, at the current position
	 * @return this
	 */
	ByteBuffer store(ByteBuffer buf);
}
