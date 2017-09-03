package assimp.importer.fbx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import assimp.common.AssUtil;
import assimp.common.FileUtils;

final class CodeGen {

	static String fbx_simple_property;
	static String fbx_simple_enum_property;
	static final String[] primitiveTypes = {"int", "float", "boolean", "short", "byte", "long", "String", "double"};
	static final HashMap<String, String> typeFilter = new HashMap<String, String>();
	
	static{
		typeFilter.put("aiVector3D", "Vector3f");
		typeFilter.put("bool", "boolean");
		typeFilter.put("std::string", "String");
		typeFilter.put("uint64_t", "long");
		
		Arrays.sort(primitiveTypes);
		load();
	}
	
	public static void main(String[] args) {
		genProperties();
	}
	
	static void genProperties(){
		String source = AssUtil.getClipboard();
		StringTokenizer tokens = new StringTokenizer(source, "\n");
		List<SimpleProperty> simpleProperties = new ArrayList<CodeGen.SimpleProperty>();
		
		while(tokens.hasMoreTokens()){
			String line = tokens.nextToken().trim();
			if(line.startsWith("fbx_simple_property")){
				int i0 = line.indexOf('(');
				int i1 = line.indexOf(',', i0);
				String name = line.substring(i0 + 1, i1).trim();
				int i2 = line.indexOf(',', i1 + 1);
				String type = line.substring(i1 + 1, i2).trim();
				int i3 = line.lastIndexOf(')');
				String defaultValue = line.substring(i2 + 1, i3);
				
				String newType = typeFilter.get(type);
				if(Arrays.binarySearch(primitiveTypes, newType != null ? newType : type) < 0){
					defaultValue = "new " + defaultValue;
					if(newType != null){
						defaultValue = defaultValue.replace(type, newType);
					}
				}
				
				if(newType != null){
					type = newType;
				}
				
				simpleProperties.add(new SimpleProperty(type, name, defaultValue));
			}else if(line.startsWith("fbx_simple_enum_property")){
				int i0 = line.indexOf('(');
				int i1 = line.indexOf(',', i0);
				String name = line.substring(i0 + 1, i1).trim();
				int i2 = line.indexOf(',', i1 + 1);
				String type = line.substring(i1 + 1, i2).trim();
				int i3 = line.lastIndexOf(')');
				String defaultValue = line.substring(i2 + 1, i3);
				
				String newType = "int";
				simpleProperties.add(new SimpleEnumProperty(type, newType, name, defaultValue));
			}
		}
		
		StringBuilder sb = new StringBuilder(2048);
		for(SimpleProperty property: simpleProperties){
			sb.append(property.toString()).append('\n');
		}
		
		System.out.println(sb.toString());
	}
	
	
	static void load(){
		fbx_simple_property = FileUtils.loadTextFromClassPath(CodeGen.class, "fbx_simple_property.txt").toString();
		fbx_simple_enum_property = FileUtils.loadTextFromClassPath(CodeGen.class, "fbx_simple_enum_property.txt").toString();
	}
	
	private static class SimpleProperty{
		String type;
		String name;
		String defaultValue;
		
		SimpleProperty(String type, String name, String defaultValue) {
			this.type = type;
			this.name = name;
			this.defaultValue = defaultValue;
		}

		@Override
		public String toString() {
			return String.format(fbx_simple_property, type, Character.toLowerCase(name.charAt(0)) + name.substring(1), name, defaultValue);
		}
	}
	
	private static final class SimpleEnumProperty extends SimpleProperty{
		
		String etype;
		SimpleEnumProperty(String etype,String type, String name, String defaultValue) {
			super(type, name, defaultValue);
			this.etype = etype;
		}

		@Override
		public String toString() {
			String ename = etype + "_MAX";
			return String.format(fbx_simple_enum_property, type, Character.toLowerCase(name.charAt(0)) + name.substring(1), name, defaultValue, ename, defaultValue, defaultValue, ename, defaultValue);
		}
	}
}
