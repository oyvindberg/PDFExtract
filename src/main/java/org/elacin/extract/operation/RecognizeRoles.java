package org.elacin.extract.operation;

import org.elacin.extract.text.Role;
import org.elacin.extract.text.Style;
import org.elacin.extract.tree.DocumentNode;
import org.elacin.extract.tree.TextNode;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: elacin
 * Date: Mar 23, 2010
 * Time: 3:11:50 AM
 * To change this template use File | Settings | File Templates.
 */
public class RecognizeRoles implements Operation {
    // ------------------------------ FIELDS ------------------------------

    /* these are used to recognize identifiers */
    static final Pattern id = Pattern.compile("(?:X\\d{1,2}|\\w{1,2})");
    static final Pattern refWithDotPattern = Pattern.compile("\\s*(" + id + "\\s*\\.\\s*\\d?).*", Pattern.DOTALL | Pattern.MULTILINE);
    static final Pattern numInParenthesisPattern = Pattern.compile("(\\(\\s*" + id + "\\s*\\)).*", Pattern.DOTALL | Pattern.MULTILINE);
    private final Style breadtext;

    // --------------------------- CONSTRUCTORS ---------------------------

    public RecognizeRoles(final Style breadtext) {
        this.breadtext = breadtext;
    }

    // ------------------------ INTERFACE METHODS ------------------------


    // --------------------- Interface Operation ---------------------

    public void doOperation(final DocumentNode root) {
        for (TextNode textText : root.textNodes) {
            checkForIdentifier(textText);
            checkForTopNote(textText);
            //            checkForFootNote(textText);
            checkForPageNumber(textText);
        }
    }

    // -------------------------- OTHER METHODS --------------------------

    void checkForIdentifier(final TextNode textText) {
        String mark = null;

        final String trimmedText = textText.text.trim();
        if ("".equals(trimmedText)) {
            return;
        }

        final Matcher matcher = numInParenthesisPattern.matcher(textText.text);
        if (matcher.matches()) {
            mark = matcher.group(1);
        } else {
            final Matcher matcher2 = refWithDotPattern.matcher(textText.text);
            if (matcher2.matches()) {
                mark = matcher2.group(1);
            }
        }

        /* if the first character is '*' or '-' set that as mark */
        //        final String firstChar = trimmedText.substring(0, 1);
        //        if ("*-".contains(firstChar)) {
        //            mark = firstChar;
        //        }

        if (mark != null) {
            textText.addRole(Role.IDENTIFIER, mark.trim());
        }
    }

    //    private void checkForFootNote(final TextNode leafText) {
    //        /* if the following is true for the leafText:
    //            - is in the bottom 50% of a page
    //
    //            - the previous was a footnote OR
    //            - ALL the remaining fragments on the page have a style which is smaller than the breadtext
    //         */
    //
    //        if (leafText.getPosition().getY() < (leafText.getPage().getPageFormat().getHeight() / 2)) {
    //            return;
    //        }
    //
    //        boolean isFootnote = false;
    //
    //        if (leafText.getPrevious().hasRole(Role.FOOTNOTE)) {
    //            isFootnote = true;
    //        }
    //
    //
    //        TextNode current = leafText;
    //        while (!isFootnote) {
    //            /* if we are at the end of page or document */
    //            if (current == null || current.getPage() !asdasd= leafText.getPage()) {
    //                isFootnote = true;
    //                break;
    //            } else if (current.getStyle().ySize >= breadtext.ySize) {
    //                break;
    //            }
    //            current = current.getNext();
    //        }
    //
    //        if (isFootnote) {
    //            leafText.addRole(Role.FOOTNOTE, "");
    //        }
    //    }

    private void checkForPageNumber(final TextNode textText) {
        boolean isNumber = true;
        if (textText.text.length() < 5 && textText.hasRole(Role.FOOTNOTE) || textText.hasRole(Role.HEADNOTE)) {
            for (int i = 0; i < textText.text.length(); i++) {
                if (!Character.isDigit(textText.text.charAt(i))) {
                    isNumber = false;
                    break;
                }
            }
            if (isNumber) {
                textText.addRole(Role.PAGENUMBER, textText.text);
            }
        }
    }

    private void checkForTopNote(final TextNode textText) {
        if (textText.getPosition().getY() < (textText.getPage().getPageFormat().getHeight() * 5 / 100)) {
            /* then check the font. we either want smaller than breadtext, or same size but different type */
            if (textText.getStyle().ySize < breadtext.ySize ||
                    (textText.getStyle().ySize == breadtext.ySize && !textText.getStyle().font.equals(breadtext.font))) textText.addRole(Role.HEADNOTE, "");
        }
    }
}
