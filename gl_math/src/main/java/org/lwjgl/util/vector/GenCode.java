package org.lwjgl.util.vector;

import java.util.Random;

final class GenCode {
	static final String returnSame = "\tcase %d: return m%d%d;\n";
	static final String returnUnsame = "\tcase %d: return columnMajor ? m%d%d : m%d%d;\n";
	
	static float[] arr = new float[16];
	
	static Random r = new Random();
	public static void main(String[] args) {
//		genVertex(4);
		
		genGet(4);
		
//		switch_vs_arr();
	}
	
	static void switch_vs_arr(){
		final int loopCount = 20;
		long[] times = new long[loopCount];
		long[] times1 = new long[loopCount];
		
		float f = 100;
		
		long startTime1;
		long totalTime1 = System.currentTimeMillis();
		
		for(int i = 0; i < loopCount; i++){
			startTime1 = System.currentTimeMillis();
			for(int j = 0; j < 1000000; j++){
				f +=  getArr(r.nextInt(16));
			}
			times1[i] = System.currentTimeMillis() - startTime1;
		}
		totalTime1 = System.currentTimeMillis() - totalTime1;
		System.out.println(f);
		
		long startTime;
		long totalTime = System.currentTimeMillis();
		
		for(int i = 0; i < loopCount; i++){
			startTime = System.currentTimeMillis();
			for(int j = 0; j < 1000000; j++){
				f +=  getSwich(r.nextInt(16));
			}
			times[i] = System.currentTimeMillis() - startTime;
		}
		totalTime = System.currentTimeMillis() - totalTime;
		System.out.println(f);
		
		System.out.println("result:");
		for(int i = 0; i < loopCount; i++){
			System.out.println(String.format("Loop: %d, %d, %d", i, times[i], times1[i]));
		}
		System.out.println("swich totalTime: " + totalTime);
		System.out.println("arr totalTime: " + totalTime1);
		
		System.out.println("swich averange time: " + totalTime/loopCount);
		System.out.println("arr averange time: " + totalTime1/loopCount);
	}
	
	static float getSwich(int index){
		switch (index) {
		case 0:  return arr[0];
		case 1:  return arr[1];
		case 2:  return arr[2];
		case 3:  return arr[3];
		case 4:  return arr[4];
		case 5:  return arr[5];
		case 6:  return arr[6];
		case 7:  return arr[7];
		case 8:  return arr[8];
		case 9:  return arr[9];
		case 10:  return arr[10];
		case 11:  return arr[11];
		case 12:  return arr[12];
		case 13:  return arr[13];
		case 14:  return arr[14];
		case 15:  return arr[15];
		default:
		      throw new IndexOutOfBoundsException();
		}
	}
	
	static float getArr(int index){
		return arr[index];
	}
	
