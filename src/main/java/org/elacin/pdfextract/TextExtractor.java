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
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * Created by IntelliJ IDEA.
 * User: elacin
 * Date: Apr 8, 2010
 * Time: 6:50:25 AM
 * To change this template use File | Settings | File Templates.
 */
public class TextExtractor {
    // --------------------- GETTER / SETTER METHODS ---------------------

    private static Options getOptions() {
        Options options = new Options();
        options.addOption("p", "password", true, "Password for decryption of document");
        options.addOption("s", "startpage", true, "First page to parse");
        options.addOption("e", "endpage", true, "Last page to parse");
        return options;
    }

    // -------------------------- PUBLIC STATIC METHODS --------------------------

    public static Writer openOutputWriter(final CommandLine cmd) {
        Writer ret = null;
        if (cmd.getArgs().length == 2) {
            try {
                final String filename = cmd.getArgs()[1];
                Loggers.getPdfExtractorLog().info("Opening " + filename + " for output");
                ret = new PrintWriter(filename, "UTF-8");
            } catch (Exception e) {
                Loggers.getPdfExtractorLog().error("Could not open output file", e);
                System.exit(2);
            }
        } else {
            Loggers.getPdfExtractorLog().info("Using stdout for output");
            ret = new PrintWriter(System.out);
        }
        return ret;
    }

    // -------------------------- STATIC METHODS --------------------------

    private static void decrypt(final PDDocument document, final String password) {
        try {
            document.decrypt(password);
        } catch (Exception e) {
            throw new RuntimeException("Error while reading encrypted PDF:", e);
        }
    }

    private static CommandLine parseParameters(final String[] args) {
        Options options = getOptions();
        CommandLineParser parser = new PosixParser();

        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            Loggers.getPdfExtractorLog().error("Could not parse command line options: " + e.getMessage());
            usage();
            System.exit(1);
        }
        return cmd;
    }

    private static void usage() {
        new HelpFormatter().printHelp(TextExtractor.class.getSimpleName() + "<PDF file> [XML output file]", getOptions());
    }

    // --------------------------- main() method ---------------------------

    public static void main(String[] args) {
        /* initialize logger */
        Loggers.getPdfExtractorLog();

        CommandLine cmd = parseParameters(args);

        /* find PDF filename */
        String pdfFile;
        if (cmd.getArgs().length > 0) {
            pdfFile = cmd.getArgs()[0];
        } else {
            Loggers.getPdfExtractorLog().error("No PDF file specified.");
            usage();
            return;
        }
        Loggers.getPdfExtractorLog().error("Opening PDF file " + pdfFile + ".");

        /* open output */
        Writer output = openOutputWriter(cmd);

        /* read pdf file */
        PDDocument document = null;
        try {
            long t0 = System.currentTimeMillis();
            document = PDDocument.load(pdfFile);
            Loggers.getPdfExtractorLog().warn("PDFBox load() took " + (System.currentTimeMillis() - t0) + "ms");
            Pdf2Xml stripper = new Pdf2Xml();
            //            stripper.setSortByPosition(true);


            if (document.isEncrypted()) {
                if (cmd.hasOption("password")) {
                    decrypt(document, cmd.getOptionValue("password"));
                } else {
                    Loggers.getPdfExtractorLog().warn("File claims to be encrypted, a password should be provided");
                }
            }

            if (cmd.hasOption("startpage")) {
                stripper.setStartPage(Integer.valueOf(cmd.getOptionValue("startpage")));
            }

            if (cmd.hasOption("endpage")) {
                stripper.setEndPage(Integer.valueOf(cmd.getOptionValue("endpage")));
            }
            t0 = System.currentTimeMillis();
            stripper.writeText(document, output);
            Loggers.getPdfExtractorLog().warn("Document analysis took " + (System.currentTimeMillis() - t0) + "ms");
        } catch (IOException e) {
            Loggers.getPdfExtractorLog().error("Error while reading " + pdfFile + ".", e);
        } finally {
            try {
                if (document != null) {
                    document.close();
                }
            } catch (IOException e) {
                Loggers.getPdfExtractorLog().error("Error while closing file", e);
            }
            try {
                output.close();
            } catch (IOException e) {
                Loggers.getPdfExtractorLog().error("Error while closing file", e);
            }
        }
    }
}

