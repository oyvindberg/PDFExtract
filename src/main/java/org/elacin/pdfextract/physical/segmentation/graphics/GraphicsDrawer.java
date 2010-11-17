package org.elacin.pdfextract.physical.segmentation.graphics;

import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;

/**
 * Created by IntelliJ IDEA. User: elacin Date: 16.11.10 Time: 15.29 To change this template use
 * File | Settings | File Templates.
 */
public interface GraphicsDrawer {

void drawImage(Image awtImage, AffineTransform at, Rectangle2D bounds);

void fill(@NotNull GeneralPath originalPath, Color color);

void strokePath(@NotNull GeneralPath path, Color color);
}
