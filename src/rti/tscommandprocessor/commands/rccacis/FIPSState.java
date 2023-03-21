// FIPSState - Class to read and manage USA Federal Information Processing System (FIPS) state data.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2023 Colorado Department of Natural Resources

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

package rti.tscommandprocessor.commands.rccacis;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import RTi.Util.IO.IOUtil;
import RTi.Util.Message.Message;

/**
Class to read and manage USA Federal Information Processing System (FIPS) state data.
These are used in some data query tasks.
@TODO SAM 2011-09-06 Decide whether this should live somewhere else so that it can be used by other packages
that deal with State and County data.
*/
public class FIPSState {

/**
Static singleton list of state FIPS data, read on first access.
*/
private static List<FIPSState> __dataList = new ArrayList<>();

/**
The FIPS state code as a two-digit zero-padded string.
*/
private String __code;

/**
The state abbreviation (e.g., "AL").
*/
private String __abbreviation;

/**
The State name, mixed case (e.g., "Alabama").
*/
private String __name;

/**
Constructor for immutable data object.
*/
public FIPSState ( String code, String abbreviation, String name ) {
    this.__code = code;
    this.__abbreviation = abbreviation;
    this.__name = name;
}

/**
Check to see whether the data file needs to be read.
It is assumed to be in the "system" folder under the software install home in a file named "FIPS-state.csv".
*/
private static void checkStaticData () {
    if ( __dataList.size() == 0 ) {
        // Try reading the FIPS data.
        String filename = IOUtil.getApplicationHomeDir() + File.separator + "system" + File.separator + "FIPS-state.csv";
        BufferedReader fp = null;
        try {
            fp = new BufferedReader ( new InputStreamReader(IOUtil.getInputStream( filename) ));
            String s;
            String [] tokens;
            FIPSState fips;
            while ( true ) {
                s = fp.readLine();
                if ( s == null ) {
                    break;
                }
                else if ( s.startsWith("#") ) {
                    // Comment.
                    continue;
                }
                else if ( s.startsWith ("\"") ) {
                    // Quotes around column headings.
                    continue;
                }
                else {
                    // Data row.
                    tokens = s.split(",");
                    if ( tokens.length > 2 ) {
                        fips = new FIPSState ( tokens[0].trim(), tokens[1].trim(), tokens[2].trim() );
                        __dataList.add(fips);
                    }
                }
            }
            fp.close();
        }
        catch ( Exception e ) {
            String message = "Error reading FIPS state data from file \"" + filename + "\" (" + e + ").";
            String routine = "FIPSState.checkStaticData";
            Message.printWarning ( 3, routine, message );
            // Don't throw and exception because this is a low-level routine and data and the message probably
            // would get swallowed.  Instead, let the lookup call provide a message.
        }
    }
}

/**
Return the FIPS abbreviation for the state.
*/
public String getAbbreviation () {
    return this.__abbreviation;
}

/**
Return the FIPS code for the state.
*/
public String getCode () {
    return this.__code;
}

/**
Return the full data list.
*/
public static List<FIPSState> getData () {
    checkStaticData();
    return __dataList;
}

/**
Return the FIPS name for the state.
*/
public String getName () {
    return this.__name;
}

/**
Look up the FIPS entry from the FIPS abbreviation.
@return the matching FIPS entry for the state abbreviation
*/
public static FIPSState lookupByAbbreviation ( String abbreviation ) {
    checkStaticData ();
    String abbreviationUpper = abbreviation.toUpperCase();
    for ( FIPSState fips : __dataList ) {
        if ( abbreviationUpper.equals(fips.getAbbreviation().toUpperCase()) ) {
            return fips;
        }
    }
    return null;
}

/**
Look up the FIPS entry from the FIPS code.
@return the matching FIPS entry for the state code
*/
public static FIPSState lookupByCode ( String code ) {
    checkStaticData ();
    String codeUpper = code.toUpperCase();
    for ( FIPSState fips : __dataList ) {
        if ( codeUpper.equals(fips.getCode().toUpperCase()) ) {
            return fips;
        }
    }
    return null;
}

/**
Look up the FIPS entry from the FIPS name.
@return the matching FIPS entry for the state name
*/
public static FIPSState lookupByName ( String name ) {
    checkStaticData ();
    String nameUpper = name.toUpperCase();
    for ( FIPSState fips : __dataList ) {
        if ( nameUpper.equals(fips.getName().toUpperCase()) ) {
            return fips;
        }
    }
    return null;
}

}