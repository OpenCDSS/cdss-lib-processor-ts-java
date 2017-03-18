/*
 * LineSplitter.java
 * 
 * Created on Feb 4, 2008, 9:11:18 AM
 * 
 */

package rti.tscommandprocessor.commands.delimited;

import java.util.List;

/**
 * Interface to describe a class that is able to split a line into a List
 * of Strings.
 * @author iws
 */
public interface LineSplitter {

    /**
     * Split the given line into a List of String objects.
     * @param line The line to split
     * @param list An optional List to place the results in
     * @return a non-null List of String objects, will be the same as the passed
     * if list, if any
     * @throws NullPointerException if the line is null
     */
    List<String> split(String line,List<String> list);
}
