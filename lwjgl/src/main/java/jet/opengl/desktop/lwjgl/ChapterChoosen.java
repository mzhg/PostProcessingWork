package jet.opengl.desktop.lwjgl;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.StringTokenizer;

import jet.opengl.postprocessing.util.Numeric;

public class ChapterChoosen {

    private static final  String[][]  chapters = {
            {},  // The first chapter,
            {},  // The second chapter,
            {}, // The third chapter,
            {
                "4.1 Basic Transforms",
                "4.2 Special Matrix Transforms and Operations",
                "4.3 Quaternions",
                "4.4 Vertex Blending",
                "4.5 Morphing",
                "4.6 Geometry Cache Playback",
                "4.7 Projections",
            },

            {
                    "5.1 Shading Models",
                    "5.2 Light Sources",
                    "5.3 Implementing Shading Models",
                    "5.4 Aliasing and Antialiasing",
                    "5.5 Transparency, Alpha, and Compositing",
                    "5.6 Display Encoding",
            },

            {
                    "6.1 The Texturing Pipeline",
                    "6.2 Image Texturing",
                    "6.3 Procedural Texturing",
                    "6.4 Texture Animation",
                    "6.5 Material Mapping",
                    "6.6 Alpha Mapping",
                    "6.7 Bump Mapping",
                    "6.8 Parallax Mapping",
                    "6.9 Textured Lights",
            },

            {
                    "7.1 Planar Shadows",
                    "7.2 Shadows on Curved Surfaces",
                    "7.3 Shadow Volumes",
                    "7.4 Shadow Maps",
                    "7.5 Percentage-Closer Filtering",
                    "7.6 Percentage-Closer Soft Shadows",
                    "7.7 Filtered Shadow Maps",
                    "7.8 Volumetric Shadow Techniques",
                    "7.9 Irregular Z -Buffer Shadows",
                    "7.10 Other Applications",
            },

            {
                    "8.1 Light Quantities",
                    "8.2 Scene to Screen",
            },

            {
                    "9.1 Physics of Light",
                    "9.2 The Camera",
                    "9.3 The BRDF",
                    "9.4 Illumination",
                    "9.5 Fresnel Reflectance",
                    "9.6 Microgeometry",
                    "9.7 Microfacet Theory",
                    "9.8 BRDF Models for Surface Reflection",
                    "9.9 BRDF Models for Subsurface Scattering",
                    "9.10 BRDF Models for Cloth",
                    "9.11 Wave Optics BRDF Models",
                    "9.12 Layered Materials",
                    "9.13 Blending and Filtering Materials",
            },

            {
                    "10.1 Area Light Sources",
                    "10.2 Environment Lighting",
                    "10.3 Spherical and Hemispherical Functions",
                    "10.4 Environment Mapping",
                    "10.5 Specular Image-Based Lighting",
                    "10.6 Irradiance Environment Mapping",
                    "10.7 Sources of Error",
            },

            {
                    "11.1 The Rendering Equation",
                    "11.2 General Global Illumination",
                    "11.3 Ambient Occlusion",
                    "11.4 Directional Occlusion",
                    "11.5 Diffuse Global Illumination",
                    "11.6 Specular Global Illumination",
                    "11.7 Unified Approaches",
            },

            {
                    "12.1 Image Processing",
                    "12.2 Reprojection Techniques",
                    "12.3 Lens Flare and Bloom",
                    "12.4 Depth of Field",
                    "12.5 Motion Blur",
            },

            {
                    "13.1 The Rendering Spectrum",
                    "13.2 Fixed-View Effects",
                    "13.3 Skyboxes",
                    "13.4 Light Field Rendering",
                    "13.5 Sprites and Layers",
                    "13.6 Billboarding",
                    "13.7 Displacement Techniques",
                    "13.8 Particle Systems",
                    "13.9 Point Rendering",
                    "13.10 Voxels",
            },

            {
                    "14.1 Light Scattering Theory",
                    "14.2 Specialized Volumetric Rendering",
                    "14.3 General Volumetric Rendering",
                    "14.4 Sky Rendering",
                    "14.5 Translucent Surfaces",
                    "14.6 Subsurface Scattering",
                    "14.7 Hair and Fur",
                    "14.8 Unified Approaches",
            },

            {
                    "15.1 Toon Shading",
                    "15.2 Outline Rendering",
                    "15.3 Stroke Surface Stylization",
                    "15.4 Lines",
                    "15.5 Text Rendering",
            },

            {
                    "16.1 Sources of Three-Dimensional Data",
                    "16.2 Tessellation and Triangulation",
                    "16.3 Consolidation",
                    "16.4 Triangle Fans, Strips, and Meshes",
                    "16.5 Simplification",
                    "16.6 Compression and Precision",
            },

            {
                    "17.1 Parametric Curves",
                    "17.2 Parametric Curved Surfaces",
                    "17.3 Implicit Surfaces",
                    "17.4 Subdivision Curves",
                    "17.5 Subdivision Surfaces",
                    "17.6 Efficient Tessellation",
            },

            {
                    "18.1 Profiling and Debugging Tools",
                    "18.2 Locating the Bottleneck",
                    "18.3 Performance Measurements",
                    "18.4 Optimization",
                    "18.5 Multiprocessing",
            },

            {
                    "19.1 Spatial Data Structures",
                    "19.2 Culling Techniques",
                    "19.3 Backface Culling",
                    "19.4 View Frustum Culling",
                    "19.5 Portal Culling",
                    "19.6 Detail and Small Triangle Culling",
                    "19.7 Occlusion Culling",
                    "19.8 Culling Systems",
                    "19.9 Level of Detail",
                    "19.10 Rendering Large Scenes",
            },

            {
                    "20.1 Deferred Shading",
                    "20.2 Decal Rendering",
                    "20.3 Tiled Shading",
                    "20.4 Clustered Shading",
                    "20.5 Deferred Texturing",
                    "20.6 Object- and Texture-Space Shading",
            },

            {
                    "21.1 Equipment and Systems Overview",
                    "21.2 Physical Elements",
                    "21.3 APIs and Hardware",
                    "21.4 Rendering Techniques",
            },

            {
                    "22.1 GPU-Accelerated Picking",
                    "22.2 Definitions and Tools",
                    "22.3 Bounding Volume Creation",
                    "22.4 Geometric Probability",
                    "22.5 Rules of Thumb",
                    "22.6 Ray/Sphere Intersection",
                    "22.7 Ray/Box Intersection",
                    "22.8 Ray/Triangle Intersection",
                    "22.9 Ray/Polygon Intersection",
                    "22.10 Plane/Box Intersection",
                    "22.11 Triangle/Triangle Intersection",
                    "22.12 Triangle/Box Intersection",
                    "22.13 Bounding-Volume/Bounding-Volume Intersection",
                    "22.14 View Frustum Intersection",
                    "22.15 Line/Line Intersection",
                    "22.16 Intersection between Three Planes",
            },

            {
                    "23.1 Rasterization",
                    "23.2 Massive Compute and Scheduling",
                    "23.3 Latency and Occupancy",
                    "23.4 Memory Architecture and Buses",
                    "23.5 Caching and Compression",
                    "23.6 Color Buffering",
                    "23.7 Depth Culling, Testing, and Buffering",
                    "23.8 Texturing",
                    "23.9 Architecture",
                    "23.10 Case Studies",
                    "23.11 Ray Tracing Architectures",
            },
        };


