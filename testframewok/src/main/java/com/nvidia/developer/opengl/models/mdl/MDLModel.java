package com.nvidia.developer.opengl.models.mdl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import jet.opengl.postprocessing.util.CommentFilter;
import jet.opengl.postprocessing.util.FileUtils;
import jet.opengl.postprocessing.util.LogUtil;

/**
 * Created by mazhen'gui on 2017/11/11.
 */

public class MDLModel {
    private static final int
        CPUT_TOPOLOGY_POINT_LIST = 1,
        CPUT_TOPOLOGY_INDEXED_LINE_LIST=2,
        CPUT_TOPOLOGY_INDEXED_LINE_STRIP=3,
        CPUT_TOPOLOGY_INDEXED_TRIANGLE_LIST=4,
        CPUT_TOPOLOGY_INDEXED_TRIANGLE_STRIP=5,
        CPUT_TOPOLOGY_INDEXED_TRIANGLE_FAN=6;

    private static final int
        CPUT_VERTEX_ELEMENT_UNDEFINED    = 0,
        // Note 1 is missing (back compatibility)
        CPUT_VERTEX_ELEMENT_POSITON      = 2,
        CPUT_VERTEX_ELEMENT_NORMAL       = 3,
        CPUT_VERTEX_ELEMENT_TEXTURECOORD = 4,
        CPUT_VERTEX_ELEMENT_VERTEXCOLOR  = 5,
        CPUT_VERTEX_ELEMENT_TANGENT      = 6,
        CPUT_VERTEX_ELEMENT_BINORMAL     = 7;

    private static final int
            tMINTYPE = 1,
            tINT8=2,      // 2  int =  1 byte
            tUINT8=3,     // 3 UINT, __int8 =  1 byte
            tINT16=4,     // 4 __int16 = 2 bytes
            tUINT16=5,    // 5 unsigned __int16  =  2 bytes
            tINT32=6,     // 6 __int32 = 4 bytes
            tUINT32=7,    // 7 unsigned __int32  =  4 bytes
            tINT64=8,     // 8 __int64  = 8 bytes
            tUINT64=9,    // 9 unsigned __int64 =  8 bytes
            tBOOL=10,      // 10 bool  =  1 byte - '0' = false, '1' = true, same as stl bool i/o
            tCHAR=11,      // 11 signed char  = 1 byte
            tUCHAR=12,     // 12 unsigned char  = 1 byte
            tWCHAR=13,     // 13 wchar_t  = 2 bytes
            tFLOAT=14,     // 14 float  = 4 bytes
            tDOUBLE=15,    // 15 double  = 8 bytes
            // add new ones here
            tINVALID = 255;

    private String m_modelFileName;
    private final List<MDLSet> mSets = new ArrayList<>();
    private final List<String> mMaterialSearchPath = new ArrayList<>();
    private final List<String> mTextureSearchPath = new ArrayList<>();

    public void addMaterialSearchPath(String path){
        if(!mMaterialSearchPath.contains(path)) {
            mMaterialSearchPath.add(path);
        }
    }

    public void addTextureSearchPath(String path){
        if(!mTextureSearchPath.contains(path)) {
            mTextureSearchPath.add(path);
        }
    }

    public void loadFromFile(String filename) throws IOException{
        m_modelFileName = filename;
        String parent = FileUtils.getParent(filename);
        addMaterialSearchPath(parent);
        addTextureSearchPath(parent);

        loadMDLSets(filename);
    }

