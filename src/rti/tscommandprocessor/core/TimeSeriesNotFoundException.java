package rti.tscommandprocessor.core;

/**
An exception to be used when a time series is not found, for example
when it is not in a database.  This can be used when code cannot simply
return a null time series to indicate a time series not found.
*/
public class TimeSeriesNotFoundException extends Exception
{
	/**
	 * Construct the exception.
	 * @param message Message to be visible to the user.
	 */
	public TimeSeriesNotFoundException ( String message )
	{
		super ( message );
	}
	
}
