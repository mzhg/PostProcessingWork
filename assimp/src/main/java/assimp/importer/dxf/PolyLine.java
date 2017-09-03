package assimp.importer.dxf;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.nio.FloatBuffer;

import assimp.common.AssUtil;

//represents a POLYLINE or a LWPOLYLINE. or even a 3DFACE The data is converted as needed.
final class PolyLine {

	FloatBuffer positions;
	FloatBuffer colors;
	IntArrayList indices;
	IntArrayList counts;
	int flags;
	
	String layer;
	String desc;
	
	PolyLine() {}
	
	PolyLine(PolyLine o) {
		positions = AssUtil.copyOf(positions);
		
		colors = o.colors;
		indices = o.indices;
		counts = o.counts;
		flags = o.flags;
		layer = o.layer;
		desc = o.desc;
	}
	
	int getNumPositions(){ return positions != null ? positions.remaining()/3 : 0;}
}
