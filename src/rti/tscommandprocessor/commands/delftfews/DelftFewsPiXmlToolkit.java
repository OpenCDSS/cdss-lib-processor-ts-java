package rti.tscommandprocessor.commands.delftfews;

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
		return null;
	}
	
	/**
	 * Parse a date "YYYY-MM-DD" and time "hh:mm:ss" into a DateTime object.
	 */
	public DateTime parseDateTime ( String date, String time, boolean convert24HourToDay ) throws Exception {
		DateTime dt = DateTime.parse(date + "T" + time);
		// Not sure about this...
		if ( convert24HourToDay ) {
			// Have a 24-hour date/time where hour 00 is in the next day so subtract a day and set precision
			if ( dt.getHour() == 0 ) {
				dt.addDay(-1);
			}
			dt.setPrecision(DateTime.PRECISION_DAY);
		}
		return dt;
	}
}