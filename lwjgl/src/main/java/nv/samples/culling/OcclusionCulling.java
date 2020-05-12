package nv.samples.culling;

import com.nvidia.developer.opengl.app.NvSampleApp;

import org.lwjgl.opengl.ARBDebugOutput;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GL40;
import org.lwjgl.opengl.GL41;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GL45;
import org.lwjgl.opengl.NVCommandList;
import org.lwjgl.opengl.NVUniformBufferUnifiedMemory;
import org.lwjgl.opengl.NVVertexArrayRange;
import org.lwjgl.opengl.NVVertexArrayRange2;
import org.lwjgl.opengl.NVVertexBufferUnifiedMemory;
import org.lwjgl.opengles.GLES30;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.LogUtil;
import jet.opengl.postprocessing.util.StackByte;
import jet.opengl.postprocessing.util.StackInt;
import nv.samples.DynamicLod;
import nv.samples.cmdlist.NvToken;
import nv.samples.cmdlist.StateSystem;

public class OcclusionCulling extends NvSampleApp {
    private static final int VERTEX_POS          = 0;
    private static final int VERTEX_NORMAL       = 1;
    private static final int VERTEX_COLOR        = 2;
    private static final int VERTEX_MATRIXINDEX  = 3;

    private static final int UBO_SCENE = 0;

    private static final int SAMPLE_SIZE_WIDTH = (800);
    private static final int SAMPLE_SIZE_HEIGHT = (600);
    private static final int SAMPLE_MAJOR_VERSION = (4);
    private static final int SAMPLE_MINOR_VERSION = (5);

    private static final int     fboFormat = GLenum.GL_RGBA16F;
    private static final int        grid = 26;
    private static final int      globalscale = 8;

    static ScanSystem       s_scanSys = new ScanSystem();

    private static final int CYCLIC_FRAMES = 2;
    private static boolean CULL_TEMPORAL_NOFRUSTUM = true;

    private static final int
//    enum GuiEnums {
        GUI_ALGORITHM = 0,
        GUI_RESULT = 1,
        GUI_DRAW = 2
    ;

    private static final int
//    enum DrawModes {
        DRAW_STANDARD = 0,
        DRAW_MULTIDRAWINDIRECT = 1,
        DRAW_TOKENBUFFER_EMULATION = 2,
        DRAW_TOKENBUFFER = 3
    ;

    private static final int
//    enum ResultType {
        RESULT_REGULAR_CURRENT = 0,
        RESULT_REGULAR_LASTFRAME = 1,
        RESULT_TEMPORAL_CURRENT = 2;
//    };

    private GLFuncProvider gl;

    private final ProgramData programs = new ProgramData();
    private  int fbos_scene;

    private final ConstantBuffers buffers = new ConstantBuffers();
    private final Adresses addresses = new Adresses();
    private final Textures textures = new Textures();

    private final Tweak           m_tweak = new Tweak();
    private final Tweak           m_tweakLast = new Tweak();

//    ImGuiH::Registry            m_ui;
    private double                      m_uiTime;

    private final DynamicLod.SceneData m_sceneUbo = new DynamicLod.SceneData();

    private final StackInt m_sceneVisBits = new StackInt();
    private final List<DrawCmd> m_sceneCmds = new ArrayList<>();
    private final List<DrawCmd> m_sceneMatrices = new ArrayList<>();
    private final List<DrawCmd> m_sceneMatricesAnimated = new ArrayList<>();

    private int                    m_numTokens;
    private final StackByte        m_tokenStream = new StackByte();
    private final StackByte        m_tokenStreamCulled = new StackByte();


    private double          m_statsTime;
    private boolean         m_statsPrint;
    private boolean         m_cmdlistNative;
    private boolean         m_bindlessVboUbo;
    private int             m_cullFrameCycle;

    private CullingSystem                         m_cullSys;
    private JobReadbackPersistent  m_cullJobReadback;
    private JobIndirectUnordered   m_cullJobIndirect;
    private CullJobToken                          m_cullJobToken;
    private BufferValue[]                 m_cullReadbackBuffers = new BufferValue[CYCLIC_FRAMES];
    private ByteBuffer[]                  m_cullReadbackMappings = new ByteBuffer[CYCLIC_FRAMES];

    private boolean has_GL_ARB_bindless_texture;

    /*Sample() {  todo UI
        m_parameterList.add("method", (int32_t*)&m_tweak.method);
        m_parameterList.add("drawmode", (int32_t*)&m_tweak.drawmode);
        m_parameterList.add("result", (int32_t*)&m_tweak.result);
        m_parameterList.add("animate", &m_tweak.animate);
        m_parameterList.add("culling", &m_tweak.culling);
        m_parameterList.add("noui", &m_tweak.noui, true);
        m_parameterList.add("minpixelsize", &m_tweak.minPixelSize);
        m_parameterList.add("animateoffset", &m_tweak.animateOffset);
    }*/

