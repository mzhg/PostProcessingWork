

		out vec2 m_Texcoord;
		out vec4 VtxGeoOutput1;
		
        void main()
        {
            vec2 TexCoords = vec2( (gl_VertexID << 1) & 2, gl_VertexID & 2 );
            gl_Position = vec4( TexCoords * vec2( 2.0, 2.0 ) + vec2( -1.0, -1.0) , 0.0, 1.0 );
            m_Texcoord = TexCoords;
            VtxGeoOutput1 = vec4(TexCoords, 0,0);
        }