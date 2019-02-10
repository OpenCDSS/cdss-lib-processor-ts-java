// DefaultScrollableRowCursor - Basically an in memory table which allows ScrollableRowCursor access but is mutable.

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

    private final List<Object> data;

    private final int columns;

    private List<Object> row;

    private int rowIdx;

    public DefaultScrollableRowCursor(int columns) {
        //data = new ArrayList<List<Object>>();
        data = new ArrayList<Object>();
        this.columns = columns;
    }

    public int getNumberOfRows() {
        return data.size();
    }

    public void moveTo(int row) {
        this.rowIdx = row;
        @SuppressWarnings("unchecked")
		List<Object> arow = (List<Object>) data.get(row);
        this.row = arow;
        
    }

    public boolean next() throws IOException {
        if (rowIdx + 1 >= data.size()) {
            //row = new ArrayList<Object>(columns);
            row = new ArrayList<Object>(columns);
            for (int i = 0; i < columns; i++) {
                row.add(null);
            }
            data.add(row);
            rowIdx++;
        } else {
        	@SuppressWarnings("unchecked")
			List<Object> arow = (List<Object>) data.get(rowIdx++);
            row = arow;
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
