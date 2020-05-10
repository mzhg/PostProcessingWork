package nv.samples;

import com.nvidia.developer.opengl.app.NvSampleApp;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector2i;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.Random;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;

import static jet.opengl.postprocessing.common.GLenum.GL_ELEMENT_ARRAY_BUFFER;
import static jet.opengl.postprocessing.common.GLenum.GL_TEXTURE0;
import static jet.opengl.postprocessing.common.GLenum.GL_UNSIGNED_INT;

/**
 * Created by Administrator on 2017/7/30 0030.
 */

public final class DynamicLod extends NvSampleApp{
    private static final int SAMPLE_SIZE_WIDTH=(1024);
    private static final int SAMPLE_SIZE_HEIGHT=(768);
    private static final int SAMPLE_MAJOR_VERSION=(4);
    private static final int SAMPLE_MINOR_VERSION=(3);

    private static boolean USE_COMPACT_PARTICLE=true;
    private static final int VERTEX_POS  =    0,
            VERTEX_COLOR   = 1,

            UBO_SCENE     =0,
            UBO_CMDS      =1,

            UNI_USE_CMDOFFSET             =0,
            UNI_CONTENT_IDX_OFFSET        =0,
            UNI_CONTENT_IDX_MAX           =1,

            TEX_PARTICLES         =0,
            TEX_PARTICLEINDICES   =1,

            ABO_DATA_COUNTS       =0,

            SSBO_DATA_INDIRECTS   =0,
            SSBO_DATA_POINTS      =1,
            SSBO_DATA_BASIC       =2,
            SSBO_DATA_TESS        =3,

            PARTICLE_BATCHSIZE      =1024,
            PARTICLE_BASICVERTICES  =12,
            PARTICLE_BASICPRIMS     =20,
            PARTICLE_BASICINDICES   =(PARTICLE_BASICPRIMS*3);

    private final Programs programs= new Programs();
    private final Buffers buffers = new Buffers();
    private final Textures textures = new Textures();

    private final Tweak    tweak=new Tweak();
    private final Tweak    lastTweak=new Tweak();

    private final int[]    workGroupSize=new int[3];
    private final SceneData sceneUbo=new SceneData();
    private GLFuncProvider gl;

    private final Matrix4f m_proj=new Matrix4f();
    private final Matrix4f m_view=new Matrix4f();

    static int snapdiv(int input, int align)
    {
        return (input + align - 1) / align;
    }

    static int snapsize(int input, int align)
    {
        return ((input + align - 1) / align) * align;
    }

    Macro updateProgramDefines()
    {
//        progManager.m_prepend = std::string("");
//        progManager.m_prepend += ProgramManager::format("#define USE_INDICES %d\n", tweak.useindices ? 1 : 0);
        return new Macro("USE_INDICES", tweak.useindices ? 1 : 0);
    }

    boolean initProgram()
    {
        boolean validated=true;
//        progManager.addDirectory( std::string(PROJECT_NAME));
//        progManager.addDirectory( sysExePath() + std::string(PROJECT_RELDIRECTORY));
//        progManager.addDirectory( std::string(PROJECT_ABSDIRECTORY));
//
//        progManager.registerInclude("common.h", "common.h");

        Macro[] defines = CommonUtil.toArray(updateProgramDefines());
        final String shader_path= "nvidia/dynamic_lod/shaders/";

//        programs.draw_sphere_point = progManager.createProgram(
//                ProgramManager::Definition(GL_VERTEX_SHADER,   "spherepoint.vert.glsl"),
//        ProgramManager::Definition(GL_FRAGMENT_SHADER, "spherepoint.frag.glsl"));
        programs.draw_sphere_point=GLSLProgram.createProgram(shader_path+"spherepoint.vert.glsl", shader_path+"spherepoint.frag.glsl", defines);

//        programs.draw_sphere = progManager.createProgram(
//                ProgramManager::Definition(GL_VERTEX_SHADER,   "sphere.vert.glsl"),
//        ProgramManager::Definition(GL_FRAGMENT_SHADER, "sphere.frag.glsl"));
        programs.draw_sphere=GLSLProgram.createProgram(shader_path+"sphere.vert.glsl", shader_path+"sphere.frag.glsl", defines);

//        programs.draw_sphere_tess = progManager.createProgram(
//                ProgramManager::Definition(GL_VERTEX_SHADER,          "spheretess.vert.glsl"),
//        ProgramManager::Definition(GL_TESS_CONTROL_SHADER,    "spheretess.tctrl.glsl"),
//        ProgramManager::Definition(GL_TESS_EVALUATION_SHADER, "spheretess.teval.glsl"),
//        ProgramManager::Definition(GL_FRAGMENT_SHADER,        "sphere.frag.glsl"));

        programs.draw_sphere=GLSLProgram.createProgram(shader_path+"spheretess.vert.glsl", shader_path+"spheretess.tctrl.glsl",  shader_path+"spheretess.teval.glsl",
                shader_path+"sphere.frag.glsl", defines);

//        programs.lodcontent = progManager.createProgram(
//                ProgramManager::Definition(GL_VERTEX_SHADER,  "lodcontent.vert.glsl"));
        programs.lodcontent=GLSLProgram.createProgram(shader_path+"lodcontent.vert.glsl", null, defines);

//        programs.lodcmds = progManager.createProgram(
//                ProgramManager::Definition(GL_VERTEX_SHADER,  "lodcmds.vert.glsl"));
        programs.lodcmds=GLSLProgram.createProgram(shader_path+"lodcmds.vert.glsl", null, defines);

        defines = Arrays.copyOf(defines, defines.length+1);
        defines[1]=new Macro("USE_COMPUTE", 1);

//        programs.lodcontent_comp = progManager.createProgram(
//                ProgramManager::Definition(GL_COMPUTE_SHADER,  "#define USE_COMPUTE 1\n","lodcontent.vert.glsl"));
        programs.lodcontent_comp=GLSLProgram.createProgram(shader_path+"lodcontent.vert.glsl", null, defines);

//        programs.lodcmds_comp = progManager.createProgram(
//                ProgramManager::Definition(GL_COMPUTE_SHADER,  "#define USE_COMPUTE 1\n","lodcmds.vert.glsl"));
        programs.lodcmds_comp=GLSLProgram.createProgram(shader_path+"lodcmds.vert.glsl", null, defines);

//        validated = progManager.areProgramsValid();
//
//        if (validated){
//            glGetProgramiv(progManager.get(programs.lodcontent_comp),GL_COMPUTE_WORK_GROUP_SIZE,(GLint*)workGroupSize);
//        }
        IntBuffer groupSize=CacheBuffer.getCachedIntBuffer(3);
        gl.glGetProgramiv(programs.lodcontent_comp.getProgram(), GLenum.GL_COMPUTE_WORK_GROUP_SIZE, groupSize);
        groupSize.get(workGroupSize);

        return validated;
    }

