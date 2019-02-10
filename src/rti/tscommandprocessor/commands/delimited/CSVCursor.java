// CSVCursor - A CSVCursor is an implementation of RowCursor that uses a BufferedReader provide values for its rows.

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

/*
 * CSVCursor.java
 * 
 * Created on Jan 4, 2008, 12:50:27 PM
 * 
 */
package rti.tscommandprocessor.commands.delimited;

import RTi.Util.Time.DateTimeFormat;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
//import rti.common.exception.LoggingRuntimeException;
/**
 * A CSVCursor is an implementation of RowCursor that uses a BufferedReader 
 * provide values for its rows.
 * @author iws
 */
public class CSVCursor implements RowCursor {

    private final String commentPrefix;
    private final Map parseCommentPatterns;
    private final LineSplitter splitter;
    private final int rowSize;
    private final List rowData;
    private boolean needParse;
    private final BufferedReader reader;
    private String rowText;

    /**
     * Create a CSVCursor that uses the provided BufferedReader to parse CSV
     * text data.
     * @param reader The reader to read from
     * @param delimiter The delimeter for column fields
     * @param commentPrefix A prefix for comments
     * @throws java.io.IOException If any exception occurs initializing
     */
    public CSVCursor(BufferedReader reader, String delimiter,
            String commentPrefix) throws IOException {
        this.reader = reader;
        this.commentPrefix = commentPrefix;
        splitter = new CSVParser(delimiter);
        parseCommentPatterns = new HashMap();

        // this is not so pretty, but we need to peek ahead and figure out how many
        // columns there are.
        int numCols = 0;
        reader.mark(8096);
        String line = null;
        while ((line = reader.readLine()) != null) {
            if (!line.startsWith(commentPrefix)) {
                numCols = splitter.split(line, null).size();
                break;
            }
        }
        rowSize = numCols;
        rowData = new ArrayList(rowSize);
        reader.reset();
    }

    /**
     * Create a CSVCursor that uses the provided BufferedReader to parse CSV
     * text data.
     * @param reader The reader to read from
     * @param delimiter The delimeter for column fields
     * @param commentPrefix A prefix for comments
     * @param commentPrefix The fixed number of columns in the body section
     * @throws java.io.IOException If any exception occurs initializing
     */
    public CSVCursor(BufferedReader reader, String delimiter,
            String commentPrefix, int numColumns) throws IOException {
        this.reader = reader;
        this.commentPrefix = commentPrefix;
        splitter = new CSVParser(delimiter);
        parseCommentPatterns = new HashMap();
        rowSize = numColumns;
        rowData = new ArrayList(rowSize);
    }

    /**
     * See if the current row is really a comment row.
     * @return true if the row is considered a comment, false otherwise.
     */
    public boolean isCommentRow() {
        return rowText.startsWith(commentPrefix);
    }

    /**
     * Add a pattern to the comment parser.
     * @param name The name of the pattern
     * @param pattern The regular expression to use for comment value extraction.
     */
    public void addParseCommentPattern(String name, Pattern pattern) {
        parseCommentPatterns.put(name, pattern);
    }

    public void close() throws IOException {
        reader.close();
    }

    /**
     * Parse any header information. This will advance through any comments
     * until the first non-comment row is found. If any comment patterns have
     * been set, the values will be extracted and placed in the returned Map.
     * @return A non-null Map containing the results of any comment value
     * extraction
     * @throws java.io.IOException If an error occurs during header parsing.
     */
    public Map parseHeader() throws IOException {
        String line = null;
        final int lineBuffer = 1024;// is 1K enough buffer?
        reader.mark(lineBuffer);
        Map parsedComments = new TreeMap();
        while ((line = reader.readLine()) != null) {
            if (!line.startsWith(commentPrefix)) {
                break;
            }
            parseComments(parsedComments, line);
            reader.mark(lineBuffer);
        }
        reader.reset();
        return parsedComments;
    }

    private List data() {
        // lazy row parsing
        if (needParse) {
            needParse = false;
            rowData.clear();
            splitter.split(rowText, rowData);
        }
        return rowData;
    }

    public int getLength() {
        return rowSize;
    }

    public Object getValue(int col) {
        try {
            return data().get(col);
        } catch (IndexOutOfBoundsException e) {
            System.out.println(data());
            throw new IllegalArgumentException("No data located at column position, " +
                    col + ", range allowed is 0 - " + (data().size() - 1));
        }
    }

    public boolean next() throws IOException {
        rowText = reader.readLine();
        if ((rowText != null) && ((rowText.trim()).length() == 0)) {
            rowText = null;
        }
        needParse = true;
        return rowText != null;
    }

    private void parseComments(Map parsed, String line) {
        Iterator patterns = parseCommentPatterns.keySet().iterator();
        while (patterns.hasNext()) {
            String name = patterns.next().toString();
            Pattern p = (Pattern) parseCommentPatterns.get(name);
            List matches = (List) parsed.get(name);
            Matcher matcher = p.matcher(line);
            while (matcher.find()) {
                if (matches == null) {
                    parsed.put(name, matches = new ArrayList(3));
                }
                matches.add(matcher.group(matcher.groupCount()));
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new DateTimeFormat("m/d/yyyy hh:MM:ss").parse("1/1/2007 12:00:00 AM");
    }
}
