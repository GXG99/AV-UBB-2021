import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ImageProcessor {
    private Image image;
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

    public ImageProcessor(Image image) {
        this.image = image;
        encodeImage(image);
        decodeImage(image);
    }

    private void decodeImage(Image image) {
        System.out.println("Decoding image");
        writeBlocks(yBlocks, "yBlocks_upSampled");
        writeBlocks(vBlocks, "vBlocks_upSampled");
        writeBlocks(uBlocks, "uBlocks_upSampled");

        image.setY(decodeBlock(yBlocks));
        image.setU(decodeBlock(uBlocks));
        image.setV(decodeBlock(vBlocks));

        image.convertImageYUVtoRGB();
    }

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
    }


    private double[][] decodeBlock(List<Block> encoded) {
        System.out.println(String.format("Decoding image of type %s", encoded.get(1).getColorType()));
        System.out.println(String.format("Decoding %s number of blocks", encoded.size()));

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

    private List<Block> upSampleBlocks(List<Block> encoded) {
        List<Block> upSampled = new ArrayList<>();
        encoded.forEach(block -> upSampled.add(upSampling(block)));
        return upSampled;
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

    private List<Block> divideMatrix(Image image, String type, double[][] matrix) {
        System.out.println(String.format("Dividing block of type %s", type));

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
        System.out.println(String.format("Encoded size %s", encoded.size()));
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

    public Image getImage() {
        return this.image;
    }
}
