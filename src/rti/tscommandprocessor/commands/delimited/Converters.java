/**
 *
 * Created on August 23, 2007, 3:52 PM
 *
 */

package rti.tscommandprocessor.commands.delimited;

import java.io.IOException;
import java.lang.IllegalArgumentException;

import RTi.Util.Time.DateTime;
import RTi.Util.Time.DateTimeFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A location to get various Converter related implementations.
 * 
 * @author iws
 */
public class Converters {

    /**
     * Obtain a Converter that will parse a String to a DateTime object using
     * the provided format type.
     * @param dateTimeFormat A DateTime.FORMAT_NNN type
     * @return a non-null converter
     */
    public static Converter getDateTimeConverter(final int dateTimeFormat) {
        return new Converter() {
            public Object convert(Object o) throws IllegalArgumentException {
                try {
                    return DateTime.parse(o.toString(), dateTimeFormat);
                } catch (Exception ex) {
                    //throw new IllegalArgumentException(ex);
                    throw new IllegalArgumentException(ex.toString());
                }
            }
        };
    }
    
    /**
     * Obtain a Converter that will parse a String to a DateTime object using
     * the provided format specification from DateTimeFormat.
     * @param dateTimeFormat A specifier for parsing DateTime
     * @return a non-null converter
     */
    public static Converter getDateTimeFormatConverter(final String dateTimeFormatSpecifier) {
        return new Converter() {
            final DateTimeFormat format = new DateTimeFormat(dateTimeFormatSpecifier);
            public Object convert(Object o) throws IllegalArgumentException {
                try {
                    return format.parse(o.toString());
                } catch (Exception ex) {
                    throw (IllegalArgumentException) new IllegalArgumentException(ex.toString()).initCause(ex);
                }
            }
        };
    }
    
    /**
     * Obtain a Converter that will parse a String to a DateTime object using
     * the provided format specification from java.text.SimpleDateFormat.
     * @param dateTimeFormat A specifier for parsing DateTime
     * @return a non-null converter
     */
    public static Converter getDateFormatConverter(final String dateFormatSpecifier) {
        return new Converter() {
            final SimpleDateFormat format = new SimpleDateFormat(dateFormatSpecifier);
            public Object convert(Object o) throws IllegalArgumentException {
                try {
                    Date parsed = format.parse(o.toString());
                    return new DateTime(parsed);
                } catch (ParseException pe) {
                    throw new IllegalArgumentException("Unable to parse " + o.toString() + " : " + pe.getMessage());
                }
            }
        };
    }

    /**
     * Obtain a RowCursor that uses the provided cursor and the provided converters
     * to convert row values.
     * @param cursor The cursor to obtain String values from.
     * @param converters A set of Converters to use to convert.
     * @return A non-null RowCursor.
     */
    public static RowCursor forCursor(RowCursor cursor, Converter[] converters) {
        return new RowConverter(cursor, converters);
    }

    /**
     * Obtain a ScrollableRowCursor that uses the provided cursor and the provided converters
     * to convert row values.
     * @param cursor The cursor to obtain String values from.
     * @param converters A set of Converters to use to convert.
     * @return A non-null RowCursor.
     */
    public static ScrollableRowCursor forScrollableCursor(ScrollableRowCursor cursor, Converter[] converters) {
        return new ScrollableRowConverter(cursor, converters);
    }

    static class ScrollableRowConverter extends RowConverter implements ScrollableRowCursor {
        ScrollableRowConverter(ScrollableRowCursor cursor, Converter[] converters) {
            super(cursor, converters);
        }

        private ScrollableRowCursor scroll() {
            return (ScrollableRowCursor) row;
        }

        public int getNumberOfRows() {
            return scroll().getNumberOfRows();
        }

        public void moveTo(int row) {
            scroll().moveTo(row);
        }
    }

    static class RowConverter implements RowCursor {
        protected final RowCursor row;

        private final Converter[] converters;

        public RowConverter(RowCursor row, Converter[] converters) {
            this.row = row;
            this.converters = converters;
        }

        public boolean next() throws IOException {
            return row.next();
        }

        public void close() throws IOException {
            row.close();
        }

        public Object getValue(int col) {
            return converters[col].convert(row.getValue(col));
        }

        public int getLength() {
            return row.getLength();
        }
    }

}
