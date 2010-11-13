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
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.elacin.pdfextract.physical.content.HasPosition;
import org.elacin.pdfextract.tree.*;
import org.elacin.pdfextract.util.Loggers;
import org.elacin.pdfextract.util.Rectangle;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Jun 17, 2010 Time: 5:02:09 AM To change this
 * template use File | Settings | File Templates.
 */
public class PageRenderer {
// ------------------------------ FIELDS ------------------------------

private static final Logger log = Logger.getLogger(PageRenderer.class);
private final int          resolution;
private final PDDocument   document;
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
	this(document, documentNode, 100);
}

// -------------------------- STATIC METHODS --------------------------

@SuppressWarnings({"NumericCastThatLosesPrecision"})
private static void drawRectangle(@NotNull final Graphics2D graphics,
                                  final float xScale,
                                  final float yScale,
                                  @NotNull final Color color,
                                  @NotNull final Rectangle pos,
                                  final boolean fill)
{
	graphics.setColor(color);
	final int x = (int) ((float) pos.getX() * xScale);
	final int width = (int) ((float) pos.getWidth() * xScale);
	int y = (int) ((float) pos.getY() * yScale);
	final int height = (int) ((float) pos.getHeight() * yScale);

	graphics.drawRect(x, y, width, height);
	if (fill) {
		graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 60));
		graphics.fillRect(x, y, width, height);
		//        graphics.fillRect(x, y, width, height);
	}
}

// -------------------------- PUBLIC METHODS --------------------------

public BufferedImage renderPage(final int pageNum) {
	final PageNode pageNode = documentNode.getPageNumber(pageNum);

	if (pageNode == null) {
		throw new RuntimeException("Renderer: No contents found for page " + pageNum + ".");
	}

	/* first have PDFBox draw the pdf to a BufferedImage */
	long t1 = System.currentTimeMillis();
	PDPage page = (PDPage) document.getDocumentCatalog().getAllPages().get(pageNum - 1);

	final BufferedImage image = createImage(page, BufferedImage.TYPE_INT_ARGB, resolution);

	/* then draw our information on top */
	final Graphics2D graphics = image.createGraphics();
	graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

	final float xScale = (float) image.getWidth() / page.getArtBox().getWidth();
	final float yScale = (float) image.getHeight() / page.getArtBox().getHeight();

	/* draw document tree */
	for (ParagraphNode paragraphNode : pageNode.getChildren()) {
		for (LineNode lineNode : paragraphNode.getChildren()) {
			for (WordNode wordNode : lineNode.getChildren()) {
				drawRectangle(graphics, xScale, yScale, Color.BLUE, wordNode.getPosition(), true);
			}
			drawRectangle(graphics, xScale, yScale, Color.YELLOW, lineNode.getPosition(), true);
		}
		drawRectangle(graphics, xScale, yScale, Color.CYAN, paragraphNode.getPosition(), true);
	}

	for (Map.Entry<Color, List<HasPosition>> o : pageNode.getDebugFeatures().entrySet()) {
		for (HasPosition position : o.getValue()) {
			drawRectangle(graphics, xScale, yScale, o.getKey(), position.getPosition(), true);
		}
	}

	Loggers.getInterfaceLog().info(String.format("LOG00180:Rendered page %d in %d ms", pageNum,
	                                             (System.currentTimeMillis() - t1)));
	return image;
}

private static final Color TRANSPARENT_WHITE           = new Color(255, 255, 255, 0);
private static final int   DEFAULT_USER_SPACE_UNIT_DPI = 72;

private BufferedImage createImage(final PDPage page, final int imageType, final int resolution) {
	PDRectangle mBox = page.findMediaBox();
	float scaling = resolution / (float) DEFAULT_USER_SPACE_UNIT_DPI;

	int widthPx = Math.round(mBox.getWidth() * scaling);
	int heightPx = Math.round(mBox.getHeight() * scaling);

	BufferedImage retval = new BufferedImage(widthPx, heightPx, imageType);
	Graphics2D graphics = (Graphics2D) retval.getGraphics();
	graphics.setBackground(TRANSPARENT_WHITE);
	graphics.clearRect(0, 0, retval.getWidth(), retval.getHeight());
	graphics.scale(scaling, scaling);

	return retval;
}
}
