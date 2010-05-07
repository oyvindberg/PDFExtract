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

    private static final Logger createTreeLog = Logger.getLogger("createTree");
    private static final Logger textExtractorLog = Logger.getLogger("textExtractor");
    private static final Logger textNodeBuilderLog = Logger.getLogger("textNodeBuilder");

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
        String createTreeFilename = "createTree.log";
        try {
            createTreeLog.addAppender(new FileAppender(new PatternLayout("%-5p [%t]: %m%n"), createTreeFilename, false));
        } catch (IOException e) {
            textExtractorLog.warn("Could not open log file " + createTreeFilename, e);
        }

        textNodeBuilderLog.setLevel(Level.DEBUG);
        textNodeBuilderLog.setAdditivity(false);
        String textNodeBuilderFilename = "textNodeBuilder.log";

        try {
            textNodeBuilderLog.addAppender(new FileAppender(new PatternLayout("%-5p [%t]: %m%n"), textNodeBuilderFilename, false));
        } catch (IOException e) {
            textExtractorLog.warn("Could not open log file " + textNodeBuilderFilename, e);
        }

    }

    public static Logger getCreateTreeLog() {
        return createTreeLog;
    }

    public static Logger getTextExtractorLog() {
        return textExtractorLog;
    }

    public static Logger getTextNodeBuilderLog() {
        return textNodeBuilderLog;
    }
}
