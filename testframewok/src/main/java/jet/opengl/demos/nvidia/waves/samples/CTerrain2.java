package jet.opengl.demos.nvidia.waves.samples;

import com.nvidia.developer.opengl.utils.NvImage;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.io.IOException;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.buffer.VertexArrayObject;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureDataDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;
import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by mazhen'gui on 2017/7/27.
 */

final class CTerrain2 implements Constants{
    private static final int NumMarkersXY = 10;
    private static final int NumMarkers = NumMarkersXY*NumMarkersXY;

    int MultiSampleCount;
    int MultiSampleQuality;

    Texture2D rock_bump_textureSRV;

    Texture2D sky_textureSRV;

    Texture2D foam_intensity_textureSRV;

    Texture2D foam_diffuse_textureSRV;

    Texture2D reflection_color_resource;
    Texture2D reflection_color_resourceSRV;
    Texture2D reflection_color_resourceRTV;
    Texture2D refraction_color_resource;
    Texture2D refraction_color_resourceSRV;
    Texture2D refraction_color_resourceRTV;

    Texture2D shadowmap_resource;
    Texture2D shadowmap_resourceSRV;
    Texture2D shadowmap_resourceDSV;

    Texture2D reflection_depth_resource;
    Texture2D reflection_depth_resourceDSV;

    Texture2D refraction_depth_resource;
    Texture2D refraction_depth_resourceRTV;
    Texture2D refraction_depth_resourceSRV;

    Texture2D main_color_resource;
    Texture2D main_color_resourceSRV;
    Texture2D main_color_resourceRTV;
    Texture2D main_depth_resource;
    Texture2D main_depth_resourceDSV;
    Texture2D main_depth_resourceSRV;
    Texture2D main_color_resource_resolved;
    Texture2D main_color_resource_resolvedSRV;

//    ID3D11Device* pDevice;
//    ID3DX11Effect* pEffect;

    final float[][]		height = new float[terrain_gridpoints+1][terrain_gridpoints+1];
    final Vector3f[][]  normal = new Vector3f[terrain_gridpoints+1][terrain_gridpoints+1];
    final Vector3f[][]	tangent = new Vector3f[terrain_gridpoints + 1][terrain_gridpoints + 1];
    final Vector3f[][]	binormal = new Vector3f[terrain_gridpoints + 1][terrain_gridpoints + 1];

    Texture2D heightmap_texture;
    Texture2D heightmap_textureSRV;

    Texture2D layerdef_texture;
    Texture2D layerdef_textureSRV;

    Texture2D depthmap_texture;
    Texture2D depthmap_textureSRV;

    BufferGL heightfield_vertexbuffer;
    BufferGL sky_vertexbuffer;

    VertexArrayObject heightfield_inputlayout;
    VertexArrayObject trianglestrip_inputlayout;

    SampleD3D11 m_context;

    CTerrain2(){
        for(int i = 0; i < normal.length; i++){
            for(int j= 0;j < normal[i].length; j++){
                normal[i][j] = new Vector3f();
            }
        }

        for(int i = 0; i < tangent.length; i++){
            for(int j= 0;j < tangent[i].length; j++){
                tangent[i][j] = new Vector3f();
            }
        }

        for(int i = 0; i < binormal.length; i++){
            for(int j= 0;j < binormal[i].length; j++){
                binormal[i][j] = new Vector3f();
            }
        }
    }

    void Initialize(SampleD3D11 context){
//        pEffect = effect;
//        pDevice = device;
        m_context = context;


//        const D3D11_INPUT_ELEMENT_DESC TerrainLayout =
//                { "PATCH_PARAMETERS",  0, DXGI_FORMAT_R32G32B32A32_FLOAT,   0, 0,  D3D11_INPUT_PER_VERTEX_DATA, 0 };
//
//        D3DX11_PASS_DESC passDesc;
//        effect->GetTechniqueByName("RenderHeightfield")->GetPassByIndex(0)->GetDesc(&passDesc);
//
//        device->CreateInputLayout( &TerrainLayout, 1, passDesc.pIAInputSignature, passDesc.IAInputSignatureSize, &heightfield_inputlayout );
//
//        const D3D11_INPUT_ELEMENT_DESC SkyLayout [] =
//                {
//                        { "POSITION",  0, DXGI_FORMAT_R32G32B32A32_FLOAT,   0, 0,  D3D11_INPUT_PER_VERTEX_DATA, 0 },
//                        { "TEXCOORD",  0, DXGI_FORMAT_R32G32_FLOAT,   0, 16,  D3D11_INPUT_PER_VERTEX_DATA, 0 }
//                };
//
//        effect->GetTechniqueByName("RenderSky")->GetPassByIndex(0)->GetDesc(&passDesc);
//
//        device->CreateInputLayout( SkyLayout,
//                2,
//                passDesc.pIAInputSignature,
//                passDesc.IAInputSignatureSize,
//                &trianglestrip_inputlayout );

        CreateTerrain();
    }

