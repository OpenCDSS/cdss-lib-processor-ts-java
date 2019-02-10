// FIPSCounty - Class to read and manage USA Federal Information Processing System (FIPS) county data.

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
Class to read and manage USA Federal Information Processing System (FIPS)
county data.  These are used in some data query tasks.
@TODO SAM 2011-09-06 Decide whether this should live somewhere else so that it can be used by other packages
that deal with State and County data.
*/
public class FIPSCounty {

/**
Static singleton list of state FIPS data, read on first access.
*/
private static List<FIPSCounty> __dataList = new Vector<FIPSCounty>();

/**
The FIPS county code as a five-digit zero-padded string.
*/
private String __code;

/**
The state abbreviation (e.g., "AL").
*/
private String __stateAbbreviation;

/**
The county name, mixed case (e.g., "Autauga").
*/
private String __name;

/**
Constructor for immutable data object.
*/
public FIPSCounty ( String code, String name, String stateAbbreviation )
{
    this.__code = code;
    this.__name = name;
    this.__stateAbbreviation = stateAbbreviation;
}

/**
Check to see whether the data file needs to be read.  It is assumed to be in the "system" folder
under the software install home in a file named "FIPS-county.csv".
*/
private static void checkStaticData ()
{
    if ( __dataList.size() == 0 ) {
        // Try reading the FIPS data
        String filename = IOUtil.getApplicationHomeDir() + File.separator + "system" + File.separator + "FIPS-county.csv";
        BufferedReader fp = null;
        try {
            fp = new BufferedReader ( new InputStreamReader(IOUtil.getInputStream( filename) ));
            String s;
            String [] tokens;
            FIPSCounty fips;
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
                        fips = new FIPSCounty ( tokens[0].trim(), tokens[1].trim(), tokens[2].trim() );
                        __dataList.add(fips);
                    }
                }
            }
            fp.close();
        }
        catch ( Exception e ) {
            String message = "Error reading FIPS county data from file \"" + filename + "\" (" + e + ").";
            String routine = "FIPSCounty.checkStaticData";
            Message.printWarning ( 3, routine, message );
            // Don't throw and exception because this is a low-level routine and data and the message probably
            // would get swallowed.  Instead, let the lookup call provide a message.
        }
    }
}

/**
Return the FIPS code for the county.
*/
public String getCode ()
{
    return this.__code;
}

/**
Return the full data list.
*/
public static List<FIPSCounty> getData ()
{
    checkStaticData();
    return __dataList;
}

/**
Return the FIPS name for the county.
*/
public String getName ()
{
    return this.__name;
}

/**
Return the FIPS state abbreviation for the county.
*/
public String getStateAbbreviation ()
{
    return this.__stateAbbreviation;
}

/**
Look up the FIPS entry from the FIPS code.
@return the matching FIPS entry for the county code
*/
public FIPSCounty lookupByCode ( String code )
{
    checkStaticData ();
    String codeUpper = code.toUpperCase();
    for ( FIPSCounty fips : __dataList ) {
        if ( codeUpper.equals(fips.getCode().toUpperCase()) ) {
            return fips;
        }
    }
    return null;
}

/**
Look up the FIPS entry from the FIPS name.
@return the matching FIPS entry for the county name
*/
public FIPSCounty lookupByName ( String name )
{
    checkStaticData ();
    String nameUpper = name.toUpperCase();
    for ( FIPSCounty fips : __dataList ) {
        if ( nameUpper.equals(fips.getName().toUpperCase()) ) {
            return fips;
        }
    }
    return null;
}

}
