package org.elacin.pdfextract.physical.segmentation.graphics;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;

/**
 * This represents a surface on which it is possible to draw.
 * <p/>
 * For graphic segmentation purposes no real drawing will occur, but a list of
 * graphic placements will be created
 */
public interface DrawingSurface {

void drawImage(Image awtImage, AffineTransform at, Rectangle2D bounds);

void fill(GeneralPath originalPath, Color color);

void strokePath(GeneralPath path, Color color);
}
