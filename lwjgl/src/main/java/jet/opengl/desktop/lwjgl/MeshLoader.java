package jet.opengl.desktop.lwjgl;

import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIFace;
import org.lwjgl.assimp.AIMaterial;
import org.lwjgl.assimp.AIMaterialProperty;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIString;
import org.lwjgl.assimp.AIVector3D;
import org.lwjgl.assimp.Assimp;
import org.lwjgl.system.MemoryUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

import jet.opengl.postprocessing.util.BufferUtils;
import jet.opengl.postprocessing.util.DebugTools;

/**
 * Created by mazhen'gui on 2017/8/30.
 */

public class MeshLoader {
    public static void main(String[] args) {
//        loadLightningXMesh();
        loadRainbow();
    }

    static void loadLightningXMesh(){
        String root = "E:\\SDK\\Lightning\\Direct3D\\Media\\Lightning\\";
        String[] tokens = {"scene", "chain_target", "seeds"};
        String[] exts = {".X", ".X", ".ASE"};

        for(int i = 0; i < tokens.length; i++){
            String token =tokens[i];
            String ext = exts[i];
            File file = new File(root + token + ext);
            if(file.exists() == false)
                throw new IllegalArgumentException();
            AIScene scene = Assimp.aiImportFile(file.getAbsolutePath(), 0);
            if (scene == null) {
                throw new IllegalStateException(Assimp.aiGetErrorString());
            }
            int numMesh = scene.mNumMeshes();
            PointerBuffer meshesBuffer  =  scene.mMeshes();

            final String output = root + token;
            for(int j = 0; j < numMesh; j++){
                saveMeshData(AIMesh.create(meshesBuffer.get(j)), j, output, token);
                System.out.println();
            }

//            int numMaterials = scene.mNumMaterials();
//            PointerBuffer materialsBuffer = scene.mMaterials();
//            for(int i = 0; i < numMaterials; i++){
//                AIMaterial material = AIMaterial.create(materialsBuffer.get(i));
//            }

            try {
                Assimp.aiReleaseImport(scene);
            } catch (Exception e) {
//                e.printStackTrace();
            }
        }

    }

    static void loadRainbow(){
        String root = "E:\\SDK\\HLSL_RainbowFogbow\\MEDIA\\models\\RainbowFogbowModels\\";
        String[] tokens = {"rainbowFogBow_skyBox", "rainbowFogBow_skyBox_Noise", "rainbowFogBow_terrain"};
        String[] exts = {".x", ".x", ".x"};

        for(int i = 0; i < tokens.length; i++){
            String token =tokens[i];
            String ext = exts[i];
            File file = new File(root + token + ext);
            if(file.exists() == false)
                throw new IllegalArgumentException();


            AIScene scene = Assimp.aiImportFile(file.getAbsolutePath(), Assimp.aiProcess_Triangulate|Assimp.aiProcess_SortByPType);
            if (scene == null) {
                throw new IllegalStateException(Assimp.aiGetErrorString());
            }
            int numMesh = scene.mNumMeshes();
            PointerBuffer meshesBuffer  =  scene.mMeshes();

            final String output = root + token;

            StringBuilder materialIdxStr = new StringBuilder(256);
            materialIdxStr.append("MeshCount: ").append(numMesh).append('\n');
            for(int j = 0; j < numMesh; j++){
                AIMesh mesh = AIMesh.create(meshesBuffer.get(j));
                saveMeshData(mesh, j, output, token);
                int materialIdx = mesh.mMaterialIndex();
                materialIdxStr.append("materialIdx: ").append(materialIdx).append('\n');
                System.out.println();
            }

            printMaterials(scene, materialIdxStr);

            try (BufferedWriter out = new BufferedWriter(new FileWriter(output + "_Materials.txt"))){
                out.write(materialIdxStr.toString());
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                Assimp.aiReleaseImport(scene);
            } catch (Exception e) {
//                e.printStackTrace();
            }
        }
    }

