/**
 *
 * Created on August 23, 2007, 3:52 PM
 *
 */

package rti.tscommandprocessor.commands.delimited;

/**
 * A RowCursor which allows random access and has a known number of rows.
 * 
 * @author iws
 */
public interface ScrollableRowCursor extends RowCursor {

    /**
     * Get the total number of rows
     * @return the number of rows, 0 or greater
     */
    int getNumberOfRows();

    /**
     * Advance the cursor to the given row
     * @param row The row to advance to.
     */
    void moveTo(int row);

}
