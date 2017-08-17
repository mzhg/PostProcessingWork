package jet.opengl.demos.nvidia.waves.samples;

import org.lwjgl.util.vector.Vector3f;

import java.util.Random;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureDataDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;
import jet.opengl.postprocessing.util.DebugTools;

/**
 * Created by mazhen'gui on 2017/8/2.
 */

public class CTerrainGenerator implements Disposeable{

    private int heightfield_vertexbuffer;
    private int m_terrian_grid_count;
    private final Random m_Random = new Random(123);
    private TerrainParams m_TerrainParams;

    private Texture2D m_layerdef_texture;
    private Texture2D m_heightmap_texture;
    private Texture2D m_depthmap_texture;

    private GLFuncProvider gl;

    public TerrainParams getTerrainParams() { return m_TerrainParams;}
    public void setTerrainParams(TerrainParams params) { m_TerrainParams.set(params);}
    public Texture2D getLayerdefTexture() { return m_layerdef_texture;}
    public Texture2D getHeightmapTexture() { return m_heightmap_texture;}
    public Texture2D getDepthmapTexture() { return m_depthmap_texture;}

    public void initlize(){
        int i,j,k,l;
        float x,z;
        int ix,iz;
        float[] backterrain;
//	    D3DXVECTOR3 vec1,vec2,vec3;
        final Vector3f vec1 = new Vector3f();
        final Vector3f vec2 = new Vector3f();
        final Vector3f vec3 = new Vector3f();
        if(m_TerrainParams == null)
            m_TerrainParams = new TerrainParams();
        int currentstep=m_TerrainParams.terrain_gridpoints;
        float mv,rm;
        float yscale=0,maxheight=0,minheight=0;

        int terrain_gridpoints = m_TerrainParams.terrain_gridpoints;
        int terrain_numpatches_1d = m_TerrainParams.terrain_numpatches_1d;
        float terrain_geometry_scale = m_TerrainParams.terrain_geometry_scale;
        float terrain_maxheight = m_TerrainParams.terrain_maxheight;
        float terrain_minheight = m_TerrainParams.terrain_minheight;
        float terrain_fractalfactor = m_TerrainParams.terrain_fractalfactor;
        float terrain_fractalinitialvalue = m_TerrainParams.terrain_fractalinitialvalue;
        float terrain_smoothfactor1 = m_TerrainParams.terrain_smoothfactor1;
        float terrain_smoothfactor2 = m_TerrainParams.terrain_smoothfactor2;
        float terrain_rockfactor = m_TerrainParams.terrain_rockfactor;
        int terrain_smoothsteps = m_TerrainParams.terrain_smoothsteps;
        float terrain_height_underwater_start = m_TerrainParams.terrain_height_underwater_start;
        float terrain_height_underwater_end = m_TerrainParams.terrain_height_underwater_end;
        float terrain_height_sand_start = m_TerrainParams.terrain_height_sand_start;
        float terrain_height_sand_end = m_TerrainParams.terrain_height_sand_end;
        float terrain_height_grass_start = m_TerrainParams.terrain_height_grass_start;
        float terrain_height_grass_end = m_TerrainParams.terrain_height_grass_end;
        float terrain_height_rocks_start = m_TerrainParams.terrain_height_rocks_start;
        float terrain_height_trees_start = m_TerrainParams.terrain_height_trees_start;
        float terrain_height_trees_end = m_TerrainParams.terrain_height_trees_end;
        float terrain_slope_grass_start = m_TerrainParams.terrain_slope_grass_start;
        float terrain_slope_rocks_start = m_TerrainParams.terrain_slope_rocks_start;
        int terrain_layerdef_map_texture_size = m_TerrainParams.terrain_layerdef_map_texture_size;
        int terrain_depth_shadow_map_texture_size = m_TerrainParams.terrain_depth_shadow_map_texture_size;
        float terrain_far_range = terrain_gridpoints*terrain_geometry_scale;
        final Random random = m_Random;
        random.setSeed(m_TerrainParams.random_seed);

        float []height_linear_array;
        float []patches_rawdata;

        float[][]			height = new float[terrain_gridpoints+1][terrain_gridpoints+1];
        float[][]           normal = new float[terrain_gridpoints+1][(terrain_gridpoints+1) * 3];
        dispose();

//		HRESULT result;
//		D3D11_SUBRESOURCE_DATA subresource_data;
//		D3D11_TEXTURE2D_DESC tex_desc;
//		D3D11_SHADER_RESOURCE_VIEW_DESC textureSRV_desc;

//		backterrain = (float *) malloc((terrain_gridpoints+1)*(terrain_gridpoints+1)*sizeof(float));
        backterrain = new float[(terrain_gridpoints+1)*(terrain_gridpoints+1)];
        rm=terrain_fractalinitialvalue;
        backterrain[0]=0;
        backterrain[0+terrain_gridpoints*terrain_gridpoints]=0;
        backterrain[terrain_gridpoints]=0;
        backterrain[terrain_gridpoints+terrain_gridpoints*terrain_gridpoints]=0;
        currentstep=terrain_gridpoints;
//		srand(12);

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
                    backterrain[i+currentstep/2+terrain_gridpoints*(j+currentstep/2)]=(float)(mv+rm*(/*(rand()%1000)/1000.0f*/ random.nextFloat() -0.5f));
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
                    mv+=backterrain[i+currentstep/2+terrain_gridpoints*gp_wrap(j-currentstep/2,terrain_gridpoints)];
                    mv/=4;
                    backterrain[i+currentstep/2+terrain_gridpoints*j]=(float)(mv+rm*(/*(rand()%1000)/1000.0f*/random.nextFloat()-0.5f));

                    mv=0;
                    mv=backterrain[i+terrain_gridpoints*j];
                    mv+=backterrain[i+terrain_gridpoints*(j+currentstep)];
                    mv+=backterrain[(i+currentstep/2)+terrain_gridpoints*(j+currentstep/2)];
                    mv+=backterrain[gp_wrap(i-currentstep/2,terrain_gridpoints)+terrain_gridpoints*(j+currentstep/2)];
                    mv/=4;
                    backterrain[i+terrain_gridpoints*(j+currentstep/2)]=(float)(mv+rm*(/*(rand()%1000)/1000.0f*/random.nextFloat()-0.5f));

                    mv=0;
                    mv=backterrain[i+currentstep+terrain_gridpoints*j];
                    mv+=backterrain[i+currentstep+terrain_gridpoints*(j+currentstep)];
                    mv+=backterrain[(i+currentstep/2)+terrain_gridpoints*(j+currentstep/2)];
                    mv+=backterrain[gp_wrap(i+currentstep/2+currentstep,terrain_gridpoints)+terrain_gridpoints*(j+currentstep/2)];
                    mv/=4;
                    backterrain[i+currentstep+terrain_gridpoints*(j+currentstep/2)]=(float)(mv+rm*(/*(rand()%1000)/1000.0f*/random.nextFloat()-0.5f));

                    mv=0;
                    mv=backterrain[i+currentstep+terrain_gridpoints*(j+currentstep)];
                    mv+=backterrain[i+terrain_gridpoints*(j+currentstep)];
                    mv+=backterrain[(i+currentstep/2)+terrain_gridpoints*(j+currentstep/2)];
                    mv+=backterrain[i+currentstep/2+terrain_gridpoints*gp_wrap(j+currentstep/2+currentstep,terrain_gridpoints)];
                    mv/=4;
                    backterrain[i+currentstep/2+terrain_gridpoints*(j+currentstep)]=(float)(mv+rm*(/*(rand()%1000)/1000.0f*/random.nextFloat()-0.5f));
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
//		offset=minheight-terrain_minheight;
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
                rm=((terrain_gridpoints*0.8f)*(terrain_gridpoints*0.8f)/4.0f);
                if(mv>rm)
                {
                    height[i][j]-=((mv-rm)/1000.0f)*terrain_geometry_scale;
                }
                if(height[i][j]<terrain_minheight)
                {
                    height[i][j]=terrain_minheight;
                }
            }


