package com.example.pbrt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

import jet.opengl.postprocessing.util.CommentFilter;
import jet.opengl.postprocessing.util.StackDouble;

public class MyClass {

    public static void main(String[] a){
        load_static_data();
    }

    private enum FileState{
        None,
        Begin,
        Process,
        End,
    }

    static void load_static_data(){
        StringBuilder loadDataArray = new StringBuilder();
        StringBuilder sourceArray = new StringBuilder();

        try(BufferedReader in = new BufferedReader(new FileReader(new File("E:\\SDK\\HosekWilkie_SkylightModel_C_Source.1.4a\\ArHosekSkyModelData_Spectral.h")))){
            CommentFilter filter = new CommentFilter(in);
            String line;

            FileState state = FileState.None;
            StackDouble parsedValues = new StackDouble();

            ArrayName name=null;
            while ((line = filter.nextLine()) != null){
                line = line.trim();
                if(line.length() == 0)
                    continue;

                switch (state){
                    case None:
                        name = getName(line);
                        if(name.isArray){
                            sourceArray.append(line).append('\n');
                        }else{
                            loadDataArray.append("static double[] ").append(name.name).append(";\n");
                        }

                        state = FileState.Begin;
                        break;
                    case Begin:
                        int endIdx = line.indexOf('}');
                        if(endIdx >= 0){
                            if(name.isArray)
                                sourceArray.append(line).append('\n');
                            else{
                                StringTokenizer tokenizer = new StringTokenizer(line, " \t{};,");
                                StackDouble floats = new StackDouble(128);
                                while (tokenizer.hasMoreElements()){
                                    floats.push(Double.parseDouble(tokenizer.nextToken()));
                                }

                                floats.trim();
                                assign(name.name, floats.getData());
                            }

                            state = FileState.None;  // goto the none state
                            parsedValues.clear();
                        }else{
                            if(!line.equals("{"))
                                throw new IllegalArgumentException("Inner error!!!");

                            if(name.isArray)
                                sourceArray.append(line).append('\n');

                            state = FileState.Process;
                        }
                        break;
                    case Process:
                        if(!line.contains("}")){
                            if(name.isArray){
                                sourceArray.append(line).append('\n');
                            }else{
                                double v = Double.parseDouble(line.substring(0, line.length() - 1));
                                parsedValues.push(v);
                            }
                        }else{
                            if(name.isArray){
                                sourceArray.append(line).append('\n');
                            }else {
                                // end
                                assign(name.name, parsedValues.copy().getData());
                                parsedValues.clear();
                            }

                            state = FileState.None;  // goto the none state
                        }
                        break;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.print(loadDataArray);
        System.out.print(sourceArray);
    }

    private static void assign(String name, double[] a){
        /*Field field = DebugTools.getField(ArHosekSkyModel.class, "name");
        try {
            field.set(null, a);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }*/
    }

    private static ArrayName getName(String line){
        StringTokenizer tokenizer = new StringTokenizer(line, " ");
        ArrayName name = new ArrayName();
        String typeName = tokenizer.nextToken();
        String arrayName = tokenizer.nextToken();
        name.isArray = typeName.indexOf('*') >= 0;
        name.name = arrayName.substring(0, arrayName.length() - 2);

        return name;
    }

    private static final class ArrayName{
        String name;
        boolean isArray;
    }
}
