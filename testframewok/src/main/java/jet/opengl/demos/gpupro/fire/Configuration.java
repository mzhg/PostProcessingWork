package jet.opengl.demos.gpupro.fire;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import jet.opengl.postprocessing.util.FileUtils;

/**
 * Created by mazhen'gui on 2017/10/28.
 */

final class Configuration {
    float mainTimetick;
    float cameraSpeed;
    int sequence1shift;
    int sequence2shift;
    float initAngSeq1;
    float initAngSeq2;
    float angIncrSeq1;
    float angIncrSeq2;

    float heatHazeSpeed,
            timeGapClouds,
            halfSizeInit,
            halfSizeAmpl,
            initHorizSpiralRadius,
            spiralParameter,
            spiralSpeedAmpl,
            vertSpeedInit,
            vertSpeedAmpl,
            turbulenceSpeed,
            lifetimeAmpl,
            rescaleDistTex;
    int maxDistort;

    boolean load(String filename){
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(FileUtils.open(filename)));
            StringTokenizer token = new StringTokenizer(in.readLine());
            mainTimetick = Float.parseFloat(token.nextToken());

            token = new StringTokenizer(in.readLine());
            cameraSpeed = Float.parseFloat(token.nextToken());

            token = new StringTokenizer(in.readLine());
            sequence1shift = Integer.parseInt(token.nextToken());

            token = new StringTokenizer(in.readLine());
            sequence2shift = Integer.parseInt(token.nextToken());

            token = new StringTokenizer(in.readLine());
            angIncrSeq1 = Float.parseFloat(token.nextToken());
            initAngSeq1 = Float.parseFloat(token.nextToken());

            token = new StringTokenizer(in.readLine());
            angIncrSeq2 = Float.parseFloat(token.nextToken());
            initAngSeq2 = Float.parseFloat(token.nextToken());
            in.readLine();

            token = new StringTokenizer(in.readLine());
            heatHazeSpeed = Float.parseFloat(token.nextToken());

            token = new StringTokenizer(in.readLine());
            timeGapClouds = Float.parseFloat(token.nextToken());

            token = new StringTokenizer(in.readLine());
            halfSizeInit = Float.parseFloat(token.nextToken());

            token = new StringTokenizer(in.readLine());
            halfSizeAmpl = Float.parseFloat(token.nextToken());

            token = new StringTokenizer(in.readLine());
            initHorizSpiralRadius = Float.parseFloat(token.nextToken());

            token = new StringTokenizer(in.readLine());
            spiralParameter = Float.parseFloat(token.nextToken());

            token = new StringTokenizer(in.readLine());
            spiralSpeedAmpl = Float.parseFloat(token.nextToken());

            token = new StringTokenizer(in.readLine());
            vertSpeedInit = Float.parseFloat(token.nextToken());

            token = new StringTokenizer(in.readLine());
            vertSpeedAmpl = Float.parseFloat(token.nextToken());

            token = new StringTokenizer(in.readLine());
            turbulenceSpeed = Float.parseFloat(token.nextToken());

            token = new StringTokenizer(in.readLine());
            lifetimeAmpl = Float.parseFloat(token.nextToken());

            token = new StringTokenizer(in.readLine());
            rescaleDistTex = Float.parseFloat(token.nextToken());

            token = new StringTokenizer(in.readLine());
            maxDistort = Integer.parseInt(token.nextToken());

            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }
}
