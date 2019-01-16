package jet.opengl.demos.nvidia.waves.wavelets;

import org.lwjgl.util.vector.ReadableVector2f;
import org.lwjgl.util.vector.Vector2f;

import java.io.IOException;
import java.util.StringTokenizer;

import jet.opengl.postprocessing.util.FileUtils;
import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.StackFloat;

final class Environment {
    float _dx;

    static float[] harbor_data;
    private static int N;
    Environment(float size){
        if(harbor_data == null){
            harbor_data = load_data("harbor_data.cpp");
            N = (int) Math.sqrt(harbor_data.length);
        }

        _dx = ((2 * size) / N);
    }

    private static float[] load_data(String filename){
        final String root = "nvidia\\Wavelets\\models\\";
        try {
            StringBuilder source = FileUtils.loadText(root + filename);
            StackFloat values = new StackFloat(1024);

            int start = source.indexOf("{");
            int end = source.lastIndexOf("}");

            StringTokenizer tokenizer = new StringTokenizer(source.substring(start + 1, end), ",");
            while (tokenizer.hasMoreElements()){
                values.push(Float.parseFloat(tokenizer.nextToken()));
            }
            values.trim();
            return values.getData();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static float data_grid(int i, int j){
        // outside of the data grid just return some high arbitrary number
        if (i < 0 || i >= N || j < 0 || j >= N)
            return 100;

        return harbor_data[j + i * N];
    }

    private static float dx_data_grid(int i, int j){
        if (i < 0 || i >= (N - 1) || j < 0 || j >= N)
            return 0;

        return harbor_data[j + (i + 1) * N] - harbor_data[j + i * N];
    };

    private static float dy_data_grid(int i, int j){
        if (i < 0 || i >= N || j < 0 || j >= (N - 1))
            return 0;

        return harbor_data[(j + 1) + i * N] - harbor_data[j + i * N];
    }

    interface Fun{
        float eval(int i, int j);
    }

    static float linearInterpolation(Fun fun, float x, float y){
        int ix = (int)Math.floor(x);
        float wx = x - ix;

        int iy = (int)Math.floor(y);
        float wy = y - iy;

        float x0 = (wx != 0 ? wx * fun.eval(ix + 1, iy) : 0) +
                (wx != 1 ? (1 - wx) * fun.eval(ix, iy) : 0);

        float x1 = (wx != 0 ? wx * fun.eval(ix + 1, iy+1) : 0) +
                (wx != 1 ? (1 - wx) * fun.eval(ix, iy+1) : 0);

        return Numeric.mix(x0, x1, wy);
    }

    private static float grid(float x, float y, float dx){
        x *= 1/dx;
        y *= 1/dx;

        x += N/2 - 0.5f;
        y += N/2 - 0.5f;

        return linearInterpolation(Environment::data_grid, x, y);
    }

    private static float grid_dx(float x, float y, float dx){
        x *= 1/dx;
        y *= 1/dx;

        x += N/2 - 1.0f;
        y += N/2 - 0.5f;

        return linearInterpolation(Environment::dx_data_grid, x, y);
    }

    private static float grid_dy(float x, float y, float dx){
        x *= 1/dx;
        y *= 1/dx;

        x += N/2 - 0.5f;
        y += N/2 - 1.0f;

        return linearInterpolation(Environment::dy_data_grid, x, y);
    }

    boolean inDomain(float x, float y) {
        return levelset(x, y) >= 0;
    }

    float levelset(ReadableVector2f pos){
        return levelset(pos.getX(), pos.getY());
    }

    float levelset(float x, float y) {
        return grid(x,y, _dx);
    }

    Vector2f levelsetGrad(float x, float y) {
        Vector2f grad = new Vector2f(grid_dx(x,y, _dx), grid_dy(x,y, _dx));
        grad.normalise();
        return grad;
    }
}