    void DeInitialize(){
        CommonUtil.safeRelease(main_color_resource);
        CommonUtil.safeRelease(main_color_resourceSRV);
        CommonUtil.safeRelease(main_color_resourceRTV);

        CommonUtil.safeRelease(main_color_resource_resolved);
        CommonUtil.safeRelease(main_color_resource_resolvedSRV);

        CommonUtil.safeRelease(main_depth_resource);
        CommonUtil.safeRelease(main_depth_resourceDSV);
        CommonUtil.safeRelease(main_depth_resourceSRV);

        CommonUtil.safeRelease(reflection_color_resource);
        CommonUtil.safeRelease(reflection_color_resourceSRV);
        CommonUtil.safeRelease(reflection_color_resourceRTV);
        CommonUtil.safeRelease(refraction_color_resource);
        CommonUtil.safeRelease(refraction_color_resourceSRV);
        CommonUtil.safeRelease(refraction_color_resourceRTV);

        CommonUtil.safeRelease(reflection_depth_resource);
        CommonUtil.safeRelease(reflection_depth_resourceDSV);
        CommonUtil.safeRelease(refraction_depth_resource);
        CommonUtil.safeRelease(refraction_depth_resourceSRV);
        CommonUtil.safeRelease(refraction_depth_resourceRTV);

        CommonUtil.safeRelease(shadowmap_resource);
        CommonUtil.safeRelease(shadowmap_resourceDSV);
        CommonUtil.safeRelease(shadowmap_resourceSRV);
    }

    void ReCreateBuffers(){
        DeInitialize();

        Texture2DDesc tex_desc = new Texture2DDesc();
        tex_desc.width              = (int)(BackbufferWidth*main_buffer_size_multiplier);
        tex_desc.height             = (int)(BackbufferHeight*main_buffer_size_multiplier);
        tex_desc.mipLevels          = 1;
        tex_desc.arraySize          = 1;
        tex_desc.format             = GLenum.GL_RGBA8; // DXGI_FORMAT_R8G8B8A8_UNORM;
//        tex_desc.SampleDesc.Count   = MultiSampleCount;
//        tex_desc.SampleDesc.Quality = MultiSampleQuality;
//        tex_desc.Usage              = D3D11_USAGE_DEFAULT;
//        tex_desc.BindFlags          = D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_RENDER_TARGET;
//        tex_desc.CPUAccessFlags     = 0;
//        tex_desc.MiscFlags          = 0;

        main_color_resourceRTV = main_color_resourceSRV = TextureUtils.createTexture2D(tex_desc, null);
        main_color_resource_resolvedSRV = TextureUtils.createTexture2D(tex_desc, null);  // sample count must be 1.

        tex_desc.format             = GLenum.GL_DEPTH_COMPONENT32F;
        main_depth_resourceSRV      = main_depth_resourceDSV = TextureUtils.createTexture2D(tex_desc, null);

        tex_desc.width              = (int)(BackbufferWidth*reflection_buffer_size_multiplier);
        tex_desc.height             = (int)(BackbufferHeight*reflection_buffer_size_multiplier);
        tex_desc.mipLevels          = (int)Math.max(1.0f, Math.log(Math.max((float)tex_desc.width,(float)tex_desc.height))/Math.log(2.0f));
        tex_desc.arraySize          = 1;
        tex_desc.format             = GLenum.GL_RGBA8;

        reflection_color_resourceSRV = reflection_color_resourceRTV = TextureUtils.createTexture2D(tex_desc, null);

        tex_desc.width              = (int)(BackbufferWidth*refraction_buffer_size_multiplier);
        tex_desc.height             = (int)(BackbufferHeight*refraction_buffer_size_multiplier);
        tex_desc.mipLevels          = (int)Math.max(1.0f,Math.log(Math.max((float)tex_desc.width,(float)tex_desc.height))/Math.log(2.0f));
        tex_desc.arraySize          = 1;
        tex_desc.format             = GLenum.GL_RGBA8;

        refraction_color_resourceSRV = refraction_color_resourceRTV = TextureUtils.createTexture2D(tex_desc, null);

        tex_desc.width              = (int)(BackbufferWidth*reflection_buffer_size_multiplier);
        tex_desc.height             = (int)(BackbufferHeight*reflection_buffer_size_multiplier);
        tex_desc.mipLevels          = 1;
        tex_desc.arraySize          = 1;
        tex_desc.format             = GLenum.GL_DEPTH_COMPONENT32F;

        reflection_depth_resourceDSV = TextureUtils.createTexture2D(tex_desc, null);

        tex_desc.width              = (int)(BackbufferWidth*refraction_buffer_size_multiplier);
        tex_desc.height             = (int)(BackbufferHeight*refraction_buffer_size_multiplier);
        tex_desc.mipLevels          = 1;
        tex_desc.arraySize          = 1;
        tex_desc.format             = GLenum.GL_DEPTH_COMPONENT32F;

        refraction_depth_resourceSRV = TextureUtils.createTexture2D(tex_desc, null);

        // recreating shadowmap resource
        tex_desc.width              = shadowmap_resource_buffer_size_xy;
        tex_desc.height             = shadowmap_resource_buffer_size_xy;
        tex_desc.mipLevels          = 1;
        tex_desc.arraySize          = 1;
        tex_desc.format             = GLenum.GL_DEPTH_COMPONENT32F;

        shadowmap_resourceSRV = shadowmap_resourceDSV = TextureUtils.createTexture2D(tex_desc, null);
    }