    boolean initScene()
    {
        {
            // Sphere VBO/IBO
        final int Faces[] = {
                2, 1, 0,
                3, 2, 0,
                4, 3, 0,
                5, 4, 0,
                1, 5, 0,
                11, 6,  7,
                11, 7,  8,
                11, 8,  9,
                11, 9,  10,
                11, 10, 6,
                1, 2, 6,
                2, 3, 7,
                3, 4, 8,
                4, 5, 9,
                5, 1, 10,
                2,  7, 6,
                3,  8, 7,
                4,  9, 8,
                5, 10, 9,
                1, 6, 10 };

            final float Verts[] = {
                0.000f,  0.000f,  1.000f, 1.0f,
                0.894f,  0.000f,  0.447f, 1.0f,
                0.276f,  0.851f,  0.447f, 1.0f,
                -0.724f,  0.526f,  0.447f, 1.0f,
                -0.724f, -0.526f,  0.447f, 1.0f,
                0.276f, -0.851f,  0.447f, 1.0f,
                0.724f,  0.526f, -0.447f, 1.0f,
                -0.276f,  0.851f, -0.447f, 1.0f,
                -0.894f,  0.000f, -0.447f, 1.0f,
                -0.276f, -0.851f, -0.447f, 1.0f,
                0.724f, -0.526f, -0.447f, 1.0f,
                0.000f,  0.000f, -1.000f, 1.0f };

            int IndexCount  = /*sizeof(Faces) / sizeof(Faces[0])*/Faces.length;
            int VertexCount = /*sizeof(Verts) / sizeof(Verts[0])*/Verts.length;

            assert(IndexCount/3  == PARTICLE_BASICPRIMS);
            assert(VertexCount/4 == PARTICLE_BASICVERTICES);

//            geometry::Mesh<vec4> icosahedron;
//            icosahedron.m_vertices.resize( VertexCount/4 );
//            memcpy(&icosahedron.m_vertices[0], Verts, sizeof(Verts));
//            icosahedron.m_indicesTriangles.resize( IndexCount/3 );
//            memcpy(&icosahedron.m_indicesTriangles[0], Faces, sizeof(Faces));
//
//            icosahedron.flipWinding();
//
//            geometry::Mesh<vec4> batched;
//            for (int i = 0; i < PARTICLE_BATCHSIZE; i++){
//                batched.append(icosahedron);
//            }  TODO

            buffers.sphere_ibo= gl.glGenBuffer();
//            gl.glNamedBufferData(buffers.sphere_ibo, batched.getTriangleIndicesSize(), &batched.m_indicesTriangles[0], GL_STATIC_DRAW);
            gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, buffers.sphere_ibo);
            gl.glBufferData(GL_ELEMENT_ARRAY_BUFFER, CacheBuffer.wrap(Faces), GLenum.GL_STATIC_DRAW);

//            newBuffer(buffers.sphere_vbo);
//            glNamedBufferDataEXT(buffers.sphere_vbo, batched.getVerticesSize(), &batched.m_vertices[0], GL_STATIC_DRAW);
            buffers.sphere_vbo= gl.glGenBuffer();
            gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, buffers.sphere_vbo);
            gl.glBufferData(GLenum.GL_ARRAY_BUFFER, CacheBuffer.wrap(Verts), GLenum.GL_STATIC_DRAW);

            if(USE_COMPACT_PARTICLE) {
                gl.glVertexAttribFormat(VERTEX_POS, 3, GLenum.GL_FLOAT, false, 0);
                gl.glVertexAttribFormat(VERTEX_COLOR, 4, GLenum.GL_UNSIGNED_BYTE, true, /*offsetof(Particle, posColor.w)*/12);
            }else {
                gl.glVertexAttribFormat(VERTEX_POS, 4, GLenum.GL_FLOAT, false, 0);
                gl.glVertexAttribFormat(VERTEX_COLOR, 4, GLenum.GL_FLOAT, false, /*offsetof(Particle, color)*/16);
            }
            gl.glVertexAttribBinding(VERTEX_POS,   0);
            gl.glVertexAttribBinding(VERTEX_COLOR, 0);

            gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
            gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
        }


        { // Scene UBO
//            newBuffer(buffers.scene_ubo);
//            glNamedBufferDataEXT(buffers.scene_ubo, sizeof(SceneData), NULL, GL_DYNAMIC_DRAW);
            buffers.scene_ubo=gl.glGenBuffer();
            gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, buffers.scene_ubo);
            gl.glBufferData(GLenum.GL_UNIFORM_BUFFER, SceneData.SIZE, GLenum.GL_DYNAMIC_DRAW);
            gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, 0);
        }

        return true;
    }
    boolean initParticleBuffer()
    {
        {
//            std::vector<Particle> particles(tweak.particleCount);
//            std::vector<int>      particleindices(tweak.particleCount);
            ByteBuffer particles =CacheBuffer.getCachedByteBuffer(tweak.particleCount*(USE_COMPACT_PARTICLE ? 16:32));
            int[] particleindices=new int[tweak.particleCount];

            int cube = 1;
            while (cube * cube * (cube/4) < tweak.particleCount){
                cube++;
            }

            float scale = 128.0f/cube;
            sceneUbo.particleSize = scale * 0.375f;

//            srand(47345356);

            final Random random=new Random(47345356);
            final Vector3f pos=new Vector3f();
            for (int i = 0; i < tweak.particleCount; i++)
            {
                int x = i % cube;
                int z = (i / cube) % (cube);
                int y = i / (cube*cube);

//                vec3 pos = (vec3(0,frand(),0) - 0.5f) * 0.1f;
                pos.set(0,random.nextFloat(),0);
                Vector3f.sub(pos, 0.5f, pos);
                pos.scale(0.f);

//                pos += vec3(x,y,z);
                pos.x+=x;
                pos.y+=y;
                pos.z+=z;
//                pos -= vec3(cube,cube/4,cube) * 0.5f;
                pos.x-=cube*0.5f;
                pos.y-=cube/4*0.5f;
                pos.z-=cube*0.5f;
//                pos *= vec3(1,4,1);
                pos.y*=4;
                float size = (1.0f + random.nextFloat()*1.0f) * 0.25f;

//                vec4 color = vec4(random.nextFloat(),random.nextFloat(),random.nextFloat(),1.0f);
                float r=random.nextFloat();
                float g=random.nextFloat();
                float b=random.nextFloat();
                float a=1;
//#if USE_COMPACT_PARTICLE
//                union {
//                GLubyte color[4];
//                float   rawFloat;
//            } packed;
//                packed.color[0] = GLubyte(color.x * 255.0);
//                packed.color[1] = GLubyte(color.y * 255.0);
//                packed.color[2] = GLubyte(color.z * 255.0);
//                packed.color[3] = GLubyte(color.w * 255.0);
//
//                particles[i].posColor = vec4(pos * scale, packed.rawFloat);
//#else
//                particles[i].posSize  = vec4(pos,size) * scale;
//                particles[i].color    = color;
//#endif
                if(USE_COMPACT_PARTICLE){
                    particles.putFloat(pos.x*scale);
                    particles.putFloat(pos.y*scale);
                    particles.putFloat(pos.z*scale);
                    particles.put((byte)(r*255.0));
                    particles.put((byte)(g*255.0));
                    particles.put((byte)(b*255.0));
                    particles.put((byte)(a*255.0));
                }else{
                    particles.putFloat(pos.x*scale);
                    particles.putFloat(pos.y*scale);
                    particles.putFloat(pos.z*scale);
                    particles.putFloat(size*scale);

                    particles.putFloat(r).putFloat(g).putFloat(b).putFloat(a);
                }
                particleindices[i]    = i;
            }
            particles.flip();

//            newBuffer(buffers.particles);
//            glNamedBufferDataEXT(buffers.particles, sizeof(Particle) * tweak.particleCount, &particles[0], GL_STATIC_DRAW);
            buffers.particles=gl.glGenBuffer();
            gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, buffers.particles);
            gl.glBufferData(GLenum.GL_ARRAY_BUFFER, particles, GLenum.GL_STATIC_DRAW);

//            newBuffer(buffers.particleindices);
//            glNamedBufferDataEXT(buffers.particleindices, sizeof(int) * tweak.particleCount, &particleindices[0], GL_STATIC_DRAW);
            buffers.particleindices=gl.glGenBuffer();
            gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, buffers.particleindices);
            gl.glBufferData(GL_ELEMENT_ARRAY_BUFFER, CacheBuffer.wrap(particleindices), GLenum.GL_STATIC_DRAW);

            textures.particles=gl.glGenTexture();
            gl.glBindTexture(GLenum.GL_TEXTURE_BUFFER, textures.particles);
