package assimp.importer.mdl;

import org.lwjgl.util.vector.Vector3f;

/** Data structure for the MDL main header */
public class Header_MDL {
	//! magic number: "IDPO"
	int ident;          

	//! version number: 6
	int version;          

	//! scale factors for each axis
	final Vector3f scale = new Vector3f();				

	//! translation factors for each axis
	final Vector3f translate = new Vector3f();	

	//! bounding radius of the mesh
	float boundingradius;
	 
	//! Position of the viewer's exe. Ignored
	final Vector3f vEyePos = new Vector3f();

	//! Number of textures
	int num_skins;       

	//! Texture width in pixels
	int skinwidth;

	//! Texture height in pixels
	int skinheight;       

	//! Number of vertices contained in the file
	int num_verts;       

	//! Number of triangles contained in the file
	int num_tris;         

	//! Number of frames contained in the file
	int num_frames;      

	//! 0 = synchron, 1 = random . Ignored
	//! (MDLn formats: number of texture coordinates)
	int synctype;         

	//! State flag
	int flags;     

	//! Could be the total size of the file (and not a float)
	float size;
}
