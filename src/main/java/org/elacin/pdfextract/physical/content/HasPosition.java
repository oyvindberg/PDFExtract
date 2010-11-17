package org.elacin.pdfextract.physical.content;

import org.elacin.pdfextract.util.Rectangle;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Oct 20, 2010 Time: 3:51:40 PM To change this
 * template use File | Settings | File Templates.
 */
public interface HasPosition extends Serializable {
// -------------------------- PUBLIC METHODS --------------------------

Rectangle getPosition();
}
