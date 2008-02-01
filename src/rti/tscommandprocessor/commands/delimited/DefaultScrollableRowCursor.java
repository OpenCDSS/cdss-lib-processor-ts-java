/**
 *
 * Created on August 23, 2007, 4:06 PM
 *
 */

package rti.tscommandprocessor.commands.delimited;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Basically an in memory table which allows ScrollableRowCursor access but is mutable.
 * 
 * @todo - currently, the use of this as a RowCursor allows appending, but to 
 * simply iterate, one must use getNumberOfRows/moveTo
 * 
 * @author iws
 */
public class DefaultScrollableRowCursor implements ScrollableRowCursor, RowCursor.Mutable {

    private final List/*<List<Object>>*/ data;

    private final int columns;

    private List/*<Object>*/ row;

    private int rowIdx;

    public DefaultScrollableRowCursor(int columns) {
        //data = new ArrayList<List<Object>>();
        data = new ArrayList();
        this.columns = columns;
    }

    public int getNumberOfRows() {
        return data.size();
    }

    public void moveTo(int row) {
        this.rowIdx = row;
        this.row = (List) data.get(row);
    }

    public boolean next() throws IOException {
        if (rowIdx + 1 >= data.size()) {
            //row = new ArrayList<Object>(columns);
            row = new ArrayList(columns);
            for (int i = 0; i < columns; i++) {
                row.add(null);
            }
            data.add(row);
            rowIdx++;
        } else {
            row = (List) data.get(rowIdx++);
        }
        return true;
    }

    public Object getValue(int col) {
        return row.get(col);
    }

    public int getLength() {
        return columns;
    }

    public void close() throws IOException {
    }

    public void setValue(int col, Object val) {
        row.set(col, val);
    }

    public String toString() {
        return data.toString();
    }

}
