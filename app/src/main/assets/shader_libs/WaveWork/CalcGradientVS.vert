
#if GL_ES  // Mobile
    #if __VERSION__ >= 300
    layout(locaion = 0) in vec4 vInPos;
    layout(locaion = 1) in vec2 vInTexCoord;
    out vec2 vInterpTexCoord;
    #else
    attribute in vec4 vInPos;
    attribute in vec2 vInTexCoord;
    varying vec2 vInterpTexCoord;
    #endif
#else  // Desktop
    #if __VERSION__ >= 400
    layout(locaion = 0) in vec4 vInPos;
    layout(locaion = 1) in vec2 vInTexCoord;
    out vec2 vInterpTexCoord;
    #else __VERSION__ >= 130
    in vec4 vInPos;
    in vec2 vInTexCoord;
    out vec2 vInterpTexCoord;
    #else
    attribute in vec4 vInPos;
    attribute in vec2 vInTexCoord;
    varying vec2 vInterpTexCoord;
    #endif
#endif


void main()
{
    vInterpTexCoord = vInTexCoord;
    gl_Position = vInPos;
}