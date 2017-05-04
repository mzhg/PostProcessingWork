package com.nvidia.developer.opengl.demos.hdr;

/**
 * Created by mazhen'gui on 2017/3/17.
 */

final class CubeData {
    private static final float TEX_COORD_MINX = 0.0f;
    private static final float TEX_COORD_MAXX = 1.0f;
    private static final float TEX_COORD_MINY = 1.0f;
    private static final float TEX_COORD_MAXY = 0.0f;
    private static final float CUBE_SCLAE = 6.0f;

    // Interleaved vertex data
    static final float verticesCube[] = {
            // Front Face
            -CUBE_SCLAE,-CUBE_SCLAE, CUBE_SCLAE,
            0.0f, 0.0f, 1.0f,
            TEX_COORD_MINX, TEX_COORD_MINY,
            CUBE_SCLAE,-CUBE_SCLAE, CUBE_SCLAE,
            0.0f, 0.0f, 1.0f,
            TEX_COORD_MAXX, TEX_COORD_MINY,
            CUBE_SCLAE, CUBE_SCLAE, CUBE_SCLAE,
            0.0f, 0.0f, 1.0f,
            TEX_COORD_MAXX, TEX_COORD_MAXY,
            -CUBE_SCLAE, CUBE_SCLAE, CUBE_SCLAE,
            0.0f, 0.0f, 1.0f,
            TEX_COORD_MINX, TEX_COORD_MAXY,

            // Back Face
            -CUBE_SCLAE,-CUBE_SCLAE,-CUBE_SCLAE,
            0.0f, 0.0f,-1.0f,
            TEX_COORD_MINX, TEX_COORD_MINY,
            CUBE_SCLAE,-CUBE_SCLAE,-CUBE_SCLAE,
            0.0f, 0.0f,-1.0f,
            TEX_COORD_MAXX, TEX_COORD_MINY,
            CUBE_SCLAE, CUBE_SCLAE,-CUBE_SCLAE,
            0.0f, 0.0f,-1.0f,
            TEX_COORD_MAXX, TEX_COORD_MAXY,
            -CUBE_SCLAE, CUBE_SCLAE,-CUBE_SCLAE,
            0.0f, 0.0f,-1.0f,
            TEX_COORD_MINX, TEX_COORD_MAXY,

            // Top Face
            -CUBE_SCLAE, CUBE_SCLAE, CUBE_SCLAE,
            0.0f, 1.0f, 0.0f,
            TEX_COORD_MINX, TEX_COORD_MINY,
            CUBE_SCLAE, CUBE_SCLAE, CUBE_SCLAE,
            0.0f, 1.0f, 0.0f,
            TEX_COORD_MAXX, TEX_COORD_MINY,
            CUBE_SCLAE, CUBE_SCLAE,-CUBE_SCLAE,
            0.0f, 1.0f, 0.0f,
            TEX_COORD_MAXX, TEX_COORD_MAXY,
            -CUBE_SCLAE, CUBE_SCLAE,-CUBE_SCLAE,
            0.0f, 1.0f, 0.0f,
            TEX_COORD_MINX, TEX_COORD_MAXY,

            // Bottom Face
            -CUBE_SCLAE,-CUBE_SCLAE, CUBE_SCLAE,
            0.0f,-1.0f, 0.0f,
            TEX_COORD_MINX, TEX_COORD_MINY,
            CUBE_SCLAE,-CUBE_SCLAE, CUBE_SCLAE,
            0.0f,-1.0f, 0.0f,
            TEX_COORD_MAXX, TEX_COORD_MINY,
            CUBE_SCLAE,-CUBE_SCLAE,-CUBE_SCLAE,
            0.0f,-1.0f, 0.0f,
            TEX_COORD_MAXX, TEX_COORD_MAXY,
            -CUBE_SCLAE,-CUBE_SCLAE,-CUBE_SCLAE,
            0.0f,-1.0f, 0.0f,
            TEX_COORD_MINX, TEX_COORD_MAXY,

            // Left Face
            -CUBE_SCLAE,-CUBE_SCLAE,-CUBE_SCLAE,
            -1.0f, 0.0f, 0.0f,
            TEX_COORD_MINX, TEX_COORD_MINY,
            -CUBE_SCLAE,-CUBE_SCLAE, CUBE_SCLAE,
            -1.0f, 0.0f, 0.0f,
            TEX_COORD_MAXX, TEX_COORD_MINY,
            -CUBE_SCLAE, CUBE_SCLAE, CUBE_SCLAE,
            -1.0f, 0.0f, 0.0f,
            TEX_COORD_MAXX, TEX_COORD_MAXY,
            -CUBE_SCLAE, CUBE_SCLAE,-CUBE_SCLAE,
            -1.0f, 0.0f, 0.0f,
            TEX_COORD_MINX, TEX_COORD_MAXY,

            // Right Face
            CUBE_SCLAE,-CUBE_SCLAE,-CUBE_SCLAE,
            1.0f, 0.0f, 0.0f,
            TEX_COORD_MINX, TEX_COORD_MINY,
            CUBE_SCLAE,-CUBE_SCLAE, CUBE_SCLAE,
            1.0f, 0.0f, 0.0f,
            TEX_COORD_MAXX, TEX_COORD_MINY,
            CUBE_SCLAE, CUBE_SCLAE, CUBE_SCLAE,
            1.0f, 0.0f, 0.0f,
            TEX_COORD_MAXX, TEX_COORD_MAXY,
            CUBE_SCLAE, CUBE_SCLAE,-CUBE_SCLAE,
            1.0f, 0.0f, 0.0f,
            TEX_COORD_MINX, TEX_COORD_MAXY
    };

    static final short indicesCube[] = {0,1,3,3,1,2,
            4,7,5,7,6,5,
            8,9,11,11,9,10,
            12,15,13,15,14,13,
            16,17,19,19,17,18,
            20,23,21,23,22,21};
}
