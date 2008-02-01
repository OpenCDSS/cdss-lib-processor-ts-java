/**
 *
 * Created on August 23, 2007, 4:47 PM
 *
 */

package rti.tscommandprocessor.commands.delimited;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;

import RTi.TS.TS;
import RTi.TS.TSException;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;

/**
 * 
 * @author iws
 */
public class Example {

    // make a 'CSV' file with the first row as date,
    // 'cols' number of TS value entries and a flags column
    // for a total of cols + 2 columns like this (if cols == 3)
    // DT V V V F
    public static String getData(int cols, int rows) {
        DateTime dt = new DateTime(DateTime.DATE_CURRENT);
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < rows; i++) {
            sb.append(dt);
            for (int j = 0; j < cols; j++) {
                sb.append(",");
                sb.append(j * 2 + i);
            }
            sb.append(",xyz").append('\n');
            dt.addInterval(TimeInterval.MINUTE, 15);
        }
        return sb.toString();
    }

    // parse the bogus data into 'cols' number of TS
    public static TS[] parseTS(String data, int cols) throws IOException {
        // create a row cursor for the data using the default delimeter
        RowCursor cursor = RowTools.createCSVCursor(new BufferedReader(new StringReader(data)), null);
        // create an assembler for using the cursor and set the date column to the first column
        TimeSeriesAssembler assembler = new TimeSeriesAssembler(cursor).setDateColumn(0);
        // the last column is the "flag" column
        int flagColumn = cols + 1;
        // there are 'cols' number of time series in this csv, configure the assembler
        // to use the given columns for TS values. they all share the same flags.
        for (int i = 0; i < cols; i++) {
            assembler.addTimeSeriesColumn(i + 1, "foo.bar" + i + "..15MINUTE").setFlagColumn(flagColumn);
        }
        // build the TS.
        return assembler.assemble();
    }

    public static void test(int cols, int rows, boolean print) throws IOException, TSException {
        String data = getData(cols, rows);
        long time = System.currentTimeMillis();
        TS[] ts = parseTS(data, cols);
        if (ts.length != cols) {
            System.out.println("invalid number of time series " + ts.length);
        }
        System.out.println(System.currentTimeMillis() - time);
        if (print) {
            for (int i = 0; i < ts.length; i++) {
                ts[i].formatOutput(new PrintWriter(System.out), null);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 5; i++) {
            test(6, 100, false);
            test(5, 1000, false);
            test(4, 10000, false);
            test(3, 100000, false);
        }

    }

}
