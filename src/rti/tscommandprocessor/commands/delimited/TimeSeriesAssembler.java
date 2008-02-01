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

    /**
     * Create a TimeSeriesAssembler that uses the provided RowCursor to read
     * data from.
     * @param cursor The RowCursor to read from.
     */
    public TimeSeriesAssembler(RowCursor cursor) {
        this.cursor = cursor;
        // FIXME SAM Java 1.5 issue
        //columnInfos = new ArrayList<ColumnInfo>();
        columnInfos = new ArrayList();
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
    }

    private ScrollableRowCursor buildScrollableRowCursor() throws IOException {
        // build the converter we use to read datetime and values
        RowConverterBuilder converter = new RowConverterBuilder(cursor);
        converter.convert(dateColumn, Converter.STRING_TO_DATE_TIME);
        for (int i = 0; i < columnInfos.size(); i++) {
            int col = ((ColumnInfo) columnInfos.get(i)).col;
            converter.convert( col, Converter.STRING_TO_DOUBLE);
        }
        return converter.createScrollableCursor();
    }

}
