

layout(location = 0) out vec4 FragColor;

vec3 ParaboloidProject(vec3 P, float zNear, float zFar)
{
    vec3 outP;
    float lenP = length(P.xyz);
    outP.xyz = P.xyz/lenP;
    outP.x = outP.x / (outP.z + 1);
    outP.y = outP.y / (outP.z + 1);
    outP.z = (lenP - zNear) / (zFar - zNear);
    //	outP.z = 2 * outP.z - 1;
    return outP;
}

in vec3 m_Pos;

void main()
{
    FragColor = vec4(0);

    vec3 clipPos = ParaboloidProject(m_Pos, 0.5, 50.0);
    if(all(lessThanEqual(abs(clipPos.xy), vec2(1))))
    {
        gl_FragDepth = clipPos.z;
    }
    else
    {
        discard;
    }

}