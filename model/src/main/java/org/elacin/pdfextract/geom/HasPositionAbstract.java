package org.elacin.pdfextract.geom;

import org.jetbrains.annotations.Nullable;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Oct 20, 2010 Time: 3:51:40 PM To change this
 * template use File | Settings | File Templates.
 */
public abstract class HasPositionAbstract implements HasPosition {

// ------------------------------ FIELDS ------------------------------
@Nullable
private Rectangle pos;

protected HasPositionAbstract() {}

// --------------------------- CONSTRUCTORS ---------------------------
protected HasPositionAbstract(final Rectangle pos) {
    this.pos = pos;
}

// --------------------- GETTER / SETTER METHODS ---------------------
public final Rectangle getPos() {

    if (pos == null) {
        calculatePos();
    }

    return pos;
}

protected void setPos(@Nullable final Rectangle pos) {
    this.pos = pos;
}

// -------------------------- PUBLIC METHODS --------------------------
public final void invalidatePos() {
    pos = null;
}
}
