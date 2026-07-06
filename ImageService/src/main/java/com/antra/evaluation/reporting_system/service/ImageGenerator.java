package com.antra.evaluation.reporting_system.service;

import com.antra.evaluation.reporting_system.pojo.api.ImageRequest;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Renders the report's tabular data as a PNG table image using Java2D — no
 * external libraries, ImageIO is part of the JDK.
 */
@Component
public class ImageGenerator {

    private static final int PADDING = 24;
    private static final int ROW_HEIGHT = 34;
    private static final int TITLE_HEIGHT = 48;
    private static final int MIN_COL_WIDTH = 120;
    private static final Color HEADER_BG = new Color(0x5B, 0x5B, 0xD6);
    private static final Color GRID = new Color(0xE0, 0xE1, 0xEB);
    private static final Color STRIPE = new Color(0xF7, 0xF7, 0xFB);

    public File generate(ImageRequest request) throws IOException {
        List<String> headers = request.getHeaders();
        List<List<String>> data = request.getData() == null ? List.of() : request.getData();
        int columns = headers.size();

        // Measure with a scratch graphics context so columns fit their content.
        BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scratch.createGraphics();
        Font titleFont = new Font("SansSerif", Font.BOLD, 20);
        Font headerFont = new Font("SansSerif", Font.BOLD, 14);
        Font cellFont = new Font("SansSerif", Font.PLAIN, 13);

        int[] colWidths = new int[columns];
        for (int c = 0; c < columns; c++) {
            g.setFont(headerFont);
            int w = g.getFontMetrics().stringWidth(headers.get(c));
            g.setFont(cellFont);
            for (List<String> row : data) {
                if (c < row.size()) {
                    w = Math.max(w, g.getFontMetrics().stringWidth(String.valueOf(row.get(c))));
                }
            }
            colWidths[c] = Math.max(MIN_COL_WIDTH, w + 24);
        }
        g.dispose();

        int tableWidth = 0;
        for (int w : colWidths) tableWidth += w;
        int width = tableWidth + PADDING * 2;
        int height = PADDING * 2 + TITLE_HEIGHT + ROW_HEIGHT * (data.size() + 1);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D gc = image.createGraphics();
        gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gc.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        gc.setColor(Color.WHITE);
        gc.fillRect(0, 0, width, height);

        // Title
        gc.setColor(new Color(0x1A, 0x1C, 0x23));
        gc.setFont(titleFont);
        gc.drawString(request.getDescription() == null ? "Report" : request.getDescription(),
                PADDING, PADDING + 24);

        int originX = PADDING;
        int originY = PADDING + TITLE_HEIGHT;

        // Header row
        gc.setColor(HEADER_BG);
        gc.fillRect(originX, originY, tableWidth, ROW_HEIGHT);
        gc.setColor(Color.WHITE);
        gc.setFont(headerFont);
        drawRow(gc, headers, colWidths, originX, originY);

        // Data rows (zebra striped)
        gc.setFont(cellFont);
        for (int r = 0; r < data.size(); r++) {
            int y = originY + ROW_HEIGHT * (r + 1);
            if (r % 2 == 1) {
                gc.setColor(STRIPE);
                gc.fillRect(originX, y, tableWidth, ROW_HEIGHT);
            }
            gc.setColor(new Color(0x2A, 0x2C, 0x36));
            drawRow(gc, data.get(r), colWidths, originX, y);
        }

        // Grid lines
        gc.setColor(GRID);
        gc.setStroke(new BasicStroke(1f));
        int totalRows = data.size() + 1;
        for (int r = 0; r <= totalRows; r++) {
            int y = originY + ROW_HEIGHT * r;
            gc.drawLine(originX, y, originX + tableWidth, y);
        }
        int x = originX;
        for (int c = 0; c <= columns; c++) {
            gc.drawLine(x, originY, x, originY + ROW_HEIGHT * totalRows);
            if (c < columns) x += colWidths[c];
        }
        gc.dispose();

        File temp = File.createTempFile("report-", ".png");
        ImageIO.write(image, "png", temp);
        return temp;
    }

    private void drawRow(Graphics2D gc, List<String> values, int[] colWidths, int originX, int y) {
        int x = originX;
        int baseline = y + ROW_HEIGHT / 2 + gc.getFontMetrics().getAscent() / 2 - 2;
        for (int c = 0; c < colWidths.length; c++) {
            String text = c < values.size() ? String.valueOf(values.get(c)) : "";
            gc.drawString(text, x + 12, baseline);
            x += colWidths[c];
        }
    }
}
