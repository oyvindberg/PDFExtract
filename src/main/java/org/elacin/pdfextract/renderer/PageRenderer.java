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
import org.elacin.pdfextract.segmentation.WhitespaceRectangle;
import org.elacin.pdfextract.tree.*;
import org.elacin.pdfextract.util.Rectangle;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Jun 17, 2010 Time: 5:02:09 AM To change this
 * template use File | Settings | File Templates.
 */
public class PageRenderer {
// ------------------------------ FIELDS ------------------------------

private static final Logger log = Logger.getLogger(PageRenderer.class);
private final int resolution;
private final PDDocument document;
private final DocumentNode documentNode;

// --------------------------- CONSTRUCTORS ---------------------------

public PageRenderer(final PDDocument document,
                    final DocumentNode documentNode,
                    final int resolution)
{
    this.document = document;
    this.documentNode = documentNode;
    this.resolution = resolution;
}

public PageRenderer(final PDDocument document, final DocumentNode documentNode) {
    this(document, documentNode, 200);
}

// -------------------------- STATIC METHODS --------------------------

@SuppressWarnings({"NumericCastThatLosesPrecision"})
private static void drawRectangleInColor(final Graphics2D graphics,
                                         final float xScale,
                                         final float yScale,
                                         final Color color,
                                         final Rectangle pos)
{
    graphics.setColor(color);
    final int x = (int) ((float) pos.getX() * xScale);
    final int width = (int) ((float) pos.getWidth() * xScale);
    int y = (int) ((float) pos.getY() * yScale);
    final int height = (int) ((float) pos.getHeight() * yScale);

    graphics.drawRect(x, y, width, height);

    graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 60));
    graphics.fillRect(x, y, width, height);
    graphics.fillRect(x, y, width, height);
}

// -------------------------- PUBLIC METHODS --------------------------

public BufferedImage renderPage(final int pageNum) {
    final PageNode pageNode = documentNode.getPageNumber(pageNum + 1);

    if (pageNode == null) {
        throw new RuntimeException("Renderer: No contents found for page " + pageNum + ".");
    }

    /* first have PDFBox draw the pdf to a BufferedImage */
    long t1 = System.currentTimeMillis();
    PDPage page = (PDPage) document.getDocumentCatalog().getAllPages().get(pageNum);

    final BufferedImage image;
    try {
        image = page.convertToImage(BufferedImage.TYPE_INT_ARGB, resolution);
    } catch (IOException e) {
        throw new RuntimeException("PDFBox failed while rendering page " + pageNum, e);
    }

    /* then draw our information on top */
    final Graphics2D graphics = image.createGraphics();
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    final float xScale = (float) image.getWidth() / page.getArtBox().getWidth();
    final float yScale = (float) image.getHeight() / page.getArtBox().getHeight();

    /* draw document tree */
    for (ParagraphNode paragraphNode : pageNode.getChildren()) {
        for (LineNode lineNode : paragraphNode.getChildren()) {
            for (WordNode wordNode : lineNode.getChildren()) {
                drawRectangleInColor(graphics, xScale, yScale, Color.BLUE, wordNode.getPosition());
            }
            //            drawRectangleInColor(graphics, xScale, yScale, Color.RED, lineNode.getPosition());
        }
        //        drawRectangleInColor(graphics, xScale, yScale, Color.GREEN, paragraphNode.getPosition());
    }

    /* draw whitespace */
    for (WhitespaceRectangle whitespace : pageNode.getWhitespaces()) {
        drawRectangleInColor(graphics, xScale, yScale, Color.RED, whitespace.getPosition());
    }

    /* draw columns if provided */
    if (pageNode.getColumns() != null) {
        for (Map.Entry<Integer, List<Integer>> yCols : pageNode.getColumns().entrySet()) {
            final Integer y = yCols.getKey();
            for (Integer index : yCols.getValue()) {
                final Rectangle pos = new Rectangle(index, y, 1, 1);
                drawRectangleInColor(graphics, xScale, yScale, Color.GREEN, pos);
            }
        }
    }
    Loggers.getInterfaceLog().info(String.format("LOG00180:Rendered page %d in %d ms", pageNum,
                                                 (System.currentTimeMillis() - t1)));
    return image;
}
}
