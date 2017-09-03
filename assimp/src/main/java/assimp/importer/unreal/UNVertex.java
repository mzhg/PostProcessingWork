package assimp.importer.unreal;

final class UNVertex {
	int x;
	int y;
	int z;
	
	int compress(){ return (z << 22)|(y << 11) |x;}
	
	void decompress(int in){
		int x_mask = 0b11111111111;
		int z_mask = 0b11111111111;
		
		this.x = in & x_mask;
		this.y = (in >> 11) & x_mask;
		this.z = (in >> 22) & z_mask;
	}
}