    void LoadTextures(){
        try {
            int rock_bump_texture = NvImage.uploadTextureFromDDSFile("nvidia/WaveWorks/textures/rock_bump6.dds");
            rock_bump_textureSRV = TextureUtils.createTexture2D(GLenum.GL_TEXTURE_2D, rock_bump_texture);

            int sky_texture = NvImage.uploadTextureFromDDSFile("nvidia/WaveWorks/textures/SunsetFair.dds");
            sky_textureSRV = TextureUtils.createTexture2D(GLenum.GL_TEXTURE_2D, sky_texture);

            int foam_intensity_texture = NvImage.uploadTextureFromDDSFile("nvidia/WaveWorks/textures/foam_intensity_perlin2.dds");
            foam_intensity_textureSRV = TextureUtils.createTexture2D(GLenum.GL_TEXTURE_2D, foam_intensity_texture);

            int foam_diffuse_texture = NvImage.uploadTextureFromDDSFile("nvidia/WaveWorks/textures/foam24bit.dds");
            foam_diffuse_textureSRV = TextureUtils.createTexture2D(GLenum.GL_TEXTURE_2D, foam_diffuse_texture);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    void Render(Matrix4f matProj, Matrix4f matView){

    }
    void RenderTerrainToHeightField(Matrix4f worldToViewMatrix, Matrix4f viewToProjectionMatrix,
                                    ReadableVector3f eyePositionWS, ReadableVector3f viewDirectionWS){

    }

    private static int gp_wrap( int a)
    {
        if(a<0) return (a+terrain_gridpoints);
        if(a>=terrain_gridpoints) return (a-terrain_gridpoints);
        return a;
    }

    float bilinear_interpolation(float fx, float fy, float a, float b, float c, float d)
    {
        float s1,s2,s3,s4;
        s1=fx*fy;
        s2=(1-fx)*fy;
        s3=(1-fx)*(1-fy);
        s4=fx*(1-fy);
        return((a*s3+b*s4+c*s1+d*s2));
    }

    void CreateTerrain(){
        int i,j,k,l;
        float x,z;
        int ix,iz;
        float[] backterrain;
        int currentstep=terrain_gridpoints;
        float mv,rm;
        float offset=0,yscale=0,maxheight=0,minheight=0;

        float []height_linear_array;
        float []patches_rawdata;
//        HRESULT result;
        TextureDataDesc subresource_data = new TextureDataDesc();
        Texture2DDesc tex_desc = new Texture2DDesc();
//        D3D11_SHADER_RESOURCE_VIEW_DESC textureSRV_desc;

//        backterrain = (float *) malloc((terrain_gridpoints+1)*(terrain_gridpoints+1)*sizeof(float));
        backterrain = new float[(terrain_gridpoints+1)*(terrain_gridpoints+1)];
        rm=terrain_fractalinitialvalue;
        backterrain[0]=0;
        backterrain[0+terrain_gridpoints*terrain_gridpoints]=0;
        backterrain[terrain_gridpoints]=0;
        backterrain[terrain_gridpoints+terrain_gridpoints*terrain_gridpoints]=0;
        currentstep=terrain_gridpoints;
//        srand(3);

        // generating fractal terrain using square-diamond method
        while (currentstep>1)
        {
            //square step;
            i=0;
            j=0;


            while (i<terrain_gridpoints)
            {
                j=0;
                while (j<terrain_gridpoints)
                {

                    mv=backterrain[i+terrain_gridpoints*j];
                    mv+=backterrain[(i+currentstep)+terrain_gridpoints*j];
                    mv+=backterrain[(i+currentstep)+terrain_gridpoints*(j+currentstep)];
                    mv+=backterrain[i+terrain_gridpoints*(j+currentstep)];
                    mv/=4.0;
                    backterrain[i+currentstep/2+terrain_gridpoints*(j+currentstep/2)]=(mv+rm*(Numeric.random()-0.5f));
                    j+=currentstep;
                }
                i+=currentstep;
            }

            //diamond step;
            i=0;
            j=0;

            while (i<terrain_gridpoints)
            {
                j=0;
                while (j<terrain_gridpoints)
                {

                    mv=0;
                    mv=backterrain[i+terrain_gridpoints*j];
                    mv+=backterrain[(i+currentstep)+terrain_gridpoints*j];
                    mv+=backterrain[(i+currentstep/2)+terrain_gridpoints*(j+currentstep/2)];
                    mv+=backterrain[i+currentstep/2+terrain_gridpoints*gp_wrap(j-currentstep/2)];
                    mv/=4;
                    backterrain[i+currentstep/2+terrain_gridpoints*j]=(float)(mv+rm*(Numeric.random()-0.5f));

                    mv=0;
                    mv=backterrain[i+terrain_gridpoints*j];
                    mv+=backterrain[i+terrain_gridpoints*(j+currentstep)];
                    mv+=backterrain[(i+currentstep/2)+terrain_gridpoints*(j+currentstep/2)];
                    mv+=backterrain[gp_wrap(i-currentstep/2)+terrain_gridpoints*(j+currentstep/2)];
                    mv/=4;
                    backterrain[i+terrain_gridpoints*(j+currentstep/2)]=(float)(mv+rm*(Numeric.random()-0.5f));

                    mv=0;
                    mv=backterrain[i+currentstep+terrain_gridpoints*j];
                    mv+=backterrain[i+currentstep+terrain_gridpoints*(j+currentstep)];
                    mv+=backterrain[(i+currentstep/2)+terrain_gridpoints*(j+currentstep/2)];
                    mv+=backterrain[gp_wrap(i+currentstep/2+currentstep)+terrain_gridpoints*(j+currentstep/2)];
                    mv/=4;
                    backterrain[i+currentstep+terrain_gridpoints*(j+currentstep/2)]=(float)(mv+rm*(Numeric.random()-0.5f));

                    mv=0;
                    mv=backterrain[i+currentstep+terrain_gridpoints*(j+currentstep)];
                    mv+=backterrain[i+terrain_gridpoints*(j+currentstep)];
                    mv+=backterrain[(i+currentstep/2)+terrain_gridpoints*(j+currentstep/2)];
                    mv+=backterrain[i+currentstep/2+terrain_gridpoints*gp_wrap(j+currentstep/2+currentstep)];
                    mv/=4;
                    backterrain[i+currentstep/2+terrain_gridpoints*(j+currentstep)]=(float)(mv+rm*(Numeric.random()-0.5f));
                    j+=currentstep;
                }
                i+=currentstep;
            }
            //changing current step;
            currentstep/=2;
            rm*=terrain_fractalfactor;
        }

        // scaling to minheight..maxheight range
        for (i=0;i<terrain_gridpoints+1;i++)
            for (j=0;j<terrain_gridpoints+1;j++)
            {
                height[i][j]=backterrain[i+terrain_gridpoints*j];
            }
        maxheight=height[0][0];
        minheight=height[0][0];
        for(i=0;i<terrain_gridpoints+1;i++)
            for(j=0;j<terrain_gridpoints+1;j++)
            {
                if(height[i][j]>maxheight) maxheight=height[i][j];
                if(height[i][j]<minheight) minheight=height[i][j];
            }
        offset=minheight-terrain_minheight;
        yscale=(terrain_maxheight-terrain_minheight)/(maxheight-minheight);

        for(i=0;i<terrain_gridpoints+1;i++)
            for(j=0;j<terrain_gridpoints+1;j++)
            {
                height[i][j]-=minheight;
                height[i][j]*=yscale;
                height[i][j]+=terrain_minheight;
            }

        // moving down edges of heightmap
        for (i=0;i<terrain_gridpoints+1;i++)
            for (j=0;j<terrain_gridpoints+1;j++)
            {
                mv=((i-terrain_gridpoints/2.0f)*(i-terrain_gridpoints/2.0f)+(j-terrain_gridpoints/2.0f)*(j-terrain_gridpoints/2.0f));
                rm=((terrain_gridpoints*0.7f)*(terrain_gridpoints*0.7f)/4.0f);
                height[i][j]-=((mv-rm)/900.0f)*terrain_geometry_scale;
            }


        // terrain banks
        for(k=0;k<4;k++)
        {
            for(i=0;i<terrain_gridpoints+1;i++)
                for(j=0;j<terrain_gridpoints+1;j++)
                {
                    mv=height[i][j];
                    if((mv) > 0.02f)
                    {
                        mv -= 0.02f;
                    }
                    if(mv < -3.00f)
                    {
                        mv += 0.5f;
                    }
                    height[i][j]=mv;
                }
        }

//        XMVECTOR vec1, vec2, vec3;
        final Vector4f vec1 = new Vector4f();
        final Vector4f vec2 = new Vector4f();
        final Vector3f vec3 = new Vector3f();

        // smoothing
        for(k=0;k<terrain_smoothsteps;k++)
        {
            for(i=0;i<terrain_gridpoints+1;i++)
                for(j=0;j<terrain_gridpoints+1;j++)
                {
                    vec1.set(2 * terrain_geometry_scale, terrain_geometry_scale*(height[gp_wrap(i + 1)][j] - height[gp_wrap(i - 1)][j]), 0, 0);
                    vec2.set(0, -terrain_geometry_scale*(height[i][gp_wrap(j + 1)] - height[i][gp_wrap(j - 1)]), -2 * terrain_geometry_scale, 0);

// 				vec1.x=2*terrain_geometry_scale;
// 				vec1.y=terrain_geometry_scale*(height[gp_wrap(i+1)][j]-height[gp_wrap(i-1)][j]);
// 				vec1.z=0;
// 				vec2.x=0;
// 				vec2.y=-terrain_geometry_scale*(height[i][gp_wrap(j+1)]-height[i][gp_wrap(j-1)]);
// 				vec2.z=-2*terrain_geometry_scale;

//                    XMVECTOR vec3 = XMVector3Cross(vec1, vec2);
//                    vec3 = XMVector3Normalize(vec3);
                    Vector3f.cross(vec1 , vec2, vec3);
                    vec3.normalise();

                    if(((vec3.y>terrain_rockfactor)||(height[i][j]<1.2f)))
                    {
                        rm=terrain_smoothfactor1;
                        mv=height[i][j]*(1.0f-rm) +rm*0.25f*(height[gp_wrap(i-1)][j]+height[i][gp_wrap(j-1)]+height[gp_wrap(i+1)][j]+height[i][gp_wrap(j+1)]);
                        backterrain[i+terrain_gridpoints*j]=mv;
                    }
                    else
                    {
                        rm=terrain_smoothfactor2;
                        mv=height[i][j]*(1.0f-rm) +rm*0.25f*(height[gp_wrap(i-1)][j]+height[i][gp_wrap(j-1)]+height[gp_wrap(i+1)][j]+height[i][gp_wrap(j+1)]);
                        backterrain[i+terrain_gridpoints*j]=mv;
                    }

                }
            for (i=0;i<terrain_gridpoints+1;i++)
                for (j=0;j<terrain_gridpoints+1;j++)
                {
                    height[i][j]=(backterrain[i+terrain_gridpoints*j]);
                }
        }
        for(i=0;i<terrain_gridpoints+1;i++)
            for(j=0;j<terrain_gridpoints+1;j++)
            {
                rm=0.5f;
                mv=height[i][j]*(1.0f-rm) +rm*0.25f*(height[gp_wrap(i-1)][j]+height[i][gp_wrap(j-1)]+height[gp_wrap(i+1)][j]+height[i][gp_wrap(j+1)]);
                backterrain[i+terrain_gridpoints*j]=mv;
            }
        for (i=0;i<terrain_gridpoints+1;i++)
            for (j=0;j<terrain_gridpoints+1;j++)
            {
                height[i][j]=(backterrain[i+terrain_gridpoints*j]);
            }


        backterrain = null;

        //calculating normals
        for (i=0;i<terrain_gridpoints+1;i++)
            for (j=0;j<terrain_gridpoints+1;j++)
            {
                vec1.set(2 * terrain_geometry_scale, terrain_geometry_scale*(height[gp_wrap(i + 1)][j] - height[gp_wrap(i - 1)][j]), 0, 0);
                vec2.set(0, -terrain_geometry_scale*(height[i][gp_wrap(j + 1)] - height[i][gp_wrap(j - 1)]), -2 * terrain_geometry_scale, 0);

// 			vec1.x=2*terrain_geometry_scale;
// 			vec1.y=terrain_geometry_scale*(height[gp_wrap(i+1)][j]-height[gp_wrap(i-1)][j]);
// 			vec1.z=0;
// 			vec2.x=0;
// 			vec2.y=-terrain_geometry_scale*(height[i][gp_wrap(j+1)]-height[i][gp_wrap(j-1)]);
// 			vec2.z=-2*terrain_geometry_scale;

//                vec3 = XMVector3Cross(vec1, vec2);
//                vec3 = XMVector3Normalize(vec3);\
                Vector3f.cross(vec1 , vec2, vec3);
                vec3.normalise();

//                XMStoreFloat3(&normal[i][j], vec3);
                normal[i][j].set(vec3);
            }


        // buiding layerdef
        byte[] temp_layerdef_map_texture_pixels=new byte[terrain_layerdef_map_texture_size*terrain_layerdef_map_texture_size*4];
        byte[] layerdef_map_texture_pixels= new byte[terrain_layerdef_map_texture_size*terrain_layerdef_map_texture_size*4];
        for(i=0;i<terrain_layerdef_map_texture_size;i++)
            for(j=0;j<terrain_layerdef_map_texture_size;j++)
            {
                x=(float)(terrain_gridpoints)*((float)i/(float)terrain_layerdef_map_texture_size);
                z=(float)(terrain_gridpoints)*((float)j/(float)terrain_layerdef_map_texture_size);
                ix=(int)Math.floor(x);
                iz=(int)Math.floor(z);
                rm=bilinear_interpolation(x-ix,z-iz,height[ix][iz],height[ix+1][iz],height[ix+1][iz+1],height[ix][iz+1])*terrain_geometry_scale;

                temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+0]=0;
                temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+1]=0;
                temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+2]=0;
                temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+3]=0;

