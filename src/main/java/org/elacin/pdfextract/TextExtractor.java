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

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.elacin.pdfextract.pdfbox.PDFTextStripper;
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
 * Created by IntelliJ IDEA. User: elacin Date: Apr 8, 2010 Time: 6:50:25 AM To change this template
 * use File | Settings | File Templates.
 */
public class TextExtractor {
// ------------------------------ FIELDS ------------------------------

private static final Logger log = Logger.getLogger(TextExtractor.class);
private final List<File> pdfFiles;
private final File destination;
private final int startPage;
private final int endPage;
private final String password;
private final boolean render;

// --------------------------- CONSTRUCTORS ---------------------------

public TextExtractor(final List<File> pdfFiles,
                     final File destination,
                     final int startPage,
                     final int endPage,
                     final String password,
                     final boolean render)
{
    this.pdfFiles = pdfFiles;
    this.destination = destination;
    this.startPage = startPage;

    this.endPage = endPage;
    this.password = password;
    this.render = render;
}

// -------------------------- STATIC METHODS --------------------------

protected static List<File> findAllPdfFilesUnderDirectory(final String filename) {
    List<File> ret = new ArrayList<File>();
    File file = new File(filename);

    if (!file.exists()) {
        throw new RuntimeException("File " + file + " does not exist");
    } else if (file.isDirectory()) {
        try {
            ret.addAll(FileWalker.getFileListing(file, ".pdf"));
        } catch (FileNotFoundException e) {
            log.error("Could not find file " + filename);
        }
    } else if (file.isFile()) {
        ret.add(file);
    }

    return ret;
}

private static Options getOptions() {
    Options options = new Options();
    options.addOption("p", "password", true, "Password for decryption of document");
    options.addOption("s", "startpage", true, "First page to parse");
    options.addOption("e", "endpage", true, "Last page to parse");
    options.addOption("r", "render", false, "Render document");
    return options;
}

protected static PrintStream openOutputStream(final File file) {
    Loggers.getInterfaceLog().info("LOG00110:Opening " + file + " for output");
    try {
        return new PrintStream(new BufferedOutputStream(new FileOutputStream(file, false),
                                                        8192 * 4), false, "UTF-8");
    } catch (Exception e) {
        throw new RuntimeException("Could not open output file", e);
    }
}

protected static PDDocument openPdfDocument(final File pdfFile, final String password) {
    long t0 = System.currentTimeMillis();
    Loggers.getInterfaceLog().info("LOG00120:Opening PDF file " + pdfFile + ".");
    MDC.put("doc", pdfFile.getName());

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
                Loggers.getInterfaceLog().warn(
                        "File claims to be encrypted, a password should be provided");
            }
        }

        Loggers.getInterfaceLog().debug(
                "LOG00130:PDFBox load() took " + (System.currentTimeMillis() - t0) + "ms");
        return document;
    } catch (IOException e) {
        MDC.put("doc", "");
        throw new RuntimeException("Error while reading " + pdfFile + ".", e);
    }
}

private static CommandLine parseParameters(final String[] args) {
    Options options = getOptions();
    CommandLineParser parser = new PosixParser();

    CommandLine cmd = null;
    try {
        cmd = parser.parse(options, args);
    } catch (ParseException e) {
        Loggers.getInterfaceLog().error("Could not parse command line options: " + e.getMessage());
        usage();
        System.exit(1);
    }
    return cmd;
}

private static void usage() {
    new HelpFormatter().printHelp(
            TextExtractor.class.getSimpleName() + "<PDF file/dir> <XML output file/dir>",
            getOptions());
}

// -------------------------- PUBLIC METHODS --------------------------

