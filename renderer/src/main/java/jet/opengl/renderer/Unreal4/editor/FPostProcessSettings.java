package jet.opengl.renderer.Unreal4.editor;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureCube;
import jet.opengl.renderer.Unreal4.scenes.EReflectedAndRefractedRayTracedShadows;
import jet.opengl.renderer.Unreal4.scenes.EReflectionsType;
import jet.opengl.renderer.Unreal4.scenes.ETranslucencyType;

/** To be able to use struct PostProcessSettings. */
// Each property consists of a bool to enable it (by default off),
// the variable declaration and further down the default value for it.
// The comment should include the meaning and usable range.
public class FPostProcessSettings implements MetaHint{

    /** first all bOverride_... as they get grouped together into bitfields<br>*/
    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_WhiteTemp;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_WhiteTint;

    /** Color Correction controls<br>*/
    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_ColorSaturation;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_ColorContrast;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_ColorGamma;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_ColorGain;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_ColorOffset;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_ColorSaturationShadow;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_ColorContrastShadow;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_ColorGammaShadow;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_ColorGainShadow;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_ColorOffsetShadow;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_ColorSaturationMidtone;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_ColorContrastMidtone;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_ColorGammaMidtone;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_ColorGainMidtone;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_ColorOffsetMidtone;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_ColorSaturationHighlight;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_ColorContrastHighlight;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_ColorGammaHighlight;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_ColorGainHighlight;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_ColorOffsetHighlight;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_ColorCorrectionShadowsMa;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_ColorCorrectionHighlightsMi;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_BlueCorrectio;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_ExpandGamu;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_FilmWhitePoin;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_FilmSaturatio;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_FilmChannelMixerRe;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_FilmChannelMixerGree;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_FilmChannelMixerBlu;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_FilmContras;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_FilmDynamicRang;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_FilmHealAmoun;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_FilmToeAmoun;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_FilmShadowTin;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_FilmShadowTintBlen;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_FilmShadowTintAmoun;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_FilmSlop;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_FilmTo;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_FilmShoulde;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_FilmBlackCli;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_FilmWhiteCli;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_SceneColorTin;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_SceneFringeIntensit;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_ChromaticAberrationStartOffse;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_AmbientCubemapTin;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_AmbientCubemapIntensit;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_BloomMetho;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_BloomIntensit;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_BloomThreshol;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_Bloom1Tin;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_Bloom1Siz;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_Bloom2Siz;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_Bloom2Tin;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_Bloom3Tin;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_Bloom3Siz;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_Bloom4Tin;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_Bloom4Siz;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_Bloom5Tin;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_Bloom5Siz;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_Bloom6Tin;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_Bloom6Siz;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_BloomSizeScal;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_BloomConvolutionTextur;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_BloomConvolutionSiz;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_BloomConvolutionCenterU;

    public boolean bOverride_BloomConvolutionPreFilter_DEPRECATE;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_BloomConvolutionPreFilterMi;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_BloomConvolutionPreFilterMa;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_BloomConvolutionPreFilterMul;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_BloomConvolutionBufferScal;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_BloomDirtMaskIntensit;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_BloomDirtMaskTin;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_BloomDirtMas;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_CameraShutterSpee;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_CameraIS;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_AutoExposureMetho;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_AutoExposureLowPercen;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_AutoExposureHighPercen;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_AutoExposureMinBrightnes;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_AutoExposureMaxBrightnes;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_AutoExposureCalibrationConstan;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_AutoExposureSpeedU;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_AutoExposureSpeedDow;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_AutoExposureBia;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_AutoExposureBiasCurv;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_HistogramLogMi;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_HistogramLogMa;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_LensFlareIntensit;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_LensFlareTin;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_LensFlareTint;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_LensFlareBokehSiz;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_LensFlareBokehShap;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_LensFlareThreshol;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_VignetteIntensit;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_GrainIntensit;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_GrainJitte;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_AmbientOcclusionIntensit;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_AmbientOcclusionStaticFractio;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_AmbientOcclusionRadiu;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_AmbientOcclusionFadeDistanc;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_AmbientOcclusionFadeRadiu;

    public boolean bOverride_AmbientOcclusionDistance_DEPRECATE;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_AmbientOcclusionRadiusInW;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_AmbientOcclusionPowe;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_AmbientOcclusionBia;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_AmbientOcclusionQualit;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_AmbientOcclusionMipBlen;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_AmbientOcclusionMipScal;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_AmbientOcclusionMipThreshol;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_RayTracingA;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_RayTracingAOSamplesPerPixe;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_LPVIntensit;

    @EditAnywhere
    @Category(value = "Overrides")
    @Meta(Hints = InlineEditConditionToggle)
    public boolean bOverride_LPVDirectionalOcclusionIntensit;

    @EditAnywhere
    @Category(value = "Overrides")
    @Meta(Hints = InlineEditConditionToggle)
    public boolean bOverride_LPVDirectionalOcclusionRadiu;

    @EditAnywhere
    @Category(value = "Overrides")
    @Meta(Hints = InlineEditConditionToggle)
    public boolean bOverride_LPVDiffuseOcclusionExponen;

    @EditAnywhere
    @Category(value = "Overrides")
    @Meta(Hints = InlineEditConditionToggle)
    public boolean bOverride_LPVSpecularOcclusionExponen;

    @EditAnywhere
    @Category(value = "Overrides")
    @Meta(Hints = InlineEditConditionToggle)
    public boolean bOverride_LPVDiffuseOcclusionIntensit;

