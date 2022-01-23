import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        Image image = new Image("images/not_working/nt-P3.ppm");
        ImageProcessor imageProcessor = new ImageProcessor(image);
        Image finalImage = imageProcessor.getImage().convertImageYUVtoRGB();
        finalImage.writeToPPM("final");
    }
}