        // terrain banks
        for(k=0;k<10;k++)
        {
            for(i=0;i<terrain_gridpoints+1;i++)
                for(j=0;j<terrain_gridpoints+1;j++)
                {
                    mv=height[i][j];
                    if((mv)>0.02f)
                    {
                        mv-=0.02f;
                    }
                    if(mv<-0.02f)
                    {
                        mv+=0.02f;
                    }
                    height[i][j]=mv;
                }
        }

        // smoothing
        for(k=0;k<terrain_smoothsteps;k++)
        {
            for(i=0;i<terrain_gridpoints+1;i++)
                for(j=0;j<terrain_gridpoints+1;j++)
                {

                    vec1.x=2*terrain_geometry_scale;
                    vec1.y=terrain_geometry_scale*(height[gp_wrap(i+1,terrain_gridpoints)][j]-height[gp_wrap(i-1,terrain_gridpoints)][j]);
                    vec1.z=0;
                    vec2.x=0;
                    vec2.y=-terrain_geometry_scale*(height[i][gp_wrap(j+1,terrain_gridpoints)]-height[i][gp_wrap(j-1,terrain_gridpoints)]);
                    vec2.z=-2*terrain_geometry_scale;

//					D3DXVec3Cross(&vec3,&vec1,&vec2);
//					D3DXVec3Normalize(&vec3,&vec3);

                    Vector3f.cross(vec1, vec2, vec3);
                    vec3.normalise();

//					vec3.x = terrain_geometry_scale*(height[gp_wrap(i+1)][j]-height[gp_wrap(i-1)][j]);
//					vec3.y = 2;
//					vec3.z=terrain_geometry_scale*(height[i][gp_wrap(j+1)]-height[i][gp_wrap(j-1)]);
//					vec3.normalise();

                    if(((vec3.y>terrain_rockfactor)||(height[i][j]<1.2f)))
                    {
                        rm=terrain_smoothfactor1;
                        mv=height[i][j]*(1.0f-rm) +rm*0.25f*(height[gp_wrap(i-1,terrain_gridpoints)][j]+height[i][gp_wrap(j-1,terrain_gridpoints)]+height[gp_wrap(i+1,terrain_gridpoints)][j]+height[i][gp_wrap(j+1,terrain_gridpoints)]);
                        backterrain[i+terrain_gridpoints*j]=mv;
                    }
                    else
                    {
                        rm=terrain_smoothfactor2;
                        mv=height[i][j]*(1.0f-rm) +rm*0.25f*(height[gp_wrap(i-1,terrain_gridpoints)][j]+height[i][gp_wrap(j-1,terrain_gridpoints)]+height[gp_wrap(i+1,terrain_gridpoints)][j]+height[i][gp_wrap(j+1,terrain_gridpoints)]);
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
                mv=height[i][j]*(1.0f-rm) +rm*0.25f*(height[gp_wrap(i-1,terrain_gridpoints)][j]+height[i][gp_wrap(j-1,terrain_gridpoints)]+height[gp_wrap(i+1,terrain_gridpoints)][j]+height[i][gp_wrap(j+1,terrain_gridpoints)]);
                backterrain[i+terrain_gridpoints*j]=mv;
            }
        for (i=0;i<terrain_gridpoints+1;i++)
            for (j=0;j<terrain_gridpoints+1;j++)
            {
                height[i][j]=(backterrain[i+terrain_gridpoints*j]);
            }


//		free(backterrain);
        backterrain = null;

        //calculating normals
        for (i=0;i<terrain_gridpoints+1;i++)
            for (j=0;j<terrain_gridpoints+1;j++)
            {
                vec1.x=2*terrain_geometry_scale;
                vec1.y=terrain_geometry_scale*(height[gp_wrap(i+1,terrain_gridpoints)][j]-height[gp_wrap(i-1,terrain_gridpoints)][j]);
                vec1.z=0;
                vec2.x=0;
                vec2.y=-terrain_geometry_scale*(height[i][gp_wrap(j+1,terrain_gridpoints)]-height[i][gp_wrap(j-1,terrain_gridpoints)]);
                vec2.z=-2*terrain_geometry_scale;
//				D3DXVec3Cross(&normal[i][j],&vec1,&vec2);
//				D3DXVec3Normalize(&normal[i][j],&normal[i][j]);

                Vector3f.cross(vec1, vec2, vec3);

//				vec3.x = terrain_geometry_scale*(height[gp_wrap(i+1)][j]-height[gp_wrap(i-1)][j]);
//				vec3.y = 2 * terrain_geometry_scale;
//				vec3.z=terrain_geometry_scale*(height[i][gp_wrap(j+1)]-height[i][gp_wrap(j-1)]);
                vec3.normalise();
                vec3.store(normal[i], j * 3);
            }


        // buiding layerdef
//		byte* temp_layerdef_map_texture_pixels=(byte *)malloc(terrain_layerdef_map_texture_size*terrain_layerdef_map_texture_size*4);
//		byte* layerdef_map_texture_pixels=(byte *)malloc(terrain_layerdef_map_texture_size*terrain_layerdef_map_texture_size*4);
        byte[] temp_layerdef_map_texture_pixels = new byte[terrain_layerdef_map_texture_size*terrain_layerdef_map_texture_size*4];
        byte[] layerdef_map_texture_pixels = new byte[terrain_layerdef_map_texture_size*terrain_layerdef_map_texture_size*4];

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
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+0]=-1;
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+1]=0;
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+2]=0;
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+3]=0;
                }

                if((rm>terrain_height_sand_start)&&(rm<=terrain_height_sand_end))
                {
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+0]=0;
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+1]=-1;
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+2]=0;
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+3]=0;
                }

                if((rm>terrain_height_grass_start)&&(rm<=terrain_height_grass_end))
                {
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+0]=0;
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+1]=0;
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+2]=-1;
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+3]=0;
                }

