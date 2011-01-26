/*
 * Copyright 2010 ?yvind Berg (elacin@gmail.com)
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
import org.elacin.pdfextract.content.PhysicalPage;
import org.elacin.pdfextract.content.PhysicalText;
import org.elacin.pdfextract.integration.DocumentContent;
import org.elacin.pdfextract.integration.PDFSource;
import org.elacin.pdfextract.integration.PageContent;
import org.elacin.pdfextract.integration.pdfbox.PDFBoxSource;
import org.elacin.pdfextract.physical.PageSegmentator;
import org.elacin.pdfextract.physical.word.WordSegmentator;
import org.elacin.pdfextract.physical.word.WordSegmentatorImpl;
import org.elacin.pdfextract.renderer.PageRenderer;
import org.elacin.pdfextract.tree.DocumentNode;
import org.elacin.pdfextract.tree.PageNode;
import org.elacin.pdfextract.xml.SimpleXMLOutput;
import org.elacin.pdfextract.xml.TEIOutput;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.elacin.pdfextract.Constants.*;

/**
 * Created by IntelliJ IDEA. User: elacin Date: 15.01.11 Time: 19.55 To change this template use
 * File | Settings | File Templates.
 */
public class DocumentAnalyzer {

// ------------------------------ FIELDS ------------------------------
    @NotNull
    public static WordSegmentator wordSegmentator = new WordSegmentatorImpl();
    private static final Logger   log             = Logger.getLogger(DocumentAnalyzer.class);
    @NotNull
    public final DocumentNode     root            = new DocumentNode();
    private final File            destination;
    public final File             pdfFile;
    @NotNull
    private final PDFSource       source;

// --------------------------- CONSTRUCTORS ---------------------------
    public DocumentAnalyzer(File pdfFile, File destination, String password, int startPage,
                            int endPage) {

        this.destination = destination;
        this.pdfFile     = pdfFile;
        source           = new PDFBoxSource(this.pdfFile, startPage, endPage, password);
    }

// -------------------------- STATIC METHODS --------------------------
    @NotNull
    private static File getOutputFile(@NotNull File destination, @NotNull File baseFile,
                                      String extension) {

        final File output;

        if (destination.isDirectory()) {
            output = new File(destination, baseFile.getName().replace(".pdf", extension));
        } else {
            output = new File(destination.getAbsolutePath().replace(".pdf", extension));
        }

        return output;
    }

    static void renderPDF(PDFSource source, @NotNull DocumentNode root,
                          final PhysicalPage[] physicalPages, @NotNull File destination) {

        long               t0       = System.currentTimeMillis();
        final PageRenderer renderer = new PageRenderer(source, root, RENDER_RESOLUTION, physicalPages);

        for (int i = 0; i < root.getChildren().size(); i++) {

            /* one indexed pages */
            final int pageNum = root.getChildren().get(i).getPageNumber();

            /* then open and write to file */
            String path = destination.getAbsolutePath();

            path = path.replace("%p", String.valueOf(pageNum));

            DateFormat dateFormat = new SimpleDateFormat("MMddHHmm");

            path = path.replace("%d", dateFormat.format(new Date()));

            final File outputFile = new File(path);

            renderer.renderToFile(pageNum, outputFile);
        }

        log.debug("Rendering of pdf took " + (System.currentTimeMillis() - t0) + " ms");
    }

// --------------------- GETTER / SETTER METHODS ---------------------
    @NotNull
    public DocumentNode getRoot() {
        return root;
    }

// -------------------------- PUBLIC METHODS --------------------------
    public void processFile() throws IOException {

        final DocumentContent content = source.readPages();
        final long            t0      = System.currentTimeMillis();

        root.getStyles().addAll(content.getStyles());

        PhysicalPage[] physicalPages = new PhysicalPage[content.getPages().size()];

        for (int i = 0; i < content.getPages().size(); i++) {
            final PageContent inputPage = content.getPages().get(i);

            MDC.put("page", inputPage.getPageNum());

            if (inputPage.getCharacters().isEmpty()) {
                log.error("LOG01150:Page " + inputPage.getPageNum() + " is empty");

                continue;
            }

            final List<PhysicalText> words = wordSegmentator.segmentWords(inputPage.getCharacters());

            /* create a physical page instance */
            PhysicalPage pp = new PhysicalPage(words, inputPage.getGraphics(), inputPage.getPageNum(),
                                               inputPage.getDimensions());

            /* save it for rendering */
            physicalPages[i] = pp;

            /* divide the page in smaller sections */
            final PageNode pageNode = PageSegmentator.analyzePage(pp);

            root.addChild(pageNode);
        }

        MDC.remove("page");

        if (SIMPLE_OUTPUT_ENABLED) {
            new SimpleXMLOutput().writeTree(root,
                                            getOutputFile(destination, pdfFile,
                                                SIMPLE_OUTPUT_EXTENSION));
        }

        if (TEI_OUTPUT_ENABLED) {
            new TEIOutput().writeTree(root, getOutputFile(destination, pdfFile, TEI_OUTPUT_EXTENSION));
        }

        final long td = System.currentTimeMillis() - t0;

        log.info("Analyzed " + content.getPages().size() + " pages in " + td + "ms");

        if (RENDER_ENABLED) {
            renderPDF(source, root, physicalPages, getOutputFile(destination, pdfFile, ".%d.%p.png"));
        }

        source.closeSource();
    }
}
