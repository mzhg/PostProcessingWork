package assimp.importer.blender;

final class Library extends ElemBase{

	final ID id = new ID();
	final byte[] name = new byte[240];
	final byte[] filename = new byte[240];
	Library parent;
}
