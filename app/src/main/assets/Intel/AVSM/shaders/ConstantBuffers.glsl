layout(binding = 5) uniform ParticlePerFrameConstants
{
    float  mScale;                             // Scene scale factor
    float  mParticleSize;                      // Particles size in (pre)projection space
    float  mParticleOpacity;				   // (Max) Particle contrbution to the CDF
    float  mParticleAlpha;				       // (Max) Particles transparency
    float  mbSoftParticles;				       // Soft Particles Enable-Disable
    float  mSoftParticlesSaturationDepth;      // Saturation Depth for Soft Particles.
};

layout(binding = 6) uniform ParticlePerPassConstants //: register(b7)
{
    float4x4  mParticleWorldViewProj;
    float4x4  mParticleWorldView;
    float4    mEyeRight;
    float4    mEyeUp;
};