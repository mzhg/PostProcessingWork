package assimp.importer.blender;

final class MTex extends ElemBase{

//	enum Projection {
	static final int
		 Proj_N = 0
		,Proj_X = 1
		,Proj_Y = 2
		,Proj_Z = 3
		,
//	};

//	enum Flag {
		 Flag_RGBTOINT		= 0x1
		,Flag_STENCIL		= 0x2
		,Flag_NEGATIVE		= 0x4
		,Flag_ALPHAMIX		= 0x8
		,Flag_VIEWSPACE		= 0x10
		,
//	};

//	enum BlendType {
		
		 BlendType_BLEND			= 0
		,BlendType_MUL				= 1
		,BlendType_ADD				= 2
		,BlendType_SUB				= 3
		,BlendType_DIV				= 4
		,BlendType_DARK				= 5
		,BlendType_DIFF				= 6
		,BlendType_LIGHT			= 7
		,BlendType_SCREEN			= 8
		,BlendType_OVERLAY			= 9
		,BlendType_BLEND_HUE		= 10
		,BlendType_BLEND_SAT		= 11
		,BlendType_BLEND_VAL		= 12
		,BlendType_BLEND_COLOR		= 13
		,
//	};

//	enum MapType {
	     MapType_COL         = 1
	    ,MapType_NORM        = 2
	    ,MapType_COLSPEC     = 4
	    ,MapType_COLMIR      = 8
	    ,MapType_REF         = 16
	    ,MapType_SPEC        = 32
	    ,MapType_EMIT        = 64
	    ,MapType_ALPHA       = 128
	    ,MapType_HAR         = 256
	    ,MapType_RAYMIRR     = 512
	    ,MapType_TRANSLU     = 1024
	    ,MapType_AMB         = 2048
	    ,MapType_DISPLACE    = 4096
	    ,MapType_WARP        = 8192
	;

	// short texco, maptoneg;
	int mapto;

	int blendtype;
	BLEObject object;
	Tex tex;
	final byte[] uvname = new byte[32];

	int projx,projy,projz;
	byte mapping;
//	float ofs[3], size[3], rot;
	final float[] ofs = new float[3];
	final float[] size = new float[3];
	float rot;

	int texflag;
	short colormodel, pmapto, pmaptoneg;
	//short normapspace, which_output;
	//char brush_map_mode;
	float r,g,b,k;
	//float def_var, rt;

	//float colfac, varfac;

	float norfac;
	//float dispfac, warpfac;
	float colspecfac, mirrfac, alphafac;
	float difffac, specfac, emitfac, hardfac;
	//float raymirrfac, translfac, ambfac;
	//float colemitfac, colreflfac, coltransfac;
	//float densfac, scatterfac, reflfac;

	//float timefac, lengthfac, clumpfac;
	//float kinkfac, roughfac, padensfac;
	//float lifefac, sizefac, ivelfac, pvelfac;
	//float shadowfac;
	//float zenupfac, zendownfac, blendfac;
	
	public MTex() {}
	
	public MTex(MTex o) {
		set(o);
	}
	
	public void set(MTex o){
		mapto = o.mapto;
		blendtype = o.blendtype;
		object = o.object;
		tex = o.tex;
		System.arraycopy(o.uvname, 0, uvname, 0, o.uvname.length);
		projx = o.projx;
		projy = o.projy;
		projz = o.projz;
		mapping = o.mapping;
		System.arraycopy(o.ofs, 0, ofs, 0, o.ofs.length);
		System.arraycopy(o.size, 0, size, 0, o.size.length);
		rot = o.rot;
		texflag = o.texflag;
		colormodel = o.colormodel;
		pmapto = o.pmapto;
		pmaptoneg = o.pmaptoneg;
		r = o.r;
		g = o.g;
		b = o.b;
		k = o.k;
		norfac = o.norfac;
		colspecfac = o.colspecfac;
		mirrfac = o.mirrfac;
		alphafac = o.alphafac;
		difffac = o.difffac;
		specfac = o.specfac;
		emitfac = o.emitfac;
		hardfac = o.hardfac;
	}

}
