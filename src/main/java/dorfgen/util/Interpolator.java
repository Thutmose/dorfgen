package dorfgen.util;

public class Interpolator
{

    public static class CubicInterpolator
    {
        public static double getValue(final double[] p, final double x)
        {
            return p[1] + 0.5 * x * (p[2] - p[0] + x * (2.0 * p[0] - 5.0 * p[1] + 4.0 * p[2] - p[3] + x * (3.0 * (p[1]
                    - p[2]) + p[3] - p[0])));
        }
    }

    public static class BicubicInterpolator extends CubicInterpolator
    {
        private final double[] arr = new double[4];

        public double getValue(final double[][] p, final double x, final double y)
        {
            this.arr[0] = CubicInterpolator.getValue(p[0], y);
            this.arr[1] = CubicInterpolator.getValue(p[1], y);
            this.arr[2] = CubicInterpolator.getValue(p[2], y);
            this.arr[3] = CubicInterpolator.getValue(p[3], y);
            return CubicInterpolator.getValue(this.arr, x);
        }

        public int interpolate(final int[][] image, final int xAbs, final int yAbs, final int scale)
        {
            final int pixelX = xAbs / scale;
            final int pixelY = yAbs / scale;
            final double x = (xAbs - scale * pixelX) / (double) scale, y = (yAbs - scale * pixelY) / (double) scale;
            final double[][] arr = new double[4][4];
            for (int i = -1; i <= 2; i++)
                for (int k = -1; k <= 2; k++)
                {
                    final int locX = pixelX + i;
                    final int locY = pixelY + k;
                    if (locX >= 0 && locX < image.length && locY >= 0 && locY < image[0].length) arr[i + 1][k
                            + 1] = image[locX][locY];
                    else arr[i + 1][k + 1] = image[pixelX][pixelY];
                }
            return (int) Math.round(this.getValue(arr, x, y));
        }

        public int interpolateBiome(final int[][] image, final int xAbs, final int yAbs, final int scale)
        {
            final int pixelX = xAbs / scale;
            final int pixelY = yAbs / scale;

            if (pixelX >= image.length || pixelY >= image[0].length) return 0;

            int val = image[pixelX][pixelY];
            final double x = (xAbs - scale * pixelX) / (double) scale, y = (yAbs - scale * pixelY) / (double) scale;

            double max = -1;
            int index = -1;
            final int[] biomes = new int[16];
            for (int i = -1; i < 3; i++)
                for (int k = -1; k < 3; k++)
                {
                    final int locX = pixelX + i;
                    final int locY = pixelY + k;
                    if (locX >= 0 && locX < image.length && locY >= 0 && locY < image[0].length) biomes[i + 1 + (k + 1)
                            * 4] = image[locX][locY];
                }
            for (int n = 0; n < 16; n++)
            {
                final int num = biomes[n];
                final double[][] arr = new double[4][4];
                for (int i = 0; i < 16; i++)
                    if (biomes[i] == num) arr[i % 4][i / 4] = 10;
                    else arr[i % 4][i / 4] = 0;
                final double temp = this.getValue(arr, x, y);
                if (temp > max)
                {
                    max = temp;
                    index = n;
                }
            }
            if (index >= 0) val = biomes[index];

            return val;
        }
    }

    public static class CachedBicubicInterpolator
    {
        private double a00, a01, a02, a03;
        private double a10, a11, a12, a13;
        private double a20, a21, a22, a23;
        private double a30, a31, a32, a33;

        private final double[][] arr = new double[4][4];

        private int     lastX, lastY;
        private int[][] lastArr;

        public int interpolate(final double x, final double y)
        {
            return (int) Math.round(this.getValue(x, y));
        }

        public int interpolateHeight(final int scale, final int xAbs, final int yAbs, final int[][] image)
        {
            final int pixelX = xAbs / scale;
            final int pixelY = yAbs / scale;
            if (pixelX >= image.length || pixelY >= image[0].length) return 10;
            this.updateCoefficients(image, pixelX, pixelY);
            int val = image[pixelX][pixelY];
            final double x = (xAbs - scale * pixelX) / (double) scale, y = (yAbs - scale * pixelY) / (double) scale;
            val = this.interpolate(x, y);
            return val;
        }

        private void updateCoefficients(final int[][] image, final int pixelX, final int pixelY)
        {
            if (image == this.lastArr && pixelX == this.lastX && pixelY == this.lastY) return;
            this.lastArr = image;
            this.lastX = pixelX;
            this.lastY = pixelY;
            for (int i = 0; i < 16; i++)
                this.arr[i % 4][i / 4] = image[pixelX][pixelY];
            for (int i = -1; i <= 2; i++)
                for (int k = -1; k <= 2; k++)
                {
                    final int locX = pixelX + i;
                    final int locY = pixelY + k;
                    if (locX >= 0 && locX < image.length && locY >= 0 && locY < image[0].length) this.arr[i + 1][k
                            + 1] = image[locX][locY];
                }
            this.updateCoefficients(this.arr);
        }

