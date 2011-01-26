package org.elacin.pdfextract.physical.graphics;

import org.elacin.pdfextract.content.GraphicContent;
import org.elacin.pdfextract.content.PhysicalPageRegion;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Created by IntelliJ IDEA. User: elacin Date: 16.11.10 Time: 15.30 To change this template use
 * File | Settings | File Templates.
 */
public interface GraphicSegmentator {

// -------------------------- PUBLIC METHODS --------------------------
    @NotNull
    CategorizedGraphics categorizeGraphics(List<GraphicContent> graphics, PhysicalPageRegion region);
}
