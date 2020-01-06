
layout(location=0) in vec4 inVertexPosition;
layout(location=1) in vec3 inNormal;

out vec3 vTexCoord;

uniform mat4 uModelViewMatrix;
uniform mat4 uNormalMatrix;

vec3 ShadeVertex()
{
    float diffuse = abs(normalize(uNormalMatrix * vec4(inNormal, 0.0)).z);
    return vec3(inVertexPosition.xy, diffuse);
}

void main(void)
{
     gl_Position = uModelViewMatrix * inVertexPosition;
	 vTexCoord = ShadeVertex();
}
