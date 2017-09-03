package assimp.common;

class TriangleFace extends Face{

	int i0, i1, i2;
	
	public TriangleFace() {
	}
	
	
	public TriangleFace(int i0, int i1, int i2) {
		this.i0 = i0;
		this.i1 = i1;
		this.i2 = i2;
	}

	@Override
	public int getNumIndices() {return 3;}

	@Override
	public void set(int index, int i) {
		switch (index) {
		case 0: i0 = i; break;
		case 1: i1 = i; break;
		case 2: i2 = i; break;
		default:
			throw new IndexOutOfBoundsException("index = " + index);
		}
	}

	@Override
	public int get(int index) {
		switch (index) {
		case 0: return i0;
		case 1: return i1;
		case 2: return i2;
		default:
			throw new IndexOutOfBoundsException("index = " + index);
		}
		
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + i0;
		result = prime * result + i1;
		result = prime * result + i2;
		return result;
	}

	@Override
	public Face copy() {
		return new TriangleFace(i0, i1, i2);
	}
}
