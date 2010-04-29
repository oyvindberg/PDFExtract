package org.elacin.extract.operation;

import org.elacin.extract.tree.DocumentNode;

/**
 * Created by IntelliJ IDEA.
 * User: elacin
 * Date: Mar 23, 2010
 * Time: 3:09:37 AM
 * To change this template use File | Settings | File Templates.
 */
public interface Operation {
    // -------------------------- PUBLIC METHODS --------------------------

    void doOperation(DocumentNode root);
}
