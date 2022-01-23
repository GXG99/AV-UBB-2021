import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ImageProcessor {
    private final Image image;
    private List<Block> yBlocks;
    private List<Block> uBlocks;
    private List<Block> vBlocks;
    private List<Block> yEntropy = new ArrayList<>();
    private List<Block> uEntropy = new ArrayList<>();
    private List<Block> vEntropy = new ArrayList<>();

    private HashMap<Integer, List<Integer>> amplitudes = new HashMap<>();
    private List<Integer> entropy = new ArrayList<>();
    private int pos;

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

        amplitudes.put(1, Arrays.asList(-1, 1));
        amplitudes.put(2, Arrays.asList(-3, -2, 2, 3));
        amplitudes.put(3, Arrays.asList(-7, -4, 4, 7));
        amplitudes.put(4, Arrays.asList(-15, -8, 8, 15));
        amplitudes.put(5, Arrays.asList(-31, -16, 16, 31));
        amplitudes.put(6, Arrays.asList(-63, -32, 32, 63));
        amplitudes.put(7, Arrays.asList(-127, -64, 64, 127));
        amplitudes.put(8, Arrays.asList(-225, -128, 128, 255));
        amplitudes.put(9, Arrays.asList(-511, -256, 256, 511));
        amplitudes.put(10, Arrays.asList(-1023, -512, 512, 1023));

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
//        writeBlocks(vBlocks, "vBlocks_upSampled");
//        writeBlocks(yBlocks, "yBlocks_upSampled");
//        writeBlocks(uBlocks, "uBlocks_upSampled");

        entropyDecoding();
        writeBlocks(yEntropy, "yEntropyBlocks");
        writeBlocks(uEntropy, "uEntropyBlocks");
        writeBlocks(vEntropy, "vEntropyBlocks");

        Thread thread1 = new Thread(() -> {
            deQuantization(yEntropy);
            IDCT(yEntropy);
            add128(yEntropy);
            image.setY(decodeBlock(yEntropy));

        });
        Thread thread2 = new Thread(() -> {
            deQuantization(uEntropy);
            IDCT(uEntropy);
            add128(uEntropy);
            image.setU(decodeBlock(uEntropy));

        });
        Thread thread3 = new Thread(() -> {
            deQuantization(vEntropy);
            IDCT(vEntropy);
            add128(vEntropy);
            image.setV(decodeBlock(vEntropy));

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

    private void entropyDecoding() {
        pos = 0;
        while (pos < entropy.size())
        {
            Block blockY = new Block(8, "Y");
            blockY.setIntegerMatrix(getBlock());
            yEntropy.add(blockY);

            Block blockU = new Block(8, "U");
            blockU.setIntegerMatrix(getBlock());
            uEntropy.add(blockU);

            Block blockV = new Block(8, "V");
            blockV.setIntegerMatrix(getBlock());
            vEntropy.add(blockV);
        }
    }

    private int[][] getBlock() {
        int[][] matrix = new int[8][8];

        pos++;
        matrix[0][0] = entropy.get(pos++);

        if (entropy.get(pos) == 0 && entropy.get(pos + 1) == 0) {
            pos += 2;
            return matrix;
        }

        //upper zig-zag

        int column = 0;
        int row = 0;

        do {
            column++;
            if (setMatrix(row, column, matrix)) return matrix;

            do {
                row++;
                column--;
                if (setMatrix(row, column, matrix)) return matrix;
            } while (column != 0);

            if (row == 7 )
                break;
            row++;
            if (setMatrix(row, column, matrix)) return matrix;
            do {
                row--;
                column++;
                if (setMatrix(row, column, matrix)) return matrix;
            } while (row != 0);
        } while (true);


        do {
            column++;
            if (setMatrix(row, column, matrix)) return matrix;
            if (column == 7)
                break;
            do {
                row--;
                column++;
                if (setMatrix(row, column, matrix)) return matrix;
            } while (column != 7);
            row++;
            if (setMatrix(row, column, matrix)) return matrix;
            do {
                row++;
                column--;
                if (setMatrix(row, column, matrix)) return matrix;
            } while (row != 7);
        } while (true);

        return matrix;
    }

    private boolean setMatrix(int row, int column, int[][] matrix) {

        if (entropy.get(pos) == 0 && entropy.get(pos + 1) == 0) {
            pos += 2;
            return true;
        }

        matrix[row][column] = entropy.get(pos) == 0 ? entropy.get(pos + 2): 0;
        if (entropy.get(pos) != 0)
            entropy.set(pos, entropy.get(pos) - 1);
        else
            pos += 3;
        return false;
    }

    private void add128(List<Block> blocks) {
        for (Block block : blocks)
            for (int i = 0; i < 8; i++)
                for (int j = 0; j < 8; j++) block.getIntegerMatrix()[i][j] += 128.0;
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
                    matrix[line + i][column + j] = block.getIntegerMatrix()[i][j];

            column += 8;

            if (column == image.getWidth()) {
                line += 8;
                column = 0;
            }
        }
        return matrix;
    }

    private void IDCT(List<Block> blocks) {
        for (Block block : blocks) block.setIntegerMatrix(inverseDCT(block.getIntegerMatrix()));
    }

    private int[][] inverseDCT(int[][] matrix) {
        int[][] f = new int[8][8];
        for (int x = 0; x < 8; x++)
            for (int y = 0; y < 8; y++) f[x][y] = (int) (constant * firstSumIDCT(matrix, x, y));
        return f;
    }

    private double firstSumIDCT(int[][] matrix, int x, int y) {
        double sum = 0.0;
        for (int u = 0; u < 8; u++) sum += secondSumIDCT(matrix, x, y, u);
        return sum;
    }

    private double secondSumIDCT(int[][] matrix, int x, int y, int u) {
        double sum = 0.0;
        for (int v = 0; v < 8; v++) sum += cosineProductIDCT(matrix[u][v], x, y, u, v);
        return sum;
    }

    private double cosineProductIDCT(int value, int x, int y, int u, int v) {
        double cosX = Math.cos(((2 * x + 1) * u * PI) / 16);
        double cosY = Math.cos(((2 * y + 1) * v * PI) / 16);
        return alpha(u) * alpha(v) * value * cosX * cosY;
    }

    private void deQuantization(List<Block> blocks) {
        for (Block block : blocks) block.setIntegerMatrix(multiplyMatrix(block.getIntegerMatrix(), Q));
    }

    private int[][] multiplyMatrix(int[][] matrix, double[][] Q) {
        int[][] deQuantization = new int[8][8];
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

        entropyEncoding();

        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter("./output/entropy.txt");
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.println(entropy);
            printWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void entropyEncoding() {
        for (int i = 0; i < yBlocks.size(); i++) {
            addEntropy(yBlocks.get(i).getIntegerMatrix());
            addEntropy(uBlocks.get(i).getIntegerMatrix());
            addEntropy(vBlocks.get(i).getIntegerMatrix());
        }
    }

    private void addEntropy(int[][] matrix) {
        int[] list = zigzagCrossing(matrix);

        int DC_size = getSize(list[0]);
        entropy.addAll(Arrays.asList(DC_size, list[0]));

        for(int i = 1; i < 64; i++) {
            int cnt = 0;
            while (list[i] == 0) {
                cnt++;
                i++;
                if (i == 64) {
                    break;
                }
            }
            if (i == 64)
                entropy.addAll(Arrays.asList(0, 0));
            else {
                entropy.addAll(Arrays.asList(cnt, getSize(list[i]), list[i]));
            }
        }
    }

    private int getSize(int value) {
        if (value == 0) return 0;
        for (Integer k : amplitudes.keySet())
            if (value == 1 || value == -1) {
                return 1;
            } else if (k != 1) {
                if (amplitudes.get(k).get(0).compareTo(value) == 0
                        || amplitudes.get(k).get(1).compareTo(value) == 0
                        || amplitudes.get(k).get(2).compareTo(value) == 0
                        || amplitudes.get(k).get(3).compareTo(value) == 0
                        || amplitudes.get(k).get(0).compareTo(value) == (-1) * amplitudes.get(k).get(1).compareTo(value)
                        || amplitudes.get(k).get(2).compareTo(value) == (-1) * amplitudes.get(k).get(3).compareTo(value))
                    return k;
            }
        return -1;
    }

    private int[] zigzagCrossing(int[][] matrix) {
        int[] list = new int[64];
        int k = 0;
        int column = 0;
        int row = 0;
        list[k] = matrix[row][column];


        do {
            k++;
            column++;
            list[k] = matrix[row][column];
            do {
                k++;
                column--;
                row++;
                list[k] = matrix[row][column];
            } while (column != 0);

            if (row == 7)
                break;
            row++;
            k++;
            list[k] = matrix[row][column];
            do {
                row--;
                column++;
                k++;
                list[k] = matrix[row][column];
            } while (row != 0);
        } while (true);

        do {

            k++;
            column++;
            list[k] = matrix[row][column];
            if (column == 7)
                break;
            do {
                k++;
                column++;
                row--;
                list[k] = matrix[row][column];
            } while (column != 7);
            row++;
            k++;
            list[k] = matrix[row][column];
            do {
                row++;
                column--;
                k++;
                list[k] = matrix[row][column];
            } while (row != 7);
        } while (true);

        return list;
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
            block.setIntegerMatrix(divideQuantization(block.getMatrix(), Q));
        }
    }

    private int[][] divideQuantization(double[][] matrix, double[][] Q) {
        int[][] quantization = new int[8][8];
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
