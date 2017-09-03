package assimp.importer.hmp;

import org.lwjgl.util.vector.Vector3f;

import assimp.common.AssUtil;

/** Data structure for the HL2 main header */
final class Header_HL2 {
	
	static final int AI_MDL_MAGIC_NUMBER_BE_HL2a = AssUtil.makeMagic("IDST");
	static final int AI_MDL_MAGIC_NUMBER_LE_HL2a = AssUtil.makeMagic("TSDI");
	static final int AI_MDL_MAGIC_NUMBER_BE_HL2b = AssUtil.makeMagic("IDSQ");
	static final int AI_MDL_MAGIC_NUMBER_LE_HL2b = AssUtil.makeMagic("QSDI");

	//! magic number: "IDST"/"IDSQ"
	final byte[]	ident = new byte[4];		

	//! Version number
	int	version;

	//! Original file name in pak ?
	final byte[]	name = new byte[64];

	//! Length of file name/length of file?
	int		length;

	//! For viewer, ignored
	final Vector3f		eyeposition = new Vector3f();	
	final Vector3f		min			= new Vector3f();			
	final Vector3f		max			= new Vector3f();			

	//! AABB of the model
	final Vector3f		bbmin		= new Vector3f();			
	final Vector3f		bbmax		= new Vector3f();		

	// File flags
	int			flags;

	//! NUmber of bones contained in the file
	int			numbones;			
	int			boneindex;

	//! Number of bone controllers for bone animation
	int			numbonecontrollers;		
	int			bonecontrollerindex;

	//! More bounding boxes ...
	int			numhitboxes;			
	int			hitboxindex;			
	
	//! Animation sequences in the file
	int			numseq;				
	int			seqindex;

	//! Loaded sequences. Ignored
	int			numseqgroups;		
	int			seqgroupindex;

	//! Raw texture data
	int			numtextures;		
	int			textureindex;
	int			texturedataindex;

	//! Number of skins (=textures?)
	int			numskinref;			
	int			numskinfamilies;
	int			skinindex;

	//! Number of parts
	int			numbodyparts;		
	int			bodypartindex;

	//! attachable points for gameplay and physics
	int			numattachments;		
	int			attachmentindex;

	//! Table of sound effects associated with the model
	int			soundtable;
	int			soundindex;
	int			soundgroups;
	int			soundgroupindex;

	//! Number of animation transitions
	int			numtransitions;		
	int			transitionindex;
}