                if((rm>terrain_height_underwater_start)&&(rm<=terrain_height_underwater_end))
                {
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+0]= (byte) 255;
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+1]=0;
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+2]=0;
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+3]=0;
                }

                if((rm>terrain_height_sand_start)&&(rm<=terrain_height_sand_end))
                {
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+0]=0;
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+1]= (byte) 255;
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+2]=0;
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+3]=0;
                }

                if((rm>terrain_height_grass_start)&&(rm<=terrain_height_grass_end))
                {
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+0]=0;
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+1]=0;
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+2]= (byte) 255;
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+3]=0;
                }

                mv=bilinear_interpolation(x-ix,z-iz,normal[ix][iz].y,normal[ix+1][iz].y,normal[ix+1][iz+1].y,normal[ix][iz+1].y);

                if((mv<terrain_slope_grass_start)&&(rm>terrain_height_sand_end))
                {
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+0]=0;
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+1]=0;
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+2]=0;
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+3]=0;
                }

                if((mv<terrain_slope_rocks_start)&&(rm>terrain_height_rocks_start))
                {
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+0]=0;
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+1]=0;
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+2]=0;
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+3]= (byte) 255;
                }

            }
        for(i=0;i<terrain_layerdef_map_texture_size;i++)
            for(j=0;j<terrain_layerdef_map_texture_size;j++)
            {
                layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+0]=temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+0];
                layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+1]=temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+1];
                layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+2]=temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+2];
                layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+3]=temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+3];
            }


        for(i=2;i<terrain_layerdef_map_texture_size-2;i++)
            for(j=2;j<terrain_layerdef_map_texture_size-2;j++)
            {
                int n1=0;
                int n2=0;
                int n3=0;
                int n4=0;
                for(k=-2;k<3;k++)
                    for(l=-2;l<3;l++)
                    {
                        n1+=temp_layerdef_map_texture_pixels[((j+k)*terrain_layerdef_map_texture_size+i+l)*4+0];
                        n2+=temp_layerdef_map_texture_pixels[((j+k)*terrain_layerdef_map_texture_size+i+l)*4+1];
                        n3+=temp_layerdef_map_texture_pixels[((j+k)*terrain_layerdef_map_texture_size+i+l)*4+2];
                        n4+=temp_layerdef_map_texture_pixels[((j+k)*terrain_layerdef_map_texture_size+i+l)*4+3];
                    }
                layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+0]=(byte)(n1/25);
                layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+1]=(byte)(n2/25);
                layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+2]=(byte)(n3/25);
                layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+3]=(byte)(n4/25);
            }

        // putting the generated data to textures
