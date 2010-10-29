//package java_lib.hdbLib;
// Provided by Dave King, Reclamation

package rti.tscommandprocessor.commands.reclamationhdb.java_lib.hdbLib;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

import rti.tscommandprocessor.commands.reclamationhdb.java_lib.dmiLib.ModelDateTime;
import rti.tscommandprocessor.commands.reclamationhdb.java_lib.dmiLib.SetModelDateTimeException;
import rti.tscommandprocessor.commands.reclamationhdb.java_lib.dmiLib.TimeValue;

//package java_lib.hdbLib;
//Provided by Dave King, Reclamation

// class TabColTimeValues
// utilities to retrieve and populate RDB's time series data using table and station column specification

// import classes stored in libraries

/**
* A class to retrieve and populate RDB's time series data using table and station column specification.
*/

public class TabColTimeValues
{
	// No constructor needed because we won't be creating an actual object

	/**
	* Method to get RDB data and return in a TimeValue array.
	* @param logBuffer Log file object.
	* @timeStep time step of data.
	* @param ourConn JDBC connection.
	* @param tableName
	* @param dateField Date field.
	* @param valueField Value field.
	* @param timeSeries[] TimeValue array.
	* @exception Exception
	* @return True if successfully completed.
	*/

	public static boolean getTabColData(PrintWriter logBuffer, String timeStep,
			Connection ourConn, String dbType, String tableName, String dateField,
			String valueField, TimeValue timeSeries[])
	{
		try
		{
			return TabColTimeValues.readTabColData(ourConn, dbType, tableName, timeStep,
					dateField, valueField, timeSeries);
		}
		catch (Exception e)
		{
			System.out.println ("Error executing getTabColData");
			logBuffer.println ("Error executing getTabColData");
			System.out.println ("Message: " + e.getMessage ());
			e.printStackTrace();
			return false;
		}
	}	// end method

	/**
	* Method to put RDB data from a TimeValue array.
	* @param logBuffer Log file object.
	* @timeStep time step of data.
	* @param ourConn JDBC connection.
	* @param tableName
	* @param dateField Date field.
	* @param valueField Value field.
	* @param timeSeries[] TimeValue array.
	* @exception Exception
	* @return True if successfully completed.
	*/

	public static boolean putTabColData(PrintWriter logBuffer, String timeStep,
			Connection ourConn, String dbType, String tableName, String dateField,
			String valueField, TimeValue timeSeries[])
	{
		try
		{
			return TabColTimeValues.writeTabColData(logBuffer, ourConn, dbType, tableName, timeStep,
					dateField, valueField, timeSeries);
		}
		catch (Exception e)
		{
			System.out.println ("Error executing putTabColData");
			logBuffer.println ("Error executing putTabColData");
			System.out.println ("Message: " + e.getMessage ());
			e.printStackTrace();
			return false;
		}
	}	// end method

	/**
	* Method to read an RDB time series table from a TimeValue array.
	* @param ourConn JDBC connection.
	* @param tableName Table name.
	* @param timeStep time step of data.
	* @param dateField Date field.
	* @param valueField Value field.
	* @param timeSeries[] TimeValue array.
	* @exception Exception
	* @return True if successfully completed.
	*/

