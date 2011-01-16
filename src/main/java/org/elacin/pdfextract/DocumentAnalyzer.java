/*
 * Copyright 2010 Øyvind Berg (elacin@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elacin.pdfextract;

import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.elacin.pdfextract.input.DocumentContent;
import org.elacin.pdfextract.input.PDFSource;
import org.elacin.pdfextract.input.PageContent;
import org.elacin.pdfextract.input.pdfbox.PDFBoxSource;
import org.elacin.pdfextract.physical.PhysicalPage;
import org.elacin.pdfextract.physical.content.PhysicalPageRegion;
import org.elacin.pdfextract.physical.content.PhysicalText;
import org.elacin.pdfextract.physical.content.WhitespaceRectangle;
import org.elacin.pdfextract.physical.segmentation.WordSegmentator;
import org.elacin.pdfextract.physical.segmentation.region.PageSegmentator;
import org.elacin.pdfextract.physical.segmentation.word.WordSegmentatorImpl;
import org.elacin.pdfextract.renderer.PageRenderer;
import org.elacin.pdfextract.tree.DocumentNode;
import org.elacin.pdfextract.tree.LayoutRegionNode;
import org.elacin.pdfextract.tree.PageNode;
import org.elacin.pdfextract.util.Loggers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: elacin
 * Date: 15.01.11
 * Time: 19.55
 * To change this template use File | Settings | File Templates.
 */
public class DocumentAnalyzer {
// ------------------------------ FIELDS ------------------------------

private static final Logger       log    = Logger.getLogger(DocumentAnalyzer.class);
private static final PDFSource    source = new PDFBoxSource();
public final         DocumentNode root   = new DocumentNode();

private final File pdfFile;
private final File destination;

private final int    startPage;
private final int    endPage;
private final String password;

// --------------------------- CONSTRUCTORS ---------------------------

public DocumentAnalyzer(File pdfFile,
                        File destination,
                        String password,
                        int startPage,
                        int endPage) {
    this.pdfFile = pdfFile;
    this.destination = destination;
    this.password = password;
    this.startPage = startPage;
    this.endPage = endPage;
}

// --------------------- GETTER / SETTER METHODS ---------------------

public DocumentNode getRoot() {
    return root;
}

// -------------------------- OTHER METHODS --------------------------

protected void processFile() throws IOException {
    final DocumentContent content = source.readPages(pdfFile, startPage, endPage, password);

    final WordSegmentator segmentator = new WordSegmentatorImpl(content.getStyles());

    final long t0 = System.currentTimeMillis();

    for (PageContent page : content.getPages()) {
        MDC.put("page", page.getPageNum());

        if (page.getCharacters().isEmpty()) {
            log.error("LOG01150:Page " + page.getPageNum() + " is empty");
            continue;
        }

        final List<PhysicalText> words = segmentator.segmentWords(page.getCharacters());

        PhysicalPage physicalPage = new PhysicalPage(words, page.getGraphics(), page.getPageNum());

        PageSegmentator.segmentPageRegionWithSubRegions(physicalPage);

        final PageNode pageNode = compileLogicalPage(physicalPage);
        root.addChild(pageNode);
    }
    MDC.remove("page");
    final long td = System.currentTimeMillis() - t0;
    Loggers.getInterfaceLog().info("LOG01200:Analyzed " + content.getPages().size() + " pages in "
            + td + " ms");
    printTree(destination, root);

    renderPDF(pdfFile, null, root);
}

@NotNull
public PageNode compileLogicalPage(PhysicalPage page) {
    PageNode ret = new PageNode(page.getPageNumber());


    List<LayoutRegionNode> regions = page.createRegionNodes();
    for (LayoutRegionNode regionNode : regions) {
        ret.addChild(regionNode);
    }
    if (log.isInfoEnabled()) {
        log.info("LOG00940:Page had " + regions.size() + " regions");
    }

    /* this is all just rendering information */
    final List<WhitespaceRectangle> whitespaces = new ArrayList<WhitespaceRectangle>();
    addWhiteSpaceFromRegion(whitespaces, page.getMainRegion());
    ret.addDebugFeatures(Color.CYAN, page.getAllGraphics());
    ret.addDebugFeatures(Color.GREEN, whitespaces);

    return ret;
}

private static void addWhiteSpaceFromRegion(List<WhitespaceRectangle> whitespaces,
                                            PhysicalPageRegion region) {
    whitespaces.addAll(region.getWhitespace());
    for (PhysicalPageRegion subRegion : region.getSubregions()) {
        addWhiteSpaceFromRegion(whitespaces, subRegion);
    }
}

protected void printTree(@NotNull final File output, @NotNull final DocumentNode root) {
    /* write to file */
    Loggers.getInterfaceLog().info("LOG00110:Opening " + output + " for output");
    final PrintStream out;
    try {
        out = new PrintStream(new BufferedOutputStream(new FileOutputStream(output, false),
                8192 * 4), false, "UTF-8");
    } catch (Exception e) {
        throw new RuntimeException("Could not open output file", e);
    }

    root.printTree(out, false);
    out.close();
}

protected void renderPDF(@NotNull final File pdfFile, @Nullable final PDDocument doc,
                         @NotNull final DocumentNode root) {
    long t0 = System.currentTimeMillis();


    final PageRenderer renderer = new PageRenderer(doc, root);

    /* one indexed pages */
    for (int i = 0; i < root.getChildren().size(); i++) {
        final int pageNum = root.getChildren().get(i).getPageNumber();

        BufferedImage image;
        try {
            image = renderer.renderPage(pageNum);
        } catch (RuntimeException e) {
            Loggers.getInterfaceLog().warn(
                    "Error while rendering page " + pageNum + ":" + e.getMessage());
            continue;
        }

        /* then open and write to file */
        final File output;
        if (destination.isDirectory()) {
            output = new File(destination, pdfFile.getName().replace(".pdf",
                    ".elc." + pageNum + ".png"));
        } else {
            output = new File(destination.getAbsolutePath().replace(".xml",
                    ".elc." + pageNum + ".png"));
        }

        try {
            ImageIO.write(image, "png", output);
        } catch (IOException e) {
            Loggers.getInterfaceLog().warn("Error while writing rendered image to file", e);
        }
    }
    Loggers.getInterfaceLog().debug(
            "LOG00170:Rendering of pdf took " + (System.currentTimeMillis() - t0) + " ms");
}
}
