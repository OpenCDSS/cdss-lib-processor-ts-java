/**
 *
 * Created on August 23, 2007, 4:47 PM
 *
 */

package rti.tscommandprocessor.commands.delimited;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import RTi.TS.TS;
import RTi.TS.TSException;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 
 * @author iws
 */
public class Example2 {

    // make a 'CSV' file with the first row as date,
    // 'cols' number of TS value entries and a flags column
    // for a total of cols + 2 columns like this (if cols == 3)
    // DT V V V F
    // Add a header that contains some types and comments
    public static String getData(int cols, int rows) {
        DateTime dt = new DateTime(DateTime.DATE_CURRENT);
        StringBuffer sb = new StringBuffer();
        sb.append("# Header\n");
        for (int i = 0; i < cols; i++) {
            sb.append("# info for " + i + " type:abc" + i + " comment:some comment xyz" + i + "\n");
        }
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
        System.out.println("the fake CSV : ");
        System.out.println(data);
        // create a row cursor for the data using the default delimeter
        CSVCursor cursor = new CSVCursor(new BufferedReader(new StringReader(data)), null,"#");
        
        // add patterns to extract 'type' and 'comment'
        // these could be used to complete the TS below.
        cursor.addParseCommentPattern("type", Pattern.compile("type:(\\w+)"));
        cursor.addParseCommentPattern("comment", Pattern.compile("comment:(.*)$"));
        // parse the header and print out what was parsed
        Map headerInfo = cursor.parseHeader();
        List types = (List) headerInfo.get("type");
        List<String> comments = (List) headerInfo.get("comment");
        // create an assembler for using the cursor and set the date column to the first column
        TimeSeriesAssembler assembler = new TimeSeriesAssembler(cursor).setDateColumn(0);
        // the last column is the "flag" column
        int flagColumn = cols + 1;
        // there are 'cols' number of time series in this csv, configure the assembler
        // to use the given columns for TS values. they all share the same flags.
        for (int i = 0; i < cols; i++) {
            assembler.addTimeSeriesColumn(i + 1, "foo." + types.get(i) + "..15MINUTE").setFlagColumn(flagColumn);
        }
        // build the TS.
        TS[] ts = assembler.assemble();
        for (int i = 0; i < ts.length; i++) {
            ts[i].getComments().add(comments.get(i));
        }
        return ts;
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
                System.out.println(ts[i].getIdentifier());
                System.out.println(ts[i].getComments());
                // FIXME SAM 2008-01-28 Need to check Java 1.5
                //System.out.println(Arrays.toString(TSUtil.toArray(ts[i], null,null)));
            }
        }
    }

    public static void main(String[] args) throws Exception {
        test(6, 5, true);
    }

}
