package rti.tscommandprocessor.commands.delftfews;

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
}