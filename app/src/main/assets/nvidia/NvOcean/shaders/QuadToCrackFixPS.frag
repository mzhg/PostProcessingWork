in vec4 m_f4UVAndScreenPos;

layout(binding = 0) uniform sampler2D g_texDiffuse;

bool lt_hull_prof(vec2 a, vec2 b)
{
    if(a.y < b.y) return true;
    else if(a.y > b.y) return false;
    else if(a.x < b.x) return true;
    else return false;
}

void swap(inout vec2 a, inout vec2 b)
{
    vec2 temp = a;
    a = b;
    b = temp;
}


out vec4 OutColor;
void main()
{
    vec2 samples[9];
    samples[0] = textureOffset(g_texDiffuse,m_f4UVAndScreenPos.xy,ivec2(-1,-1)).xy;  // g_samplerImageProcess
    samples[1] = textureOffset(g_texDiffuse,m_f4UVAndScreenPos.xy,ivec2( 0,-1)).xy;
    samples[2] = textureOffset(g_texDiffuse,m_f4UVAndScreenPos.xy,ivec2( 1,-1)).xy;
    samples[3] = textureOffset(g_texDiffuse,m_f4UVAndScreenPos.xy,ivec2(-1, 0)).xy;
    samples[4] = textureOffset(g_texDiffuse,m_f4UVAndScreenPos.xy,ivec2( 0, 0)).xy;
    samples[5] = textureOffset(g_texDiffuse,m_f4UVAndScreenPos.xy,ivec2( 1, 0)).xy;
    samples[6] = textureOffset(g_texDiffuse,m_f4UVAndScreenPos.xy,ivec2(-1, 1)).xy;
    samples[7] = textureOffset(g_texDiffuse,m_f4UVAndScreenPos.xy,ivec2( 0, 1)).xy;
    samples[8] = textureOffset(g_texDiffuse,m_f4UVAndScreenPos.xy,ivec2( 1, 1)).xy;

    float coverage_count = 0.f;
//    [unroll]
    for (int i = 0; i != 9; ++i) {
        coverage_count += samples[i].y;
    }

        #define TwoSort(a,b) { if(lt_hull_prof(b,a)) swap(a,b);  }
//    [unroll]
    for (int n = 8; n !=0; --n) {
//    [unroll]
        for (int i = 0; i < n; ++i) {
            TwoSort (samples[i], samples[i+1])
        }
    }

    OutColor= coverage_count > 4.5f ? samples[int(8.5f - 0.5f * coverage_count)].xyyy : samples[int(0.5f * coverage_count - 0.5f)].xyyy;
}