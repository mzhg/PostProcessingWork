uniform sampler2D _MainTex;
uniform float _Alpha;

in vec2 uv;

out vec4 Color;

void main()
{
    Color = texture(_MainTex, uv);

//    UNITY_APPLY_FOG(input.fogCoord, col);

    col.a *= _Alpha;
}