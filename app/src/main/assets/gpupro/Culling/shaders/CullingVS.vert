
out flat int occludeeID;

layout(binding = 0) readonly buffer InstanceBuffer
{
    mat4 instanceBuffer[];
};

uniform mat4 gViewProj;
uniform float gBoundingBoxScaling = 1.5;
void main()
{
    int vertexID = gl_VertexID;
    int instanceID = gl_InstanceID;

    occludeeID = gl_InstanceID;

    // generate unit cube position
    /*vec3 position = vec3(((vertexID & 0x4)==0)?-1.0 : 1.0,
                        ((vertexID & 0x2)==0)?-1.0 : 1.0,
                        ((vertexID & 0x1)==0)?-1.0 : 1.0);*/

    // Faces
    // +X, +Y, +Z, -X, -Y, -Z
    // Vertices
    // 0 ---15
    // |   / |
    // |  /  |
    // | /   |
    // 24--- 3

    int face_idx = vertexID / 6;
    int vtx_idx = vertexID % 6;
    vec3 P;
    P.x = ((vtx_idx % 3) == 2) ? -1.0 : 1.0;
    P.y = ((vtx_idx % 3) == 1) ? -1.0 : 1.0;
    P.z = 0;
    if ((face_idx % 3) == 0)
    P.yzx = P.xyz;
    else if ((face_idx % 3) == 1)
    P.xzy = P.xyz;
    // else if ((face_idx % 3) == 2)
    //    P.xyz = P.xyz;
    P *= ((vtx_idx / 3) == 0) ? 1.0 : -1.0;

    mat4 instanceMatrix = instanceBuffer[instanceID];
    vec4 poisitionWS = instanceMatrix * vec4(P * gBoundingBoxScaling, 1);
    gl_Position = gViewProj * poisitionWS;

    // When camera is inside the bounding box, it is possible that the bounding box is fully occluded even
    // when the object itself is visible. Therefore, bounding box vertices behind the near plane are clamped
    // in fornt of the near plane to avoid culling such objects.
    if(gl_Position.w < 0.0)
    {
        gl_Position = vec4(clamp(gl_Position.xy, vec2(-0.99), vec2(0.99)), 0,1);
    }
}