    public static void main(String[] args){
        randomChapter();
        randomSection();

        /** 2019年6月3日19点58分, Cho
          Chanpter 11: Global Illumination
          Chanpter 20: 20.6 Object- and Texture-Space Shading
         Choose both.
         */
    }

    private static void parseChapters(){
        String source = getTextFromClipBoard();
        StringBuilder out = new StringBuilder("{\n");

        StringTokenizer tokenizer = new StringTokenizer(source, "\n");
        while (tokenizer.hasMoreElements()){
            String line = tokenizer.nextToken().trim();

            int dot = line.indexOf('.', 3);
            if(dot > 0){
                line = line.substring(0, dot).trim();
            }

            out.append('\t').append('"').append(line).append('"').append(',').append('\n');
        }

        out.append("},");

        System.out.println(out);
    }

    private static String getTextFromClipBoard() {
        Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
        try {
            return (String) t.getTransferData(DataFlavor.stringFlavor);
        } catch (UnsupportedFlavorException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "";
    }

    private static void randomChapter(){
        int chapter;
        do {
            chapter = (int) Numeric.random(0, chapters.length);
        }while (chapters[chapter].length == 0);

        String[] values = chapters[chapter];

        System.out.println("The selected chapter is " + (chapter+1));
        for(int i = 0; i < values.length; i++) System.out.println(values[i]);
    }

    private static void randomSection(){
        int chapter;
        do {
            chapter = (int) Numeric.random(0, chapters.length);
        }while (chapters[chapter].length == 0);

        int section = (int)Numeric.random(0, chapters[chapter].length);
        System.out.println("The selected chapter is " + (chapter+1) + ", and the selection is " + chapters[chapter][section]);
    }
}
