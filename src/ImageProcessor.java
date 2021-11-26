import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ImageProcessor {
    private final Image image;
    private List<Block> yBlocks;
    private List<Block> uBlocks;
    private List<Block> vBlocks;


    private final double[][] Q = {
            {6, 4, 4, 6, 10, 16, 20, 24},
            {5, 5, 6, 8, 10, 23, 24, 22},
            {6, 5, 6, 10, 16, 23, 28, 22},
            {6, 7, 9, 12, 20, 35, 32, 25},
            {7, 9, 15, 22, 27, 44, 41, 31},
            {10, 14, 22, 26, 32, 42, 45, 37},
            {20, 26, 31, 35, 41, 48, 48, 40},
            {29, 37, 38, 39, 45, 40, 41, 40}
    };

    private final double sqrtConstant = 1 / Math.sqrt(2.0);
    private final double constant = 1 / 4.0;
    private final double PI = Math.PI;

    public ImageProcessor(Image image) {
        this.image = image;
        encodeImage(image);
        decodeImage(image);
    }

    // Decoding part
    // Decoding part
    // Decoding part
    // Decoding part
    private void decodeImage(Image image) {
        System.out.println("Decoding image");
        writeBlocks(yBlocks, "yBlocks_upSampled");
        writeBlocks(vBlocks, "vBlocks_upSampled");
        writeBlocks(uBlocks, "uBlocks_upSampled");
        Thread thread1 = new Thread(() -> {
            deQuantization(yBlocks);
            IDCT(yBlocks);
            add128(yBlocks);
            image.setY(decodeBlock(yBlocks));
        });
        Thread thread2 = new Thread(() -> {
            deQuantization(uBlocks);
            IDCT(uBlocks);
            add128(uBlocks);
            image.setU(decodeBlock(uBlocks));
        });
        Thread thread3 = new Thread(() -> {
            deQuantization(vBlocks);
            IDCT(vBlocks);
            add128(vBlocks);
            image.setV(decodeBlock(vBlocks));
        });
        thread1.start();
        thread2.start();
        thread3.start();
        try {
            thread1.join();
            thread2.join();
            thread3.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void add128(List<Block> blocks) {
        for (Block block : blocks)
            for (int i = 0; i < 8; i++)
                for (int j = 0; j < 8; j++) block.getMatrix()[i][j] += 128.0;
    }

    private double[][] decodeBlock(List<Block> encoded) {
        System.out.printf("Decoding block of type %s%n", encoded.get(1).getColorType());
        System.out.printf("Decoding %s number of blocks%n", encoded.size());

        double[][] matrix = new double[image.getHeight()][image.getWidth()];

        int line = 0;
        int column = 0;
        for (Block block : encoded) {
            for (int i = 0; i < 8; i++)
                for (int j = 0; j < 8; j++)
                    matrix[line + i][column + j] = block.getMatrix()[i][j];

            column += 8;

            if (column == image.getWidth()) {
                line += 8;
                column = 0;
            }
        }
        return matrix;
    }

    private void IDCT(List<Block> blocks) {
        for (Block block : blocks) block.setMatrix(inverseDCT(block.getMatrix()));
    }

    private double[][] inverseDCT(double[][] matrix) {
        double[][] f = new double[8][8];
        for (int x = 0; x < 8; x++)
            for (int y = 0; y < 8; y++) f[x][y] = constant * firstSumIDCT(matrix, x, y);
        return f;
    }

    private double firstSumIDCT(double[][] matrix, int x, int y) {
        double sum = 0.0;
        for (int u = 0; u < 8; u++) sum += secondSumIDCT(matrix, x, y, u);
        return sum;
    }

    private double secondSumIDCT(double[][] matrix, int x, int y, int u) {
        double sum = 0.0;
        for (int v = 0; v < 8; v++) sum += cosineProductIDCT(matrix[u][v], x, y, u, v);
        return sum;
    }

    private double cosineProductIDCT(double value, int x, int y, int u, int v) {
        double cosX = Math.cos(((2 * x + 1) * u * PI) / 16);
        double cosY = Math.cos(((2 * y + 1) * v * PI) / 16);
        return alpha(u) * alpha(v) * value * cosX * cosY;
    }

    private void deQuantization(List<Block> blocks) {
        for (Block block : blocks) block.setMatrix(multiplyMatrix(block.getMatrix(), Q));
    }

    private double[][] multiplyMatrix(double[][] matrix, double[][] Q) {
        double[][] deQuantization = new double[8][8];
        for (int i = 0; i < 8; i++)
            for (int j = 0; j < 8; j++) deQuantization[i][j] = (int) (matrix[i][j] * Q[i][j]);
        return deQuantization;
    }

    // Encoding part
    // Encoding part
    // Encoding part
    // Encoding part
    // Encoding part
    private void encodeImage(Image image) {
        System.out.println("Encoding image");
        yBlocks = divideMatrix(image, "Y", image.getY());
        uBlocks = divideMatrix(image, "U", image.getU());
        vBlocks = divideMatrix(image, "V", image.getV());

        writeBlocks(yBlocks, "yBlocks");
        writeBlocks(vBlocks, "vBlocks");
        writeBlocks(uBlocks, "uBlocks");

        uBlocks = upSampleBlocks(uBlocks);
        vBlocks = upSampleBlocks(vBlocks);

        Thread thread1 = new Thread(() -> {
            substract128(yBlocks);
            FDCT(yBlocks);
            quantization(yBlocks);
        });
        Thread thread2 = new Thread(() -> {
            substract128(uBlocks);
            FDCT(uBlocks);
            quantization(uBlocks);
        });
        Thread thread3 = new Thread(() -> {
            substract128(vBlocks);
            FDCT(vBlocks);
            quantization(vBlocks);
        });
        thread1.start();
        thread2.start();
        thread3.start();

        try {
            thread1.join();
            thread2.join();
            thread3.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void FDCT(List<Block> blocks) {
        for (Block block : blocks) block.setMatrix(forwardDCT(block.getMatrix()));
    }

    private double[][] forwardDCT(double[][] matrix) {
        double[][] G = new double[8][8];
        for (int u = 0; u < 8; u++)
            for (int v = 0; v < 8; v++) G[u][v] = constant * alpha(u) * alpha(v) * firstSumFDCT(matrix, u, v);
        return G;
    }

    private double firstSumFDCT(double[][] matrix, int u, int v) {
        double sum = 0.0;
        for (int x = 0; x < 8; x++) sum += secondSumFDCT(matrix, u, v, x);
        return sum;
    }

    private double secondSumFDCT(double[][] matrix, int u, int v, int x) {
        double sum = 0.0;
        for (int y = 0; y < 8; y++) sum += cosineProductFDCT(matrix[x][y], x, y, u, v);
        return sum;
    }

    private double cosineProductFDCT(double value, int x, int y, int u, int v) {
        double cosX = Math.cos(((2 * x + 1) * u * PI) / 16);
        double cosY = Math.cos(((2 * y + 1) * v * PI) / 16);
        return value * cosX * cosY;
    }

    private double alpha(int value) {
        return value > 0 ? 1 : sqrtConstant;
    }

    private void substract128(List<Block> encoded) {
        for (Block block : encoded)
            for (int i = 0; i < 8; i++)
                for (int j = 0; j < 8; j++)
                    block.getMatrix()[i][j] -= 128.0;
    }

    private void quantization(List<Block> blocks) {
        for (Block block : blocks) {
            block.setMatrix(divideQuantization(block.getMatrix(), Q));
        }
    }

    private double[][] divideQuantization(double[][] matrix, double[][] Q) {
        double[][] quantization = new double[8][8];
        for (int i = 0; i < 8; i++)
            for (int j = 0; j < 8; j++)
                quantization[i][j] = (int) (matrix[i][j] / Q[i][j]);
        return quantization;
    }

    private List<Block> upSampleBlocks(List<Block> encoded) {
        List<Block> upSampled = new ArrayList<>();
        encoded.forEach(block -> upSampled.add(upSampling(block)));
        return upSampled;
    }


    private List<Block> divideMatrix(Image image, String type, double[][] matrix) {
        System.out.printf("Dividing block of type %s%n", type);

        List<Block> encoded = new ArrayList<>();
        for (int i = 0; i < image.getHeight(); i += 8) {
            for (int j = 0; j < image.getWidth(); j += 8) {
                Block block = subMatrix8x8(type, i, j, matrix);
                if (type.equals("Y"))
                    encoded.add(block);
                else
                    encoded.add(subSampling(block));
            }
        }
        System.out.printf("Encoded size %s%n", encoded.size());
        return encoded;
    }

    public Block subSampling(Block toSample) {
        Block sampled = new Block(4, toSample.getColorType());
        int line = 0;
        int column = 0;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                sampled.getMatrix()[i][j] = (toSample.getMatrix()[line][column] +
                        toSample.getMatrix()[line][column + 1] +
                        toSample.getMatrix()[line + 1][column] +
                        toSample.getMatrix()[line + 1][column + 1])
                        / 4.0;
                column += 2;
            }
            line += 2;
            column = 0;
        }
        return sampled;
    }

    public Block upSampling(Block toSample) {
        Block sample = new Block(8, toSample.getColorType());
        int line = 0;
        int column = 0;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                double value = toSample.getMatrix()[i][j];
                sample.getMatrix()[line][column] = value;
                sample.getMatrix()[line][column + 1] = value;
                sample.getMatrix()[line + 1][column] = value;
                sample.getMatrix()[line + 1][column + 1] = value;
                column += 2;
            }
            line += 2;
            column = 0;
        }
        return sample;
    }

    public Block subMatrix8x8(String type, int iPos, int jPos, double[][] matrix) {
        Block block = new Block(8, type);
        for (int i = 0; i < 8; i++)
            for (int j = 0; j < 8; j++)
                block.getMatrix()[i][j] = matrix[i + iPos][j + jPos];
        return block;
    }

    private void writeBlocks(List<Block> blocks, String fileName) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("output/" + fileName + ".txt"));
            blocks.forEach(block -> {
                try {
                    writer.write(block.toString());
                    writer.write("\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Image getImage() {
        return this.image;
    }
}
