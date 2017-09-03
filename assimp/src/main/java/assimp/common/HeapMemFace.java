package assimp.common;

import java.util.Arrays;

class HeapMemFace extends Face{

	int[] indices;
	
	public HeapMemFace(int[] indices) {
		this.indices = indices;
	}
	
	@Override
	public int getNumIndices() {
		return indices == null ? 0 : indices.length;
	}

	@Override
	public void set(int index, int i) {
		indices[index] = i;
	}

	@Override
	public int get(int index) {
		return indices[index];
	}

	@Override
	public Face set(Face o) {
		if(this == o)
			return this;
		
		if(o instanceof HeapMemFace){
			System.arraycopy(((HeapMemFace)o).indices, 0, indices, 0, Math.min(getNumIndices(), o.getNumIndices()));
		}else{
			for(int i = 0; i < getNumIndices(); i++){
				indices[i] = o.get(i);
			}
		}
		
		return this;
	}

	@Override
	public Face copy() {
		return new HeapMemFace(Arrays.copyOf(indices, indices.length));
	}

}
