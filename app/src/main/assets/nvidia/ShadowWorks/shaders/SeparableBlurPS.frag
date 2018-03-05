out vec4 Out_Color;

in vec4 m_f4UVAndScreenPos;

float BoxFilterStart( float fWidth )  //Assumes filter is odd
{
    return ( ( fWidth - 1.0f ) / 2.0f );
}

uniform bool bVertical = false;

const float fStepSize = 1.0f;
const float fFilterWidth = 9.0;
layout(binding = 0) uniform sampler2D blurTexture;

void main()
{
    ivec2 texSize = textureSize(blurTexture, 0);
    float fTextureWidth = float(texSize.x);
    //PreShader - This should all be optimized away by the compiler
    //====================================
    float fStartOffset = BoxFilterStart( fFilterWidth );
    vec2 fTexelOffset = vec2( float(bVertical) * ( fStepSize / fTextureWidth ), float(!bVertical) * ( fStepSize / fTextureWidth ) );
    //====================================

    vec2 fTexStart = /*input.tex*/m_f4UVAndScreenPos.xy - ( fStartOffset * fTexelOffset );

    Out_Color = vec4(0);
    for( float i = 0.0; i < fFilterWidth; i+=1.0 )
        Out_Color += textureLod( blurTexture, vec2( fTexStart + fTexelOffset * i),0.);

    Out_Color =  Out_Color / fFilterWidth;
}