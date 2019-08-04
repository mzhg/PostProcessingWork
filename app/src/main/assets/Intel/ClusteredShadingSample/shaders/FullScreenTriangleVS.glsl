/////////////////////////////////////////////////////////////////////////////////////////////
// Copyright 2017 Intel Corporation
//
// Licensed under the Apache License, Version 2.0 (the "License");
/////////////////////////////////////////////////////////////////////////////////////////////

in int gl_VertexID;

out gl_PerVertex
{
	vec4 gl_Position;
};

void main()
{
    // Parametrically work out vertex location for full screen triangle
    vec2 grid = vec2( float((gl_VertexID << 1) & 2), float(gl_VertexID & 2) );
    gl_Position = vec4(grid * vec2(2.0, -2.0) + vec2(-1.0, 1.0), 1.0, 1.0);
}