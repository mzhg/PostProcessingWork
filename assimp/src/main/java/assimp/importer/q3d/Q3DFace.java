package assimp.importer.q3d;

import it.unimi.dsi.fastutil.ints.IntArrayList;

final class Q3DFace {

	final IntArrayList indices/* = new IntArrayList()*/;
	final IntArrayList uvindices/* = new IntArrayList()*/;
	int mat;
	
	public Q3DFace(int s) {
		indices = IntArrayList.wrap(new int[s]);
		uvindices = IntArrayList.wrap(new int[s]);
	}
	
	public static void main(String[] args) {
		System.out.println((int)' ');
		System.out.println((int)'\0');
	}
}