    boolean begin(){
        m_statsPrint = false;

//        ImGuiH::Init(m_windowState.m_viewSize[0], m_windowState.m_viewSize[1], this);
//        ImGui::InitGL();

        gl.glPixelStorei(GLenum.GL_UNPACK_ALIGNMENT, 1);
        gl.glEnable(GLenum.GL_CULL_FACE);
        gl.glEnable(GLenum.GL_DEPTH_TEST);
        gl.glDepthFunc(GLenum.GL_LEQUAL);

        m_cmdlistNative   = gl.isSupportExt("GL_NV_command_list");
//        m_bindlessVboUbo  = has_GL_NV_vertex_buffer_unified_memory && has_GL_NV_uniform_buffer_unified_memory;
        m_bindlessVboUbo = gl.isSupportExt("GL_NV_vertex_buffer_unified_memory") && gl.isSupportExt("GL_NV_uniform_buffer_unified_memory");
        NvToken.nvtokenInitInternals(m_cmdlistNative, m_bindlessVboUbo);

        boolean validated = (true);

        int defaultVAO;
        defaultVAO = gl.glGenVertexArray();
        gl.glBindVertexArray(defaultVAO);

        validated = validated && initProgram();
        validated = validated && initScene();
        validated = validated && initFramebuffers(getGLContext().width(),getGLContext().height());

        if (!validated) return false;

        {
            ScanSystem.Programs scanprograms = new ScanSystem.Programs();
            getScanPrograms(scanprograms);
            s_scanSys.init(scanprograms);
        }

        {
            Programs cullprograms = new Programs();
            getCullPrograms(cullprograms);
            m_cullSys.init( cullprograms, false );

            m_cullFrameCycle = 0;

            for (int i = 0; i < CYCLIC_FRAMES; i++) {
                m_cullReadbackBuffers[i] = new BufferValue( buffers.cull_bitsReadback[i] );
                m_cullReadbackMappings[i] = gl.glMapNamedBufferRange( buffers.cull_bitsReadback[i], 0, m_cullReadbackBuffers[i].size, GLenum.GL_MAP_PERSISTENT_BIT | GLenum.GL_MAP_READ_BIT | GLenum.GL_MAP_COHERENT_BIT,m_cullReadbackMappings[i] );
            }

            initCullingJob( m_cullJobReadback );
            m_cullJobReadback.m_bufferVisBitsReadback = m_cullReadbackBuffers[0];
            m_cullJobReadback.m_bufferVisBitsMapping = m_cullReadbackMappings[0];
            m_cullJobReadback.m_fence = 0;


            initCullingJob(m_cullJobIndirect);
            m_cullJobIndirect.m_program_indirect_compact = m_progManager.get( programs.indirect_unordered );
            m_cullJobIndirect.m_bufferObjectIndirects = new BufferValue(buffers.scene_indirect);
            m_cullJobIndirect.m_bufferIndirectCounter = new BufferValue(buffers.cull_counter);
            m_cullJobIndirect.m_bufferIndirectResult  = new BufferValue(buffers.cull_indirect);

            initCullingJob(m_cullJobToken);
            m_cullJobToken.program_cmds   = m_progManager.get( programs.token_cmds );
            m_cullJobToken.program_sizes  = m_progManager.get( programs.token_sizes );
            m_cullJobToken.numTokens      = m_numTokens;

            // if we had multiple stateobjects, we would be using multiple sequences
            // where each sequence covers the token range per stateobject
            CullJobToken.Sequence sequence = new CullJobToken.Sequence();
            sequence.first = 0;
            sequence.num   = m_numTokens;
            sequence.offset = 0;
            sequence.endoffset = (int)(m_tokenStream.size()/4 /* sizeof(GLuint)*/);
            m_cullJobToken.sequences.add(sequence);


            m_cullJobToken.tokenOrig    = new ScanSystem.Buffer(buffers.scene_token);
            m_cullJobToken.tokenObjects = new ScanSystem.Buffer(buffers.scene_tokenObjects);
            m_cullJobToken.tokenOffsets = new ScanSystem.Buffer(buffers.scene_tokenOffsets);
            m_cullJobToken.tokenSizes   = new ScanSystem.Buffer(buffers.scene_tokenSizes);

            m_cullJobToken.tokenOut           = new ScanSystem.Buffer(buffers.cull_token);
            m_cullJobToken.tokenOutSizes      = new ScanSystem.Buffer(buffers.cull_tokenSizes);
            m_cullJobToken.tokenOutScan       = new ScanSystem.Buffer(buffers.cull_tokenScan);
            m_cullJobToken.tokenOutScanOffset = new ScanSystem.Buffer(buffers.cull_tokenScanOffsets);
        }

        /*{  TODO UI
            m_ui.enumAdd(GUI_ALGORITHM, CullingSystem::METHOD_FRUSTUM, "frustum");
            m_ui.enumAdd(GUI_ALGORITHM, CullingSystem::METHOD_HIZ, "hiz");
            m_ui.enumAdd(GUI_ALGORITHM, CullingSystem::METHOD_RASTER, "raster");

            m_ui.enumAdd(GUI_RESULT, RESULT_REGULAR_CURRENT, "regular current frame");
            m_ui.enumAdd(GUI_RESULT, RESULT_REGULAR_LASTFRAME, "regular last frame");
            m_ui.enumAdd(GUI_RESULT, RESULT_TEMPORAL_CURRENT, "temporal current frame");

            m_ui.enumAdd(GUI_DRAW, DRAW_STANDARD, "standard CPU");
            m_ui.enumAdd(GUI_DRAW, DRAW_MULTIDRAWINDIRECT, "MultiDrawIndirect GPU");
            m_ui.enumAdd(GUI_DRAW, DRAW_TOKENBUFFER_EMULATION, "nvcmdlist emulation");
            if (m_cmdlistNative) {
                m_ui.enumAdd(GUI_DRAW, DRAW_TOKENBUFFER, "nvcmdlist GPU");
            }
        }

        m_control.m_sceneOrbit = vec3(0.0f);
        m_control.m_sceneDimension = float(globalscale) * 2.0f;
        float dist = m_control.m_sceneDimension * 0.75f;
        m_control.m_viewMatrix = nvmath::look_at(m_control.m_sceneOrbit - normalize(vec3(1,0,-1))*dist, m_control.m_sceneOrbit, vec3(0,1,0));

        m_statsTime = NVPSystem::getTime();*/

        return validated;
    }

    void processUI(float time){
        /*int width = m_windowState.m_viewSize[0];  todo UI
        int height = m_windowState.m_viewSize[1];

        // Update imgui configuration
        auto &imgui_io = ImGui::GetIO();
        imgui_io.DeltaTime = static_cast<float>(time - m_uiTime);
        imgui_io.DisplaySize = ImVec2(width, height);

        m_uiTime = time;

        ImGui::NewFrame();
        ImGui::SetNextWindowSize(ImVec2(350, 0), ImGuiCond_FirstUseEver);
        if (ImGui::Begin("NVIDIA " PROJECT_NAME, nullptr)) {
            ImGui::Checkbox("culling", &m_tweak.culling);
            ImGui::Checkbox("freeze result", &m_tweak.freeze);
            ImGui::SliderFloat("min.pixelsize", &m_tweak.minPixelSize, 0.0f, 16.0f);
            m_ui.enumCombobox(GUI_ALGORITHM, "algorithm", &m_tweak.method);
            m_ui.enumCombobox(GUI_RESULT, "result", &m_tweak.result);
            m_ui.enumCombobox(GUI_DRAW, "drawmode", &m_tweak.drawmode);
            ImGui::SliderFloat("animate", &m_tweak.animate, 0.0f, 32.0f);
        }
        ImGui::End();*/
    }

    void think(float time){
//        NV_PROFILE_GL_SECTION("Frame");

        processUI(time);

        /*m_control.processActions(m_windowState.m_viewSize, todo camera
                nvmath::vec2f(m_windowState.m_mouseCurrent[0],m_windowState.m_mouseCurrent[1]),
        m_windowState.m_mouseButtonFlags, m_windowState.m_mouseWheel);*/

        /*if (m_windowState.onPress(KEY_R)){  todo reload programs
            m_progManager.reloadPrograms();

            ScanSystem::Programs scanprograms;
            getScanPrograms(scanprograms);
            s_scanSys.update(scanprograms);

            CullingSystem::Programs cullprograms;
            getCullPrograms(cullprograms);
            m_cullSys.update( cullprograms, false );
            m_cullJobIndirect.m_program_indirect_compact = m_progManager.get( programs.indirect_unordered );
            m_cullJobToken.program_cmds  = m_progManager.get( programs.token_cmds );
            m_cullJobToken.program_sizes = m_progManager.get( programs.token_sizes );
        }*/

        /*if (!m_progManager.areProgramsValid()){
            waitEvents();
            return;
        }*/

        if ( /*memcmp(&m_tweak,&m_tweakLast,sizeof(Tweak)) != 0*/ m_tweak.equals(m_tweakLast) && m_tweak.freeze == m_tweakLast.freeze) {
            systemChange();
            m_tweak.freeze = false;
        }
        if (!m_tweak.culling || m_tweak.result == RESULT_TEMPORAL_CURRENT){
            m_tweak.freeze = false;
        }

        boolean _DEBUG = true;

        if(_DEBUG){
            if (m_tweak.drawmode != m_tweakLast.drawmode && gl.isSupportExt("GL_NV_shader_buffer_load")){
                // disable a few expected but annoying debug messages for NVIDIA
                if (m_tweakLast.drawmode == DRAW_TOKENBUFFER_EMULATION || m_tweakLast.drawmode == DRAW_TOKENBUFFER){
                    ARBDebugOutput.glDebugMessageControlARB(GLenum.GL_DONT_CARE, GLenum.GL_DONT_CARE, GLenum.GL_DONT_CARE, 0, true);
                }
                if (m_tweak.drawmode == DRAW_TOKENBUFFER_EMULATION || m_tweak.drawmode == DRAW_TOKENBUFFER){
                    IntBuffer msgid = CacheBuffer.wrap(65537);// residency warning
                    ARBDebugOutput.glDebugMessageControlARB(GLenum.GL_DEBUG_SOURCE_API, GLenum.GL_DEBUG_TYPE_OTHER, GLenum.GL_DONT_CARE, msgid, false);
                    msgid = CacheBuffer.wrap(131186);// emulation does GetData on buffer
                    ARBDebugOutput.glDebugMessageControlARB(GLenum.GL_DEBUG_SOURCE_API, GLenum.GL_DEBUG_TYPE_PERFORMANCE_ARB, GLenum.GL_DONT_CARE, msgid, false);
                }
            }
        }

        if ( m_tweak.drawmode == DRAW_STANDARD && (getTotalTime()- m_statsTime) > 2.0){
            m_statsTime = getTotalTime();
            m_statsPrint = true;
        }
        else{
            m_statsPrint = false;
        }

        int width   = getGLContext().width();
        int height  = getGLContext().height();
        {
            gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, fbos_scene);
            gl.glViewport(0, 0, width, height);
            gl.glClearColor(1.0f,1.0f,1.0f,1.0f);
            gl.glClearDepthf(1.0f);
            gl.glClear(GLenum.GL_COLOR_BUFFER_BIT | GLenum.GL_DEPTH_BUFFER_BIT | GLenum.GL_STENCIL_BUFFER_BIT);
            gl.glEnable(GLenum.GL_DEPTH_TEST);


            { // Update UBO
                nvmath::mat4 projection = nvmath::perspective((45.f), float(width)/float(height), 0.1f, 100.0f);
                nvmath::mat4 view = m_control.m_viewMatrix;

                m_sceneUbo.viewProjMatrix = projection * view;
                m_sceneUbo.viewMatrix = view;
                m_sceneUbo.viewMatrixIT = nvmath::transpose(nvmath::invert(view));

                m_sceneUbo.viewPos = m_sceneUbo.viewMatrixIT.row(3);
                m_sceneUbo.viewDir = -view.row(2);

                gl.glBindBuffer(GL_UNIFORM_BUFFER, buffers.scene_ubo);
                gl.glBufferSubData(GL_UNIFORM_BUFFER, 0, sizeof(SceneData), &m_sceneUbo);
            }

        }