//        subresource_data.pSysMem = layerdef_map_texture_pixels;
//        subresource_data.SysMemPitch = terrain_layerdef_map_texture_size*4;
//        subresource_data.SysMemSlicePitch = 0;
        subresource_data.data = layerdef_map_texture_pixels;
        subresource_data.format = TextureUtils.measureFormat(GLenum.GL_RGBA8);
        subresource_data.type = TextureUtils.measureDataType(GLenum.GL_RGBA8);

        tex_desc.width = terrain_layerdef_map_texture_size;
        tex_desc.height = terrain_layerdef_map_texture_size;
        tex_desc.mipLevels = 1;
        tex_desc.arraySize = 1;
        tex_desc.format = GLenum.GL_RGBA8; // DXGI_FORMAT_R8G8B8A8_UNORM;
//        tex_desc.SampleDesc.Count = 1;
//        tex_desc.SampleDesc.Quality = 0;
//        tex_desc.Usage = D3D11_USAGE_DEFAULT;
//        tex_desc.BindFlags = D3D11_BIND_SHADER_RESOURCE;
//        tex_desc.CPUAccessFlags = 0;
//        tex_desc.MiscFlags = 0;
//        result=pDevice->CreateTexture2D(&tex_desc,&subresource_data,&layerdef_texture);
        layerdef_textureSRV = layerdef_texture = TextureUtils.createTexture2D(tex_desc, subresource_data);

