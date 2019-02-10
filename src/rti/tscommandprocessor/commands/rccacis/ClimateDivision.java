// ClimateDivision - Class to read and manage US Climate Divisions used by NCDC and others.

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

package rti.tscommandprocessor.commands.rccacis;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Vector;

import RTi.Util.IO.IOUtil;
import RTi.Util.Message.Message;

/**
Class to read and manage US Climate Divisions used by NCDC and others.
These are used in some data query tasks.
@TODO SAM 2011-09-07 Decide whether this should live somewhere else so that it can be used by other packages
that deal with State and County data.
*/
public class ClimateDivision {

/**
Static singleton list of climate division data, read on first access.
*/
private static List<ClimateDivision> __dataList = new Vector<ClimateDivision>();

/**
The climate division code is a sequential number within a state.
*/
private int __code;

/**
The state abbreviation (e.g., "Alabama").
*/
private String __stateName;

/**
The state number (e.g., 1 for Alabama).  Note that these DO NOT agree with FIPS.
*/
private int __stateNumber;

/**
The climate division name, mixed case (e.g., "North Valley").
*/
private String __name;

/**
Constructor for immutable data object.
*/
public ClimateDivision ( int stateNumber, String stateName, int code, String name )
{
    this.__stateNumber = stateNumber;
    this.__stateName = stateName;
    this.__code = code;
    this.__name = name;
}

/**
Check to see whether the data file needs to be read.  It is assumed to be in the "system" folder
under the software install home in a file named "ClimateDivisions.csv".
*/
private static void checkStaticData ()
{
    if ( __dataList.size() == 0 ) {
        // Try reading the climate division data
        String filename = IOUtil.getApplicationHomeDir() + File.separator + "system" + File.separator + "ClimateDivisions.csv";
        BufferedReader fp = null;
        try {
            fp = new BufferedReader ( new InputStreamReader(IOUtil.getInputStream( filename) ));
            String s;
            String [] tokens;
            ClimateDivision div;
            while ( true ) {
                s = fp.readLine();
                if ( s == null ) {
                    break;
                }
                else if ( s.startsWith("#") ) {
                    // Comment
                    continue;
                }
                else if ( s.startsWith ("\"") ) {
                    // Quotes around column headings
                    continue;
                }
                else {
                    // Data row
                    tokens = s.split(",");
                    if ( tokens.length > 2 ) {
                        div = new ClimateDivision ( Integer.parseInt(tokens[0].trim()),
                            tokens[1].trim(), Integer.parseInt(tokens[2].trim()), tokens[3].trim() );
                        __dataList.add(div);
                    }
                }
            }
            fp.close();
        }
        catch ( Exception e ) {
            String message = "Error reading climate division data from file \"" + filename + "\" (" + e + ").";
            String routine = "ClimateDivision.checkStaticData";
            Message.printWarning ( 3, routine, message );
            // Don't throw and exception because this is a low-level routine and data and the message probably
            // would get swallowed.  Instead, let the lookup call provide a message.
        }
    }
}

/**
Return the climate division code (sequential within state).
*/
public int getCode ()
{
    return this.__code;
}

/**
Return the full data list.
*/
public static List<ClimateDivision> getData ()
{
    checkStaticData();
    return __dataList;
}

/**
Return the climate division name.
*/
public String getName ()
{
    return this.__name;
}

/**
Return the state name for the climate division.
*/
public String getStateName ()
{
    return this.__stateName;
}

/**
Return the state number for the climate division.
*/
public int getStateNumber ()
{
    return this.__stateNumber;
}

/**
Look up the climate division entry from the state name and the climate division code.
@return the matching climate division entry for the state name and climate division code
*/
public ClimateDivision lookupByStateNameAndCode ( String stateName, int code )
{
    checkStaticData ();
    String stateNameUpper = stateName.toUpperCase();
    for ( ClimateDivision div : __dataList ) {
        if ( stateNameUpper.equals(div.getStateName().toUpperCase()) && (div.getCode() == code) ) {
            return div;
        }
    }
    return null;
}

/**
Look up the climate division entry from the name.
@return the matching climate division entry for the climate division name
*/
/* Probably not unique so don't implement yet
public ClimateDivision lookupByName ( String name )
{
    checkStaticData ();
    String nameUpper = name.toUpperCase();
    for ( ClimateDivision div : __dataList ) {
        if ( nameUpper.equals(div.getName().toUpperCase()) ) {
            return div;
        }
    }
    return null;
}
*/

}
