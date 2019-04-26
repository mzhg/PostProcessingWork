
vec4 packFloatToVec4 (float value )
{
    vec4 bitSh = vec4 ( 256.0*256.0*256.0, 256.0*256.0 , 256.0 , 1.0 );
    vec4 bitMask = vec4 ( 0.0 , 1.0/256.0 , 1.0/256.0 , 1.0/256.0);
    vec4 res = fract ( value * bitSh );
    res -= res.xxyz * bitMask;
    return res;
}

float unpackFloatFromVec4 ( vec4 value )
{
    const vec4 bitSh = vec4 ( 1.0/( 256.0*256.0*256.0 ), 1.0/( 256.0*256.0 ) , 1.0/256.0 , 1.);
    return dot ( value , bitSh ) ;
}