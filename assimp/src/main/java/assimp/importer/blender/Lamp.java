package assimp.importer.blender;

final class Lamp extends ElemBase{

//	enum FalloffType {
	static final int
		 FalloffType_Constant	= 0x0
		,FalloffType_InvLinear	= 0x1
		,FalloffType_InvSquare	= 0x2;
		//,FalloffType_Curve	= 0x3
		//,FalloffType_Sliders	= 0x4
//	};

//	enum Type {
	static final int
		 Type_Local			= 0x0
		,Type_Sun			= 0x1
		,Type_Spot			= 0x2
		,Type_Hemi			= 0x3
		,Type_Area			= 0x4;
		//,Type_YFPhoton	= 0x5
//	};
	
	final ID id = new ID();
    //AnimData *adt;  
    
    int type;
	short flags;

    //int mode;
    
    short colormodel, totex;
    float r,g,b,k;
    //float shdwr, shdwg, shdwb;
    
    float energy, dist, spotsize, spotblend;
    //float haint;
       
    float att1, att2; 
    //struct CurveMapping *curfalloff;
    int falloff_type;
    
    //float clipsta, clipend, shadspotsize;
    //float bias, soft, compressthresh;
    //short bufsize, samp, buffers, filtertype;
    //char bufflag, buftype;
    
    //short ray_samp, ray_sampy, ray_sampz;
    //short ray_samp_type;
    //short area_shape;
	  //float area_size, area_sizey, area_sizez;
	  //float adapt_thresh;
	  //short ray_samp_method;

	  //short texact, shadhalostep;

	  //short sun_effect_type;
	  //short skyblendtype;
	  //float horizon_brightness;
	  //float spread;
	  float sun_brightness;
	  //float sun_size;
	  //float backscattered_light;
	  //float sun_intensity;
	  //float atm_turbidity;
	  //float atm_inscattering_factor;
	  //float atm_extinction_factor;
	  //float atm_distance_factor;
	  //float skyblendfac;
	  //float sky_exposure;
	  //short sky_colorspace;

	  // int YF_numphotons, YF_numsearch;
	  // short YF_phdepth, YF_useqmc, YF_bufsize, YF_pad;
	  // float YF_causticblur, YF_ltradius;

	  // float YF_glowint, YF_glowofs;
    // short YF_glowtype, YF_pad2;
    
    //struct Ipo *ipo;                    
    //struct MTex *mtex[18];              
    // short pr_texture;
    
    //struct PreviewImage *preview;
}
