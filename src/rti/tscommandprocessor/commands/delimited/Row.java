// Row - A very simple, immutable array-like data holder.

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

package rti.tscommandprocessor.commands.delimited;

/**
 * A very simple, immutable array-like data holder.
 * 
 * @author Ian Schneider
 */
public interface Row {

    /**
     * Get the value of the row at the given column.
     * 
     * @return value of column in row or null.
     */
    Object getValue(int col);

    /**
     * Get the length of this row (the number of columns).
     * 
     * @return length of row.
     */
    int getLength();

    /**
     * Like row, but mutable.
     */
    public static interface Mutable extends Row {
        /**
         * Set the value of a column.
         * @param col The column to set.
         * @param val The value to set to.
         */
        void setValue(int col, Object val);
    }
}