//            gl.glTextureBuffer(textures.particles,GL_TEXTURE_BUFFER,GL_RGBA32F, buffers.particles);
            gl.glTexBuffer(GLenum.GL_TEXTURE_BUFFER, GLenum.GL_RGBA32F, buffers.particles);
            gl.glBindTexture(GLenum.GL_TEXTURE_BUFFER, 0);

            int texels    = tweak.particleCount * /*(sizeof(Particle)/sizeof(vec4))*/ (USE_COMPACT_PARTICLE?1:2);
            int maxtexels = gl.glGetInteger(GLenum.GL_MAX_TEXTURE_BUFFER_SIZE_ARB );
            if ( texels > maxtexels ){
                System.out.printf("\nWARNING: buffer size too big for texturebuffer: %d max %d\n", texels, maxtexels);
            }
        }

        return true;
    }

    private int createBuffer(int target, int size, int usage){
        int buffer = gl.glGenBuffer();
        gl.glBindBuffer(target, buffer);
        gl.glBufferData(target, size, usage);
        gl.glBindBuffer(target, 0);
        return buffer;
    }

    private void updateSubresource(int target, int buffer, float[] data){
        FloatBuffer content = CacheBuffer.wrap(data);
        gl.glBindBuffer(target, buffer);
        gl.glBufferSubData(target, 0, content);
        gl.glBindBuffer(target, 0);
    }

    boolean initLodBuffers()
    {
        int  itemSize;
        int  itemFormat;
        int     itemTexels;

        if (tweak.useindices){
            itemSize    = /*sizeof(int)*/4;
            itemFormat  = GLenum.GL_R32I;
            itemTexels  = 1;
        }
        else{
            itemSize    = /*sizeof(Particle)*/USE_COMPACT_PARTICLE?16:32;
            itemFormat  = GLenum.GL_RGBA32F;
            itemTexels  = /*sizeof(Particle)/sizeof(vec4)*/USE_COMPACT_PARTICLE?1:2;
        }
        //size_t size   = snapsize(itemSize * tweak.particleCount, 256);
        int size  = snapsize(itemSize * (tweak.particleCount / tweak.jobCount), 256);

//        GLint maxtexels = 1;
        int texels    = (size / itemSize) * itemTexels;
        int maxtexels=gl.glGetInteger(GLenum.GL_MAX_TEXTURE_BUFFER_SIZE_ARB );
        if ( texels > maxtexels ){
            System.out.printf("\nWARNING: buffer size too big for texturebuffer: %d max %d\n", texels, maxtexels);
        }

//        newBuffer(buffers.lodparticles0);
//        glNamedBufferDataEXT(buffers.lodparticles0, size, NULL, GL_DYNAMIC_COPY);
        buffers.lodparticles0 = createBuffer(GLenum.GL_ARRAY_BUFFER, size, GLenum.GL_DYNAMIC_COPY);
//        newBuffer(buffers.lodparticles1);
//        glNamedBufferDataEXT(buffers.lodparticles1, size, NULL, GL_DYNAMIC_COPY);
        buffers.lodparticles1 = createBuffer(GLenum.GL_ARRAY_BUFFER, size, GLenum.GL_DYNAMIC_COPY);
//        newBuffer(buffers.lodparticles2);
//        glNamedBufferDataEXT(buffers.lodparticles2, size, NULL, GL_DYNAMIC_COPY);
        buffers.lodparticles2 = createBuffer(GLenum.GL_ARRAY_BUFFER, size, GLenum.GL_DYNAMIC_COPY);

//        newTexture(textures.lodparticles);
//        glTextureBufferEXT(textures.lodparticles,GL_TEXTURE_BUFFER, itemFormat, buffers.lodparticles0);
        textures.lodparticles=gl.glGenTexture();
        gl.glBindTexture(GLenum.GL_TEXTURE_BUFFER, textures.lodparticles);
        gl.glTexBuffer(GLenum.GL_TEXTURE_BUFFER, itemFormat, buffers.lodparticles0);

//        newBuffer(buffers.lodcmds);
//        glNamedBufferDataEXT(buffers.lodcmds, snapsize(sizeof(DrawIndirects),256) * tweak.jobCount, NULL, GL_DYNAMIC_COPY);
        buffers.lodcmds=createBuffer(GLenum.GL_ARRAY_BUFFER, snapsize(DrawIndirects.SIZE, 256)*tweak.jobCount, GLenum.GL_DYNAMIC_COPY);
//        glClearNamedBufferDataEXT(buffers.lodcmds,GL_RGBA32F,GL_RGBA, GL_FLOAT, NULL);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, buffers.lodcmds);
        gl.glClearBufferData(GLenum.GL_ARRAY_BUFFER, GLenum.GL_RGBA32F,GLenum.GL_RGBA, GLenum.GL_FLOAT, null);

        return true;
    }

    boolean begin()
    {
//        TwInit(TW_OPENGL_CORE,NULL);
//        TwWindowSize(m_window.m_viewsize[0],m_window.m_viewsize[1]);

        gl.glPixelStorei(GLenum.GL_UNPACK_ALIGNMENT, 1);
        gl.glEnable(GLenum.GL_CULL_FACE);
        gl.glEnable(GLenum.GL_DEPTH_TEST);

        boolean validated=true;

        int defaultVAO=gl.glGenVertexArray();
        gl.glBindVertexArray(defaultVAO);

        validated = validated && initProgram();
        validated = validated && initScene();
        validated = validated && initParticleBuffer();
        validated = validated && initLodBuffers();

        /*TwBar *bar = TwNewBar("mainbar");
        TwDefine(" GLOBAL contained=true help='OpenGL samples.\nCopyright NVIDIA Corporation 2013-2014' ");
        TwDefine(" mainbar position='0 0' size='250 250' color='0 0 0' alpha=128 ");
        TwDefine((std::string(" mainbar label='") + PROJECT_NAME + "'").c_str());

        TwAddVarRW(bar, "uselod",   TW_TYPE_BOOLCPP, &tweak.uselod, " label='use lod' ");
        TwAddVarRW(bar, "nolodtess",   TW_TYPE_BOOLCPP, &tweak.nolodtess, " label='use tess (if no lod)' ");
        TwAddVarRW(bar, "wireframe",   TW_TYPE_BOOLCPP, &tweak.wireframe, " label='wireframe' ");
        TwAddVarRW(bar, "indices",   TW_TYPE_BOOLCPP, &tweak.useindices, " label='use indexing' ");
        TwAddVarRW(bar, "compute",   TW_TYPE_BOOLCPP, &tweak.usecompute, " label='use compute' ");
        TwAddVarRW(bar, "pause",   TW_TYPE_BOOLCPP, &tweak.pause, " label='pause lod' ");
        TwAddVarRW(bar, "count",  TW_TYPE_INT32, &tweak.particleCount, " label='num particles' min=1 ");
        TwAddVarRW(bar, "jobs",   TW_TYPE_INT32, &tweak.jobCount, " label='num jobs' min=1 ");
        TwAddSeparator(bar,NULL,NULL);
        TwAddVarRW(bar, "near",   TW_TYPE_FLOAT, &sceneUbo.nearPixels, " label='lod near pixelsize' min=1 max=1000");
        TwAddVarRW(bar, "far",    TW_TYPE_FLOAT, &sceneUbo.farPixels, " label='lod far pixelsize' min=1 max=1000");
        TwAddVarRW(bar, "tess",   TW_TYPE_FLOAT, &sceneUbo.tessPixels, " label='tess pixelsize' min=1");
        TwAddSeparator(bar,NULL,NULL);
        TwAddVarRW(bar, "fov",   TW_TYPE_FLOAT, &tweak.fov, " label='fov degrees' min=1 max=90");*/

        sceneUbo.nearPixels = 10.0f;
        sceneUbo.farPixels  = 1.5f;
        sceneUbo.tessPixels   = 10.0f;

//        m_control.m_sceneOrbit = vec3(0.0f);
//        m_control.m_sceneDimension = 256.0f;
//        m_control.m_viewMatrix = nv_math::look_at(m_control.m_sceneOrbit + vec3(0.9,0.9,1)*m_control.m_sceneDimension*0.3f, m_control.m_sceneOrbit, vec3(0,1,0));

        return validated;
    }

    private void glBindMultiTextureEXT(int unit, int target, int texture){
        gl.glActiveTexture(unit);
        gl.glBindTexture(target, texture);
    }

    void drawLod()
    {
//        NV_PROFILE_SPLIT();

        // due to SSBO alignment (256 bytes) we need to calculate some counts
        // dynamically
        int itemSize;
        int itemFormat;

        if (tweak.useindices){
            itemSize    = /*sizeof(int)*/4;
            itemFormat  = GLenum.GL_R32I;
        }
        else{
            itemSize    = /*sizeof(Particle)*/USE_COMPACT_PARTICLE?16:32;
            itemFormat  = GLenum.GL_RGBA32F;
        }

        int jobSize    = snapsize(DrawIndirects.SIZE,256);
        int jobCount      = (int)(snapsize(itemSize * (tweak.particleCount / tweak.jobCount), 256) / itemSize);
        int jobOffset  = snapsize(itemSize * tweak.particleCount, 256);
        int jobs          = (int)snapdiv(tweak.particleCount, jobCount);
        int jobRest       = tweak.particleCount - (jobs - 1) * jobCount;

        int offset = 0;
        for (int i = 0; i < jobs; i++){
            int cnt = i == jobs-1 ? jobRest : jobCount;

            if (!tweak.pause || jobs > 1)
            {
//                NV_PROFILE_SECTION("Lod");
                gl.glEnable(GLenum.GL_RASTERIZER_DISCARD);

                {
//                    NV_PROFILE_SECTION("Cont");

                    gl.glUseProgram(tweak.usecompute ? programs.lodcontent_comp.getProgram() : programs.lodcontent.getProgram());

                    if (tweak.usecompute){
                        gl.glUniform1i(UNI_CONTENT_IDX_MAX, offset + cnt);
//                        gl.glBindMultiTextureEXT(GLenum.GL_TEXTURE0 + TEX_PARTICLES,       GLenum. GL_TEXTURE_BUFFER, textures.particles);
                        gl.glActiveTexture(GL_TEXTURE0 + TEX_PARTICLES);
                        gl.glBindTexture(GLenum. GL_TEXTURE_BUFFER, textures.particles);
                    }
                    else{
                        gl.glEnableVertexAttribArray(VERTEX_POS);
                        gl.glEnableVertexAttribArray(VERTEX_COLOR);

                        gl.glBindVertexBuffer(0, buffers.particles,/*sizeof(Particle)*/(USE_COMPACT_PARTICLE?16:32) * offset,/*sizeof(Particle)*/USE_COMPACT_PARTICLE?16:32);
                    }

                    gl.glUniform1i(UNI_CONTENT_IDX_OFFSET, offset);

                    gl.glBindBufferRange(GLenum.GL_ATOMIC_COUNTER_BUFFER, ABO_DATA_COUNTS,
                            buffers.lodcmds, jobSize * i, DrawCounters.SIZE);
                    gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, SSBO_DATA_POINTS,
                            buffers.lodparticles0);
                    gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, SSBO_DATA_BASIC,
                            buffers.lodparticles1);
                    gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, SSBO_DATA_TESS,
                            buffers.lodparticles2);

                    if (tweak.usecompute){
                        int numGroups = (cnt+workGroupSize[0]-1)/workGroupSize[0];

                        gl.glDispatchCompute(numGroups,1,1);
                    }
                    else{
                        gl.glDrawArrays(GLenum.GL_POINTS, 0, cnt);

                        gl.glDisableVertexAttribArray(VERTEX_POS);
                        gl.glDisableVertexAttribArray(VERTEX_COLOR);
                    }

                }

                {
//                    NV_PROFILE_SECTION("Cmds");

                    gl.glMemoryBarrier(GLenum.GL_SHADER_STORAGE_BARRIER_BIT | GLenum.GL_ATOMIC_COUNTER_BARRIER_BIT | GLenum.GL_TEXTURE_FETCH_BARRIER_BIT);

                    gl.glUseProgram(tweak.usecompute ? programs.lodcmds_comp.getProgram() : programs.lodcmds.getProgram());

                    gl.glBindBufferRange(GLenum.GL_SHADER_STORAGE_BUFFER, SSBO_DATA_INDIRECTS, buffers.lodcmds, jobSize * i, DrawIndirects.SIZE);
                    if (tweak.usecompute){
                        gl.glDispatchCompute(1,1,1);
                    }
                    else{
                        gl.glDrawArrays(GLenum.GL_POINTS,0,1);
                    }

                    gl.glMemoryBarrier(GLenum.GL_SHADER_STORAGE_BARRIER_BIT | GLenum.GL_ATOMIC_COUNTER_BARRIER_BIT | GLenum.GL_TEXTURE_FETCH_BARRIER_BIT | GLenum.GL_COMMAND_BARRIER_BIT);
                }

                gl.glDisable(GLenum.GL_RASTERIZER_DISCARD);
            }

            {
//                NV_PROFILE_SECTION("Draw");
                // the following drawcalls all source the amount of works from drawindirect buffers
                // generated above
                gl.glBindBuffer(GLenum.GL_DRAW_INDIRECT_BUFFER, buffers.lodcmds);
                //glEnable(GL_RASTERIZER_DISCARD);
                {
//                    NV_PROFILE_SECTION("Tess");

                    gl.glUseProgram(programs.draw_sphere_tess.getProgram());
                    gl.glPatchParameteri(GLenum.GL_PATCH_VERTICES,3);

                    gl.glBindVertexBuffer(0, buffers.sphere_vbo,0,/*sizeof(vec4)*/Vector4f.SIZE);
                    gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, buffers.sphere_ibo);
                    gl.glEnableVertexAttribArray(VERTEX_POS);

                    if (tweak.useindices){
                        glBindMultiTextureEXT(GL_TEXTURE0 + TEX_PARTICLES,        GLenum.GL_TEXTURE_BUFFER, textures.particles);
                        glBindMultiTextureEXT(GL_TEXTURE0 + TEX_PARTICLEINDICES,  GLenum.GL_TEXTURE_BUFFER, textures.lodparticles);
                    }
                    else{
                        glBindMultiTextureEXT(GL_TEXTURE0 + TEX_PARTICLES,        GLenum.GL_TEXTURE_BUFFER, textures.lodparticles);
                        glBindMultiTextureEXT(GL_TEXTURE0 + TEX_PARTICLEINDICES,  GLenum.GL_TEXTURE_BUFFER, 0);
                    }

                    gl.glTextureBuffer(textures.lodparticles, /*GLenum.GL_TEXTURE_BUFFER,*/ itemFormat,
                            buffers.lodparticles2);

                    gl.glBindBufferRange(GLenum.GL_UNIFORM_BUFFER,UBO_CMDS, buffers.lodcmds,
                            (i * jobSize), jobSize);

                    gl.glUniform1i(UNI_USE_CMDOFFSET, 0);
                    gl.glDrawElementsIndirect(GLenum.GL_PATCHES, GL_UNSIGNED_INT,
                            /*NV_BUFFER_OFFSET(offsetof(DrawIndirects,nearFull)*/DrawIndirects.SIZE- DrawElements.SIZE*2 + (i * jobSize));

                    gl.glUniform1i(UNI_USE_CMDOFFSET, 1);
                    gl.glDrawElementsIndirect(GLenum.GL_PATCHES, GL_UNSIGNED_INT,
                            /*NV_BUFFER_OFFSET(offsetof(DrawIndirects,nearRest)*/DrawIndirects.SIZE- DrawElements.SIZE + (i * jobSize));

                    gl.glDisableVertexAttribArray(VERTEX_POS);
                    gl.glBindVertexBuffer(0,0,0,0);
                    gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
                    gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER,UBO_CMDS,0);
                }

                {
//                    NV_PROFILE_SECTION("Mesh");

                    gl.glUseProgram(programs.draw_sphere.getProgram());

                    gl.glBindVertexBuffer(0, buffers.sphere_vbo,0,/*sizeof(vec4)*/Vector4f.SIZE);
                    gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, buffers.sphere_ibo);
                    gl.glEnableVertexAttribArray(VERTEX_POS);

                    if (tweak.useindices){
                        glBindMultiTextureEXT(GL_TEXTURE0 + TEX_PARTICLES,        GLenum.GL_TEXTURE_BUFFER, textures.particles);
                        glBindMultiTextureEXT(GL_TEXTURE0 + TEX_PARTICLEINDICES,  GLenum.GL_TEXTURE_BUFFER, textures.lodparticles);
                    }
                    else{
                        glBindMultiTextureEXT(GL_TEXTURE0 + TEX_PARTICLES,        GLenum.GL_TEXTURE_BUFFER, textures.lodparticles);
                        glBindMultiTextureEXT(GL_TEXTURE0 + TEX_PARTICLEINDICES,  GLenum.GL_TEXTURE_BUFFER, 0);
                    }

                    gl.glTextureBuffer(textures.lodparticles, /*GL_TEXTURE_BUFFER,*/ itemFormat,
                            buffers.lodparticles1);

                    gl.glBindBufferRange(GLenum.GL_UNIFORM_BUFFER,UBO_CMDS, buffers.lodcmds,
                            (i * jobSize), jobSize);

                    gl.glUniform1i(UNI_USE_CMDOFFSET, 0);
                    gl.glDrawElementsIndirect(GLenum.GL_TRIANGLES, GL_UNSIGNED_INT,
                            /*NV_BUFFER_OFFSET(offsetof(DrawIndirects,medFull)*/DrawIndirects.SIZE- DrawElements.SIZE*2 + (i * jobSize));

                    gl.glUniform1i(UNI_USE_CMDOFFSET, 1);
                    gl.glDrawElementsIndirect(GLenum.GL_TRIANGLES, GL_UNSIGNED_INT,
                            /*NV_BUFFER_OFFSET(offsetof(DrawIndirects,medRest)*/ DrawIndirects.SIZE- DrawElements.SIZE+ (i * jobSize));

                    gl.glDisableVertexAttribArray(VERTEX_POS);
                    gl.glBindVertexBuffer(0,0,0,0);
                    gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);
                    gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER,UBO_CMDS,0);
                }

                {
//                    NV_PROFILE_SECTION("Pnts");

                    gl.glEnable(GLenum.GL_VERTEX_PROGRAM_POINT_SIZE);

                    gl.glUseProgram(programs.draw_sphere_point.getProgram());

                    if (tweak.useindices){
                        glBindMultiTextureEXT(GL_TEXTURE0 + TEX_PARTICLES,        GLenum.GL_TEXTURE_BUFFER, textures.particles);
                        glBindMultiTextureEXT(GL_TEXTURE0 + TEX_PARTICLEINDICES,  GLenum.GL_TEXTURE_BUFFER, textures.lodparticles);
                    }
                    else{
                        gl.glEnableVertexAttribArray(VERTEX_POS);
                        gl.glEnableVertexAttribArray(VERTEX_COLOR);
                    }

                    if (tweak.useindices){
                        gl.glTextureBuffer(textures.lodparticles, /*GLenum.GL_TEXTURE_BUFFER,*/ itemFormat,
                                buffers.lodparticles0);
                        gl.glDrawArraysIndirect(GLenum.GL_POINTS, /*NV_BUFFER_OFFSET(offsetof(DrawIndirects,farArray)*/DrawCounters.SIZE + (i * jobSize));
                    }
                    else {
                        gl.glBindVertexBuffer(0, buffers.lodparticles0, 0, itemSize);
                        gl.glDrawArraysIndirect(GLenum.GL_POINTS, /*NV_BUFFER_OFFSET(offsetof(DrawIndirects,farArray)*/DrawCounters.SIZE + (i * jobSize));
                    }

                    if (!tweak.useindices){
                        gl.glDisableVertexAttribArray(VERTEX_POS);
                        gl.glDisableVertexAttribArray(VERTEX_COLOR);
                    }

                    gl.glDisable(GLenum.GL_VERTEX_PROGRAM_POINT_SIZE);

                    gl.glBindVertexBuffer(0,0,0,0);
                }

                gl.glBindBuffer(GLenum.GL_DRAW_INDIRECT_BUFFER, 0);
                //glDisable(GL_RASTERIZER_DISCARD);
            }

            offset += cnt;
        }

