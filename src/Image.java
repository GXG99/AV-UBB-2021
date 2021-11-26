import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Image {
    private final String fileName;
    private String author;
    private String title;
    private int width;
    private int height;
    private int[][] r;
    private int[][] g;
    private int[][] b;
    private double[][] y;
    private double[][] u;
    private double[][] v;

    private Image(Image image) {
        this.fileName = image.fileName;
        this.width = image.width;
        this.height = image.height;
        this.author = image.author;
        this.title = image.title;
        this.r = new int[image.height][image.width];
        this.g = new int[image.height][image.width];
        this.b = new int[image.height][image.width];
        this.y = image.y;
        this.u = image.u;
        this.v = image.v;
    }

    public Image(String fileName) {
        this.fileName = fileName;
        System.out.printf("Reading image %s%n", fileName);
        readImage();
        convertRGBtoYUV();
    }

    private void convertRGBtoYUV() {
        y = new double[height][width];
        u = new double[height][width];
        v = new double[height][width];
        for (int line = 0; line < height; line++) {
            for (int column = 0; column < width; column++) {
                y[line][column] = 0.299d * r[line][column] + 0.587d * g[line][column] + 0.144 * b[line][column];
                u[line][column] = 128 - 0.1687d * r[line][column] - 0.3312d * g[line][column] + 0.5d * b[line][column];
                v[line][column] = 128 + 0.5d * r[line][column] - 0.4186d * g[line][column] - 0.0813d * b[line][column];
            }
        }
    }

    public Image convertImageYUVtoRGB() {
        Image newImage = new Image(this);
        for (int line = 0; line < height; line++) {
            for (int column = 0; column < width; column++) {
                double R = y[line][column] + (1.370705 * (v[line][column] - 128));
                double G = y[line][column] - (0.698001 * (u[line][column] - 128)) - (0.337633 * (v[line][column] - 128));
                double B = y[line][column] + (1.732446 * (u[line][column] - 128));

//                double R = y[line][column] + (1.402 * (v[line][column] - 128));
//                double G = y[line][column] - (0.344 * (u[line][column] - 128)) - (0.714 * (v[line][column] - 128));
//                double B = y[line][column] + (1.772 * (u[line][column] - 128));

                if (R > 255) R = 255.0;
                if (G > 255) G = 255.0;
                if (B > 255) B = 255.0;

                if (R < 0) R = 0.0;
                if (G < 0) G = 0.0;
                if (B < 0) B = 0.0;

                newImage.r[line][column] = (int) R;
                newImage.g[line][column] = (int) G;
                newImage.b[line][column] = (int) B;
            }
        }
        return newImage;
    }

    public void writeToPPM(String outputFileName) {
        try {
            System.out.println(String.format("Image of size height: %s, width: %s final size %s", height, width, height * width * 3));
            BufferedWriter writer = new BufferedWriter(new FileWriter("output/" + outputFileName + ".ppm"));
            writer.write("P3\n");
            writer.write(author + "\n");
            writer.write(height + " "  + width + "\n");
            writer.write("255\n");
            writer.write("255\n");
            writer.write("255\n");
            for (int line = 0; line < height; line++) {
                for (int column = 0; column < width; column++) {
                    writer.write(this.r[line][column] + "\n");
                    writer.write(this.g[line][column] + "\n");
                    writer.write(this.b[line][column] + "\n");
                }
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readImage() {
        try {
            BufferedReader reader = Files.newBufferedReader(Paths.get(this.fileName));
            title = reader.readLine();
            author = reader.readLine();
            String[] size = reader.readLine().split(" ");
            height = Integer.parseInt(size[0]);
            width = Integer.parseInt(size[1]);
            r = new int[height][width];
            g = new int[height][width];
            b = new int[height][width];
            y = new double[height][width];
            u = new double[height][width];
            v = new double[height][width];
            for (int line = 0; line < height; line++) {
                for (int column = 0; column < width; column++) {
                    r[line][column] = Integer.parseInt(reader.readLine());
                    g[line][column] = Integer.parseInt(reader.readLine());
                    b[line][column] = Integer.parseInt(reader.readLine());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int[][] getR() {
        return r;
    }

    public int[][] getG() {
        return g;
    }

    public int[][] getB() {
        return b;
    }

    public double[][] getY() {
        return y;
    }

    public double[][] getU() {
        return u;
    }

    public double[][] getV() {
        return v;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setR(int[][] r) {
        this.r = r;
    }

    public void setG(int[][] g) {
        this.g = g;
    }

    public void setB(int[][] b) {
        this.b = b;
    }

    public void setY(double[][] y) {
        this.y = y;
    }

    public void setU(double[][] u) {
        this.u = u;
    }

    public void setV(double[][] v) {
        this.v = v;
    }
}
