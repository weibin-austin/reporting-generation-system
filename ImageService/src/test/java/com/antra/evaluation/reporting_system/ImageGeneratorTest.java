package com.antra.evaluation.reporting_system;

import com.antra.evaluation.reporting_system.pojo.api.ImageRequest;
import com.antra.evaluation.reporting_system.service.ImageGenerator;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ImageGeneratorTest {

    @Test
    public void generatesReadablePngWithExpectedSize() throws IOException {
        ImageRequest request = new ImageRequest();
        request.setDescription("Math Report");
        request.setSubmitter("Tester");
        request.setHeaders(List.of("Name", "Score"));
        request.setData(List.of(List.of("Alice", "100"), List.of("Bob", "97")));

        File file = new ImageGenerator().generate(request);
        try {
            assertTrue(file.exists());
            assertTrue(file.length() > 0);
            BufferedImage image = ImageIO.read(file); // null if not a valid image
            assertNotNull(image);
            // header + 2 data rows, plus title and padding -> a non-trivial canvas
            assertTrue(image.getWidth() > 100);
            assertTrue(image.getHeight() > 100);
        } finally {
            file.delete();
        }
    }
}