    public static void main(String[] args){
        MDLModel mdlModel = new MDLModel();
        try {
            mdlModel.addMaterialSearchPath("E:\\SDK\\avsm\\Media\\Material\\");
            mdlModel.addTextureSearchPath("E:\\SDK\\avsm\\Media\\Texture\\");
            mdlModel.loadFromFile("E:\\SDK\\avsm\\Media\\Asset\\roofTop.set");
            for(MDLSet set : mdlModel.mSets) {
                System.out.println(set);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadMDLSets(String filename) throws IOException{
        try(BufferedReader in = new BufferedReader(new InputStreamReader(FileUtils.open(filename)))){
            CommentFilter filter = new CommentFilter(in);

            MDLSet mdlSet = null;
            String line;
            while ((line = filter.nextLine()) != null){
                line = line.trim();
                if(line.isEmpty())
                    continue;
                if(line.charAt(0) == '['){
                    // parse the node
                    int first = line.indexOf(' ');
                    int end = line.indexOf(']');

                    if(first < 0 || end < 0 ){
                        throw new IllegalArgumentException("Invalid synatx");
                    }

                    if(mdlSet != null){
                        mSets.add(mdlSet);
                    }
                    mdlSet = new MDLSet();
                    mdlSet.node = Integer.parseInt(line.substring(first+1, end));
                }else {  // other properties.
                    int dot = line.indexOf('=');
                    String key = line.substring(0, dot).trim();
                    String value = line.substring(dot+1).trim();

                    if(key.equalsIgnoreCase("type")){
                        mdlSet.type = value;
                    }else if(key.equalsIgnoreCase("name")){
                        mdlSet.name = value;
                    }else if(key.equalsIgnoreCase("parent")){
                        mdlSet.parent = Integer.parseInt(value);
                    }else if(key.equalsIgnoreCase("matrixRow0")){
                        mdlSet.matrix.setColumn(0, parseAsFloatArray(value), 0);
                    }else if(key.equalsIgnoreCase("matrixRow1")){
                        mdlSet.matrix.setColumn(1, parseAsFloatArray(value), 0);
                    }else if(key.equalsIgnoreCase("matrixRow2")){
                        mdlSet.matrix.setColumn(2, parseAsFloatArray(value), 0);
                    }else if(key.equalsIgnoreCase("matrixRow3")){
                        mdlSet.matrix.setColumn(3, parseAsFloatArray(value), 0);
                    }else if(key.equalsIgnoreCase("BoundingBoxCenter")){
                        mdlSet.boundingBoxCenter.load(parseAsFloatArray(value), 0);
                    }else if(key.equalsIgnoreCase("BoundingBoxHalf")){
                        mdlSet.boundingBoxHalf.load(parseAsFloatArray(value), 0);
                    }else if(key.equalsIgnoreCase("meshcount")){
                        mdlSet.meshcount = Integer.parseInt(value);
                    }else if(key.startsWith("material")){
                        parseMaterialData(value, mdlSet.materialNames, mdlSet.materials);
                    }
                }
            }

            mSets.add(mdlSet);
        }
    }

    private void parseMaterialData(String name, List<String> materials, List<Properties> data) throws IOException{
        List<String> subMaterials = new ArrayList<>();

        String file = resolveResourcePath(mMaterialSearchPath, name + ".mtl");
        if(file == null){
            LogUtil.e(LogUtil.LogType.DEFAULT, "Couldn't find the file: " + name + ".mtl");
            return;
        }

        Properties properties = new Properties();
        properties.load(FileUtils.open(file));
        if(properties.getProperty("MultiMaterial", "").equals("true")){
            int index = 0;
            while (true){
                String key_name = "Material" + index;
                String value = properties.getProperty(key_name);
                if(value != null){
                    subMaterials.add(value);
                }else{ // value is null
                    break;
                }

                index++;
            }
        }else{
            materials.add(name);
            data.add(properties);
            return;
        }

        if(subMaterials.isEmpty())
            return;

        for(String materialName: subMaterials){
            parseMaterialData(materialName, materials, data);
        }
    }

    private static String resolveResourcePath(List<String> searchPaths, String filename){
        for(String path: searchPaths){
            String file = path + "\\" + filename;
            if(FileUtils.g_IntenalFileLoader.exists(file)){
                return file;
            }
        }

        return null;
    }

    private static float[] parseAsFloatArray(String value){
        String[] tokens = value.split(" ");
        float[] array = new float[tokens.length];
        for(int i = 0; i < tokens.length; i++){
            array[i] = Float.parseFloat(tokens[i]);
        }

        return array;
    }
}
