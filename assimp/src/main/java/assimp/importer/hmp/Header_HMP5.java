package assimp.importer.hmp;

import java.nio.ByteBuffer;

import assimp.common.AssUtil;

/** Data structure for the header of a HMP5 file.
 *  This is also used by HMP4 and HMP7, but with modifications
*/
final class Header_HMP5 {

	final byte[] ident = new byte[4]; // "HMP5"
	int		version;
	
	// ignored
	final float[]	scale = new float[3];
	final float[]	scale_origin = new float[3];
	float	boundingradius;
	
	//! Size of one triangle in x direction
	float	ftrisize_x;		
	//! Size of one triangle in y direction
	float	ftrisize_y;		
	//! Number of vertices in x direction
	float	fnumverts_x;	
							
	//! Number of skins in the file
	int		numskins;

	// can ignore this?
	int		skinwidth;
	int		skinheight;

	//!Number of vertices in the file
	int		numverts;

	// ignored and zero
	int		numtris;

	//! only one supported ...
	int		numframes;		

	//! Always 0 ...
	int		num_stverts;	
	int		flags;
	float	size;
	
	public static void main(String[] args) {
		AssUtil.genLoadBytebuffer(Header_HMP5.class);
	}
	
	Header_HMP5 load(ByteBuffer buf){
		int old_pos = buf.position();
		buf.get(ident);
		version = buf.getInt();
		for(int i = 0; i < scale.length; i++)
			scale[i] = buf.getFloat();
		for(int i = 0; i < scale_origin.length; i++)
			scale_origin[i] = buf.getFloat();
		boundingradius = buf.getFloat();
		ftrisize_x = buf.getFloat();
		ftrisize_y = buf.getFloat();
		fnumverts_x = buf.getFloat();
		numskins = buf.getInt();
		skinwidth = buf.getInt();
		skinheight = buf.getInt();
		numverts = buf.getInt();
		numtris = buf.getInt();
		numframes = buf.getInt();
		num_stverts = buf.getInt();
		flags = buf.getInt();
		size = buf.getFloat();

		buf.position(old_pos);
		return this;
	}
}
