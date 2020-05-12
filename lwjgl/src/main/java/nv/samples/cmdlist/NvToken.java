package nv.samples.cmdlist;

import org.lwjgl.opengl.NVCommandList;

import java.lang.reflect.Field;

import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CommonUtil;
import jet.opengl.postprocessing.util.DebugTools;
import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.StackByte;

public final class NvToken {
    public static final int NVTOKEN_TYPES =  (GLenum.GL_FRONT_FACE_COMMAND_NV+1);

    public static boolean     s_nvcmdlist_bindless;
    public static final int[]   s_nvcmdlist_header = new int[NVTOKEN_TYPES];
    public static final int[]   s_nvcmdlist_headerSizes = new int[NVTOKEN_TYPES];
    public static final int[] s_nvcmdlist_stages = new int[NVTokenShaderStage.NVTOKEN_STAGES.ordinal()];

    private static void nvtokenRegisterSize(Class<?> clazz){
        Field id =  DebugTools.getField(clazz, "ID");
        try {
            int idValue = id.getInt(null);
            int sizeInBytes = CommonUtil.sizeof(clazz);

            s_nvcmdlist_headerSizes[idValue] = sizeInBytes;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public static void nvtokenInitInternals( boolean hwsupport, boolean bindlessSupport){
        assert( !hwsupport || (hwsupport && bindlessSupport) );

        nvtokenRegisterSize(NVTokenTerminate.class);
        nvtokenRegisterSize(NVTokenNop.class);
        nvtokenRegisterSize(NVTokenDrawElems.class);
        nvtokenRegisterSize(NVTokenDrawArrays.class);
        nvtokenRegisterSize(NVTokenDrawElemsStrip.class);
        nvtokenRegisterSize(NVTokenDrawArraysStrip.class);
        nvtokenRegisterSize(NVTokenDrawElemsInstanced.class);
        nvtokenRegisterSize(NVTokenDrawArraysInstanced.class);
        nvtokenRegisterSize(NVTokenVbo.class);
        nvtokenRegisterSize(NVTokenIbo.class);
        nvtokenRegisterSize(NVTokenUbo.class);
        nvtokenRegisterSize(NVTokenLineWidth.class);
        nvtokenRegisterSize(NVTokenPolygonOffset.class);
        nvtokenRegisterSize(NVTokenScissor.class);
        nvtokenRegisterSize(NVTokenBlendColor.class);
        nvtokenRegisterSize(NVTokenViewport.class);
        nvtokenRegisterSize(NVTokenAlphaRef.class);
        nvtokenRegisterSize(NVTokenStencilRef.class);
        nvtokenRegisterSize(NVTokenFrontFace.class);

        for (int i = 0; i < NVTOKEN_TYPES; i++){
            int sz = s_nvcmdlist_headerSizes[i];
            assert(sz > 0);
        }

        s_nvcmdlist_bindless  = bindlessSupport;

        if (hwsupport){
            for (int i = 0; i < NVTOKEN_TYPES; i++){
                s_nvcmdlist_header[i] = NVCommandList.glGetCommandHeaderNV(i,s_nvcmdlist_headerSizes[i]);
            }
            s_nvcmdlist_stages[NVTokenShaderStage.NVTOKEN_STAGE_VERTEX.ordinal()] = NVCommandList.glGetStageIndexNV(GLenum.GL_VERTEX_SHADER);
            s_nvcmdlist_stages[NVTokenShaderStage.NVTOKEN_STAGE_TESS_CONTROL.ordinal()] = NVCommandList.glGetStageIndexNV(GLenum.GL_TESS_CONTROL_SHADER);
            s_nvcmdlist_stages[NVTokenShaderStage.NVTOKEN_STAGE_TESS_EVALUATION.ordinal()] = NVCommandList.glGetStageIndexNV(GLenum.GL_TESS_EVALUATION_SHADER);
            s_nvcmdlist_stages[NVTokenShaderStage.NVTOKEN_STAGE_GEOMETRY.ordinal()] = NVCommandList.glGetStageIndexNV(GLenum.GL_GEOMETRY_SHADER);
            s_nvcmdlist_stages[NVTokenShaderStage.NVTOKEN_STAGE_FRAGMENT.ordinal()] = NVCommandList.glGetStageIndexNV(GLenum.GL_FRAGMENT_SHADER);
        }
        else{
            for (int i = 0; i < NVTOKEN_TYPES; i++){
                s_nvcmdlist_header[i] = nvtokenHeaderSW(i,s_nvcmdlist_headerSizes[i]);
            }
            for (int i = 0; i < NVTokenShaderStage.NVTOKEN_STAGES.ordinal(); i++){
                s_nvcmdlist_stages[i] = i;
            }
        }
    }

    public static String nvtokenCommandToString( int type ){
        switch  (type){
            case GLenum.GL_NOP_COMMAND_NV :  return "GL_NOP_COMMAND_NV";
            case GLenum.GL_DRAW_ELEMENTS_INSTANCED_COMMAND_NV : return "GL_DRAW_ELEMENTS_INSTANCED_COMMAND_NV";
            case GLenum.GL_DRAW_ARRAYS_INSTANCED_COMMAND_NV : return "GL_DRAW_ARRAYS_INSTANCED_COMMAND_NV";
            case GLenum.GL_ELEMENT_ADDRESS_COMMAND_NV:     return "GL_ELEMENT_ADDRESS_COMMAND_NV";
            case GLenum.GL_ATTRIBUTE_ADDRESS_COMMAND_NV:     return "GL_ATTRIBUTE_ADDRESS_COMMAND_NV";
            case GLenum.GL_UNIFORM_ADDRESS_COMMAND_NV:     return "GL_UNIFORM_ADDRESS_COMMAND_NV";
            case GLenum.GL_BLEND_COLOR_COMMAND_NV:     return "GL_BLEND_COLOR_COMMAND_NV";
            case GLenum.GL_STENCIL_REF_COMMAND_NV:     return "GL_STENCIL_REF_COMMAND_NV";
            case GLenum.GL_TERMINATE_SEQUENCE_COMMAND_NV:     return "GL_TERMINATE_SEQUENCE_COMMAND_NV";
            case GLenum.GL_LINE_WIDTH_COMMAND_NV:     return "GL_LINE_WIDTH_COMMAND_NV";
            case GLenum.GL_POLYGON_OFFSET_COMMAND_NV:     return "GL_POLYGON_OFFSET_COMMAND_NV";
            case GLenum.GL_ALPHA_REF_COMMAND_NV:     return "GL_ALPHA_REF_COMMAND_NV";
            case GLenum.GL_VIEWPORT_COMMAND_NV:     return "GL_VIEWPORT_COMMAND_NV";
            case GLenum.GL_DRAW_ARRAYS_STRIP_COMMAND_NV:     return "GL_DRAW_ARRAYS_STRIP_COMMAND_NV";
            case GLenum.GL_SCISSOR_COMMAND_NV:     return "GL_SCISSOR_COMMAND_NV";
            case GLenum.GL_DRAW_ELEMENTS_COMMAND_NV:     return "GL_DRAW_ELEMENTS_COMMAND_NV";
            case GLenum.GL_DRAW_ARRAYS_COMMAND_NV:     return "GL_DRAW_ARRAYS_COMMAND_NV";
            case GLenum.GL_DRAW_ELEMENTS_STRIP_COMMAND_NV:     return "GL_DRAW_ELEMENTS_STRIP_COMMAND_NV";
        }
        return null;

    }
    public static void        nvtokenGetStats(int[] stream, int streamSize, int stats[/*NVTOKEN_TYPES*/]){
        int  current =  0;
        final int streamEnd = current + streamSize;

        while (current < streamEnd){
            int             header  = stream[current];

            int type = nvtokenHeaderCommand(header);
            stats[type]++;

            current += s_nvcmdlist_headerSizes[type];
        }
    }

    public static void nvtokenDrawCommandsSW(int mode, StackByte stream, int streamSize,
                                             int[] offsets, int[] sizes,
                                             int count,
                                             StateSystem state){
        /*const char* NV_RESTRICT tokens = (const char*)stream;
        GLenum type = GL_UNSIGNED_SHORT;
        for (GLuint i = 0; i < count; i++)
        {
            size_t offset = offsets[i];
            size_t size   = sizes[i];

            assert(size + offset <= streamSize);

            type = nvtokenDrawCommandSequenceSW(&tokens[offset], size, mode, type, state);
        }*/
        throw new UnsupportedOperationException();
    }

    private static int nvtokenHeaderSW(int type, int size){
        return type | (size<<16);
    }

    private static int nvtokenHeaderCommandSW(int header)
    {
        return header & 0xFFFF;
    }

    private static int nvtokenHeaderSizeSW(int header)
    {
        return header>>16;
    }

    private static int nvtokenHeaderCommand(int header)
    {
        for (int i = 0; i < NVTOKEN_TYPES; i++){
            if (header == s_nvcmdlist_header[i]) return i;
        }

        throw new IllegalStateException("can't find header");
    }

    // Emulation related
    private static int nvtokenDrawCommandSequenceSW(byte[] stream, int streamSize, int mode, int type, StateSystem state )
    {
        int current = /*(GLubyte*)stream*/0;
        final int streamEnd = current + streamSize;

        int modeStrip;
        if      (mode == GLenum.GL_LINES)                modeStrip = GLenum.GL_LINE_STRIP;
        else if (mode == GLenum.GL_TRIANGLES)            modeStrip = GLenum.GL_TRIANGLE_STRIP;
            /* else if (mode == GL_QUADS)                modeStrip = GL_QUAD_STRIP; */
        else if (mode == GLenum.GL_LINES_ADJACENCY)      modeStrip = GLenum.GL_LINE_STRIP_ADJACENCY;
        else if (mode == GLenum.GL_TRIANGLES_ADJACENCY)  modeStrip = GLenum.GL_TRIANGLE_STRIP_ADJACENCY;
        else    modeStrip = mode;

        int modeSpecial;
        if      (mode == GLenum.GL_LINES)      modeSpecial = GLenum.GL_LINE_LOOP;
        else if (mode == GLenum.GL_TRIANGLES)  modeSpecial = GLenum.GL_TRIANGLE_FAN;
        else    modeSpecial = mode;

        while (current < streamEnd){
            final int             header  = Numeric.getInt(stream, current);

            int cmdtype = nvtokenHeaderCommand(header);
            // if you always use emulation on non-native tokens you can use
            // cmdtype = nvtokenHeaderCommandSW(header->encoded)
            switch(cmdtype){
                case GLenum.GL_TERMINATE_SEQUENCE_COMMAND_NV:
                {
                    return type;
                }
                case GLenum.GL_NOP_COMMAND_NV:
                {
                }
                break;
                /*case GLenum.GL_DRAW_ELEMENTS_COMMAND_NV:
                {
                    const DrawElementsCommandNV* cmd = (const DrawElementsCommandNV*)current;
                    glDrawElementsBaseVertex(mode, cmd->count, type, (const GLvoid*)(cmd->firstIndex * sizeof(GLuint)), cmd->baseVertex);
                }
                break;
                case GL_DRAW_ARRAYS_COMMAND_NV:
                {
                    const DrawArraysCommandNV* cmd = (const DrawArraysCommandNV*)current;
                    glDrawArrays(mode, cmd->first, cmd->count);
                }
                break;
                case GL_DRAW_ELEMENTS_STRIP_COMMAND_NV:
                {
          const DrawElementsCommandNV* cmd = (const DrawElementsCommandNV*)current;
                    glDrawElementsBaseVertex(modeStrip, cmd->count, type, (const GLvoid*)(cmd->firstIndex * sizeof(GLuint)), cmd->baseVertex);
                }
                break;
                case GL_DRAW_ARRAYS_STRIP_COMMAND_NV:
                {
          const DrawArraysCommandNV* cmd = (const DrawArraysCommandNV*)current;
                    glDrawArrays(modeStrip, cmd->first, cmd->count);
                }
                break;
                case GL_DRAW_ELEMENTS_INSTANCED_COMMAND_NV:
                {
          const DrawElementsInstancedCommandNV* cmd = (const DrawElementsInstancedCommandNV*)current;

                    assert (cmd->mode == mode || cmd->mode == modeStrip || cmd->mode == modeSpecial);

                    glDrawElementsIndirect(cmd->mode, type, &cmd->count);
                }
                break;
                case GL_DRAW_ARRAYS_INSTANCED_COMMAND_NV:
                {
          const DrawArraysInstancedCommandNV* cmd = (const DrawArraysInstancedCommandNV*)current;

                    assert (cmd->mode == mode || cmd->mode == modeStrip || cmd->mode == modeSpecial);

                    glDrawArraysIndirect(cmd->mode, &cmd->count);
                }
                break;
                case GL_ELEMENT_ADDRESS_COMMAND_NV:
                {
          const ElementAddressCommandNV* cmd = (const ElementAddressCommandNV*)current;
                    type = cmd->typeSizeInByte == 4 ? GL_UNSIGNED_INT : GL_UNSIGNED_SHORT;
                    if (s_nvcmdlist_bindless){
                        glBufferAddressRangeNV(GL_ELEMENT_ARRAY_ADDRESS_NV, 0, GLuint64(cmd->addressLo) | (GLuint64(cmd->addressHi)<<32), 0x7FFFFFFF);
                    }
                    else{
            const ElementAddressCommandEMU* cmd = (const ElementAddressCommandEMU*)current;
                        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, cmd->buffer);
                    }
                }
                break;
                case GL_ATTRIBUTE_ADDRESS_COMMAND_NV:
                {
                    if (s_nvcmdlist_bindless){
            const AttributeAddressCommandNV* cmd = (const AttributeAddressCommandNV*)current;
                        glBufferAddressRangeNV(GL_VERTEX_ATTRIB_ARRAY_ADDRESS_NV, cmd->index, GLuint64(cmd->addressLo) | (GLuint64(cmd->addressHi)<<32), 0x7FFFFFFF);
                    }
                    else{
            const AttributeAddressCommandEMU* cmd = (const AttributeAddressCommandEMU*)current;
                        glBindVertexBuffer(cmd->index, cmd->buffer, cmd->offset, state.vertexformat.bindings[cmd->index].stride);
                    }
                }
                break;
                case GL_UNIFORM_ADDRESS_COMMAND_NV:
                {
                    if (s_nvcmdlist_bindless){
            const UniformAddressCommandNV* cmd = (const UniformAddressCommandNV*)current;
                        glBufferAddressRangeNV(GL_UNIFORM_BUFFER_ADDRESS_NV, cmd->index, GLuint64(cmd->addressLo) | (GLuint64(cmd->addressHi)<<32), 0x10000);
                    }
                    else{
            const UniformAddressCommandEMU* cmd = (const UniformAddressCommandEMU*)current;
                        glBindBufferRange(GL_UNIFORM_BUFFER,cmd->index, cmd->buffer, cmd->offset256 * 256, cmd->size4*4);
                    }
                }
                break;
                case GL_BLEND_COLOR_COMMAND_NV:
                {
          const BlendColorCommandNV* cmd = (const BlendColorCommandNV*)current;
                    glBlendColor(cmd->red,cmd->green,cmd->blue,cmd->alpha);
                }
                break;
                case GL_STENCIL_REF_COMMAND_NV:
                {
          const StencilRefCommandNV* cmd = (const StencilRefCommandNV*)current;
                    glStencilFuncSeparate(GL_FRONT, state.stencil.funcs[StateSystem::FACE_FRONT].func, cmd->frontStencilRef, state.stencil.funcs[StateSystem::FACE_FRONT].mask);
                    glStencilFuncSeparate(GL_BACK,  state.stencil.funcs[StateSystem::FACE_BACK ].func, cmd->backStencilRef,  state.stencil.funcs[StateSystem::FACE_BACK ].mask);
                }
                break;

                case GL_LINE_WIDTH_COMMAND_NV:
                {
          const LineWidthCommandNV* cmd = (const LineWidthCommandNV*)current;
                    glLineWidth(cmd->lineWidth);
                }
                break;
                case GL_POLYGON_OFFSET_COMMAND_NV:
                {
          const PolygonOffsetCommandNV* cmd = (const PolygonOffsetCommandNV*)current;
                    glPolygonOffset(cmd->scale,cmd->bias);
                }
                break;
                case GL_ALPHA_REF_COMMAND_NV:
                {
          const AlphaRefCommandNV* cmd = (const AlphaRefCommandNV*)current;
                    *//* glAlphaFunc(state.alpha.mode, cmd->alphaRef); *//*
                }
                break;
                case GL_VIEWPORT_COMMAND_NV:
                {
          const ViewportCommandNV* cmd = (const ViewportCommandNV*)current;
                    glViewport(cmd->x, cmd->y, cmd->width, cmd->height);
                }
                break;
                case GL_SCISSOR_COMMAND_NV:
                {
          const ScissorCommandNV* cmd = (const ScissorCommandNV*)current;
                    glScissor(cmd->x,cmd->y,cmd->width,cmd->height);
                }
                break;
                case GL_FRONT_FACE_COMMAND_NV:
                {
                    FrontFaceCommandNV* cmd = (FrontFaceCommandNV*)current;
                    glFrontFace(cmd->frontFace?GL_CW:GL_CCW);
                }
                break;*/
            }


            int tokenSize = s_nvcmdlist_headerSizes[cmdtype];
            assert(tokenSize > 0);

            current += tokenSize;

        }
        return type;
    }
}