//        NV_PROFILE_SPLIT();
    }

    void think(float time)
    {
//        m_control.processActions(m_window.m_viewsize,
//                nv_math::vec2f(m_window.m_mouseCurrent[0],m_window.m_mouseCurrent[1]),
//        m_window.m_mouseButtonFlags, m_window.m_wheel);

        tweak.jobCount = Math.min(tweak.particleCount,tweak.jobCount);

        if (lastTweak.useindices != tweak.useindices)
        {
            updateProgramDefines();
//            progManager.reloadPrograms(); TODO need reload programs
        }

        if (lastTweak.particleCount != tweak.particleCount){
            initParticleBuffer();
            initLodBuffers();
        }

        if (lastTweak.jobCount != tweak.jobCount ||
                lastTweak.useindices != tweak.useindices )
        {
            initLodBuffers();
        }

//        if (m_window.onPress(KEY_R)){
//            progManager.reloadPrograms();
//            glGetProgramiv(progManager.get(programs.lodcontent_comp),GL_COMPUTE_WORK_GROUP_SIZE,(GLint*)workGroupSize);
//        }
//        if (!progManager.areProgramsValid()){
//            waitEvents();
//            return;
//        }

//        int width   = m_window.m_viewsize[0];
//        int height  = m_window.m_viewsize[1];
        int width=getGLContext().width();
        int height=getGLContext().height();

        gl.glViewport(0, 0, width, height);

        gl.glClearColor(0.1f,0.1f,0.1f,0.0f);
        gl.glClearDepthf(1.0f);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT | GLenum.GL_DEPTH_BUFFER_BIT | GLenum.GL_STENCIL_BUFFER_BIT);

        { // Update UBO
            gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, UBO_SCENE, buffers.scene_ubo);

            sceneUbo.viewport.set(width,height);

            float farplane = 1000.0f;

            Matrix4f projection = Matrix4f.perspective( (tweak.fov), (float)(width)/(float)(height), 0.1f, farplane, m_proj);
