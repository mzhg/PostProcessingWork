
layout (points) in;
layout (triangle_strip, max_vertices = 4) out;

#define TEX2D_COORDINATES   3
#define TEX2D_INTERP_SOURCE 5

layout(binding = TEX2D_COORDINATES) uniform sampler2D g_tex2DCoordinates            /*: register( t1 )*/;
layout(binding = TEX2D_INTERP_SOURCE) uniform usampler2D  g_tex2DInterpolationSource    /*: register( t7 )*/;

in int uiVertexID[];

out SRenderSamplePositionsGS_Output
{
	vec3 f3Color;
	vec2 f2PosXY;
	vec4 f4QuadCenterAndSize;
}Out;

void main()
{
	ivec2 CoordTexDim;
//    g_tex2DCoordinates.GetDimensions(CoordTexDim.x, CoordTexDim.y);
	CoordTexDim = textureSize(g_tex2DCoordinates, 0);
    ivec2 TexelIJ = ivec2( uiVertexID[0]%CoordTexDim.x, uiVertexID[0]/CoordTexDim.x );
    vec2 f2QuadCenterPos = // g_tex2DCoordinates.Load(int3(TexelIJ,0));
    						 texelFetch(g_tex2DCoordinates, TexelIJ, 0).xy;

    uvec2 ui2InterpolationSources = // g_tex2DInterpolationSource.Load( uint3(TexelIJ,0) );
    						 texelFetch(g_tex2DInterpolationSource, TexelIJ, 0).xy;
    bool bIsInterpolation = ui2InterpolationSources.x != ui2InterpolationSources.y;

    vec2 f2QuadSize = (bIsInterpolation ? 1.f : 4.f) / SCREEN_RESLOUTION.xy;
    vec4 MinMaxUV = vec4(f2QuadCenterPos.x-f2QuadSize.x, f2QuadCenterPos.y - f2QuadSize.y, f2QuadCenterPos.x+f2QuadSize.x, f2QuadCenterPos.y + f2QuadSize.y);
    
    vec3 f3Color = bIsInterpolation ? vec3(0.5,0,0) : vec3(1,0,0);
    vec4 Verts[4] = vec4[4]
    (
        vec4(MinMaxUV.xy, 1.0, 1.0), 
        vec4(MinMaxUV.xw, 1.0, 1.0),
        vec4(MinMaxUV.zy, 1.0, 1.0),
        vec4(MinMaxUV.zw, 1.0, 1.0)
    );

    for(int i=0; i<4; i++)
    {
 //       SRenderSamplePositionsGS_Output Out;
 //       Out.f4PosPS = Verts[i];
 		gl_Position = Verts[i];
        Out.f2PosXY = gl_Position.xy;
        Out.f3Color = f3Color;
        Out.f4QuadCenterAndSize = vec4(f2QuadCenterPos, f2QuadSize);
//        triStream.Append( Out );
		EmitVertex();
    }
    
    EndPrimitive();
}