//        ZeroMemory(&textureSRV_desc,sizeof(textureSRV_desc));
//        textureSRV_desc.Format=tex_desc.Format;
//        textureSRV_desc.ViewDimension=D3D11_SRV_DIMENSION_TEXTURE2D;
//        textureSRV_desc.Texture2D.MipLevels=tex_desc.MipLevels;
//        textureSRV_desc.Texture2D.MostDetailedMip=0;
//        pDevice->CreateShaderResourceView(layerdef_texture,&textureSRV_desc,&layerdef_textureSRV);
//
//        free(temp_layerdef_map_texture_pixels);
//        free(layerdef_map_texture_pixels);

        height_linear_array = new float [terrain_gridpoints*terrain_gridpoints*4];
        patches_rawdata = new float [terrain_numpatches_1d*terrain_numpatches_1d*4];

        for(i=0;i<terrain_gridpoints;i++)
            for(j=0; j<terrain_gridpoints;j++)
            {
                height_linear_array[(i+j*terrain_gridpoints)*4+0]=normal[i][j].x;
                height_linear_array[(i+j*terrain_gridpoints)*4+1]=normal[i][j].y;
                height_linear_array[(i+j*terrain_gridpoints)*4+2]=normal[i][j].z;
                height_linear_array[(i+j*terrain_gridpoints)*4+3]=height[i][j];
            }
//        subresource_data.pSysMem = height_linear_array;
//        subresource_data.SysMemPitch = terrain_gridpoints*4*sizeof(float);
//        subresource_data.SysMemSlicePitch = 0;
        subresource_data.data = height_linear_array;
        subresource_data.format = TextureUtils.measureFormat(GLenum.GL_RGBA32F);
        subresource_data.type = TextureUtils.measureDataType(GLenum.GL_RGBA32F);

        tex_desc.width = terrain_gridpoints;
        tex_desc.height = terrain_gridpoints;
        tex_desc.mipLevels = 1;
        tex_desc.arraySize = 1;
        tex_desc.format = GLenum.GL_RGBA32F;
//        tex_desc.SampleDesc.Count = 1;
//        tex_desc.SampleDesc.Quality = 0;
//        tex_desc.Usage = D3D11_USAGE_DEFAULT;
//        tex_desc.BindFlags = D3D11_BIND_SHADER_RESOURCE;
//        tex_desc.CPUAccessFlags = 0;
//        tex_desc.MiscFlags = 0;
//        result=pDevice->CreateTexture2D(&tex_desc,&subresource_data,&heightmap_texture);

//        free(height_linear_array);

//        ZeroMemory(&textureSRV_desc,sizeof(textureSRV_desc));
//        textureSRV_desc.Format=tex_desc.Format;
//        textureSRV_desc.ViewDimension=D3D11_SRV_DIMENSION_TEXTURE2D;
//        textureSRV_desc.Texture2D.MipLevels=tex_desc.MipLevels;
//        pDevice->CreateShaderResourceView(heightmap_texture,&textureSRV_desc,&heightmap_textureSRV);
        heightmap_texture = heightmap_textureSRV = TextureUtils.createTexture2D(tex_desc, subresource_data);

        //building depthmap
        byte[] depth_shadow_map_texture_pixels= new byte[terrain_depth_shadow_map_texture_size*terrain_depth_shadow_map_texture_size*4];
        for(i=0;i<terrain_depth_shadow_map_texture_size;i++)
            for(j=0;j<terrain_depth_shadow_map_texture_size;j++)
            {
                x=(float)(terrain_gridpoints)*((float)i/(float)terrain_depth_shadow_map_texture_size);
                z=(float)(terrain_gridpoints)*((float)j/(float)terrain_depth_shadow_map_texture_size);
                ix=(int)Math.floor(x);
                iz=(int)Math.floor(z);
                rm=bilinear_interpolation(x-ix,z-iz,height[ix][iz],height[ix+1][iz],height[ix+1][iz+1],height[ix][iz+1])*terrain_geometry_scale;

                if(rm>0)
                {
                    depth_shadow_map_texture_pixels[(j*terrain_depth_shadow_map_texture_size+i)*4+0]=0;
                    depth_shadow_map_texture_pixels[(j*terrain_depth_shadow_map_texture_size+i)*4+1]=0;
                    depth_shadow_map_texture_pixels[(j*terrain_depth_shadow_map_texture_size+i)*4+2]=0;
                }
                else
                {
                    float no=(1.0f*255.0f*(rm/(terrain_minheight*terrain_geometry_scale)))-1.0f;
                    if(no>255) no=255;
                    if(no<0) no=0;
                    depth_shadow_map_texture_pixels[(j*terrain_depth_shadow_map_texture_size+i)*4+0]=(byte)no;

                    no=(10.0f*255.0f*(rm/(terrain_minheight*terrain_geometry_scale)))-80.0f;
                    if(no>255) no=255;
                    if(no<0) no=0;
                    depth_shadow_map_texture_pixels[(j*terrain_depth_shadow_map_texture_size+i)*4+1]=(byte)no;

                    no=(100.0f*255.0f*(rm/(terrain_minheight*terrain_geometry_scale)))-300.0f;
                    if(no>255) no=255;
                    if(no<0) no=0;
                    depth_shadow_map_texture_pixels[(j*terrain_depth_shadow_map_texture_size+i)*4+2]=(byte)no;
                }
                depth_shadow_map_texture_pixels[(j*terrain_depth_shadow_map_texture_size+i)*4+3]=0;
            }

        subresource_data.data = depth_shadow_map_texture_pixels;
        subresource_data.format = GLenum.GL_RGBA;
        subresource_data.type = GLenum.GL_UNSIGNED_BYTE;

        tex_desc.width = terrain_depth_shadow_map_texture_size;
        tex_desc.height = terrain_depth_shadow_map_texture_size;
        tex_desc.mipLevels = 1;
        tex_desc.arraySize = 1;
        tex_desc.format = GLenum.GL_RGBA8; // DXGI_FORMAT_R8G8B8A8_UNORM;