    static void loadOrcXMesh(){
        String root = "E:\\SDK\\PerlinFire\\Direct3D\\Media\\Orcs\\";
        String[] tokens = {"bonfire_wOrcs"};
        String[] exts = {".X"};

        for(int i = 0; i < tokens.length; i++){
            String token =tokens[i];
            String ext = exts[i];
            File file = new File(root + token + ext);
            if(file.exists() == false)
                throw new IllegalArgumentException();


            AIScene scene = Assimp.aiImportFile(file.getAbsolutePath(), Assimp.aiProcess_Triangulate);
            if (scene == null) {
                throw new IllegalStateException(Assimp.aiGetErrorString());
            }
            int numMesh = scene.mNumMeshes();
            PointerBuffer meshesBuffer  =  scene.mMeshes();

            final String output = root + token;

            StringBuilder materialIdxStr = new StringBuilder(256);
            materialIdxStr.append("MeshCount: ").append(numMesh).append('\n');
            for(int j = 0; j < numMesh; j++){
                AIMesh mesh = AIMesh.create(meshesBuffer.get(j));
                saveMeshData(mesh, j, output, token);
                int materialIdx = mesh.mMaterialIndex();
                materialIdxStr.append("materialIdx: ").append(materialIdx).append('\n');
                System.out.println();
            }

            printMaterials(scene, materialIdxStr);

            try (BufferedWriter out = new BufferedWriter(new FileWriter(output + "_Materials.txt"))){
                out.write(materialIdxStr.toString());
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                Assimp.aiReleaseImport(scene);
            } catch (Exception e) {
//                e.printStackTrace();
            }
        }
    }

    static void printMaterials(AIScene scene, StringBuilder textureStr){
        int numMaterials = scene.mNumMaterials();
        PointerBuffer materialsBuffer = scene.mMaterials();
        textureStr.append("TextureCount: ").append(numMaterials).append('\n');
        for(int j = 0; j < numMaterials; j++){
            AIMaterial material = AIMaterial.create(materialsBuffer.get(j));
            int numProperties = material.mNumProperties();
            PointerBuffer propertyViews = material.mProperties();
            for(int k = 0; k < numProperties; k++){
                AIMaterialProperty property = AIMaterialProperty.create(propertyViews.get(k));
                AIString key =  property.mKey();
                ByteBuffer value = property.mData();
                int type = property.mType();
                String type_str;
                if(type == Assimp.aiPTI_Float){
                    int count = value.remaining()/4;
                    if(count == 1){
                        type_str = Float.toString(value.getFloat());
                    }else{
                        float[] values = new float[count];
                        for(int l = 0; l < count; l++){
                            values[l] = value.getFloat();
                        }
                        type_str = Arrays.toString(values);
                    }
                }else if(type == Assimp.aiPTI_Integer){
                    int count = value.remaining()/4;
                    if(count == 1){
                        type_str = Integer.toString(value.getInt());
                    }else{
                        int[] values = new int[count];
                        for(int l = 0; l < count; l++){
                            values[l] = value.getInt();
                        }
                        type_str = Arrays.toString(values);
                    }
                }else if(type == Assimp.aiPTI_Double){
                    int count = value.remaining()/8;
                    if(count == 1){
                        type_str = Double.toString(value.getDouble());
                    }else{
                        double[] values = new double[count];
                        for(int l = 0; l < count; l++){
                            values[l] = value.getDouble();
                        }
                        type_str = Arrays.toString(values);
                    }
                }else if(type == Assimp.aiPTI_Buffer){
                    type_str = "Binary";
                }else if(type == Assimp.aiPTI_String){
                    type_str = MemoryUtil.memUTF8(value).trim();
                    int dot = type_str.lastIndexOf('\n');
                    if(dot > 0){
                        type_str = type_str.substring(0, dot);
                    }
                }else{
                    throw new IllegalArgumentException("Unkown type");
                }

                System.out.println("Material: " + j + ", property: "+ k +" ,key = " + key.dataString() + ", value = " + type_str);

                if(key.dataString().equals("$tex.file")){
                    textureStr.append("Texture: ").append(type_str).append('\n');
                }
            }
        }
    }

