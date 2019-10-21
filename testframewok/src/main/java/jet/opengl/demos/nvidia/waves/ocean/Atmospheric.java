package jet.opengl.demos.nvidia.waves.ocean;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

//-----------------------------------------------------------------------------
// Mie + Raileigh atmospheric scattering code
// based on Sean O'Neil Accurate Atmospheric Scattering
// from GPU Gems 2
//-----------------------------------------------------------------------------
final class Atmospheric {
    static final class AtmosphereColorsType
    {
        final Vector3f RayleighColor = new Vector3f();
        final Vector3f MieColor = new Vector3f();
        final Vector3f Attenuation = new Vector3f();
    }

    static float scale(float fCos, float fScaleDepth)
    {
        float x = 1.0f - fCos;
        return (float) (fScaleDepth * Math.exp(-0.00287f + x*(0.459f + x*(3.83f + x*(-6.80f + x*5.25f)))));
    }

    static float atmospheric_depth(ReadableVector3f position, ReadableVector3f dir){
        float a = Vector3f.dot(dir, dir);
        float b = 2.0f*Vector3f.dot(dir, position);
        float c = Vector3f.dot(position, position)-1.0f;
        float det = b*b-4.0f*a*c;
        float detSqrt = (float) Math.sqrt(det);
        float q = (-b - detSqrt)/2.0f;
        float t1 = c/q;
        return t1;
    }

    static AtmosphereColorsType CalculateAtmosphericScattering(ReadableVector3f EyeVec, ReadableVector3f VecToLight, float LightIntensity)
    {
        AtmosphereColorsType output = new AtmosphereColorsType();
        final int nSamples = 5;
        final float fSamples = 5.0f;

        Vector3f fWavelength = new Vector3f(0.65f,0.57f,0.47f);		// wavelength for the red, green, and blue channels
        Vector3f fInvWavelength = new Vector3f(5.60f,9.47f,20.49f);	// 1 / pow(wavelength, 4) for the red, green, and blue channels
        float fOuterRadius = 6520000.0f;								// The outer (atmosphere) radius
        float fOuterRadius2 = 6520000.0f*6520000.0f;					// fOuterRadius^2
        float fInnerRadius = 6400000.0f;								// The inner (planetary) radius
        float fInnerRadius2 = 6400000.0f*6400000.0f;					// fInnerRadius^2
        float fKrESun = 0.0075f * LightIntensity;						// Kr * ESun	// initially was 0.0025 * 20.0
        float fKmESun = 0.0001f * LightIntensity;						// Km * ESun	// initially was 0.0010 * 20.0;
        float fKr4PI = 0.0075f*4.0f*3.14f;								// Kr * 4 * PI
        float fKm4PI = 0.0001f*4.0f*3.14f;								// Km * 4 * PI
        float fScale = 1.0f/(6520000.0f - 6400000.0f);					// 1 / (fOuterRadius - fInnerRadius)
        float fScaleDepth	= 0.25f;									// The scale depth (i.e. the altitude at which the atmosphere's average density is found)
        float fScaleOverScaleDepth = (1.0f/(6520000.0f - 6400000.0f)) / 0.25f;	// fScale / fScaleDepth
        float G =	-0.98f;												// The Mie phase asymmetry factor
        float G2 = (-0.98f)*(-0.98f);

        // Get the ray from the camera to the vertex, and its length (which is the far point of the ray passing through the atmosphere)
        float d = atmospheric_depth(new Vector3f(0,0,fInnerRadius/fOuterRadius),EyeVec);
//        D3DXVECTOR3 Pos = fOuterRadius*EyeVec*d+D3DXVECTOR3(0,0.0,fInnerRadius);
//        D3DXVECTOR3 Ray = fOuterRadius*EyeVec*d;
        Vector3f Ray = new Vector3f(EyeVec);  Ray.scale(fOuterRadius * d);
        Vector3f Pos = new Vector3f(Ray); Pos.z += fInnerRadius;
        float  Far = Vector3f.length(Ray);
        Ray.scale(1/Far);

        // Calculate the ray's starting position, then calculate its scattering offset
        Vector3f Start = new Vector3f(0,0,fInnerRadius);
        float Height = Vector3f.length(Start);
        float Depth = 1.0f;
        float StartAngle = Vector3f.dot(Ray, Start) / Height;
        float StartOffset = Depth*scale(StartAngle, fScaleDepth);

        // Initialize the scattering loop variables
        float SampleLength = Far / fSamples;
        float ScaledLength = SampleLength * fScale;
        Vector3f SampleRay = Vector3f.scale(Ray, SampleLength, null);
        Vector3f SamplePoint = Vector3f.linear(Start, SampleRay, 0.5f, null);

        // Now loop through the sample points
        Vector3f SkyColor = new Vector3f(0.0f, 0.0f, 0.0f);
        Vector3f Attenuate = output.Attenuation;
        for(int i=0; i<nSamples; i++)
        {
            Height = Vector3f.length(SamplePoint);
            Depth = (float) Math.exp(fScaleOverScaleDepth * (fInnerRadius - Height));
            float LightAngle = Vector3f.dot(VecToLight, SamplePoint) / Height;
            float CameraAngle = Vector3f.dot(Ray, SamplePoint) / Height;
            float Scatter = (StartOffset + Depth*(scale(LightAngle, fScaleDepth) - scale(CameraAngle, fScaleDepth)));
            Attenuate.x = (float) Math.exp(-Scatter * (fInvWavelength.x * fKr4PI + fKm4PI));
            Attenuate.y = (float) Math.exp(-Scatter * (fInvWavelength.y * fKr4PI + fKm4PI));
            Attenuate.z = (float) Math.exp(-Scatter * (fInvWavelength.z * fKr4PI + fKm4PI));
//            SkyColor += Attenuate * (Depth * ScaledLength);
            Vector3f.linear(SkyColor, Attenuate, Depth * ScaledLength, SkyColor);
//            SamplePoint += SampleRay;
            Vector3f.add(SamplePoint, SampleRay, SamplePoint);
        }
//        D3DXVECTOR3 MieColor = SkyColor * fKmESun;
        Vector3f.scale(SkyColor, fKmESun, output.MieColor);
        Vector3f RayleighColor = output.RayleighColor;
        RayleighColor.x = SkyColor.x * (fInvWavelength.x * fKrESun);
        RayleighColor.y = SkyColor.y * (fInvWavelength.y * fKrESun);
        RayleighColor.z = SkyColor.z * (fInvWavelength.z * fKrESun);

        float fcos = -Vector3f.dot(VecToLight, EyeVec) / Vector3f.length(EyeVec);
        float fMiePhase = (float) (1.5f * ((1.0f - G2) / (2.0f + G2)) * (1.0f + fcos*fcos) / Math.pow(1.0f + G2 - 2.0f*G*fcos, 1.5f));
//        output.RayleighColor = RayleighColor;
//        output.MieColor = fMiePhase* MieColor;
        output.MieColor.scale(fMiePhase);
//        output.Attenuation = Attenuate;
        return output;
    }
}
