//package java_lib.hdbLib;
// Provided by Dave King, Reclamation
package rti.tscommandprocessor.commands.reclamationhdb.java_lib.hdbLib;

import java.io.PrintWriter;
import java.sql.BatchUpdateException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

import rti.tscommandprocessor.commands.reclamationhdb.java_lib.dmiLib.ModelDateTime;
import rti.tscommandprocessor.commands.reclamationhdb.java_lib.dmiLib.SetModelDateTimeException;
import rti.tscommandprocessor.commands.reclamationhdb.java_lib.dmiLib.TimeValue;

// class HdbTimeValues// utilities to retrieve and populate HDB time series data

// import classes stored in libraries


/**
* A class to retrieve and populate HDB time series data.
*/

public class HdbTimeValues
{
	// No constructor needed because we won't be creating an actual object

	/**
	* Method to get HDB data and place into a TimeValue array
	* @param logBuffer Log file object.
	* @param ourConn JDBC connection.
	* @param dbType Database type
	* @param site_datatype_id site datatype id.
	* @param model_run_id model run id.
	* @param timeSeries[] Time series values array.
	* @exception Exception
	* @return True if successfully completed.
	*/

	public static boolean getHdbData(PrintWriter logBuffer, String timeStep,
			String tableType, Connection ourConn, String dbType, int site_datatype_id,
			int model_run_id, TimeValue timeSeries[])
	{
		try
		{
			// determine table type

			if (tableType.equalsIgnoreCase("Real"))
			{
				// real data tables

				if (timeStep.equalsIgnoreCase("1YEAR"))
				{
					return HdbTimeValues.readHdbData(logBuffer, ourConn, dbType, "r_year", timeStep,
						tableType, site_datatype_id, 0, timeSeries);
				}
				else if (timeStep.equalsIgnoreCase("1MONTH"))
				{
					return HdbTimeValues.readHdbData(logBuffer, ourConn, dbType, "r_month", timeStep,
						tableType, site_datatype_id, 0, timeSeries);
				}
				else if (timeStep.equalsIgnoreCase("1DAY"))
				{
					return HdbTimeValues.readHdbData(logBuffer, ourConn, dbType, "r_day", timeStep,
						tableType, site_datatype_id, 0, timeSeries);
				}
				else if (timeStep.equalsIgnoreCase("1HOUR"))
				{
					return HdbTimeValues.readHdbData(logBuffer, ourConn, dbType, "r_hour", timeStep,
						tableType, site_datatype_id, 0, timeSeries);
				}
				else if (timeStep.equalsIgnoreCase("INSTANT"))
				{
					return HdbTimeValues.readRInstant(logBuffer, ourConn, dbType, site_datatype_id, timeSeries);
				}
				else
				{
					System.out.println ("Timestep " + timeStep + " is invalid.");
					logBuffer.println ("Timestep " + timeStep + " is invalid.");
					return false;
				}
			}
			else
			{
				// model data tables

				if (timeStep.equalsIgnoreCase("1YEAR"))
				{
					return HdbTimeValues.readHdbData(logBuffer, ourConn, dbType, "m_year", timeStep,
						tableType, site_datatype_id, model_run_id, timeSeries);
				}
				else if (timeStep.equalsIgnoreCase("1MONTH"))
				{
					return HdbTimeValues.readHdbData(logBuffer, ourConn, dbType, "m_month", timeStep,
						tableType, site_datatype_id, model_run_id, timeSeries);
				}
				else if (timeStep.equalsIgnoreCase("1DAY"))
				{
					return HdbTimeValues.readHdbData(logBuffer, ourConn, dbType, "m_day", timeStep,
						tableType, site_datatype_id, model_run_id, timeSeries);
				}
				else if (timeStep.equalsIgnoreCase("1HOUR"))
				{
					return HdbTimeValues.readHdbData(logBuffer, ourConn, dbType, "m_hour", timeStep,
						tableType, site_datatype_id, model_run_id, timeSeries);
				}
				else
				{
					System.out.println ("Timestep " + timeStep + " is invalid.");
					logBuffer.println ("Timestep " + timeStep + " is invalid.");
					return false;
				}
			}
		}
		catch (Exception e)
		{
			System.out.println ("Error executing getHdbData");
			logBuffer.println ("Error executing getHdbData");
			System.out.println ("Message: " + e.getMessage ());
			e.printStackTrace();
			return false;
		}

	}	// end method

	/**
	* Method to put HDB data using a TimeValue array.
	* @param logBuffer Log file object.
	* @param ourConn JDBC connection.
	* @param dbType Database type
	* @param site_datatype_id Site datatype id.
	* @param model_run_id Model run id.
	* @param coll_sys_id collection system id.
	* @param loading_app_id loading application id.
	* @param agen_id agency id.
	* @param compute_id computation id.
	* @param method_id method id.
	* @param overwriteFlag overwrite flag
	* @param timeSeries[] Time series values array.
	* @exception Exception
	* @return True if successfully completed.
	*/

	public static boolean putHdbData(PrintWriter logBuffer, String timeStep,
		String tableType, Connection ourConn, String dbType, int site_datatype_id,
		int model_run_id, int coll_sys_id, int loading_app_id, int agen_id,
		int compute_id, int method_id, boolean overwriteFlag, TimeValue timeSeries[])
	{
		// verify that sdi is ok

		if (site_datatype_id <= 1)
		{
			System.out.println ("Site datatype id " + site_datatype_id + " is invalid.");
			logBuffer.println ("Site datatype id " + site_datatype_id + " is invalid.");
			return false;
		}

		try
		{
			// determine table type

			if (tableType.equalsIgnoreCase("Real"))
			{
				// real data tables

				if (dbType.equalsIgnoreCase("OracleHDB"))
				{
					// Oracle database uses stored procedure (Remote Procedure Call)

					if (timeStep.equalsIgnoreCase("1YEAR"))
					{
						return HdbTimeValues.writeRealRPC(ourConn, "r_year",
								timeStep, tableType, "year", site_datatype_id,
								coll_sys_id, loading_app_id, agen_id, compute_id,
								method_id, overwriteFlag, timeSeries, logBuffer);
					}
					else if (timeStep.equalsIgnoreCase("1MONTH"))
					{
						return HdbTimeValues.writeRealRPC(ourConn, "r_month",
								timeStep, tableType, "month", site_datatype_id,
								coll_sys_id, loading_app_id, agen_id, compute_id,
								method_id, overwriteFlag, timeSeries, logBuffer);
					}
					else if (timeStep.equalsIgnoreCase("1DAY"))
					{
						return HdbTimeValues.writeRealRPC(ourConn, "r_day",
								timeStep, tableType, "day", site_datatype_id,
								coll_sys_id, loading_app_id, agen_id, compute_id,
								method_id, overwriteFlag, timeSeries, logBuffer);
					}
					else if (timeStep.equalsIgnoreCase("1HOUR"))
					{
						return HdbTimeValues.writeRealRPC(ourConn, "r_hour",
								timeStep, tableType, "hour", site_datatype_id,
								coll_sys_id, loading_app_id, agen_id, compute_id,
								method_id, overwriteFlag, timeSeries, logBuffer);
					}
					else if (timeStep.equalsIgnoreCase("INSTANT"))
					{
						return HdbTimeValues.writeRealRPC(ourConn, "r_instant",
								timeStep, tableType, "instant", site_datatype_id,
								coll_sys_id, loading_app_id, agen_id, compute_id,
								method_id, overwriteFlag, timeSeries, logBuffer);
					}
					else
					{
						System.out.println ("Timestep " + timeStep + " is invalid.");
						logBuffer.println ("Timestep " + timeStep + " is invalid.");
						return false;
					}
				}
				else
				{

					// non-Oracle database uses java and jdbc

					if (timeStep.equalsIgnoreCase("1YEAR"))
					{
						return HdbTimeValues.writeRealData(ourConn, dbType, "r_year",
								timeStep, tableType, "year", site_datatype_id,
								coll_sys_id, loading_app_id, agen_id, compute_id,
								method_id, overwriteFlag, timeSeries, logBuffer);
					}
					else if (timeStep.equalsIgnoreCase("1MONTH"))
					{
						return HdbTimeValues.writeRealData(ourConn, dbType, "r_month",
								timeStep, tableType, "month", site_datatype_id,
								coll_sys_id, loading_app_id, agen_id, compute_id,
								method_id, overwriteFlag, timeSeries, logBuffer);
					}
					else if (timeStep.equalsIgnoreCase("1DAY"))
					{
						return HdbTimeValues.writeRealData(ourConn, dbType, "r_day",
								timeStep, tableType, "day", site_datatype_id,
								coll_sys_id, loading_app_id, agen_id, compute_id,
								method_id, overwriteFlag, timeSeries, logBuffer);
					}
					else if (timeStep.equalsIgnoreCase("1HOUR"))
					{
						return HdbTimeValues.writeRealData(ourConn, dbType, "r_hour",
								timeStep, tableType, "hour", site_datatype_id,
								coll_sys_id, loading_app_id, agen_id, compute_id,
								method_id, overwriteFlag, timeSeries, logBuffer);
					}
					else if (timeStep.equalsIgnoreCase("INSTANT"))
					{
						return HdbTimeValues.writeRealData(ourConn, dbType, "r_instant",
								timeStep, tableType, "instant", site_datatype_id,
								coll_sys_id, loading_app_id, agen_id, compute_id,
								method_id, overwriteFlag, timeSeries, logBuffer);
					}
					else
					{
						System.out.println ("Timestep " + timeStep + " is invalid.");
						logBuffer.println ("Timestep " + timeStep + " is invalid.");
						return false;
					}
				}
			}
			else
			{
				// model data tables

				if (dbType.equalsIgnoreCase("OracleHDB"))
				{

					// Oracle database uses stored procedure (Remote Procedure Call)

					if (timeStep.equalsIgnoreCase("1YEAR"))
					{
						return HdbTimeValues.writeModelRPC(ourConn, timeStep,
								tableType, "year", site_datatype_id, model_run_id,
								timeSeries, logBuffer);
					}
					else if (timeStep.equalsIgnoreCase("1MONTH"))
					{
						return HdbTimeValues.writeModelRPC(ourConn, timeStep,
								tableType, "month", site_datatype_id, model_run_id,
								timeSeries, logBuffer);
					}
					else if (timeStep.equalsIgnoreCase("1DAY"))
					{
						return HdbTimeValues.writeModelRPC(ourConn, timeStep,
								tableType, "day", site_datatype_id, model_run_id,
								timeSeries, logBuffer);
					}
					else if (timeStep.equalsIgnoreCase("1HOUR"))
					{
						return HdbTimeValues.writeModelRPC(ourConn, timeStep,
								tableType, "hour", site_datatype_id, model_run_id,
								timeSeries, logBuffer);
					}
					else
					{
						System.out.println ("Timestep " + timeStep + " is invalid.");
						logBuffer.println ("Timestep " + timeStep + " is invalid.");
						return false;
					}
				}
				else
				{
					// non-Oracle database uses java and jdbc

					if (timeStep.equalsIgnoreCase("1YEAR"))
					{
						return HdbTimeValues.writeModelData(ourConn, dbType, "m_year",
								timeStep, tableType, site_datatype_id, model_run_id,
								timeSeries, logBuffer);
					}
					else if (timeStep.equalsIgnoreCase("1MONTH"))
					{
						return HdbTimeValues.writeModelData(ourConn, dbType, "m_month",
								timeStep, tableType, site_datatype_id, model_run_id,
								timeSeries, logBuffer);
					}
					else if (timeStep.equalsIgnoreCase("1DAY"))
					{
						return HdbTimeValues.writeModelData(ourConn, dbType, "m_day",
								timeStep, tableType, site_datatype_id, model_run_id,
								timeSeries, logBuffer);
					}
					else if (timeStep.equalsIgnoreCase("1HOUR"))
					{
						return HdbTimeValues.writeModelData(ourConn, dbType, "m_hour",
								timeStep, tableType, site_datatype_id, model_run_id,
								timeSeries, logBuffer);
					}
					else
					{
						System.out.println ("Timestep " + timeStep + " is invalid.");
						logBuffer.println ("Timestep " + timeStep + " is invalid.");
						return false;
					}
				}
			}
		}
		catch (Exception e)
		{
			System.out.println ("Error executing putHdbData");
			logBuffer.println ("Error executing putHdbData");
			System.out.println ("Message: " + e.getMessage ());
			e.printStackTrace();
			return false;
		}

	}	// end method

