// TimeSeriesAssembler - Build TS from a RowCursor and additional information.

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
 * Created on August 23, 2007, 4:17 PM
 *
 */

package rti.tscommandprocessor.commands.delimited;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import RTi.TS.TS;
import RTi.TS.TSUtil;
import RTi.Util.Time.DateTime;

/**
 * Build TS from a RowCursor and additional information.
 * 
 * @author iws
 */
public class TimeSeriesAssembler {
    private final RowCursor cursor;

    private int dateColumn;

    private final List/*<ColumnInfo>*/ columnInfos;
    
    private Converter dateTimeConverter = Converter.STRING_TO_DATE_TIME;

    /**
     * Create a TimeSeriesAssembler that uses the provided RowCursor to read
     * data from.
     * @param cursor The RowCursor to read from.
     */
    public TimeSeriesAssembler(RowCursor cursor) {
        this.cursor = cursor;
        columnInfos = new ArrayList/*<ColumnInfo>*/();
    }

    /**
     * Set the column that the date will be read from.
     * @todo allow multiple data columns or is this in another class??
     * @param col the column to use for reading the dates.
     * @return this TimeSeriesAssembler for use in method chaining
     */
    public TimeSeriesAssembler setDateColumn(int col) {
        this.dateColumn = col;
        return this;
    }
    
    /**
     * Set the Converter to use for parsing the time column.
     * @param dateConverter A non-null String to DateTime Converter.
     * @return this TimeSeriesAssembler for use in method chaining
     */
    public TimeSeriesAssembler setDateTimeConverter(Converter dateConverter) {
        if (dateConverter == null) {
            throw new NullPointerException("dateConverter");
        }
        this.dateTimeConverter = dateConverter;
        return this;
    }

    /**
     * Add a column to be used for reading time series data from.
     * @param dataCol The column to read data from.
     * @param tsIdent The identifier of the time series 
     * @return A ColumnInfo object for use in further configuration
     */
    public ColumnInfo addTimeSeriesColumn(int dataCol, String tsIdent) {
        ColumnInfo info = new ColumnInfo(dataCol, tsIdent);
        columnInfos.add(info);
        return info;
    }

    /**
     * Perform the read of the time series using the current configuration.
     * @return An array of TS
     * @throws java.io.IOException If an error occurs during the read
     */
    public TS[] assemble() throws IOException {
        TS[] ts = new TS[columnInfos.size()];

        ScrollableRowCursor rows = buildScrollableRowCursor();

        // get start and end dates
        rows.moveTo(0);
        DateTime start = (DateTime) rows.getValue(dateColumn);
        rows.moveTo(rows.getNumberOfRows() - 1);
        DateTime end = (DateTime) rows.getValue(dateColumn);

        // create the TS.
        for (int i = 0; i < ts.length; i++) {
            ColumnInfo info = (ColumnInfo) columnInfos.get(i);
            ts[i] = info.buildTS(start, end);
        }

        // copy indexes to local array for optimization
        final int[] columnData = new int[columnInfos.size()];
        final int[] flagData = new int[columnInfos.size()];
        for (int i = 0; i < columnData.length; i++) {
            ColumnInfo info = (ColumnInfo) columnInfos.get(i);
            columnData[i] = info.col;
            flagData[i] = info.flagColumn;
        }

        // read rows, getting date and values
        for (int i = 0, ii = rows.getNumberOfRows(); i < ii; i++) {
            rows.moveTo(i);
            DateTime date = (DateTime) rows.getValue(dateColumn);
            for (int j = 0; j < columnData.length; j++) {
                double value = ((Double) rows.getValue(columnData[j])).doubleValue();
                String flags = flagData[j] < 0 ? null : rows.getValue(flagData[j]).toString();
                int SOME_UNKNOWN_VALUE = 1;
                ts[j].setDataValue(date, value, flags, SOME_UNKNOWN_VALUE);
            }
        }
        return ts;
    }

    public static class ColumnInfo {
        private final int col;

        private final String ident;

        private int flagColumn;

        // private String dateFormat;
        private ColumnInfo(int dataCol, String ident) {
            this.col = dataCol;
            this.ident = ident;
            flagColumn = -1;
        }

        public ColumnInfo setFlagColumn(int col) {
            this.flagColumn = col;
            return this;
        }

        // public ColumnInfo setDateFormat(String format) {
        // this.dateFormat = format;
        // return this;
        // }
        TS buildTS(DateTime start, DateTime end) throws IOException {
            TS ts = null;
            try {
                ts = TSUtil.newTimeSeries(ident, true);
                ts.setIdentifier(ident);
            } catch (Exception ex) {
                throw new IOException("Illegal identifier " + ex.getMessage());
            }
            ts.setDate1(start);
            ts.setDate2(end);
            ts.allocateDataSpace();
            return ts;
        }

        public String toString() {
            return ident + ", " + col;
        }
        
    }

    private ScrollableRowCursor buildScrollableRowCursor() throws IOException {
        // build the converter we use to read datetime and values
        RowConverterBuilder converter = new RowConverterBuilder(cursor);
        converter.convert(dateColumn, dateTimeConverter);
        for (int i = 0; i < columnInfos.size(); i++) {
            int col = ((ColumnInfo) columnInfos.get(i)).col;
            converter.convert( col, Converter.STRING_TO_DOUBLE);
        }
        return converter.createScrollableCursor();
    }

}
