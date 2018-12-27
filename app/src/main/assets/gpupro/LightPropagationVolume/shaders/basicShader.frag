#version 430

#define CASCADES 3

uniform sampler3D RAccumulatorLPV_l0;
uniform sampler3D GAccumulatorLPV_l0;
uniform sampler3D BAccumulatorLPV_l0;

uniform sampler3D RAccumulatorLPV_l1;
uniform sampler3D GAccumulatorLPV_l1;
uniform sampler3D BAccumulatorLPV_l1;

uniform sampler3D RAccumulatorLPV_l2;
uniform sampler3D GAccumulatorLPV_l2;
uniform sampler3D BAccumulatorLPV_l2;

#define PI 3.1415926f

//For MRT attachment 0
layout(location = 0) out vec4 final_color;
layout(location = 1) out vec4 normals;
in vec2 uv;
in vec3 eyePosition,eyeNormal,eyeLightPos;
in vec3 worldPos, worldNorm;
in vec4 shadowCoord;

uniform sampler2D tex;
uniform sampler2DShadow depthTexture;
uniform vec3 v_gridDim;
uniform float f_indirectAttenuation;
uniform bool b_enableGI;
uniform bool b_enableCascades;
uniform bool b_lightIntesityOnly;
uniform bool b_interpolateBorders;
uniform vec3 v_allGridMins[CASCADES];
uniform vec3 v_allCellSizes; //x - level 0 cellSize, y - level 1 cellSize, z - level 2 cellSize

vec3 blendRegion = vec3(0.3);

/*Spherical harmonics coefficients - precomputed*/
#define SH_C0 0.282094792f // 1 / 2sqrt(pi)
#define SH_C1 0.488602512f // sqrt(3/pi) / 2

// no normalization
vec4 evalSH_direct( vec3 direction ) {	
	return vec4( SH_C0, -SH_C1 * direction.y, SH_C1 * direction.z, -SH_C1 * direction.x );
}

bool isInside(vec3 i) {
	if (i.x < 0 || i.x > int(v_gridDim.x))
		return false;
	if (i.y < 0 || i.y > int(v_gridDim.y))
		return false;
	if (i.z < 0 || i.z > int(v_gridDim.z))
		return false;
	return true;
}

ivec4 isBorder(vec3 i) {
	ivec4 ret = ivec4(0);
	if(isInside(i)) {
		if (i.x == 0 || i.x == int(v_gridDim.x)) {
			ret.w = 1;
			ret.x = 1;
		}
		if (i.y == 0 || i.y == int(v_gridDim.y)) {
			ret.w = 1;
			ret.y = 1;
		}
		if (i.z == 0 || i.z == int(v_gridDim.z)) {
			ret.w = 1;
			ret.z = 1;
		}
	}
	return ret;
}


void main()
{
	float shadow = 1.0;
	vec4 coord = shadowCoord / shadowCoord.w;
	if (shadowCoord.w > 0.0) {
		shadow = texture(depthTexture, vec3(coord.xyz)-vec3(0.00001));
		//textureProj does perspective division for me
		//shadow = textureProj(depthTexture, shadowCoord);
		shadow = (shadow > 0) ? 1.0 : 0.05;
	}
	float distance = length(eyeLightPos.xyz-eyePosition.xyz);
	float att=1.0/(0.0005+0.009*distance+0.00035*distance*distance);

	vec3 ld = vec3(1.0,1.0,1.0);
	vec3 ls = vec3(1.0,1.0,1.0);
	vec3 la = vec3(1.0,1.0,1.0);

	vec3 kd = texture(tex, uv).rgb;
	vec3 ks = vec3(0.8,0.8,0.8);
	vec3 ka = vec3(0.1,0.1,0.1);
	vec3 tmpNormal = normalize(eyeNormal);
	vec3 s = normalize(vec3(eyeLightPos - eyePosition));
	vec3 v = normalize(-eyePosition);
	vec3 r = reflect (-s,tmpNormal); 
	float sDotN = max(dot(s, tmpNormal),0.0);
	vec3 diffuse = ld * kd * sDotN; // * kd
	vec3 spec = vec3(0.0);
	if( sDotN > 0.0) {
	    spec = ls * ks *pow(max(dot(r,v),0.0),8);
	}
	vec3 ambient = la * ka;
	


	vec4 SHintensity = evalSH_direct( -worldNorm );
	vec3 lpvIntensity = vec3(0.0);
	if(!b_enableCascades) {
		vec3 lpvCellCoords = (worldPos - v_allGridMins[0]) / v_allCellSizes.x / v_gridDim; //<0,1>
		//lpvCellCoords = clamp(lpvCellCoords, 0.0, 1.0);
		lpvIntensity = vec3( 
			dot( SHintensity, texture( RAccumulatorLPV_l0, lpvCellCoords) ),
			dot( SHintensity, texture( GAccumulatorLPV_l0, lpvCellCoords ) ),
			dot( SHintensity, texture( BAccumulatorLPV_l0, lpvCellCoords ) )
		);
	} else {
		//l2 is the finest
		vec3 lpvCellCoords_l2 = (worldPos - v_allGridMins[2]) / v_allCellSizes.z;
		vec3 lpvCellCoords_l1 = (worldPos - v_allGridMins[1]) / v_allCellSizes.y;
		vec3 lpvCellCoords_l0 = (worldPos - v_allGridMins[0]) / v_allCellSizes.x;

		vec3 lpvIntensity_l2 = vec3(0);
		vec3 lpvIntensity_l1 = vec3(0);
		vec3 lpvIntensity_l0 = vec3(0);

		lpvCellCoords_l2 /= v_gridDim;
		lpvIntensity_l2 = vec3( 
			dot( SHintensity, texture( RAccumulatorLPV_l2, lpvCellCoords_l2) ),
			dot( SHintensity, texture( GAccumulatorLPV_l2, lpvCellCoords_l2 ) ),
			dot( SHintensity, texture( BAccumulatorLPV_l2, lpvCellCoords_l2 ) )
		);

		lpvCellCoords_l1 /= v_gridDim;
		lpvIntensity_l1 = vec3( 
			dot( SHintensity, texture( RAccumulatorLPV_l1, lpvCellCoords_l1) ),
			dot( SHintensity, texture( GAccumulatorLPV_l1, lpvCellCoords_l1 ) ),
			dot( SHintensity, texture( BAccumulatorLPV_l1, lpvCellCoords_l1 ) )
		);

		lpvCellCoords_l0 /= v_gridDim;
		lpvIntensity_l0 = vec3( 
			dot( SHintensity, texture( RAccumulatorLPV_l0, lpvCellCoords_l0) ),
			dot( SHintensity, texture( GAccumulatorLPV_l0, lpvCellCoords_l0 ) ),
			dot( SHintensity, texture( BAccumulatorLPV_l0, lpvCellCoords_l0 ) )
		);


		lpvIntensity = (lpvIntensity_l0 + lpvIntensity_l1 + lpvIntensity_l2);
	}


	vec3 finalLPVRadiance = (f_indirectAttenuation / PI)*  max( lpvIntensity, 0 ) ;

	vec3 GI = vec3(0.0);
	if(b_enableGI) {
		GI = kd * finalLPVRadiance;
	}
	//vec3 lightIntesity =  sDotN * la * kd * shadow + la * spec * shadow + GI;
	vec3 lightIntesity =  sDotN * la * kd * shadow + GI;
	if(b_lightIntesityOnly) {
		final_color = vec4(finalLPVRadiance,1.0);
	} else {
		final_color = vec4(lightIntesity,1.0);
	}

}