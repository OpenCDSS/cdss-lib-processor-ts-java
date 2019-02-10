// WaterMLVersionNotSupportedException - This exception should be thrown when a method does not support a certain WaterML version.

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

package rti.tscommandprocessor.commands.wateroneflow.ws;

import java.security.InvalidParameterException;

/**
This exception should be thrown when a method does not support a certain WaterML version.
*/
public class WaterMLVersionNotSupportedException extends InvalidParameterException
{

/**
Construct with a string message.
@param s String message.
*/
public WaterMLVersionNotSupportedException( String s )
{	super ( s );
}

}
