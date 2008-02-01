/**
 *
 * Created on August 23, 2007, 3:51 PM
 *
 */

package rti.tscommandprocessor.commands.delimited;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

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

    /**
     * Create a RowCursor which reads a BufferedReader as CSV. 
     * At this point, the reader must be advanced to the position where data begins.
     * 
     * @param reader the BufferedReader to read the CSV from.
     * @param delimiter The delimter to use or null - defaults to ","
     */
    public static RowCursor createCSVCursor(final BufferedReader reader, String delimiter) throws IOException {
        // this is not so pretty, but we need to peek ahead and figure out howmany
        // columns there are.
        reader.mark(8096);
        String line = reader.readLine();
        final CSVParser parser = new CSVParser(delimiter);
        final int rowSize = parser.parse(line).size();
        reader.reset();

        return new RowCursor() {
            String rowText;

            // FIXME SAM 2008-01-28 Java 1.5 issue
            //List<String> rowData;
            List rowData;

            public void close() throws IOException {
                reader.close();
            }

            // FIXME SAM 2008-01-28 Java 1.5 issue
            //private List<String> data() {
            private List data() {
                // lazy row parsing
                if (rowData == null) {
                    rowData = parser.parse(rowText);
                }
                return rowData;
            }

            public int getLength() {
                return rowSize;
            }

            public Object getValue(int col) {
                try {
                    return data().get(col);
                } catch (IndexOutOfBoundsException e) {
                    // FIXME SAM 2008-01-28 Need to unify logging code
                    throw new RuntimeException("No data located at column position, " + col + ", '0' is the first column position.");
                    //throw new LoggingRuntimeException("No data located at column position, " + col + ", '0' is the first column position.");
                }
            }

            public boolean next() throws IOException {
                rowText = reader.readLine();
                if ((rowText != null) && ((rowText = rowText.trim()).length() == 0)) {
                    rowText = null;
                }
                rowData = null;
                return rowText != null;
            }
        };
    }

}