//        tex_desc.SampleDesc.Count = 1;
//        tex_desc.SampleDesc.Quality = 0;
//        tex_desc.Usage = D3D11_USAGE_DEFAULT;
//        tex_desc.BindFlags = D3D11_BIND_SHADER_RESOURCE;
//        tex_desc.CPUAccessFlags = 0;
//        tex_desc.MiscFlags = 0;
//        result=pDevice->CreateTexture2D(&tex_desc,&subresource_data,&depthmap_texture);
//
//        ZeroMemory(&textureSRV_desc,sizeof(textureSRV_desc));
//        textureSRV_desc.Format=tex_desc.Format;
//        textureSRV_desc.ViewDimension=D3D11_SRV_DIMENSION_TEXTURE2D;
//        textureSRV_desc.Texture2D.MipLevels=tex_desc.MipLevels;
//        pDevice->CreateShaderResourceView(depthmap_texture,&textureSRV_desc,&depthmap_textureSRV);

        depth_shadow_map_texture_pixels = null;

        // creating terrain vertex buffer
        for(i=0;i<terrain_numpatches_1d;i++)
            for(j=0;j<terrain_numpatches_1d;j++)
            {
                patches_rawdata[(i+j*terrain_numpatches_1d)*4+0]=i*terrain_geometry_scale*terrain_gridpoints/terrain_numpatches_1d;
                patches_rawdata[(i+j*terrain_numpatches_1d)*4+1]=j*terrain_geometry_scale*terrain_gridpoints/terrain_numpatches_1d;
                patches_rawdata[(i+j*terrain_numpatches_1d)*4+2]=terrain_geometry_scale*terrain_gridpoints/terrain_numpatches_1d;
                patches_rawdata[(i+j*terrain_numpatches_1d)*4+3]=terrain_geometry_scale*terrain_gridpoints/terrain_numpatches_1d;
            }