        if (m_tweak.animate != 0 || m_tweak.animateOffset != m_tweakLast.animateOffset)
        {
            mat4 rotator = nvmath::rotation_mat4_y( float(time)*0.1f * m_tweak.animate + m_tweak.animateOffset);

            for (int i = 0; i < m_sceneMatrices.size()/2; i++){
                mat4 changed = rotator * m_sceneMatrices[i*2 + 0];
                m_sceneMatricesAnimated[i*2 + 0] = changed;
                m_sceneMatricesAnimated[i*2 + 1] = nvmath::transpose(nvmath::invert(changed));
            }

            GL45.glNamedBufferSubData(buffers.scene_matrices,0,sizeof(mat4)*m_sceneMatricesAnimated.size(), m_sceneMatricesAnimated.data() );
        }


        if (m_tweak.culling && !m_tweak.freeze) {
            m_cullJobReadback.m_hostVisBits = m_sceneVisBits.data();

            // We change the output buffer for token emulation, as once the driver sees frequent readbacks on buffers
            // it moves the allocation to read-friendly memory. This would be bad for the native tokenbuffer.
            m_cullJobToken.tokenOut.buffer = (m_tweak.drawmode == DRAW_TOKENBUFFER_EMULATION ? buffers.cull_tokenEmulation : buffers.cull_token);
            Job  cullJob =
                    (m_tweak.drawmode == DRAW_STANDARD) ? m_cullJobReadback :
            (m_tweak.drawmode == DRAW_MULTIDRAWINDIRECT ? m_cullJobIndirect : m_cullJobToken);

            if (m_tweak.drawmode == DRAW_STANDARD) {
                if (m_tweak.result == RESULT_REGULAR_LASTFRAME) {
                    // When using persistent mapped bindings, we optimize our readback behavior.
                    // We perform the "server-side" result copy for the current frame,
                    // but read the client-side mapped results from the previous frame.
                    m_cullJobReadback.m_bufferVisBitsReadback = m_cullReadbackBuffers[m_cullFrameCycle];
                    m_cullJobReadback.m_bufferVisBitsMapping = m_cullReadbackMappings[m_cullFrameCycle ^ 1];
                }
                else {
                    m_cullJobReadback.m_bufferVisBitsReadback = m_cullReadbackBuffers[0];
                    m_cullJobReadback.m_bufferVisBitsMapping = m_cullReadbackMappings[0];
                }
            }

            switch(m_tweak.result)
            {
                case RESULT_REGULAR_CURRENT:
                    drawCullingRegular(cullJob);
                    break;
                case RESULT_REGULAR_LASTFRAME:
                    drawCullingRegularLastFrame(cullJob);
                    break;
                case RESULT_TEMPORAL_CURRENT:
                    drawCullingTemporal(cullJob);
                    break;
            }

            m_cullFrameCycle = m_cullFrameCycle ^ 1;
        }
        else{
            drawScene(false,"Draw");
        }

        if (m_statsPrint){
//            LOGI("\n");
        }


        // blit to background
        gl.glBindFramebuffer(GLenum.GL_READ_FRAMEBUFFER, fbos_scene);
        gl.glBindFramebuffer(GLenum.GL_DRAW_FRAMEBUFFER, 0);
        gl.glBlitFramebuffer(0,0,width,height,
                0,0,width,height,GLenum.GL_COLOR_BUFFER_BIT, GLenum.GL_NEAREST);

        m_tweakLast.set(m_tweak);

