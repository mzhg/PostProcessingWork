//-----------------------------------------------------------------------------
// Mie + Raileigh atmospheric scattering code
// based on Sean O'Neil Accurate Atmospheric Scattering
// from GPU Gems 2
//-----------------------------------------------------------------------------

float scale(float fCos, float fScaleDepth)
{
    float x = 1.0 - fCos;
    return fScaleDepth * exp(-0.00287 + x*(0.459 + x*(3.83 + x*(-6.80 + x*5.25))));
}

float atmospheric_depth(float3 position, float3 dir){
    float a = dot(dir, dir);
    float b = 2.0*dot(dir, position);
    float c = dot(position, position)-1.0;
    float det = b*b-4.0*a*c;
    float detSqrt = sqrt(det);
    float q = (-b - detSqrt)/2.0;
    float t1 = c/q;
    return t1;
}

struct AtmosphereColorsType
{
    float3 RayleighColor;
    float3 MieColor;
    float3 Attenuation;
} ;

AtmosphereColorsType CalculateAtmosphericScattering(float3 EyeVec, float3 VecToLight, float LightIntensity)
{
    AtmosphereColorsType _output;
    const int nSamples = 5;
    const float fSamples = 5.0;

    float3 fWavelength = float3(0.65,0.57,0.47);				// wavelength for the red, green, and blue channels
    float3 fInvWavelength = float3(5.60,9.47,20.49);			// 1 / pow(wavelength, 4) for the red, green, and blue channels
    float fOuterRadius = 6520000.0;								// The outer (atmosphere) radius
    float fOuterRadius2 = 6520000.0*6520000.0;					// fOuterRadius^2
    float fInnerRadius = 6400000.0;								// The inner (planetary) radius
    float fInnerRadius2 = 6400000.0*6400000.0;					// fInnerRadius^2
    float fKrESun = 0.0075 * LightIntensity;					// Kr * ESun	// initially was 0.0025 * 20.0
    float fKmESun = 0.0001 * LightIntensity;					// Km * ESun	// initially was 0.0010 * 20.0;
    float fKr4PI = 0.0075*4.0*3.14;								// Kr * 4 * PI
    float fKm4PI = 0.0001*4.0*3.14;								// Km * 4 * PI
    float fScale = 1.0/(6520000.0 - 6400000.0);					// 1 / (fOuterRadius - fInnerRadius)
    float fScaleDepth	= 0.25;									// The scale depth (i.e. the altitude at which the atmosphere's average density is found)
    float fScaleOverScaleDepth = (1.0/(6520000.0 - 6400000.0)) / 0.25;	// fScale / fScaleDepth
    float G =	-0.98;											// The Mie phase asymmetry factor
    float G2 = (-0.98)*(-0.98);

    // Get the ray from the camera to the vertex, and its length (which is the far point of the ray passing through the atmosphere)
    float d = atmospheric_depth(float3(0,0,fInnerRadius/fOuterRadius),EyeVec);
    float3 Pos = fOuterRadius*EyeVec*d+float3(0,0.0,fInnerRadius);
    float3 Ray = fOuterRadius*EyeVec*d;
    float  Far = length(Ray);
    Ray /= Far;



    // Calculate the ray's starting position, then calculate its scattering offset
    float3 Start = float3(0,0,fInnerRadius);
    float Height = length(Start);
    float Depth = 1.0;
    float StartAngle = dot(Ray, Start) / Height;
    float StartOffset = Depth*scale(StartAngle, fScaleDepth);

    // Initialize the scattering loop variables
    float SampleLength = Far / fSamples;
    float ScaledLength = SampleLength * fScale;
    float3 SampleRay = Ray * SampleLength;
    float3 SamplePoint = Start + SampleRay * 0.5;

    // Now loop through the sample points
    float3 SkyColor = float3(0.0, 0.0, 0.0);
    float3 Attenuate;
    for(int i=0; i<nSamples; i++)
    {
        float Height = length(SamplePoint);
        float Depth = exp(fScaleOverScaleDepth * (fInnerRadius - Height));
        float LightAngle = dot(VecToLight, SamplePoint) / Height;
        float CameraAngle = dot(Ray, SamplePoint) / Height;
        float Scatter = (StartOffset + Depth*(scale(LightAngle, fScaleDepth) - scale(CameraAngle, fScaleDepth)));
        Attenuate = exp(-Scatter * (fInvWavelength * fKr4PI + fKm4PI));
        SkyColor += Attenuate * (Depth * ScaledLength);
        SamplePoint += SampleRay;
    }
    float3 MieColor = SkyColor * fKmESun;
    float3 RayleighColor = SkyColor * (fInvWavelength * fKrESun);

    float fcos = dot(VecToLight, -EyeVec) / length(EyeVec);
    float fMiePhase = 1.5 * ((1.0 - G2) / (2.0 + G2)) * (1.0 + fcos*fcos) / pow(1.0 + G2 - 2.0*G*fcos, 1.5);
    _output.RayleighColor = RayleighColor;
    _output.MieColor = fMiePhase* MieColor;
    _output.Attenuation = Attenuate;
    return _output;
}