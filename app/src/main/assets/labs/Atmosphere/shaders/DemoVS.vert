#version 330
uniform mat4 model_from_view;
uniform mat4 view_from_clip;
out vec3 view_ray;
out vec2 vTex;

void main()
{
    int idx = gl_VertexID % 3;  // allows rendering multiple fullscreen triangles
    vec2 uv = vec2((idx << 1) & 2, idx & 2);
    vec4 vertex = vec4(uv.xy * 2.0 - 1.0, 0, 1);

    view_ray = (model_from_view * vec4((view_from_clip * vertex).xyz, 0.0)).xyz;
    gl_Position = vertex;
    vTex = uv;
}