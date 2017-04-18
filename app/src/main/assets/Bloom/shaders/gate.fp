#version 130

//#extension GL_EXT_shadow_samplers : require
//precision mediump float;

	varying vec2 v_TexCoord;
	varying vec3 v_Normal;
	varying vec3 v_WorldSpacePosition;	
	varying vec4 v_ShadowMapTexCoord;
			
	uniform vec3 u_LightVector;
	uniform sampler2D diffuse_tex;
	uniform sampler2D glow_tex;
	uniform float u_glowIntensity;
	
	uniform sampler2DShadow variance_shadowmap_tex;
	
    void main() 
    {
    	vec3 tex;
		float diffuse_factor;
		float specular_factor;
		vec3 color;
	    vec3 normal;
	    float distance;
	    float shadow_factor;   
		// fetching normal
		normal = normalize(v_Normal);

//		shadow_factor = (v_ShadowMapTexCoord.w > 0.0) 
//		    ? shadow2DEXT(variance_shadowmap_tex,v_ShadowMapTexCoord.xyz) : 1.0;
		
		shadow_factor = textureProj(variance_shadowmap_tex, v_ShadowMapTexCoord);
		tex = texture2D(diffuse_tex,v_TexCoord.xy).rgb;

		// calculating surface color
    	diffuse_factor = 0.1+shadow_factor*clamp(dot(u_LightVector,normal),0.0,1.0);
	    color.rgb = tex * diffuse_factor 
			+ u_glowIntensity * texture2D(glow_tex,v_TexCoord.xy).rgb;

		gl_FragColor = vec4(color.rgb, 1.0);
    }
