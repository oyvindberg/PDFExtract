/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elacin.pdfextract.pdfbox;


import org.apache.log4j.Logger;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.exceptions.WrappedIOException;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.PDExtendedGraphicsState;
import org.apache.pdfbox.pdmodel.graphics.PDGraphicsState;
import org.apache.pdfbox.pdmodel.graphics.color.PDColorSpace;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObject;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.PDFOperator;
import org.apache.pdfbox.util.operator.OperatorProcessor;

import java.io.IOException;
import java.util.*;

/**
 * This class will run through a PDF content stream and execute certain operations and provide a
 * callback interface for clients that want to do things with the stream. See the PDFTextStripper
 * class for an example of how to use this class.
 *
 * @author <a href="mailto:ben@benlitchfield.com">Ben Litchfield</a>
 * @version $Revision: 1.38 $
 */
public class PDFStreamEngine extends org.apache.pdfbox.util.PDFStreamEngine {
// ------------------------------ FIELDS ------------------------------

private static final Logger log = Logger.getLogger(PDFStreamEngine.class);
/**
 * Log instance.
 */

private static final byte[] SPACE_BYTES = {(byte) 32};

/**
 * The PDF operators that are ignored by this engine.
 */
private final Set<String> unsupportedOperators = new HashSet<String>();

private PDGraphicsState graphicsState;

private Matrix textMatrix;
private Matrix textLineMatrix;
private Stack graphicsStack = new Stack();

private final Map<String, OperatorProcessor> operators = new HashMap<String, OperatorProcessor>();

private final Stack<StreamResources> streamResourcesStack = new Stack<StreamResources>();

private PDPage page;

private final Map documentFontCache = new HashMap();

private int validCharCnt;
private int totalCharCnt;

// --------------------------- CONSTRUCTORS ---------------------------

/**
 * Constructor.
 */
public PDFStreamEngine() {
    //default constructor
    validCharCnt = 0;
    totalCharCnt = 0;
}

/**
 * Constructor with engine properties.  The property keys are all PDF operators, the values are
 * class names used to execute those operators. An empty value means that the operator will be
 * silently ignored.
 *
 * @param properties The engine properties.
 * @throws IOException If there is an error setting the engine properties.
 */
public PDFStreamEngine(Properties properties) throws IOException {
    if (properties == null) {
        throw new NullPointerException("properties cannot be null");
    }
    Enumeration<?> names = properties.propertyNames();
    for (Object name : Collections.list(names)) {
        String operator = name.toString();
        String processorClassName = properties.getProperty(operator);
        if ("".equals(processorClassName)) {
            unsupportedOperators.add(operator);
        } else {
            try {
                Class<?> klass = Class.forName(processorClassName);
                OperatorProcessor processor = (OperatorProcessor) klass.newInstance();
                registerOperatorProcessor(operator, processor);
            } catch (Exception e) {
                throw new WrappedIOException("OperatorProcessor class " + processorClassName
                        + " could not be instantiated", e);
            }
        }
    }
    validCharCnt = 0;
    totalCharCnt = 0;
}

// ------------------------ OVERRIDING METHODS ------------------------

/**
 * @return Returns the colorSpaces.
 */
public Map getColorSpaces() {
    return (streamResourcesStack.peek()).colorSpaces;
}

/**
 * Get the current page that is being processed.
 *
 * @return The page being processed.
 */
public PDPage getCurrentPage() {
    return page;
}

/**
 * @return Returns the fonts.
 */
public Map getFonts() {
    return (streamResourcesStack.peek()).fonts;
}

/**
 * @return Returns the graphicsStack.
 */
public Stack getGraphicsStack() {
    return graphicsStack;
}

/**
 * @param value The graphicsStack to set.
 */
public void setGraphicsStack(Stack value) {
    graphicsStack = value;
}

/**
 * @return Returns the graphicsState.
 */
public PDGraphicsState getGraphicsState() {
    return graphicsState;
}

/**
 * @param value The graphicsState to set.
 */
public void setGraphicsState(PDGraphicsState value) {
    graphicsState = value;
}

/**
 * @return Returns the graphicsStates.
 */
public Map getGraphicsStates() {
    return (streamResourcesStack.peek()).graphicsStates;
}

/**
 * @return Returns the resources.
 */
public PDResources getResources() {
    return (streamResourcesStack.peek()).resources;
}

/**
 * @return Returns the textLineMatrix.
 */
public Matrix getTextLineMatrix() {
    return textLineMatrix;
}

/**
 * @param value The textLineMatrix to set.
 */
public void setTextLineMatrix(Matrix value) {
    textLineMatrix = value;
}

/**
 * @return Returns the textMatrix.
 */
public Matrix getTextMatrix() {
    return textMatrix;
}

/**
 * @param value The textMatrix to set.
 */
public void setTextMatrix(Matrix value) {
    textMatrix = value;
}

/**
 * Get the total number of characters in the doc (including ones that could not be mapped).
 *
 * @return The number of characters.
 */
public int getTotalCharCnt() {
    return totalCharCnt;
}

/**
 * Get the total number of valid characters in the doc that could be decoded in
 * processEncodedText().
 *
 * @return The number of valid characters.
 */
public int getValidCharCnt() {
    return validCharCnt;
}

/**
 * @return Returns the colorSpaces.
 */
public Map getXObjects() {
    return (streamResourcesStack.peek()).xobjects;
}

/**
 * Process encoded text from the PDF Stream. You should override this method if you want to perform
 * an action when encoded text is being processed.
 *
 * @param string The encoded text
 * @throws IOException If there is an error processing the string
 */
public void processEncodedText(byte[] string) throws IOException {
    /* Note on variable names.  There are three different units being used
    * in this code.  Character sizes are given in glyph units, text locations
    * are initially given in text units, and we want to save the data in
    * display units. The variable names should end with Text or Disp to
    * represent if the values are in text or disp units (no glyph units are saved).
    */
    final float fontSizeText = graphicsState.getTextState().getFontSize();
    final float horizontalScalingText = graphicsState.getTextState().getHorizontalScalingPercent()
            / 100f;
    //float verticalScalingText = horizontalScaling;//not sure if this is right but what else to do???
    final float riseText = graphicsState.getTextState().getRise();
    final float wordSpacingText = graphicsState.getTextState().getWordSpacing();
    final float characterSpacingText = graphicsState.getTextState().getCharacterSpacing();

    //We won't know the actual number of characters until
    //we process the byte data(could be two bytes each) but
    //it won't ever be more than string.length*2(there are some cases
    //were a single byte will result in two output characters "fi"

    final PDFont font = graphicsState.getTextState().getFont();

    //This will typically be 1000 but in the case of a type3 font
    //this might be a different number
    final float glyphSpaceToTextSpaceFactor = 1f / font.getFontMatrix().getValue(0, 0);
    float spaceWidthText = 0.0F;

    try { // to avoid crash as described in PDFBOX-614
        // lets see what the space displacement should be
        spaceWidthText = (font.getFontWidth(SPACE_BYTES, 0, 1) / glyphSpaceToTextSpaceFactor);
    } catch (Throwable exception) {
        log.warn(exception, exception);
    }

    if (spaceWidthText == 0.0F) {
        spaceWidthText = (font.getAverageFontWidth() / glyphSpaceToTextSpaceFactor);
        //The average space width appears to be higher than necessary
        //so lets make it a little bit smaller.
        spaceWidthText *= .80f;
    }


    /* Convert textMatrix to display units */
    final Matrix initialMatrix = new Matrix();
    initialMatrix.setValue(0, 0, 1.0F);
    initialMatrix.setValue(0, 1, 0.0F);
    initialMatrix.setValue(0, 2, 0.0F);
    initialMatrix.setValue(1, 0, 0.0F);
    initialMatrix.setValue(1, 1, 1.0F);
    initialMatrix.setValue(1, 2, 0.0F);
    initialMatrix.setValue(2, 0, 0.0F);
    initialMatrix.setValue(2, 1, riseText);
    initialMatrix.setValue(2, 2, 1.0F);

    final Matrix ctm = graphicsState.getCurrentTransformationMatrix();
    final Matrix dispMatrix = initialMatrix.multiply(ctm);

    Matrix textMatrixStDisp = textMatrix.multiply(dispMatrix);
    Matrix textMatrixEndDisp = null;

    final float xScaleDisp = textMatrixStDisp.getXScale();
    final float yScaleDisp = textMatrixStDisp.getYScale();

    final float spaceWidthDisp = spaceWidthText * xScaleDisp * fontSizeText;
    final float wordSpacingDisp = wordSpacingText * xScaleDisp * fontSizeText;

    float maxVerticalDisplacementText = 0.0F;

    float[] individualWidthsBuffer = new float[string.length];
    StringBuilder characterBuffer = new StringBuilder(string.length);

    int codeLength = 1;
    for (int i = 0; i < string.length; i += codeLength) {
        // Decode the value to a Unicode character
        codeLength = 1;
        String c = font.encode(string, i, codeLength);
        if (c == null && i + 1 < string.length) {
            //maybe a multibyte encoding
            codeLength++;
            c = font.encode(string, i, codeLength);
        }

        //todo, handle horizontal displacement
        // get the width and height of this character in text units
        float characterHorizontalDisplacementText = font.getFontWidth(string, i, codeLength)
                / glyphSpaceToTextSpaceFactor;
        maxVerticalDisplacementText = Math.max(maxVerticalDisplacementText, font.getFontHeight(
                string, i, codeLength) / glyphSpaceToTextSpaceFactor);

        // PDF Spec - 5.5.2 Word Spacing
        //
        // Word spacing works the same was as character spacing, but applies
        // only to the space character, code 32.
        //
        // Note: Word spacing is applied to every occurrence of the single-byte
        // character code 32 in a string.  This can occur when using a simple
        // font or a composite font that defines code 32 as a single-byte code.
        // It does not apply to occurrences of the byte value 32 in multiple-byte
        // codes.
        //
        // RDD - My interpretation of this is that only character code 32's that
        // encode to spaces should have word spacing applied.  Cases have been
        // observed where a font has a space character with a character code
        // other than 32, and where word spacing (Tw) was used.  In these cases,
        // applying word spacing to either the non-32 space or to the character
        // code 32 non-space resulted in errors consistent with this interpretation.
        //
        float spacingText = characterSpacingText;
        if ((string[i] == (byte) 0x20) && codeLength == 1) {
            spacingText += wordSpacingText;
        }

        /* The text matrix gets updated after each glyph is placed.  The updated
        * version will have the X and Y coordinates for the next glyph.
        */
        Matrix glyphMatrixStDisp = textMatrix.multiply(dispMatrix);

        //The adjustment will always be zero.  The adjustment as shown in the
        //TJ operator will be handled separately.
        float adjustment = 0.0F;
        // TODO : tx should be set for horizontal text and ty for vertical text
        // which seems to be specified in the font (not the direction in the matrix).
        float tx = ((characterHorizontalDisplacementText - adjustment / glyphSpaceToTextSpaceFactor)
                * fontSizeText) * horizontalScalingText;
        float ty = 0.0F;

        Matrix td = new Matrix();
        td.setValue(2, 0, tx);
        td.setValue(2, 1, ty);

        textMatrix = td.multiply(textMatrix);

        Matrix glyphMatrixEndDisp = textMatrix.multiply(dispMatrix);

        float sx = spacingText * horizontalScalingText;
        float sy = 0.0F;

        Matrix sd = new Matrix();
        sd.setValue(2, 0, sx);
        sd.setValue(2, 1, sy);

        textMatrix = sd.multiply(textMatrix);

        // determine the width of this character
        // XXX: Note that if we handled vertical text, we should be using Y here

        float widthText = glyphMatrixEndDisp.getXPosition() - glyphMatrixStDisp.getXPosition();

        while (characterBuffer.length() + (c != null ? c.length() : 1)
                > individualWidthsBuffer.length) {
            float[] tmp = new float[individualWidthsBuffer.length * 2];
            System.arraycopy(individualWidthsBuffer, 0, tmp, 0, individualWidthsBuffer.length);
            individualWidthsBuffer = tmp;
        }

        //there are several cases where one character code will
        //output multiple characters.  For example "fi" or a
        //glyphname that has no mapping like "visiblespace"
        if (c != null) {
            Arrays.fill(individualWidthsBuffer, characterBuffer.length(),
                        characterBuffer.length() + c.length(), widthText / (float) c.length());

            validCharCnt += c.length();
        } else {
            // PDFBOX-373: Replace a null entry with "?" so it is
            // not printed as "(null)"
            c = "?";

            individualWidthsBuffer[characterBuffer.length()] = widthText;
        }
        characterBuffer.append(c);

        totalCharCnt += c.length();

        if (spacingText == 0.0F && (i + codeLength) < (string.length - 1)) {
            continue;
        }

        textMatrixEndDisp = glyphMatrixEndDisp;

        float totalVerticalDisplacementDisp = maxVerticalDisplacementText * fontSizeText
                * yScaleDisp;

        float[] individualWidths = new float[characterBuffer.length()];
        System.arraycopy(individualWidthsBuffer, 0, individualWidths, 0, individualWidths.length);

        // process the decoded text
        processTextPosition(new ETextPosition(page, textMatrixStDisp, textMatrixEndDisp,
                                              totalVerticalDisplacementDisp, individualWidths,
                                              spaceWidthDisp, characterBuffer.toString(), font,
                                              fontSizeText,
                                              (int) (fontSizeText * textMatrix.getXScale()),
                                              wordSpacingDisp));

        textMatrixStDisp = textMatrix.multiply(dispMatrix);

        characterBuffer.setLength(0);
    }
}

/**
 * This is used to handle an operation.
 *
 * @param operation The operation to perform.
 * @param arguments The list of arguments.
 * @throws IOException If there is an error processing the operation.
 */
public void processOperator(String operation, List arguments) throws IOException {
    try {
        PDFOperator oper = PDFOperator.getOperator(operation);
        processOperator(oper, arguments);
    } catch (IOException e) {
        log.warn(e, e);
    }
}

/**
 * This is used to handle an operation.
 *
 * @param operator  The operation to perform.
 * @param arguments The list of arguments.
 * @throws IOException If there is an error processing the operation.
 */
protected void processOperator(PDFOperator operator, List arguments) throws IOException {
    try {
        String operation = operator.getOperation();
        OperatorProcessor processor = operators.get(operation);
        if (processor != null) {
            processor.setContext(this);
            processor.process(operator, arguments);
        } else {
            if (!unsupportedOperators.contains(operation)) {
                log.info("unsupported/disabled operation: " + operation);
                unsupportedOperators.add(operation);
            }
        }
    } catch (Exception e) {
        log.warn(e, e);
    }
}

/**
 * This will process the contents of the stream.
 *
 * @param aPage     The page.
 * @param resources The location to retrieve resources.
 * @param cosStream the Stream to execute.
 * @throws IOException if there is an error accessing the stream.
 */
public void processStream(PDPage aPage,
                          PDResources resources,
                          COSStream cosStream) throws IOException
{
    graphicsState = new PDGraphicsState(aPage.findCropBox());
    textMatrix = null;
    textLineMatrix = null;
    graphicsStack.clear();
    streamResourcesStack.clear();

    processSubStream(aPage, resources, cosStream);
}

/**
 * Process a sub stream of the current stream.
 *
 * @param aPage     The page used for drawing.
 * @param resources The resources used when processing the stream.
 * @param cosStream The stream to process.
 * @throws IOException If there is an exception while processing the stream.
 */
public void processSubStream(PDPage aPage,
                             PDResources resources,
                             COSStream cosStream) throws IOException
{
    page = aPage;
    if (resources != null) {
        StreamResources sr = new StreamResources();
        sr.fonts = resources.getFonts(documentFontCache);
        sr.colorSpaces = resources.getColorSpaces();
        sr.xobjects = resources.getXObjects();
        sr.graphicsStates = resources.getGraphicsStates();
        sr.resources = resources;
        streamResourcesStack.push(sr);
    }
    try {
        List<Object> arguments = new ArrayList<Object>();
        List tokens = cosStream.getStreamTokens();
        if (tokens != null) {
            for (final Object next : tokens) {
                if (next instanceof COSObject) {
                    arguments.add(((COSObject) next).getObject());
                } else if (next instanceof PDFOperator) {
                    processOperator((PDFOperator) next, arguments);
                    arguments = new ArrayList<Object>();
                } else {
                    arguments.add(next);
                }
                if (log.isDebugEnabled()) {
                    log.debug("token: " + next);
                }
            }
        }
    } finally {
        if (resources != null) {
            streamResourcesStack.pop();
        }
    }
}

/**
 * Register a custom operator processor with the engine.
 *
 * @param operator The operator as a string.
 * @param op       Processor instance.
 */
public final void registerOperatorProcessor(String operator, OperatorProcessor op) {
    op.setContext(this);
    operators.put(operator, op);
}

/**
 * This method must be called between processing documents.  The PDFStreamEngine caches information
 * for the document between pages and this will release the cached information. This only needs to
 * be called if processing a new document.
 */
public void resetEngine() {
    documentFontCache.clear();
    validCharCnt = 0;
    totalCharCnt = 0;
}

/**
 * @param value The colorSpaces to set.
 */
public void setColorSpaces(Map value) {
    (streamResourcesStack.peek()).colorSpaces = value;
}

/**
 * @param value The fonts to set.
 */
public void setFonts(Map value) {
    (streamResourcesStack.peek()).fonts = value;
}

/**
 * @param value The graphicsStates to set.
 */
public void setGraphicsStates(Map value) {
    (streamResourcesStack.peek()).graphicsStates = value;
}

// -------------------------- OTHER METHODS --------------------------

/**
 * A method provided as an event interface to allow a subclass to perform some specific
 * functionality when text needs to be processed.
 *
 * @param text The text to be processed.
 */
protected void processTextPosition(ETextPosition text) {
    //subclasses can override to provide specific functionality.
}

// -------------------------- INNER CLASSES --------------------------

/**
 * This is a simple internal class used by the Stream engine to handle the resources stack.
 */
private static class StreamResources {
    private Map<String, PDFont> fonts;
    private Map<String, PDColorSpace> colorSpaces;
    private Map<String, PDXObject> xobjects;
    private Map<String, PDExtendedGraphicsState> graphicsStates;
    private PDResources resources;

    private StreamResources() {
    }
}
}
