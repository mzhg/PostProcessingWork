package assimp.importer.blender;

import assimp.common.AssimpConfig;
import assimp.common.DeadlyImportError;
import assimp.common.DefaultLogger;
import assimp.common.StreamReader;

/** Utility to read all master file blocks in turn. */
final class SectionParser {

	final FileBlockHead current = new FileBlockHead();
	StreamReader stream;
	boolean ptr64;
	
	FileBlockHead getCurrent() { return current;}
	
	/** @param stream Inout stream, must point to the 
	 *  first section in the file. Call Next() once
	 *  to have it read. 
	 *  @param ptr64 Pointer size in file is 64 bits? */
	public SectionParser(StreamReader stream, boolean ptr64) {
		this.stream = stream;
		this.ptr64 = ptr64;
		
		current.size = current.start = 0;
	}
	
	/** Advance to the next section. 
	 *  @throw DeadlyImportError if the last chunk was passed. */
	void next(){
		stream.setCurrentPos(current.start + current.size);

		final byte[] tmp = {
			stream.getI1(),
			stream.getI1(),
			stream.getI1(),
			stream.getI1()
		};
		current.id = new String(tmp,0, tmp[3] != 0?4:tmp[2] != 0?3:tmp[1] != 0?2:1);

		current.size = stream.getI4();
		current.address.val = ptr64 ? stream.getI8() : stream.getI4();

		current.dna_index = stream.getI4();
		current.num = stream.getI4();

		current.start = stream.getCurrentPos();
		if (stream.getRemainingSizeToLimit() < current.size) {
			throw new DeadlyImportError("BLEND: invalid size of file block");
		}
		
		if(AssimpConfig.ASSIMP_BUILD_BLENDER_DEBUG)
			DefaultLogger.debug(current.id);
	}
}
