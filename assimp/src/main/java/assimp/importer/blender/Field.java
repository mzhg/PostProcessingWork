package assimp.importer.blender;

/** Represents a single member of a data structure in a BLEND file */
final class Field {

	String name;
	String type;
	
	int size;
	int offset;
	
	/** Size of each array dimension. For flat arrays,
	 *  the second dimension is set to 1. */
	final int[] array_sizes = new int[2];
	
	/** Any of the {@link #FieldFlags} enumerated values */
	int flags;
}
