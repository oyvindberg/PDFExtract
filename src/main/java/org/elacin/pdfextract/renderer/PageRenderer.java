/*
 * Copyright 2010 Øyvind Berg (elacin@gmail.com)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.elacin.pdfextract.renderer;

import org.apache.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.elacin.pdfextract.Loggers;
import org.elacin.pdfextract.tree.*;
import org.elacin.pdfextract.util.Rectangle;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: elacin
 * Date: Jun 17, 2010
 * Time: 5:02:09 AM
 * To change this template use File | Settings | File Templates.
 */
public class PageRenderer {
    // ------------------------------ FIELDS ------------------------------

    private static final Logger LOG = Loggers.getPdfExtractorLog();

    private final int resolution;
    private final PDDocument document;
    private final DocumentNode documentNode;
    private final Map<Integer, BufferedImage> pagesCache = new HashMap<Integer, BufferedImage>();

    // --------------------------- CONSTRUCTORS ---------------------------

    public PageRenderer(final PDDocument document, final DocumentNode documentNode, final int resolution) {
        this.document = document;
        this.documentNode = documentNode;
        this.resolution = resolution;
    }

    public PageRenderer(final PDDocument document, final DocumentNode documentNode) {
        this(document, documentNode, 200);
    }

    // -------------------------- PUBLIC METHODS --------------------------

    public BufferedImage renderPage(final int pageNum) throws IOException {
        final PageNode pageNode = documentNode.getPageNumber(pageNum + 1);

        /* first have PDFBox draw the pdf to a BufferedImage */
        long t1 = System.currentTimeMillis();
        PDPage page = (PDPage) document.getDocumentCatalog().getAllPages().get(pageNum);
        BufferedImage image = getPDFBoxRenderingForPage(pageNum);

        /* then draw our information on top */
        final Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        final float xScale = ((float) image.getWidth() / page.getArtBox().getWidth()) / 100.0f;
        final float yScale = ((float) image.getHeight() / page.getArtBox().getHeight()) / 100.0f;

        for (ParagraphNode paragraphNode : pageNode.getChildren()) {
            for (LineNode lineNode : paragraphNode.getChildren()) {
                for (WordNode wordNode : lineNode.getChildren()) {
                    drawNode(wordNode, graphics, xScale, yScale);
                }
                drawNode(lineNode, graphics, xScale, yScale);
            }
            drawNode(paragraphNode, graphics, xScale, yScale);
        }
        LOG.warn("Rendered page " + pageNum + " in " + (System.currentTimeMillis() - t1) + " ms");
        return image;
    }

    private BufferedImage getPDFBoxRenderingForPage(final int pageNum) throws IOException {
        BufferedImage image = null;
        if (!pagesCache.containsKey(pageNum)) {
            PDPage page = (PDPage) document.getDocumentCatalog().getAllPages().get(pageNum);
            image = page.convertToImage(BufferedImage.TYPE_INT_ARGB, resolution);
            pagesCache.put(pageNum, image);
        }
        return image;
    }

    @SuppressWarnings({"NumericCastThatLosesPrecision"})
    public static void drawNode(final AbstractNode node, final Graphics2D graphics, final float xScale, final float yScale) {
        Color color;

        if (node instanceof WordNode) {
            color = Color.BLUE;
        } else if (node instanceof LineNode) {
            color = Color.RED;
        } else if (node instanceof ParagraphNode) {
            color = Color.GREEN;
        } else {
            color = Color.BLACK;
        }

        graphics.setColor(color);
        final Rectangle pos = node.getPosition();

        final int x = (int) ((float) pos.getX() * xScale);
        final int width = (int) ((float) pos.getWidth() * xScale);
        int y = (int) ((float) pos.getY() * yScale);
        final int height = (int) ((float) pos.getHeight() * yScale);

        graphics.drawRect(x, y, width, height);

        graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 60));
        graphics.fillRect(x, y, width, height);
        graphics.fillRect(x, y, width, height);
    }
}