//				mv=bilinear_interpolation(x-ix,z-iz,normal[ix][iz][1],normal[ix+1][iz][1],normal[ix+1][iz+1][1],normal[ix][iz+1][1]);
                mv=bilinear_interpolation(x-ix,z-iz,normal[ix][iz*3+1],normal[ix+1][iz*3+1],normal[ix+1][(iz+1)*3+1],normal[ix][(iz+1)*3+1]);

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
                    temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+3]=-1;
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

        Texture2DDesc tex_desc = new Texture2DDesc();
        tex_desc.width = terrain_layerdef_map_texture_size;
        tex_desc.height = terrain_layerdef_map_texture_size;
        tex_desc.format = GLenum.GL_RGBA8;
        TextureDataDesc initData=new TextureDataDesc();
        initData.data = layerdef_map_texture_pixels;
        initData.format=GLenum.GL_RGBA;
        initData.type=GLenum.GL_UNSIGNED_BYTE;

        m_layerdef_texture = TextureUtils.createTexture2D(tex_desc, initData);

        temp_layerdef_map_texture_pixels = null;
        layerdef_map_texture_pixels = null;

        height_linear_array = new float [terrain_gridpoints*terrain_gridpoints*4];
        patches_rawdata = new float [terrain_numpatches_1d*terrain_numpatches_1d*4];

        for(i=0;i<terrain_gridpoints;i++)
            for(j=0; j<terrain_gridpoints;j++)
            {
                height_linear_array[(i+j*terrain_gridpoints)*4+0]=normal[i][j*3+0];
                height_linear_array[(i+j*terrain_gridpoints)*4+1]=normal[i][j*3+1];
                height_linear_array[(i+j*terrain_gridpoints)*4+2]=normal[i][j*3+2];
                height_linear_array[(i+j*terrain_gridpoints)*4+3]=height[i][j];
            }

        tex_desc.width = terrain_gridpoints;
        tex_desc.height = terrain_gridpoints;
        tex_desc.format = GLenum.GL_RGBA32F;
