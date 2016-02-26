package rti.tscommandprocessor.commands.delftfews;

import RTi.Util.Message.Message;
import RTi.Util.Time.DateTime;

/**
 * Utility methods for Delft FEWS PI XML files, as a toolkit to avoid static methods.
 */
public class DelftFewsPiXmlToolkit {
	
	/**
	 * Construct toolkit.
	 */
	public DelftFewsPiXmlToolkit () {
	}
	
	/**
	 * Convert timestep in format "second" and "21600" to "6Hour", needed because software like TSTool
	 * uses alternate intervals that don't use second as the base unit.
	 */
	public String convertTimestepToInterval ( String unit, String multiplier ) {
		if ( unit.equalsIgnoreCase("second") ) {
			// Figure out how many hours
			int seconds = Integer.parseInt(multiplier);
			int hours = seconds/3600;
			int hourFraction = seconds%3600;
			if ( hourFraction == 0 ) {
				// Assume hourly data
				return "" + hours + "Hour";
			}
			else {
				// Try to figure out minutes
				throw new RuntimeException ( "Don't know how to handle non-hourly data yet." );
			}
		}
		else if ( unit.equalsIgnoreCase("nonequidistant") ) {
			// Irregular - TODO SAM 2016-01-24 How to handle precision of date/time for period?
			return "Irregular";
		}
		return null;
	}
	
	/**
	 * Parse a date "YYYY-MM-DD" and time "hh:mm:ss" into a DateTime object.  The input is expected to be
	 * from the PI XML and therefore in the time zone for that file.  The timeZoneShift and timeZone parameters
	 * are used to convert to the output date/time to be returned in time series.
	 * @param date date string
	 * @param time time string
	 * @param timeZoneShift hours to add to file time for desired output time zone
	 * @param timeZone time zone string to assign to output date/time
	 * @param convert24HourToDay if true convert the original time to a suitable day precision date
	 * @param convert24HourToDayCutoff hour value to indicate when previous day should be used
	 */
	public DateTime parseDateTime ( String date, String time, int timeZoneShift, String timeZone,
		boolean convert24HourToDay, int convert24HourToDayCutoff ) throws Exception {
		DateTime dt = DateTime.parse(date + "T" + time);
		if ( timeZoneShift != 0 ) {
			dt.addHour(timeZoneShift);
		}
		// Not sure about this...
		if ( convert24HourToDay ) {
			// Have a 24-hour date/time where hour is such that the previous day should be used
			if ( dt.getHour() <= convert24HourToDayCutoff ) {
				dt.addDay(-1);
			}
			dt.setPrecision(DateTime.PRECISION_DAY);
		}
		if ( (timeZone != null) && !timeZone.equals("") ) {
			dt.setTimeZone(timeZone);
		}
		return dt;
	}
}