//            nv_math::mat4 view = m_control.m_viewMatrix;
            Matrix4f view=m_transformer.getModelViewMat(m_view);

//            vec4  hPos = projection * nv_math::vec4(1.0f,1.0f,-1000.0f,1.0f);
//            vec2  hCoord = vec2(hPos.x/hPos.w, hPos.y/hPos.w);
//            vec2  dim  = nv_math::nv_abs(hCoord);
            Vector4f hPos=new Vector4f(1.0f,1.0f,-1000.0f,1.0f);
            Matrix4f.transform(projection, hPos, hPos);
            Vector2f hCoord=new Vector2f(hPos.x/hPos.w, hPos.y/hPos.w);
            Vector2f dim= Vector2f.abs(hCoord, hCoord);

//            sceneUbo.viewpixelsize = dim * vec2(float(width),float(height)) * farplane * 0.5f;
            sceneUbo.viewpixelsize.set(dim);
            sceneUbo.viewpixelsize.x*=width*farplane * 0.5f;
            sceneUbo.viewpixelsize.y*=height*farplane * 0.5f;

//            sceneUbo.viewProjMatrix = projection * view;
            Matrix4f.mul(projection, view, sceneUbo.viewProjMatrix);
            sceneUbo.viewMatrix.load(view);
//            sceneUbo.viewMatrixIT = nv_math::transpose(nv_math::invert(view));
            Matrix4f.invert(view, sceneUbo.viewMatrixIT);
            sceneUbo.viewMatrixIT.transpose();

