uniform mat4 gViewProj;
uniform vec3 HZBUvFactor;

in vec4 m_f4UVAndScreenPos;
layout(binding = 0) uniform sampler2D HZBTexture;
layout(binding = 1) uniform sampler2D BoundsCenterTexture;
layout(binding = 2) uniform sampler2D BoundsExtentTexture;

out float OutColor;

void main()
{
    vec2 InUV = m_f4UVAndScreenPos.xy;
    vec4 BoundsCenter = textureLod( BoundsCenterTexture, InUV, 0 );
    vec4 BoundsExtent = textureLod( BoundsExtentTexture, InUV, 0 ) * 1.5;
//    BoundsCenter.xyz += View.PreViewTranslation.xyz;

#if 1
    if( BoundsExtent.w == 0 )
    {
        OutColor = 1.0;
        return;
    }
#endif

    // Could frustum cull here

    vec3 BoundsMin = BoundsCenter.xyz - BoundsExtent.xyz;
    vec3 BoundsMax = BoundsCenter.xyz + BoundsExtent.xyz;
    vec3 Bounds[2] = { BoundsMin, BoundsMax };

    // Screen rect from bounds
    vec3 RectMin = vec3( 1000, 1000, 1000 );
    vec3 RectMax = vec3( -1000, -1000, -1000 );
    for( int i = 0; i < 8; i++ )
    {
        vec3 PointSrc;
        PointSrc.x = Bounds[ (i >> 0) & 1 ].x;
        PointSrc.y = Bounds[ (i >> 1) & 1 ].y;
        PointSrc.z = Bounds[ (i >> 2) & 1 ].z;

        vec4 PointClip = gViewProj * vec4( PointSrc, 1 );
        vec3 PointScreen = PointClip.xyz / PointClip.w;

        RectMin = min( RectMin, PointScreen );
        RectMax = max( RectMax, PointScreen );
    }

    // Camera culling if CPU doesn't do it.
    for(int i = 0; i < 3; i++)  // testing XYZ
    {
        if(RectMin[i] > 1 || RectMax[i] < -1)
        {
            OutColor = 0.0;
            return;
        }
    }

#if 0
    if( RectMax.z >= 1 )
    {
        // Crosses near plane
        OutColor = 1;
        return;
    }
#endif

    // FIXME assumes DX
//    float4 Rect = saturate( float4( RectMin.xy, RectMax.xy ) * float2( 0.5, -0.5 ).xyxy + 0.5 ).xwzy;
    vec4 Rect = clamp(vec4(RectMin.xy, RectMax.xy) * 0.5 + 0.5, vec4(0), vec4(1));
    ivec2 HZBSize = textureSize(HZBTexture, 0);
    vec4 RectPixels = Rect * HZBSize.xyxy;
    vec2 RectSize = ( RectPixels.zw - RectPixels.xy ) * 0.5;	// 0.5 for 4x4
    float Level = max(ceil( log2( max( RectSize.x, RectSize.y ) ) ), HZBUvFactor.z);

    // Check if we can drop one level lower
    float LevelLower = max( Level - 1, 0 );
    vec4 LowerRect = RectPixels * exp2( -LevelLower );
    vec2 LowerRectSize = ceil( LowerRect.zw ) - floor( LowerRect.xy );
    if((  LowerRectSize.x<= 4 ) && (LowerRectSize.y <=4) )
    {
        Level = LevelLower;
    }

    // 4x4 samples
    vec2 Scale = HZBUvFactor.xy * ( Rect.zw - Rect.xy ) / 3;
    vec2 Bias = HZBUvFactor.xy * Rect.xy;

    vec4 MaxDepth = vec4(-1000);
#if 1
    for( int i = 0; i < 4; i++ )
    {
        // TODO could vectorize this
        vec4 Depth;
        Depth.x = textureLod(HZBTexture, vec2( i, 0 ) * Scale + Bias, Level ).r;
        Depth.y = textureLod(HZBTexture, vec2( i, 1 ) * Scale + Bias, Level ).r;
        Depth.z = textureLod(HZBTexture, vec2( i, 2 ) * Scale + Bias, Level ).r;
        Depth.w = textureLod(HZBTexture, vec2( i, 3 ) * Scale + Bias, Level ).r;
        MaxDepth = max( MaxDepth, Depth );
    }
#else
    MaxDepth.x = textureLod(HZBTexture, Rect.xy, Level).x;
    MaxDepth.y = textureLod(HZBTexture, Rect.zy, Level).x;
    MaxDepth.z = textureLod(HZBTexture, Rect.xw, Level).x;
    MaxDepth.w = textureLod(HZBTexture, Rect.zw, Level).x;
#endif
    MaxDepth.x = max( max(MaxDepth.x, MaxDepth.y), max(MaxDepth.z, MaxDepth.w) );

    OutColor = (RectMin.z * 0.5 + 0.5) <= (MaxDepth.x + 0.0000) ? 1.0 : 0.0;
}