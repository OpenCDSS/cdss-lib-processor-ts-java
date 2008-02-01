/*
 * RowCursor.java
 *
 * Created on January 12, 2006, 3:06 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package rti.tscommandprocessor.commands.delimited;

import java.io.IOException;

/**
 * A RowCursor presents a scrollable view of tabular data. Because a RowCursor 
 * most likely represents an I/O resource, ensure that close() is explicity 
 * invoked when done.
 * 
 * @author Ian Schneider
 */
public interface RowCursor extends Row {

    /**
     * Close this RowCursor and release any underlying resources.
     * @throws java.io.IOException If an error occurs closing the resource.
     */
    void close() throws IOException;

    /**
     * Advance the RowCursor.
     * @return true if the advance was successful.
     * @throws java.io.IOException if an error occurs advancing the row.
     */
    boolean next() throws IOException;

    /**
     * A Mutable RowCursor
     */
    public static interface Mutable extends RowCursor, Row.Mutable {

    }

}