    static void loadBrigeXMesh(){
        File file = new File("E:\\SDK\\Rain\\Direct3D\\Media\\Bridge\\Bridge.x");
        if(file.exists() == false)
            throw new IllegalArgumentException();
        AIScene scene = Assimp.aiImportFile(file.getAbsolutePath(), 0);
        if (scene == null) {
            throw new IllegalStateException(Assimp.aiGetErrorString());
        }
        int numMesh = scene.mNumMeshes();
        PointerBuffer meshesBuffer  =  scene.mMeshes();

        final String output = "E:\\SDK\\Rain\\Direct3D\\Media\\Bridge\\Bridge";
        for(int i = 0; i < numMesh; i++){
            saveMeshData(AIMesh.create(meshesBuffer.get(i)), i, output, "Bridge");
            System.out.println();
        }

        Assimp.aiReleaseImport(scene);
    }

    static void saveMeshData(AIMesh mesh, int index, String output, String name){
        int materialIdx = mesh.mMaterialIndex();
        System.out.println(name + "_" + index + "_: materialIdx = " + materialIdx);
        AIVector3D.Buffer vertices = mesh.mVertices();
        ByteBuffer verticesBuffer = MemoryUtil.memByteBuffer(vertices.address(), AIVector3D.SIZEOF * vertices.remaining());
        try {
            String filename = output + index + "_vertice.dat";
            DebugTools.write(verticesBuffer, filename, false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(name + "_" + index + "_:Vertice sizeof = " + AIVector3D.SIZEOF * vertices.remaining());

        AIVector3D.Buffer normals = mesh.mNormals();
        ByteBuffer normalsBuffer = MemoryUtil.memByteBuffer(normals.address(), AIVector3D.SIZEOF * normals.remaining());
        try {
            String filename = output + index + "_normal.dat";
            DebugTools.write(normalsBuffer, filename, false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        AIVector3D.Buffer tangents = mesh.mTangents();
        if(tangents != null) {
            ByteBuffer tangentsBuffer = MemoryUtil.memByteBuffer(tangents.address(), AIVector3D.SIZEOF * tangents.remaining());
            try {
                String filename = output + index + "_tangent.dat";
                DebugTools.write(tangentsBuffer, filename, false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        AIVector3D.Buffer texcoords = mesh.mTextureCoords(0);
        if(texcoords != null){
            ByteBuffer texcoordsBuffer = MemoryUtil.memByteBuffer(texcoords.address(), AIVector3D.SIZEOF * texcoords.remaining());
            try {
                String filename = output + index + "_texcoord.dat";
                DebugTools.write(texcoordsBuffer, filename, false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        int faceCount = mesh.mNumFaces();
        if(faceCount == 0)
            return;

        AIFace.Buffer facesBuffer = mesh.mFaces();
        int vertexPerFace = facesBuffer.get(0).mNumIndices();
        if(vertexPerFace == 4){
            int k = 2;
        }
        int elementCount = faceCount * vertexPerFace;
        int[] elementArrayBufferData = new int[elementCount];


        for (int i = 0; i < faceCount; ++i) {
            AIFace face = facesBuffer.get(i);
            if (face.mNumIndices() != vertexPerFace) {
                throw new IllegalStateException("AIFace.mNumIndices() = " + face.mNumIndices());
            }
//            elementArrayBufferData.put(face.mIndices());
            IntBuffer indices = face.mIndices();
            for(int j = 0; j < vertexPerFace; j++){
                elementArrayBufferData[i * vertexPerFace + j] = indices.get();
            }
        }

        try {
            ByteBuffer bytes = BufferUtils.createByteBuffer(elementArrayBufferData.length * 4);
            IntBuffer shorts = bytes.asIntBuffer();
            shorts.put(elementArrayBufferData).flip();
            String filename = output + index + "_indices.dat";
            DebugTools.write(bytes, filename, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
