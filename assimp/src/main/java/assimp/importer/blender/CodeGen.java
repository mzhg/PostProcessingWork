package assimp.importer.blender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import assimp.common.AssUtil;
import assimp.common.FileUtils;

final class CodeGen {

	public static void main(String[] args) {
//		genReader(BLEImage.class);
		genRegister();
	}
	
	static void genRegister(){
		String template = FileUtils.loadTextFromClassPath(CodeGen.class, "register_template.txt").toString();
		String source = AssUtil.getClipboard();
		String[] array = {
			"Object", "Mesh", "Scene", "Camera", "Image", "Material"	
		};
		Arrays.sort(array);
		
		List<String> lines = new ArrayList<String>();
		StringTokenizer tokens = new StringTokenizer(source, "\n");
		while(tokens.hasMoreTokens())
			lines.add(tokens.nextToken().trim());
		
		StringBuilder out = new StringBuilder();
		for(String line : lines){
			int i1 = line.indexOf('\"');
			if(i1 > 0){
				int i2 = line.indexOf('\"', i1+1);
				String name1 = line.substring(i1 + 1, i2);
				String name2, name3;
//				if(name1.equals("Object")){
				if(Arrays.binarySearch(array, name1) >= 0){
					name2 = name3 = "BLE" + name1;
				}else{
					name2 = name3 = name1;
				}
				
				out.append("\t\t");
				out.append(String.format(template, name1, name2, name3));
				out.append('\n');
			}
		}
		
		System.out.println(out);
		
	}
	
	static void genReader(Class<?> clazz){
		final String pattern = "dest.%s = readField%s(\"%s\",db,%s);\n";
		String source = AssUtil.getClipboard();
		
		List<String> lines = new ArrayList<String>();
		StringTokenizer tokens = new StringTokenizer(source, "\n");
		while(tokens.hasMoreTokens())
			lines.add(tokens.nextToken().trim());
		
		StringBuilder sb = new StringBuilder();
		for(String line : lines){
			int i1 = line.indexOf('<');
			if(i1 > 0 && line.subSequence(0, i1).equals("ReadField")){
				int i2 = line.indexOf('>', i1);
				final String errorPolicy = line.substring(i1 + 1, i2).trim();
				
				int i3 = line.indexOf('.', i2);
				int i4 = line.indexOf(',', i3);
				final String varName = line.substring(i3 + 1, i4).trim();
				
				try {
					Class<?> type = clazz.getDeclaredField(varName).getType();
					if(type == float.class){
						sb.append(String.format(pattern, varName, "Float", varName, errorPolicy));
					}else if(type == double.class){
						sb.append(String.format(pattern, varName, "Double", varName, errorPolicy));
					}else if(type == int.class){
						sb.append(String.format(pattern, varName, "Int", varName, errorPolicy));
					}else if(type == short.class){
						sb.append(String.format(pattern, varName, "Short", varName, errorPolicy));
					}else if(type == byte.class){
						sb.append(String.format(pattern, varName, "Byte", varName, errorPolicy));
					}else{
//						throw new RuntimeException("Unkown type: " + type.getName());
						sb.append(line).append("\n");
					}
				} catch (NoSuchFieldException | SecurityException e) {
					e.printStackTrace();
				}
			}else if(line.endsWith("db.reader->IncPtr(size);")){
				sb.append("db.reader.incPtr(size);\n");
			}else{
				sb.append(line).append('\n');
			}
		}
		
		System.out.println(sb);
	}
}
