package jet.opengl.demos.nvidia.lightning;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.util.FileUtils;
import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by mazhen'gui on 2017/8/30.
 */

final class Arena {
    final ArenaSettings settings = new ArenaSettings();

    private int m_back_buffer_sample_desc;

    private LightningRenderer	m_lightning_renderer;

    private PathLightning		m_inter_coil_lightning;
    private PathLightning		m_fence_lightning;
    private PathLightning		m_coil_helix_lightning;

    private ChainLightning		m_chain_lightning;


    private final LightningAppearance m_red_beam = new LightningAppearance();
    private final LightningAppearance m_blue_beam = new LightningAppearance();
    private final LightningAppearance m_blue_cyan_beam = new LightningAppearance();
    private final Matrix4f m_world = new Matrix4f();

    private Scene			m_scene;

    private float m_time;

    Arena(int back_buffer_sample_desc){
        m_lightning_renderer = new LightningRenderer(back_buffer_sample_desc);
        m_back_buffer_sample_desc = back_buffer_sample_desc;

        m_scene = new Scene();
        CreateLightning();
    }

    void Matrices(Matrix4f view, Matrix4f projection){
        m_scene.Matrices(m_world, view, projection);
        m_lightning_renderer.SetMatrices(m_world, view, projection);
    }
    void Time(float time, float delta_time){
        m_time = time;
        m_scene.Time(time);
        m_lightning_renderer.SetTime(time);
    }

    void RenderTargetResize(int width, int height, Texture2D render_target_view, Texture2D depth_stencil_view){
        m_lightning_renderer.OnRenderTargetResize(width, height, render_target_view, depth_stencil_view);
    }

    void Render(){
        if(settings.Scene)
            m_scene.Render();

        boolean do_lightning = settings.Fence ||settings.InterCoil || settings.CoilHelix|| settings.Chain;
        do_lightning = false;

        if(do_lightning)
        {
            m_lightning_renderer.Begin();

            if(settings.Fence)
                m_lightning_renderer.Render(m_fence_lightning,m_red_beam,1.0f,settings.AnimationSpeed, settings.Lines);

            if(settings.InterCoil)
                m_lightning_renderer.Render(m_inter_coil_lightning,m_blue_beam,1.0f,settings.AnimationSpeed, settings.Lines);

            if(settings.CoilHelix)
                m_lightning_renderer.Render(m_coil_helix_lightning,m_blue_cyan_beam,1.0f,settings.AnimationSpeed, settings.Lines);

            m_chain_lightning.Properties.ChainSource.set(0,25,31);
            m_chain_lightning.Properties.NumTargets = 4;

            for( int i = 0; i < m_chain_lightning.Properties.NumTargets; ++i)
                m_chain_lightning.Properties.ChainTargetPositions[i] = m_scene.TargetPosition(i);

            if(settings.Chain)
                m_lightning_renderer.Render(m_chain_lightning, settings.Beam,1.0f,settings.AnimationSpeed, settings.Lines);

            m_lightning_renderer.End(settings.Glow, settings.BlurSigma);
        }
    }

    private static final class SeedRecord
    {
        String Name;

        final ArrayList<Vector3f> Vertices = new ArrayList<>();
        final ArrayList<Vector3f> InterpolatedVertices = new ArrayList<>();
        boolean Closed = false;
    }

    private static SeedRecord get(Map<String, SeedRecord > seeds, String name){
        SeedRecord record = seeds.get(name);
        if(record == null){
            record = new SeedRecord();
            seeds.put(name, record);
        }

        return record;
    }