//		tex_desc.pixels = GLUtil.wrapToBytes(height_linear_array);
        initData.data=height_linear_array;
        initData.format=GLenum.GL_RGBA;
        initData.type=GLenum.GL_FLOAT;

        m_heightmap_texture = TextureUtils.createTexture2D(tex_desc, initData);
        height_linear_array = null;

        //building depthmap
//		byte * depth_shadow_map_texture_pixels=(byte *)malloc(terrain_depth_shadow_map_texture_size*terrain_depth_shadow_map_texture_size*4);
        byte[] depth_shadow_map_texture_pixels = new byte[terrain_depth_shadow_map_texture_size*terrain_depth_shadow_map_texture_size*4];
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

                    no=(10.0f*255.0f*(rm/(terrain_minheight*terrain_geometry_scale)))-40.0f;
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

        tex_desc.width = terrain_depth_shadow_map_texture_size;
        tex_desc.height = terrain_depth_shadow_map_texture_size;
        tex_desc.format = GLenum.GL_RGBA8;
//		tex_desc.pixels = GLUtil.wrap(depth_shadow_map_texture_pixels);
        initData.data = depth_shadow_map_texture_pixels;
        initData.format=GLenum.GL_RGBA;
        initData.type=GLenum.GL_UNSIGNED_BYTE;

        m_depthmap_texture = TextureUtils.createTexture2D(tex_desc, initData);
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

        if(gl == null)
            gl = GLFuncProviderFactory.getGLFuncProvider();

        heightfield_vertexbuffer = gl.glGenBuffer();
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, heightfield_vertexbuffer);
        gl.glBufferData(GLenum.GL_ARRAY_BUFFER, CacheBuffer.wrap(patches_rawdata), GLenum.GL_STATIC_DRAW);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);

        m_terrian_grid_count = terrain_numpatches_1d*terrain_numpatches_1d;
    }

    /**
     * Binding the buffer of the heightmap and draw the terrains' grid points.
     * @param posAttribLocation The location of the position attributes in the vertex shader.
     * @param cullBackFace True indicates cull the back face of CCW, false cull the front face of CW.
     */
    public void draw(int posAttribLocation, boolean cullBackFace){
        gl.glBindVertexArray(0);   // no  vao
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);  // no indices.
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, heightfield_vertexbuffer);
        gl.glVertexAttribPointer(posAttribLocation, 4, GLenum.GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(posAttribLocation);
        if(cullBackFace){
            gl.glFrontFace(GLenum.GL_CCW);
            gl.glCullFace(GLenum.GL_BACK);
        }else{
            gl.glFrontFace(GLenum.GL_CW);
            gl.glCullFace(GLenum.GL_FRONT);
        }
        gl.glEnable(GLenum.GL_CULL_FACE);
        gl.glPatchParameteri(GLenum.GL_PATCH_VERTICES, 1);
        gl.glDrawArrays(GLenum.GL_PATCHES, 0, m_terrian_grid_count);

        // reset the state.
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
        gl.glDisableVertexAttribArray(posAttribLocation);

        gl.glDisable(GLenum.GL_CULL_FACE);
        if(!cullBackFace)
            gl.glFrontFace(GLenum.GL_CCW);
    }

    public static void main(String[] args){
        DebugTools.genStructCopy(TerrainParams.class);
    }

    static float sin(float angle) { return (float)Math.sin(angle);}
    static float cos(float angle) { return (float)Math.cos(angle);}

    private int gp_wrap( int a, int terrain_gridpoints)
    {
        if(a<0) return (a+terrain_gridpoints);
        if(a>=terrain_gridpoints) return (a-terrain_gridpoints);
        return a;
    }

    private static float bilinear_interpolation(float fx, float fy, float a, float b, float c, float d)
    {
        float s1,s2,s3,s4;
        s1=fx*fy;
        s2=(1-fx)*fy;
        s3=(1-fx)*(1-fy);
        s4=fx*(1-fy);
        return((a*s3+b*s4+c*s1+d*s2));
    }

    @Override
    public void dispose() {
        if(heightfield_vertexbuffer != 0)
            gl.glDeleteBuffer(heightfield_vertexbuffer);
        CommonUtil.safeRelease(m_layerdef_texture);
        CommonUtil.safeRelease(m_heightmap_texture);
        CommonUtil.safeRelease(m_depthmap_texture);
    }

    public static class TerrainParams {
        public int terrain_gridpoints = 512;
        public int terrain_numpatches_1d = 64;
        public float terrain_geometry_scale = 1.0f;
        public float terrain_maxheight = 30.0f;
        public float terrain_minheight = -30.0f;
        public float terrain_fractalfactor = 0.68f;;
        public float terrain_fractalinitialvalue = 100.0f;
        public float terrain_smoothfactor1 = 0.99f;
        public float terrain_smoothfactor2 = 0.10f;
        public float terrain_rockfactor = 0.95f;
        public int   terrain_smoothsteps = 40;
        public float terrain_height_underwater_start = -100.0f;
        public float terrain_height_underwater_end = -8.0f;
        public float terrain_height_sand_start = -30.0f;
        public float terrain_height_sand_end = 1.7f;
        public float terrain_height_grass_start = 1.7f;
        public float terrain_height_grass_end = 30.0f;
        public float terrain_height_rocks_start = -2.0f;
        public float terrain_height_trees_start = 4.0f;
        public float terrain_height_trees_end = 30.0f;
        public float terrain_slope_grass_start = 0.96f;
        public float terrain_slope_rocks_start = 0.85f;
//        public float terrain_far_range = terrain_gridpoints*terrain_geometry_scale;

//        public int shadowmap_resource_buffer_size_xy = 2048;
//        public int water_normalmap_resource_buffer_size_xy = 2048;
        public int terrain_layerdef_map_texture_size = 1024;
        public int terrain_depth_shadow_map_texture_size = 512;
        public long random_seed = 123;
//        public int sky_gridpoints = 10;
//        public float sky_texture_angle = 0.425f;
//        public float main_buffer_size_multiplier = 1.1f;
//        public float reflection_buffer_size_multiplier = 1.1f;
//        public float refraction_buffer_size_multiplier = 1.1f;
//        public float scene_z_near = 1.0f;
//        public float scene_z_far = 25000.0f;
//        public float camera_fov = 110.0f;

        public TerrainParams() {}

        public TerrainParams(TerrainParams o) {
            set(o);
        }

        public void set(TerrainParams o){
            terrain_gridpoints = o.terrain_gridpoints;
            terrain_numpatches_1d = o.terrain_numpatches_1d;
            terrain_geometry_scale = o.terrain_geometry_scale;
            terrain_maxheight = o.terrain_maxheight;
            terrain_minheight = o.terrain_minheight;
            terrain_fractalfactor = o.terrain_fractalfactor;
            terrain_fractalinitialvalue = o.terrain_fractalinitialvalue;
            terrain_smoothfactor1 = o.terrain_smoothfactor1;
            terrain_smoothfactor2 = o.terrain_smoothfactor2;
            terrain_rockfactor = o.terrain_rockfactor;
            terrain_smoothsteps = o.terrain_smoothsteps;
            terrain_height_underwater_start = o.terrain_height_underwater_start;
            terrain_height_underwater_end = o.terrain_height_underwater_end;
            terrain_height_sand_start = o.terrain_height_sand_start;
            terrain_height_sand_end = o.terrain_height_sand_end;
            terrain_height_grass_start = o.terrain_height_grass_start;
            terrain_height_grass_end = o.terrain_height_grass_end;
            terrain_height_rocks_start = o.terrain_height_rocks_start;
            terrain_height_trees_start = o.terrain_height_trees_start;
            terrain_height_trees_end = o.terrain_height_trees_end;
            terrain_slope_grass_start = o.terrain_slope_grass_start;
            terrain_slope_rocks_start = o.terrain_slope_rocks_start;
            terrain_layerdef_map_texture_size = o.terrain_layerdef_map_texture_size;
            terrain_depth_shadow_map_texture_size = o.terrain_depth_shadow_map_texture_size;
            random_seed = o.random_seed;
        }
    }
}