	/**
	* Method to read an HDB time series table and place into a TimeValue array.
	* @param ourConn JDBC connection.
	* @param dbType Database type.
	* @param tableName Table name.
	* @param timeStep time step of data.
	* @param tableType Table type.
	* @param site_datatype_id Site datatype id.
	* @param model_run_id Model run id.
	* @param timeSeries[] Time series values array.
	* @exception Exception
	* @return True if successfully completed.
	*/

	public static boolean readHdbData(PrintWriter logBuffer, Connection ourConn, String dbType,
		String tableName, String timeStep, String tableType,
		int site_datatype_id, int model_run_id, TimeValue timeSeries[]) throws Exception
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

		if (dbType.equalsIgnoreCase("OracleHDB"))
		{
			sqlCommand  = "select start_date_time, value from " + tableName
						+ " where start_date_time >= TO_DATE('"
						+ timeSeries[0].date.getOracleDateTime()
						+ "','YYYY-MM-DD-HH24-MI-SS')"
						+ " and end_date_time <= TO_DATE('"
						+ timeSeries[numberPeriods - 1].date.advanceNTimesteps(timeStep, 1).getOracleDateTime()
						+ "','YYYY-MM-DD-HH24-MI-SS')"
						+ " and site_datatype_id = " + site_datatype_id;
		}
		else
		{
			sqlCommand  = "select start_date_time, value from " + tableName
						+ " where start_date_time >= "
						+ "'" + timeSeries[0].date.getSQLDateTime()
						+ "' and end_date_time <= '"
						+ timeSeries[numberPeriods - 1].date.advanceNTimesteps(timeStep, 1).getSQLDateTime()
						+ "' and site_datatype_id = " + site_datatype_id;
		}

		if (tableType.equalsIgnoreCase("Model"))
			sqlCommand	= sqlCommand + " and model_run_id = " + model_run_id;
		sqlCommand	= sqlCommand + " order by start_date_time";

		// System.out.println("query string is " + sqlCommand);

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
logBuffer.print("The point, date, and value are: " + thePoint + " - ");
logBuffer.print(dateString + " - " + theDate.getSQLDateTime() + " - ");
logBuffer.println(doubleValue);
*/

