in PSIn_Diffuse
{
//	vec4 position;
	vec2 texcoord;
	vec3 normal;
	vec3 positionWS;
	vec4 layerdef;
	vec4 depthmap_scaler;
}_input;

layout (location = 0) out vec4 Out_f4Color;
#include "RenderHeightfieldCommon.glsl"

const float kTopDownDataPixelsPerMeter = 256.0f/700.0; // taken from SDF generation source code, the SDF texture size is 256x256, the viewport size is 700x700
const float kMaxDepthBelowSea = 50.0f;
const float kMaxDistance = 20.0f; // taken from SDF generation code
const float kNumWaves = 1.0; // Total number of Gerster waves of different amplitude, speed etc to calculate, 
									// i+1-th wave has 20% smaller amplitude, 
								    // 20% smaller phase and group speed and 20% less parallelity
							        // Note that all the waves will share the same gerstnerMultiplierOut (lerping between ocean waves and Gerstner waves) for simplicity
const float kBackWaveSpeed = 0.5;  // the speed of wave rolling back from shore, in vertical dimension, in meters/sec

void GetGerstnerShoreAttributes(float3 posWS, out float waveOut, out float3 normalOut, out float foamTrailOut, out float2 foamWSShift, out float waterLayerOut, out float waterLayerSlowOut)
{
	// getting UV for fetching SDF texture 
	float4 topDownPosition = mul( float4( posWS.xyz, 1), g_WorldToTopDownTextureMatrix );
	float2 uv = topDownPosition.xy/topDownPosition.w * 0.5f + 0.5f;
//	uv.y = 1-uv.y;  TODO

	// initializing the outputs
	normalOut = float3(0.0,1.0,0.0);
	waveOut = 0;
	foamWSShift = float2(0.0,0.0);
	foamTrailOut = 0;
	waterLayerOut = 0;
	waterLayerSlowOut = 0;

	// getting SDF
	const float4 tdData = g_DataTexture.SampleLevel(SamplerLinearBorder, uv, 0 );
	
	// getting terrain altitude gradient in y meters per xz meter
	float terrain_dy = 0.25*(tdData.y - g_DataTexture(g_DataTexture, uv - kTopDownDataPixelsPerMeter*float2(tdData.z,-tdData.w)/256.0, 0 ).y);  // SamplerLinearBorder

	// initializing variables common to all Gerstner waves
	float phaseShift = g_Time;
	float sdfPhase = tdData.x*kMaxDistance/kTopDownDataPixelsPerMeter; 
	float distanceMultiplier = saturate(1.0-tdData.x); // Shore waves linearly fade in on the edges of SDF
	float depthMultiplier = saturate((g_BaseGerstnerWavelength*0.5 + tdData.y)); // Shore waves fade in when depth is less than half the wave length

	// initializing variables to be changed along summing up the waves
	float gerstnerWavelength = g_BaseGerstnerWavelength;
	float gerstnerOmega = 2.0*3.141592 / g_BaseGerstnerWavelength; // angular speed of gerstner wave
	float gerstnerParallelness = g_BaseGerstnerParallelness; // "parallelness" of shore waves. 0 means the waves are parallel to shore, 1 means the waves are parallel to wind gradient
	float gerstnerSpeed = g_BaseGerstnerSpeed; // phase speed of gerstner waves
	float gerstnerAmplitude = g_BaseGerstnerAmplitude; 
	float2 windDirection = g_WindDirection;

	// summing up the waves
	for(float i = 0.0; i < kNumWaves; i+=1.0)
	{
		float windPhase = dot(windDirection, posWS.xz); 
		float gerstnerPhase = 2.0*3.141592*(lerp( sdfPhase, windPhase, gerstnerParallelness)/gerstnerWavelength); 
		float2 propagationDirection = normalize( lerp(-tdData.zw + windDirection * 0.000001f, g_WindDirection, gerstnerParallelness*gerstnerParallelness));
		float gerstnerGroupSpeedPhase = 2.0*3.141592*(lerp( sdfPhase, windPhase, gerstnerParallelness*3.0)/gerstnerWavelength); // letting the group speed phase to be non-parallel to propagation phase, so altering parallelness modificator fot this

		float groupSpeedMultiplier = 0.5 + 0.5*cos((gerstnerGroupSpeedPhase + gerstnerOmega*gerstnerSpeed*phaseShift/2.0)/2.7); // Group speed for water waves is half of the phase speed, we allow 2.7 wavelengths to be in wave group, not so much as breaking shore waves lose energy quickly
		float worldSpacePosMultiplier = 0.75 + 0.25*sin(phaseShift*0.3 + 0.5*posWS.x/gerstnerWavelength)*sin(phaseShift*0.4 + 0.5*posWS.y/gerstnerWavelength); // slowly crawling worldspace aligned checkerboard pattern that damps gerstner waves further
		float depthMultiplier = saturate((gerstnerWavelength*0.5 + tdData.y)*0.5); // Shore waves fade in when depth is less than half the wave length
		float gerstnerMultiplier = distanceMultiplier*depthMultiplier*worldSpacePosMultiplier*groupSpeedMultiplier; 
	
		float steepness = gerstnerMultiplier * g_GerstnerSteepness;	// steepness gradually increases as wave runs over shallower seabed
		float baseAmplitude = gerstnerMultiplier * gerstnerAmplitude; //amplitude gradually increases as wave runs over shallower seabed
		float skewMultiplier = saturate((baseAmplitude*2.0*1.28 + tdData.y)/gerstnerAmplitude); // Wave height is 2*amplitude, a wave will start to break when it approximately reaches a water depth of 1.28 times the wave height, empirically: http://passyworldofmathematics.com/mathematics-of-ocean-waves-and-surfing/ 
		float breakerMultiplier = saturate((baseAmplitude*2.0*1.28 + tdData.y)/gerstnerAmplitude); // Wave height is 2*amplitude, a wave will start to break when it approximately reaches a water depth of 1.28 times the wave height, empirically: http://passyworldofmathematics.com/mathematics-of-ocean-waves-and-surfing/ 

		// calculating Gerstner offset
		float s,c;
		sincos(gerstnerPhase + gerstnerOmega*gerstnerSpeed*phaseShift, s, c);
		float waveVerticalOffset = (s*0.5+0.5)*(s*0.5+0.5);

		// calculating normal
		normalOut.y -= gerstnerOmega*steepness*baseAmplitude*s;
		normalOut.xz -= gerstnerOmega*baseAmplitude*c*propagationDirection;   // orienting normal according to direction of wave propagation. No need to normalize, it is unit length.

		// calculating foam parameters
		foamTrailOut += gerstnerMultiplier*breakerMultiplier;

		// calculating wave falling edges moving slow and fast
		float foamTrailPhase = gerstnerPhase + gerstnerOmega*gerstnerSpeed*phaseShift + 3.141592*0.05; // delaying foam trail a bit so it's following the breaker
		float fp = frac(foamTrailPhase/(3.141592*2.0));

		float k = kBackWaveSpeed*terrain_dy/((gerstnerSpeed/gerstnerWavelength)*baseAmplitude);
		float sawtooth = 1.0 - k + k*(saturate(fp*10.0) - saturate(fp*1.1));
		waterLayerOut += sawtooth*baseAmplitude + baseAmplitude;

		k = kBackWaveSpeed/(gerstnerOmega*gerstnerSpeed);
		sawtooth = k*(saturate(fp*10.0) - saturate(fp*1.1));

		foamWSShift += 10.0*sawtooth*propagationDirection*gerstnerAmplitude;

		k = 0.33*kBackWaveSpeed*terrain_dy/((gerstnerSpeed/gerstnerWavelength)*baseAmplitude);
		sawtooth = 1.0 - k + k*(saturate(fp*10.0) - saturate(fp*1.1));
		waterLayerSlowOut += sawtooth*baseAmplitude + baseAmplitude;

		waveOut += waveVerticalOffset*baseAmplitude;
		
		// updating the parameters for next wave
		gerstnerWavelength *= 0.66;
		gerstnerOmega /= 0.66;
		gerstnerSpeed *= 0.66;
		gerstnerAmplitude *= 0.66; 
		gerstnerParallelness *= 0.66;
		windDirection.xy *= float2(-1.0,1.0)*windDirection.yx; // rotating wind direction
	}
}