	static void genGet(int dimension){
		// gen get(index, columnMajor)
		StringBuilder sb = new StringBuilder();
		sb.append("public float get(int index, boolean columnMajor) {\n");
		sb.append("\tswitch (index) {\n");
		for(int i = 0; i < dimension; i++){
			for(int j = 0; j < dimension; j++){
				int index = i * dimension + j;
				sb.append(getString(index, i, j));
			}
		}
		sb.append("\tdefault:\n");
		sb.append("\t	throw new IndexOutOfBoundsException(\"index = \" + index);\n");
		sb.append("\t}\n");
		sb.append("}\n");
		
		System.out.println(sb.toString());
		
		// gen set
		sb = new StringBuilder();
		sb.append("public float set(int index, float v, boolean columnMajor) {\n");
		sb.append("\tfloat t;\n");
		sb.append("\tif(columnMajor){\n");
		sb.append("\t\tswitch (index) {\n");
		for(int i = 0; i < dimension; i++){
			for(int j = 0; j < dimension; j++){
				int index = i * dimension + j;
				String str = String.format("\t\t case %d: t = m%d%d; m%d%d = v; return t;\n", index, i, j, i, j);
				sb.append(str);
			}
		}
		sb.append("\t\tdefault:\n");
		sb.append("\t\t	throw new IndexOutOfBoundsException(\"index = \" + index);\n");
		sb.append("\t\t}\n");
		sb.append("\t}else{\n");
		sb.append("\t\tswitch (index) {\n");
		for(int i = 0; i < dimension; i++){
			for(int j = 0; j < dimension; j++){
				int index = i * dimension + j;
				String str = String.format("\t\t case %d: t = m%d%d; m%d%d = v; return t;\n", index, j, i, j, i);
				sb.append(str);
			}
		}
		sb.append("\t\tdefault:\n");
		sb.append("\t\t	throw new IndexOutOfBoundsException(\"index = \" + index);\n");
		sb.append("\t\t}\n");
		sb.append("\t}\n");
		sb.append("}\n");
		
		System.out.println(sb);
		
		// gen get(i, j , colunmajor)
		sb = new StringBuilder();
		sb.append("public float get(int i, int j, boolean columnMajor) {\n");
		sb.append("\tswitch(i){\n");
		for(int i = 0; i < dimension; i++){
			sb.append("\tcase ").append(i).append(": \n");
			sb.append("\t\tswitch(j){\n");
			for(int j = 0; j < dimension; j++){
				sb.append('\t').append(getString(j, i, j));
			}
			sb.append("\t\tdefault:\n");
			sb.append("\t\t	throw new IndexOutOfBoundsException(\"j = \" + j);\n");
			sb.append("\t\t}\n");
		}
		sb.append("\tdefault:\n");
		sb.append("\t	throw new IndexOutOfBoundsException(\"i = \" + i);\n");
		sb.append("\t}\n");
		sb.append("}\n");
		
		System.out.println(sb);
		
		// gen set
		final String set_pattern = "\t\t\tcase %d: t = m%d%d; m%d%d = v; return t;\n";
		sb = new StringBuilder();
		sb.append("public float set(int i, int j, float v, boolean columnMajor) {\n");
		sb.append("\tfloat t;\n");
		sb.append("\tif(columnMajor){\n");
		sb.append("\t\tswitch (i) {\n");
		for(int i = 0; i < dimension; i++){
			sb.append("\t\tcase ").append(i).append(": \n");
			sb.append("\t\t\tswitch(j){\n");
			for(int j = 0; j < dimension; j++){
//				sb.append('\t').append(getString(j, i, j));
				sb.append(String.format(set_pattern, j, i, j, i, j));
			}
			sb.append("\t\t\tdefault:\n");
			sb.append("\t\t\t	throw new IndexOutOfBoundsException(\"j = \" + j);\n");
			sb.append("\t\t\t}\n");
		}
		sb.append("\t\tdefault:\n");
		sb.append("\t\t	throw new IndexOutOfBoundsException(\"i = \" + i);\n");
		sb.append("\t\t}\n");
		sb.append("\t}else{\n");
		sb.append("\t\tswitch (i) {\n");
		for(int i = 0; i < dimension; i++){
			sb.append("\t\tcase ").append(i).append(": \n");
			sb.append("\t\t\tswitch(j){\n");
			for(int j = 0; j < dimension; j++){
//				sb.append('\t').append(getString(j, i, j));
				sb.append(String.format(set_pattern, j, j, i, j, i));
			}
			sb.append("\t\t\tdefault:\n");
			sb.append("\t\t\t	throw new IndexOutOfBoundsException(\"j = \" + j);\n");
			sb.append("\t\t\t}\n");
		}
		sb.append("\t\tdefault:\n");
		sb.append("\t\t	throw new IndexOutOfBoundsException(\"i = \" + i);\n");
		sb.append("\t\t}\n");
		sb.append("\t}\n");
		sb.append("}");
		
		System.out.println(sb);
	}
	
	static String getString(int index, int i, int j){
		if(i == j){
			return String.format(returnSame, index, i, j);
		}else
			return String.format(returnUnsame, index, i, j, j, i);
	}
	
