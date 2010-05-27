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
import org.apache.pdfbox.pdmodel.PDDocument;
import org.elacin.pdfextract.tree.DocumentNode;
import org.elacin.pdfextract.util.FileWalker;

import java.io.*;
import java.util.ArrayList;
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

    private static final Logger log = Loggers.getPdfExtractorLog();

    // --------------------- GETTER / SETTER METHODS ---------------------

    private static Options getOptions() {
        Options options = new Options();
        options.addOption("p", "password", true, "Password for decryption of document");
        options.addOption("s", "startpage", true, "First page to parse");
        options.addOption("e", "endpage", true, "Last page to parse");
        return options;
    }

    // --------------------------- main() method ---------------------------

    public static void main(String[] args) {
        CommandLine cmd = parseParameters(args);

        if (cmd.getArgs().length != 2) {
            usage();
            return;
        }

        int startPage = -1, endPage = -1;
        if (cmd.hasOption("startpage")) {
            startPage = Integer.valueOf(cmd.getOptionValue("startpage"));
        }

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
                    log.error("When specifying multiple input files, output needs to be a directory");
                    return;
                }
            } else {
                if (!destination.mkdirs()) {
                    log.error("Could not create output directory");
                    return;
                }
            }
        }

        List<Long> timings = new ArrayList<Long>();
        for (File pdfFile : pdfFiles) {
            try {
                /* open outStream */
                final long t0 = System.currentTimeMillis();
                final DocumentNode root = getDocumentTree(pdfFile, password, startPage, endPage);

                final File output;
                if (destination.isDirectory()) {
                    output = new File(destination, pdfFile.getName().replace(".pdf", ".elc.xml"));
                } else {
                    output = destination;
                }

                final PrintStream outStream = openOutputStream(output);
                root.printTree(outStream);
                outStream.close();

                timings.add(System.currentTimeMillis() - t0);
            } catch (Exception e) {
                log.error("Error:", e);
            }
        }

        long sum = 0;
        for (Long timing : timings) {
            sum += timing;
        }
        log.error("Total time for analyzing " + pdfFiles.size() + " documents: " + sum + "ms (" + (sum / pdfFiles.size() + "ms average)"));
    }

    private static CommandLine parseParameters(final String[] args) {
        Options options = getOptions();
        CommandLineParser parser = new PosixParser();

        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            log.error("Could not parse command line options: " + e.getMessage());
            usage();
            System.exit(1);
        }
        return cmd;
    }

    private static void usage() {
        new HelpFormatter().printHelp(TextExtractor.class.getSimpleName() + "<PDF file/dir> <XML output file/dir>", getOptions());
    }

    private static List<File> getPdfFiles(final String filename) {
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

    private static DocumentNode getDocumentTree(final File pdfFile, final String password, final int startPage, final int endPage) {
        PDDocument document = null;
        final DocumentNode root;
        try {
            long t0 = System.currentTimeMillis();

            log.warn("Opening PDF file " + pdfFile + ".");

            document = PDDocument.load(pdfFile);
            log.info("PDFBox load() took " + (System.currentTimeMillis() - t0) + "ms");
            Pdf2Xml stripper = new Pdf2Xml();


            if (endPage != -1) {
                stripper.setEndPage(endPage);
            }
            if (startPage != -1) {
                stripper.setStartPage(startPage);
            }

            if (document.isEncrypted()) {
                if (password != null) {
                    try {
                        document.decrypt(password);
                    } catch (Exception e) {
                        throw new RuntimeException("Error while reading encrypted PDF:", e);
                    }
                } else {
                    log.warn("File claims to be encrypted, a password should be provided");
                }
            }

            t0 = System.currentTimeMillis();
            stripper.writeText(document, null);
            root = stripper.getRoot();
            log.warn("Document analysis took " + (System.currentTimeMillis() - t0) + "ms");
        } catch (IOException e) {
            throw new RuntimeException("Error while reading " + pdfFile + ".", e);
        } finally {
            try {
                if (document != null) {
                    document.close();
                }
            } catch (IOException e) {
                log.error("Error while closing file", e);
            }
        }
        return root;
    }

    public static PrintStream openOutputStream(final File file) {
        PrintStream ret;
        try {
            log.warn("Opening " + file + " for output");
            ret = new PrintStream(new BufferedOutputStream(new FileOutputStream(file, false), 8192 * 4), false, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("Could not open output file", e);
        }
        return ret;
    }
}

