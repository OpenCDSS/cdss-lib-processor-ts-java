// RowTools - Utilities related to Row and RowCursor objects.

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

/**
 *
 * Created on August 23, 2007, 3:51 PM
 *
 */

package rti.tscommandprocessor.commands.delimited;

import java.io.IOException;

//import rti.common.exception.LoggingRuntimeException;

/**
 * Utilities related to Row and RowCursor objects.
 * @author iws
 */
public class RowTools {

    /**
     * Copy the data from a Row to a Row.Mutable.
     * @param from the source Row
     * @param to the destination Row
     */
    public static void transferRow(Row from, Row.Mutable to) {
        for (int i = 0, ii = from.getLength(); i < ii; i++) {
            to.setValue(i, from.getValue(i));
        }
    }

    /**
     * Copy all of the data from a RowCursor to a RowCursor.Mutable
     * @param from the source Row
     * @param to the destination Row
     * @throws java.io.IOException If an error occurs during the copy.
     */
    public static void transferCursor(RowCursor from, RowCursor.Mutable to) throws IOException {
        while (from.next()) {
            to.next();
            transferRow(from, to);
        }
    }

}
