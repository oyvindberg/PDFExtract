package org.elacin.extract;

import org.apache.log4j.*;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: elacin
 * Date: Apr 19, 2010
 * Time: 2:50:18 AM
 * To change this template use File | Settings | File Templates.
 */
public class Loggers {

    private static final Logger createTreeLog = Logger.getLogger("CreateTree");
    private static final Logger textExtractorLog = Logger.getLogger("TextExtractor");

    static {

        /* configure root logging level */
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);

        /* set up console appender */
        final Appender consoleAppender = (Appender) Logger.getRootLogger().getAllAppenders().nextElement();
        //consoleAppender.setLayout(new PatternLayout("%d{ISO8601} - %p %C(%M):%L - %m%n"));
        consoleAppender.setLayout(new PatternLayout("%d [%p] %m%n"));

        /* set up createTreeLog */
        createTreeLog.setLevel(Level.DEBUG);
        createTreeLog.setAdditivity(false);
        String filename = "createTree.log";
        try {
            createTreeLog.addAppender(new FileAppender(new PatternLayout("%-5p [%t]: %m%n"), filename, false));
        } catch (IOException e) {
            textExtractorLog.warn("Could not open log file " + filename, e);
        }
    }

    public static Logger getCreateTreeLog() {
        return createTreeLog;
    }

    public static Logger getTextExtractorLog() {
        return textExtractorLog;
    }
}
