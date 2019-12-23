
out flat int occludeeID;

layout(binding = 0) buffer InstanceBuffer
{
    mat4 instanceBuffer[];
};

uniform mat4 gViewProj;

void main()
{
    int vertexID = gl_VertexID;
    int instanceID = gl_InstanceID;

    occludeeID = gl_InstanceID;

    // generate unit cube position
    vec3 position = vec3(((vertexID & 0x4)==0)?-1.0 : 1.0,
                        ((vertexID & 0x2)==0)?-1.0 : 1.0,
                        ((vertexID & 0x1)==0)?-1.0 : 1.0);
    mat4 instanceMatrix = instanceBuffer[instanceID];
    vec4 poisitionWS = instanceMatrix * vec4(position, 1);
    gl_Position = gViewProj * poisitionWS;

    // When camera is inside the bounding box, it is possible that the bounding box is fully occluded even
    // when the object itself is visible. Therefore, bounding box vertices behind the near plane are clamped
    // in fornt of the near plane to avoid culling such objects.
    if(gl_Position.w < 0.0)
    {
        gl_Position = vec4(clamp(gl_Position.xy, vec2(-0.99), vec2(0.99), 0,1));
    }
}