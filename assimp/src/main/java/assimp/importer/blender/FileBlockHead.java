package assimp.importer.blender;

/** Describes a master file block header. Each master file sections holds n
 *  elements of a certain SDNA structure (or otherwise unspecified data). */
class FileBlockHead implements Comparable<FileBlockHead>{

	// points right after the header of the file block
	int start;
	
	String id;
	int size;

	// original memory address of the data
	Pointer address;

	// index into DNA
	int dna_index;

	// number of structure instances to follow
	int num;

	@Override
	public int compareTo(FileBlockHead o) {
		return (int) (address.val - o.address.val);
	}
}
