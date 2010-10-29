// Time series data object that stores datetime and values

// class includes some static methods for TimeValues processing

//package java_lib.dmiLib;
// Provided by Dave King, Reclamation
package rti.tscommandprocessor.commands.reclamationhdb.java_lib.dmiLib;

import java.util.*;

/**
* A class of time series data objects that store datetime and values
*/

public class TimeValue extends Object
{
	public ModelDateTime date;
	public Double value;

	// construct using Julian date time

	public TimeValue (double julDay, Double v)
	{
		date = new ModelDateTime(julDay);
		value = v;
	}

	// construct using ModelDateTime

	public TimeValue (ModelDateTime mdt, Double v)
	{
		date = mdt;
		value = v;
	}

	// end constructors

	/**
	* Method to determine if data are missing in time series array.
	* @param timeSeries[] Time series values object.
	* @return True if data missing.
	*/

	public static boolean dataAreMissing(TimeValue timeSeries[])
	{
		for (int i = 0; i < timeSeries.length; i++)
		{
			if (timeSeries[i].value.doubleValue() == -9999.0)return true;
		}
		return false;

	}	// end method

	/**
	* Method to determine are missing in a TimeValues vector
	* @param Vector Vector of TimeValues.
	* @return True if data missing.
	*/

	public static boolean dataAreMissing(Vector vv)
	{
		TimeValue thisData = null;
		for (int i = 0; i < vv.size(); i++)
		{
			thisData = (TimeValue) vv.elementAt(i);
			if (thisData.value.doubleValue() == -9999.0)return true;
		}
		return false;
	}	// end method

	/**
	* Method to determine point in TimeValues vector given date time
	* @param Vector Vector of TimeValues.
	* @param dateTime Date time.
	* @return point of Vector.
	*/

	public static int pointOfTVVector(Vector vv, ModelDateTime dateTime)
	{
		TimeValue thisData = null;
		String sDateTime = dateTime.getSQLDateTime();
		for (int i = 0; i < vv.size(); i++)
		{
			thisData = (TimeValue) vv.elementAt(i);
			if (thisData.date.getSQLDateTime().equalsIgnoreCase(sDateTime))return i;
		}
		return -1;
	}	// end method

	/**
	* Method to determine point in array of TimeValues given array and date time
	* @param timeSeries[] Time series values array.
	* @param dateTime Date time.
	* @return point of array.
	*/

	public static int pointOfTVArray(TimeValue timeSeries[], ModelDateTime dateTime)
	{
		String sDateTime = dateTime.getSQLDateTime();
		for (int i = 0; i < timeSeries.length; i++)
		{
			if (timeSeries[i].date.getSQLDateTime().equalsIgnoreCase(sDateTime))return i;
		}
		return -1;
	}	// end method

	/**
	* Method to sum values to monthly
	* @param timeSeries[] Time series values object.
	*/

	public static void sumDailyToMonthly(TimeValue timeSeries[])
	{
		double value = 0.0;
		for (int i = 0; i < timeSeries.length; i++)
		{
			value = timeSeries[i].value.doubleValue();
			if (value != -9999.0)value = value * timeSeries[i].date.daysInMonth();
			timeSeries[i].value = new Double(value);
		}
		return;
	}	// end method

	/**
	* Method to sum values to annual
	* @param timeSeries[] Time series values object.
	*/

	public static void sumDailyToAnnual(TimeValue timeSeries[])
	{
		double value = 0.0;
		for (int i = 0; i < timeSeries.length; i++)
		{
			value = timeSeries[i].value.doubleValue();
			if (value != -9999.0)value = value * timeSeries[i].date.daysInYear();
			timeSeries[i].value = new Double(value);
		}
		return;
	}	// end method

	/**
	* Method to compute monthly value from average monthly values
	* @param timeSeries[] Time series values object.
	*/

	public static void MonthlyFromAverageDaily(TimeValue timeSeries[])
	{
		double value = 0.0;
		for (int i = 0; i < timeSeries.length; i++)
		{
			value = timeSeries[i].value.doubleValue();
			if (value != -9999.0)value = value * timeSeries[i].date.daysInMonth();
			timeSeries[i].value = new Double(value);
		}
		return;
	}	// end method

	/**
	* Method to compute annual value from average daily values
	* @param timeSeries[] Time series values object.
	*/

	public static void AnnualFromAverageDaily(TimeValue timeSeries[])
	{
		double value = 0.0;
		for (int i = 0; i < timeSeries.length; i++)
		{
			value = timeSeries[i].value.doubleValue();
			if (value != -9999.0)value = value * timeSeries[i].date.daysInYear();
			timeSeries[i].value = new Double(value);
		}
		return;
	}	// end method

	/**
	* Method to get average daily value from monthly
	* @param timeSeries[] Time series values object.
	*/

	public static void avgDailyFromMonthly(TimeValue timeSeries[])
	{
		double value = 0.0;
		for (int i = 0; i < timeSeries.length; i++)
		{
			value = timeSeries[i].value.doubleValue();
			if (value != -9999.0)value = value / timeSeries[i].date.daysInMonth();
			timeSeries[i].value = new Double(value);
		}
		return;
	}	// end method

	/**
	* Method to get average daily value from annual
	* @param timeSeries[] Time series values object.
	*/

	public static void avgDailyFromAnnual(TimeValue timeSeries[])
	{
		double value = 0.0;
		for (int i = 0; i < timeSeries.length; i++)
		{
			value = timeSeries[i].value.doubleValue();
			if (value != -9999.0)value = value / timeSeries[i].date.daysInYear();
			timeSeries[i].value = new Double(value);
		}
		return;
	}	// end method

	/**
	* Method to convert cfs data to acre-feet/timestep
	* @param timeSeries[] Time series values object.
	*/

	public static void cfsToAcreFeet(TimeValue timeSeries[])
	{
		double value = 0.0;
		for (int i = 0; i < timeSeries.length; i++)
		{
			value = timeSeries[i].value.doubleValue();
			if (value != -9999.0)value = value * 1.98347107438016;
			timeSeries[i].value = new Double(value);
		}
		return;
	}	// end method

	/**
	* Method to convert acre-feet/timestep data to cfs
	* @param timeSeries[] Time series values object.
	*/

	public static void acreFeetToCfs(TimeValue timeSeries[])
	{
		double value = 0.0;
		for (int i = 0; i < timeSeries.length; i++)
		{
			value = timeSeries[i].value.doubleValue();
			if (value != -9999.0)value = value / 1.98347107438016;
			timeSeries[i].value = new Double(value);
		}
		return;
	}	// end method

	/**
	* Method to tests if an int year is a leap year.
	* @param year Year.
	* @return True if leap year.
	*/

	public static boolean leapYear(int year)
	{
		if (year % 4 == 0 && year != 0 && (year % 100 != 0 || year % 400 == 0))
			return true;
		else
			return false;
	}	// end method

	/**
	* Method to return days in month given integer month and year
	* @param month Month.
	* @param year Year.
	* @return days in an integer month.
	*/

	public static int daysInMonth(int month, int year)
	{
		int[] dim = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
		if (month == 2 && TimeValue.leapYear(year))
			return 29;
		else
			return dim[month -1];
	}	// end method
}	// end class