public final void processFiles() {
    Collection<Long> timings = new ArrayList<Long>();
    for (File pdfFile : pdfFiles) {
        try {
            final long t0 = System.currentTimeMillis();
            processFile(pdfFile);
            timings.add(System.currentTimeMillis() - t0);
        } catch (Exception e) {
            Loggers.getInterfaceLog().error("Error while processing PDF:", e);
        }
    }

    long sum = 0L;
    for (Long timing : timings) {
        sum += timing;
    }
    Loggers.getInterfaceLog().info(String.format(
            "Total time for analyzing %d documents: %dms (%sms average)", pdfFiles.size(), sum,
            sum / pdfFiles.size()));
}

// -------------------------- OTHER METHODS --------------------------

protected void printTree(final File pdfFile, final DocumentNode root) {
    /* write to file */
    final File output;
    if (destination.isDirectory()) {
        output = new File(destination, pdfFile.getName().replace(".pdf", ".elc.xml"));
    } else {
        output = destination;
    }

    final PrintStream outStream = openOutputStream(output);
    root.printTree(outStream);
    outStream.close();
}

protected void processFile(final File pdfFile) throws IOException {
    if (startPage != -1) {
        Loggers.getInterfaceLog().info("LOG00140:Reading from page " + startPage);
    }
    if (endPage != Integer.MAX_VALUE) {
        Loggers.getInterfaceLog().info("LOG00150:Reading until page " + endPage);
    }

    PDDocument doc = null;
    try {
        /* open the document */
        doc = openPdfDocument(pdfFile, password);

        long t1 = System.currentTimeMillis();

        PDFTextStripper stripper = new PDFTextStripper(doc, startPage, endPage);
        stripper.processDocument();

        Loggers.getInterfaceLog().debug(
                "LOG00160:Document analysis took " + (System.currentTimeMillis() - t1) + "ms");

        final DocumentNode root = stripper.getDocumentNode();
        printTree(pdfFile, root);

        if (render) {
            renderPDF(pdfFile, doc, root);
        }
    } catch (IOException e) {
        throw new RuntimeException("Error while parsing document", e);
    } finally {
        if (doc != null) {
            MDC.put("doc", "");
            doc.close();
        }
    }
}

protected void renderPDF(final File pdfFile, final PDDocument doc, final DocumentNode root) {
    long t0 = System.currentTimeMillis();

    List pages = doc.getDocumentCatalog().getAllPages();
    final PageRenderer renderer = new PageRenderer(doc, root);

    /* one indexed pages */
    for (int i = Math.max(1, startPage); i <= Math.min(pages.size(), endPage); i++) {

        BufferedImage image;
        try {
            image = renderer.renderPage(i);
        } catch (RuntimeException e) {
            Loggers.getInterfaceLog().warn("Error while rendering page " + i, e);
            continue;
        }

        /* then open and write to file */
        final File output;
        if (destination.isDirectory()) {
            output = new File(destination, pdfFile.getName().replace(".pdf", ".elc." + i + ".png"));
        } else {
            output = new File(destination.getAbsolutePath().replace(".xml", ".elc." + i + ".png"));
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

    int endPage = Integer.MAX_VALUE;
    if (cmd.hasOption("endpage")) {
        endPage = Integer.valueOf(cmd.getOptionValue("endpage"));
    }

    String password = null;
    if (cmd.hasOption("password")) {
        password = cmd.getOptionValue("password");
    }

    boolean render = false;
    if (cmd.hasOption("render")) {
        render = true;
    }

    List<File> pdfFiles = findAllPdfFilesUnderDirectory(cmd.getArgs()[0]);

    final File destination = new File(cmd.getArgs()[1]);

    if (pdfFiles.size() > 1) {
        /* if we have more than one input file, demand that the output be a directory */
        if (destination.exists()) {
            if (!destination.isDirectory()) {
                Loggers.getInterfaceLog().error(
                        "When specifying multiple input files, output needs to be a directory");
                return;
            }
        } else {
            if (!destination.mkdirs()) {
                Loggers.getInterfaceLog().error("Could not create output directory");
                return;
            }
        }
    }

    final TextExtractor textExtractor = new TextExtractor(pdfFiles, destination, startPage, endPage,
                                                          password, render);
    textExtractor.processFiles();
}
}