//            Frustum::init((float (*)[4])&sceneUbo.frustum[0].x,sceneUbo.viewProjMatrix.mat_array);

            ByteBuffer buffer=CacheBuffer.getCachedByteBuffer(SceneData.SIZE);
            sceneUbo.store(buffer).flip();
            gl.glBufferSubData(GLenum.GL_UNIFORM_BUFFER, 0, /*sizeof(SceneData), &sceneUbo*/buffer);
        }

        gl.glPolygonMode(GLenum.GL_FRONT_AND_BACK, tweak.wireframe ? GLenum.GL_LINE : GLenum.GL_FILL );

        if (tweak.uselod){
            drawLod();
        }
        else{
//            NV_PROFILE_SECTION("NoLod");

            boolean useTess = tweak.nolodtess;

            gl.glUseProgram(useTess ? programs.draw_sphere_tess.getProgram() : programs.draw_sphere.getProgram());
            gl.glPatchParameteri(GLenum.GL_PATCH_VERTICES,3);

            gl.glBindVertexBuffer(0, buffers.sphere_vbo,0,/*sizeof(vec4)*/Vector4f.SIZE);
            gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, buffers.sphere_ibo);

            gl.glEnableVertexAttribArray(VERTEX_POS);

            int fullCnt = tweak.particleCount / PARTICLE_BATCHSIZE;
            int restCnt = tweak.particleCount % PARTICLE_BATCHSIZE;

            int prim = useTess ? GLenum.GL_PATCHES: GLenum.GL_TRIANGLES;
            int  itemFormat;
            int     itemSize;
            int     itemBuffer;

            if (tweak.useindices){
                itemFormat  = GLenum.GL_R32I;
                itemSize    = /*sizeof(uint)*/4;
                itemBuffer  = buffers.particleindices;

                glBindMultiTextureEXT(GL_TEXTURE0 + TEX_PARTICLES, GLenum.GL_TEXTURE_BUFFER,       textures.particles);
                glBindMultiTextureEXT(GL_TEXTURE0 + TEX_PARTICLEINDICES, GLenum.GL_TEXTURE_BUFFER, textures.lodparticles);
            }
            else{
                itemFormat  = GLenum.GL_RGBA32F;
                itemSize    = /*sizeof(Particle)*/USE_COMPACT_PARTICLE?16:32;
                itemBuffer  = buffers.particles;

                glBindMultiTextureEXT(GL_TEXTURE0 + TEX_PARTICLES, GLenum.GL_TEXTURE_BUFFER,       textures.lodparticles);
                glBindMultiTextureEXT(GL_TEXTURE0 + TEX_PARTICLEINDICES, GLenum.GL_TEXTURE_BUFFER, 0);
            }

            gl.glTextureBuffer(textures.lodparticles, /*GL_TEXTURE_BUFFER,*/ itemFormat,  itemBuffer);
            gl.glDrawElementsInstanced(prim, PARTICLE_BATCHSIZE * PARTICLE_BASICINDICES, GL_UNSIGNED_INT, 0, fullCnt);

            if (restCnt!=0){
                gl.glTextureBufferRange(textures.lodparticles, /*GLenum.GL_TEXTURE_BUFFER,*/ itemFormat, itemBuffer,itemSize * fullCnt * PARTICLE_BATCHSIZE, restCnt * itemSize);
                gl.glDrawElementsInstanced(prim, restCnt * PARTICLE_BASICINDICES,GL_UNSIGNED_INT, 0, 1);
            }

            gl.glDisableVertexAttribArray(VERTEX_POS);

            gl.glBindVertexBuffer(0,0,0,0);
            gl.glBindBuffer(GLenum.GL_DRAW_INDIRECT_BUFFER, 0);
            gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        }

        gl.glPolygonMode( GLenum.GL_FRONT_AND_BACK, GLenum.GL_FILL );

        gl.glBindBufferBase( GLenum.GL_UNIFORM_BUFFER, UBO_SCENE, 0);

        {
//            NV_PROFILE_SECTION("TwDraw");
//            TwDraw();
        }

        lastTweak.set(tweak);
    }

    /*void Sample::resize(int width, int height)
    {
        TwWindowSize(width,height);
    }*/


    private static final class Programs {
        GLSLProgram
        draw_sphere_point,
                draw_sphere,
                draw_sphere_tess,
                lodcontent,
                lodcmds,
                lodcontent_comp,
                lodcmds_comp;
    } /*programs;*/

    private static final class Buffers {
        int
                sphere_vbo,
                sphere_ibo,
                scene_ubo,
                particles,
                particleindices,
                lodparticles0,
                lodparticles1,
                lodparticles2,
                lodcmds;
    } /*buffers;*/

    private static final class Textures {
        int
                particles,
                lodparticles;
    } /*textures;*/

    private static final class Tweak {
//        Tweak()
//        : particleCount(0xFFFFF)
//                , jobCount(1)
//                , pause(false)
//                , uselod(true)
//                , nolodtess(false)
//                , wireframe(false)
//                , useindices(true)
//                , usecompute(true)
//                , fov(60.0f)
//        {}

        int       particleCount = 0xFFFFF;
        int       jobCount =1;
        float     fov=60.0f;
        boolean      pause=false;
        boolean      uselod=true;
        boolean      nolodtess=false;
        boolean      wireframe=false;
        boolean      useindices=true;
        boolean      usecompute=true;

        void set(Tweak o){
            particleCount=o.particleCount;
            jobCount=o.jobCount;
            fov=o.fov;
            pause=o.pause;
            uselod=o.uselod;
            nolodtess=o.nolodtess;
            wireframe=o.wireframe;
            useindices=o.useindices;
            usecompute=o.usecompute;
        }
    };

    private static final class DrawArrays {
        static final int SIZE=16;
        int  count;
        int  instanceCount;
        int  first;
        int  baseInstance;
    };

    private static final class DrawElements {
        static final int SIZE=32;//todo ????
        int  count;
        int  instanceCount;
        int  first;
        int  baseVertex;
        int  baseInstance;
        int _pad0, _pad1;
    };

    private static final class DrawCounters {
        static final int SIZE=16;
        int  farCnt;
        int  medCnt;
        int  nearCnt;
        int  _pad;
    };

    private static final class DrawIndirects {
        static final int SIZE= DrawCounters.SIZE+ DrawArrays.SIZE+ DrawElements.SIZE*5;
        final DrawCounters  counters=new DrawCounters();

        final DrawArrays    farArray=new DrawArrays();
        final DrawElements  farIndexed=new DrawElements();

        final DrawElements  medFull=new DrawElements();
        final DrawElements  medRest=new DrawElements();

        final DrawElements  nearFull=new DrawElements();
        final DrawElements  nearRest=new DrawElements();
    };

    private static final class Particle_Compute {
        final Vector4f posColor=new Vector4f();
    };

    private static final class Particle{
        final Vector4f  posSize=new Vector4f();
        final Vector4f  color=new Vector4f();
    };

    public static final class SceneData{
        static final int SIZE = Matrix4f.SIZE*3+8*Vector4f.SIZE;

        final Matrix4f  viewProjMatrix=new Matrix4f();
        final Matrix4f  viewMatrix =new Matrix4f();
        final Matrix4f  viewMatrixIT=new Matrix4f();

        final Vector2i viewport=new Vector2i();
        final Vector2f viewpixelsize=new Vector2f();

        final Vector4f[] frustum=new Vector4f[6];

        float farPixels;
        float nearPixels;
        float tessPixels;
        float particleSize;

        public SceneData(){
            for(int i=0;i<frustum.length;i++)
                frustum[i]=new Vector4f();
        }

        public ByteBuffer store(ByteBuffer buf){
            viewProjMatrix.store(buf);
            viewMatrix.store(buf);
            viewMatrixIT.store(buf);
            viewport.store(buf);
            viewpixelsize.store(buf);
            CacheBuffer.put(buf, frustum);

            buf.putFloat(farPixels);
            buf.putFloat(nearPixels);
            buf.putFloat(tessPixels);
            buf.putFloat(particleSize);
            return buf;
        }
    }
}
