package jet.opengl.demos.nvidia.shadows;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

final class KnightModel {

	static final int POSITION_SIZE = 3;
	static final int POSITION_OFFSET = 0;
	
	static final int NORMAL_SIZE = 3;
	static final int NORMAL_OFFSET = 3;
	
	static final int COLOR_SIZE = 4;
	static final int COLOR_OFFSET = 6;
	
	static final int UV_SIZE = 2;
	static final int UV_OFFSET = 10;
	
	static final int numVertices = 3455;
	static float[][] vertices;
	
	static final int numIndices = 18129;
	static short[] indices;
	
	private static boolean hasLoaded = false;
	
	static void loadData(){
		if(hasLoaded)
			return;
		hasLoaded = true;
		
		vertices = new float[numVertices][12];
		indices = new short[numIndices];
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader("advance/SoftShadowsDemo/models/KnightModel.cpp"));
			final int none = 0;
			final int data = 1;
			final int indices = 2;
			
			int state = none;
			int index = 0;
			String line;
			while((line = reader.readLine()) != null){
				if(state == none){
					if(line.equals("const ModelVertex KnightModel::vertices[3455] =")){
						state = data;
						reader.readLine(); // skip the next line '{'
						index = 0;
					}
					
					if(line.equals("const uint16_t KnightModel::indices[18129] =")){
						state = indices;
						reader.readLine();
						index = 0;
					}
				}else if(state == data){
					if(line.equals("};")){
						state = none;
					}else{
						StringTokenizer token = new StringTokenizer(line," \t{},");
						int i= 0;
						while(token.hasMoreElements()){
							vertices[index][i++] = Float.parseFloat(token.nextToken());
						}
						
						index++;
					}
				}else if(state == indices){
					if(line.equals("};")){
						state = none;
					}else{
						StringTokenizer token = new StringTokenizer(line," \t,");
						while(token.hasMoreElements()){
							KnightModel.indices[index++] = (short)Integer.parseInt(token.nextToken());
						}
					}
				}
			}
			
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
