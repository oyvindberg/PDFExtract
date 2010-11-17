/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.pdfbox.pdfviewer;

import org.apache.log4j.Logger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDMatrix;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.PDGraphicsState;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.PDFStreamEngine;
import org.apache.pdfbox.util.ResourceLoader;
import org.apache.pdfbox.util.TextPosition;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.geom.*;
import java.io.IOException;


public class PageDrawer extends PDFStreamEngine {

private static final Logger log = Logger.getLogger(PageDrawer.class);

// ------------------------------ FIELDS ------------------------------

protected Color currentColor;
/* pdfbox things */
private GeneralPath linePath = new GeneralPath();
private BasicStroke stroke;

// --------------------------- CONSTRUCTORS ---------------------------

/**
 * Default constructor, loads properties from file.
 *
 * @throws IOException If there is an error loading properties from the file.
 */
public PageDrawer() throws IOException {
	super(ResourceLoader.loadProperties("org/apache/pdfbox/resources/PageDrawer.properties", true));
}


// --------------------- GETTER / SETTER METHODS ---------------------

/**
 * Get the current line path to be drawn.
 *
 * @return The current line path to be drawn.
 */
public GeneralPath getLinePath() {
	return linePath;
}

/**
 * Get the page that is currently being drawn.
 *
 * @return The page that is being drawn.
 */
public PDPage getPage() {
	return page;
}

public void SHFill(COSName asd) {
	//Dummy
}

/**
 * This will return the current stroke.
 *
 * @return The current stroke.
 */
public BasicStroke getStroke() {
	return stroke;
}

/**
 * This will set the current stroke.
 *
 * @param newStroke The current stroke.
 */
public void setStroke(BasicStroke newStroke) {
	stroke = newStroke;
}

// -------------------------- PUBLIC METHODS --------------------------

/**
 * Draw the AWT graphics. Called by Invoke. Moved into PageDrawer so that Invoke doesn't have to
 * reach in here for Graphics as that breaks extensibility.
 *
 * @param awtImage The graphics to draw.
 * @param at       The transformation to use when drawing.
 */
public void drawImage(Image awtImage, AffineTransform at) {
	currentClippingPath = getGraphicsState().getCurrentClippingPath();
	final Rectangle2D bounds2D = getGraphicsState().getCurrentClippingPath().getBounds2D();
	graphicSegmentator.drawImage(awtImage, at, bounds2D);
}

public void drawPage(Graphics g, PDPage p, Dimension pageDimension) throws IOException {

}

/**
 * Fill the path.
 *
 * @param windingRule The winding rule this path will use.
 * @throws IOException If there is an IO error while filling the path.
 */
public void fillPath(int windingRule) throws IOException {
	currentColor = getGraphicsState().getNonStrokingColor().getJavaColor();
	getLinePath().setWindingRule(windingRule);
	currentClippingPath = getGraphicsState().getCurrentClippingPath();
	graphicSegmentator.fill(getLinePath(), currentColor);
	getLinePath().reset();
}

/**
 * Fix the y coordinate.
 *
 * @param y The y coordinate.
 * @return The updated y coordinate.
 */
public double fixY(double y) {
	return getPageSize().getHeight() - y;
}

/**
 * Get the size of the page that is currently being drawn.
 *
 * @return The size of the page that is being drawn.
 */
@NotNull
public Dimension getPageSize() {
	final PDRectangle mediaBox = page.getArtBox();
	return new Dimension((int) mediaBox.getWidth(), (int) mediaBox.getHeight());
}

/**
 * Set the clipping Path.
 *
 * @param windingRule The winding rule this path will use.
 */
public void setClippingPath(int windingRule) {
	PDGraphicsState graphicsState = getGraphicsState();
	GeneralPath clippingPath = (GeneralPath) getLinePath().clone();
	clippingPath.setWindingRule(windingRule);
	// If there is already set a clipping path, we have to intersect the new with the existing one
	if (graphicsState.getCurrentClippingPath() != null) {
		Area currentArea = new Area(getGraphicsState().getCurrentClippingPath());
		Area newArea = new Area(clippingPath);
		currentArea.intersect(newArea);
		graphicsState.setCurrentClippingPath(currentArea);
	} else {
		graphicsState.setCurrentClippingPath(clippingPath);
	}
	getLinePath().reset();
}

/**
 * Set the line path to draw.
 *
 * @param newLinePath Set the line path to draw.
 */
public void setLinePath(GeneralPath newLinePath) {
	if (linePath == null || linePath.getCurrentPoint() == null) {
		linePath = newLinePath;
	} else {
		linePath.append(newLinePath, false);
	}
}

/**
 * Stroke the path.
 *
 * @throws IOException If there is an IO error while stroking the path.
 */
public void strokePath() throws IOException {
	currentColor = getGraphicsState().getStrokingColor().getJavaColor();
	currentClippingPath = getGraphicsState().getCurrentClippingPath();
	graphicSegmentator.strokePath(getLinePath(), currentColor);

	getLinePath().reset();
}

//This code generalizes the code Jim Lynch wrote for AppendRectangleToPath

/**
 * use the current transformation matrix to transform a single point.
 *
 * @param x x-coordinate of the point to be transform
 * @param y y-coordinate of the point to be transform
 * @return the transformed coordinates as Point2D.Double
 */
@NotNull
public java.awt.geom.Point2D.Double transformedPoint(double x, double y) {
	double[] position = {x, y};
	getGraphicsState().getCurrentTransformationMatrix().createAffineTransform().transform(position, 0, position, 0, 1);
	position[1] = fixY(position[1]);
	return new Point2D.Double(position[0], position[1]);
}

// -------------------------- OTHER METHODS --------------------------

/**
 * You should override this method if you want to perform an action when a text is being processed.
 *
 * @param text The text to process
 */
protected void processTextPosition(@NotNull TextPosition text) {
	//    try {
	//        switch (this.getGraphicsState().getTextState().getRenderingMode()) {
	//            case PDTextState.RENDERING_MODE_FILL_TEXT:
	//                graphics.setColor(this.getGraphicsState().getNonStrokingColor().getJavaColor());
	//                break;
	//            case PDTextState.RENDERING_MODE_STROKE_TEXT:
	//                graphics.setColor(this.getGraphicsState().getStrokingColor().getJavaColor());
	//                break;
	//            case PDTextState.RENDERING_MODE_NEITHER_FILL_NOR_STROKE_TEXT:
	//                //basic support for text rendering mode "invisible"
	//                Color nsc = this.getGraphicsState().getStrokingColor().getJavaColor();
	//                float[] components = {Color.black.getRed(),
	//                                      Color.black.getGreen(),
	//                                      Color.black.getBlue()};
	//                Color c = new Color(nsc.getColorSpace(), components, 0f);
	//                graphics.setColor(c);
	//                break;
	//            default:
	//                // TODO : need to implement....
	//                log.debug("Unsupported RenderingMode "
	//                        + this.getGraphicsState().getTextState().getRenderingMode()
	//                        + " in PageDrawer.processTextPosition()." + " Using RenderingMode "
	//                        + PDTextState.RENDERING_MODE_FILL_TEXT + " instead");
	//                graphics.setColor(this.getGraphicsState().getNonStrokingColor().getJavaColor());
	//        }

	PDFont font = text.getFont();
	Matrix textPos = text.getTextPos().copy();
	float x = textPos.getXPosition();
	// the 0,0-reference has to be moved from the lower left (PDF) to the upper left (AWT-graphics)
	float y = (float) getPageSize().height - textPos.getYPosition();
	// Set translation to 0,0. We only need the scaling and shearing
	textPos.setValue(2, 0, 0.0F);
	textPos.setValue(2, 1, 0.0F);
	// because of the moved 0,0-reference, we have to shear in the opposite direction
	textPos.setValue(0, 1, (float) (-1) * textPos.getValue(0, 1));
	textPos.setValue(1, 0, (float) (-1) * textPos.getValue(1, 0));
	AffineTransform at = textPos.createAffineTransform();
	PDMatrix fontMatrix = font.getFontMatrix();
	at.scale((double) (fontMatrix.getValue(0, 0) * 1000f), (double) (fontMatrix.getValue(1, 0)
			* 1000f));
	//        graphics.setClip(getGraphicsState().getCurrentClippingPath());
	// the fontSize is no longer needed as it is already part of the transformation
	// we should remove it from the parameter list in the long run
	//        font.drawString(text.getCharacter(), graphics, 1, at, x, y);
	//    } catch (IOException io) {
	//        io.printStackTrace();
	//    }
}
}
