/*
 * Copyright 2010 Øyvind Berg (elacin@gmail.com)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.elacin.pdfextract;

import com.thoughtworks.xstream.XStream;
import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.elacin.pdfextract.renderer.PageRenderer;
import org.elacin.pdfextract.tree.DocumentNode;
import org.elacin.pdfextract.util.FileWalker;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: elacin
 * Date: Apr 8, 2010
 * Time: 6:50:25 AM
 * To change this template use File | Settings | File Templates.
 */
public class TextExtractor {
    // ------------------------------ FIELDS ------------------------------

    private static final Logger LOG = Loggers.getPdfExtractorLog();
    private final List<File> pdfFiles;
    private final File destination;
    private final int startPage;
    private final int endPage;
    private final String password;

    // --------------------------- CONSTRUCTORS ---------------------------

    public TextExtractor(final List<File> pdfFiles, final File destination, final int startPage, final int endPage, final String password) {
        this.pdfFiles = pdfFiles;
        this.destination = destination;
        this.startPage = startPage;
        this.endPage = endPage;
        this.password = password;
    }

    // --------------------- GETTER / SETTER METHODS ---------------------

    private static Options getOptions() {
        Options options = new Options();
        options.addOption("p", "password", true, "Password for decryption of document");
        options.addOption("s", "startpage", true, "First page to parse");
        options.addOption("e", "endpage", true, "Last page to parse");
        return options;
    }

    // -------------------------- OTHER METHODS --------------------------

    private void printXStreamtree(final DocumentNode root, final PrintStream outStream) {
        XStream xstream = new XStream();
        xstream.autodetectAnnotations(true);
        //                xstream.registerConverter(new RectangleConverter());
        xstream.setMode(XStream.ID_REFERENCES);
        //                xstream.setMode(XStream.NO_REFERENCES);
        xstream.toXML(root, outStream);
    }

    // --------------------------- main() method ---------------------------

    public static void main(String[] args) {
        CommandLine cmd = parseParameters(args);

        if (cmd.getArgs().length != 2) {
            usage();
            return;
        }

        int startPage = -1;
        if (cmd.hasOption("startpage")) {
            startPage = Integer.valueOf(cmd.getOptionValue("startpage"));
        }

        int endPage = -1;
        if (cmd.hasOption("endpage")) {
            endPage = Integer.valueOf(cmd.getOptionValue("endpage"));
        }

        String password = null;
        if (cmd.hasOption("password")) {
            password = cmd.getOptionValue("password");
        }

        List<File> pdfFiles = getPdfFiles(cmd.getArgs()[0]);

        final File destination = new File(cmd.getArgs()[1]);
        if (pdfFiles.size() > 1) {
            /* if we have more than one input file, demand that the output be a directory */
            if (destination.exists()) {
                if (!destination.isDirectory()) {
                    LOG.error("When specifying multiple input files, output needs to be a directory");
                    return;
                }
            } else {
                if (!destination.mkdirs()) {
                    LOG.error("Could not create output directory");
                    return;
                }
            }
        }

        final TextExtractor textExtractor = new TextExtractor(pdfFiles, destination, startPage, endPage, password);
        textExtractor.processFiles();
    }