				if (thePoint >= 0 && thePoint < numberPeriods) timeSeries[thePoint].value = doubleValue;
			}
			ranOk = true;
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
	* Method to read HDB r_base table and place into a TimeValue array.
	* @param ourConn JDBC connection.
	* @param dbType Database type.
	* @param tableName Table name.
	* @param timeStep time step of data.
	* @param tableType Table type.
	* @param site_datatype_id Site datatype id.
	* @param coll_sys_id collection system id.
	* @param loading_app_id loading application id.
	* @param agen_id agency id.
	* @param compute_id computation id.
	* @param method_id method id.
	* @param timeSeries[] Time series values array.
	* @exception Exception
	* @return True if successfully completed.
	*/

	public static boolean readRBaseData(PrintWriter logBuffer, Connection ourConn, String dbType,
		String tableName, String timeStep, int site_datatype_id,
		int coll_sys_id, int loading_app_id, int agen_id, int compute_id,
		int method_id, String interval, TimeValue timeSeries[]) throws Exception
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

		sqlCommand  = "select start_date_time, value from r_base where"
					+ " site_datatype_id = " + site_datatype_id
					+ " and interval = '" + interval + "'";
		if (dbType.equalsIgnoreCase("OracleHDB"))
		{
			sqlCommand	= sqlCommand
						+ " and start_date_time >= TO_DATE('"
						+ timeSeries[0].date.getOracleDateTime()
						+ "','YYYY-MM-DD-HH24-MI-SS')"
						+ " and end_date_time <= TO_DATE('"
						+ timeSeries[numberPeriods - 1].date.advanceNTimesteps(timeStep, 1).getOracleDateTime()
						+ "','YYYY-MM-DD-HH24-MI-SS')";
		}
		else
		{
			sqlCommand	= sqlCommand
						+ " and start_date_time >= "
						+ "'" + timeSeries[0].date.getSQLDateTime()
						+ "' and end_date_time <= '"
						+ timeSeries[numberPeriods - 1].date.advanceNTimesteps(timeStep, 1).getSQLDateTime()
						+ "'";
		}
		sqlCommand	= sqlCommand
				+ " and agen_id = " + agen_id
				+ " and collection_system_id = " + coll_sys_id
				+ " and loading_application_id = " + loading_app_id
				+ " and method_id = " + method_id
				+ " and computation_id = " + compute_id
				+ " order by start_date_time";

		// System.out.println("query string is " + sqlCommand);

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
logBuffer.print("The point, date, and value are: " + thePoint + " - ");
logBuffer.print(dateString + " - " + theDate.getSQLDateTime() + " - ");
logBuffer.println(doubleValue);
*/

				if (thePoint >= 0 && thePoint < numberPeriods) timeSeries[thePoint].value = doubleValue;
			}
			ranOk = true;
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
	* Method to write data to HDB model tables using data in a TimeValue array.
	* @param ourConn JDBC connection.
	* @param dbType Database type.
	* @param tableName Table name.
	* @param timeStep
	* @param tableType Table type.
	* @param site_datatype_id Site datatype id.
	* @param model_run_id model run id.
	* @param timeSeries[] Time series values array.
	* @exception Exception
	* @return True if successfully completed.
	*/

	public static boolean writeModelData(Connection ourConn, String dbType,
		String tableName, String timeStep, String tableType, int site_datatype_id,
		int model_run_id, TimeValue timeSeries[], PrintWriter logBuffer) throws Exception
	{
		int i;
		String updateStatement, insertStatement;
		PreparedStatement ps = null;
		boolean action;

		insertStatement = "insert into " + tableName + " values(?,?,?,?,?)";
		updateStatement = "update " + tableName + " set value = ? where site_datatype_id = ?";
		updateStatement = updateStatement + " and model_run_id = ?";
		updateStatement	= updateStatement + " and start_date_time = ?";

		// retrieve an array of existing data

		TimeValue existingData[] = new TimeValue[timeSeries.length];

		for (i = 0; i < timeSeries.length; i++)
		{
			existingData[i] = new TimeValue(timeSeries[i].date, Double.valueOf("-9999"));
		}

		getHdbData(logBuffer, timeStep, tableType, ourConn, dbType, site_datatype_id,
					model_run_id, existingData);

		// loop thru all periods and process by inserts and updates

		for (i = 0; i < timeSeries.length; i++)
		{
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
						ps.setInt(2, site_datatype_id);
						ps.setInt(3, model_run_id);
						if (timeStep.equalsIgnoreCase("1HOUR"))
						{
							ps.setTimestamp(4, timeSeries[i].date.getJDBCDateTime());
						}
						else
						{
							ps.setDate(4, timeSeries[i].date.getJDBCDate());
						}
						action = true;
					}
				}
				else
				{
					// record does not exists - do an insert

					// fill in prepared statement for sql call

					ps = ourConn.prepareStatement(insertStatement);
					ps.setInt(1, model_run_id);
					ps.setInt(2, site_datatype_id);
					if (timeStep.equalsIgnoreCase("1HOUR"))
					{
						ps.setTimestamp(3, timeSeries[i].date.getJDBCDateTime());
						ps.setTimestamp(4, timeSeries[i].date.advanceNTimesteps(timeStep, 1).getJDBCDateTime());
					}
					else
					{
						ps.setDate(3, timeSeries[i].date.getJDBCDate());
						ps.setDate(4, timeSeries[i].date.advanceNTimesteps(timeStep, 1).getJDBCDate());
					}
					ps.setDouble(5, timeSeries[i].value.doubleValue());
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
	* Method to write data in a TimeValues array to model tables using stored procedure
	* @param ourConn JDBC connection.
	* @param timeStep
	* @param tableType Table type.
	* @param interval HDB interval.
	* @param site_datatype_id Site datatype id.
	* @param model_run_id Model run id.
	* @param timeSeries[] Time series values array.
	* @exception Exception
	* @return True if successfully completed.
	*/

	public static boolean writeModelRPC(Connection ourConn,
		String timeStep, String tableType, String interval,
		int site_datatype_id, int model_run_id, TimeValue timeSeries[],
		PrintWriter logBuffer) throws Exception
	{
		System.out.println("Posting model data to HDB using stored procedure.");

		// Local variables

		int i;
		boolean action;
		CallableStatement cs = null;

		// java stored procedure

		// cs = ourConn.prepareCall("{call modify_m_table_raw (?,?,?,?,?,?,?)}");
		// PL/SQL stored procedure

		cs = ourConn.prepareCall("begin modify_m_table_raw (?,?,?,?,?,?,?); end;");

		// loop thru time series values add to a batch update

		action = false;
		for (i = 0; i < timeSeries.length; i++)
		{
			if (timeSeries[i].value.doubleValue() != -9999.0)
			{
				cs.setInt(1, model_run_id);
				cs.setInt(2, site_datatype_id);
				if (timeStep.equalsIgnoreCase("1HOUR"))
				{
					cs.setTimestamp(3, timeSeries[i].date.getJDBCDateTime());
					cs.setTimestamp(4, timeSeries[i].date.advanceNTimesteps(timeStep, 1).getJDBCDateTime());
				}
				else
				{
					cs.setDate(3, timeSeries[i].date.getJDBCDate());
					cs.setDate(4, timeSeries[i].date.advanceNTimesteps(timeStep, 1).getJDBCDate());
				}
				cs.setDouble(5, timeSeries[i].value.doubleValue());
				cs.setString(6, interval);
				cs.setString(7, "Y");
				action = true;
				cs.addBatch();
			}
		}

		// post the update if action required

		if (action)
		{
			try
			{
				cs.executeBatch();
				cs.close();
			}
			catch (BatchUpdateException e)
			{
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println ("Message: " + e.getMessage ());
				System.err.println ("Vendor: " + e.getErrorCode ());
				int [] updateCounts = e.getUpdateCounts();
				for (i = 0; i < updateCounts.length; i++)
				{
					System.err.println("Item " + i + " update count is " + updateCounts[i]);
				}
				e.printStackTrace();
				return false;
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
		return true;

	}	// end method

	/**
	* Method to write TimeValues array data to real tables via r_base using stored procedure
	* @param ourConn JDBC connection.
	* @param tableName Table name.
	* @param timeStep
	* @param tableType Table type.
	* @param interval HDB interval.
	* @param site_datatype_id Site datatype id.
	* @param coll_sys_id collection system id.
	* @param loading_app_id loading application id.
	* @param agen_id agency id.
	* @param compute_id computation id.
	* @param method_id method id.
	* @param overwriteFlag overwrite flag
	* @param timeSeries[] Time series values array.
	* @exception Exception
	* @return True if successfully completed.
	*/

	public static boolean writeRealRPC(Connection ourConn,
		String tableName, String timeStep, String tableType, String interval,
		int site_datatype_id, int coll_sys_id, int loading_app_id, int agen_id,
		int compute_id, int method_id, boolean overwriteFlag, TimeValue timeSeries[],
		PrintWriter logBuffer) throws Exception
	{
		System.out.println("Posting real data to HDB using stored procedure.");
		int i;
		boolean action;
		CallableStatement cs = null;

		// verify that user did not specify an overwrite flag for non derived data

		boolean owFlag;

		if (timeStep.equalsIgnoreCase("INSTANT"))
		{
			owFlag = false;
		}
		else
		{
			if (method_id == 18 || method_id == 13 || method_id == 6)
			{
				owFlag = false;
			}
			else
			{
				owFlag = overwriteFlag;
			}
		}

		// java stored procedure

		// ourConn.prepareCall("{call modify_r_base_raw ?,?,?,?,?,?,?,?,?,?,?,?,?}");

		// PL/SQL stored procedure

		cs = ourConn.prepareCall("begin modify_r_base_raw (?,?,?,?,?,?,?,?,?,?,?,?,?); end;");

		// loop thru time series values add to a batch update
		// NOTE - although the stored procedure determines inserts or updates, it posts all data if
		// csid, laid, or aid change.  Therefore, we added insert and delete knowledge here to only
		// post changed or new data.

		action = false;
		for (i = 0; i < timeSeries.length; i++)
		{
/*
logBuffer.print(timeSeries[i].date.getSQLDateTime() + " Existing - ");
logBuffer.print(existingData[i].value + " Revised - ");
logBuffer.print(timeSeries[i].value + " Difference - ");
logBuffer.println(Math.abs(existingData[i].value.doubleValue() - timeSeries[i].value.doubleValue()));
*/
			if (timeSeries[i].value.doubleValue() != -9999.0)
			{
				cs.setInt(1, site_datatype_id);
				cs.setString(2, interval);
				if (timeStep.equalsIgnoreCase("1HOUR"))
				{
					cs.setTimestamp(3, timeSeries[i].date.getJDBCDateTime());
					cs.setTimestamp(4, timeSeries[i].date.advanceNTimesteps(timeStep, 1).getJDBCDateTime());
				}
				else
				{
					cs.setDate(3, timeSeries[i].date.getJDBCDate());
					cs.setDate(4, timeSeries[i].date.advanceNTimesteps(timeStep, 1).getJDBCDate());
				}
				cs.setDouble(5, timeSeries[i].value.doubleValue());
				cs.setInt(6, agen_id);
				if (owFlag && !timeStep.equalsIgnoreCase("INSTANT"))
					cs.setString(7, "O");
				else
					cs.setNull(7, java.sql.Types.VARCHAR);
				cs.setNull(8, java.sql.Types.CHAR);
				cs.setInt(9, coll_sys_id);
				cs.setInt(10, loading_app_id);
				cs.setInt(11, method_id);
				cs.setInt(12, compute_id);
				cs.setString(13, "Y");
				action = true;
				cs.addBatch();
			}
		}

		// post the update if action required

		if (action)
		{
			try
			{
				cs.executeBatch();
				cs.close();
			}
			catch (BatchUpdateException e)
			{
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println ("Message: " + e.getMessage ());
				System.err.println ("Vendor: " + e.getErrorCode ());
				int [] updateCounts = e.getUpdateCounts();
				for (i = 0; i < updateCounts.length; i++)
				{
					System.err.println("Item " + i + " update count is " + updateCounts[i]);
				}
				e.printStackTrace();
				return false;
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

		return true;

	}	// end method

	/**
	* Method to write TimeValues array data to real tables via r_base using java and jdbc
	* @param ourConn JDBC connection.
	* @param dbType Database type.
	* @param tableName Table name.
	* @param timeStep
	* @param tableType Table type.
	* @param interval HDB interval.
	* @param site_datatype_id Site datatype id.
	* @param coll_sys_id collection system id.
	* @param loading_app_id loading application id.
	* @param agen_id agency id.
	* @param compute_id computation id.
	* @param method_id method id.
	* @param overwriteFlag overwrite flag.
	* @param timeSeries[] Time series values array.
	* @exception Exception
	* @return True if successfully completed.
	*/

	public static boolean writeRealData(Connection ourConn, String dbType,
		String tableName, String timeStep, String tableType, String interval,
		int site_datatype_id, int coll_sys_id, int loading_app_id, int agen_id,
		int compute_id, int method_id, boolean overwriteFlag, TimeValue timeSeries[],
		PrintWriter logBuffer) throws Exception
	{
		// Note - this method is not yet using overwrite flag

		int i;
		String updateStatement, insertStatement;
		PreparedStatement ps = null;
		boolean action;

		insertStatement = "insert into r_base values(?,?,?,?,?,?,?,?,?,?,?,?,?)";

		updateStatement = "update r_base set value = ? where site_datatype_id = ?"
						+ " and interval = ?"
						+ " and start_date_time = ?"
						+ " and end_date_time = ?"
						+ " and agen_id = ?"
						+ " and collection_system_id = ?"
						+ " and loading_application_id = ?"
						+ " and method_id = ?"
						+ " and computation_id = ?";

		// retrieve an array of existing data

		TimeValue existingData[] = new TimeValue[timeSeries.length];

		for (i = 0; i < timeSeries.length; i++)
		{
			existingData[i] = new TimeValue(timeSeries[i].date, Double.valueOf("-9999"));
		}

		getHdbData(logBuffer, timeStep, tableType, ourConn, dbType, site_datatype_id,
					0, existingData);

		// loop thru all periods and process by inserts and updates

		for (i = 0; i < timeSeries.length; i++)
		{
			action = false;
			if (timeSeries[i].value.doubleValue() != -9999.0)
			{
				if (existingData[i].value.doubleValue() != -9999.0)
				{
					if(Math.abs(existingData[i].value.doubleValue() - timeSeries[i].value.doubleValue())
> 0.0000001)
					{
						// record already exists and changes - do an update

						ps = ourConn.prepareStatement(updateStatement);
						ps.setDouble(1, timeSeries[i].value.doubleValue());
						ps.setInt(2, site_datatype_id);
						ps.setString(3, interval);
						if (timeStep.equalsIgnoreCase("1HOUR"))
						{
							ps.setTimestamp(4, timeSeries[i].date.getJDBCDateTime());
							ps.setTimestamp(5, timeSeries[i].date.advanceNTimesteps(timeStep, 1).getJDBCDateTime());
						}
						else
						{
							ps.setDate(4, timeSeries[i].date.getJDBCDate());
							ps.setDate(5, timeSeries[i].date.advanceNTimesteps(timeStep, 1).getJDBCDate());
						}
						ps.setInt(6, agen_id);
						ps.setInt(7, coll_sys_id);
						ps.setInt(8, loading_app_id);
						ps.setInt(9, method_id);
						ps.setInt(10, compute_id);
						action = true;
					}
				}
				else
				{
					// record does not exists - do an insert

					// fill in prepared statement for sql call

					ps = ourConn.prepareStatement(insertStatement);
					ps.setInt(1, site_datatype_id);
					ps.setString(2, interval);
					ps.setDate(3, timeSeries[i].date.getJDBCDate());
					ps.setDate(4, timeSeries[i].date.advanceNTimesteps(timeStep, 1).getJDBCDate());
					ps.setDouble(5, timeSeries[i].value.doubleValue());
					ps.setInt(6, agen_id);
					ps.setString(7, null);
					ps.setDate(8, null);
					ps.setString(9, null);
					ps.setInt(10, coll_sys_id);
					ps.setInt(11, loading_app_id);
					ps.setInt(12, method_id);
					ps.setInt(13, compute_id);
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
	* Method to read real instantaneous (r_instant) values from HDB and place into a TimeValues array.
	* @param ourConn JDBC connection.
	* @param dbType Database type
	* @param site_datatype_id Site datatype id.
	* @param timeSeries[] Time series values array.
	* @exception Exception
	* @return True if successfully completed.
	*/

	public static boolean readRInstant(PrintWriter logBuffer, Connection ourConn, String dbType,
				int site_datatype_id, TimeValue timeSeries[]) throws Exception
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

		if (dbType.equalsIgnoreCase("OracleHDB"))
		{
			sqlCommand  = "select start_date_time, value from r_instant"
						+ " where start_date_time >= TO_DATE('"
						+ timeSeries[0].date.getOracleDateTime()
						+ "','YYYY-MM-DD-HH24-MI-SS')"
						+ " and end_date_time <= TO_DATE('"
						+ timeSeries[numberPeriods - 1].date.advanceNTimesteps("INSTANT", 1).getOracleDateTime()
						+ "','YYYY-MM-DD-HH24-MI-SS')"
						+ " and site_datatype_id = " + site_datatype_id
						+ " order by start_date_time";
		}
		else
		{
			sqlCommand  = "select start_date_time, value from r_instant"
						+ " where start_date_time >= "
						+ "'" + timeSeries[0].date.getOracleDateTime()
						+ "' and end_date_time <= '"
						+ timeSeries[numberPeriods - 1].date.advanceNTimesteps("INSTANT", 1).getSQLDateTime()
						+ "' and site_datatype_id = " + site_datatype_id
						+ " order by start_date_time";
		}
		// System.out.println("query string is " + sqlCommand);

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
				thePoint = TimeValue.pointOfTVArray(timeSeries, theDate);
				if (thePoint >= 0 && thePoint < numberPeriods) timeSeries[thePoint].value = doubleValue;
			}
			ranOk = true;
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
	* Method to get HDB data and place into a TimeValue vector
	* @param logBuffer Log file object.
	* @param ourConn JDBC connection.
	* @param dbType Database type
	* @param site_datatype_id site datatype id.
	* @param model_run_id model run id.
	* @param tvVec Time Values vector.
	* @exception Exception
	* @return True if successfully completed.
	*/

	public static boolean getHdbData(PrintWriter logBuffer, String timeStep,
			ModelDateTime startDate, ModelDateTime endDate,
			String tableType, Connection ourConn, String dbType, int site_datatype_id,
			int model_run_id, Vector tvVec)
	{
		try
		{
			// determine table type

			if (tableType.equalsIgnoreCase("Real"))
			{
				// real data tables

				if (timeStep.equalsIgnoreCase("1YEAR"))
				{
					return HdbTimeValues.readHdbData(logBuffer, ourConn, dbType, startDate, endDate,
						"r_year", timeStep, tableType, site_datatype_id, 0, tvVec);
				}
				else if (timeStep.equalsIgnoreCase("1MONTH"))
				{
					return HdbTimeValues.readHdbData(logBuffer, ourConn, dbType,  startDate, endDate,
						"r_month", timeStep, tableType, site_datatype_id, 0, tvVec);
				}
				else if (timeStep.equalsIgnoreCase("1DAY"))
				{
					return HdbTimeValues.readHdbData(logBuffer, ourConn, dbType,  startDate, endDate,
						"r_day", timeStep, tableType, site_datatype_id, 0, tvVec);
				}
				else if (timeStep.equalsIgnoreCase("1HOUR"))
				{
					return HdbTimeValues.readHdbData(logBuffer, ourConn, dbType,  startDate, endDate,
						"r_hour", timeStep, tableType, site_datatype_id, 0, tvVec);
				}
				else if (timeStep.equalsIgnoreCase("INSTANT"))
				{
					return HdbTimeValues.readRInstant(logBuffer, ourConn, dbType,  startDate, endDate,
							site_datatype_id, tvVec);
				}
				else
				{
					System.out.println ("Timestep " + timeStep + " is invalid.");
					logBuffer.println ("Timestep " + timeStep + " is invalid.");
					return false;
				}
			}
			else
			{
				// model data tables

				if (timeStep.equalsIgnoreCase("1YEAR"))
				{
					return HdbTimeValues.readHdbData(logBuffer, ourConn, dbType,  startDate, endDate,
						"m_year", timeStep, tableType, site_datatype_id, model_run_id, tvVec);
				}
				else if (timeStep.equalsIgnoreCase("1MONTH"))
				{
					return HdbTimeValues.readHdbData(logBuffer, ourConn, dbType,  startDate, endDate,
						"m_month", timeStep, tableType, site_datatype_id, model_run_id, tvVec);
				}
				else if (timeStep.equalsIgnoreCase("1DAY"))
				{
					return HdbTimeValues.readHdbData(logBuffer, ourConn, dbType,  startDate, endDate,
						"m_day", timeStep, tableType, site_datatype_id, model_run_id, tvVec);
				}
				else if (timeStep.equalsIgnoreCase("1HOUR"))
				{
					return HdbTimeValues.readHdbData(logBuffer, ourConn, dbType,  startDate, endDate,
						"m_hour", timeStep, tableType, site_datatype_id, model_run_id, tvVec);
				}
				else
				{
					System.out.println ("Timestep " + timeStep + " is invalid.");
					logBuffer.println ("Timestep " + timeStep + " is invalid.");
					return false;
				}
			}
		}
		catch (Exception e)
		{
			System.out.println ("Error executing getHdbData");
			logBuffer.println ("Error executing getHdbData");
			System.out.println ("Message: " + e.getMessage ());
			e.printStackTrace();
			return false;
		}

	}	// end method

	/**
	* Method to put HDB data using a TimeValue vector.
	* @param logBuffer Log file object.
	* @param ourConn JDBC connection.
	* @param dbType Database type
	* @param site_datatype_id Site datatype id.
	* @param model_run_id Model run id.
	* @param coll_sys_id collection system id.
	* @param loading_app_id loading application id.
	* @param agen_id agency id.
	* @param compute_id computation id.
	* @param method_id method id.
	* @param overwriteFlag overwrite flag
	* @param tvVec Time Values vector.
	* @exception Exception
	* @return True if successfully completed.
	*/

	public static boolean putHdbData(PrintWriter logBuffer, String timeStep,
		ModelDateTime startDate, ModelDateTime endDate,
		String tableType, Connection ourConn, String dbType, int site_datatype_id,
		int model_run_id, int coll_sys_id, int loading_app_id, int agen_id,
		int compute_id, int method_id, boolean overwriteFlag, Vector tvVec)
	{
		// determine table type

		try
		{
			if (tableType.equalsIgnoreCase("Real"))
			{
				// real data tables

				if (dbType.equalsIgnoreCase("OracleHDB"))
				{
					// Oracle database uses stored procedure (Remote Procedure Call)

					if (timeStep.equalsIgnoreCase("1YEAR"))
					{
						return HdbTimeValues.writeRealRPC(ourConn, startDate, endDate,
							"r_year", timeStep, tableType, "year", site_datatype_id,
							coll_sys_id, loading_app_id, agen_id, compute_id,
							method_id, overwriteFlag, tvVec, logBuffer);
					}
					else if (timeStep.equalsIgnoreCase("1MONTH"))
					{
						return HdbTimeValues.writeRealRPC(ourConn, startDate, endDate,
							"r_month", timeStep, tableType, "month", site_datatype_id,
							coll_sys_id, loading_app_id, agen_id, compute_id,
							method_id, overwriteFlag, tvVec, logBuffer);
					}
					else if (timeStep.equalsIgnoreCase("1DAY"))
					{
						return HdbTimeValues.writeRealRPC(ourConn, startDate, endDate,
							"r_day", timeStep, tableType, "day", site_datatype_id,
							coll_sys_id, loading_app_id, agen_id, compute_id,
							method_id, overwriteFlag, tvVec, logBuffer);
					}
					else if (timeStep.equalsIgnoreCase("1HOUR"))
					{
						return HdbTimeValues.writeRealRPC(ourConn, startDate, endDate,
							"r_hour", timeStep, tableType, "hour", site_datatype_id,
							coll_sys_id, loading_app_id, agen_id, compute_id,
							method_id, overwriteFlag, tvVec, logBuffer);
					}
					else if (timeStep.equalsIgnoreCase("INSTANT"))
					{
						return HdbTimeValues.writeRealRPC(ourConn, startDate, endDate,
							"r_instant", timeStep, tableType, "instant", site_datatype_id,
							coll_sys_id, loading_app_id, agen_id, compute_id,
							method_id, overwriteFlag, tvVec, logBuffer);
					}
					else
					{
						System.out.println ("Timestep " + timeStep + " is invalid.");
						logBuffer.println ("Timestep " + timeStep + " is invalid.");
						return false;
					}
				}
				else
				{
					// non-Oracle database uses java and jdbc

					if (timeStep.equalsIgnoreCase("1YEAR"))
					{
						return HdbTimeValues.writeRealData(ourConn, dbType,  startDate, endDate,
							"r_year", timeStep, tableType, "year", site_datatype_id,
							coll_sys_id, loading_app_id, agen_id, compute_id,
							method_id, overwriteFlag, tvVec, logBuffer);
					}
					else if (timeStep.equalsIgnoreCase("1MONTH"))
					{
						return HdbTimeValues.writeRealData(ourConn, dbType,  startDate, endDate,
							"r_month", timeStep, tableType, "month", site_datatype_id,
							coll_sys_id, loading_app_id, agen_id, compute_id,
							method_id, overwriteFlag, tvVec, logBuffer);
					}
					else if (timeStep.equalsIgnoreCase("1DAY"))
					{
						return HdbTimeValues.writeRealData(ourConn, dbType,  startDate, endDate,
							"r_day", timeStep, tableType, "day", site_datatype_id,
							coll_sys_id, loading_app_id, agen_id, compute_id,
							method_id, overwriteFlag, tvVec, logBuffer);
					}
					else if (timeStep.equalsIgnoreCase("1HOUR"))
					{
						return HdbTimeValues.writeRealData(ourConn, dbType,  startDate, endDate,
							"r_hour", timeStep, tableType, "hour", site_datatype_id,
							coll_sys_id, loading_app_id, agen_id, compute_id,
							method_id, overwriteFlag, tvVec, logBuffer);
					}
					else if (timeStep.equalsIgnoreCase("INSTANT"))
					{
						return HdbTimeValues.writeRealData(ourConn, dbType,  startDate, endDate,
							"r_instant", timeStep, tableType, "instant", site_datatype_id,
							coll_sys_id, loading_app_id, agen_id, compute_id,
							method_id, overwriteFlag, tvVec, logBuffer);
					}
					else
					{
						System.out.println ("Timestep " + timeStep + " is invalid.");
						logBuffer.println ("Timestep " + timeStep + " is invalid.");
						return false;
					}
				}
			}
			else
			{
				// model data tables

				if (dbType.equalsIgnoreCase("OracleHDB"))
				{

					// Oracle database uses stored procedure (Remote Procedure Call)

					if (timeStep.equalsIgnoreCase("1YEAR"))
					{
						return HdbTimeValues.writeModelRPC(ourConn, startDate, endDate,
							timeStep, tableType, "year", site_datatype_id, model_run_id,
							tvVec, logBuffer);
					}
					else if (timeStep.equalsIgnoreCase("1MONTH"))
					{
						return HdbTimeValues.writeModelRPC(ourConn, startDate, endDate,
							timeStep, tableType, "month", site_datatype_id, model_run_id,
							tvVec, logBuffer);
					}
					else if (timeStep.equalsIgnoreCase("1DAY"))
					{
						return HdbTimeValues.writeModelRPC(ourConn, startDate, endDate,
							timeStep, tableType, "day", site_datatype_id, model_run_id,
							tvVec, logBuffer);
					}
					else if (timeStep.equalsIgnoreCase("1HOUR"))
					{
						return HdbTimeValues.writeModelRPC(ourConn, startDate, endDate,
							timeStep, tableType, "hour", site_datatype_id, model_run_id,
							tvVec, logBuffer);
					}
					else
					{
						System.out.println ("Timestep " + timeStep + " is invalid.");
						logBuffer.println ("Timestep " + timeStep + " is invalid.");
						return false;
					}
				}
				else
				{
					// non-Oracle database uses java and jdbc

					if (timeStep.equalsIgnoreCase("1YEAR"))
					{
						return HdbTimeValues.writeModelData(ourConn, dbType,  startDate, endDate,
							"m_year", timeStep, tableType, site_datatype_id, model_run_id,
							tvVec, logBuffer);
					}
					else if (timeStep.equalsIgnoreCase("1MONTH"))
					{
						return HdbTimeValues.writeModelData(ourConn, dbType,  startDate, endDate,
							"m_month", timeStep, tableType, site_datatype_id, model_run_id,
							tvVec, logBuffer);
					}
					else if (timeStep.equalsIgnoreCase("1DAY"))
					{
						return HdbTimeValues.writeModelData(ourConn, dbType,  startDate, endDate,
							"m_day", timeStep, tableType, site_datatype_id, model_run_id,
							tvVec, logBuffer);
					}
					else if (timeStep.equalsIgnoreCase("1HOUR"))
					{
						return HdbTimeValues.writeModelData(ourConn, dbType,  startDate, endDate,
							"m_hour", timeStep, tableType, site_datatype_id, model_run_id,
							tvVec, logBuffer);
					}
					else
					{
						System.out.println ("Timestep " + timeStep + " is invalid.");
						logBuffer.println ("Timestep " + timeStep + " is invalid.");
						return false;
					}
				}
			}
		}
		catch (Exception e)
		{
			System.out.println ("Error executing putHdbData");
			logBuffer.println ("Error executing putHdbData");
			System.out.println ("Message: " + e.getMessage ());
			e.printStackTrace();
			return false;
		}

	}	// end method

	/**
	* Method to read an HDB time series table and place into a TimeValue vector.
	* @param ourConn JDBC connection.
	* @param dbType Database type.
	* @param tableName Table name.
	* @param timeStep time step of data.
	* @param tableType Table type.
	* @param site_datatype_id Site datatype id.
	* @param model_run_id Model run id.
	* @param tvVec Time Values vector.
	* @exception Exception
	* @return True if successfully completed.
	*/

	public static boolean readHdbData(PrintWriter logBuffer, Connection ourConn, String dbType,
		ModelDateTime startDate, ModelDateTime endDate,
		String tableName, String timeStep, String tableType,
		int site_datatype_id, int model_run_id, Vector tvVec) throws Exception
	{
		ResultSet rs = null;
		Statement stmt = null;
		boolean ranOk = false;
		String sqlCommand;
		Double doubleValue;
		String dateString;
		TimeValue thisData = null;
		ModelDateTime theDate = null;

		if (dbType.equalsIgnoreCase("OracleHDB"))
		{
			sqlCommand  = "select start_date_time, value from " + tableName
						+ " where start_date_time >= TO_DATE('"
						+ startDate.getOracleDateTime()
						+ "','YYYY-MM-DD-HH24-MI-SS')"
						+ " and end_date_time <= TO_DATE('"
						+ endDate.advanceNTimesteps(timeStep, 1).getOracleDateTime()
						+ "','YYYY-MM-DD-HH24-MI-SS')"
						+ " and site_datatype_id = " + site_datatype_id;
		}
		else
		{
			sqlCommand  = "select start_date_time, value from " + tableName
						+ " where start_date_time >= "
						+ "'" + startDate.getSQLDateTime()
						+ "' and end_date_time <= '"
						+ endDate.advanceNTimesteps(timeStep, 1).getSQLDateTime()
						+ "' and site_datatype_id = " + site_datatype_id;
		}

		if (tableType.equalsIgnoreCase("Model"))
			sqlCommand	= sqlCommand + " and model_run_id = " + model_run_id;
		sqlCommand	= sqlCommand + " order by start_date_time";

		// System.out.println("query string is " + sqlCommand);

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
			ranOk =  true;
		}
		catch (SetModelDateTimeException e)
		{
			System.out.println ("Set model date time exception found executing readRdbData.");
			e.printStackTrace();
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
	* Method to read HDB r_base table and place into a TimeValue vector.
	* @param ourConn JDBC connection.
	* @param dbType Database type.
	* @param tableName Table name.
	* @param timeStep time step of data.
	* @param tableType Table type.
	* @param site_datatype_id Site datatype id.
	* @param coll_sys_id collection system id.
	* @param loading_app_id loading application id.
	* @param agen_id agency id.
	* @param compute_id computation id.
	* @param method_id method id.
	* @param tvVec Time Values vector.
	* @exception Exception
	* @return True if successfully completed.
	*/

	public static boolean readRBaseData(PrintWriter logBuffer, Connection ourConn, String dbType,
		ModelDateTime startDate, ModelDateTime endDate,
		String tableName, String timeStep, int site_datatype_id,
		int coll_sys_id, int loading_app_id, int agen_id, int compute_id,
		int method_id, String interval, Vector tvVec) throws Exception
	{
		ResultSet rs = null;
		Statement stmt = null;
		boolean ranOk = false;
		String sqlCommand;
		Double doubleValue;
		String dateString;
		TimeValue thisData = null;
		ModelDateTime theDate = null;

		sqlCommand  = "select start_date_time, value from r_base where"
					+ " site_datatype_id = " + site_datatype_id
					+ " and interval = '" + interval + "'";
		if (dbType.equalsIgnoreCase("OracleHDB"))
		{
			sqlCommand	= sqlCommand
						+ " and start_date_time >= TO_DATE('"
						+ startDate.getOracleDateTime()
						+ "','YYYY-MM-DD-HH24-MI-SS')"
						+ " and end_date_time <= TO_DATE('"
						+ endDate.advanceNTimesteps(timeStep, 1).getOracleDateTime()
						+ "','YYYY-MM-DD-HH24-MI-SS')";
		}
		else
		{
			sqlCommand	= sqlCommand
						+ " and start_date_time >= "
						+ "'" + startDate.getSQLDateTime()
						+ "' and end_date_time <= '"
						+ endDate.advanceNTimesteps(timeStep, 1).getSQLDateTime()
						+ "'";
		}
		sqlCommand	= sqlCommand
				+ " and agen_id = " + agen_id
				+ " and collection_system_id = " + coll_sys_id
				+ " and loading_application_id = " + loading_app_id
				+ " and method_id = " + method_id
				+ " and computation_id = " + compute_id
				+ " order by start_date_time";
		// System.out.println("query string is " + sqlCommand);

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
		catch (SetModelDateTimeException e)
		{
			System.out.println ("Set model date time exception found executing readRdbData.");
			e.printStackTrace();
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
	* Method to write data to HDB model tables using data in a TimeValue vector.
	* @param ourConn JDBC connection.
	* @param dbType Database type.
	* @param tableName Table name.
	* @param timeStep
	* @param tableType Table type.
	* @param site_datatype_id Site datatype id.
	* @param model_run_id model run id.
	* @param tvVec Time Values vector.
	* @exception Exception
	* @return True if successfully completed.
	*/

	public static boolean writeModelData(Connection ourConn, String dbType,
		ModelDateTime startDate, ModelDateTime endDate,
		String tableName, String timeStep, String tableType, int site_datatype_id,
		int model_run_id, Vector tvVec, PrintWriter logBuffer) throws Exception
	{
		int i;
		int thePoint = 0;
		String updateStatement, insertStatement;
		PreparedStatement ps = null;
		boolean action;
		TimeValue thisData = null;
		TimeValue thatData = null;

		insertStatement = "insert into " + tableName + " values(?,?,?,?,?)";
		updateStatement = "update " + tableName + " set value = ? where site_datatype_id = ?";
		updateStatement = updateStatement + " and model_run_id = ?";
		updateStatement	= updateStatement + " and start_date_time = ?";

		// retrieve an vector of existing data

		Vector existingData = new Vector (900,300);

		getHdbData(logBuffer, timeStep, startDate, endDate, tableType, ourConn, dbType,
				site_datatype_id, model_run_id, existingData);

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
						ps.setInt(2, site_datatype_id);
						ps.setInt(3, model_run_id);
						if (timeStep.equalsIgnoreCase("1HOUR"))
						{
							ps.setTimestamp(4, thisData.date.getJDBCDateTime());
						}
						else
						{
							ps.setDate(4, thisData.date.getJDBCDate());
						}
						action = true;
					}
				}
				else
				{
					// record does not exists - do an insert

					// fill in prepared statement for sql call

					ps = ourConn.prepareStatement(insertStatement);
					ps.setInt(1, model_run_id);
					ps.setInt(2, site_datatype_id);
					if (timeStep.equalsIgnoreCase("1HOUR"))
					{
						ps.setTimestamp(3, thisData.date.getJDBCDateTime());
						ps.setTimestamp(4, thisData.date.advanceNTimesteps(timeStep, 1).getJDBCDateTime());
					}
					else
					{
						ps.setDate(3, thisData.date.getJDBCDate());
						ps.setDate(4, thisData.date.advanceNTimesteps(timeStep, 1).getJDBCDate());
					}
					ps.setDouble(5, thisData.value.doubleValue());
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
	* Method to write data in a TimeValues vector to model tables using stored procedure
	* @param ourConn JDBC connection.
	* @param timeStep
	* @param tableType Table type.
	* @param interval HDB interval.
	* @param site_datatype_id Site datatype id.
	* @param model_run_id Model run id.
	* @param tvVec Time Values vector.
	* @exception Exception
	* @return True if successfully completed.
	*/

	public static boolean writeModelRPC(Connection ourConn,
		ModelDateTime startDate, ModelDateTime endDate,
		String timeStep, String tableType, String interval,
		int site_datatype_id, int model_run_id, Vector tvVec,
		PrintWriter logBuffer) throws Exception
	{
		System.out.println("Posting model data to HDB using stored procedure.");
		int i;
		boolean action;
		CallableStatement cs = null;
		TimeValue thisData = null;

		// java stored procedure

		// cs = ourConn.prepareCall("{call modify_m_table_raw (?,?,?,?,?,?,?)}");
		// PL/SQL stored procedure

		cs = ourConn.prepareCall("begin modify_m_table_raw (?,?,?,?,?,?,?); end;");

		// loop thru time series values add to a batch update

		action = false;
		for (i = 0; i < tvVec.size(); i++)
		{
			thisData = (TimeValue) tvVec.elementAt(i);
			if (thisData.value.doubleValue() != -9999.0)
			{
				cs.setInt(1, model_run_id);
				cs.setInt(2, site_datatype_id);
				if (timeStep.equalsIgnoreCase("1HOUR"))
				{
					cs.setTimestamp(3, thisData.date.getJDBCDateTime());
					cs.setTimestamp(4, thisData.date.advanceNTimesteps(timeStep, 1).getJDBCDateTime());
				}
				else
				{
					cs.setDate(3, thisData.date.getJDBCDate());
					cs.setDate(4, thisData.date.advanceNTimesteps(timeStep, 1).getJDBCDate());
				}
				cs.setDouble(5, thisData.value.doubleValue());
				cs.setString(6, interval);
				cs.setString(7, "Y");
				action = true;
				cs.addBatch();
			}
		}

		// post the update if action required

		if (action)
		{
			try
			{
				cs.executeBatch();
				cs.close();
			}
			catch (BatchUpdateException e)
			{
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println ("Message: " + e.getMessage ());
				System.err.println ("Vendor: " + e.getErrorCode ());
				int [] updateCounts = e.getUpdateCounts();
				for (i = 0; i < updateCounts.length; i++)
				{
					System.err.println("Item " + i + " update count is " + updateCounts[i]);
				}
				e.printStackTrace();
				return false;
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
		return true;

	}	// end method

	/**
	* Method to write TimeValues vector data to real tables via r_base using stored procedure
	* @param ourConn JDBC connection.
	* @param tableName Table name.
	* @param timeStep
	* @param tableType Table type.
	* @param interval HDB interval.
	* @param site_datatype_id Site datatype id.
	* @param coll_sys_id collection system id.
	* @param loading_app_id loading application id.
	* @param agen_id agency id.
	* @param compute_id computation id.
	* @param method_id method id.
	* @param overwriteFlag overwrite flag
	* @param tvVec Time Values vector.
	* @exception Exception
	* @return True if successfully completed.
	*/

	public static boolean writeRealRPC(Connection ourConn,
		ModelDateTime startDate, ModelDateTime endDate,
		String tableName, String timeStep, String tableType, String interval,
		int site_datatype_id, int coll_sys_id, int loading_app_id, int agen_id,
		int compute_id, int method_id, boolean overwriteFlag, Vector tvVec,
		PrintWriter logBuffer) throws Exception
	{
		System.out.println("Posting real data to HDB using stored procedure.");
		int i;
		boolean action;
		CallableStatement cs = null;
		TimeValue theData = null;

		// verify that user did not specify an overwrite flag for non derived data

		boolean owFlag;

		if (timeStep.equalsIgnoreCase("INSTANT"))
		{
			owFlag = false;
		}
		else
		{
			if (method_id == 18 || method_id == 13 || method_id == 6)
			{
				owFlag = false;
			}
			else
			{
				owFlag = overwriteFlag;
			}
		}

		// java stored procedure

		// ourConn.prepareCall("{call modify_r_base_raw ?,?,?,?,?,?,?,?,?,?,?,?,?}");

		// PL/SQL stored procedure

		cs = ourConn.prepareCall("begin modify_r_base_raw (?,?,?,?,?,?,?,?,?,?,?,?,?); end;");

		// loop thru time series values add to a batch update

		action = false;
		for (i = 0; i < tvVec.size(); i++)
		{
			theData = (TimeValue) tvVec.elementAt(i);
			if (theData.value.doubleValue() != -9999.0)
			{
				cs.setInt(1, site_datatype_id);
				cs.setString(2, interval);
				if (timeStep.equalsIgnoreCase("1HOUR"))
				{
					cs.setTimestamp(3, theData.date.getJDBCDateTime());
					cs.setTimestamp(4, theData.date.advanceNTimesteps(timeStep, 1).getJDBCDateTime());
				}
				else
				{
					if (timeStep.equalsIgnoreCase("INSTANT"))
					{
						cs.setTimestamp(3, theData.date.getJDBCDateTime());
						cs.setTimestamp(4, theData.date.getJDBCDateTime());
					}
					else
					{
						cs.setDate(3, theData.date.getJDBCDate());
						cs.setDate(4, theData.date.advanceNTimesteps(timeStep, 1).getJDBCDate());
					}
				}
				cs.setDouble(5, theData.value.doubleValue());
				cs.setInt(6, agen_id);
				if (owFlag && !timeStep.equalsIgnoreCase("INSTANT"))
					cs.setString(7, "O");
				else
					cs.setNull(7, java.sql.Types.VARCHAR);
				cs.setNull(8, java.sql.Types.CHAR);
				cs.setInt(9, coll_sys_id);
				cs.setInt(10, loading_app_id);
				cs.setInt(11, method_id);
				cs.setInt(12, compute_id);
				cs.setString(13, "Y");
				action = true;
				cs.addBatch();
			}
		}

		// post the update if action required

		if (action)
		{
			try
			{
				cs.executeBatch();
				cs.close();
			}
			catch (BatchUpdateException e)
			{
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println ("Message: " + e.getMessage ());
				System.err.println ("Vendor: " + e.getErrorCode ());
				int [] updateCounts = e.getUpdateCounts();
				for (i = 0; i < updateCounts.length; i++)
				{
					System.err.println("Item " + i + " update count is " + updateCounts[i]);
				}
				e.printStackTrace();
				return false;
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

		return true;

	}	// end method

	/**
	* Method to write TimeValues vector data to real tables via r_base using java and jdbc
	* @param ourConn JDBC connection.
	* @param dbType Database type.
	* @param tableName Table name.
	* @param timeStep
	* @param tableType Table type.
	* @param interval HDB interval.
	* @param site_datatype_id Site datatype id.
	* @param coll_sys_id collection system id.
	* @param loading_app_id loading application id.
	* @param agen_id agency id.
	* @param compute_id computation id.
	* @param method_id method id.
	* @param overwriteFlag overwrite flag.
	* @param tvVec[] Time Values vector.
	* @exception Exception
	* @return True if successfully completed.
	*/

	public static boolean writeRealData(Connection ourConn, String dbType,
		ModelDateTime startDate, ModelDateTime endDate,
		String tableName, String timeStep, String tableType, String interval,
		int site_datatype_id, int coll_sys_id, int loading_app_id, int agen_id,
		int compute_id, int method_id, boolean overwriteFlag, Vector tvVec,
		PrintWriter logBuffer) throws Exception
	{
		// Note - this method is not yet using overwrite flag

		int i;
		int thePoint = 0;
		String updateStatement, insertStatement;
		PreparedStatement ps = null;
		boolean action;
		TimeValue thisData = null;
		TimeValue thatData = null;

		insertStatement = "insert into r_base values(?,?,?,?,?,?,?,?,?,?,?,?,?)";

		updateStatement = "update r_base set value = ? where site_datatype_id = ?"
						+ " and interval = ?"
						+ " and start_date_time = ?"
						+ " and end_date_time = ?"
						+ " and agen_id = ?"
						+ " and collection_system_id = ?"
						+ " and loading_application_id = ?"
						+ " and method_id = ?"
						+ " and computation_id = ?";

		// retrieve an vector of existing data

		Vector existingData = new Vector (900,300);

		readRBaseData(logBuffer, ourConn, dbType, startDate, endDate, tableType, timeStep,
				site_datatype_id, coll_sys_id, loading_app_id, agen_id, compute_id,
				method_id, interval, existingData);

		// loop thru all periods and process by inserts and updates

		for (i = 0; i < tvVec.size(); i++)
		{
			action = false;
			thisData = (TimeValue) tvVec.elementAt(i);
			if (thisData.value.doubleValue() != -9999.0)
			{
				thePoint = TimeValue.pointOfTVVector(existingData, thisData.date);
				if (thePoint > -1)
				{
					thatData = (TimeValue) existingData.elementAt(thePoint);
					if(Math.abs(thatData.value.doubleValue() - thisData.value.doubleValue())
> 0.0000001)
					{
						// record already exists and changes - do an update

						ps = ourConn.prepareStatement(updateStatement);
						ps.setDouble(1, thisData.value.doubleValue());
						ps.setInt(2, site_datatype_id);
						ps.setString(3, interval);
						if (timeStep.equalsIgnoreCase("1HOUR"))
						{
							ps.setTimestamp(4, thisData.date.getJDBCDateTime());
							ps.setTimestamp(5, thisData.date.advanceNTimesteps(timeStep, 1).getJDBCDateTime());
						}
						if (timeStep.equalsIgnoreCase("INSTANT"))
						{
							ps.setTimestamp(4, thisData.date.getJDBCDateTime());
							ps.setTimestamp(5, thisData.date.getJDBCDateTime());
						}
						else
						{
							ps.setDate(4, thisData.date.getJDBCDate());
							ps.setDate(5, thisData.date.advanceNTimesteps(timeStep, 1).getJDBCDate());
						}
						ps.setInt(6, agen_id);
						ps.setInt(7, coll_sys_id);
						ps.setInt(8, loading_app_id);
						ps.setInt(9, method_id);
						ps.setInt(10, compute_id);
						action = true;
					}
				}
				else
				{
					// record does not exists - do an insert

					// fill in prepared statement for sql call

					ps = ourConn.prepareStatement(insertStatement);
					ps.setInt(1, site_datatype_id);
					ps.setString(2, interval);
					ps.setDate(3, thisData.date.getJDBCDate());
					ps.setDate(4, thisData.date.advanceNTimesteps(timeStep, 1).getJDBCDate());
					ps.setDouble(5, thisData.value.doubleValue());
					ps.setInt(6, agen_id);
					ps.setString(7, null);
					ps.setDate(8, null);
					ps.setString(9, null);
					ps.setInt(10, coll_sys_id);
					ps.setInt(11, loading_app_id);
					ps.setInt(12, method_id);
					ps.setInt(13, compute_id);
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
	* Method to read real instantaneous (r_instant) values from HDB and place into a TimeValues vector.
	* @param ourConn JDBC connection.
	* @param dbType Database type
	* @param site_datatype_id Site datatype id.
	* @param tvVec Time values vector.
	* @exception Exception
	* @return True if successfully completed.
	*/

	public static boolean readRInstant(PrintWriter logBuffer, Connection ourConn, String dbType,
			ModelDateTime startDate, ModelDateTime endDate,
			int site_datatype_id, Vector tvVec) throws Exception
	{
		ResultSet rs = null;
		Statement stmt = null;
		boolean ranOk = false;
		String sqlCommand;
		ModelDateTime theDate;
		Double doubleValue;
		String dateString;
		TimeValue thisData = null;

		if (dbType.equalsIgnoreCase("OracleHDB"))
		{
			sqlCommand  = "select start_date_time, value from r_instant"
						+ " where start_date_time >= TO_DATE('"
						+ startDate.getOracleDateTime()
						+ "','YYYY-MM-DD-HH24-MI-SS')"
						+ " and end_date_time <= TO_DATE('"
						+ endDate.advanceNTimesteps("INSTANT", 1).getOracleDateTime()
						+ "','YYYY-MM-DD-HH24-MI-SS')"
						+ " and site_datatype_id = " + site_datatype_id
						+ " order by start_date_time";
		}
		else
		{
			sqlCommand  = "select start_date_time, value from r_instant"
						+ " where start_date_time >= "
						+ "'" + startDate.getSQLDateTime()
						+ "' and end_date_time <= '"
						+ endDate.advanceNTimesteps("INSTANT", 1).getSQLDateTime()
						+ "' and site_datatype_id = " + site_datatype_id
						+ " order by start_date_time";
		}
//		System.out.println("query string is " + sqlCommand);

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
		catch (SetModelDateTimeException e)
		{
			System.out.println ("Set model date time exception found executing readRdbData.");
			e.printStackTrace();
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
	* Method to put three time periods of data into HDB.
	* @param logBuffer Log file object.
	* @param timeStep time step of data.
	* @param ourConn JDBC connection.
	* @param dbType Database type
	* @param site_datatype_id Site datatype id.
	* @param coll_sys_id collection system id.
	* @param loading_app_id loading application id.
	* @param agen_id agency id.
	* @param overwriteFlag overwrite flag.
	* @param model_run_id Model run id.
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
		Connection ourConn, String dbType, int site_datatype_id, int datatype_id,
		String restOfLine, int model_run_id, int coll_sys_id, int loading_app_id,
		int agen_id, boolean overwriteFlag, int histTimeSteps, int curTimeSteps, String histTable,
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

			if (histTable.equalsIgnoreCase("Real"))
			{
				method_id = HdbDmiUtils.getMethodId(ourConn, restOfLine, timeStep,
							site_datatype_id, sDate, tsData[histTimeSteps - 1].date);
				if (method_id <= 0)
				{
					System.out.println("Unable to put historic data into HDB.");
					logBuffer.println("Unable to put historic data into HDB.");
					return false;
				}
			}

			//	put values into HDB

			if (!HdbTimeValues.putHdbData(logBuffer, timeStep, histTable, ourConn, dbType,
					site_datatype_id, model_run_id, coll_sys_id, loading_app_id,
					agen_id, 1, method_id, overwriteFlag, tsData))
			{
				System.out.println("Unable to populate historic period data for " + site_datatype_id);
				logBuffer.println("Unable to populate historic period data for " + site_datatype_id);
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

			if (curTable.equalsIgnoreCase("Real"))
			{
				// determine method id

				method_id = HdbDmiUtils.getMethodId(ourConn, restOfLine, timeStep,
							site_datatype_id, sDate, tsData[curTimeSteps - 1].date);
				if (method_id <= 0)
				{
					System.out.println("Unable to put current data into HDB.");
					logBuffer.println("Unable to put current data into HDB.");
					return false;
				}
			}

			//	put values into HDB

			if (!HdbTimeValues.putHdbData(logBuffer, timeStep, curTable, ourConn, dbType,
					site_datatype_id, model_run_id, coll_sys_id, loading_app_id,
					agen_id, 1, method_id, overwriteFlag, tsData))
			{
				System.out.println("Unable to populate current period data for " + site_datatype_id);
				logBuffer.println("Unable to populate current period data for " + site_datatype_id);
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

			//	put values into HDB

			if (!HdbTimeValues.putHdbData(logBuffer, timeStep, foreTable, ourConn, dbType,
					site_datatype_id, model_run_id, coll_sys_id, loading_app_id,
					agen_id, 1, method_id, overwriteFlag, tsData))
			{
				System.out.println("Unable to populate forecast period data for " + site_datatype_id);
				logBuffer.println("Unable to populate forecast period data for " + site_datatype_id);
				return false;
			}
		}
		return true;

	}	// end method

	/**
	* Method to get three time periods of data from HDB.
	* @param logBuffer Log file object.
	* @param timeStep time step of data.
	* @param ourConn JDBC connection.
	* @param dbType Database type
	* @param site_datatype_id Site datatype id.
	* @param model_run_id Model run id.
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
		Connection ourConn, String dbType, int site_datatype_id, int model_run_id,
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

			if (getHdbData(logBuffer, timeStep, histTable, ourConn, dbType, site_datatype_id, model_run_id, tsData))
			{
				for (j = 0; j < histTimeSteps; j++)
				{
					k++;
					timeSeries[k].value = tsData[j].value;
				}
			}
			else
			{
				System.out.println("Unable to read historic period data for " + site_datatype_id);
				logBuffer.println("Unable to read historic period data for " + site_datatype_id);
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

			if (getHdbData(logBuffer, timeStep, curTable, ourConn, dbType, site_datatype_id, model_run_id, tsData))
			{
				for (j = 0; j < curTimeSteps; j++)
				{
					k++;
					timeSeries[k].value = tsData[j].value;
				}
			}
			else
			{
				System.out.println("Unable to read current period data for " + site_datatype_id);
				logBuffer.println("Unable to read current period data for " + site_datatype_id);
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

			if (getHdbData(logBuffer, timeStep, foreTable, ourConn, dbType, site_datatype_id, model_run_id, tsData))
			{
				for (j = 0; j < foreTimeSteps; j++)
				{
					k++;
					timeSeries[k].value = tsData[j].value;
				}
			}
			else
			{
				System.out.println("Unable to read forecast period data for " + site_datatype_id);
				logBuffer.println("Unable to read forecast period data for " + site_datatype_id);
				return false;
			}
		}
		return true;

	}	// end method

}  // end class
