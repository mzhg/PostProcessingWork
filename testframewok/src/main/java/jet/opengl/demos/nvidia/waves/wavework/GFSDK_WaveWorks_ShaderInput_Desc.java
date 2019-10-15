package jet.opengl.demos.nvidia.waves.wavework;

/**
 * Created by Administrator on 2017/7/23 0023.
 */

public class GFSDK_WaveWorks_ShaderInput_Desc {
    public static final int
            VertexShader_FloatConstant = 0,
            VertexShader_ConstantBuffer=1,
            VertexShader_Texture=2,
            VertexShader_Sampler=3,
            HullShader_FloatConstant=4,
            HullShader_ConstantBuffer=5,
            HullShader_Texture=6,
            HullShader_Sampler=7,
            DomainShader_FloatConstant=8,
            DomainShader_ConstantBuffer=9,
            DomainShader_Texture=10,
            DomainShader_Sampler=11,
            PixelShader_FloatConstant=12,
            PixelShader_ConstantBuffer=13,
            PixelShader_Texture=14,
            PixelShader_Sampler=15,
            GL_VertexShader_UniformLocation=16,
            GL_TessEvalShader_UniformLocation=17,
            GL_FragmentShader_UniformLocation=18,
            GL_VertexShader_TextureBindLocation=19,
            GL_TessEvalShader_TextureBindLocation=20,
            GL_FragmentShader_TextureBindLocation=21,
            GL_VertexShader_TextureArrayBindLocation=22,
            GL_TessEvalShader_TextureArrayBindLocation=23,
            GL_FragmentShader_TextureArrayBindLocation=24,
            GL_AttribLocation=25;

    public int Type;
    public String Name;
    public int RegisterOffset;	// This will be the offset specified to the shader macro i.e. 'Regoff'

    public GFSDK_WaveWorks_ShaderInput_Desc(){}

    public GFSDK_WaveWorks_ShaderInput_Desc(int type, String name, int registerOffset) {
        Type = type;
        Name = name;
        RegisterOffset = registerOffset;
    }

    public void set(GFSDK_WaveWorks_ShaderInput_Desc other){
        Type = other.Type;
        Name = other.Name;
        RegisterOffset = other.RegisterOffset;
        Type = other.Type;
    }
}
