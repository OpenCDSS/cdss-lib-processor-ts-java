// RowCursor - A RowCursor presents a scrollable view of tabular data.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2019 Colorado Department of Natural Resources

CDSS Time Series Processor Java Library is free software:  you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    CDSS Time Series Processor Java Library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with CDSS Time Series Processor Java Library.  If not, see <https://www.gnu.org/licenses/>.

NoticeEnd */

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
