#version 100

attribute vec3 Position;
attribute vec3 PositionOther;
attribute vec4 OffsetUV;

/*
out block
{
	vec2 Texcoord;
} Out;
*/

varying vec2 Texcoord;

uniform mat4 MVP;
uniform float radius;
uniform float invScrRatio;
uniform sampler2D lineTexture;

//const float invScrRatio=1280.0/720.0;

void main()
{
	Texcoord = OffsetUV.zw;

	//compute vertices position in clip space
	vec4 vMVP     = MVP * vec4(Position,1.0);
	vec4 otherMVP = MVP * vec4(PositionOther,1.0);

	//  line direction in screen space (perspective division required)
	vec2 lineDirProj = radius * normalize(vMVP.xy/vMVP.ww - otherMVP.xy/otherMVP.ww);

	// small trick to avoid inversed line condition when points are not on the same side of Z plane
	if( sign(otherMVP.w) != sign(vMVP.w) )
		lineDirProj = -lineDirProj;

	// offset position in screen space along line direction and orthogonal direction
	vMVP.xy += lineDirProj.xy					* OffsetUV.xx * vec2(1.0,invScrRatio);
	vMVP.xy += lineDirProj.yx*vec2(1.0,-1.0)	* OffsetUV.yy * vec2(1.0,invScrRatio);

	gl_Position = vMVP;
}

