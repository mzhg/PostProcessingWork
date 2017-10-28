layout(location = 0) out vec4 Out_f4Color;

in vec4 m_tex1;
in vec4 m_tex2;
in vec3 m_tex3;

layout(binding = 0) uniform sampler2DRect blur_input;

void main()
{
    Out_f4Color = ( 2.5*texture( blur_input , IN.tex1.rb )

    			+ texture( blur_input , m_tex3.gb )
    			+ texture( blur_input , m_tex3.rb )
    			+ texture( blur_input , m_tex1.ra )
    			+ texture( blur_input , m_tex1.rg )

    			+ texture( blur_input , m_tex2.bg)
    			+ texture( blur_input , m_tex2.rg)
    			+ texture( blur_input , m_tex2.ba)
    			+ texture( blur_input , m_tex2.ra) ) / 10.5;
}