    @EditAnywhere
    @Category(value = "Overrides")
    @Meta(Hints = InlineEditConditionToggle)
    public boolean bOverride_LPVSpecularOcclusionIntensit;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_LPVSiz;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_LPVSecondaryOcclusionIntensit;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_LPVSecondaryBounceIntensit;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_LPVGeometryVolumeBia;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_LPVVplInjectionBia;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_LPVEmissiveInjectionIntensit;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_LPVFadeRang;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_LPVDirectionalOcclusionFadeRang;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_IndirectLightingColo;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_IndirectLightingIntensit;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_ColorGradingIntensit;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_ColorGradingLU;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_DepthOfFieldFocalDistanc;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_DepthOfFieldFsto;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_DepthOfFieldMinFsto;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_DepthOfFieldBladeCoun;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_DepthOfFieldSensorWidt;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_DepthOfFieldDepthBlurRadiu;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_DepthOfFieldDepthBlurAmoun;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_DepthOfFieldFocalRegio;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_DepthOfFieldNearTransitionRegio;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_DepthOfFieldFarTransitionRegio;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_DepthOfFieldScal;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_DepthOfFieldNearBlurSiz;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_DepthOfFieldFarBlurSiz;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_MobileHQGaussia;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_DepthOfFieldOcclusio;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_DepthOfFieldSkyFocusDistanc;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_DepthOfFieldVignetteSiz;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_MotionBlurAmoun;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_MotionBlurMa;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_MotionBlurTargetFP;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_MotionBlurPerObjectSiz;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_ScreenPercentag;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_ScreenSpaceReflectionIntensit;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_ScreenSpaceReflectionQualit;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_ScreenSpaceReflectionMaxRoughnes;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault)
    public boolean bOverride_ScreenSpaceReflectionRoughnessScal;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault|InlineEditConditionToggle)
    public boolean bOverride_ReflectionsTyp;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault|InlineEditConditionToggle)
    public boolean bOverride_RayTracingReflectionsMaxRoughnes;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault|InlineEditConditionToggle)
    public boolean bOverride_RayTracingReflectionsMaxBounce;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault|InlineEditConditionToggle)
    public boolean bOverride_RayTracingReflectionsSamplesPerPixe;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault|InlineEditConditionToggle)
    public boolean bOverride_RayTracingReflectionsShadow;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault|InlineEditConditionToggle)
    public boolean bOverride_TranslucencyTyp;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault|InlineEditConditionToggle)
    public boolean bOverride_RayTracingTranslucencyMaxRoughnes;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault|InlineEditConditionToggle)
    public boolean bOverride_RayTracingTranslucencyRefractionRay;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault|InlineEditConditionToggle)
    public boolean bOverride_RayTracingTranslucencySamplesPerPixe;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault|InlineEditConditionToggle)
    public boolean bOverride_RayTracingTranslucencyShadow;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault|InlineEditConditionToggle)
    public boolean bOverride_RayTracingTranslucencyRefractio;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault|InlineEditConditionToggle)
    public boolean bOverride_RayTracingG;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault|InlineEditConditionToggle)
    public boolean bOverride_RayTracingGIMaxBounce;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault|InlineEditConditionToggle)
    public boolean bOverride_RayTracingGISamplesPerPixe;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault|InlineEditConditionToggle)
    public boolean bOverride_PathTracingMaxBounce;

    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Overrides")
    @Meta(Hints = PinHiddenByDefault|InlineEditConditionToggle)
    public boolean bOverride_PathTracingSamplesPerPixe;

    /*** Enable HQ Gaussian on high end mobile platforms. (ES3_1) <br>*/
    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Lens|Mobile Depth of Field")
    @Meta(Editcondition = "bOverride_MobileHQGaussian",DisplayName = "High Quality Gaussian DoF on Mobile")
    public boolean bMobileHQGaussia;

    /*** Bloom algorithm <br>*/
    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Lens|Bloom")
    @Meta(Editcondition = "bOverride_BloomMethod",DisplayName = "Method")
    public EBloomMethod BloomMethod;

    /*** Luminance computation method <br>*/
    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Lens|Exposure")
    @Meta(Editcondition = "bOverride_AutoExposureMethod",DisplayName = "Metering Mode")
    public EAutoExposureMethod AutoExposureMethod;

    @Interp
    @BlueprintReadWrite
    @Category(value = "Color Grading|WhiteBalance")
    @Meta(UIMin = 1500.0f,UIMax = 15000.0f,Editcondition = "bOverride_WhiteTemp",DisplayName = "Temp")
    public float WhiteTemp;

    @Interp
    @BlueprintReadWrite
    @Category(value = "Color Grading|WhiteBalance")
    @Meta(UIMin = -1.0f,UIMax = 1.0f,Editcondition = "bOverride_WhiteTint",DisplayName = "Tint")
    public float WhiteTint;

    /** Color Correction controls<br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Color Grading|Global")
    @Meta(UIMin = 0.0f,UIMax = 2.0f,Delta = 0.01f,ColorGradingMode = "saturation",ShiftMouseMovePixelPerDelta = 10,SupportDynamicSliderMaxValue = true,Editcondition = "bOverride_ColorSaturation",DisplayName = "Saturation")
    public final Vector4f ColorSaturation = new Vector4f();

    @Interp
    @BlueprintReadWrite
    @Category(value = "Color Grading|Global")
    @Meta(UIMin = 0.0f,UIMax = 2.0f,Delta = 0.01f,ColorGradingMode = "contrast",ShiftMouseMovePixelPerDelta = 10,SupportDynamicSliderMaxValue = true,Editcondition = "bOverride_ColorContrast",DisplayName = "Contrast")
    public final Vector4f ColorContrast = new Vector4f();

    @Interp
    @BlueprintReadWrite
    @Category(value = "Color Grading|Global")
    @Meta(UIMin = 0.0f,UIMax = 2.0f,Delta = 0.01f,ColorGradingMode = "gamma",ShiftMouseMovePixelPerDelta = 10,SupportDynamicSliderMaxValue = true,Editcondition = "bOverride_ColorGamma",DisplayName = "Gamma")
    public final Vector4f ColorGamma = new Vector4f();

    @Interp
    @BlueprintReadWrite
    @Category(value = "Color Grading|Global")
    @Meta(UIMin = 0.0f,UIMax = 2.0f,Delta = 0.01f,ColorGradingMode = "gain",ShiftMouseMovePixelPerDelta = 10,SupportDynamicSliderMaxValue = true,Editcondition = "bOverride_ColorGain",DisplayName = "Gain")
    public final Vector4f ColorGain = new Vector4f();

    @Interp
    @BlueprintReadWrite
    @Category(value = "Color Grading|Global")
    @Meta(UIMin = -1.0f,UIMax = 1.0f,Delta = 0.001f,ColorGradingMode = "offset",ShiftMouseMovePixelPerDelta = 20,SupportDynamicSliderMaxValue = true,SupportDynamicSliderMinValue = true,Editcondition = "bOverride_ColorOffset",DisplayName = "Offset")
    public final Vector4f ColorOffset = new Vector4f();

    @Interp
    @BlueprintReadWrite
    @Category(value = "Color Grading|Shadows")
    @Meta(UIMin = 0.0f,UIMax = 2.0f,Delta = 0.01f,ColorGradingMode = "saturation",ShiftMouseMovePixelPerDelta = 10,SupportDynamicSliderMaxValue = true,Editcondition = "bOverride_ColorSaturationShadows",DisplayName = "Saturation")
    public final Vector4f ColorSaturationShadows = new Vector4f();

    @Interp
    @BlueprintReadWrite
    @Category(value = "Color Grading|Shadows")
    @Meta(UIMin = 0.0f,UIMax = 2.0f,Delta = 0.01f,ColorGradingMode = "contrast",ShiftMouseMovePixelPerDelta = 10,SupportDynamicSliderMaxValue = true,Editcondition = "bOverride_ColorContrastShadows",DisplayName = "Contrast")
    public final Vector4f ColorContrastShadows = new Vector4f();

    @Interp
    @BlueprintReadWrite
    @Category(value = "Color Grading|Shadows")
    @Meta(UIMin = 0.0f,UIMax = 2.0f,Delta = 0.01f,ColorGradingMode = "gamma",ShiftMouseMovePixelPerDelta = 10,SupportDynamicSliderMaxValue =true,Editcondition = "bOverride_ColorGammaShadows",DisplayName = "Gamma")
    public final Vector4f ColorGammaShadows = new Vector4f();

    @Interp
    @BlueprintReadWrite
    @Category(value = "Color Grading|Shadows")
    @Meta(UIMin = 0.0f,UIMax = 2.0f,Delta = 0.01f,ColorGradingMode = "gain",ShiftMouseMovePixelPerDelta = 10,SupportDynamicSliderMaxValue = true,Editcondition = "bOverride_ColorGainShadows",DisplayName = "Gain")
    public final Vector4f ColorGainShadows = new Vector4f();

    @Interp
    @BlueprintReadWrite
    @Category(value = "Color Grading|Shadows")
    @Meta(UIMin = -1.0f,UIMax = 1.0f,Delta = 0.001f,ColorGradingMode = "offset",ShiftMouseMovePixelPerDelta = 20,SupportDynamicSliderMaxValue = true,SupportDynamicSliderMinValue = true,Editcondition = "bOverride_ColorOffsetShadows",DisplayName = "Offset")
    public final Vector4f ColorOffsetShadows = new Vector4f();

    @Interp
    @BlueprintReadWrite
    @Category(value = "Color Grading|Midtones")
    @Meta(UIMin = 0.0f,UIMax = 2.0f,Delta = 0.01f,ColorGradingMode = "saturation",ShiftMouseMovePixelPerDelta = 10,SupportDynamicSliderMaxValue = true,Editcondition = "bOverride_ColorSaturationMidtones",DisplayName = "Saturation")
    public final Vector4f ColorSaturationMidtones = new Vector4f();

    @Interp
    @BlueprintReadWrite
    @Category(value = "Color Grading|Midtones")
    @Meta(UIMin = 0.0f,UIMax = 2.0f,Delta = 0.01f,ColorGradingMode = "contrast",ShiftMouseMovePixelPerDelta = 10,SupportDynamicSliderMaxValue = true,Editcondition = "bOverride_ColorContrastMidtones",DisplayName = "Contrast")
    public final Vector4f ColorContrastMidtones = new Vector4f();

    @Interp
    @BlueprintReadWrite
    @Category(value = "Color Grading|Midtones")
    @Meta(UIMin = 0.0f,UIMax = 2.0f,Delta = 0.01f,ColorGradingMode = "gamma",ShiftMouseMovePixelPerDelta = 10,SupportDynamicSliderMaxValue = true,Editcondition = "bOverride_ColorGammaMidtones",DisplayName = "Gamma")
    public final Vector4f ColorGammaMidtones = new Vector4f();

    @Interp
    @BlueprintReadWrite
    @Category(value = "Color Grading|Midtones")
    @Meta(UIMin = 0.0f,UIMax = 2.0f,Delta = 0.01f,ColorGradingMode = "gain",ShiftMouseMovePixelPerDelta = 10,SupportDynamicSliderMaxValue = true,Editcondition = "bOverride_ColorGainMidtones",DisplayName = "Gain")
    public final Vector4f ColorGainMidtones = new Vector4f();

    @Interp
    @BlueprintReadWrite
    @Category(value = "Color Grading|Midtones")
    @Meta(UIMin = -1.0f,UIMax = 1.0f,Delta = 0.001f,ColorGradingMode = "offset",ShiftMouseMovePixelPerDelta = 20,SupportDynamicSliderMaxValue = true,SupportDynamicSliderMinValue = true,Editcondition = "bOverride_ColorOffsetMidtones",DisplayName = "Offset")
    public final Vector4f ColorOffsetMidtones = new Vector4f();

    @Interp
    @BlueprintReadWrite
    @Category(value = "Color Grading|Highlights")
    @Meta(UIMin = 0.0f,UIMax = 2.0f,Delta = 0.01f,ColorGradingMode = "saturation",ShiftMouseMovePixelPerDelta = 10,SupportDynamicSliderMaxValue = true,Editcondition = "bOverride_ColorSaturationHighlights",DisplayName = "Saturation")
    public final Vector4f ColorSaturationHighlights = new Vector4f();

    @Interp
    @BlueprintReadWrite
    @Category(value = "Color Grading|Highlights")
    @Meta(UIMin = 0.0f,UIMax = 2.0f,Delta = 0.01f,ColorGradingMode = "contrast",ShiftMouseMovePixelPerDelta = 10,SupportDynamicSliderMaxValue = true,Editcondition = "bOverride_ColorContrastHighlights",DisplayName = "Contrast")
    public final Vector4f ColorContrastHighlights = new Vector4f();

    @Interp
    @BlueprintReadWrite
    @Category(value = "Color Grading|Highlights")
    @Meta(UIMin = 0.0f,UIMax = 2.0f,Delta = 0.01f,ColorGradingMode = "gamma",ShiftMouseMovePixelPerDelta = 10,SupportDynamicSliderMaxValue = true,Editcondition = "bOverride_ColorGammaHighlights",DisplayName = "Gamma")
    public final Vector4f ColorGammaHighlights = new Vector4f();

    @Interp
    @BlueprintReadWrite
    @Category(value = "Color Grading|Highlights")
    @Meta(UIMin = 0.0f,UIMax = 2.0f,Delta = 0.01f,ColorGradingMode = "gain",ShiftMouseMovePixelPerDelta = 10,SupportDynamicSliderMaxValue = true,Editcondition = "bOverride_ColorGainHighlights",DisplayName = "Gain")
    public final Vector4f ColorGainHighlights = new Vector4f();

    @Interp
    @BlueprintReadWrite
    @Category(value = "Color Grading|Highlights")
    @Meta(UIMin = -1.0f,UIMax = 1.0f,Delta = 0.001f,ColorGradingMode = "offset",ShiftMouseMovePixelPerDelta = 20,SupportDynamicSliderMaxValue =true,SupportDynamicSliderMinValue = true,Editcondition = "bOverride_ColorOffsetHighlights",DisplayName = "Offset")
    public final Vector4f ColorOffsetHighlights = new Vector4f();

    @Interp
    @BlueprintReadWrite
    @Category(value = "Color Grading|Highlights")
    @Meta(UIMin = -1.0f,UIMax = 1.0f,Editcondition = "bOverride_ColorCorrectionHighlightsMin",DisplayName = "HighlightsMin")
    public float ColorCorrectionHighlightsMin;

    @Interp
    @BlueprintReadWrite
    @Category(value = "Color Grading|Shadows")
    @Meta(UIMin = -1.0f,UIMax = 1.0f,Editcondition = "bOverride_ColorCorrectionShadowsMax",DisplayName = "ShadowsMax")
    public float ColorCorrectionShadowsMax;

    /*** Correct for artifacts with "electric" blues due to the ACEScg color space. Bright blue desaturates instead of going to violet. <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Color Grading|Misc")
    @Meta(ClampMin = 0.0f,ClampMax = 1.0f,Editcondition = "bOverride_BlueCorrection")
    public float BlueCorrection;

    /*** Expand bright saturated colors outside the sRGB gamut to fake wide gamut rendering. <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Color Grading|Misc")
    @Meta(ClampMin = 0.0f,UIMax = 1.0f,Editcondition = "bOverride_ExpandGamut")
    public float ExpandGamut;

    @Interp
    @BlueprintReadWrite
    @Category(value = "Film")
    @Meta(UIMin = 0.0f,UIMax = 1.0f,Editcondition = "bOverride_FilmSlope",DisplayName = "Slope")
    public float FilmSlope;

    @Interp
    @BlueprintReadWrite
    @Category(value = "Film")
    @Meta(UIMin = 0.0f,UIMax = 1.0f,Editcondition = "bOverride_FilmToe",DisplayName = "Toe")
    public float FilmToe;

    @Interp
    @BlueprintReadWrite
    @Category(value = "Film")
    @Meta(UIMin = 0.0f,UIMax = 1.0f,Editcondition = "bOverride_FilmShoulder",DisplayName = "Shoulder")
    public float FilmShoulder;

    @Interp
    @BlueprintReadWrite
    @Category(value = "Film")
    @Meta(UIMin = 0.0f,UIMax = 1.0f,Editcondition = "bOverride_FilmBlackClip",DisplayName = "Black clip")
    public float FilmBlackClip;

    @Interp
    @BlueprintReadWrite
    @Category(value = "Film")
    @Meta(UIMin = 0.0f,UIMax = 1.0f,Editcondition = "bOverride_FilmWhiteClip",DisplayName = "White clip")
    public float FilmWhiteClip;

    @Interp
    @BlueprintReadWrite
    @Category(value = "Film")
    @Meta(Editcondition = "bOverride_FilmWhitePoint",DisplayName = "Tint",Hints = HideAlphaChannel|LegacyTonemapper)
    public final Vector4f FilmWhitePoint = new Vector4f();

    @Interp
    @BlueprintReadWrite
    @Category(value = "Film")
    @AdvancedDisplay
    @Meta(Editcondition = "bOverride_FilmShadowTint",DisplayName = "Tint Shadow",Hints = HideAlphaChannel|LegacyTonemapper)
    public final Vector4f FilmShadowTint = new Vector4f();

    @Interp
    @BlueprintReadWrite
    @Category(value = "Film")
    @AdvancedDisplay
    @Meta(UIMin = 0.0f,UIMax = 1.0f,Editcondition = "bOverride_FilmShadowTintBlend",DisplayName = "Tint Shadow Blend",Hints = LegacyTonemapper)
    public float FilmShadowTintBlend;

    @Interp
    @BlueprintReadWrite
    @Category(value = "Film")
    @AdvancedDisplay
    @Meta(UIMin = 0.0f,UIMax = 1.0f,Editcondition = "bOverride_FilmShadowTintAmount",DisplayName = "Tint Shadow Amount",Hints = LegacyTonemapper)
    public float FilmShadowTintAmount;

    @Interp
    @BlueprintReadWrite
    @Category(value = "Film")
    @Meta(UIMin = 0.0f,UIMax = 2.0f,Editcondition = "bOverride_FilmSaturation",DisplayName = "Saturation",Hints = LegacyTonemapper)
    public float FilmSaturation;

    @Interp
    @BlueprintReadWrite
    @Category(value = "Film")
    @AdvancedDisplay
    @Meta(Editcondition = "bOverride_FilmChannelMixerRed",DisplayName = "Channel Mixer Red",Hints = HideAlphaChannel|LegacyTonemapper)
    public final Vector4f FilmChannelMixerRed = new Vector4f();

    @Interp
    @BlueprintReadWrite
    @Category(value = "Film")
    @AdvancedDisplay
    @Meta(Editcondition = "bOverride_FilmChannelMixerGreen",DisplayName = "Channel Mixer Green",Hints = HideAlphaChannel|LegacyTonemapper)
    public final Vector4f FilmChannelMixerGreen = new Vector4f();

    @Interp
    @BlueprintReadWrite
    @Category(value = "Film")
    @AdvancedDisplay
    @Meta(Editcondition = "bOverride_FilmChannelMixerBlue",DisplayName = " Channel Mixer Blue",Hints = HideAlphaChannel|LegacyTonemapper)
    public final Vector4f FilmChannelMixerBlue = new Vector4f();

    @Interp
    @BlueprintReadWrite
    @Category(value = "Film")
    @Meta(UIMin = 0.0f,UIMax = 1.0f,Editcondition = "bOverride_FilmContrast",DisplayName = "Contrast",Hints = LegacyTonemapper)
    public float FilmContrast;

    @Interp
    @BlueprintReadWrite
    @Category(value = "Film")
    @AdvancedDisplay
    @Meta(UIMin = 0.0f,UIMax = 1.0f,Editcondition = "bOverride_FilmToeAmount",DisplayName = "Crush Shadows",Hints = LegacyTonemapper)
    public float FilmToeAmount;

    @Interp
    @BlueprintReadWrite
    @Category(value = "Film")
    @AdvancedDisplay
    @Meta(UIMin = 0.0f,UIMax = 1.0f,Editcondition = "bOverride_FilmHealAmount",DisplayName = "Crush Highlights",Hints = LegacyTonemapper)
    public float FilmHealAmount;

    @Interp
    @BlueprintReadWrite
    @Category(value = "Film")
    @AdvancedDisplay
    @Meta(UIMin = 1.0f,UIMax = 4.0f,Editcondition = "bOverride_FilmDynamicRange",DisplayName = "Dynamic Range",Hints = LegacyTonemapper)
    public float FilmDynamicRange;

    /*** Scene tint color <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Color Grading|Misc")
    @Meta(Editcondition = "bOverride_SceneColorTint",Hints = HideAlphaChannel)
    public final Vector4f SceneColorTint = new Vector4f();

    /*** in percent, Scene chromatic aberration / color fringe (camera imperfection) to simulate an artifact that happens in real-world lens, mostly visible in the image corners. <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Chromatic Aberration")
    @Meta(UIMin = 0.0f,UIMax = 5.0f,Editcondition = "bOverride_SceneFringeIntensity",DisplayName = "Intensity")
    public float SceneFringeIntensity;

    /*** A normalized distance to the center of the framebuffer where the effect takes place. <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Chromatic Aberration")
    @Meta(UIMin = 0.0f,UIMax = 1.0f,Editcondition = "bOverride_ChromaticAberrationStartOffset",DisplayName = "Start Offset")
    public float ChromaticAberrationStartOffset;

    /*** Multiplier for all bloom contributions >=0: off, 1(default), >1 brighter <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Bloom")
    @Meta(ClampMin = 0.0f,UIMax = 8.0f,Editcondition = "bOverride_BloomIntensity",DisplayName = "Intensity")
    public float BloomIntensity;

    /***<br>
     * minimum brightness the bloom starts having effect<br>
     * -1:all pixels affect bloom equally (physically correct, faster as a threshold pass is omitted), 0:all pixels affect bloom brights more, 1(default), >1 brighter<br>
     <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Bloom")
    @Meta(ClampMin = -1.0f,UIMax = 8.0f,Editcondition = "bOverride_BloomThreshold",DisplayName = "Threshold")
    public float BloomThreshold;

    /***<br>
     * Scale for all bloom sizes<br>
     <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Bloom")
    @AdvancedDisplay
    @Meta(ClampMin = 0.0f,UIMax = 64.0f,Editcondition = "bOverride_BloomSizeScale",DisplayName = "Size scale")
    public float BloomSizeScale;

    /***<br>
     * Diameter size for the Bloom1 in percent of the screen width<br>
     * (is done in 1/2 resolution, larger values cost more performance, good for high frequency details)<br>
     * >=0: can be clamped because of shader limitations<br>
     <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Bloom")
    @AdvancedDisplay
    @Meta(ClampMin = 0.0f,UIMax = 4.0f,Editcondition = "bOverride_Bloom1Size",DisplayName = "#1 Size")
    public float Bloom1Size;

    /***<br>
     * Diameter size for Bloom2 in percent of the screen width<br>
     * (is done in 1/4 resolution, larger values cost more performance)<br>
     * >=0: can be clamped because of shader limitations<br>
     <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Bloom")
    @AdvancedDisplay
    @Meta(ClampMin = 0.0f,UIMax = 8.0f,Editcondition = "bOverride_Bloom2Size",DisplayName = "#2 Size")
    public float Bloom2Size;

    /***<br>
     * Diameter size for Bloom3 in percent of the screen width<br>
     * (is done in 1/8 resolution, larger values cost more performance)<br>
     * >=0: can be clamped because of shader limitations<br>
     <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Bloom")
    @AdvancedDisplay
    @Meta(ClampMin = 0.0f,UIMax = 16.0f,Editcondition = "bOverride_Bloom3Size",DisplayName = "#3 Size")
    public float Bloom3Size;

    /***<br>
     * Diameter size for Bloom4 in percent of the screen width<br>
     * (is done in 1/16 resolution, larger values cost more performance, best for wide contributions)<br>
     * >=0: can be clamped because of shader limitations<br>
     <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Bloom")
    @AdvancedDisplay
    @Meta(ClampMin = 0.0f,UIMax = 32.0f,Editcondition = "bOverride_Bloom4Size",DisplayName = "#4 Size")
    public float Bloom4Size;

    /***<br>
     * Diameter size for Bloom5 in percent of the screen width<br>
     * (is done in 1/32 resolution, larger values cost more performance, best for wide contributions)<br>
     * >=0: can be clamped because of shader limitations<br>
     <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Bloom")
    @AdvancedDisplay
    @Meta(ClampMin = 0.0f,UIMax = 64.0f,Editcondition = "bOverride_Bloom5Size",DisplayName = "#5 Size")
    public float Bloom5Size;

    /***<br>
     * Diameter size for Bloom6 in percent of the screen width<br>
     * (is done in 1/64 resolution, larger values cost more performance, best for wide contributions)<br>
     * >=0: can be clamped because of shader limitations<br>
     <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Bloom")
    @AdvancedDisplay
    @Meta(ClampMin = 0.0f,UIMax = 128.0f,Editcondition = "bOverride_Bloom6Size",DisplayName = "#6 Size")
    public float Bloom6Size;

    /*** Bloom1 tint color <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Bloom")
    @AdvancedDisplay
    @Meta(Editcondition = "bOverride_Bloom1Tint",DisplayName = "#1 Tint",Hints = HideAlphaChannel)
    public final Vector4f Bloom1Tint = new Vector4f();

    /*** Bloom2 tint color <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Bloom")
    @AdvancedDisplay
    @Meta(Editcondition = "bOverride_Bloom2Tint",DisplayName = "#2 Tint",Hints = HideAlphaChannel)
    public final Vector4f Bloom2Tint = new Vector4f();

    /*** Bloom3 tint color <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Bloom")
    @AdvancedDisplay
    @Meta(Editcondition = "bOverride_Bloom3Tint",DisplayName = "#3 Tint",Hints = HideAlphaChannel)
    public final Vector4f Bloom3Tint = new Vector4f();

    /*** Bloom4 tint color <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Bloom")
    @AdvancedDisplay
    @Meta(Editcondition = "bOverride_Bloom4Tint",DisplayName = "#4 Tint",Hints = HideAlphaChannel)
    public final Vector4f Bloom4Tint = new Vector4f();

    /*** Bloom5 tint color <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Bloom")
    @AdvancedDisplay
    @Meta(Editcondition = "bOverride_Bloom5Tint",DisplayName = "#5 Tint",Hints = HideAlphaChannel)
    public final Vector4f Bloom5Tint = new Vector4f();

    /*** Bloom6 tint color <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Bloom")
    @AdvancedDisplay
    @Meta(Editcondition = "bOverride_Bloom6Tint",DisplayName = "#6 Tint",Hints = HideAlphaChannel)
    public final Vector4f Bloom6Tint = new Vector4f();

    /*** Relative size of the convolution kernel image compared to the minor axis of the viewport  <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Bloom")
    @AdvancedDisplay
    @Meta(ClampMin = 0.0f,UIMax = 1.0f,Editcondition = "bOverride_BloomConvolutionSize",DisplayName = "Convolution Scale")
    public float BloomConvolutionSize;

    /*** Texture to replace default convolution bloom kernel <br>*/
    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Lens|Bloom")
    @Meta(Editcondition = "bOverride_BloomConvolutionTexture",DisplayName = "Convolution Kernel")
    public Texture2D BloomConvolutionTexture;

    /*** The UV location of the center of the kernel.  Should be very close to (.5,.5) <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Bloom")
    @AdvancedDisplay
    @Meta(Editcondition = "bOverride_BloomConvolutionCenterUV",DisplayName = "Convolution Center")
    public final Vector2f BloomConvolutionCenterUV = new Vector2f();

    /** Boost intensity of select pixels  prior to computing bloom convolution (Min, Max, Multiplier).  Max < Min disables <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Bloom")
    @AdvancedDisplay
    @Meta(Editcondition = "bOverride_BloomConvolutionPreFilterMin",DisplayName = "Convolution Boost Min")
    public float BloomConvolutionPreFilterMin;

    /** Boost intensity of select pixels  prior to computing bloom convolution (Min, Max, Multiplier).  Max < Min disables <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Bloom")
    @AdvancedDisplay
    @Meta(Editcondition = "bOverride_BloomConvolutionPreFilterMax",DisplayName = "Convolution Boost Max")
    public float BloomConvolutionPreFilterMax;

    /** Boost intensity of select pixels  prior to computing bloom convolution (Min, Max, Multiplier).  Max < Min disables <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Bloom")
    @AdvancedDisplay
    @Meta(Editcondition = "bOverride_BloomConvolutionPreFilterMult",DisplayName = "Convolution Boost Mult")
    public float BloomConvolutionPreFilterMult;

    /** Implicit buffer region as a fraction of the screen size to insure the bloom does not wrap across the screen.  Larger sizes have perf impact.<br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Bloom")
    @AdvancedDisplay
    @Meta(ClampMin = 0.0f,UIMax = 1.0f,Editcondition = "bOverride_BloomConvolutionBufferScale",DisplayName = "Convolution Buffer")
    public float BloomConvolutionBufferScale;

    /**<br>
     * Texture that defines the dirt on the camera lens where the light of very bright objects is scattered.<br>
     <br>*/
    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Lens|Dirt Mask")
    @Meta(Editcondition = "bOverride_BloomDirtMask",DisplayName = "Dirt Mask Texture")
    public Texture2D BloomDirtMask;

    /** BloomDirtMask intensity <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Dirt Mask")
    @Meta(ClampMin = 0.0f,UIMax = 8.0f,Editcondition = "bOverride_BloomDirtMaskIntensity",DisplayName = "Dirt Mask Intensity")
    public float BloomDirtMaskIntensity;

    /** BloomDirtMask tint color <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Dirt Mask")
    @Meta(Editcondition = "bOverride_BloomDirtMaskTint",DisplayName = "Dirt Mask Tint",Hints = HideAlphaChannel)
    public final Vector4f BloomDirtMaskTint = new Vector4f();

    /** AmbientCubemap tint color <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Ambient Cubemap")
    @Meta(Editcondition = "bOverride_AmbientCubemapTint",DisplayName = "Tint",Hints = HideAlphaChannel)
    public final Vector4f AmbientCubemapTint = new Vector4f();

    /**<br>
     * To scale the Ambient cubemap brightness<br>
     * >=0: off, 1(default), >1 brighter<br>
     <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Ambient Cubemap")
    @Meta(ClampMin = 0.0f,UIMax = 4.0f,Editcondition = "bOverride_AmbientCubemapIntensity",DisplayName = "Intensity")
    public float AmbientCubemapIntensity;

    /** The Ambient cubemap (Affects diffuse and specular shading), blends additively which if different from all other settings here <br>*/
    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Ambient Cubemap")
    @Meta(DisplayName = "Cubemap Texture")
    public TextureCube AmbientCubemap;

    /** The camera shutter in seconds.<br>*/
    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Lens|Camera")
    @Meta(ClampMin = 1.0f,ClampMax = 2000.0f,Editcondition = "bOverride_CameraShutterSpeed",DisplayName = "Shutter Speed (1/s)")
    public float CameraShutterSpeed;

    /** The camera sensor sensitivity in ISO.<br>*/
    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Lens|Camera")
    @Meta(ClampMin = 1.0f,Tooltip = "The camera sensor sensitivity",Editcondition = "bOverride_CameraISO",DisplayName = "ISO")
    public float CameraISO;

    /** Defines the opening of the camera lens, Aperture is 1/fstop, typical lens go down to f/1.2 (large opening), larger numbers reduce the DOF effect <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Camera")
    @Meta(ClampMin = 1.0f,ClampMax = 32.0f,Editcondition = "bOverride_DepthOfFieldFstop",DisplayName = "Aperture (F-stop)")
    public float DepthOfFieldFstop;

    /** Defines the maximum opening of the camera lens to control the curvature of blades of the diaphragm. Set it to 0 to get straight blades. <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Camera")
    @Meta(ClampMin = 0.0f,ClampMax = 32.0f,Editcondition = "bOverride_DepthOfFieldMinFstop",DisplayName = "Maximum Aperture (min F-stop)")
    public float DepthOfFieldMinFstop;

    /** Defines the number of blades of the diaphragm within the lens (between 4 and 16). <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Camera")
    @Meta(ClampMin = 4,ClampMax = 16,Editcondition = "bOverride_DepthOfFieldBladeCount",DisplayName = "Number of diaphragm blades")
    public int DepthOfFieldBladeCount;

    /**<br>
     * Logarithmic adjustment for the exposure. Only used if a tonemapper is specified.<br>
     * 0: no adjustment, -1:2x darker, -2:4x darker, 1:2x brighter, 2:4x brighter, ...<br>
     <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Exposure")
    @Meta(UIMin = -8.0f,UIMax = 8.0f,Editcondition = "bOverride_AutoExposureBias",DisplayName = "Exposure Compensation ")
    public float AutoExposureBias;

    /*<br>
     * Exposure compensation based on the scene EV100.<br>
     * Used to calibrate the final exposure differently depending on the average scene luminance.<br>
     * 0: no adjustment, -1:2x darker, -2:4x darker, 1:2x brighter, 2:4x brighter, ...<br>
     <br>
    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Lens|Exposure")
    @Meta(Editcondition = "bOverride_AutoExposureBiasCurve",DisplayName = "Exposure Compensation Curve")
    public class UCurveFloat* AutoExposureBiasCurve = nullptr;*/

    /**<br>
     * The eye adaptation will adapt to a value extracted from the luminance histogram of the scene color.<br>
     * The value is defined as having x percent below this brightness. Higher values give bright spots on the screen more priority<br>
     * but can lead to less stable results. Lower values give the medium and darker values more priority but might cause burn out of<br>
     * bright spots.<br>
     * >0, <100, good values are in the range 70 .. 80<br>
     <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Exposure")
    @AdvancedDisplay
    @Meta(ClampMin = 0.0f,ClampMax = 100.0f,Editcondition = "bOverride_AutoExposureLowPercent",DisplayName = "Low Percent")
    public float AutoExposureLowPercent;

    /**<br>
     * The eye adaptation will adapt to a value extracted from the luminance histogram of the scene color.<br>
     * The value is defined as having x percent below this brightness. Higher values give bright spots on the screen more priority<br>
     * but can lead to less stable results. Lower values give the medium and darker values more priority but might cause burn out of<br>
     * bright spots.<br>
     * >0, <100, good values are in the range 80 .. 95<br>
     <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Exposure")
    @AdvancedDisplay
    @Meta(ClampMin = 0.0f,ClampMax = 100.0f,Editcondition = "bOverride_AutoExposureHighPercent",DisplayName = "High Percent")
    public float AutoExposureHighPercent;

    /**<br>
     * Auto-Exposure minimum adaptation. Eye Adaptation is disabled if Min = Max. <br>
     * Auto-exposure is implemented by choosing an exposure value for which the average luminance generates a pixel brightness equal to the Constant Calibration value.<br>
     * The Min/Max are expressed in pixel luminance (cd/m2) or in EV100 when using ExtendDefaultLuminanceRange (see project settings).<br>
     <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Exposure")
    @Meta(ClampMin = -10.0f,UIMax = 20.0f,Editcondition = "bOverride_AutoExposureMinBrightness",DisplayName = "Min Brightness")
    public float AutoExposureMinBrightness;

    /**<br>
     * Auto-Exposure maximum adaptation. Eye Adaptation is disabled if Min = Max. <br>
     * Auto-exposure is implemented by choosing an exposure value for which the average luminance generates a pixel brightness equal to the Constant Calibration value.<br>
     * The Min/Max are expressed in pixel luminance (cd/m2) or in EV100 when using ExtendDefaultLuminanceRange (see project settings).<br>
     <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Exposure")
    @Meta(ClampMin = -10.0f,UIMax = 20.0f,Editcondition = "bOverride_AutoExposureMaxBrightness",DisplayName = "Max Brightness")
    public float AutoExposureMaxBrightness;

    /** >0 <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Exposure")
    @Meta(ClampMin = 0.02f,UIMax = 20.0f,Editcondition = "bOverride_AutoExposureSpeedUp",DisplayName = "Speed Up")
    public float AutoExposureSpeedUp;

    /** >0 <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Exposure")
    @Meta(ClampMin = 0.02f,UIMax = 20.0f,Editcondition = "bOverride_AutoExposureSpeedDown",DisplayName = "Speed Down")
    public float AutoExposureSpeedDown;

    /** Histogram Min value. Expressed in Log2(Luminance) or in EV100 when using ExtendDefaultLuminanceRange (see project settings) <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Exposure")
    @AdvancedDisplay
    @Meta(UIMin = -16,UIMax = 0.0f,Editcondition = "bOverride_HistogramLogMin")
    public float HistogramLogMin;

    /** Histogram Max value. Expressed in Log2(Luminance) or in EV100 when using ExtendDefaultLuminanceRange (see project settings) <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Exposure")
    @AdvancedDisplay
    @Meta(UIMin = 0.0f,UIMax = 16.0f,Editcondition = "bOverride_HistogramLogMax")
    public float HistogramLogMax;

    /** Calibration constant for 18% albedo. <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Exposure")
    @AdvancedDisplay
    @Meta(UIMin = 0,UIMax = 100.0f,Editcondition = "bOverride_AutoExposureCalibrationConstant",DisplayName = "Calibration Constant")
    public float AutoExposureCalibrationConstant;

    /** Brightness scale of the image cased lens flares (linear) <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Lens Flares")
    @Meta(UIMin = 0.0f,UIMax = 16.0f,Editcondition = "bOverride_LensFlareIntensity",DisplayName = "Intensity")
    public float LensFlareIntensity;

    /** Tint color for the image based lens flares. <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Lens Flares")
    @Meta(Editcondition = "bOverride_LensFlareTint",DisplayName = "Tint",Hints = HideAlphaChannel)
    public final Vector4f LensFlareTint = new Vector4f();

    /** Size of the Lens Blur (in percent of the view width) that is done with the Bokeh texture (note: performance cost is radius*radius) <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Lens Flares")
    @Meta(UIMin = 0.0f,UIMax = 32.0f,Editcondition = "bOverride_LensFlareBokehSize",DisplayName = "BokehSize")
    public float LensFlareBokehSize;

    /** Minimum brightness the lens flare starts having effect (this should be as high as possible to avoid the performance cost of blurring content that is too dark too see) <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Lens Flares")
    @Meta(UIMin = 0.1f,UIMax = 32.0f,Editcondition = "bOverride_LensFlareThreshold",DisplayName = "Threshold")
    public float LensFlareThreshold;

    /** Defines the shape of the Bokeh when the image base lens flares are blurred, cannot be blended <br>*/
    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Lens|Lens Flares")
    @Meta(Editcondition = "bOverride_LensFlareBokehShape",DisplayName = "BokehShape")
    public Texture2D LensFlareBokehShape;

    /** RGB defines the lens flare color, A it's position. This is a temporary solution. <br>*/
    @EditAnywhere
    @Category(value = "Lens|Lens Flares")
    @Meta(Editcondition = "bOverride_LensFlareTints",DisplayName = "Tints")
    public final Vector4f[] LensFlareTints = new Vector4f[8];

    /** 0..1 0=off/no vignette .. 1=strong vignette <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Image Effects")
    @Meta(UIMin = 0.0f,UIMax = 1.0f,Editcondition = "bOverride_VignetteIntensity")
    public float VignetteIntensity;

    /** 0..1 grain jitter <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Image Effects")
    @Meta(UIMin = 0.0f,UIMax = 1.0f,Editcondition = "bOverride_GrainJitter")
    public float GrainJitter;

    /** 0..1 grain intensity <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Image Effects")
    @Meta(UIMin = 0.0f,UIMax = 1.0f,Editcondition = "bOverride_GrainIntensity")
    public float GrainIntensity;

    /** 0..1 0=off/no ambient occlusion .. 1=strong ambient occlusion, defines how much it affects the non direct lighting after base pass <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Ambient Occlusion")
    @Meta(ClampMin = 0.0f,ClampMax = 1.0f,Editcondition = "bOverride_AmbientOcclusionIntensity",DisplayName = "Intensity")
    public float AmbientOcclusionIntensity;

    /** 0..1 0=no effect on static lighting .. 1=AO affects the stat lighting, 0 is free meaning no extra rendering pass <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Ambient Occlusion")
    @AdvancedDisplay
    @Meta(ClampMin = 0.0f,ClampMax = 1.0f,Editcondition = "bOverride_AmbientOcclusionStaticFraction",DisplayName = "Static Fraction")
    public float AmbientOcclusionStaticFraction;

    /** >0, in unreal units, bigger values means even distant surfaces affect the ambient occlusion <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Ambient Occlusion")
    @Meta(ClampMin = 0.1f,UIMax = 500.0f,Editcondition = "bOverride_AmbientOcclusionRadius",DisplayName = "Radius")
    public float AmbientOcclusionRadius;

    /** true: AO radius is in world space units, false: AO radius is locked the view space in 400 units <br>*/
    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Ambient Occlusion")
    @AdvancedDisplay
    @Meta(Editcondition = "bOverride_AmbientOcclusionRadiusInWS",DisplayName = "Radius in WorldSpace")
    public boolean AmbientOcclusionRadiusInW;

    /** >0, in unreal units, at what distance the AO effect disppears in the distance (avoding artifacts and AO effects on huge object) <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Ambient Occlusion")
    @AdvancedDisplay
    @Meta(ClampMin = 0.0f,UIMax = 20000.0f,Editcondition = "bOverride_AmbientOcclusionFadeDistance",DisplayName = "Fade Out Distance")
    public float AmbientOcclusionFadeDistance;

    /** >0, in unreal units, how many units before AmbientOcclusionFadeOutDistance it starts fading out <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Ambient Occlusion")
    @AdvancedDisplay
    @Meta(ClampMin = 0.0f,UIMax = 20000.0f,Editcondition = "bOverride_AmbientOcclusionFadeRadius",DisplayName = "Fade Out Radius")
    public float AmbientOcclusionFadeRadius;

    /** >0, in unreal units, how wide the ambient occlusion effect should affect the geometry (in depth), will be removed - only used for non normal method which is not exposed <br>*/
    public float AmbientOcclusionDistance_DEPRECATED;

    /** >0, in unreal units, bigger values means even distant surfaces affect the ambient occlusion <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Ambient Occlusion")
    @AdvancedDisplay
    @Meta(ClampMin = 0.1f,UIMax = 8.0f,Editcondition = "bOverride_AmbientOcclusionPower",DisplayName = "Power")
    public float AmbientOcclusionPower;

    /** >0, in unreal units, default (3.0) works well for flat surfaces but can reduce details <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Ambient Occlusion")
    @AdvancedDisplay
    @Meta(ClampMin = 0.0f,UIMax = 10.0f,Editcondition = "bOverride_AmbientOcclusionBias",DisplayName = "Bias")
    public float AmbientOcclusionBias;

    /** 0=lowest quality..100=maximum quality, only a few quality levels are implemented, no soft transition <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Ambient Occlusion")
    @AdvancedDisplay
    @Meta(ClampMin = 0.0f,UIMax = 100.0f,Editcondition = "bOverride_AmbientOcclusionQuality",DisplayName = "Quality")
    public float AmbientOcclusionQuality;

    /** Affects the blend over the multiple mips (lower resolution versions) , 0:fully use full resolution, 1::fully use low resolution, around 0.6 seems to be a good value <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Ambient Occlusion")
    @AdvancedDisplay
    @Meta(ClampMin = 0.1f,UIMax = 1.0f,Editcondition = "bOverride_AmbientOcclusionMipBlend",DisplayName = "Mip Blend")
    public float AmbientOcclusionMipBlend;

    /** Affects the radius AO radius scale over the multiple mips (lower resolution versions) <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Ambient Occlusion")
    @AdvancedDisplay
    @Meta(ClampMin = 0.5f,UIMax = 4.0f,Editcondition = "bOverride_AmbientOcclusionMipScale",DisplayName = "Mip Scale")
    public float AmbientOcclusionMipScale;

    /** to tweak the bilateral upsampling when using multiple mips (lower resolution versions) <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Ambient Occlusion")
    @AdvancedDisplay
    @Meta(ClampMin = 0.0f,UIMax = 0.1f,Editcondition = "bOverride_AmbientOcclusionMipThreshold",DisplayName = "Mip Threshold")
    public float AmbientOcclusionMipThreshold;

    /** Enables ray tracing ambient occlusion. <br>*/
    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Ray Tracing Ambient Occlusion")
    @Meta(Editcondition = "bOverride_RayTracingAO",DisplayName = "Enabled")
    public boolean RayTracingA;

    /** Sets the samples per pixel for ray tracing ambient occlusion. <br>*/
    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Ray Tracing Ambient Occlusion")
    @Meta(ClampMin = 1,ClampMax = 64,Editcondition = "bOverride_RayTracingAOSamplesPerPixel",DisplayName = "Samples Per Pixel")
    public int RayTracingAOSamplesPerPixel;

    /** Adjusts indirect lighting color. (1,1,1) is default. (0,0,0) to disable GI. The show flag 'Global Illumination' must be enabled to use this property. <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Global Illumination")
    @Meta(Editcondition = "bOverride_IndirectLightingColor",DisplayName = "Indirect Lighting Color",Hints = HideAlphaChannel)
    public final Vector4f IndirectLightingColor = new Vector4f();

    /** Scales the indirect lighting contribution. A value of 0 disables GI. Default is 1. The show flag 'Global Illumination' must be enabled to use this property. <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Global Illumination")
    @Meta(ClampMin = 0,UIMax = 4.0f,Editcondition = "bOverride_IndirectLightingIntensity",DisplayName = "Indirect Lighting Intensity")
    public float IndirectLightingIntensity;

    /** Enables ray tracing global illumination. <br>*/
    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Ray Tracing Global Illumination")
    @Meta(Editcondition = "bOverride_RayTracingGI",DisplayName = "Enabled")
    public boolean RayTracingG;

    /** Sets the ray tracing global illumination maximum bounces. <br>*/
    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Ray Tracing Global Illumination")
    @Meta(ClampMin = 0,ClampMax = 50,Editcondition = "bOverride_RayTracingGIMaxBounces",DisplayName = "Max. Bounces")
    public int RayTracingGIMaxBounces;

    /** Sets the samples per pixel for ray tracing global illumination. <br>*/
    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Ray Tracing Global Illumination")
    @Meta(ClampMin = 1,ClampMax = 64,Editcondition = "bOverride_RayTracingGISamplesPerPixel",DisplayName = "Samples Per Pixel")
    public int RayTracingGISamplesPerPixel;

    /** Color grading lookup table intensity. 0 = no intensity, 1=full intensity <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Color Grading|Misc")
    @Meta(ClampMin = 0,ClampMax = 1.0f,Editcondition = "bOverride_ColorGradingIntensity",DisplayName = "Color Grading LUT Intensity")
    public float ColorGradingIntensity;

    /** Look up table texture to use or none of not used<br>*/
    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Color Grading|Misc")
    @Meta(Editcondition = "bOverride_ColorGradingLUT",DisplayName = "Color Grading LUT")
    public Texture2D ColorGradingLUT;

    /** Width of the camera sensor to assume, in mm. <br>*/
    @BlueprintReadWrite
    @Category(value = "Lens|Depth of Field")
    @Meta(ForceUnits = "mm",ClampMin = 0.1f,UIMin = 0.1f,UIMax = 1000.0f,Editcondition = "bOverride_DepthOfFieldSensorWidth",DisplayName = "Sensor Width (mm)")
    public float DepthOfFieldSensorWidth;

    /** Distance in which the Depth of Field effect should be sharp, in unreal units (cm) <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Depth of Field")
    @Meta(ClampMin = 0.0f,UIMin = 1.0f,UIMax = 10000.0f,Editcondition = "bOverride_DepthOfFieldFocalDistance",DisplayName = "Focal Distance")
    public float DepthOfFieldFocalDistance;

    /** CircleDOF only: Depth blur km for 50% <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Depth of Field")
    @Meta(ClampMin = 1.0E-6f,ClampMax = 100.0f,Editcondition = "bOverride_DepthOfFieldDepthBlurAmount",DisplayName = "Depth Blur km for 50%")
    public float DepthOfFieldDepthBlurAmount;

    /** CircleDOF only: Depth blur radius in pixels at 1920x <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Depth of Field")
    @Meta(ClampMin = 0.0f,UIMax = 4.0f,Editcondition = "bOverride_DepthOfFieldDepthBlurRadius",DisplayName = "Depth Blur Radius")
    public float DepthOfFieldDepthBlurRadius;

    /** Artificial region where all content is in focus, starting after DepthOfFieldFocalDistance, in unreal units  (cm) <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Mobile Depth of Field")
    @Meta(UIMin = 0.0f,UIMax = 10000.0f,Editcondition = "bOverride_DepthOfFieldFocalRegion",DisplayName = "Focal Region")
    public float DepthOfFieldFocalRegion;

    /** To define the width of the transition region next to the focal region on the near side (cm) <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Mobile Depth of Field")
    @Meta(UIMin = 0.0f,UIMax = 10000.0f,Editcondition = "bOverride_DepthOfFieldNearTransitionRegion",DisplayName = "Near Transition Region")
    public float DepthOfFieldNearTransitionRegion;

    /** To define the width of the transition region next to the focal region on the near side (cm) <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Mobile Depth of Field")
    @Meta(UIMin = 0.0f,UIMax = 10000.0f,Editcondition = "bOverride_DepthOfFieldFarTransitionRegion",DisplayName = "Far Transition Region")
    public float DepthOfFieldFarTransitionRegion;

    /** SM5: BokehDOF only: To amplify the depth of field effect (like aperture)  0=off <br>
     ES2: Used to blend DoF. 0=off<br>
     <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Mobile Depth of Field")
    @Meta(ClampMin = 0.0f,ClampMax = 2.0f,Editcondition = "bOverride_DepthOfFieldScale",DisplayName = "Scale")
    public float DepthOfFieldScale;

    /** Gaussian only: Maximum size of the Depth of Field blur (in percent of the view width) (note: performance cost scales with size) <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Mobile Depth of Field")
    @Meta(UIMin = 0.0f,UIMax = 32.0f,Editcondition = "bOverride_DepthOfFieldNearBlurSize",DisplayName = "Near Blur Size")
    public float DepthOfFieldNearBlurSize;

    /** Gaussian only: Maximum size of the Depth of Field blur (in percent of the view width) (note: performance cost scales with size) <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Mobile Depth of Field")
    @Meta(UIMin = 0.0f,UIMax = 32.0f,Editcondition = "bOverride_DepthOfFieldFarBlurSize",DisplayName = "Far Blur Size")
    public float DepthOfFieldFarBlurSize;

    /** Occlusion tweak factor 1 (0.18 to get natural occlusion, 0.4 to solve layer color leaking issues) <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Mobile Depth of Field")
    @AdvancedDisplay
    @Meta(ClampMin = 0.0f,ClampMax = 1.0f,Editcondition = "bOverride_DepthOfFieldOcclusion",DisplayName = "Occlusion")
    public float DepthOfFieldOcclusion;

    /** Artificial distance to allow the skybox to be in focus (e.g. 200000), <=0 to switch the feature off, only for GaussianDOF, can cost performance <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Mobile Depth of Field")
    @AdvancedDisplay
    @Meta(ClampMin = 0.0f,ClampMax = 200000.0f,Editcondition = "bOverride_DepthOfFieldSkyFocusDistance",DisplayName = "Sky Distance")
    public float DepthOfFieldSkyFocusDistance;

    /** Artificial circular mask to (near) blur content outside the radius, only for GaussianDOF, diameter in percent of screen width, costs performance if the mask is used, keep Feather can Radius on default to keep it off <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Lens|Mobile Depth of Field")
    @AdvancedDisplay
    @Meta(UIMin = 0.0f,UIMax = 100.0f,Editcondition = "bOverride_DepthOfFieldVignetteSize",DisplayName = "Vignette Size")
    public float DepthOfFieldVignetteSize;

    /** Strength of motion blur, 0:off, should be renamed to intensity <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Motion Blur")
    @Meta(ClampMin = 0.0f,ClampMax = 1.0f,Editcondition = "bOverride_MotionBlurAmount",DisplayName = "Amount")
    public float MotionBlurAmount;

    /** max distortion caused by motion blur, in percent of the screen width, 0:off <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Motion Blur")
    @Meta(ClampMin = 0.0f,ClampMax = 100.0f,Editcondition = "bOverride_MotionBlurMax",DisplayName = "Max")
    public float MotionBlurMax;

    /**
     * Defines the target FPS for motion blur. Makes motion blur independent of actual frame rate and relative
     * to the specified target FPS instead. Higher target FPS results in shorter frames, which means shorter
     * shutter times and less motion blur. Lower FPS means more motion blur. A value of zero makes the motion
     * blur dependent on the actual frame rate.
     */
    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Motion Blur")
    @Meta(ClampMin = 0,ClampMax = 120,Editcondition = "bOverride_MotionBlurTargetFPS",DisplayName = "Target FPS")
    public int MotionBlurTargetFPS;

    /** The minimum projected screen radius for a primitive to be drawn in the velocity pass, percentage of screen width. smaller numbers cause more draw calls, default: 4% <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Motion Blur")
    @Meta(ClampMin = 0.0f,UIMax = 100.0f,Editcondition = "bOverride_MotionBlurPerObjectSize",DisplayName = "Per Object Size")
    public float MotionBlurPerObjectSize;

    /** How strong the dynamic GI from the LPV should be. 0.0 is off, 1.0 is the "normal" value, but higher values can be used to boost the effect<br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Light Propagation Volume")
    @Meta(Editcondition = "bOverride_LPVIntensity",UIMin = 0,UIMax = 20,DisplayName = "Intensity")
    public float LPVIntensity;

    /** Bias applied to light injected into the LPV in cell units. Increase to reduce bleeding through thin walls<br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Light Propagation Volume")
    @AdvancedDisplay
    @Meta(Editcondition = "bOverride_LPVVplInjectionBias",UIMin = 0,UIMax = 2,DisplayName = "Light Injection Bias")
    public float LPVVplInjectionBias;

    /** The size of the LPV volume, in Unreal units<br>*/
    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Light Propagation Volume")
    @Meta(Editcondition = "bOverride_LPVSize",UIMin = 100,UIMax = 20000,DisplayName = "Size")
    public float LPVSize;

    /** Secondary occlusion strength (bounce light shadows). Set to 0 to disable<br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Light Propagation Volume")
    @Meta(Editcondition = "bOverride_LPVSecondaryOcclusionIntensity",UIMin = 0,UIMax = 1,DisplayName = "Secondary Occlusion Intensity")
    public float LPVSecondaryOcclusionIntensity;

    /** Secondary bounce light strength (bounce light shadows). Set to 0 to disable<br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Light Propagation Volume")
    @AdvancedDisplay
    @Meta(Editcondition = "bOverride_LPVSecondaryBounceIntensity",UIMin = 0,UIMax = 1,DisplayName = "Secondary Bounce Intensity")
    public float LPVSecondaryBounceIntensity;

    /** Bias applied to the geometry volume in cell units. Increase to reduce darkening due to secondary occlusion <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Light Propagation Volume")
    @AdvancedDisplay
    @Meta(Editcondition = "bOverride_LPVGeometryVolumeBias",UIMin = 0,UIMax = 2,DisplayName = "Geometry Volume Bias")
    public float LPVGeometryVolumeBias;

    @Interp
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Light Propagation Volume")
    @AdvancedDisplay
    @Meta(Editcondition = "bOverride_LPVEmissiveInjectionIntensity",UIMin = 0,UIMax = 20,DisplayName = "Emissive Injection Intensity")
    public float LPVEmissiveInjectionIntensity;

    /** Controls the amount of directional occlusion. Requires LPV. Values very close to 1.0 are recommended <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Light Propagation Volume")
    @Meta(Editcondition = "bOverride_LPVDirectionalOcclusionIntensity",UIMin = 0,UIMax = 1,DisplayName = "Occlusion Intensity")
    public float LPVDirectionalOcclusionIntensity;

    /** Occlusion Radius - 16 is recommended for most scenes <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Light Propagation Volume")
    @AdvancedDisplay
    @Meta(Editcondition = "bOverride_LPVDirectionalOcclusionRadius",UIMin = 1,UIMax = 16,DisplayName = "Occlusion Radius")
    public float LPVDirectionalOcclusionRadius;

    /** Diffuse occlusion exponent - increase for more contrast. 1 to 2 is recommended <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Light Propagation Volume")
    @Meta(Editcondition = "bOverride_LPVDiffuseOcclusionExponent",UIMin = 0.5f,UIMax = 5,DisplayName = "Diffuse occlusion exponent")
    public float LPVDiffuseOcclusionExponent;

    /** Specular occlusion exponent - increase for more contrast. 6 to 9 is recommended <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Light Propagation Volume")
    @Meta(Editcondition = "bOverride_LPVSpecularOcclusionExponent",UIMin = 1,UIMax = 16,DisplayName = "Specular occlusion exponent")
    public float LPVSpecularOcclusionExponent;

    /** Diffuse occlusion intensity - higher values provide increased diffuse occlusion.<br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Light Propagation Volume")
    @AdvancedDisplay
    @Meta(Editcondition = "bOverride_LPVDiffuseOcclusionIntensity",UIMin = 0,UIMax = 4,DisplayName = "Diffuse occlusion intensity")
    public float LPVDiffuseOcclusionIntensity;

    /** Specular occlusion intensity - higher values provide increased specular occlusion.<br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Light Propagation Volume")
    @AdvancedDisplay
    @Meta(Editcondition = "bOverride_LPVSpecularOcclusionIntensity",UIMin = 0,UIMax = 4,DisplayName = "Specular occlusion intensity")
    public float LPVSpecularOcclusionIntensity;

    /** Sets the reflections type <br>*/
    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Reflections")
    @Meta(Editcondition = "bOverride_ReflectionsType",DisplayName = "Type")
    public EReflectionsType ReflectionsType;

    /** Enable/Fade/disable the Screen Space Reflection feature, in percent, avoid numbers between 0 and 1 fo consistency <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Screen Space Reflections")
    @Meta(ClampMin = 0.0f,ClampMax = 100.0f,Editcondition = "bOverride_ScreenSpaceReflectionIntensity",DisplayName = "Intensity")
    public float ScreenSpaceReflectionIntensity;

    /** 0=lowest quality..100=maximum quality, only a few quality levels are implemented, no soft transition, 50 is the default for better performance. <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Screen Space Reflections")
    @Meta(ClampMin = 0.0f,UIMax = 100.0f,Editcondition = "bOverride_ScreenSpaceReflectionQuality",DisplayName = "Quality")
    public float ScreenSpaceReflectionQuality;

    /** Until what roughness we fade the screen space reflections, 0.8 works well, smaller can run faster <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Screen Space Reflections")
    @Meta(ClampMin = 0.01f,ClampMax = 1.0f,Editcondition = "bOverride_ScreenSpaceReflectionMaxRoughness",DisplayName = "Max Roughness")
    public float ScreenSpaceReflectionMaxRoughness;

    /** Sets the maximum roughness until which ray tracing reflections will be visible (lower value is faster). Reflection contribution is smoothly faded when close to roughness threshold. This parameter behaves similarly to ScreenSpaceReflectionMaxRoughness. <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Ray Tracing Reflections")
    @Meta(ClampMin = 0.01f,ClampMax = 1.0f,Editcondition = "bOverride_RayTracingReflectionsMaxRoughness",DisplayName = "Max Roughness")
    public float RayTracingReflectionsMaxRoughness;

    /** Sets the maximum number of ray tracing reflection bounces. <br>*/
    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Ray Tracing Reflections")
    @Meta(ClampMin = 0,ClampMax = 50,Editcondition = "bOverride_RayTracingReflectionsMaxBounces",DisplayName = "Max. Bounces")
    public int RayTracingReflectionsMaxBounces;

    /** Sets the samples per pixel for ray traced reflections. <br>*/
    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Ray Tracing Reflections")
    @Meta(ClampMin = 1,ClampMax = 64,Editcondition = "bOverride_RayTracingReflectionsSamplesPerPixel",DisplayName = "Samples Per Pixel")
    public int RayTracingReflectionsSamplesPerPixel;

    /** Sets the reflected shadows type. <br>*/
    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Ray Tracing Reflections")
    @Meta(Editcondition = "bOverride_RayTracingReflectionsShadows",DisplayName = "Shadows")
    public EReflectedAndRefractedRayTracedShadows RayTracingReflectionsShadows;

    /** Sets the translucency type <br>*/
    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Translucency")
    @Meta(Editcondition = "bOverride_TranslucencyType",DisplayName = "Type")
    public ETranslucencyType TranslucencyType;

    /** Sets the maximum roughness until which ray tracing translucency will be visible (lower value is faster). Translucency contribution is smoothly faded when close to roughness threshold. This parameter behaves similarly to ScreenSpaceReflectionMaxRoughness. <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Ray Tracing Translucency")
    @Meta(ClampMin = 0.01f,ClampMax = 1.0f,Editcondition = "bOverride_RayTracingTranslucencyMaxRoughness",DisplayName = "Max Roughness")
    public float RayTracingTranslucencyMaxRoughness;

    /** Sets the maximum number of ray tracing refraction rays. <br>*/
    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Ray Tracing Translucency")
    @Meta(ClampMin = 0,ClampMax = 50,Editcondition = "bOverride_RayTracingTranslucencyRefractionRays",DisplayName = "Max. Refraction Rays")
    public int RayTracingTranslucencyRefractionRays;

    /** Sets the samples per pixel for ray traced translucency. <br>*/
    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Ray Tracing Translucency")
    @Meta(ClampMin = 1,ClampMax = 64,Editcondition = "bOverride_RayTracingTranslucencySamplesPerPixel",DisplayName = "Samples Per Pixel")
    public int RayTracingTranslucencySamplesPerPixel;

    /** Sets the translucency shadows type. <br>*/
    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Ray Tracing Translucency")
    @Meta(Editcondition = "bOverride_RayTracingTranslucencyShadows",DisplayName = "Shadows")
    public EReflectedAndRefractedRayTracedShadows RayTracingTranslucencyShadows;

    /** Sets whether refraction should be enabled or not (if not rays will not scatter and only travel in the same direction as before the intersection event). <br>*/
    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Ray Tracing Translucency")
    @Meta(Editcondition = "bOverride_RayTracingTranslucencyRefraction",DisplayName = "Refraction")
    public boolean RayTracingTranslucencyRefractio;

    /** Sets the path tracing maximum bounces <br>*/
    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Rendering Features|PathTracing")
    @Meta(ClampMin = 0,ClampMax = 50,Editcondition = "bOverride_PathTracingMaxBounces",DisplayName = "Max. Bounces")
    public int PathTracingMaxBounces;

    /** Sets the samples per pixel for the path tracer. <br>*/
    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Rendering Features|PathTracing")
    @Meta(ClampMin = 1,ClampMax = 64,Editcondition = "bOverride_PathTracingSamplesPerPixel",DisplayName = "Samples Per Pixel")
    public int PathTracingSamplesPerPixel;

    /** LPV Fade range - increase to fade more gradually towards the LPV edges.<br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Light Propagation Volume")
    @AdvancedDisplay
    @Meta(Editcondition = "bOverride_LPVFadeRange",UIMin = 0,UIMax = 9,DisplayName = "Fade range")
    public float LPVFadeRange;

    /** LPV Directional Occlusion Fade range - increase to fade more gradually towards the LPV edges.<br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Light Propagation Volume")
    @AdvancedDisplay
    @Meta(Editcondition = "bOverride_LPVDirectionalOcclusionFadeRange",UIMin = 0,UIMax = 9,DisplayName = "DO Fade range")
    public float LPVDirectionalOcclusionFadeRange;

    /**<br>
     * To render with lower or high resolution than it is presented,<br>
     * controlled by console variable,<br>
     * 100:off, needs to be <99 to get upsampling and lower to get performance,<br>
     * >100 for super sampling (slower but higher quality),<br>
     * only applied in game<br>
     <br>*/
    @Interp
    @BlueprintReadWrite
    @Category(value = "Rendering Features|Misc")
    @Meta(ClampMin = 0.0f,ClampMax = 400.0f,Editcondition = "bOverride_ScreenPercentage")
    public float ScreenPercentage;

    /*
     * Allows custom post process materials to be defined, using a MaterialInstance with the same Material as its parent to allow blending.<br>
     * For materials this needs to be the "PostProcess" domain type. This can be used for any UObject object implementing the IBlendableInterface (e.g. could be used to fade weather settings).<br>
     <br>
    @EditAnywhere
    @BlueprintReadWrite
    @Category(value = "Rendering Features")
    @Meta(Keywords = "PostProcess",DisplayName = "Post Process Materials")
    public FWeightedBlendables WeightedBlendables;*/

    /**
     * Used to define the values before any override happens.
     * Should be as neutral as possible.
     */
    public void SetBaseValues()
    {
//		*this = FPostProcessSettings();

        AmbientCubemapIntensity = 0.0f;
        ColorGradingIntensity = 0.0f;
    }


    /** Default number of blade of the diaphragm to simulate in depth of field. */
    public static final int kDefaultDepthOfFieldBladeCount = 5;
}
