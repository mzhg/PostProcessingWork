package jet.opengl.renderer.Unreal4.editor;

public enum EBloomMethod {
    /** Sum of Gaussian formulation */
    BM_SOG  /*UMETA(DisplayName = "Standard")*/,
    /** Fast Fourier Transform Image based convolution, intended for cinematics (too expensive for games)  */
    BM_FFT  /*UMETA(DisplayName = "Convolution")*/,
}
