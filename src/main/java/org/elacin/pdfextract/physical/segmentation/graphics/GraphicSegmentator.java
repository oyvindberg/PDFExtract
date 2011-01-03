package org.elacin.pdfextract.physical.segmentation.graphics;

import org.elacin.pdfextract.physical.content.GraphicContent;
import org.elacin.pdfextract.physical.content.PhysicalPageRegion;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Created by IntelliJ IDEA. User: elacin Date: 16.11.10 Time: 15.30 To change this template use
 * File | Settings | File Templates.
 */
public interface GraphicSegmentator {

@NotNull
List<GraphicContent> getContents();

@NotNull
List<GraphicContent> getContainers();

@NotNull
List<GraphicContent> getGraphicsToRender();

void segmentGraphicsUsingContentInRegion(PhysicalPageRegion region);

List<GraphicContent> getVerticalSeparators();

List<GraphicContent> getHorizontalSeparators();
}
