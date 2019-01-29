package jet.opengl.demos.intel.clustered;

final class FragmentFactory {
    final long[] masks = new long[48];

    FragmentFactory(){
        for (int k = 0; k < 16; k++)
        {
            int b = k % 4;
            int a = k / 4;

            long one = 1;
            long x_segment = 0;
            long y_segment = 0;
            long z_segment = 0;

            for (int l = a; l <= b; l++)
                for (int m = 0; m < 16; m++)
                {
                    x_segment |= one << ((l / 2 % 2 + m / 8 % 2 * 2) * 16 + (l % 2 + m / 4 % 2 * 2) * 4 + (m % 4));
                    y_segment |= one << ((l / 2 % 2 * 2 + m / 8 % 2) * 16 + (l % 2 * 2 + m / 4 % 2) * 4 + (m % 4));
                    //x_segment |= one * 15 << (m * 4);
                    //y_segment |= one * 15 << (m * 4);
                    z_segment |= one << (m * 4 + l);
                }

            //x_segment = 0xFFFFFFFFFFFFFFFFull;
            //y_segment = 0xFFFFFFFFFFFFFFFFull;
            //z_segment = 0xFFFFFFFFFFFFFFFFull;

            masks[0 + k] = x_segment;
            masks[16 + k] = y_segment;
            masks[32 + k] = z_segment;
        }
    }

    long coverage(int x1, int x2, int y1, int y2, int z1, int z2)
    {
        long x_segment = masks[0 + x1 * 4 + x2];
        long y_segment = masks[16 + y1 * 4 + y2];
        long z_segment = masks[32 + z1 * 4 + z2];
        long coverage = x_segment & y_segment & z_segment;
        return coverage;
    }
}
