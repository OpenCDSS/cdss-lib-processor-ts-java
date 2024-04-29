// TimeSeriesNotFoundException - An exception to be used when a time series is not found

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2024 Colorado Department of Natural Resources

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

package rti.tscommandprocessor.core;

/**
An exception to be used when a time series is not found, for example when it is not in a database.
This can be used when code cannot simply return a null time series to indicate a time series not found.
*/
@SuppressWarnings("serial")
public class TimeSeriesNotFoundException extends RuntimeException {
	/**
	 * Construct the exception.
	 * @param message Message to be visible to the user.
	 */
	public TimeSeriesNotFoundException ( String message ) {
		super ( message );
	}
}