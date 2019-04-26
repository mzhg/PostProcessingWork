layout(location = 0) out vec4 Out_Irradiance;

layout(binding = 0) uniform samplerCube g_EnvMap;

in vec3 m_Normal;

#define PI 3.1415926

vec3 tonemap(vec3 C)
{
	// Filmic -- model film properties
	C = max(vec3(0), C - 0.004);
	return (C*(6.2*C+0.5))/(C*(6.2*C+1.7)+0.06);
}

void main()
{
    ivec2 size = textureSize(g_EnvMap, 0);
    if(size.x == 0)
    {
        Out_Irradiance = vec4(0);
    }
    else
    {
        vec3 irradiance = vec3(0);
        float weights = 0.;
        const int samplerCount = min(size.x, 512);
        vec3 N = normalize(m_Normal);

        for(int w = 0; w < samplerCount; w ++)
        {
            float x = (float(w)) / float(samplerCount);
            float theta = PI * x; //  2.0 * acos(sqrt(1.0 - x));
            for(int h = 0; h < samplerCount; h++)
            {
                float y = (float(h)) / float(samplerCount);
                float phi = 2.0 * PI * y;

                // Convert the polar angle to direction.
                vec3 l;
                l.x = sin(theta) * cos(phi);
                l.y = sin(theta) * sin(phi);
                l.z = cos(theta);

                float NdotL = dot(N, l);
                if(NdotL > 0.0)
                {
                    irradiance += (textureLod(g_EnvMap, l, 0.0).rgb * NdotL);
                    weights += NdotL;
                }
            }
        }

        // TODO here I drop the PI because the irradiance scatter in all direction of the hemisphere centered the normal.
//        Out_Irradiance.rgb = irradiance * 4.0 / float(samplerCount * samplerCount);

        // The method from the source code: https://github.com/wwwtyro/regl-irradiance-envmap/blob/master/index.js
        Out_Irradiance.rgb = irradiance/weights;
        Out_Irradiance.a = 0.0;
    }
}