	public static boolean readTabColData(Connection ourConn, String dbType, String tableName,
		String timeStep, String dateField, String valueField, TimeValue timeSeries[]) throws Exception
	{
		ResultSet rs = null;
		Statement stmt = null;
		boolean ranOk = false;
		String sqlCommand;
		ModelDateTime theDate;
		int thePoint;
		Double doubleValue;
		String dateString;
		int numberPeriods = timeSeries.length;

		if (dbType.equalsIgnoreCase("Access"))
		{
			sqlCommand  = "select `" + dateField + "`, `" + valueField + "` from " + tableName
						+ " where `" + dateField + "` >= "
						+ "#" + timeSeries[0].date.getAccessDateTime()
						+ "# and `" + dateField + "` <= #"
						+ timeSeries[numberPeriods - 1].date.advanceNTimesteps(timeStep, 1).getAccessDateTime()
						+ "# order by `" + dateField + "`";
		}
		else
		{
			sqlCommand  = "select " + dateField + ", " + valueField + " from " + tableName
						+ " where " + dateField + " >= "
						+ "'" + timeSeries[0].date.getSQLDateTime()
						+ "' and " + dateField + " <= '"
						+ timeSeries[numberPeriods - 1].date.advanceNTimesteps(timeStep, 1).getSQLDateTime()
						+ "' order by " + dateField;
		}

//		System.out.println("Query string is " + sqlCommand);

		try
		{
			stmt = ourConn.createStatement();
			rs = stmt.executeQuery(sqlCommand);
			while (rs.next())
			{
				dateString = rs.getString(1);
				doubleValue = new Double(rs.getDouble(2));
				if(dateString==null || doubleValue==null) break;
				theDate = new ModelDateTime(dateString);
				thePoint = timeSeries[0].date.timestepsBetween(timeStep, theDate);

/*
System.out.print("The point, date, and value are: " + thePoint + " - ");
System.out.print(dateString + " - " + theDate.getSQLDateTime() + " - ");
System.out.println(doubleValue);
*/

				if (thePoint >= 0 && thePoint < numberPeriods) timeSeries[thePoint].value = doubleValue;
			}
			ranOk = true;
		}
		catch (NullPointerException e)
		{
			System.out.println ("Null pointer exception found executing readTabColData.");
			e.printStackTrace();
			return false;
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing HDB query.");
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			e.printStackTrace();
		}
		finally
		{
			rs.close();
			stmt.close();
		}
		return ranOk;

	}	// end method

	/**
	* Method to write data to RDB model tables
	* @param logBuffer Log file object.
	* @param ourConn JDBC connection.
	* @param tableName Table name.
	* @param timeStep time step of data.
	* @param dateField Date field.
	* @param valueField Value field.
	* @param timeSeries[] Time series values array.
	* @exception Exception
	* @return True if successfully completed.
	*/

	public static boolean writeTabColData(PrintWriter logBuffer, Connection ourConn, String dbType,
		String tableName, String timeStep, String dateField, String valueField,
		TimeValue timeSeries[]) throws Exception
	{
		int i;
		String updateStatement, insertStatement;
		PreparedStatement ps = null;
		boolean action;

		if (dbType.equalsIgnoreCase("Access"))
		{
			if (valueField.equalsIgnoreCase("value"))
			{
				insertStatement = "insert into " + tableName
								+ " (`" + dateField + "`, [value]"
								+ ") values(?,?)";
			}
			else
			{
				insertStatement = "insert into " + tableName
								+ " (`" + dateField + "`, " + valueField
								+ ") values(?,?)";
			}
		}
		else
		{
			insertStatement = "insert into " + tableName
							+ " (" + dateField + ", " + valueField
							+ ") values(?,?)";
		}
//		System.out.println("Insert statement is '" + insertStatement + "'");

		if (dbType.equalsIgnoreCase("Access"))
		{
			updateStatement = "update " + tableName + " set " + tableName + ".`" + valueField
							+ "` = ? where `" + dateField + "` = ?";
		}
		else
		{
			updateStatement = "update " + tableName + " set " + valueField
							+ " = ? where "	+ dateField + " = ?";
		}
//		System.out.println("Update statement is '" + updateStatement + "'");

		// retrieve an array of existing data

		TimeValue existingData[] = new TimeValue[timeSeries.length];

		for (i = 0; i < timeSeries.length; i++)
		{
			existingData[i] = new TimeValue(timeSeries[i].date, Double.valueOf("-9999"));
		}

		TabColTimeValues.getTabColData(logBuffer, timeStep, ourConn, dbType, tableName, dateField, valueField, existingData);

		// loop thru all periods and process by inserts and updates

		for (i = 0; i < timeSeries.length; i++)
		{
//			System.out.println("Processsing " + timeSeries[i].date.getJDBCDateTime() + " SV is " + timeSeries[i].value.doubleValue() + " EV is " + existingData[i].value.doubleValue());
			action = false;
			if (timeSeries[i].value.doubleValue() != -9999.0)
			{
				if (existingData[i].value.doubleValue() != -9999.0)
				{

					// record already exists - do an update

					if(Math.abs(existingData[i].value.doubleValue() - timeSeries[i].value.doubleValue())
> 0.0000001)
					{
						ps = ourConn.prepareStatement(updateStatement);
						ps.setDouble(1, timeSeries[i].value.doubleValue());
						if (timeStep.equalsIgnoreCase("1HOUR"))
						{
							ps.setTimestamp(2, timeSeries[i].date.getJDBCDateTime());
						}
						else
						{
							ps.setDate(2, timeSeries[i].date.getJDBCDate());
						}
						action = true;
					}
				}
				else
				{

					// record does not exists - do an insert

					// fill in prepared statement for sql call

					ps = ourConn.prepareStatement(insertStatement);
					if (timeStep.equalsIgnoreCase("1HOUR"))
					{
						ps.setTimestamp(1, timeSeries[i].date.getJDBCDateTime());
					}
					else
					{
						ps.setDate(1, timeSeries[i].date.getJDBCDate());
					}
					ps.setDouble(2, timeSeries[i].value.doubleValue());
					action = true;
				}

				if (action)
				{
					try
					{
						ps.executeUpdate();
						ps.close();
					}
					catch (SQLException e)
					{
						System.err.println ("SQLState: " + e.getSQLState ());
						System.err.println ("Message: " + e.getMessage ());
						System.err.println ("Vendor: " + e.getErrorCode ());
						e.printStackTrace();
						return false;
					}
				}

			}	// end if of existence test

		}	// end of loop thru number of periods

		return true;

	}	// end method

