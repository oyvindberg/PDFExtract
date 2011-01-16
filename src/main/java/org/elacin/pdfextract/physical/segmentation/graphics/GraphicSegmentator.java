package org.elacin.pdfextract.physical.segmentation.graphics;

import org.elacin.pdfextract.physical.content.GraphicContent;
import org.elacin.pdfextract.physical.content.PhysicalPageRegion;

import java.util.List;

/**
 * Created by IntelliJ IDEA. User: elacin Date: 16.11.10 Time: 15.30 To change this template use
 * File | Settings | File Templates.
 */
public interface GraphicSegmentator {

CategorizedGraphics segmentGraphicsUsingContentInRegion(List<GraphicContent> graphics,
                                                        PhysicalPageRegion region);
}