	static void gen(int dimension){
		final String loadArr = 
			"public Matrix%df load(float[] arr, int offset) {\n"
		 +  "%s" 
		 +  "\treturn this;\n}";
		
		final String loadArr2 = 
				"public Matrix%df load(float[][] arr) {\n"
				+ "%s"
			    + "\treturn this;\n}";
		
		final String loadTransposeArr = 
				"public Matrix%df loadTranspose(float[] arr, int offset) {\n"
				+"%s"
			    + "\treturn this;\n}";
		
		final String loadTransposeArr2 = 
				"public Matrix%df loadTranspose(float[][] arr) {\n"
				+"%s"
			    + "\treturn this;\n}";
		
		final String storeArr = 
				"public Matrix%df store(float[] arr, int offset) {\n"
		        +"%s"
		        + "\treturn this;\n}";
		
		final String storeArr2 = 
				"public Matrix%df store(float[][] arr) {\n"
		        +"%s"
		        + "\treturn this;\n}";
		
		final String storeTransposeArr = 
				"public Matrix%df storeTranspose(float[] arr, int offset) {\n"
		        +"%s"
		        + "\treturn this;\n}";
		
		final String storeTransposeArr2 = 
				"public Matrix%df storeTranspose(float[][] arr) {\n"
		        +"%s"
		        + "\treturn this;\n}";
		
		final String arr2 = "arr[%d][%d]";
		
		StringBuilder output = new StringBuilder(512);
		StringBuilder sb = new StringBuilder();
		
		{// gen loadArr
			sb.delete(0, sb.length()); //clear buffer
			for(int i = 0; i < dimension; i++){
				for(int j = 0; j < dimension; j++){
				sb.append("\tm").append(formatDigit(i,j)).append(" = ").append("arr[offset++];\n");
				}
			}
			
			output.append(String.format(loadArr, dimension, sb.toString())).append('\n');
		}
		
		{ // gen loadArr2
			sb.delete(0, sb.length()); //clear buffer
			for(int i = 0; i < dimension; i++){
				for(int j = 0; j < dimension; j++){
					sb.append("\tm").append(formatDigit(i,j)).append(" = ").append(String.format(arr2, i, j)).append(";\n");
				}
			}
			
			output.append(String.format(loadArr2, dimension, sb.toString())).append('\n');
		}
		
		{// gen loadTransposArr
			sb.delete(0, sb.length()); //clear buffer
			for(int i = 0; i < dimension; i++){
				for(int j = 0; j < dimension; j++){
				sb.append("\tm").append(formatDigit(j,i)).append(" = ").append("arr[offset++];\n");
				}
			}
			
			output.append(String.format(loadTransposeArr, dimension, sb.toString())).append('\n');
		}
		
		{// gen loadTransposArr2
			sb.delete(0, sb.length()); //clear buffer
			for(int i = 0; i < dimension; i++){
				for(int j = 0; j < dimension; j++){
				sb.append("\tm").append(formatDigit(j,i)).append(" = ").append(String.format(arr2, i, j)).append(";\n");
				}
			}
			
			output.append(String.format(loadTransposeArr2, dimension, sb.toString())).append('\n');
		}
		
		{// gen storeArr
			sb.delete(0, sb.length()); //clear buffer
			for(int i = 0; i < dimension; i++){
				for(int j = 0; j < dimension; j++){
				sb.append("\tarr[offset++]").append(" = m").append(formatDigit(i,j)).append(";\n");
				}
			}
			
			output.append(String.format(storeArr, dimension, sb.toString())).append('\n');
		}
		
		{// gen storeTransposeArr
			sb.delete(0, sb.length()); //clear buffer
			for(int i = 0; i < dimension; i++){
				for(int j = 0; j < dimension; j++){
				sb.append("\tarr[offset++]").append(" = m").append(formatDigit(j,i)).append(";\n");
				}
			}
			
			output.append(String.format(storeTransposeArr, dimension, sb.toString())).append('\n');
		}
		
		{// gen storeArr2
			sb.delete(0, sb.length()); //clear buffer
			for(int i = 0; i < dimension; i++){
				for(int j = 0; j < dimension; j++){
				sb.append('\t').append(String.format(arr2, i,j)).append(" = m").append(formatDigit(i,j)).append(";\n");
				}
			}
			
			output.append(String.format(storeArr2, dimension, sb.toString())).append('\n');
		}
		
		{// gen storeTransposeArr2
			sb.delete(0, sb.length()); //clear buffer
			for(int i = 0; i < dimension; i++){
				for(int j = 0; j < dimension; j++){
				sb.append('\t').append(String.format(arr2, i,j)).append(" = m").append(formatDigit(j,i)).append(";\n");
				}
			}
			
			output.append(String.format(storeTransposeArr2, dimension, sb.toString())).append('\n');
		}
		
		System.out.println(output);
	}
	
	static void genVertex(int dimension){
		final String loadArr = 
				"public Vector%df load(float[] arr, int offset){\n"
				+ "%s"
				+ "\treturn this;\t\n}\n\n";
		final String storeArr = 
				"public Vector%df store(float[] arr, int offset){\n"
				+ "%s"
				+ "\treturn this;\t\n}\n\n";
		
		final String[] comps = {"x", "y", "z", "w"};
		
		StringBuilder output = new StringBuilder();
		StringBuilder sb = new StringBuilder();
		
		{// loadArr
			for(int i = 0; i < dimension; i++){
				sb.append('\t').append(comps[i]).append(" = arr[offset++];\n");
			}
			
			output.append(String.format(loadArr, dimension, sb.toString()));
		}
		
		{// storeArr
			sb.delete(0, sb.length());
			for(int i = 0; i < dimension; i++){
//				sb.append('\t').append(comps[i]).append(" = arr[offset++];\n");
				sb.append("\tarr[offset++] = ").append(comps[i]).append(";\n");
			}
			
			output.append(String.format(storeArr, dimension, sb.toString()));
		}
		
		System.out.println(output);
		 
	}
	
	static String formatDigit(int i){
		return i < 10 ? "0" + i : "" + i;
	}
	
	static String formatDigit(int i, int j){
		return i + "" + j;
	}
}
