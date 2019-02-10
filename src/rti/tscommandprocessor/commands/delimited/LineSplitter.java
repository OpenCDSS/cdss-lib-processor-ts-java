// LineSplitter - Interface to describe a class that is able to split a line into a List of Strings.

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
