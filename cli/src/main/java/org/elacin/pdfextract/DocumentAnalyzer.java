package org.elacin.pdfextract;/*
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

import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.elacin.pdfextract.content.PhysicalPage;
import org.elacin.pdfextract.content.PhysicalText;
import org.elacin.pdfextract.integration.DocumentContent;
import org.elacin.pdfextract.integration.PDFSource;
import org.elacin.pdfextract.integration.PageContent;
import org.elacin.pdfextract.integration.pdfbox.PDFBoxSource;
import org.elacin.pdfextract.physical.segmentation.TreeCreator;
import org.elacin.pdfextract.physical.segmentation.region.PageSegmentator;
import org.elacin.pdfextract.physical.segmentation.word.WordSegmentator;
import org.elacin.pdfextract.physical.segmentation.word.WordSegmentatorImpl;
import org.elacin.pdfextract.renderer.PageRenderer;
import org.elacin.pdfextract.tree.DocumentNode;
import org.elacin.pdfextract.tree.PageNode;
import org.elacin.pdfextract.xml.SimpleXMLOutput;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.elacin.pdfextract.Constants.OUTPUT_EXTENSION;

/**
 * Created by IntelliJ IDEA.
 * User: elacin
 * Date: 15.01.11
 * Time: 19.55
 * To change this template use File | Settings | File Templates.
 */
public class DocumentAnalyzer {
// ------------------------------ FIELDS ------------------------------

private static final Logger log = Logger.getLogger(DocumentAnalyzer.class);

public static WordSegmentator wordSegmentator = new WordSegmentatorImpl();


public final DocumentNode root = new DocumentNode();
private final PDFSource source;
private final File      destination;

// --------------------------- CONSTRUCTORS ---------------------------

public DocumentAnalyzer(File pdfFile,
                        File destination,
                        String password,
                        int startPage,
                        int endPage) {
    this.destination = destination;
    source = new PDFBoxSource(pdfFile, startPage, endPage, password);
}

// --------------------- GETTER / SETTER METHODS ---------------------

public DocumentNode getRoot() {
    return root;
}

// -------------------------- OTHER METHODS --------------------------

public void processFile() throws IOException {
    final DocumentContent content = source.readPages();

    final long t0 = System.currentTimeMillis();

    for (PageContent inputPage : content.getPages()) {
        MDC.put("page", inputPage.getPageNum());

        if (inputPage.getCharacters().isEmpty()) {
            log.error("LOG01150:Page " + inputPage.getPageNum() + " is empty");
            continue;
        }

        final List<PhysicalText> words = wordSegmentator.segmentWords(inputPage.getCharacters());

        /* create a physical page instance */
        PhysicalPage physicalPage = new PhysicalPage(words, inputPage.getGraphics(), inputPage.getPageNum());

        /* divide the page in smaller sections */
        PageSegmentator.segmentPageRegionWithSubRegions(physicalPage);

        /* create the tree representation */
        final PageNode pageNode = TreeCreator.compileLogicalPage(physicalPage);

        root.addChild(pageNode);
    }

    MDC.remove("page");

    SimpleXMLOutput.printTree(root, destination);

    final long td = System.currentTimeMillis() - t0;

    log.info("Analyzed " + content.getPages().size() + " pages in " + td + "ms");

    if (Constants.RENDER_ENABLED) {
        renderPDF();
    }
    source.closeSource();
}


public void renderPDF() {
    long t0 = System.currentTimeMillis();

    final PageRenderer renderer = new PageRenderer(source, root, Constants.RENDER_RESOLUTION);

    for (int i = 0; i < root.getChildren().size(); i++) {
        /* one indexed pages */
        final int pageNum = root.getChildren().get(i).getPageNumber();

        /* then open and write to file */
        final File outputFile = new File(destination.getAbsolutePath().replace(OUTPUT_EXTENSION,
                "_" + pageNum + ".png"));

        renderer.renderToFile(pageNum, outputFile);
    }

    log.debug("Rendering of pdf took " + (System.currentTimeMillis() - t0) + " ms");
}
}
