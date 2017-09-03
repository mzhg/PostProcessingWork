package assimp.importer.blender;

final class Tex extends ElemBase{

	// actually, the only texture type we support is Type_IMAGE
//	enum Type {
	static final int
		 Type_CLOUDS		= 1
		,Type_WOOD			= 2
		,Type_MARBLE		= 3
		,Type_MAGIC			= 4
		,Type_BLEND			= 5
		,Type_STUCCI		= 6
		,Type_NOISE			= 7
		,Type_IMAGE			= 8
		,Type_PLUGIN		= 9
		,Type_ENVMAP		= 10
		,Type_MUSGRAVE		= 11
		,Type_VORONOI		= 12
		,Type_DISTNOISE		= 13
		,Type_POINTDENSITY	= 14
		,Type_VOXELDATA		= 15
	;

//	enum ImageFlags {
	static final int
	     ImageFlags_INTERPOL    	 = 1
	    ,ImageFlags_USEALPHA    	 = 2
	    ,ImageFlags_MIPMAP      	 = 4
	    ,ImageFlags_IMAROT      	 = 16
	    ,ImageFlags_CALCALPHA   	 = 32
	    ,ImageFlags_NORMALMAP   	 = 2048
	    ,ImageFlags_GAUSS_MIP   	 = 4096
	    ,ImageFlags_FILTER_MIN  	 = 8192
	    ,ImageFlags_DERIVATIVEMAP   = 16384
	;

	final ID id = new ID();
	// AnimData *adt; 

	//float noisesize, turbul;
	//float bright, contrast, rfac, gfac, bfac;
	//float filtersize;

	//float mg_H, mg_lacunarity, mg_octaves, mg_offset, mg_gain;
	//float dist_amount, ns_outscale;

	//float vn_w1;
	//float vn_w2;
	//float vn_w3;
	//float vn_w4;
	//float vn_mexp;
	//short vn_distm, vn_coltype;

	//short noisedepth, noisetype;
	//short noisebasis, noisebasis2;

	//short flag;
	int imaflag;
	int type;
	//short stype;

	//float cropxmin, cropymin, cropxmax, cropymax;
	//int texfilter;
	//int afmax;  
	//short xrepeat, yrepeat;
	//short extend;

	//short fie_ima;
	//int len;
	//int frames, offset, sfra;

	//float checkerdist, nabla;
	//float norfac;

	//ImageUser iuser;

	//bNodeTree *nodetree;
	//Ipo *ipo;                  
	BLEImage ima;
	//PluginTex *plugin;
	//ColorBand *coba;
	//EnvMap *env;
	//PreviewImage * preview;
	//PointDensity *pd;
	//VoxelData *vd;

	//char use_nodes;
	
	static String getTextureTypeDisplayString(int t)
	{
		switch (t)	{
		case Type_CLOUDS		:  return  "Clouds";			
		case Type_WOOD			:  return  "Wood";			
		case Type_MARBLE		:  return  "Marble";			
		case Type_MAGIC			:  return  "Magic";		
		case Type_BLEND			:  return  "Blend";			
		case Type_STUCCI		:  return  "Stucci";			
		case Type_NOISE			:  return  "Noise";			
		case Type_PLUGIN		:  return  "Plugin";			
		case Type_MUSGRAVE		:  return  "Musgrave";		
		case Type_VORONOI		:  return  "Voronoi";			
		case Type_DISTNOISE		:  return  "DistortedNoise";	
		case Type_ENVMAP		:  return  "EnvMap";	
		case Type_IMAGE			:  return  "Image";	
		default: 
			break;
		}
		return "<Unknown>";
	}
}