    private void ReadSeeds(String filename){
        try(BufferedReader file = new BufferedReader(new InputStreamReader(FileUtils.open(filename)))){
            Vector3f up = new Vector3f(0,1,0);

            final Map<String, SeedRecord > seeds = new HashMap<>();
            String cur_seed = "";
            String line_buffer;
            while((line_buffer = file.readLine()) != null)
            {
//                char line_buffer[512] = {0};
//                file.getline(line_buffer,512);

                String cur_command;
//                std::stringstream line;
//                line << line_buffer;
//                line >> cur_command;
                StringTokenizer line = new StringTokenizer(line_buffer);
                cur_command = line.nextToken();

                if ("*NODE_NAME".equals(cur_command))
                {
//                    std::string name;
//                    line >> name;
//                    name.erase(name.size()-1,1);
//                    name.erase(0,1);
                    String name = line.nextToken();
                    name = name.substring(1, name.length() - 1);

                    cur_seed = name;
//                    seeds[cur_seed].Name = name;
                    get(seeds, cur_seed).Name = name;

                }
                else if("*SHAPE_CLOSED".equals(cur_command))
                {
//                    seeds[cur_seed].Closed = true;
                    get(seeds, cur_seed).Closed = true;
                }
                else if ("*SHAPE_VERTEX_KNOT".equals(cur_command) )
                {
                    Vector3f vertex = new Vector3f();

//                    int dummy;
//                    line >> dummy >> vertex.x >> vertex.z >> vertex.y;
//                    seeds[cur_seed].Vertices.push_back(vertex);
//                    seeds[cur_seed].InterpolatedVertices.push_back(vertex);
                    line.nextToken();
                    vertex.x = Float.parseFloat(line.nextToken());
                    vertex.y = Float.parseFloat(line.nextToken());
                    vertex.z = Float.parseFloat(line.nextToken());

                    SeedRecord seedRecord = get(seeds, cur_seed);
                    seedRecord.Vertices.add(vertex);
                    seedRecord.InterpolatedVertices.add(vertex);
                }
                else if ("*SHAPE_VERTEX_INTERP".equals(cur_command) )
                {
                    Vector3f vertex = new Vector3f();

//                    int dummy;
//                    line >> dummy >> vertex.x >> vertex.z >> vertex.y;
//                    seeds[cur_seed].InterpolatedVertices.push_back(vertex);
                    line.nextToken();
                    vertex.x = Float.parseFloat(line.nextToken());
                    vertex.y = Float.parseFloat(line.nextToken());
                    vertex.z = Float.parseFloat(line.nextToken());

                    SeedRecord seedRecord = get(seeds, cur_seed);
                    seedRecord.InterpolatedVertices.add(vertex);
                }
            }

//            ArrayList<LightningPathSegment> the_seeds =new ArrayList();
            ArrayList<LightningPathSegment> fence_seeds =new ArrayList();
            ArrayList<LightningPathSegment> inter_coil_seeds =new ArrayList();
            ArrayList<LightningPathSegment> coil_helix_seeds =new ArrayList();

//            for(std::map<std::string, SeedRecord>::iterator it = seeds.begin(); it != seeds.end(); ++it)
            for(Map.Entry<String, SeedRecord > it : seeds.entrySet())
            {
                ArrayList<LightningPathSegment> the_seeds = null;
                ArrayList<Vector3f> _seeds = /*&(it->second.Vertices)*/ it.getValue().Vertices;

                if(it.getKey().contains("CoilConnector") /*!= std::string::npos*/ && it.getValue().InterpolatedVertices.size() > 1)
                {
                    _seeds = (it.getValue().InterpolatedVertices);
                    the_seeds = inter_coil_seeds;
                }
                else if(it.getKey().contains("Fence") /*!= std::string::npos*/)
                {
                    the_seeds = fence_seeds;
                }
                else if(it.getKey().contains("TeslaCoilHelix") /*!= std::string::npos*/)
                {
                    the_seeds = coil_helix_seeds;
                }
                else
                {
                    continue;
                }

                // We duplicate the seed lines for the beams between the tesla coils in order to make the beam thicker

                int replicates = 1;
                float jitter = 0.0f;

                if(it.getKey().contains("CoilConnector") /*!= std::string::npos*/)
                {
                    jitter = 1.0f;
                    replicates  = 10;
                }

                for(int j = 0; j < replicates; ++j)
                {
                    if(_seeds.size() > 1)
                    {
                        for(int i = 0; i <_seeds.size()-1; ++i)
                        {
                            the_seeds.add
                                    (
                                            new LightningPathSegment
                                                    (
                                                            _seeds.get(i) ,
                                                            _seeds.get(i+1),
                                    up
                            )
                            );
                        }

                        if(it.getValue().Closed)
                        {
                            the_seeds.add
                                    (
                                            new LightningPathSegment
                                                    (
                                                            _seeds.get(_seeds.size()-1),
                                                            _seeds.get(0) ,
                                    up
                            )
                            );
                        }
                    }
                }

                for(int i = 0; i < the_seeds.size(); ++i)
                {
//                    the_seeds.get(i).Start += jitter * D3DXVECTOR3(Utility::Random(-1,1),Utility::Random(-1,1),Utility::Random(-1,1));
//                    the_seeds.get(i).End += jitter * D3DXVECTOR3(Utility::Random(-1,1),Utility::Random(-1,1),Utility::Random(-1,1));
                    Vector3f start = the_seeds.get(i).Start;
                    start.x += jitter * Numeric.random(-1.f, 1.f);
                    start.y += jitter * Numeric.random(-1.f, 1.f);
                    start.z += jitter * Numeric.random(-1.f, 1.f);

                    Vector3f end = the_seeds.get(i).End;
                    end.x += jitter * Numeric.random(-1.f, 1.f);
                    end.y += jitter * Numeric.random(-1.f, 1.f);
                    end.z += jitter * Numeric.random(-1.f, 1.f);
                }
            }

            m_fence_lightning =   m_lightning_renderer.CreatePathLightning(fence_seeds,0x00,5);
            m_inter_coil_lightning =  m_lightning_renderer.CreatePathLightning(inter_coil_seeds,0x08,5);
            m_coil_helix_lightning =  m_lightning_renderer.CreatePathLightning(coil_helix_seeds,0x03,5);

            m_chain_lightning = m_lightning_renderer.CreateChainLightning(0x0C,5);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void CreateLightning(){
        LightningStructure inter_coil_structure = new LightningStructure();
        {

            inter_coil_structure.ZigZagFraction.set(0.45f, 0.55f);

            inter_coil_structure.ZigZagDeviationRight.set(-5.0f,5.0f);
            inter_coil_structure.ZigZagDeviationUp.set(-5.0f,5.0f);

            inter_coil_structure.ZigZagDeviationDecay = 0.5f;


            inter_coil_structure.ForkFraction.set(0.45f, 0.55f);

            inter_coil_structure.ForkZigZagDeviationRight.set(-1.0f,1.0f);
            inter_coil_structure.ForkZigZagDeviationUp.set(-1.0f,1.0f);
            inter_coil_structure.ForkZigZagDeviationDecay = 0.5f;


            inter_coil_structure.ForkDeviationRight.set(-1.0f,1.0f);
            inter_coil_structure.ForkDeviationUp.set(-1.0f,1.0f);
            inter_coil_structure.ForkDeviationForward.set(0.0f,1.0f);
            inter_coil_structure.ForkDeviationDecay = 0.5f;

            inter_coil_structure.ForkLength.set(1.0f,2.0f);
            inter_coil_structure.ForkLengthDecay = 0.01f;
        }

        LightningStructure fence_structure = new LightningStructure();
        {

            fence_structure.ZigZagFraction.set(0.45f, 0.55f);

            fence_structure.ZigZagDeviationRight.set(-1.0f,1.0f);
            fence_structure.ZigZagDeviationUp.set(-1.0f,1.0f);

            fence_structure.ZigZagDeviationDecay = 0.5f;


            fence_structure.ForkFraction.set(0.45f, 0.55f);

            fence_structure.ForkZigZagDeviationRight.set(-1.0f,1.0f);
            fence_structure.ForkZigZagDeviationUp.set(-1.0f,1.0f);
            fence_structure.ForkZigZagDeviationDecay = 0.5f;


            fence_structure.ForkDeviationRight.set(-1.0f,1.0f);
            fence_structure.ForkDeviationUp.set(-1.0f,1.0f);
            fence_structure.ForkDeviationForward.set(-1.0f,1.0f);
            fence_structure.ForkDeviationDecay = 0.5f;

            fence_structure.ForkLength.set(1.0f,2.0f);
            fence_structure.ForkLengthDecay = 0.01f;
        }
        LightningStructure coil_helix_structure = new LightningStructure();
        {

            coil_helix_structure.ZigZagFraction.set(0.45f, 0.55f);

            coil_helix_structure.ZigZagDeviationRight.set(-5.0f,5.0f);
            coil_helix_structure.ZigZagDeviationUp.set(-5.0f,5.0f);

            coil_helix_structure.ZigZagDeviationDecay = 0.5f;


            coil_helix_structure.ForkFraction.set(0.45f, 0.55f);

            coil_helix_structure.ForkZigZagDeviationRight.set(-1.0f,1.0f);
            coil_helix_structure.ForkZigZagDeviationUp.set(-1.0f,1.0f);
            coil_helix_structure.ForkZigZagDeviationDecay = 0.5f;


            coil_helix_structure.ForkDeviationRight.set(-1.0f,1.0f);
            coil_helix_structure.ForkDeviationUp.set(-1.0f,1.0f);
            coil_helix_structure.ForkDeviationForward.set(0.0f,1.0f);
            coil_helix_structure.ForkDeviationDecay = 0.5f;

            coil_helix_structure.ForkLength.set(1.0f,2.0f);
            coil_helix_structure.ForkLengthDecay = 0.01f;
        }

        LightningStructure chain_structure = new LightningStructure();
        {

            chain_structure.ZigZagFraction.set(0.45f, 0.55f);

            chain_structure.ZigZagDeviationRight.set(-5.0f,5.0f);
            chain_structure.ZigZagDeviationUp.set(-5.0f,5.0f);

            chain_structure.ZigZagDeviationDecay = 0.5f;


            chain_structure.ForkFraction.set(0.45f, 0.55f);

            chain_structure.ForkZigZagDeviationRight.set(-1.0f,1.0f);
            chain_structure.ForkZigZagDeviationUp.set(-1.0f,1.0f);
            chain_structure.ForkZigZagDeviationDecay = 0.5f;


            chain_structure.ForkDeviationRight.set(-1.0f,1.0f);
            chain_structure.ForkDeviationUp.set(-1.0f,1.0f);
            chain_structure.ForkDeviationForward.set(0.0f,1.0f);
            chain_structure.ForkDeviationDecay = 0.5f;

            chain_structure.ForkLength.set(1.0f,2.0f);
            chain_structure.ForkLengthDecay = 0.01f;
        }

        {
            m_blue_beam.BoltWidth.set(0.125f,0.5f);
            m_blue_beam.ColorInside.set(1,1,1);
            m_blue_beam.ColorOutside.set(0,0,1);
            m_blue_beam.ColorFallOffExponent = 2.0f;
        }

        {
            m_blue_cyan_beam.BoltWidth.set(0.25f,0.5f);
            m_blue_cyan_beam.ColorInside.set(0,1,1);
            m_blue_cyan_beam.ColorOutside.set(0,0,1);
            m_blue_cyan_beam.ColorFallOffExponent = 5.0f;
        }

        {
            m_red_beam.BoltWidth.set(0.5f,0.5f);
            m_red_beam.ColorInside.set(1,1,1);
            m_red_beam.ColorOutside.set(1,0,0);
            m_red_beam.ColorFallOffExponent = 5.0f;
        }

        {
            ReadSeeds("nvidia\\Lightning\\models\\seeds.ASE");
            m_inter_coil_lightning.Structure = inter_coil_structure;
            m_fence_lightning.Structure = fence_structure;
            m_coil_helix_lightning.Structure = coil_helix_structure;
            m_chain_lightning.Structure = chain_structure;
        }
    }
}
