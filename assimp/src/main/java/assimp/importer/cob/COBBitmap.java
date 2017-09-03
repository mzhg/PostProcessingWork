package assimp.importer.cob;

import java.nio.ByteBuffer;

/** Embedded bitmap, for instance for the thumbnail image */
final class COBBitmap extends ChunkInfo{

	int orig_size;
	ByteBuffer buff_zipped;
}
