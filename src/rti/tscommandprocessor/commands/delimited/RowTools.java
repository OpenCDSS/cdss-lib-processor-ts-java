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