        /*if (!m_tweak.noui)
        {
            NV_PROFILE_GL_SECTION("GUI");
            ImGui::Render();
            ImGui::RenderDrawDataGL(ImGui::GetDrawData());
        }

        ImGui::EndFrame();*/
    }

    void resize(int width, int height) {
        initFramebuffers(width, height);
    }

    void initCullingJob(Job cullJob){
        cullJob.m_numObjects = (int)m_sceneCmds.size();

        cullJob.m_bufferMatrices        = new BufferValue(buffers.scene_matrices);
        cullJob.m_bufferObjectMatrix    = new BufferValue(buffers.scene_matrixindices);
        cullJob.m_bufferObjectBbox      = new BufferValue(buffers.scene_bboxes);

        cullJob.m_textureDepthWithMipmaps = textures.scene_depthstencil;

        cullJob.m_bufferVisOutput       = new BufferValue(buffers.cull_output);

        cullJob.m_bufferVisBitsCurrent  = new BufferValue(buffers.cull_bits);
        cullJob.m_bufferVisBitsLast     = new BufferValue(buffers.cull_bitsLast);
    }

    void drawScene(boolean depthonly, String what){
//        NV_PROFILE_GL_SECTION(what);

        if (depthonly){
            gl.glColorMask(false,false,false,false);
        }

        // need to set here, as culling also modifies vertex format state
        gl.glVertexAttribFormat(VERTEX_POS,    3, GLenum.GL_FLOAT, false,  /*offsetof(Vertex,position)*/0);
        gl.glVertexAttribFormat(VERTEX_NORMAL, 3, GLenum.GL_FLOAT, false,  /*offsetof(Vertex,normal)*/16);
        gl.glVertexAttribFormat(VERTEX_COLOR,  4, GLenum.GL_FLOAT, false,  /*offsetof(Vertex,color)*/32);
        gl.glVertexAttribBinding(VERTEX_POS,   0);
        gl.glVertexAttribBinding(VERTEX_NORMAL,0);
        gl.glVertexAttribBinding(VERTEX_COLOR, 0);

        GL43.glVertexAttribIFormat(VERTEX_MATRIXINDEX, 1, GLenum.GL_INT, 0);
        gl.glVertexAttribBinding(VERTEX_MATRIXINDEX, 1);
        GL43.glVertexBindingDivisor(1, 1);

        gl.glEnableVertexAttribArray(VERTEX_POS);
        gl.glEnableVertexAttribArray(VERTEX_NORMAL);
        gl.glEnableVertexAttribArray(VERTEX_COLOR);

        gl.glEnableVertexAttribArray(VERTEX_MATRIXINDEX);

        gl.glUseProgram(m_progManager.get(programs.draw_scene));

        // these bindings are replicated in the tokenbuffer as well
        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, UBO_SCENE, buffers.scene_ubo);
        gl.glBindVertexBuffer(0,buffers.scene_vbo,0,/*sizeof(Vertex)*/ 16 * 3);
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, buffers.scene_ibo);
        gl.glBindVertexBuffer(1,buffers.scene_matrixindices,0,/*sizeof(GLint)*/4);


        if (!has_GL_ARB_bindless_texture){
            gl.glActiveTexture(GLenum.GL_TEXTURE0 + TEX_MATRICES);
            gl.glBindTexture(GLenum.GL_TEXTURE_BUFFER,textures.scene_matrices);
        }

        if (m_tweak.drawmode == DRAW_MULTIDRAWINDIRECT){
            gl.glBindBuffer(GLenum.GL_DRAW_INDIRECT_BUFFER, m_tweak.culling ? buffers.cull_indirect : buffers.scene_indirect );
            if (m_tweak.culling){
                gl.glMemoryBarrier(GLenum.GL_COMMAND_BARRIER_BIT);
            }
            GL43.glMultiDrawElementsIndirect(GLenum.GL_TRIANGLES, GLenum.GL_UNSIGNED_INT, 0, m_sceneCmds.size(), 0);
            gl.glBindBuffer(GLenum.GL_DRAW_INDIRECT_BUFFER, 0);
        }
        else if (m_tweak.drawmode == DRAW_TOKENBUFFER || m_tweak.drawmode == DRAW_TOKENBUFFER_EMULATION)
        {
            if (m_bindlessVboUbo){
                GL11.glEnableClientState(NVUniformBufferUnifiedMemory.GL_UNIFORM_BUFFER_UNIFIED_NV);
                GL11.glEnableClientState(NVVertexBufferUnifiedMemory.GL_VERTEX_ATTRIB_ARRAY_UNIFIED_NV);
                GL11.glEnableClientState(NVVertexBufferUnifiedMemory.GL_ELEMENT_ARRAY_UNIFIED_NV);
            }
            if (m_tweak.culling){
                if (m_tweak.drawmode == DRAW_TOKENBUFFER_EMULATION){
//                    NV_PROFILE_GL_SECTION("Read");
                    ByteBuffer bytes = CacheBuffer.getCachedByteBuffer(m_cullJobToken.tokenOut.size);
                     m_cullJobToken.tokenOut.GetNamedBufferSubData(bytes);
//                    m_tokenStreamCulled[0];
                    bytes.get(m_tokenStreamCulled.getData());
                }
                else{
                    gl.glMemoryBarrier(GLenum.GL_COMMAND_BARRIER_BIT);
                }

//                NV_PROFILE_GL_SPLIT();
            }

            int offset = 0;
            int  size   = m_tokenStream.size();
            if (m_tweak.drawmode == DRAW_TOKENBUFFER_EMULATION){
                StateSystem state = new StateSystem();
                state.bindings[0] = /*sizeof(Vertex)*/ Vector4f.SIZE * 3;
                state.bindings[1] = /*sizeof(GLint)*/4;

                StackByte stream = m_tweak.culling ? m_tokenStreamCulled : m_tokenStream;

                NvToken.nvtokenDrawCommandsSW(GLenum.GL_TRIANGLES, stream.data(), stream.size(), &offset, &size, 1, state);
            }
            else{
                NVCommandList.glDrawCommandsNV(GLenum.GL_TRIANGLES, m_tweak.culling ? buffers.cull_token : buffers.scene_token, &offset, &size, 1);
            }

            if (m_bindlessVboUbo){
                GL11.glDisableClientState(NVUniformBufferUnifiedMemory.GL_UNIFORM_BUFFER_UNIFIED_NV);
                GL11.glDisableClientState(NVVertexBufferUnifiedMemory.GL_VERTEX_ATTRIB_ARRAY_UNIFIED_NV);
                GL11.glDisableClientState(NVVertexBufferUnifiedMemory.GL_ELEMENT_ARRAY_UNIFIED_NV);
            }

        }
        else{
            int visible = 0;
            for (int i = 0; i < m_sceneCmds.size(); i++)
            {
                if ((m_sceneVisBits.get(i / 32) & (1<< (i%32))) != 0 ){
//                    gl.glDrawElementsIndirect(GLenum.GL_TRIANGLES, GLenum.GL_UNSIGNED_INT, m_sceneCmds[i] );
                    GL40.glDrawElementsIndirect(GLenum.GL_TRIANGLES, GLenum.GL_UNSIGNED_INT, m_sceneCmds[i]);
                    visible++;
                }
            }
            if (m_statsPrint){
                LogUtil.i(LogUtil.LogType.DEFAULT, String.format("%s visible: %d pct\n", what, (visible * 100) / (int)m_sceneCmds.size()) );
            }
        }

        gl.glDisableVertexAttribArray(VERTEX_POS);
        gl.glDisableVertexAttribArray(VERTEX_NORMAL);
        gl.glDisableVertexAttribArray(VERTEX_COLOR);
        gl.glDisableVertexAttribArray(VERTEX_MATRIXINDEX);
        GL43.glVertexBindingDivisor(1, 0);

        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, UBO_SCENE, 0);
        gl.glBindVertexBuffer(0,0,0,0);
        gl.glBindVertexBuffer(1,0,0,0);
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);
        if (!has_GL_ARB_bindless_texture){
            gl.glActiveTexture(GLenum.GL_TEXTURE0 + TEX_MATRICES);
            gl.glBindTexture(GLenum.GL_TEXTURE_BUFFER,0);
        }

        if (depthonly){
            gl.glColorMask(true,true,true,true);
        }
    }

    void drawCullingRegular(Job cullJob){
        View view = new View();
        view.viewWidth         = getGLContext().width();
        view.viewHeight        = getGLContext().height();
        view.viewCullThreshold = m_tweak.minPixelSize;
        memcpy(view.viewPos, m_sceneUbo.viewPos.get_value(), sizeof(view.viewPos));
        memcpy(view.viewDir, m_sceneUbo.viewDir.get_value(), sizeof(view.viewDir));
        memcpy(view.viewProjMatrix, m_sceneUbo.viewProjMatrix.get_value(), sizeof(view.viewProjMatrix));

        switch(m_tweak.method){
            case METHOD_FRUSTUM:
            {
                {
//                    NV_PROFILE_GL_SECTION("CullF");
                    m_cullSys.buildOutput( m_tweak.method, cullJob, view );
                    m_cullSys.bitsFromOutput( cullJob, BitType.BITS_CURRENT );
                    m_cullSys.resultFromBits( cullJob );
                    m_cullSys.resultClient( cullJob );
                }

                drawScene(false,"Scene");
            }
            break;
            case METHOD_HIZ:
            {
                {
//                    NV_PROFILE_GL_SECTION("CullF");
                    m_cullSys.buildOutput( MethodType.METHOD_FRUSTUM, cullJob, view );
                    m_cullSys.bitsFromOutput( cullJob, BitType.BITS_CURRENT );
                    m_cullSys.resultFromBits( cullJob );
                    m_cullSys.resultClient( cullJob );
                }

                drawScene(true,"Depth");

                {
//                    NV_PROFILE_GL_SECTION("Mip");
                    // changes FBO binding
                    m_cullSys.buildDepthMipmaps( textures.scene_depthstencil, getGLContext().width(), getGLContext().height());
                }


                {
//                    NV_PROFILE_GL_SECTION("CullH");
                    m_cullSys.buildOutput( MethodType.METHOD_HIZ, cullJob, view );
                    m_cullSys.bitsFromOutput( cullJob, BitType.BITS_CURRENT );
                    m_cullSys.resultFromBits( cullJob );
                    m_cullSys.resultClient( cullJob );
                }

                gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, fbos_scene );
                drawScene(false,"Scene");
            }
            break;
            case METHOD_RASTER:
            {
                {
//                    NV_PROFILE_GL_SECTION("CullF");
                    m_cullSys.buildOutput( MethodType.METHOD_FRUSTUM, cullJob, view );
                    m_cullSys.bitsFromOutput( cullJob, BitType.BITS_CURRENT );
                    m_cullSys.resultFromBits( cullJob );
                    m_cullSys.resultClient( cullJob );
                }

                drawScene(true,"Depth");


                {
//                    NV_PROFILE_GL_SECTION("CullR");
                    m_cullSys.buildOutput( MethodType.METHOD_RASTER, cullJob, view );
                    m_cullSys.bitsFromOutput( cullJob, BitType.BITS_CURRENT );
                    m_cullSys.resultFromBits( cullJob );
                    m_cullSys.resultClient( cullJob );
                }

                drawScene(false,"Scene");
            }
            break;
        }
    }

    void drawCullingRegularLastFrame(Job cullJob){
        View view = new View();
        view.viewWidth         = getGLContext().width();
        view.viewHeight        = getGLContext().height();
        view.viewCullThreshold = m_tweak.minPixelSize;
        memcpy(view.viewPos, m_sceneUbo.viewPos.get_value(), sizeof(view.viewPos));
        memcpy(view.viewDir, m_sceneUbo.viewDir.get_value(), sizeof(view.viewDir));
        memcpy(view.viewProjMatrix, m_sceneUbo.viewProjMatrix.get_value(), sizeof(view.viewProjMatrix));

        switch(m_tweak.method){
            case METHOD_FRUSTUM:
            {
                {
//                    NV_PROFILE_GL_SECTION("Wait");
                    m_cullSys.resultClient(cullJob);
                }

                drawScene(false,"Scene");

                {
//                    NV_PROFILE_GL_SECTION("CullF");
                    m_cullSys.buildOutput( MethodType.METHOD_FRUSTUM, cullJob, view );
                    m_cullSys.bitsFromOutput( cullJob, BitType.BITS_CURRENT );
                    m_cullSys.resultFromBits( cullJob );
                }
            }
            break;
            case METHOD_HIZ:
            {
                {
//                    NV_PROFILE_GL_SECTION("Wait");
                    m_cullSys.resultClient(cullJob);
                }

                drawScene(false,"Scene");

                {
//                    NV_PROFILE_GL_SECTION("Mip");
                    // changes FBO binding
                    m_cullSys.buildDepthMipmaps( textures.scene_depthstencil, getGLContext().width(), getGLContext().height());
                }

                {
//                    NV_PROFILE_GL_SECTION("Cull");
                    m_cullSys.buildOutput( MethodType.METHOD_HIZ, cullJob, view );
                    m_cullSys.bitsFromOutput( cullJob, BitType.BITS_CURRENT );
                    m_cullSys.resultFromBits( cullJob );
                }
            }
            break;
            case METHOD_RASTER:
            {
                {
//                    NV_PROFILE_GL_SECTION("Wait");
                    m_cullSys.resultClient( cullJob );
                }

                drawScene(false,"Scene");

                {
//                    NV_PROFILE_GL_SECTION("Cull");
                    m_cullSys.buildOutput( MethodType.METHOD_RASTER, cullJob, view );
                    m_cullSys.bitsFromOutput( cullJob, BitType.BITS_CURRENT );
                    m_cullSys.resultFromBits( cullJob );
                }
            }
            break;
        }
    }

    void drawCullingTemporal(Job cullJob){
        View view = new View();
        view.viewWidth         = getGLContext().width();
        view.viewHeight        = getGLContext().height();
        view.viewCullThreshold = m_tweak.minPixelSize;
        memcpy(view.viewPos, m_sceneUbo.viewPos.get_value(), sizeof(view.viewPos));
        memcpy(view.viewDir, m_sceneUbo.viewDir.get_value(), sizeof(view.viewDir));
        memcpy(view.viewProjMatrix, m_sceneUbo.viewProjMatrix.get_value(), sizeof(view.viewProjMatrix));

        switch(m_tweak.method){
            case METHOD_FRUSTUM:
            {
                // kinda pointless to use temporal ;)
                {
//                    NV_PROFILE_GL_SECTION("CullF");
                    m_cullSys.buildOutput( m_tweak.method, cullJob, view );
                    m_cullSys.bitsFromOutput( cullJob, BitType.BITS_CURRENT );
                    m_cullSys.resultFromBits( cullJob );
                    m_cullSys.resultClient( cullJob );
                }

                drawScene(false,"Scene");
            }
            break;
            case METHOD_HIZ:
            {
                {
//                    NV_PROFILE_GL_SECTION("CullF");
                    if(!CULL_TEMPORAL_NOFRUSTUM) {
                        m_cullSys.buildOutput(MethodType.METHOD_FRUSTUM, cullJob, view);
                        m_cullSys.bitsFromOutput(cullJob, BitType.BITS_CURRENT_AND_LAST);
                        m_cullSys.resultFromBits(cullJob);
                    }
                    m_cullSys.resultClient( cullJob );
                }

                drawScene(false,"Last");

                // changes FBO binding
                m_cullSys.buildDepthMipmaps( textures.scene_depthstencil, getGLContext().width(), getGLContext().height());

                {
//                    NV_PROFILE_GL_SECTION("CullH");
                    m_cullSys.buildOutput( MethodType.METHOD_HIZ, cullJob, view );

                    m_cullSys.bitsFromOutput( cullJob, BitType.BITS_CURRENT_AND_NOT_LAST );
                    m_cullSys.resultFromBits( cullJob );
                    m_cullSys.resultClient( cullJob );

                    // for next frame
                    m_cullSys.bitsFromOutput( cullJob, BitType.BITS_CURRENT );
                    if(CULL_TEMPORAL_NOFRUSTUM) {
                        m_cullSys.resultFromBits(cullJob);
                    }

                    m_cullSys.swapBits( cullJob );  // last/output

                }

                gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, fbos_scene );
                drawScene(false,"New");
            }
            break;
            case METHOD_RASTER:
            {
                {
//                    NV_PROFILE_GL_SECTION("CullF");
                    if(!CULL_TEMPORAL_NOFRUSTUM) {
                        m_cullSys.buildOutput(MethodType.METHOD_FRUSTUM, cullJob, view);
                        m_cullSys.bitsFromOutput(cullJob, BitType.BITS_CURRENT_AND_LAST);
                        m_cullSys.resultFromBits(cullJob);
                    }
                    m_cullSys.resultClient( cullJob );
                }

                drawScene(false,"Last");

                {
//                    NV_PROFILE_GL_SECTION("CullR");
                    m_cullSys.buildOutput( MethodType.METHOD_RASTER, cullJob, view );
                    m_cullSys.bitsFromOutput( cullJob, BitType.BITS_CURRENT_AND_NOT_LAST );
                    m_cullSys.resultFromBits( cullJob );
                    m_cullSys.resultClient( cullJob );

                    // for next frame
                    m_cullSys.bitsFromOutput( cullJob, BitType.BITS_CURRENT );
                    if(CULL_TEMPORAL_NOFRUSTUM)
                        m_cullSys.resultFromBits( cullJob );
                    m_cullSys.swapBits( cullJob );  // last/output
                }

                drawScene(false,"New");
            }
            break;
        }
    }

    boolean initProgram(){
        boolean validated = true;
        m_progManager.m_filetype = nvh::ShaderFileManager::FILETYPE_GLSL;
        m_progManager.addDirectory( std::string("GLSL_" PROJECT_NAME));
        m_progManager.addDirectory( exePath() + std::string(PROJECT_RELDIRECTORY));
        //m_progManager.addDirectory( std::string(PROJECT_ABSDIRECTORY));

        m_progManager.registerInclude("common.h");
        m_progManager.registerInclude("noise.glsl");

        programs.draw_scene = m_progManager.createProgram(
                nvgl::ProgramManager::Definition(GL_VERTEX_SHADER,   "scene.vert.glsl"),
        nvgl::ProgramManager::Definition(GL_FRAGMENT_SHADER, "scene.frag.glsl"));

        programs.object_raster = m_progManager.createProgram(
                nvgl::ProgramManager::Definition(GL_VERTEX_SHADER,   "cull-raster.vert.glsl"),
        nvgl::ProgramManager::Definition(GL_GEOMETRY_SHADER, "cull-raster.geo.glsl"),
                nvgl::ProgramManager::Definition(GL_FRAGMENT_SHADER, "cull-raster.frag.glsl"));

        programs.object_frustum = m_progManager.createProgram(
                nvgl::ProgramManager::Definition(GL_VERTEX_SHADER,  "cull-basic.vert.glsl"));

        programs.object_hiz = m_progManager.createProgram(
                nvgl::ProgramManager::Definition(GL_VERTEX_SHADER,  "#define OCCLUSION\n", "cull-basic.vert.glsl"));

        programs.bit_regular = m_progManager.createProgram(
                nvgl::ProgramManager::Definition(GL_VERTEX_SHADER,  "#define TEMPORAL 0\n", "cull-bitpack.vert.glsl"));
        programs.bit_temporallast = m_progManager.createProgram(
                nvgl::ProgramManager::Definition(GL_VERTEX_SHADER,  "#define TEMPORAL TEMPORAL_LAST\n", "cull-bitpack.vert.glsl"));
        programs.bit_temporalnew = m_progManager.createProgram(
                nvgl::ProgramManager::Definition(GL_VERTEX_SHADER,  "#define TEMPORAL TEMPORAL_NEW\n", "cull-bitpack.vert.glsl"));

        programs.indirect_unordered = m_progManager.createProgram(
                nvgl::ProgramManager::Definition(GL_VERTEX_SHADER, "cull-indirectunordered.vert.glsl"));

        programs.depth_mips = m_progManager.createProgram(
                nvgl::ProgramManager::Definition(GL_VERTEX_SHADER,   "cull-downsample.vert.glsl"),
        nvgl::ProgramManager::Definition(GL_FRAGMENT_SHADER, "cull-downsample.frag.glsl"));

        programs.token_sizes = m_progManager.createProgram(
                nvgl::ProgramManager::Definition(GL_VERTEX_SHADER, "cull-tokensizes.vert.glsl"));
        programs.token_cmds = m_progManager.createProgram(
                nvgl::ProgramManager::Definition(GL_VERTEX_SHADER, "cull-tokencmds.vert.glsl"));

        programs.scan_prefixsum = m_progManager.createProgram(
                nvgl::ProgramManager::Definition(GL_COMPUTE_SHADER,  "#define TASK TASK_SUM\n", "scan.comp.glsl"));
        programs.scan_offsets = m_progManager.createProgram(
                nvgl::ProgramManager::Definition(GL_COMPUTE_SHADER,  "#define TASK TASK_OFFSETS\n", "scan.comp.glsl"));
        programs.scan_combine = m_progManager.createProgram(
                nvgl::ProgramManager::Definition(GL_COMPUTE_SHADER,  "#define TASK TASK_COMBINE\n", "scan.comp.glsl"));

        validated = m_progManager.areProgramsValid();

        return validated;
    }

    boolean initFramebuffers(int width, int height){
        nvgl::newTexture(textures.scene_color, GL_TEXTURE_2D);
        glBindTexture (GL_TEXTURE_2D, textures.scene_color);
        glTexStorage2D(GL_TEXTURE_2D, 1, fboFormat, width, height);

        int dim = width > height ? width : height;
        int levels = 0;
        while (dim > 0){
            levels++;
            dim/=2;
        }

        nvgl::newTexture(textures.scene_depthstencil, GL_TEXTURE_2D);
        glBindTexture (GLenum.GL_TEXTURE_2D, textures.scene_depthstencil);
        glTexStorage2D(GLenum.GL_TEXTURE_2D, levels, GL_DEPTH24_STENCIL8, width, height);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_NEAREST_MIPMAP_NEAREST);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_COMPARE_MODE, GL_NONE);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glBindTexture (GL_TEXTURE_2D, 0);

        nvgl::newFramebuffer(fbos.scene);
        glBindFramebuffer(GLenum.GL_FRAMEBUFFER,     fbos.scene);
        glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0,        GLenum.GL_TEXTURE_2D, textures.scene_color, 0);
        glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_DEPTH_STENCIL_ATTACHMENT, GLenum.GL_TEXTURE_2D, textures.scene_depthstencil, 0);
        glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);

        return true;
    }

    boolean initScene(){
        { // Scene UBO
            nvgl::newBuffer(buffers.scene_ubo);
            glNamedBufferData(buffers.scene_ubo, sizeof(SceneData) + sizeof(GLuint64), NULL, GL_DYNAMIC_DRAW);
        }

        { // Scene Geometry
            nvh::geometry::Mesh<Vertex>    sceneMesh;

            // we store all geometries in one big mesh, for sake of simplicity
            // and to allow standard MultiDrawIndirect to be efficient

            std::vector<Geometry>     geometries;
            for(int i = 0; i < 37; i++) {
        const int resmul = 2;
                mat4 identity;
                identity.identity();

                uint oldverts   = sceneMesh.getVerticesCount();
                uint oldindices = sceneMesh.getTriangleIndicesCount();

                switch(i % 2){
                    case 0:
                        nvh::geometry::Sphere<Vertex>::add(sceneMesh,identity,16*resmul,8*resmul);
                        break;
                    case 1:
                        nvh::geometry::Box<Vertex>::add(sceneMesh,identity,8*resmul,8*resmul,8*resmul);
                        break;
                }

                vec4 color(nvh::frand(),nvh::frand(),nvh::frand(),1.0f);
                for (uint v = oldverts; v < sceneMesh.getVerticesCount(); v++){
                    sceneMesh.m_vertices[v].color = color;
                }

                Geometry geom;
                geom.firstIndex    = oldindices;
                geom.count         = sceneMesh.getTriangleIndicesCount() - oldindices;

                geometries.push_back(geom);
            }

            nvgl::newBuffer(buffers.scene_ibo);
            glNamedBufferData(buffers.scene_ibo, sceneMesh.getTriangleIndicesSize(), sceneMesh.m_indicesTriangles.data(), GL_STATIC_DRAW);

            nvgl::newBuffer(buffers.scene_vbo);
            glNamedBufferData(buffers.scene_vbo, sceneMesh.getVerticesSize(), sceneMesh.m_vertices.data(), GL_STATIC_DRAW);


            // Scene Objects
            std::vector<CullBbox>     bboxes;
            std::vector<int>          matrixIndex;

            CullBbox  bbox;
            bbox.min = vec4(-1,-1,-1,1);
            bbox.max = vec4(1,1,1,1);

            int obj = 0;
            for (int i = 0; i < grid * grid * grid; i++){

                vec3  pos(i % grid, (i / grid) % grid, i / (grid * grid));

                pos -=  vec3( grid/2, grid/2, grid/2);
                pos += (vec3(nvh::frand(),nvh::frand(),nvh::frand())*2.0f ) - vec3(1.0f);
                pos /=  float(grid);

                float scale;
                if ( nvmath::length(pos) < 0.52f ){
                    scale = globalscale * 0.35f;
                    pos *=  globalscale * 0.5f;
                }
        else{
                    scale = globalscale;
                    pos *=  globalscale;
                }

                mat4 matrix =
                        nvmath::translation_mat4( pos) *
                        nvmath::rotation_mat4_y(nvh::frand()*nv_pi) *
                nvmath::scale_mat4( (vec3(scale) * (vec3(0.25f) + vec3(nvh::frand(),nvh::frand(),nvh::frand())*0.5f ))/float(grid) );

                m_sceneMatrices.push_back(matrix);
                m_sceneMatrices.push_back(nvmath::transpose(nvmath::invert(matrix)));
                matrixIndex.push_back(obj);

                // all have same bbox
                bboxes.push_back(bbox);

                DrawCmd cmd;
                cmd.count         = geometries[obj % geometries.size()].count;
                cmd.firstIndex    = geometries[obj % geometries.size()].firstIndex;
                cmd.baseVertex    = 0;
                cmd.baseInstance  = obj;
                cmd.instanceCount = 1;

                m_sceneCmds.push_back(cmd);
                obj++;
            }

            m_sceneMatricesAnimated.resize( m_sceneMatrices.size() );

            m_sceneVisBits.clear();
            m_sceneVisBits.resize( snapdiv(m_sceneCmds.size(),32), 0xFFFFFFFF );

            nvgl::newBuffer(buffers.scene_indirect);
            gl.glNamedBufferData(buffers.scene_indirect,sizeof(DrawCmd) * m_sceneCmds.size(), m_sceneCmds.data(), GL_STATIC_DRAW);

            nvgl::newBuffer(buffers.scene_matrices);
            gl.glNamedBufferData(buffers.scene_matrices, sizeof(mat4) * m_sceneMatrices.size(), m_sceneMatrices.data(), GL_STATIC_DRAW);
            nvgl::newTexture(textures.scene_matrices, GL_TEXTURE_BUFFER);
            gl.glTextureBuffer(textures.scene_matrices, GL_RGBA32F, buffers.scene_matrices);

            if (has_GL_ARB_bindless_texture){
                GLuint64 handle = glGetTextureHandleARB(textures.scene_matrices);
                gl.glMakeTextureHandleResidentARB(handle);
                gl.glNamedBufferSubData(buffers.scene_ubo, sizeof(SceneData), sizeof(GLuint64), &handle);
            }

            nvgl::newBuffer(buffers.scene_bboxes);
            gl.glNamedBufferData(buffers.scene_bboxes, sizeof(CullBbox) * bboxes.size(), bboxes.data(), GL_STATIC_DRAW);

            nvgl::newBuffer(buffers.scene_matrixindices);
            gl.glNamedBufferData(buffers.scene_matrixindices, sizeof(int) * matrixIndex.size(), matrixIndex.data(), GL_STATIC_DRAW);

            // for culling
            nvgl::newBuffer(buffers.cull_indirect);
            glNamedBufferData(buffers.cull_indirect, sizeof(DrawCmd) * m_sceneCmds.size(), NULL, GL_DYNAMIC_COPY);

            nvgl::newBuffer(buffers.cull_counter);
            glNamedBufferData(buffers.cull_counter, sizeof(int), NULL, GL_DYNAMIC_COPY);

            nvgl::newBuffer(buffers.cull_output);
            glNamedBufferData(buffers.cull_output, snapdiv( m_sceneCmds.size(), 32) * 32 * sizeof(uint32_t), NULL, GL_DYNAMIC_COPY);

            nvgl::newBuffer(buffers.cull_bits);
            glNamedBufferData(buffers.cull_bits, snapdiv( m_sceneCmds.size(), 32) * sizeof( uint32_t ), NULL, GL_DYNAMIC_COPY);

            nvgl::newBuffer(buffers.cull_bitsLast);
            glNamedBufferData(buffers.cull_bitsLast, snapdiv( m_sceneCmds.size(), 32) * sizeof( uint32_t ), NULL, GL_DYNAMIC_COPY);

            for (int i = 0; i < CYCLIC_FRAMES; i++) {
                nvgl::newBuffer( buffers.cull_bitsReadback[i] );
                glNamedBufferStorage( buffers.cull_bitsReadback[i], snapdiv( m_sceneCmds.size(), 32 ) * sizeof( uint32_t ), NULL, GL_MAP_PERSISTENT_BIT | GL_CLIENT_STORAGE_BIT | GL_MAP_READ_BIT | GL_MAP_COHERENT_BIT );
            }

            // for command list

            if (m_bindlessVboUbo)
            {
                glGetNamedBufferParameterui64vNV(buffers.scene_ubo, GL_BUFFER_GPU_ADDRESS_NV, &addresses.scene_ubo);
                glMakeNamedBufferResidentNV(buffers.scene_ubo, GL_READ_ONLY);

                glGetNamedBufferParameterui64vNV(buffers.scene_vbo, GL_BUFFER_GPU_ADDRESS_NV, &addresses.scene_vbo);
                glMakeNamedBufferResidentNV(buffers.scene_vbo, GL_READ_ONLY);

                glGetNamedBufferParameterui64vNV(buffers.scene_ibo, GL_BUFFER_GPU_ADDRESS_NV, &addresses.scene_ibo);
                glMakeNamedBufferResidentNV(buffers.scene_ibo, GL_READ_ONLY);

                glGetNamedBufferParameterui64vNV(buffers.scene_matrixindices, GL_BUFFER_GPU_ADDRESS_NV, &addresses.scene_matrixindices);
                glMakeNamedBufferResidentNV(buffers.scene_matrixindices, GL_READ_ONLY);
            }

            std::vector<int>          tokenObjects;
            std::vector<GLuint>       tokenSizes;
            std::vector<GLuint>       tokenOffsets;
            size_t offset;
            {
                // default setup for the scene
                NVTokenUbo  ubo;
                ubo.setBuffer(buffers.scene_ubo, addresses.scene_ubo, 0, sizeof(SceneData)+sizeof(GLuint64));
                ubo.setBinding(UBO_SCENE, NVTOKEN_STAGE_VERTEX);

                offset = nvtokenEnqueue(m_tokenStream, ubo);
                tokenObjects. push_back(-1);
                tokenSizes.   push_back(num32bit(sizeof(ubo)));
                tokenOffsets. push_back(num32bit(offset));

                ubo.setBinding(UBO_SCENE, NVTOKEN_STAGE_FRAGMENT);

                offset = nvtokenEnqueue(m_tokenStream, ubo);
                tokenObjects. push_back(-1);
                tokenSizes.   push_back(num32bit(sizeof(ubo)));
                tokenOffsets. push_back(num32bit(offset));

                NVTokenVbo vbo;
                vbo.setBinding(0);
                vbo.setBuffer(buffers.scene_vbo, addresses.scene_vbo, 0);

                offset = nvtokenEnqueue(m_tokenStream, vbo);
                tokenObjects. push_back(-1);
                tokenSizes.   push_back(num32bit(sizeof(vbo)));
                tokenOffsets. push_back(num32bit(offset));

                vbo.setBinding(1);
                vbo.setBuffer(buffers.scene_matrixindices, addresses.scene_matrixindices, 0);

                offset = nvtokenEnqueue(m_tokenStream, vbo);
                tokenObjects. push_back(-1);
                tokenSizes.   push_back(num32bit(sizeof(vbo)));
                tokenOffsets. push_back(num32bit(offset));

                NVTokenIbo ibo;
                ibo.setBuffer(buffers.scene_ibo, addresses.scene_ibo);
                ibo.setType(GL_UNSIGNED_INT);

                offset = nvtokenEnqueue(m_tokenStream, ibo);
                tokenObjects. push_back(-1);
                tokenSizes.   push_back(num32bit(sizeof(ibo)));
                tokenOffsets. push_back(num32bit(offset));
            }

            for (size_t i = 0; i < m_sceneCmds.size(); i++){
        const DrawCmd& cmd = m_sceneCmds[i];

                // for commandlist token technique
                NVTokenDrawElemsInstanced drawtoken;
                drawtoken.cmd.baseInstance  = cmd.baseInstance;
                drawtoken.cmd.baseVertex    = cmd.baseVertex;
                drawtoken.cmd.firstIndex    = cmd.firstIndex;
                drawtoken.cmd.instanceCount = cmd.instanceCount;
                drawtoken.cmd.count         = cmd.count;
                drawtoken.cmd.mode          = GL_TRIANGLES;
                offset = nvtokenEnqueue(m_tokenStream,drawtoken);

                // In this simple case we have one token per "object",
                // but typically one would have multiple tokens (vbo,ibo...) per object
                // as well, hence the token culling code presented, accounts for the
                // more generic use-case.
                tokenObjects. push_back(int(i));
                tokenSizes.   push_back(num32bit(sizeof(drawtoken)));
                tokenOffsets. push_back(num32bit(offset));
            }
            m_numTokens = GLuint(tokenSizes.size());
            // pad to multiple of 4
            while(tokenSizes.size() % 4)
            {
                tokenObjects.push_back(-1);
                tokenSizes.push_back(0);
                tokenOffsets.push_back(0);
            }

            m_tokenStreamCulled = m_tokenStream;

            nvgl::newBuffer(buffers.scene_token);
            glNamedBufferData(buffers.scene_token, m_tokenStream.size(), m_tokenStream.data(), GL_STATIC_DRAW);

            // for command list culling

            nvgl::newBuffer(buffers.scene_tokenSizes);
            glNamedBufferData(buffers.scene_tokenSizes, tokenSizes.size() * sizeof(GLuint), tokenSizes.data(), GL_STATIC_DRAW);

            nvgl::newBuffer(buffers.scene_tokenOffsets);
            glNamedBufferData(buffers.scene_tokenOffsets, tokenOffsets.size() * sizeof(GLuint), tokenOffsets.data(), GL_STATIC_DRAW);

            nvgl::newBuffer(buffers.scene_tokenObjects);
            glNamedBufferData(buffers.scene_tokenObjects, tokenObjects.size() * sizeof(GLint), tokenObjects.data(), GL_STATIC_DRAW);

            nvgl::newBuffer(buffers.cull_token);
            glNamedBufferData(buffers.cull_token, m_tokenStream.size(), NULL, GL_DYNAMIC_COPY);

            nvgl::newBuffer(buffers.cull_tokenEmulation); // only for emulation
            glNamedBufferData(buffers.cull_tokenEmulation, m_tokenStream.size(), NULL, GL_DYNAMIC_READ);

            nvgl::newBuffer(buffers.cull_tokenSizes);
            glNamedBufferData(buffers.cull_tokenSizes, tokenSizes.size() * sizeof(GLuint), NULL, GL_DYNAMIC_COPY);

            nvgl::newBuffer(buffers.cull_tokenScan);
            glNamedBufferData(buffers.cull_tokenScan, tokenSizes.size() * sizeof(GLuint), NULL, GL_DYNAMIC_COPY);

            nvgl::newBuffer(buffers.cull_tokenScanOffsets);
            glNamedBufferData(buffers.cull_tokenScanOffsets, ScanSystem::getOffsetSize(GLuint(tokenSizes.size())), NULL, GL_DYNAMIC_COPY);
        }

        return true;
    }

    void getCullPrograms( Programs cullprograms ){
        cullprograms.bit_regular      = m_progManager.get( programs.bit_regular );
        cullprograms.bit_temporallast = m_progManager.get( programs.bit_temporallast );
        cullprograms.bit_temporalnew  = m_progManager.get( programs.bit_temporalnew );
        cullprograms.depth_mips       = m_progManager.get( programs.depth_mips );
        cullprograms.object_frustum   = m_progManager.get( programs.object_frustum );
        cullprograms.object_hiz       = m_progManager.get( programs.object_hiz );
        cullprograms.object_raster    = m_progManager.get( programs.object_raster );
    }

    void getScanPrograms( ScanSystem.Programs scanprograms ){
        scanprograms.prefixsum  = m_progManager.get( programs.scan_prefixsum );
        scanprograms.offsets    = m_progManager.get( programs.scan_offsets );
        scanprograms.combine    = m_progManager.get( programs.scan_combine );
    }

    void systemChange(){
        // clear last visibles to 0
        gl.glBindBuffer(GLenum.GL_COPY_WRITE_BUFFER, buffers.cull_bitsLast);
        gl.glClearBufferData(GLenum.GL_COPY_WRITE_BUFFER, GLenum.GL_R32UI, GLenum.GL_RED_INTEGER, GLenum.GL_UNSIGNED_INT, null);
        // current are all visible
//        memset(m_sceneVisBits.data(),0xFFFFFFFF,sizeof(uint32_t) * m_sceneVisBits.size() );
        m_sceneVisBits.fill(0, m_sceneVisBits.size(), 0xFFFFFFFF);
        // rest token buffer
        gl.glCopyNamedBufferSubData(buffers.scene_token, buffers.cull_token, 0, 0, m_tokenStream.size());
        gl.glCopyNamedBufferSubData(buffers.scene_token, buffers.cull_tokenEmulation, 0, 0, m_tokenStream.size());
        // reset indirect buffer
        gl.glCopyNamedBufferSubData(buffers.scene_indirect, buffers.cull_indirect, 0, 0, m_sceneCmds.size() * DrawCmd.SIZE);
    }

    private static int num32bit(int input)
    {
        return input/4;
    }

    private static final class ProgramData{
        GLSLProgram
                draw_scene,

        object_frustum,
                object_hiz,
                object_raster,
                bit_temporallast,
                bit_temporalnew,
                bit_regular,
                indirect_unordered,
                depth_mips,

        token_sizes,
                token_cmds,

        scan_prefixsum,
                scan_offsets,
                scan_combine;
    }

    private static final class ConstantBuffers {
        int scene_ubo = 0;
        int scene_vbo = 0;
        int scene_ibo = 0;
        int scene_matrices = 0;
        int scene_bboxes = 0;
        int scene_matrixindices = 0;
        int scene_indirect = 0;

        int scene_token = 0;
        int scene_tokenSizes = 0;
        int scene_tokenOffsets = 0;
        int scene_tokenObjects = 0;

        int cull_output = 0;
        int cull_bits = 0;
        int cull_bitsLast = 0;
        final int[] cull_bitsReadback = new int[CYCLIC_FRAMES];
        int cull_indirect = 0;
        int cull_counter = 0;

        int cull_token = 0;
        int cull_tokenEmulation = 0;
        int cull_tokenSizes = 0;
        int cull_tokenScan = 0;
        int cull_tokenScanOffsets = 0;
    }

    private static final class Adresses {
        long
                scene_ubo,
                scene_ibo,
                scene_vbo,
                scene_matrixindices;
    }

    private static final class Textures {
        Texture2D  scene_color ;
        Texture2D scene_depthstencil ;
        Texture2D  scene_matrices ;
    }

    private static final class DrawCmd {
        static final int SIZE  = 5 * 4;

        int count;
        int instanceCount;
        int firstIndex;
        int  baseVertex;
        int baseInstance;
    };

    private static final class Geometry {
        int firstIndex;
        int count;
    };

    private static final class Tweak {
        MethodType method        = MethodType.METHOD_RASTER;
        int                result        = RESULT_REGULAR_CURRENT;
        int                 drawmode      = DRAW_STANDARD;
        boolean                      culling       = false;
        boolean                      freeze        = false;
        float                     minPixelSize  = 0.0f;
        float                     animate       = 0;
        float                     animateOffset = 0;
        boolean                      noui          = false;

        void set(Tweak o){
            method = o.method;
            result = o.result;
            drawmode = o.drawmode;
            culling = o.culling;
            freeze = o.freeze;
            minPixelSize = o.minPixelSize;
            animate = o.animate;
            animateOffset = o.animateOffset;
            noui = o.noui;
        }

        boolean equals(Tweak o){
            if(method != o.method) return false;
            if(result != o.result) return false;
            if(drawmode != o.drawmode) return false;
            if(culling != o.culling) return false;
            if(freeze != o.freeze) return false;
            if(minPixelSize != o.minPixelSize) return false;
            if(animate != o.animate) return false;
            if(animateOffset != o.animateOffset) return false;
            if(noui != o.noui) return false;

            return true;
        }
    };
}
