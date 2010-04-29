package org.elacin.extract;

import org.apache.commons.cli.*;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.FileNotFoundException;
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

    // -------------------------- STATIC METHODS --------------------------

    private static void decrypt(final PDDocument document, final String password) {
        try {
            document.decrypt(password);
        } catch (Exception e) {
            throw new RuntimeException("Error while reading encrypted PDF:", e);
        }
    }

    private static Writer openOutputWriter(final CommandLine cmd) {
        Writer ret = null;
        if (cmd.getArgs().length == 2) {
            try {
                ret = new PrintWriter(cmd.getArgs()[1]);
            } catch (FileNotFoundException e) {
                Loggers.getTextExtractorLog().error("Could not open output file", e);
                System.exit(2);
            }
        } else {
            ret = new PrintWriter(System.out);
        }
        return ret;
    }

    private static CommandLine parseParameters(final String[] args) {
        Options options = getOptions();
        CommandLineParser parser = new PosixParser();

        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            Loggers.getTextExtractorLog().error("Could not parse command line options: " + e.getMessage());
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
        Loggers.getTextExtractorLog();

        CommandLine cmd = parseParameters(args);

        /* find PDF filename */
        String pdfFile;
        if (cmd.getArgs().length > 0) {
            pdfFile = cmd.getArgs()[0];
        } else {
            Loggers.getTextExtractorLog().error("No PDF file specified.");
            usage();
            return;
        }

        /* open output */
        Writer output = openOutputWriter(cmd);

        /* read pdf file */
        PDDocument document = null;
        try {
            document = PDDocument.load(pdfFile);
            Pdf2Xml stripper = new Pdf2Xml();
//            stripper.setSortByPosition(true);


            if (document.isEncrypted()) {
                if (cmd.hasOption("password")) {
                    decrypt(document, cmd.getOptionValue("password"));
                } else {
                    Loggers.getTextExtractorLog().warn("File claims to be encrypted, a password should be provided");
                }
            }

            if (cmd.hasOption("startpage")) {
                stripper.setStartPage(Integer.valueOf(cmd.getOptionValue("startpage")));
            }

            if (cmd.hasOption("endpage")) {
                stripper.setEndPage(Integer.valueOf(cmd.getOptionValue("endpage")));
            }

            stripper.writeText(document, output);
        } catch (IOException e) {
            Loggers.getTextExtractorLog().error("Error while reading " + pdfFile + ".", e);
        } finally {
            try {
                if (document != null) {
                    document.close();
                }
            } catch (IOException e) {
                Loggers.getTextExtractorLog().error("Error while closing file", e);
            }
            try {
                output.close();
            } catch (IOException e) {
                Loggers.getTextExtractorLog().error("Error while closing file", e);
            }
        }
    }
}