	/**
	* Method to get RDB data and return in a TimeValue vector.
	* @param logBuffer Log file object.
	* @timeStep time step of data.
	* @param ourConn JDBC connection.
	* @param tableName
	* @param dateField Date field.
	* @param valueField Value field.
	* @param tvVec[] TimeValue vector.
	* @exception Exception
	* @return True if successfully completed.
	*/

	public static boolean getTabColData(PrintWriter logBuffer, String timeStep,
			ModelDateTime startDate, ModelDateTime endDate,
			Connection ourConn, String dbType, String tableName, String dateField,
			String valueField, Vector tvVec)
	{
		try
		{
			return TabColTimeValues.readTabColData(ourConn, dbType, tableName, startDate, endDate,
					timeStep, dateField, valueField, tvVec);
		}
		catch (Exception e)
		{
			System.out.println ("Error executing getTabColData");
			logBuffer.println ("Error executing getTabColData");
			System.out.println ("Message: " + e.getMessage ());
			e.printStackTrace();
			return false;
		}
	}	// end method

	/**
	* Method to put RDB data from a TimeValue vector.
	* @param logBuffer Log file object.
	* @timeStep time step of data.
	* @param ourConn JDBC connection.
	* @param tableName
	* @param dateField Date field.
	* @param valueField Value field.
	* @param tvVec[] TimeValue vector.
	* @exception Exception
	* @return True if successfully completed.
	*/

	public static boolean putTabColData(PrintWriter logBuffer, String timeStep,
			ModelDateTime startDate, ModelDateTime endDate,
			Connection ourConn, String dbType, String tableName, String dateField,
			String valueField, Vector tvVec)
	{
		try
		{
			return TabColTimeValues.writeTabColData(logBuffer, ourConn, dbType, startDate, endDate,
						tableName, timeStep, dateField, valueField, tvVec);
		}
		catch (Exception e)
		{
			System.out.println ("Error executing putTabColData");
			logBuffer.println ("Error executing putTabColData");
			System.out.println ("Message: " + e.getMessage ());
			e.printStackTrace();
			return false;
		}
	}	// end method

	/**
	* Method to read an RDB time series table from a TimeValue vector.
	* @param ourConn JDBC connection.
	* @param tableName Table name.
	* @param timeStep time step of data.
	* @param dateField Date field.
	* @param valueField Value field.
	* @param tvVec[] TimeValue vector.
	* @exception Exception
	* @return True if successfully completed.
	*/