    private static CommandLine parseParameters(final String[] args) {
        Options options = getOptions();
        CommandLineParser parser = new PosixParser();

        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            LOG.error("Could not parse command line options: " + e.getMessage());
            usage();
            System.exit(1);
        }
        return cmd;
    }

    private static void usage() {
        new HelpFormatter().printHelp(TextExtractor.class.getSimpleName() + "<PDF file/dir> <XML output file/dir>", getOptions());
    }

    protected static List<File> getPdfFiles(final String filename) {
        List<File> ret = new ArrayList<File>();
        File file = new File(filename);

        if (!file.exists()) {
            throw new RuntimeException("File " + file + " does not exist");
        } else if (file.isDirectory()) {
            try {
                ret.addAll(FileWalker.getFileListing(file, ".pdf"));
            } catch (FileNotFoundException e) {
                LOG.error("Could not find file " + filename);
            }
        } else if (file.isFile()) {
            ret.add(file);
        }

        return ret;
    }

    public final void processFiles() {
        Collection<Long> timings = new ArrayList<Long>();
        for (File pdfFile : pdfFiles) {
            try {
                final long t0 = System.currentTimeMillis();
                processFile(pdfFile);
                timings.add(System.currentTimeMillis() - t0);
            } catch (Exception e) {
                LOG.error("Error while processing PDF:", e);
            }
        }

        long sum = 0L;
        for (Long timing : timings) {
            sum += timing;
        }
        LOG.error("Total time for analyzing " + pdfFiles.size() + " documents: " + sum + "ms (" + (sum / pdfFiles.size() + "ms average)"));
    }

    protected void processFile(final File pdfFile) throws IOException {
        /* open document and parse it */
        final PDDocument doc = openDocument(pdfFile, password);
        try {
            final DocumentNode root = getDocumentTree(doc, startPage, endPage);
            printTree(pdfFile, root);
            renderPDF(pdfFile, doc, root);
        } finally {
            doc.close();
        }
    }

    protected PDDocument openDocument(final File pdfFile, final String password) {
        long t0 = System.currentTimeMillis();
        LOG.warn("Opening PDF file " + pdfFile + ".");


        try {
            final PDDocument document = PDDocument.load(pdfFile);
            if (document.isEncrypted()) {
                if (password != null) {
                    try {
                        document.decrypt(password);
                    } catch (Exception e) {
                        throw new RuntimeException("Error while reading encrypted PDF:", e);
                    }
                } else {
                    LOG.warn("File claims to be encrypted, a password should be provided");
                }
            }

            LOG.info("PDFBox load() took " + (System.currentTimeMillis() - t0) + "ms");
            return document;
        } catch (IOException e) {
            throw new RuntimeException("Error while reading " + pdfFile + ".", e);
        }
    }

    protected DocumentNode getDocumentTree(final PDDocument document, final int startPage, final int endPage) {
        try {
            Pdf2Xml stripper = new Pdf2Xml();

            if (startPage != -1) {
                LOG.warn("Reading from page " + startPage);
                stripper.setStartPage(startPage);
            }
            if (endPage != -1) {
                LOG.warn("Reading until page " + endPage);
                stripper.setEndPage(endPage);
            }

            long t1 = System.currentTimeMillis();
            stripper.writeText(document, null);
            final DocumentNode root = stripper.getRoot();

            LOG.warn("Document analysis took " + (System.currentTimeMillis() - t1) + "ms");
            return root;
        } catch (IOException e) {
            throw new RuntimeException("Error while parsing document", e);
        }
    }

    protected void printTree(final File pdfFile, final DocumentNode root) {
        /* write to file */
        final File output;
        if (destination.isDirectory()) {
            output = new File(destination, pdfFile.getName().replace(".pdf", ".elc.xml"));
        } else {
            output = destination;
        }

        final PrintStream outStream = openOutputStream(output);
        //                printXStreamtree(root, outStream);
        root.printTree(outStream);
        outStream.close();
    }

    protected PrintStream openOutputStream(final File file) {
        PrintStream ret;
        try {
            LOG.warn("Opening " + file + " for output");
            ret = new PrintStream(new BufferedOutputStream(new FileOutputStream(file, false), 8192 * 4), false, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("Could not open output file", e);
        }
        return ret;
    }

    protected void renderPDF(final File pdfFile, final PDDocument doc, final DocumentNode root) throws IOException {
        long t0 = System.currentTimeMillis();

        List pages = doc.getDocumentCatalog().getAllPages();
        for (int i = Math.max(0, startPage); i < Math.min(pages.size(), endPage); i++) {

            final PageRenderer renderer = new PageRenderer(doc, root);
            BufferedImage image = renderer.renderPage(i);

            /* then write to file */
            final File output;
            if (destination.isDirectory()) {
                output = new File(destination, pdfFile.getName().replace(".pdf", ".elc." + i + ".png"));
            } else {
                output = new File(destination.getAbsolutePath().replace(".xml", ".elc." + i + ".png"));
            }
            ImageIO.write(image, "png", output);
        }
        LOG.warn("Rendering of pdf took " + (System.currentTimeMillis() - t0) + " ms");
    }

}

