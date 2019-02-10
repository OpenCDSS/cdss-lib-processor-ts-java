// RowConverterBuilder - Assist in building RowCursor and ScrollableRowCursor which use converters to 
// parse another RowCursor's data.

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
 * Created on August 23, 2007, 3:57 PM
 *
 */

package rti.tscommandprocessor.commands.delimited;

import java.io.IOException;
import java.util.Arrays;

/**
 * Assist in building RowCursor and ScrollableRowCursor which use converters to 
 * parse another RowCursor's data.
 * 
 * @author iws
 */
public class RowConverterBuilder {
    private final RowCursor cursor;
    private final Converter[] converters;

    /**
     * Create a RowConverterBuilder that uses the provided RowCursor.
     * @param cursor The RowCursor to use for building.
     */
    public RowConverterBuilder(RowCursor cursor) {
        this.cursor = cursor;
        converters = new Converter[cursor.getLength()];
        Arrays.fill(converters, Converter.IDENTITY);
    }
    
    /**
     * Create a RowConverterBuilder that uses the provided ScrollableRowCursor.
     * @param cursor The ScrollableRowCursor to use for building.
     */
    public RowConverterBuilder(ScrollableRowCursor cursor) {
        this.cursor = cursor;
        converters = new Converter[cursor.getLength()];
    }

    /**
     * Assign a Converter to a column.
     * @param col The column to assign
     * @param converter The converter to use for the column
     * @return this RowConverterBuilder for use in method chaining
     */
    public RowConverterBuilder convert(int col, Converter converter) {
        converters[col] = converter;
        return this;
    }

    /**
     * Build a ScrollableRowCursor based on the current state of the
     * builder.
     * @return A ScrollableRowCursor
     * @throws java.io.IOException If there is a problem with the underlying
     * cursor
     */
    public ScrollableRowCursor createScrollableCursor() throws IOException {
        ScrollableRowCursor converter;
        if (cursor instanceof ScrollableRowCursor) {
            converter = (ScrollableRowCursor) cursor;
        } else {
            converter = buildScrollable(cursor);
        }
        return Converters.forScrollableCursor(converter, converters);
    }

    /**
     * Build a RowCursor based on the current state of the
     * builder.
     * @return RowCursor
     */
    public RowCursor createRowCursor() {
        return Converters.forCursor(cursor, converters);
    }

    private ScrollableRowCursor buildScrollable(RowCursor cursor) throws IOException {
        DefaultScrollableRowCursor scrollable = new DefaultScrollableRowCursor(cursor.getLength());
        RowTools.transferCursor(cursor, scrollable);
        return scrollable;
    }

}
