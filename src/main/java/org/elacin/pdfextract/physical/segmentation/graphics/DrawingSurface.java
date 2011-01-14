package org.elacin.pdfextract.physical.segmentation.graphics;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;

/**
 * This represents a surface on which it is possible to draw.
 * <p/>
 * For graphic segmentation purposes no real drawing will occur, but a list of
 * graphic placements will be created
 */
public interface DrawingSurface {

void drawImage(Image image, AffineTransform at, Shape clippingPath);

void fill(GeneralPath originalPath, Color color, Shape clippingPath);

void strokePath(GeneralPath originalPath, Color color, Shape clippingPath);
}