void main()
{
    float3 color;
    float3 pixel_to_light_vector = normalize(g_LightPosition-_input.positionWS);
    float3 pixel_to_eye_vector = normalize(g_CameraPosition-_input.positionWS);

    // culling halfspace if needed
    clip(g_HalfSpaceCullSign*(_input.positionWS.y-g_HalfSpaceCullPosition));

    float darkening_change_rate = min(1.0,1.0/(3.0*g_BaseGerstnerAmplitude));
    float shore_darkening_factor = saturate((_input.positionWS.y + 1.0)*darkening_change_rate);

    // getting diffuse color
    color = float3(0.3,0.3,0.3);

    // adding per-vertex lighting defined by displacement of vertex
    color*=0.5+0.5*min(1.0,max(0.0, _input.brightness/3.0f+0.5f));

    // calculating pixel position in light view space
    float4 positionLS = mul(float4(_input.positionWS,1),g_LightModelViewProjectionMatrix);
    positionLS.xyz/=positionLS.w;
    positionLS.x=(positionLS.x+1)*0.5;
    positionLS.y=(positionLS.y+1)*0.5;
    positionLS.z=(positionLS.z+1)*0.5;

    // fetching shadowmap and shading
    float dsf = 0.66f/4096.0f;
    float shadow_factor = 0.2*texture(g_ShadowmapTexture,float3(positionLS.xy,positionLS.z* 0.99f)).r;   // SamplerDepthAnisotropic
    shadow_factor+=0.2*texture(g_ShadowmapTexture,float3(positionLS.xy+float2(dsf,dsf),positionLS.z* 0.99f)).r;
    shadow_factor+=0.2*texture(g_ShadowmapTexture,float3(positionLS.xy+float2(-dsf,dsf),positionLS.z* 0.99f)).r;
    shadow_factor+=0.2*texture(g_ShadowmapTexture,float3(positionLS.xy+float2(dsf,-dsf),positionLS.z* 0.99f)).r;
    shadow_factor+=0.2*texture(g_ShadowmapTexture,float3(positionLS.xy+float2(-dsf,-dsf),positionLS.z* 0.99f)).r;
    color *= g_AtmosphereBrightColor*max(0,dot(pixel_to_light_vector,_input.normal))*shadow_factor;

    // making all brighter
    color*=2.0;
    // adding light from the sky
    color += (0.0+0.2*max(0,(dot(float3(0,1,0),_input.normal))))*g_AtmosphereDarkColor;


    // calculating shore effects
    if((g_enableShoreEffects > 0) && (shore_darkening_factor < 1.0))
    {
        float3 normal;
        float foam_trail;
        float water_layer;
        float water_layer_slow;
        float wave_pos;
        float2 foamWSShift;

        GetGerstnerShoreAttributes(_input.positionWS, wave_pos, normal, foam_trail, foamWSShift, water_layer, water_layer_slow);

        float waterlayer_change_rate = max(2.0,1.0/(0.1 + water_layer_slow - water_layer));

        float underwater_factor = saturate((_input.positionWS.y - wave_pos + 2.0)*5.0);
        float darkening_factor = saturate((_input.positionWS.y - g_BaseGerstnerAmplitude*2.0 + 2.0)*1.0);
        float fresnel_damp_factor = saturate((_input.positionWS.y + 0.1 - wave_pos + 2.0)*5.0);
        float shore_waterlayer_factor_windy = saturate((_input.positionWS.y - water_layer + 2.0)*waterlayer_change_rate);
        float shore_waterlayer_factor_calm = saturate((_input.positionWS.y + 2.0)*10.0);
        float shore_waterlayer_factor = lerp(shore_waterlayer_factor_calm, shore_waterlayer_factor_windy, saturate(g_BaseGerstnerAmplitude*5.0));
        float shore_foam_lower_bound_factor = saturate((_input.positionWS.y + g_BaseGerstnerAmplitude - wave_pos + 2.0)*min(3.0,3.0/(2.0*g_BaseGerstnerAmplitude)));


        float3 reflected_eye_to_pixel_vector=-pixel_to_eye_vector+2*dot(pixel_to_eye_vector,_input.normal)*_input.normal;
        float specular_light = pow(max(0,dot(reflected_eye_to_pixel_vector,pixel_to_light_vector)),40.0);

        // calculating fresnel factor
        float r = (1.0 - 1.33)*(1.0 - 1.33)/((1.0 + 1.33)*(1.0 + 1.33));
        float fresnel_factor = r + (1.0-r)*pow(saturate(1.0 - dot(_input.normal,pixel_to_eye_vector)),5.0);

        fresnel_factor *= (1.0-shore_waterlayer_factor)*fresnel_damp_factor;

        // darkening the terrain close to water
        color *= 0.6 + 0.4*darkening_factor;
        // darkening terrain underwater
        color *= min(1.0,exp((_input.positionWS.y + 2.0)));

        // adding specular
        color += 5.0*g_AtmosphereBrightColor*specular_light*shadow_factor*fresnel_factor;

        // calculating reflection color
        float3 reflection_color = CalculateFogColor(pixel_to_light_vector,-reflected_eye_to_pixel_vector);

        color = lerp(color, reflection_color.rgb, fresnel_factor);

        // adding foam
        float2 positionWS_shifted = _input.positionWS.xz + foamWSShift;
        float foam_intensity_map_lf = texture(g_FoamIntensityTexture, positionWS_shifted*0.04*float2(1,1)).x - 1.0;  // SamplerLinearWrap
        float foam_intensity_map_hf = texture(g_FoamIntensityTexture, positionWS_shifted*0.15*float2(1,1)).x - 1.0;

        float foam_intensity;
        float k = 1.5;
        float ff2 = (2.0/g_BaseGerstnerAmplitude)*saturate(_input.positionWS.y - water_layer*0.8 + 2.0);
        float ff = (1.0-ff2)*shore_foam_lower_bound_factor*foam_trail;
        foam_intensity = saturate(foam_intensity_map_hf + min(3.5,k*ff-0.2));
        foam_intensity += (foam_intensity_map_lf + min(1.5,k*ff));
        foam_intensity = max(0.0, foam_intensity);
        float foam_bubbles = texture(g_FoamDiffuseTexture, positionWS_shifted*0.5).r;   // SamplerLinearWrap
        foam_bubbles = saturate(5.0*(foam_bubbles-0.8));
        foam_intensity = pow(foam_intensity, 0.7);
        foam_intensity = saturate(foam_intensity*foam_bubbles*1.0);

        // foam diffuse color
        float foam_diffuse_factor = max(0,0.8+max(0,0.2*dot(pixel_to_light_vector,normal)));

        color = lerp(color, foam_diffuse_factor*float3(1.0,1.0,1.0),foam_intensity);
    }

    // applying fog
    if(g_ApplyFog > 0)
    {
        color = lerp(CalculateFogColor(pixel_to_light_vector,pixel_to_eye_vector).rgb, color, min(1,exp(-length(g_CameraPosition-_input.positionWS)*g_FogDensity)));
    }


    Out_f4Color = float4(color, length(g_CameraPosition-_input.positionWS));
}