package assimp.importer.blender;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import assimp.common.AssUtil;
import assimp.common.FileUtils;

/** Internal class used to generate the necessary java code from C++ templates. */
final class StructureGen {
	
	static final String ROOT_FILE = "D:\\SDK\\assimp\\assimp-3.1.1\\code\\BlenderScene.cpp";
	static final int PTR_OBJECT = 0; 
	static final int PTR_ARRAY = 1; 
	static final int PTR_LIST = 2;
	
	static final String[] ptr_files = {"readfield_ptr.txt", "readfield_ptr_array.txt", "readfield_ptr_list.txt"};
	
	static final int GEN_PRIMITIVE = 0;
	static final int GEN_OBJECT = 1;
	static final int GEN_ARRAY1 = 2;
	static final int GEN_ARRAY2 = 3;
	static final int GEN_PTR = 4;
	static final int GEN_PTR_ARRAY = 5;
	static final int GEN_PTR_LIST = 6;
	
	public static void main(String[] args) {
		start();
	}
	
	private static void start(){
		final HashMap<Class<?>, List<Seed>> functions = new LinkedHashMap<Class<?>, List<Seed>>(64);
		final HashMap<String, Class<?>> classes = new HashMap<String, Class<?>>(64);
		
		String line = null;
		try(BufferedReader reader = new BufferedReader(new FileReader(new File(ROOT_FILE)))){
			boolean inFunction = false;
			String tag = "template <> void Structure ::";
			Class<?> currentClass = null;
			List<Seed> seeds = null;
			while((line = reader.readLine()) != null){
				if(!inFunction){
					if(line.startsWith(tag)){
						int i0 = line.lastIndexOf('<');
						int i1 = line.lastIndexOf('>');
						String name = line.substring(i0 + 1, i1);
						if(name.equals("Base"))
							continue;
						
						currentClass = forName(name, classes);
						seeds = new ArrayList<Seed>();
						inFunction = true;
					}
				}else{
					line = line.trim();
					Seed seed = new Seed();
					if(line.equals("db.reader->IncPtr(size);")){
						seed.varName = "db.reader.incPtr(size);";
						seeds.add(seed);
						functions.put(currentClass, seeds);
						seeds = null;
						inFunction = false;
					}else if(line.startsWith("ReadFieldArray")){
						String name = parseSeed(line, seed, currentClass, classes);
						
						if(line.startsWith("ReadFieldArray2"))
							seed.demension = 2;
						else
							seed.demension = 1;
						
						if(!seed.varName.equals(name)){
							throw new RuntimeException(seed.varName + " : " + name);
						}
						
						seeds.add(seed);
					}else if(line.startsWith("ReadFieldPtr")){
						String name = parseSeed(line, seed, currentClass, classes);
						if(!name.equals("**mat") &&!seed.varName.equals(name.substring(1))){
							throw new RuntimeException(seed.varName + " : " + name);
						}
						
						seed.isPtr = true;
						seeds.add(seed);
					}else if(line.startsWith("ReadField")){
						String name = parseSeed(line, seed, currentClass, classes);
						if(!seed.varName.equals(name)){
							throw new RuntimeException(seed.varName + " : " + name);
						}
						
						seeds.add(seed);
					}else {
					
					}
				
				}
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e){
			System.out.println("line = " + line);
			e.printStackTrace();
		}
		
		System.out.println("read file done!");
		final String convert_template = FileUtils.loadTextFromClassPath(StructureGen.class, "convert.txt").toString();
		System.out.println("read convert_template done!");
		final StringBuilder out = new StringBuilder(10000);
		final HashSet<String> lines = new LinkedHashSet<String>();
		final StringBuilder content = new StringBuilder();
		System.out.println("Allocate data done!");
		
		final Gen[] gens = new Gen[GEN_PTR_LIST + 1];
		gens[GEN_PRIMITIVE] = new PrimitiveGen();
		System.out.println("Init primitive done!");
		gens[GEN_OBJECT] = new ObjectGen();
		System.out.println("Init Object gen done!");
		gens[GEN_ARRAY1] = new ArrayGen(1);
		System.out.println("Init Array1 done!");
		gens[GEN_ARRAY2] = new ArrayGen(2);
		System.out.println("Init Array2 done!");
		gens[GEN_PTR] = new ObjectPtr(PTR_OBJECT);
		System.out.println("Init PTR_OBJECT  done!");
		gens[GEN_PTR_ARRAY] = new ObjectPtr(PTR_ARRAY);
		System.out.println("Init PTR_ARRAY done!");
		gens[GEN_PTR_LIST] = new ObjectPtr(PTR_LIST);
		
		System.out.println("read file done!");
		System.out.println("function count = " + functions.size());

		for(Map.Entry<Class<?>, List<Seed>> entry : functions.entrySet()){
			lines.clear();
			
//			System.out.println("Begin to make the convert<" + entry.getKey().getSimpleName() + ">");
			
			List<Seed> seeds = entry.getValue();
			for(int i = 0; i < seeds.size() - 1; i++){
				Seed s = seeds.get(i);
				int k = s.getGen();
				Gen gen = gens[k];
				if(k == GEN_PTR_ARRAY){
					System.out.println(s.varName);
					System.out.println(s.type.getName());
				}
				
				String l =gen.declar(s.type.getSimpleName());
				if(l.length() > 0)
					lines.add(l);
				
				l = gen.genMethod(s);
				if(l.length() > 0)
					out.append(l).append("\n\n");
			}
			
			content.delete(0, content.length());
			for(String _line : lines){
				content.append(_line).append('\n');
			}
			
			for(int i = 0; i < seeds.size() - 1; i++){
				Seed s = seeds.get(i);
				Gen gen = gens[s.getGen()];
				content.append("\t\t").append(gen.gen(s)).append('\n');
			}
			
			content.append("\n\t\t").append(AssUtil.back(seeds).varName).append('\n');
//			System.out.println("end to make the convert<" + entry.getKey().getSimpleName() + ">");
			out.append(format(convert_template, entry.getKey().getSimpleName()).replace("##", content));
		}
		
//		System.out.println(out);
		try(BufferedWriter writer = new BufferedWriter(new FileWriter(new File("struct")))){
			writer.write(out.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static String parseSeed(String line, Seed seed, Class<?> currentClass, HashMap<String, Class<?>> classes) throws NoSuchFieldException, SecurityException{
		int i0 = line.lastIndexOf('<');
		int i1 = line.lastIndexOf('>');
		seed.error_flag = line.substring(i0 + 1, i1);
		i0 = line.indexOf('.', i1);
		if(i0 < 0)
			i0 = line.indexOf('(', i1);
		i1 = line.indexOf(',', i0);
		seed.varName = line.substring(i0 + 1, i1);
		java.lang.reflect.Field field = currentClass.getDeclaredField(seed.varName);
		Class<?> varClass = field.getType();
		if(varClass.getName().startsWith("java.util")){
			seed.isList = true;
			String typename = field.getGenericType().getTypeName();
			int m0 = typename.lastIndexOf('.');
			int m1 = typename.lastIndexOf('>');
			seed.type = forName(typename.substring(m0 + 1, m1), classes);
//			System.out.println(varClass.getName());
		}else{
			seed.type = varClass;
		}
//		Class<?> varClazz = getFieldType(currentClass, seed.varName);
//		if(!classes.containsKey(varClazz.getName()))
//			classes.put(varClazz.getName(), varClazz);
		
		if(line.contains("int&"))
			seed.type = int.class;
		else if(line.contains("short&"))
			seed.type = short.class;
		
		i0 = line.indexOf('\"', i1);
		i1 = line.indexOf('\"', i0 + 1);
		return line.substring(i0 + 1, i1);
	}
	
	private static Class<?> forName(String name, HashMap<String, Class<?>> cache){
		String className = "assimp.importer.blender." + name;
		for(int i = 0; i < 2; i++){
			Class<?> clazz = cache.get(className);
			
			try {
				if(clazz == null)
					clazz = Class.forName(className);
				else
					return clazz;
			} catch (ClassNotFoundException e) {
				clazz = null;
			}
			
			if(clazz != null){
				cache.put(clazz.getName(), clazz);
				return clazz;
			}else{
				className = "assimp.importer.blender.BLE" + name;
			}
		}
		
		throw new ClassCastException(className);
	}
	
	private static String getClassSimpleName(Class<?> clzz){
		while(clzz.isArray()){
			clzz = clzz.getComponentType();
		}
		
		return clzz.getSimpleName();
	}
	
	private final static class Seed{
		int demension;  // the dimension of the array, 0 means it isn't an array.
		boolean isPtr;
		boolean isList;
		Class<?> type;
		String varName;
		String error_flag;
		
		int getGen(){
			if(isList){
				return GEN_PTR_LIST;
			}
			if(type.isPrimitive()){
				return GEN_PRIMITIVE;
			}else if(type.isArray()){
				if(demension == 1)
					return GEN_ARRAY1;
				else if(demension == 2)
					return GEN_ARRAY2;
				else 
					return GEN_PTR_ARRAY;
			}else{
				return isPtr ? GEN_PTR : GEN_OBJECT;
			}
		}
	}

	private abstract static class Gen{
		final String template;
		
		public Gen(String template) {
			this.template = template;
		}
		
		String declar(String typeName) { return "";}
		String genMethod(Seed seed) { return  "";}
		abstract String gen(Seed seed);
	}
	
	private final static class PrimitiveGen extends Gen{
		
		public PrimitiveGen() {
			super(null);
		}
		
		public String gen(Seed seed){
			String type = seed.type.getTypeName();
			String _type = Character.toUpperCase(type.charAt(0)) + type.substring(1);
			return format("dest.%s = readField%s(\"%s\",db,%s);", seed.varName, _type, seed.varName, seed.error_flag);
		}
	}
	
	private final static class ObjectGen extends Gen{
		private final HashMap<String, String> caches = new HashMap<String, String>();
		
		public ObjectGen() {
			super(FileUtils.loadTextFromClassPath(StructureGen.class, "readfield_object.txt").toString());
		}
		
		@Override
		String genMethod(Seed seed) {
			String name = seed.type.getSimpleName();
			String content = caches.get(name);
			if(content == null){
				content = format(template, seed.type.getSimpleName());
				caches.put(name, content);
				return content;
			}
			
			return "";
		}
		
		@Override
		String gen(Seed seed) {
			return format("readField(dest.%s,\"%s\",db, %s);", seed.varName, seed.varName, seed.error_flag);
		}
	}
	
	private final static class ArrayGen extends Gen{
		private final HashMap<String, String> caches = new HashMap<String, String>(8);
		
		public ArrayGen(int dimension) {
			super(FileUtils.loadTextFromClassPath(StructureGen.class, format("readfield_array%d.txt", dimension)).toString());
		}
		
		@Override
		String genMethod(Seed seed) {
			String name = getClassSimpleName(seed.type);
			String content = caches.get(name);
			if(content == null){
				String upper = Character.toUpperCase(name.charAt(0)) + name.substring(1);
				content = format(template, name, name, upper);
				caches.put(name, content);
				return content;
			}
			
			return "";
		}
		
		@Override
		String gen(Seed seed) {
			return format("readFieldArray(dest.%s,\"%s\",db, %s);", seed.varName, seed.varName, seed.error_flag);
		}
	}
	
	private final static class ObjectPtr extends Gen{
		private static final String source1 =  "readFieldPtr%s(holder%s,\"*%s\",db, false, %s);\n\t\t" +
			    "dest.%s = holder%s.get();";
		private static final String source2 = "readFieldPtr%s(dest.%s,\"*%s\",db, %s);";
		private static final String source3 = "readFieldPtr%s(dest.%s,\"*%s\",db, false, %s);";
		
		private final HashMap<String, String> caches = new HashMap<String, String>();
		private final HashMap<String, String> holders = new HashMap<String, String>();
		
		private int ptr;
		private int count;
		public ObjectPtr(int ptr) {
			super(FileUtils.loadTextFromClassPath(StructureGen.class, ptr_files[ptr]).toString());
			this.ptr = ptr;
			
			int index = 0;
			while((index = template.indexOf("%s", index + 1)) > 0){
				count++;
			}
		}
		
		@Override
		String declar(String typeName) {
			if(ptr != PTR_OBJECT)
				return "";
			
			String content = holders.get(typeName);
			if(content == null){
				content = format("ObjectHolder<%s> holder%s = new ObjectHolder<%s>();", typeName, typeName, typeName);
				holders.put(typeName, content);
			}
			return content;
		}
		
		@Override
		String genMethod(Seed seed) {
			String name = seed.type.getSimpleName().replace("[]", "");
			String content = caches.get(name);
			if(content == null){
				Object[] array = new Object[count];
				Arrays.fill(array, name);
				content = format(template, array);
				caches.put(name, content);
				return content;
			}
			
			return "";
			
		}
		
		@Override
		String gen(Seed seed) {
			switch (ptr) {
			case PTR_OBJECT:
			{
				String typename = seed.type.getSimpleName();
				return format(source1, typename, typename, seed.varName, seed.error_flag, seed.varName, typename);
			}
			case PTR_LIST:
			{
				String typename = getClassSimpleName(seed.type);
				return format(source3, typename, seed.varName, seed.varName,seed.error_flag);
			}
			case PTR_ARRAY:
			{
				String typename = getClassSimpleName(seed.type);
				return format(source2, typename, seed.varName, seed.varName,seed.error_flag);
			}
			default:
				throw new IllegalArgumentException();
			}
		}
		
	}
	
	static String format(String pat, Object...args){
		try {
			return String.format(pat, args);
		} catch (Exception e) {
			System.out.println("format = " + pat);
			System.out.println(Arrays.toString(args));
			System.out.println();
			e.printStackTrace();
			System.exit(0);
		}
		
		return "";
	}
}