	public static boolean readTabColData(Connection ourConn, String dbType, String tableName,
		ModelDateTime startDate, ModelDateTime endDate, String timeStep, String dateField,
		String valueField, Vector tvVec) throws Exception
	{
		ResultSet rs = null;
		Statement stmt = null;
		boolean ranOk = false;
		String sqlCommand;
		Double doubleValue;
		String dateString;
		TimeValue thisData = null;
		ModelDateTime theDate = null;

		if (dbType.equalsIgnoreCase("Access"))
		{
			sqlCommand  = "select " + dateField + ", " + valueField + " from " + tableName
						+ " where " + dateField + " >= "
						+ "{ts '" + endDate.getSQLDateTime()
						+ "'} and " + dateField + " <= {ts '"
						+ endDate.advanceNTimesteps(timeStep, 1).getSQLDateTime()
						+ "'} order by " + dateField;
		}
		else
		{
			sqlCommand  = "select " + dateField + ", " + valueField + " from " + tableName
						+ " where " + dateField + " >= "
						+ "'" + endDate.getSQLDateTime()
						+ "' and " + dateField + " <= '"
						+ endDate.advanceNTimesteps(timeStep, 1).getSQLDateTime()
						+ "' order by " + dateField;
		}

		try
		{
			stmt = ourConn.createStatement();
			rs = stmt.executeQuery(sqlCommand);
			while (rs.next())
			{
				dateString = rs.getString(1);
				if(dateString==null) break;
				theDate = new ModelDateTime(dateString);
				doubleValue = new Double(rs.getDouble(2));
				if(doubleValue==null) break;
				thisData = new TimeValue(theDate, doubleValue);
				tvVec.addElement(thisData);
			}
			ranOk = true;
		}
		catch (NullPointerException e)
		{
			System.out.println ("Null pointer exception found executing readTabColData.");
			e.printStackTrace();
			return false;
		}
		catch (SetModelDateTimeException e)
		{
			System.out.println ("Set model date time exception found executing readRdbData.");
			e.printStackTrace();
			return false;
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing HDB query.");
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			e.printStackTrace();
		}
		finally
		{
			rs.close();
			stmt.close();
		}
		return ranOk;

	}	// end method

	/**
	* Method to write data to RDB model tables
	* @param logBuffer Log file object.
	* @param ourConn JDBC connection.
	* @param tableName Table name.
	* @param timeStep time step of data.
	* @param dateField Date field.
	* @param valueField Value field.
	* @param tvVec[] Time series values vector.
	* @exception Exception
	* @return True if successfully completed.
	*/

	public static boolean writeTabColData(PrintWriter logBuffer, Connection ourConn, String dbType,
		ModelDateTime startDate, ModelDateTime endDate, String tableName, String timeStep,
		String dateField, String valueField, Vector tvVec) throws Exception
	{
		int i;
		int thePoint = 0;
		String updateStatement, insertStatement;
		PreparedStatement ps = null;
		boolean action;
		TimeValue thisData = null;
		TimeValue thatData = null;

		if (dbType.equalsIgnoreCase("Access"))
		{
			if (valueField.equalsIgnoreCase("value"))
			{
				insertStatement = "insert into " + tableName
								+ " (" + dateField + ", [value]"
								+ ") values(?,?)";
			}
			else
			{
				insertStatement = "insert into " + tableName
								+ " (" + dateField + ", " + valueField
								+ ") values(?,?)";
			}
		}
		else
		{
			insertStatement = "insert into " + tableName
							+ " (" + dateField + ", " + valueField
							+ ") values(?,?)";
		}

		if (dbType.equalsIgnoreCase("Access"))
		{
			updateStatement = "update " + tableName + " set " + tableName + ".`" + valueField
							+ "` = ? where `" + dateField + "` = ?";
		}
		else
		{
			updateStatement = "update " + tableName + " set " + valueField
							+ " = ? where "	+ dateField + " = ?";
		}

		// retrieve an vector of existing data

		Vector existingData = new Vector (900,300);

		TabColTimeValues.getTabColData(logBuffer, timeStep, startDate, endDate, ourConn, dbType,
									tableName, dateField, valueField, existingData);

		// loop thru all periods and process by inserts and updates

		for (i = 0; i < tvVec.size(); i++)
		{
			thisData = (TimeValue) tvVec.elementAt(i);
			action = false;
			if (thisData.value.doubleValue() != -9999.0)
			{
				thePoint = TimeValue.pointOfTVVector(existingData, thisData.date);
				if (thePoint > -1)
				{
					// record already exists - do an update

					thatData = (TimeValue) existingData.elementAt(thePoint);
					if(Math.abs(thatData.value.doubleValue() - thisData.value.doubleValue())
> 0.0000001)
					{
						ps = ourConn.prepareStatement(updateStatement);
						ps.setDouble(1, thisData.value.doubleValue());
						if (timeStep.equalsIgnoreCase("1HOUR"))
						{
							ps.setTimestamp(2, thisData.date.getJDBCDateTime());
						}
						else
						{
							ps.setDate(2, thisData.date.getJDBCDate());
						}
						action = true;
					}
				}
				else
				{
					// record does not exists - do an insert using a prepared statement

					ps = ourConn.prepareStatement(insertStatement);
					if (timeStep.equalsIgnoreCase("1HOUR"))
					{
						ps.setTimestamp(1, thisData.date.getJDBCDateTime());
					}
					else
					{
						ps.setDate(1, thisData.date.getJDBCDate());
					}
					ps.setDouble(2, thisData.value.doubleValue());
					action = true;
				}

				if (action)
				{
					try
					{
						ps.executeUpdate();
						ps.close();
					}
					catch (SQLException e)
					{
						System.err.println ("SQLState: " + e.getSQLState ());
						System.err.println ("Message: " + e.getMessage ());
						System.err.println ("Vendor: " + e.getErrorCode ());
						e.printStackTrace();
						return false;
					}
				}

			}	// end if of existence test

		}	// end of loop thru number of periods

		return true;

	}	// end method