        public void updateCoefficients(final double[][] p)
        {
            this.a00 = p[1][1];
            this.a01 = -.5 * p[1][0] + .5 * p[1][2];
            this.a02 = p[1][0] - 2.5 * p[1][1] + 2 * p[1][2] - .5 * p[1][3];
            this.a03 = -.5 * p[1][0] + 1.5 * p[1][1] - 1.5 * p[1][2] + .5 * p[1][3];
            this.a10 = -.5 * p[0][1] + .5 * p[2][1];
            this.a11 = .25 * p[0][0] - .25 * p[0][2] - .25 * p[2][0] + .25 * p[2][2];
            this.a12 = -.5 * p[0][0] + 1.25 * p[0][1] - p[0][2] + .25 * p[0][3] + .5 * p[2][0] - 1.25 * p[2][1]
                    + p[2][2] - .25 * p[2][3];
            this.a13 = .25 * p[0][0] - .75 * p[0][1] + .75 * p[0][2] - .25 * p[0][3] - .25 * p[2][0] + .75 * p[2][1]
                    - .75 * p[2][2] + .25 * p[2][3];
            this.a20 = p[0][1] - 2.5 * p[1][1] + 2 * p[2][1] - .5 * p[3][1];
            this.a21 = -.5 * p[0][0] + .5 * p[0][2] + 1.25 * p[1][0] - 1.25 * p[1][2] - p[2][0] + p[2][2] + .25
                    * p[3][0] - .25 * p[3][2];
            this.a22 = p[0][0] - 2.5 * p[0][1] + 2 * p[0][2] - .5 * p[0][3] - 2.5 * p[1][0] + 6.25 * p[1][1] - 5
                    * p[1][2] + 1.25 * p[1][3] + 2 * p[2][0] - 5 * p[2][1] + 4 * p[2][2] - p[2][3] - .5 * p[3][0] + 1.25
                            * p[3][1] - p[3][2] + .25 * p[3][3];
            this.a23 = -.5 * p[0][0] + 1.5 * p[0][1] - 1.5 * p[0][2] + .5 * p[0][3] + 1.25 * p[1][0] - 3.75 * p[1][1]
                    + 3.75 * p[1][2] - 1.25 * p[1][3] - p[2][0] + 3 * p[2][1] - 3 * p[2][2] + p[2][3] + .25 * p[3][0]
                    - .75 * p[3][1] + .75 * p[3][2] - .25 * p[3][3];
            this.a30 = -.5 * p[0][1] + 1.5 * p[1][1] - 1.5 * p[2][1] + .5 * p[3][1];
            this.a31 = .25 * p[0][0] - .25 * p[0][2] - .75 * p[1][0] + .75 * p[1][2] + .75 * p[2][0] - .75 * p[2][2]
                    - .25 * p[3][0] + .25 * p[3][2];
            this.a32 = -.5 * p[0][0] + 1.25 * p[0][1] - p[0][2] + .25 * p[0][3] + 1.5 * p[1][0] - 3.75 * p[1][1] + 3
                    * p[1][2] - .75 * p[1][3] - 1.5 * p[2][0] + 3.75 * p[2][1] - 3 * p[2][2] + .75 * p[2][3] + .5
                            * p[3][0] - 1.25 * p[3][1] + p[3][2] - .25 * p[3][3];
            this.a33 = .25 * p[0][0] - .75 * p[0][1] + .75 * p[0][2] - .25 * p[0][3] - .75 * p[1][0] + 2.25 * p[1][1]
                    - 2.25 * p[1][2] + .75 * p[1][3] + .75 * p[2][0] - 2.25 * p[2][1] + 2.25 * p[2][2] - .75 * p[2][3]
                    - .25 * p[3][0] + .75 * p[3][1] - .75 * p[3][2] + .25 * p[3][3];
        }

        public double getValue(final double x, final double y)
        {
            final double x2 = x * x;
            final double x3 = x2 * x;
            final double y2 = y * y;
            final double y3 = y2 * y;

            return this.a00 + this.a01 * y + this.a02 * y2 + this.a03 * y3 + (this.a10 + this.a11 * y + this.a12 * y2
                    + this.a13 * y3) * x + (this.a20 + this.a21 * y + this.a22 * y2 + this.a23 * y3) * x2 + (this.a30
                            + this.a31 * y + this.a32 * y2 + this.a33 * y3) * x3;
        }
    }
}
