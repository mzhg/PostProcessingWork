package jet.opengl.demos.Unreal4.atmosphere;

/** Structure storing Data for pre-computation */
public class FAtmospherePrecomputeParameters {
//    GENERATED_USTRUCT_BODY()

    /** Rayleigh scattering density height scale, ranges from [0...1] */
//    UPROPERTY(EditAnywhere, BlueprintReadOnly, Category=AtmosphereParam)
    public float DensityHeight = 0.5f;

//    UPROPERTY()
    public float DecayHeight_DEPRECATED = 0.5f;

    /** Maximum scattering order */
//    UPROPERTY(EditAnywhere, BlueprintReadOnly, Category=AtmosphereParam)
    public int MaxScatteringOrder = 4;

    /** Transmittance Texture Width */
//    UPROPERTY()
    public int TransmittanceTexWidth = 256;

    /** Transmittance Texture Height */
//    UPROPERTY()
    public int TransmittanceTexHeight = 64;

    /** Irradiance Texture Width */
//    UPROPERTY()
    public int IrradianceTexWidth = 64;

    /** Irradiance Texture Height */
//    UPROPERTY()
    public int IrradianceTexHeight = 16;

    /** Number of different altitudes at which to sample inscatter color (size of 3D texture Z dimension)*/
//    UPROPERTY(EditAnywhere, BlueprintReadOnly, Category=AtmosphereParam)
    public int InscatterAltitudeSampleNum = 2;

    /** Inscatter Texture Height */
//    UPROPERTY()
    public int InscatterMuNum = 128;

    /** Inscatter Texture Width */
//    UPROPERTY()
    public int InscatterMuSNum = 32;

    /** Inscatter Texture Width */
//    UPROPERTY()
    public int InscatterNuNum = 8;
}