	/**
	* Method to put three time periods of data into RDB.
	* @param logBuffer Log file object.
	* @param timeStep time step of data.
	* @param ourConn JDBC connection.
	* @param dateField Date field.
	* @param valueField Value field.
	* @param histTimeSteps Number of historic time steps.
	* @param curTimeSteps Number of current time steps.
	* @param histTable Historic table.
	* @param curTable Current table.
	* @param foreTable Forecast table.
	* @param timeSeries[] Time series values array.
	* @exception Exception.
	* @return True if successfully completed.
	*/

	public static boolean put3PeriodValues(PrintWriter logBuffer, String timeStep,
		Connection ourConn, String dbType, String dateField, String valueField,
		int histTimeSteps, int curTimeSteps, String histTable,
		String curTable, String foreTable, TimeValue timeSeries[]) throws Exception
	{
		ModelDateTime sDate;
		int k = 0;
		int j = 0;
		int method_id = 0;
		int numberPeriods = timeSeries.length;
		int foreTimeSteps = numberPeriods - histTimeSteps - curTimeSteps;

		if (histTimeSteps > 0 && !histTable.equals("NaN"))
		{

		//	historic period (less than or equal initial date of RiverWare Model)

			sDate = timeSeries[0].date;
			k = -1;

			//	set up array of historic period data

			TimeValue tsData[] = new TimeValue[histTimeSteps];

			for (j = 0; j < histTimeSteps; j++)
			{
				k++;
				tsData[j] = new TimeValue(sDate.advanceNTimesteps(timeStep, j), timeSeries[k].value);
			}

			//	put values into RDB

			if (!TabColTimeValues.putTabColData(logBuffer, timeStep, ourConn, dbType, histTable,
						dateField, valueField, tsData))
			{
				System.out.println("Unable to populate historic period data for " + histTable + " - " + valueField + ".");
				logBuffer.println("Unable to populate historic period data for " + histTable + " - " + valueField + ".");
				return false;
			}
		}

		if (curTimeSteps > 0 && !curTable.equals("NaN"))

		//	current period (from date of RiverWare Model to start of forecast period)

		{
			sDate = timeSeries[histTimeSteps].date;
			k = histTimeSteps - 1;

			// set up array of current period data

			TimeValue tsData[] = new TimeValue[curTimeSteps];

			for (j = 0; j < curTimeSteps; j++)
			{
				k++;
				tsData[j] = new TimeValue(sDate.advanceNTimesteps(timeStep, j), timeSeries[k].value);
			}

			//	put values into RDB

			if (!TabColTimeValues.putTabColData(logBuffer, timeStep, ourConn, dbType, curTable,
						dateField, valueField, tsData))
			{
				System.out.println("Unable to populate current period data for " + curTable + " - " + valueField + ".");
				logBuffer.println("Unable to populate current period data for " + curTable + " - " + valueField + ".");
				return false;
			}
		}

		//	forecast period

		if (foreTimeSteps > 0 && !foreTable.equalsIgnoreCase("NaN"))
		{
			sDate = timeSeries[histTimeSteps + curTimeSteps].date;
			k = histTimeSteps + curTimeSteps - 1;

			// set up array of forecast period data

			TimeValue tsData[] = new TimeValue[foreTimeSteps];

			for (j = 0; j < foreTimeSteps; j++)
			{
				k++;
				tsData[j] = new TimeValue(sDate.advanceNTimesteps(timeStep, j), timeSeries[k].value);
			}

			//	put values into RDB

			if (!TabColTimeValues.putTabColData(logBuffer, timeStep, ourConn, dbType, foreTable,
						dateField, valueField, tsData))
			{
				System.out.println("Unable to populate forecast period data for " + foreTable + " - " + valueField + ".");
				return false;
			}
		}
		return true;

	}	// end method