//        D3D11_BUFFER_DESC buf_desc;
//        memset(&buf_desc,0,sizeof(buf_desc));
//
//        buf_desc.ByteWidth = terrain_numpatches_1d*terrain_numpatches_1d*4*sizeof(float);
//        buf_desc.BindFlags = D3D11_BIND_VERTEX_BUFFER;
//        buf_desc.Usage = D3D11_USAGE_DEFAULT;
//
//        subresource_data.pSysMem=patches_rawdata;
//        subresource_data.SysMemPitch=0;
//        subresource_data.SysMemSlicePitch=0;
//
//        result=pDevice->CreateBuffer(&buf_desc,&subresource_data,&heightfield_vertexbuffer);
//        free (patches_rawdata);
        heightfield_vertexbuffer = new BufferGL();
        heightfield_vertexbuffer.initlize(GLenum.GL_ARRAY_BUFFER, terrain_numpatches_1d*terrain_numpatches_1d*4*4, CacheBuffer.wrap(patches_rawdata), GLenum.GL_STATIC_DRAW);
        heightfield_vertexbuffer.unbind();

        // creating sky vertex buffer
        float[] sky_vertexdata;
        int floatnum;
        sky_vertexdata = new float [sky_gridpoints*(sky_gridpoints+2)*2*6];

        for(j=0;j<sky_gridpoints;j++)
        {
            i=0;
            floatnum=(j*(sky_gridpoints+2)*2)*6;
            sky_vertexdata[floatnum+0]= (float) (terrain_gridpoints*terrain_geometry_scale*0.5f+4000.0f*Math.cos(2.0*Math.PI*i/sky_gridpoints)*Math.cos(-0.5f*Math.PI+Math.PI*j/sky_gridpoints));
            sky_vertexdata[floatnum+1]= (float) (4000.0f*Math.sin(-0.5f*Math.PI+Math.PI*j/sky_gridpoints));
            sky_vertexdata[floatnum+2]= (float) (terrain_gridpoints*terrain_geometry_scale*0.5f+4000.0f*Math.sin(2.0f*Math.PI*(float)i/(float)sky_gridpoints)*Math.cos(-0.5f*Math.PI+Math.PI*(float)j/(float)sky_gridpoints));
            sky_vertexdata[floatnum+3]=1;
            sky_vertexdata[floatnum+4]=(sky_texture_angle+(float)i/(float)sky_gridpoints);
            sky_vertexdata[floatnum+5]=2.0f-2.0f*(float)(j+0)/(float)sky_gridpoints;
            floatnum+=6;
            for(i=0;i<sky_gridpoints+1;i++)
            {
                sky_vertexdata[floatnum+0]= (float) (terrain_gridpoints*terrain_geometry_scale*0.5f+4000.0f*Math.cos(2.0f*Math.PI*(float)i/(float)sky_gridpoints)*Math.cos(-0.5f*Math.PI+Math.PI*(float)j/(float)sky_gridpoints));
                sky_vertexdata[floatnum+1]= (float) (4000.0f*Math.sin(-0.5f*Math.PI+Math.PI*(float)(j)/(float)sky_gridpoints));
                sky_vertexdata[floatnum+2]= (float) (terrain_gridpoints*terrain_geometry_scale*0.5f+4000.0f*Math.sin(2.0f*Math.PI*(float)i/(float)sky_gridpoints)*Math.cos(-0.5f*Math.PI+Math.PI*(float)j/(float)sky_gridpoints));
                sky_vertexdata[floatnum+3]=1;
                sky_vertexdata[floatnum+4]=(sky_texture_angle+(float)i/(float)sky_gridpoints);
                sky_vertexdata[floatnum+5]=2.0f-2.0f*(float)(j+0)/(float)sky_gridpoints;
                floatnum+=6;
                sky_vertexdata[floatnum+0]= (float) (terrain_gridpoints*terrain_geometry_scale*0.5f+4000.0f*Math.cos(2.0f*Math.PI*(float)i/(float)sky_gridpoints)*Math.cos(-0.5f*Math.PI+Math.PI*(float)(j+1)/(float)sky_gridpoints));
                sky_vertexdata[floatnum+1]= (float) (4000.0f*Math.sin(-0.5f*Math.PI+Math.PI*(float)(j+1)/(float)sky_gridpoints));
                sky_vertexdata[floatnum+2]= (float) (terrain_gridpoints*terrain_geometry_scale*0.5f+4000.0f*Math.sin(2.0f*Math.PI*(float)i/(float)sky_gridpoints)*Math.cos(-0.5f*Math.PI+Math.PI*(float)(j+1)/(float)sky_gridpoints));
                sky_vertexdata[floatnum+3]=1;
                sky_vertexdata[floatnum+4]=(sky_texture_angle+(float)i/(float)sky_gridpoints);
                sky_vertexdata[floatnum+5]=2.0f-2.0f*(float)(j+1)/(float)sky_gridpoints;
                floatnum+=6;
            }
            i=sky_gridpoints;
            sky_vertexdata[floatnum+0]= (float) (terrain_gridpoints*terrain_geometry_scale*0.5f+4000.0f*Math.cos(2.0f*Math.PI*(float)i/(float)sky_gridpoints)*Math.cos(-0.5f*Math.PI+Math.PI*(float)(j+1)/(float)sky_gridpoints));
            sky_vertexdata[floatnum+1]= (float) (4000.0f*Math.sin(-0.5f*Math.PI+Math.PI*(float)(j+1)/(float)sky_gridpoints));
            sky_vertexdata[floatnum+2]= (float) (terrain_gridpoints*terrain_geometry_scale*0.5f+4000.0f*Math.sin(2.0f*Math.PI*(float)i/(float)sky_gridpoints)*Math.cos(-0.5f*Math.PI+Math.PI*(float)(j+1)/(float)sky_gridpoints));
            sky_vertexdata[floatnum+3]=1;
            sky_vertexdata[floatnum+4]=(sky_texture_angle+(float)i/(float)sky_gridpoints);
            sky_vertexdata[floatnum+5]=2.0f-2.0f*(float)(j+1)/(float)sky_gridpoints;
            floatnum+=6;
        }

//        memset(&buf_desc,0,sizeof(buf_desc));
//
//        buf_desc.ByteWidth = sky_gridpoints*(sky_gridpoints+2)*2*6*sizeof(float);
//        buf_desc.BindFlags = D3D11_BIND_VERTEX_BUFFER;
//        buf_desc.Usage = D3D11_USAGE_DEFAULT;
//
//        subresource_data.pSysMem=sky_vertexdata;
//        subresource_data.SysMemPitch=0;
//        subresource_data.SysMemSlicePitch=0;
//
//        result=pDevice->CreateBuffer(&buf_desc,&subresource_data,&sky_vertexbuffer);
//
//        free (sky_vertexdata);
        sky_vertexbuffer = new BufferGL();
        sky_vertexbuffer.initlize(GLenum.GL_ARRAY_BUFFER, sky_gridpoints*(sky_gridpoints+2)*2*6*4, CacheBuffer.wrap(sky_vertexdata) ,GLenum.GL_STATIC_DRAW);
    }

    float DynamicTesselationFactor;
    void SetupNormalView(Matrix4f matProj, Matrix4f matView ){

    }

    void SetupReflectionView(Matrix4f matProj, Matrix4f matView){

    }

    void SetupRefractionView(Matrix4f matProj, Matrix4f matView ){

    }

    void SetupLightView(Matrix4f matProj, Matrix4f matView){

    }
    float BackbufferWidth;
    float BackbufferHeight;
}
