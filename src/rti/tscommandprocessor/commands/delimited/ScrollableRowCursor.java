// ScrollableRowCursor - A RowCursor which allows random access and has a known number of rows.

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