	/**
	* Method to get three time periods of data from RDB.
	* @param logBuffer Log file object.
	* @param timeStep time step of data.
	* @param ourConn JDBC connection.
	* @param dateField Date field.
	* @param valueField Value field.
	* @param histTimeSteps Number of historic time steps.
	* @param curTimeSteps Number of current time steps.
	* @param histTable Historic table.
	* @param curTable Current table.
	* @param foreTable Forecast table.
	* @param timeSeries[] Time series values array.
	* @exception Exception
	* @return True if successfully completed.
	*/

	public static boolean get3PeriodValues(PrintWriter logBuffer, String timeStep,
		Connection ourConn, String dbType, String dateField, String valueField,
		int histTimeSteps, int curTimeSteps, String histTable, String curTable,
		String foreTable, TimeValue timeSeries[]) throws Exception
	{
		ModelDateTime sDate;
		int j = 0;
		int k = 0;
		Double doubleValue;
		String dateString;
		int numberPeriods = timeSeries.length;
		int foreTimeSteps = numberPeriods - histTimeSteps - curTimeSteps;

		if (histTimeSteps > 0 && !histTable.equals("NaN"))
		{

			// historic period (less than or equal initial date of RiverWare Model)

			sDate = timeSeries[0].date;
			k = -1;

			// set up array of historic period data

			TimeValue tsData[] = new TimeValue[histTimeSteps];

			for (j = 0; j < histTimeSteps; j++)
			{
				tsData[j] = new TimeValue(sDate.advanceNTimesteps(timeStep, j), Double.valueOf("-9999"));
			}

			// read values

			if (TabColTimeValues.getTabColData(logBuffer, timeStep, ourConn, dbType, histTable, dateField, valueField, tsData))
			{
				for (j = 0; j < histTimeSteps; j++)
				{
					k++;
					timeSeries[k].value = tsData[j].value;
				}
			}
			else
			{
				System.out.println("Unable to read historic period data for " + histTable + " - " + valueField + ".");
				logBuffer.println("Unable to read historic period data for " + histTable + " - " + valueField + ".");
				return false;
			}
		}

		if (curTimeSteps > 0 && !curTable.equals("NaN"))
		{

			// current period (from date of RiverWare Model to start of forecast period)

			sDate = timeSeries[histTimeSteps].date;
			k = histTimeSteps - 1;

			// set up array of current period data

			TimeValue tsData[] = new TimeValue[curTimeSteps];

			for (j = 0; j < curTimeSteps; j++)
			{
				tsData[j] = new TimeValue(sDate.advanceNTimesteps(timeStep, j), Double.valueOf("-9999"));
			}

			// read values

			if (TabColTimeValues.getTabColData(logBuffer, timeStep, ourConn, dbType, curTable, dateField, valueField, tsData))
			{
				for (j = 0; j < curTimeSteps; j++)
				{
					k++;
					timeSeries[k].value = tsData[j].value;
				}
			}
			else
			{
				System.out.println("Unable to read current period data for " + curTable + " - " + valueField + ".");
				logBuffer.println("Unable to read current period data for " + curTable + " - " + valueField + ".");
				return false;
			}
		}

		// forecast period

		if (foreTimeSteps > 0 && !foreTable.equalsIgnoreCase("NaN"))
		{
			sDate = timeSeries[histTimeSteps + curTimeSteps].date;
			k = histTimeSteps + curTimeSteps - 1;

			// set up array of forecast period data

			TimeValue tsData[] = new TimeValue[foreTimeSteps];

			for (j = 0; j < foreTimeSteps; j++)
			{
				tsData[j] = new TimeValue(sDate.advanceNTimesteps(timeStep, j), Double.valueOf("-9999"));
			}

			// read values

			if (TabColTimeValues.getTabColData(logBuffer, timeStep, ourConn, dbType, foreTable, dateField, valueField, tsData))
			{
				for (j = 0; j < foreTimeSteps; j++)
				{
					k++;
					timeSeries[k].value = tsData[j].value;
				}
			}
			else
			{
				System.out.println("Unable to read forecast period data for " + foreTable + " - " + valueField + ".");
				logBuffer.println("Unable to read forecast period data for " + foreTable + " - " + valueField + ".");
				return false;
			}
		}
		return true;

	}	// end method

}  // end class
