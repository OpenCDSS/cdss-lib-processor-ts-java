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
