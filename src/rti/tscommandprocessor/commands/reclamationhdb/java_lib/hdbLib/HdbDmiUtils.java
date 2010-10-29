//package java_lib.hdbLib;
// Provided by Dave King, Reclamation
package rti.tscommandprocessor.commands.reclamationhdb.java_lib.hdbLib;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.StringTokenizer;

import rti.tscommandprocessor.commands.reclamationhdb.java_lib.dmiLib.ControlFileUtils;
import rti.tscommandprocessor.commands.reclamationhdb.java_lib.dmiLib.ModelDateTime;

// various utilities to support HDB DMI's
// import classes stored in libraries


/**
* Class to support HDB DMI's
*/

public class HdbDmiUtils
{
	// No constructor needed because we won't be creating an actual object

	/**
	* Method to get site data type id for a site id and data type id.
	* @param con JDBC connection.
	* @param site_id Site id.
	* @param datatype_id Datatype id.
	* @exception SQLException
	* @return Site datatype id.
	*/

	public static int getSiteDataType(Connection conn, int site_id,
				int datatype_id)
	{
		Statement stmt = null;
		ResultSet rs = null;
		int sdtid = 0;
		String sqlCommand = null;
		sqlCommand	= "SELECT site_datatype_id FROM hdb_site_datatype"
					+ " where site_id = " + site_id
					+ " and datatype_id = " + datatype_id;
		try
		{
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sqlCommand);
			if (rs.next())
			{
				sdtid = rs.getInt(1);
			}
			else
			{
 				System.out.println("No site datatype id exists for site " + site_id +
					" and datatype " + datatype_id + " in table hdb_site_datatype.");
			}
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing getSiteDataType.");
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				rs.close();
				stmt.close();
			}
			catch (SQLException e)
			{
				System.out.println ("SQL error executing getSiteDataType.");
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				e.printStackTrace();
			}
		}
		return sdtid;
	}	// end method

	/**
	* Method to get site data type id for an object, slot, and model_id from table ref_ext_site_data_map.
	* @param con JDBC connection.
	* @param primary_site_code Object name.
	* @param primary_data_code Slot name.
	* @param ext_data_source_id External data source site and datatype id.
	* @exception SQLException
	* @return Site data type.
	*/

	public static int getSiteDataType(Connection conn, String object_name, String slot_name,
					int ext_data_source_id)
	{
		Statement stmt = null;
		ResultSet rs = null;
		int sdtid = 0;
		String sqlCommand = null;

		sqlCommand	= "SELECT site_datatype_id FROM ref_ext_site_data_map"
					+ " where primary_site_code = '" + object_name
					+ "' and primary_data_code = '" + slot_name
					+ "' and ext_data_source_id = " + ext_data_source_id;
		try
		{
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sqlCommand);
			if (rs.next())
			{
				sdtid = rs.getInt(1);
			}
			else
			{
 				System.out.println("No site datatype id exists for object " + object_name +
					" and slot " + slot_name + " in table ref_ext_site_data_map.");
			}
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing getSiteDataType.");
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				rs.close();
				stmt.close();
			}
			catch (SQLException e)
			{
				System.out.println ("SQL error executing getSiteDataType.");
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				e.printStackTrace();
			}
		}
		return sdtid;
	}	// end method

	/**
	* Method to get maximum existing model run id.
	* @param con JDBC connection.
	* @exception SQLException
	* @return Max model run id.
	*/

	public static int getMaxModelRun(Connection conn)
	{
		Statement stmt = null;
		ResultSet rs = null;
		int max_mrid = 0;
		String buff;
		buff = "select max(model_run_id) from ref_model_run";
		try
		{
			stmt = conn.createStatement();
			rs = stmt.executeQuery(buff);
			if (rs.next())
			{
				max_mrid = rs.getInt(1);
			}
			else
			{
 				System.out.println("No model run id's exists.");
			}
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing getMaxModelRun.");
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			e.printStackTrace();
		}
		finally
		{
				try
			{
				rs.close();
				stmt.close();
			}
			catch (SQLException e)
			{
				System.out.println ("SQL error executing getMaxModelRun.");
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				e.printStackTrace();
			}
		}
		return max_mrid;
	}	// end method

	/**
	* Method to get a model id for a given model name.
	* @param con JDBC connection.
	* @param model_name Model name.
	* @exception SQLException
	* @return Hdb model id.
	*/

	public static int getHdbModelIdGivenName(Connection conn, String model_name)
				 throws SQLException
	{
		int model_id = 0;
		Statement stmt = null;
		ResultSet rs = null;
		String sqlCommand = null;

		sqlCommand = "select model_id from hdb_model where model_name = " +	model_name;
   		try
		{
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sqlCommand);
			if (rs.next())
			{
				model_id = rs.getInt(1);
			}
			else
			{
				System.out.println("Invalid model name");
			}
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing getHdbModelIdGivenName.");
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				rs.close();
				stmt.close();
			}
			catch (SQLException e)
			{
				System.out.println ("SQL error executing getHdbModelIdGivenName.");
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				e.printStackTrace();
			}
		}
		return model_id;
	}  // end method

	/**
	* Method to get a source id for a given source name.
	* @param con JDBC connection.
	* @param source_name Source name.
	* @exception SQLException
	* @return Source id.
	*/

	public static int getHdbSourceIdGivenName(Connection conn, String source_name)
	{
		int source_id = 0;
		Statement stmt = null;
		ResultSet rs = null;
		String sqlCommand = null;

		sqlCommand	= "select source_id from hdb_data_source where source_name = '"
					+ source_name + "'";
   		try
		{
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sqlCommand);
			if (rs.next())
			{
				source_id = rs.getInt(1);
			}
			else
			{
				System.out.println("Invalid source name");
			}
		}
		catch (SQLException e)
		{
			System.out.println ("SQL Error executing getHdbSourceIdGivenName.");
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				rs.close();
				stmt.close();
			}
			catch (SQLException e)
			{
				System.out.println ("SQL error executing getHdbSourceIdGivenName.");
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				e.printStackTrace();
			}
		}
		return source_id;
	}  // end method

	/**
	* Method to get a source id for a given executable name.
	* @param con JDBC connection.
	* @param exe_name Executeable name.
	* @exception SQLException
	* @return Source id.
	*/

	public static int getHdbSourceIdGivenExe(Connection conn, String exe_name)
	{
		int source_id = 0;
		Statement stmt = null;
		ResultSet rs = null;
		String sqlCommand = null;

		sqlCommand	= "select source_id from ref_app_data_source where executable_name = '"
					+ exe_name + "'";
   		try
		{
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sqlCommand);
			if (rs.next())
			{
				source_id = rs.getInt(1);
			}
			else
			{
				System.out.println("Invalid executable name");
			}
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing getHdbSourceIdGivenExe.");
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				rs.close();
				stmt.close();
			}
			catch (SQLException e)
			{
				System.out.println ("SQL error executing getHdbSourceIdGivenExe.");
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				e.printStackTrace();
			}
		}
		return source_id;
	}  // end method

	/**
	* Method to retrieve a source id for a given source name.
	* @param con JDBC connection.
	* @param source_id Source id.
	* @exception SQLException
	* @return Source id.
	*/

	public static String getValidationGivenSourceId(Connection conn, int source_id)
	{
		// for now, just return default value of 'Z'

		String v = "Z";
		return v;

	}  // end method

	/**
	* Method to retrieve unit id for a data type id.
	* @param con JDBC connection.
	* @param datatype_id Datatype id.
	* @exception SQLException
	* @return Unit id.
	*/

	public static int getUnitIdGivenDataTypeId(Connection conn, int datatype_id)
	{
		int unitId = 0;
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			String sqlCommand	= "select unit_id from hdb_datatype where "
								+ "datatype_id = " + datatype_id;
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sqlCommand);
			if (rs.next())
			{
				unitId = rs.getInt(1);
			}
			else
			{
				System.out.println("getUnitIdGivenDataTypeId did not find a valid."
							+ " unit id for datatype_id " + datatype_id);
			}
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing getUnitIdGivenDataTypeId.");
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				rs.close();
				stmt.close();
			}
			catch (SQLException e)
			{
				System.out.println ("SQL error executing getUnitIdGivenDataTypeId.");
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				e.printStackTrace();
			}
		}
		return unitId;
	}  // end method

	/**
	* Method to retrieve unit name given unit id.
	* @param con JDBC connection.
	* @param unit_id Unit id.
	* @exception SQLException
	* @return Unit name.
	*/

	public static String getUnitNameGivenUnitId(Connection conn, int unit_id)
	{
		String unitName = null;
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			String sqlCommand	= "select unit_name from hdb_unit where "
								+ "unit_id = " + unit_id;
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sqlCommand);
			if (rs.next())
			{
				unitName = rs.getString(1);
			}
			else
			{
				System.out.println("getUnitNameGivenUnitId did not find a valid unit name for"
								+ " unit_id " + unit_id);
			}
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing getUnitNameGivenUnitId.");
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				rs.close();
				stmt.close();
			}
			catch (SQLException e)
			{
				System.out.println ("SQL error executing getUnitNameGivenUnitId.");
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				e.printStackTrace();
			}
		}
		return unitName;
	}  // end method

	/**
	* Method to retrieve stored unit name given datatype id.
	* @param con JDBC connection.
	* @param datatype_id Datatype id.
	* @exception SQLException
	* @return Stored unit name.
	*/

	public static String getStoredUnitNameGivenDataTypeId(Connection conn, int datatype_id)
	{
		String unitName = null;
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			String sqlCommand	= "select unit_name from hdb_unit a where unit_id = "
								+ " (select stored_unit_id from hdb_unit b, hdb_datatype c "
								+ "where b.unit_id = c.unit_id and "
								+ "datatype_id = " + datatype_id + ")";
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sqlCommand);
			if (rs.next())
			{
				unitName = rs.getString(1);
			}
			else
			{
				System.out.println("getStoredUnitNameGivenDataTypeId did not find a valid unit name for"
								+ " datatype_id " + datatype_id);
			}
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing getStoredUnitNameGivenDataTypeId.");
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				rs.close();
				stmt.close();
			}
			catch (SQLException e)
			{
				System.out.println ("SQL error executing getStoredUnitNameGivenDataTypeId.");
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				e.printStackTrace();
			}
		}
		return unitName;
	}  // end method

	/**
	* Method to retrieve RiverWare unit name for a unit id.
	* @param con JDBC connection.
	* @param unit_id Unit id.
	* @exception SQLException
	* @return RiverWare unit name.
	*/

	public static String getRiverWareUnits(Connection conn, int unit_id)
	{
		Statement stmt = null;
		ResultSet rs = null;
		String rwUnits = "cfs";
		try
		{
			String sqlCommand	= "select pr_unit_name from hdb_dmi_unit_map "
								+ "where unit_id = " + unit_id;
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sqlCommand);
			if (rs.next())
			{
				rwUnits = rs.getString(1);
			}
			else
			{
				System.out.println("getRiverWareUnits call did not find valid units");
			}
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing getRiverWareUnits.");
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				rs.close();
				stmt.close();
			}
			catch (SQLException e)
			{
				System.out.println ("SQL error executing getRiverWareUnits.");
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				e.printStackTrace();
			}
		}
		return rwUnits;
	}  // end method

	/**
	* Method to retrieve a model run id for a given model id and probability.
	* Assumes that one with largest number is desired one.
	* @param con JDBC connection.
	* @param model_id Model id.
	* @param hydroInd Hydrologic indicator
	* @exception SQLException
	* @return Model run id.
	*/

	public static int getModelRunIdGivenModelIdHydroIndicator(Connection conn,
				int model_id, String hydroInd)
	{
		int model_run_id = 0;
		Statement stmt = null;
		ResultSet rs = null;
		String sqlCommand = null;

		sqlCommand	= "select model_run_id from ref_model_run where model_id = " + model_id
					+ " and hydrologic_indicator = " + hydroInd + " order by model_run_id";
   		try
		{
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sqlCommand);
			while (rs.next())
			{
				model_run_id = rs.getInt(1);
			}
			if (model_run_id <= 0) System.out.println("Invalid model id or probability.");
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing getModelRunIdGivenModelIdHydroIndicator.");
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				rs.close();
				stmt.close();
			}
			catch (SQLException e)
			{
				System.out.println ("SQL error executing getModelRunIdGivenModelIdHydroIndicator.");
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				e.printStackTrace();
			}
		}
		return model_run_id;
	}  // end method

	/**
	* Method to retrieve a model run id for a given model id and model run name.
	* Assumes that one with largest number is desired one.
	* @param con JDBC connection.
	* @param model_id Model id.
	* @param model_run_name Model Run Name.
	* @exception SQLException
	* @return Model run id.
	*/

	public static int getModelRunIdGivenModelIdModelRunName(Connection conn,
				int model_id, String model_run_name)
	{
		int model_run_id = 0;
		Statement stmt = null;
		ResultSet rs = null;
		String sqlCommand = null;

		sqlCommand	= "select model_run_id from ref_model_run where model_id = " + model_id
					+ " and model_run_name = '" + model_run_name + "' order by model_run_id";
   		try
		{
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sqlCommand);
			while (rs.next())
			{
				model_run_id = rs.getInt(1);
			}
			if (model_run_id <= 0) System.out.println("Invalid model id or model run name.");
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing getModelRunIdGivenModelIdModelRunName.");
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				rs.close();
				stmt.close();
			}
			catch (SQLException e)
			{
				System.out.println ("SQL error executing getModelRunIdGivenModelIdModelRunName.");
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				e.printStackTrace();
			}
		}
		return model_run_id;

	}  // end method

	/**
	* Method to retrieve a probability (hydrologic indicator) for a given model run id.
	* @param con JDBC connection.
	* @param model_run_id Model run id.
	* @exception SQLException
	* @return Probability.
	*/

	public static String getHydroIndicatorGivenModelRunId(Connection conn, int model_run_id)
	{
		String hydroInd = null;
		Statement stmt = null;
		ResultSet rs = null;
		String sqlCommand = null;

		sqlCommand = "select probability from ref_model_run where model_run_id = " + model_run_id;
   		try
		{
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sqlCommand);
			if (rs.next())
			{
				hydroInd = rs.getString(1);
			}
			else
			{
				System.out.println("Invalid model run id.");
			}
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing getProbabilityGivenModelRunid.");
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				rs.close();
				stmt.close();
			}
			catch (SQLException e)
			{
				System.out.println ("SQL error executing getProbabilityGivenModelRunId.");
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				e.printStackTrace();
			}
		}
		return hydroInd;
	}  // end method

	/**
	* Method to retrieve a site name for a given site id.
	* @param con JDBC connection.
	* @param site_id Site id.
	* @exception SQLException
	* @return Site name.
	*/

	public static String getSiteNameGivenSiteId(Connection conn, int site_id)
	{
		String site_name = null;
		Statement stmt = null;
		ResultSet rs = null;
   		try
		{
			String sqlString = null;
			sqlString = "select site_name from hdb_site where site_id = " + site_id;
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sqlString);
			if (rs.next())
			{
				site_name = rs.getString(1);
			}
			else
			{
				System.out.println("Invalid site id.");
			}
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing getSiteIdGivenSiteid.");
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				rs.close();
				stmt.close();
			}
			catch (SQLException e)
			{
				System.out.println ("SQL error executing getSiteIdGivenSiteId.");
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				e.printStackTrace();
			}
		}
		return site_name;
	}  // end method

	/**
	* Method to retrieve a site id for a given usgs id.
	* @param con JDBC connection.
	* @param usgs_id Usgs site id.
	* @exception SQLException
	* @return Site Id.
	*/

	public static int getSiteIdGivenUSGSId(Connection conn, int usgs_id)
	{
		int site_id = 0;
		Statement stmt = null;
		ResultSet rs = null;
		String sqlCommand = null;

		sqlCommand = "select site_id from hdb_site where usgs_id = " + usgs_id;
   		try
		{
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sqlCommand);
			if (rs.next())
			{
				site_id = rs.getInt(1);
			}
			else
			{
				System.out.println("Invalid USGS id.");
			}
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing getSiteIdGivenUSGSId.");
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				rs.close();
				stmt.close();
			}
			catch (SQLException e)
			{
				System.out.println ("SQL error executing getSiteIdGivenUSGSId.");
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				e.printStackTrace();
			}
		}
		return site_id;
	}  // end method

	/**
	* Method to get a site id for a given nws id.
	* @param con JDBC connection.
	* @param nws_id NWS id.
	* @exception SQLException
	* @return Site id.
	*/

	public static int getSiteIdGivenNWSId(Connection conn, int nws_id)
	{
		int site_id = 0;
		Statement stmt = null;
		ResultSet rs = null;
		String sqlCommand = null;

		sqlCommand = "select site_id from hdb_site where nws_id = " + nws_id;
   		try
		{
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sqlCommand);
			if (rs.next())
			{
				site_id = rs.getInt(1);
			}
			else
			{
				System.out.println("Invalid NWS id.");
			}
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing getSiteIdGivenNWSId.");
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				rs.close();
				stmt.close();
			}
			catch (SQLException e)
			{
				System.out.println ("SQL error executing getSiteIdGivenNWSId.");
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				e.printStackTrace();
			}
		}
		return site_id;
	}  // end method

	/**
	* Method to get a site id for a given scs id.
	* @param con JDBC connection.
	* @param scs_id SCS id.
	* @exception SQLException
	* @return Site id.
	*/

	public static int getSiteIdGivenSCSId(Connection conn, int scs_id)
	{
		int site_id = 0;
		Statement stmt = null;
		ResultSet rs = null;
		String sqlCommand = null;

		sqlCommand = "select site_id from hdb_site where scs_id = " + scs_id;
   		try
		{
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sqlCommand);
			if (rs.next())
			{
				site_id = rs.getInt(1);
			}
			else
			{
				System.out.println("Invalid SCS id.");
			}
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing getSiteIdGivenSCSId.");
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				rs.close();
				stmt.close();
			}
			catch (SQLException e)
			{
				System.out.println ("SQL error executing getSiteIdGivenSCSId.");
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				e.printStackTrace();
			}
		}
		return site_id;
	}  // end method

	/**
	* Method to get a scs id for a given site id.
	* @param con JDBC connection.
	* @param site_id Site id.
	* @exception SQLException
	* @return SCS Id.
	*/

	public static int getSCSIdGivenSiteId(Connection conn, int site_id)
	{
		int scs_id = 0;
		Statement stmt = null;
		ResultSet rs = null;
		String sqlCommand = null;

		sqlCommand = "select scs_id from hdb_site where site_id = " + site_id;
   		try
		{
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sqlCommand);
			if (rs.next())
			{
				scs_id = rs.getInt(1);
			}
			else
			{
				System.out.println("Invalid Site id.");
			}
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing getSCSIdGivenSiteId.");
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				rs.close();
				stmt.close();
			}
			catch (SQLException e)
			{
				System.out.println ("SQL error executing getSCSIdGivenSiteId.");
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				e.printStackTrace();
			}
		}
		return scs_id;
	}  // end method

	/**
	* Method to get a nws id for a given site id.
	* @param con JDBC connection.
	* @param site_id Site id.
	* @exception SQLException
	* @return NWS id.
	*/

	public static int getNWSIdGivenSiteId(Connection conn, int site_id)
	{
		int nws_id = 0;
		Statement stmt = null;
		ResultSet rs = null;
		String sqlCommand = null;

		sqlCommand = "select nws_id from hdb_site where site_id = " + site_id;
   		try
		{
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sqlCommand);
			if (rs.next())
			{
				nws_id = rs.getInt(1);
			}
			else
			{
				System.out.println("Invalid Site id.");
			}
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing getNWSIdGivenSiteId.");
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				rs.close();
				stmt.close();
			}
			catch (SQLException e)
			{
				System.out.println ("SQL error executing getNWSIdGivenSiteId.");
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				e.printStackTrace();
			}
		}
		return nws_id;
	}  // end method

	/**
	* Method to get a usgs id for a given site id.
	* @param con JDBC connection.
	* @param site_id Site id.
	* @exception SQLException
	* @return USGS id.
	*/

	public static int getUSGSIdGivenSiteId(Connection conn, int site_id)
	{
		int usgs_id = 0;
		Statement stmt = null;
		ResultSet rs = null;
		String sqlCommand = null;

		sqlCommand = "select usgs_id from hdb_site where site_id = " + site_id;
   		try
		{
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sqlCommand);
			if (rs.next())
			{
				usgs_id = rs.getInt(1);
			}
			else
			{
				System.out.println("Invalid Site id.");
			}
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing getUSGSIdGivenSiteId.");
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				rs.close();
				stmt.close();
			}
			catch (SQLException e)
			{
				System.out.println ("SQL error executing getUSGSIdGivenSiteId.");
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				e.printStackTrace();
			}
		}
		return usgs_id;
	}  // end method

	/**
	* Method to get a state id for a given site id.
	* @param con JDBC connection.
	* @param site_id Site id.
	* @exception SQLException
	* @return State id.
	*/

	public static int getStateIdGivenSiteId(Connection conn, int site_id)
	{
		int state_id = 0;
		Statement stmt = null;
		ResultSet rs = null;
		String sqlCommand = null;

		sqlCommand = "select state_id from hdb_site where site_id = " + site_id;
   		try
		{
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sqlCommand);
			if (rs.next())
			{
				state_id = rs.getInt(1);
			}
			else
			{
				System.out.println("Invalid Site id.");
			}
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing getStateIdGivenSiteId.");
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				rs.close();
				stmt.close();
			}
			catch (SQLException e)
			{
				System.out.println ("SQL error executing getStateIdGivenSiteId.");
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				e.printStackTrace();
			}
		}
		return state_id;
	}  // end method

	/**
	* Method to get a basin id for a given site id.
	* @param con JDBC connection.
	* @param site_id Site id.
	* @exception SQLException
	* @return Basin id.
	*/

	public static int getBasinIdGivenSiteId(Connection conn, int site_id)
	{
		int basin_id = 0;
		Statement stmt = null;
		ResultSet rs = null;
		String sqlCommand = null;

		sqlCommand = "select basin_id from hdb_site where site_id = " + site_id;
   		try
		{
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sqlCommand);
			if (rs.next())
			{
				basin_id = rs.getInt(1);
			}
			else
			{
				System.out.println("Invalid Site id.");
			}
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing getBasinIdGivenSiteId.");
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				rs.close();
				stmt.close();
			}
			catch (SQLException e)
			{
				System.out.println ("SQL error executing getBasinIdGivenSiteId.");
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				e.printStackTrace();
			}
		}
		return basin_id;
	}  // end method

	/**
	* Method to get a parent site id given site id.
	* @param con JDBC connection.
	* @param site_id Site id.
	* @exception SQLException
	* @return Parent site id.
	*/

	public static int getParentSiteIdGivenSiteId(Connection conn, int site_id)
	{
		int parent_site_id = 0;
		Statement stmt = null;
		ResultSet rs = null;
		String sqlCommand = null;

		sqlCommand = "select parent_site_id from hdb_site where site_id = " + site_id;
   		try
		{
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sqlCommand);
			if (rs.next())
			{
				parent_site_id = rs.getInt(1);
			}
			else
			{
				System.out.println("Invalid Site id.");
			}
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing getParentIdGivenSiteId.");
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				rs.close();
				stmt.close();
			}
			catch (SQLException e)
			{
				System.out.println ("SQL error executing getParentIdGivenSiteId.");
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				e.printStackTrace();
			}
		}
		return parent_site_id;
	}  // end method

	/**
	* Method to get a parent object type given site id.
	* @param con JDBC connection.
	* @param site_id Site id.
	* @exception SQLException
	* @return Parent object type.
	*/

	public static String getParentObjectTypeGivenSiteId(Connection conn, int site_id)
	{
		String parent_object_type = null;
		Statement stmt = null;
		ResultSet rs = null;
		String sqlCommand = null;

		sqlCommand = "select parent_object_type from hdb_site where site_id = " + site_id;
   		try
		{
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sqlCommand);
			if (rs.next())
			{
				parent_object_type = rs.getString(1);
			}
			else
			{
				System.out.println("Invalid Site id.");
			}
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing getParentIdGivenSiteId.");
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				rs.close();
				stmt.close();
			}
			catch (SQLException e)
			{
				System.out.println ("SQL error executing getParentIdGivenId.");
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				e.printStackTrace();
			}
		}
		return parent_object_type;
	}  // end method

	/**
	* Method to get hydrologic_unit given site id.
	* @param con JDBC connection.
	* @param site_id Site id.
	* @exception SQLException
	* @return Huc.
	*/

	public static String getHucGivenSiteId(Connection conn, int site_id)
	{
		String hydrologic_unit = null;
		Statement stmt = null;
		ResultSet rs = null;
		String sqlCommand = null;

		sqlCommand = "select hydrologic_unit from hdb_site where site_id = " + site_id;
   		try
		{
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sqlCommand);
			if (rs.next())
			{
				hydrologic_unit = rs.getString(1);
			}
			else
			{
				System.out.println("Invalid Site id.");
			}
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing getHucGivenSiteId.");
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				rs.close();
				stmt.close();
			}
			catch (SQLException e)
			{
				System.out.println ("SQL error executing getHucGivenSiteId.");
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				e.printStackTrace();
			}
		}
		return hydrologic_unit;
	}  // end method

	/**
	* Method to get a model id given model run id.
	* @param con JDBC connection.
	* @param model_run_id Model run id.
	* @exception SQLException
	* @return Model id.
	*/

	public static int getModelIdGivenModelRunId(Connection conn, int model_run_id)
	{
		int model_id = 0;
		Statement stmt = null;
		ResultSet rs = null;
		String sqlCommand = null;

		sqlCommand = "select model_id from hdb_site where model_run_id = " + model_run_id;
   		try
		{
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sqlCommand);
			if (rs.next())
			{
				model_id = rs.getInt(1);
			}
			else
			{
				System.out.println("Invalid Site id.");
			}
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing getBasinIdGivenSiteId.");
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				rs.close();
				stmt.close();
			}
			catch (SQLException e)
			{
				System.out.println ("SQL error executing getBasinIdGivenSiteId.");
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				e.printStackTrace();
			}
		}
		return model_id;
	}  // end method

	/**
	* Method to get a model run id given probability (hydroInd) and probability map file.
	* Also verifies that the model run id is valid.
	* @param con JDBC connection.
	* @param hydroInd Hydrologic indicator
	* @param probMapFile Probability mapping file.
	* @exception IOException
	* @return Model run id.
	*/

	public static int getModelRunIdGivenProbMapFile(Connection conn, String hydroInd,
					String probMapFile)
	{
		BufferedReader readBuffer = null;
		int model_run_id = 0;
		int umrid = 0;

		try
		{
			readBuffer = new BufferedReader(new FileReader (probMapFile));
			readBuffer.readLine();
			umrid = getProbModelRunId(readBuffer, hydroInd);
			if(umrid != 0)
			{
				// verify that model run id is legit

				if (VerifyRecords.verifyRefModelRun(conn, umrid))
				{
					model_run_id = umrid;
					try
					{
						readBuffer.close();
						return model_run_id;
					}
					catch (Exception e){};
					return model_run_id;
				}
				else
				{
					try
					{
						readBuffer.close();
						return model_run_id;
					}
					catch (Exception e){};
					return model_run_id;
				}
			}
			else
			{
				try
				{
					readBuffer.close();
					return model_run_id;
				}
				catch (Exception e){};
				return model_run_id;
			}
		}
		catch (Exception e)
		{
			System.out.println("Unable to open probability mapping file " + probMapFile);
			return model_run_id;
		}

	}  // end method

	/**
	* Method to get a model run id given model run name and mapping file
	* Also verifies that the model run id is valid.
	* @param con JDBC connection.
	* @param model_run_name Model Run Name.
	* @param mrnMapFile Model Run Name  mapping file.
	* @exception IOException
	* @return Model run id.
	*/

	public static int getModelRunIdGivenMRNMapFile(Connection conn, String model_run_name,
					String mrnMapFile) throws IOException
	{
		BufferedReader readBuffer = null;
		int model_run_id = 0;
		int umrid = 0;

		try
		{
			readBuffer = new BufferedReader(new FileReader (mrnMapFile));
			readBuffer.readLine();
			umrid = getMRNModelRunId(readBuffer, model_run_name);
			if(umrid != 0)
			{

				// verify that model run id is legit

				if (VerifyRecords.verifyRefModelRun(conn, umrid))
				{
					model_run_id = umrid;
					try
					{
						readBuffer.close();
						return model_run_id;
					}
					catch (Exception e){};
					return model_run_id;
				}
				else
				{
					try
					{
						readBuffer.close();
						return model_run_id;
					}
					catch (Exception e){};
					return model_run_id;
				}
			}
			else
			{
				try
				{
					readBuffer.close();
					return model_run_id;
				}
				catch (Exception e){};
				return model_run_id;
			}
		}
		catch (Exception e)
		{
			System.out.println("Unable to open model run name mapping file " + mrnMapFile);
			return model_run_id;
		}

	}  // end method

	/**
	* Method to find model_run_id in probability mapping file.
	* @param readBuffer Buffered reader object.
	* @param probability Probability.
	* @return model_run_id Model Run Id.
	*/

	private static int getProbModelRunId(BufferedReader readBuffer, String probability)
	{
		StringTokenizer st;
		String line = null;
		int umrid = 0;
		String fileProb = null;

		try
		{
			while((line = readBuffer.readLine()) != null)
			{
				// parse probability

				st = new StringTokenizer(line, "\t");
				fileProb = st.nextToken();

				if (fileProb == probability)
				{

					// parse model run id

					umrid = Integer.valueOf(st.nextToken()).intValue();
					return umrid;
				}
			}	// end while loop thru file
			return umrid;
		}
		catch (Exception e)
		{
			System.out.println("Unable to read probabilities mapping file.");
			return umrid;
		}
	}	// end method

	/**
	* Method to find model_run_id in probability mapping file.
	* @param readBuffer Buffered reader object.
	* @param probability Probability.
	* @return model_run_id Model Run Id.
	*/

	private static int getProbModelRunId(BufferedReader readBuffer, int probability)
	{
		StringTokenizer st;
		String line = null;
		int umrid = 0;
		int fileProb = 0;

		try
		{
			while((line = readBuffer.readLine()) != null)
			{
				// parse probability

				st = new StringTokenizer(line, "\t");
				fileProb = Integer.valueOf(st.nextToken()).intValue();

				if (fileProb == probability)
				{
					// parse model run id

					umrid = Integer.valueOf(st.nextToken()).intValue();
					return umrid;
				}
			}	// end while loop thru file

			return umrid;
		}
		catch (Exception e)
		{
			System.out.println("Unable to read probabilities mapping file.");
			return umrid;
		}
	}	// end method

	/**
	* Method to find model_run_id in model run name mapping file.
	* @param readBuffer Buffered reader object.
	* @param model_run_name Model Run Name.
	* @return model_run_id  Model Run Id.
	*/

	private static int getMRNModelRunId(BufferedReader readBuffer, String model_run_name)
	{
		StringTokenizer st;
		String line = null;
		int umrid = 0;
		String fileMRN = null;

		try
		{
			while((line = readBuffer.readLine()) != null)
			{
				// parse model_run_name

				st = new StringTokenizer(line, "\t");
				fileMRN = st.nextToken();

				if (fileMRN.equalsIgnoreCase(model_run_name))
				{
					// parse model run id

					umrid = Integer.valueOf(st.nextToken()).intValue();
					return umrid;
				}
			}	// end while loop thru file

			return umrid;
		}
		catch (Exception e)
		{
			System.out.println("Unable to read model run names mapping file.");
			return umrid;
		}
	}	// end method

	/**
	* Method to create ref model run table using stored procedure
	* @param ourConn JDBC connection.
	* @param model_id Model id.
	* @param model_run_id Model run id.
	* @param model_run_name Model run name.
	* @param startDate Start date of model data period.
	* @param endDate End date of model data period.
	* @param hydroInd Hydrologic indicator
	* @exception Exception
	* @return True if successfully completed.
	*/

	public static boolean createRefModelRunRPC (Connection conn, int model_id, int model_run_id,
			String model_run_name, ModelDateTime startDate, ModelDateTime endDate,
			String hydroInd) throws Exception
	{
		System.out.println("Updating ref_model_run using stored procedure.");
		ModelDateTime runDate = new ModelDateTime();
		CallableStatement cs = null;

		// java stored procedure

		// cs = conn.prepareCall("{call update_model_run_id (?,?,?,?,?,?,?,?,?,?,?,?)}");
		// PL/SQL stored procedure

		cs = conn.prepareCall("begin update_model_run_id (?,?,?,?,?,?,?,?,?,?,?,?); end;");

		cs.setInt(1, model_run_id);
		cs.setString(2, model_run_name);
		cs.setInt(3, model_id);
		cs.setTimestamp(4, runDate.getJDBCDateTime());
		cs.setInt(5, 0);
		cs.setTimestamp(6, startDate.getJDBCDateTime());
		cs.setTimestamp(7, endDate.getJDBCDateTime());
		cs.setString(8, hydroInd);
		cs.setNull(9, java.sql.Types.VARCHAR);
		cs.setNull(10, java.sql.Types.VARCHAR);
		cs.setNull(11, java.sql.Types.VARCHAR);
		cs.setNull(12, java.sql.Types.VARCHAR);			// has to be 'null' if number of keys (item 5) is 0
		// cs.setString(12, "N");

		// run stored procedure

		try
		{
			cs.execute();
			cs.close();
		}
		catch (SQLException e)
		{
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println ("Message: " + e.getMessage ());
			System.err.println ("Vendor: " + e.getErrorCode ());
			e.printStackTrace();
			return false;
		}
		return true;
	}	// end method

	/**
	* Method to update ref model run table using stored procedure
	* @param ourConn JDBC connection.
	* @param model_run_id Model run id.
	* @param model_run_name Model run name.
	* @param startDate Start date of model data period.
	* @param endDate End date of model data period.
	* @param hydroInd Hydrologic indicator
	* @exception Exception
	* @return True if successfully completed.
	*/

	public static boolean updateRefModelRunRPC (Connection conn, int model_run_id,
			String model_run_name, ModelDateTime startDate, ModelDateTime endDate,
			String hydroInd) throws Exception
	{
		System.out.println("Updating ref_model_run using stored procedure.");
		ModelDateTime runDate = new ModelDateTime();
		CallableStatement cs = null;

		// java stored procedure

		// cs = conn.prepareCall("{call update_model_run_id (?,?,?,?,?,?,?,?,?,?,?,?)}");
		// PL/SQL stored procedure

		cs = conn.prepareCall("begin update_model_run_id (?,?,?,?,?,?,?,?,?,?,?,?); end;");

		cs.setInt(1, model_run_id);
		cs.setString(2, model_run_name);
		cs.setTimestamp(3, runDate.getJDBCDateTime());
		cs.setInt(4, 0);
		cs.setTimestamp(5, startDate.getJDBCDateTime());
		cs.setTimestamp(6, endDate.getJDBCDateTime());
		cs.setString(7, hydroInd);
		cs.setNull(8, java.sql.Types.VARCHAR);
		cs.setNull(9, java.sql.Types.VARCHAR);
		cs.setNull(10, java.sql.Types.VARCHAR);
		cs.setNull(11, java.sql.Types.VARCHAR);			// has to be 'null' if number of keys (item 4) is 0
		// cs.setString(11, "N");
		cs.setString(12, "Y");

		// run stored procedure

		try
		{
			cs.execute();
			cs.close();
		}
		catch (SQLException e)
		{
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println ("Message: " + e.getMessage ());
			System.err.println ("Vendor: " + e.getErrorCode ());
			e.printStackTrace();
			return false;
		}
		return true;
	}	// end method

	/**
	* Method to update ref model run table
	* @param con JDBC connection.
	* @param model_run_id Model run id.
	* @param startDate Starting date.
	* @param endDate Ending date.
	* @exception SQLException
	* @return True if successful.
	*/

	public static boolean updateRefModelRun (Connection conn, int model_run_id,
							ModelDateTime startDate, ModelDateTime endDate)
	{
		ModelDateTime theDate;
		ModelDateTime sDate = startDate;
		ModelDateTime eDate = endDate;
		Statement stmt = null;
		ResultSet rs = null;
		String sqlCommand = null;
		String hydroInd = "0";
		String model_run_name = null;

		sqlCommand	= "select start_date, end_date, model_run_name, hydrologic_indicator from ref_model_run"
					+ " where model_run_id = " + model_run_id;
		try
		{
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sqlCommand);
			if (rs.next())
			{
				if (rs.getString(1) != null)
					theDate = new ModelDateTime(rs.getString(1));
				else
					theDate = new ModelDateTime ();
				if (theDate.getJulian() < sDate.getJulian())sDate = theDate;
				if (rs.getString(2) != null)
					theDate = new ModelDateTime(rs.getString(2));
				else
					theDate = new ModelDateTime ();
				if (theDate.getJulian() > eDate.getJulian())eDate = theDate;
				model_run_name = rs.getString(3);
				hydroInd = rs.getString(4);
				rs.close();
				stmt.close();
			}
			else
			{
				System.out.println("Unable to retrieve dates for model run id "
					+ model_run_id);
				rs.close();
				stmt.close();
				return false;
			}

            // call function that updates ref_model_run using stored procedure

            return HdbDmiUtils.updateRefModelRunRPC(conn, model_run_id, model_run_name, sDate, eDate, hydroInd);
		}
		catch (SQLException e)
		{
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			e.printStackTrace();
			return false;
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
			return false;
		}
	}	// end method

	/**
	* Method to updates ref model run table for newer dates and model run name.
	* @param con JDBC connection.
	* @param model_run_id Model run id.
	* @param model_run_name Model Run Name.
	* @param startDate Starting date.
	* @param endDate Ending date.
	* @exception SQLException
	* @return True if successful.
	*/

	public static boolean updateRefModelRun (Connection conn, int model_run_id,
			String model_run_name, ModelDateTime startDate, ModelDateTime endDate)
	{
		ModelDateTime theDate;
		ModelDateTime sDate = startDate;
		ModelDateTime eDate = endDate;
		Statement stmt = null;
		ResultSet rs = null;
		String hydroInd = "0";
		String sqlCommand = null;

		sqlCommand	= "select start_date, end_date, hydrologic_indicator from ref_model_run"
					+ " where model_run_id = " + model_run_id;

		try
		{
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sqlCommand);
			if (rs.next())
			{
				if (rs.getString(1) != null)
					theDate = new ModelDateTime(rs.getString(1));
				else
					theDate = new ModelDateTime ();
				if (theDate.getJulian() < sDate.getJulian())sDate = theDate;
				if (rs.getString(2) != null)
					theDate = new ModelDateTime(rs.getString(2));
				else
					theDate = new ModelDateTime ();
				if (theDate.getJulian() > eDate.getJulian())eDate = theDate;
				hydroInd = rs.getString(3);
				rs.close();
				stmt.close();
			}
			else
			{
				System.out.println("Unable to retrieve dates for model run id "
					+ model_run_id);
				rs.close();
				stmt.close();
				return false;
			}

            // call function that updates ref_model_run using stored procedure

            return HdbDmiUtils.updateRefModelRunRPC(conn, model_run_id, model_run_name, sDate, eDate, hydroInd);
		}
		catch (SQLException e)
		{
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			e.printStackTrace();
			return false;
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
			return false;
		}
	}	// end method

	/**
	* Method to updates ref model run table for newer dates and probability.
	* @param con JDBC connection.
	* @param model_run_id Model run id.
	* @param startDate Starting date.
	* @param endDate Ending date.
	* @exception SQLException
	* @return True if successful.
	*/

	public static boolean updateRefModelRun (Connection conn, String hydroInd,
			int model_run_id, ModelDateTime startDate, ModelDateTime endDate)
	{
		ModelDateTime theDate;
		ModelDateTime sDate = startDate;
		ModelDateTime eDate = endDate;
		Statement stmt = null;
		ResultSet rs = null;
		String model_run_name = null;
		String sqlCommand = null;

		sqlCommand	= "select start_date, end_date, model_run_name from ref_model_run"
					+ " where model_run_id = " + model_run_id;

		try
		{
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sqlCommand);
			if (rs.next())
			{
				if (rs.getString(1) != null)
					theDate = new ModelDateTime(rs.getString(1));
				else
					theDate = new ModelDateTime ();
				if (theDate.getJulian() < sDate.getJulian())sDate = theDate;
				if (rs.getString(2) != null)
					theDate = new ModelDateTime(rs.getString(2));
				else
					theDate = new ModelDateTime ();
				if (theDate.getJulian() > eDate.getJulian())eDate = theDate;
				model_run_name = rs.getString(3);
				rs.close();
				stmt.close();
			}
			else
			{
				System.out.println("Unable to retrieve dates for model run id "
					+ model_run_id);
				rs.close();
				stmt.close();
				return false;
			}

            // call function that updates ref_model_run using stored procedure

            return HdbDmiUtils.updateRefModelRunRPC(conn, model_run_id, model_run_name, sDate, eDate, hydroInd);
		}
		catch (SQLException e)
		{
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			e.printStackTrace();
			return false;
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
			return false;
		}
	}	// end method

	/**
	* Method to get method id
	* @param con JDBC connection.
	* @param datatype_id Datatype id.
	* @param restOfLine Rest of control file line.
	* @return method_id.
	*/

	public static int getMethodId(Connection conn, String restOfLine)
	{
		int mid = 0;

		// determine if method id was specified in control file

		mid = ControlFileUtils.getMethodId(restOfLine);
		if (mid > 0)
		{
			// verfiy that user's method id is valid

			if (VerifyRecords.verifyHdbMethod(conn, mid)) return mid;
			else
			{
				System.out.println("Control file method id " + mid + " is invalid.");
				mid = 0;
				return mid;
			}
		}
		else
		{
			// use default method id

			mid = 18;
			System.out.println("Default method id is " + mid);
			return mid;
		}
	}	// end method

	/**
	* Method to get method id given timestep, sdi and date specs
	* @param con JDBC connection.
	* @param datatype_id Datatype id.
	* @param restOfLine Rest of control file line.
	* @param timeStep DMI timestep.
	* @param sdi site_datatype_id.
	* @param sDate Start date of query.
	* @param eDate End date of query.
	* @return method_id.
	*/

	public static int getMethodId(Connection conn, String restOfLine, String timeStep, int sdi,
								ModelDateTime sDate, ModelDateTime eDate)
	{
		int mid = 0;

		// determine if method id was specified in control file

		mid = ControlFileUtils.getMethodId(restOfLine);
		if (mid > 0)
		{
			// verfiy that user's method id is valid

			if (VerifyRecords.verifyHdbMethod(conn, mid)) return mid;
			else
			{
				System.out.println("Control file method id " + mid + " is invalid.");
				mid = 0;
				return mid;
			}
		}
		else
		{
			// try to use existing data to determine method id

			mid = HdbDmiUtils.getMethodIdGivenTimestepSDIDate(conn, timeStep, sdi, sDate);
			if (mid < 1)
			{
				mid = HdbDmiUtils.getMethodIdGivenTimestepSDIDates(conn, timeStep, sdi, sDate, eDate);
			}
			if (mid < 1)
			{
				// use default method id

				mid = 18;
			}
			return mid;
		}
	}	// end method

	/**
	* Method to retrieve a site id for a given site datatype id.
	* @param con JDBC connection.
	* @param site_datatype_id Site datatype id.
	* @exception SQLException
	* @return site id.
	*/

	public static int getSiteIdGivenSiteDatatypeId(Connection conn, int site_datatype_id)
	{
		int site_id = 0;
		Statement stmt = null;
		ResultSet rs = null;
		String sqlString = null;
		sqlString = "select site_id from hdb_site_datatype where site_datatype_id = " + site_datatype_id;
   		try
		{
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sqlString);
			if (rs.next())
			{
				site_id = rs.getInt(1);
			}
			else
			{
				System.out.println("Invalid site datatype id.");
			}
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing getSiteIdGivenSiteDatatypeId.");
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				rs.close();
				stmt.close();
			}
			catch (SQLException e)
			{
				System.out.println ("SQL error executing getSiteIdGivenSiteDatatypeId.");
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				e.printStackTrace();
			}
		}
		return site_id;
	}  // end method

	/**
	* Method to retrieve a datatype id for a given site datatype id.
	* @param con JDBC connection.
	* @param site_datatype_id Site datatype id.
	* @exception SQLException
	* @return datatype id.
	*/

	public static int getDatatypeIdGivenSiteDatatypeId(Connection conn, int site_datatype_id)
	{
		int datatype_id = 0;
		Statement stmt = null;
		ResultSet rs = null;
		String sqlString = null;
		sqlString = "select datatype_id from hdb_site_datatype where site_datatype_id = " + site_datatype_id;
   		try
		{
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sqlString);
			if (rs.next())
			{
				datatype_id = rs.getInt(1);
			}
			else
			{
				System.out.println("Invalid site datatype id.");
			}
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing getDatatypeIdGivenSiteDatatypeId.");
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				rs.close();
				stmt.close();
			}
			catch (SQLException e)
			{
				System.out.println ("SQL error executing getDatatypeIdGivenDatatypeId.");
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				e.printStackTrace();
			}
		}
		return datatype_id;
	}  // end method

	/**
	* Method to retrieve a datatype name for a given site datatype id.
	* @param con JDBC connection.
	* @param site_datatype_id Site datatype id.
	* @exception SQLException
	* @return datatype name.
	*/

	public static String getDatatypeNameGivenSiteDatatypeId(Connection conn, int site_datatype_id)
	{
		String datatype_name = null;
		Statement stmt = null;
		ResultSet rs = null;
		String sqlString = null;
		sqlString = "select a.datatype_name from hdb_datatype a, hdb_site_datatype b where a.datatype_id = b.datatype_id and site_datatype_id = " + site_datatype_id;
   		try
		{
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sqlString);
			if (rs.next())
			{
				datatype_name = rs.getString(1);
			}
			else
			{
				System.out.println("Invalid site datatype id.");
			}
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing getDatatypeNameGivenSiteDatatypeId.");
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				rs.close();
				stmt.close();
			}
			catch (SQLException e)
			{
				System.out.println ("SQL error executing getDatatypeNameGivenDatatypeId.");
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				e.printStackTrace();
			}
		}
		return datatype_name;
	}  // end method

	/**
	* Method to retrieve a unit id for a given site datatype id.
	* @param con JDBC connection.
	* @param site_datatype_id Site datatype id.
	* @exception SQLException
	* @return unit id.
	*/

	public static int getUnitIdGivenSiteDatatypeId(Connection conn, int site_datatype_id)
	{
		int unit_id = 0;
		Statement stmt = null;
		ResultSet rs = null;
		String sqlString = null;
		sqlString = "select a.unit_id from hdb_datatype a, hdb_site_datatype b where a.datatype_id = b.datatype_id and site_datatype_id = " + site_datatype_id;
   		try
		{
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sqlString);
			if (rs.next())
			{
				unit_id = rs.getInt(1);
			}
			else
			{
				System.out.println("Invalid site datatype id.");
			}
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing getUnitIdGivenSiteDatatypeId.");
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				rs.close();
				stmt.close();
			}
			catch (SQLException e)
			{
				System.out.println ("SQL error executing getUnitIdGivenDatatypeId.");
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				e.printStackTrace();
			}
		}
		return unit_id;
	}  // end method

	/**
	* Method to retrieve a unit name for a given site datatype id.
	* @param con JDBC connection.
	* @param site_datatype_id Site datatype id.
	* @exception SQLException
	* @return unit name.
	*/

	public static String getUnitNameGivenSiteDatatypeId(Connection conn, int site_datatype_id)
	{
		String unit_name = null;
		Statement stmt = null;
		ResultSet rs = null;
		String sqlString = null;
		sqlString = "select c.unit_name from hdb_datatype a, hdb_site_datatype b, hdb_unit c where a.datatype_id = b.datatype_id and a.unit_id = c.unit_id and site_datatype_id = " + site_datatype_id;
		// System.out.println("query string is " + sqlString);
   		try
		{
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sqlString);
			if (rs.next())
			{
				unit_name = rs.getString(1);
			}
			else
			{
				System.out.println("Invalid site datatype id - " + site_datatype_id);
			}
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing.");
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				rs.close();
				stmt.close();
			}
			catch (SQLException e)
			{
				System.out.println ("SQL error executing getUnitNameGivenDatatypeId.");
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				e.printStackTrace();
			}
		}
		return unit_name;
	}  // end method

	/**
	* Method to retrieve a unit common name for a given site datatype id.
	* @param con JDBC connection.
	* @param site_datatype_id Site datatype id.
	* @exception SQLException
	* @return unit name.
	*/

	public static String getUnitCommonNameGivenSiteDatatypeId(Connection conn, int site_datatype_id)
	{
		String unit_name = null;
		Statement stmt = null;
		ResultSet rs = null;
		String sqlString = null;
		sqlString = "select c.unit_common_name from hdb_datatype a, hdb_site_datatype b, hdb_unit c where a.datatype_id = b.datatype_id and a.unit_id = c.unit_id and site_datatype_id = " + site_datatype_id;
		// System.out.println("query string is " + sqlString);
   		try
		{
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sqlString);
			if (rs.next())
			{
				unit_name = rs.getString(1);
			}
			else
			{
				System.out.println("Invalid site datatype id - " + site_datatype_id);
			}
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing.");
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				rs.close();
				stmt.close();
			}
			catch (SQLException e)
			{
				System.out.println ("SQL error executing getUnitNameGivenDatatypeId,");
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				e.printStackTrace();
			}
		}
		return unit_name;
	}  // end method

	// method to get an hdb site datatype id

	public static int getSiteDatatypeId(Connection conn, int sid, int dtid)
	{
		int sdtid = 0;
		try
		{
			sdtid = HdbDmiUtils.getSiteDataType(conn, sid, dtid);
			if (sdtid <= 0)
			{
				System.out.println("Unable to validate site datatype id for site id " + sid + " and datatype id " + dtid);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.out.println("Unable to validate site datatype id for site id " + sid + " and datatype id " + dtid);
		}
		return sdtid;

	}	//	end method

	/**
	* Method to get method id given timestep, sdi, and date
	* @param conn JDBC connection.
	* @param timeStep DMI timestep.
	* @param sdi site_datatype_id.
	* @param theDate Date of query.
	* @return method_id.
	*/

	public static int getMethodIdGivenTimestepSDIDate(Connection conn, String timeStep, int sdi, ModelDateTime theDate)
	{
		Statement stmt = null;
		ResultSet rs = null;
		String sqlString = null;
		int mid = 0;

		// try to determine method id from an existing value point

		// sDate.getJDBCDate()

		if (timeStep.equalsIgnoreCase("1YEAR"))
		{
			sqlString = "select method_id from r_year where site_datatype_id = " + sdi;
		}
		else if (timeStep.equalsIgnoreCase("1MONTH"))
		{
			sqlString = "select method_id from r_month where site_datatype_id = " + sdi;
		}
		else if (timeStep.equalsIgnoreCase("1DAY"))
		{
			sqlString = "select method_id from r_day where site_datatype_id = " + sdi;
		}
		else if (timeStep.equalsIgnoreCase("1HOUR"))
		{
			sqlString = "select method_id from r_hour where site_datatype_id = " + sdi;
		}
		else if (timeStep.equalsIgnoreCase("INSTANT"))
		{
			mid = 13;
			return mid;
		}
		else
		{
			System.out.println ("Timestep " + timeStep + " is invalid.");
			return mid;
		}
		sqlString  = sqlString
					+ " and start_date_time = TO_DATE('"
					+ theDate.getOracleDateTime()
					+ "','YYYY-MM-DD-HH24-MI-SS')";
   		try
		{
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sqlString);
			if (rs.next())
			{
				mid = rs.getInt(1);
			}
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing getMethodIdGivenTimestepSDIDate.");
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				rs.close();
				stmt.close();
			}
			catch (SQLException e)
			{
				System.out.println ("SQL error executing getMethodIdGivenTimestepSDIDate.");
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				e.printStackTrace();
			}
		}
		return mid;
	}	// end method

	/**
	* Method to get method id given timestep, sdi, and a date range
	* @param conn JDBC connection.
	* @param timeStep DMI timestep.
	* @param sdi site_datatype_id.
	* @param sDate Start date of query.
	* @param eDate End date of query.
	* @return method_id.
	*/

	public static int getMethodIdGivenTimestepSDIDates(Connection conn, String timeStep, int sdi,
			ModelDateTime sDate, ModelDateTime eDate)
	{
		Statement stmt = null;
		ResultSet rs = null;
		String sqlString = null;
		int mid = 0;

		// try to determine method id from an existing value point

		// sDate.getJDBCDate()

		if (timeStep.equalsIgnoreCase("1YEAR"))
		{
			sqlString = "select method_id from r_year where site_datatype_id = " + sdi;
		}
		else if (timeStep.equalsIgnoreCase("1MONTH"))
		{
			sqlString = "select method_id from r_month where site_datatype_id = " + sdi;
		}
		else if (timeStep.equalsIgnoreCase("1DAY"))
		{
			sqlString = "select method_id from r_day where site_datatype_id = " + sdi;
		}
		else if (timeStep.equalsIgnoreCase("1HOUR"))
		{
			sqlString = "select method_id from r_hour where site_datatype_id = " + sdi;
		}
		else if (timeStep.equalsIgnoreCase("INSTANT"))
		{
			mid = 13;
			return mid;
		}
		else
		{
			System.out.println ("Timestep " + timeStep + " is invalid.");
			return mid;
		}
		sqlString  = sqlString
					+ " and start_date_time >= TO_DATE('"
					+ sDate.getOracleDateTime()
					+ "','YYYY-MM-DD-HH24-MI-SS')"
					+ " and start_date_time >= TO_DATE('"
					+ eDate.getOracleDateTime()
					+ "','YYYY-MM-DD-HH24-MI-SS')";
   		try
		{
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sqlString);
			if (rs.next())
			{
				mid = rs.getInt(1);
			}
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing getMethodIdGivenTimestepSDIDates.");
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				rs.close();
				stmt.close();
			}
			catch (SQLException e)
			{
				System.out.println ("SQL error executing getMethodIdGivenTimestepSDIDates.");
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				e.printStackTrace();
			}
		}
		return mid;
	}	// end method

	/**
	* Method to retrieve RiverWare units for a given site datatype id.
	* @param con JDBC connection.
	* @param site_datatype_id Site datatype id.
	* @exception SQLException
	* @return unit name.
	*/

	public static String getRiverWareUnitsGivenSiteDatatypeId(Connection conn, int site_datatype_id)
	{
		// default to HDB units

		String unit_name = HdbDmiUtils.getRiverWareUnitsGivenUnitName(HdbDmiUtils.getUnitCommonNameGivenSiteDatatypeId(conn, site_datatype_id));
		return unit_name;
	}  // end method

	// method to get RiverWare units given HDB unit id

	public static String getRiverWareUnitsGivenUnitId(int unitId)
	{
		String idUnit = String.valueOf(unitId);
		String getRiverWareUnitsGivenUnitId = "N/A";
		String[][] rwUnits	= {
		{"1","acre-feet"},
		{"2","cfs"},
		{"3","F"},
		{"4","feet"},
		{"5","in"},
		{"6","percent"},
		{"7","cfs-day"},
		{"8","cfs/ft"},
		{"9","cms"},
		{"10","m"},
		{"11","cm"},
		{"12","N/A"},
		{"13","C"},
		{"14","MW"},
		{"15","MWH"},
		{"16","decimal"},
		{"17","hour"},
		{"18","cms/m"},
		{"19","KWH/acre-ft"},
		{"20","acre"},
		{"21","mi2"},
		{"22","N/A"},
		{"23","N/A"},
		{"24","mg/l"},
		{"25","mi/hour"},
		{"26","N/A"},
		{"27","$/MWh"},
		{"28","N/A"},
		{"30","mi"},
		{"31","mg"},
		{"32","liters"},
		{"33","KWH"},
		{"34","ft3"},
		{"35","m3"},
		{"36","sec"},
		{"37","min"},
		{"38","in/day"},
		{"39","day"},
		{"40","km"},
		{"41","yd"},
		{"42","m3"},
		{"43","acre-ft/month"},
		{"44","ft2"},
		{"45","ha"},
		{"46","acre-ft/day"},
		{"47","acre-ft/year"},
		{"48","GW"},
		{"49","KW"},
		{"50","HP"},
		{"51","GWH"},
		{"52","g"},
		{"53","kg/m3"},
		{"54","mg"},
		{"55","N/A"},
		{"56","N/A"},
		{"57","ppm"},
		{"58","1/sec"},
		{"59","N/A"},
		{"60","1/hr"},
		{"61","1/day"},
		{"62","ft/s"},
		{"63","m/s"},
		{"64","cm/s"},
		{"65","km/hour"},
		{"66","cal"},
		{"67","N/A"},
		{"68","month"},
		{"69","year"},
		{"70","year"},
		{"71","N/A"},
		{"72","N/A"},
		{"73","N/A"},
		{"74","N/A"},
		{"75","NONE"},
		{"76","N/A"},
		{"77","N/A"},
		{"78","N/A"},
		{"79","N/A"},
		{"80","NONE"},
		{"81","N/A"},
		{"82","N/A"},
		{"83","N/A"},
		{"84","N/A"},
		{"85","N/A"},
		{"86","N/A"},
		{"87","N/A"},
		{"88","gal"},
		{"89","GL"},
		{"90","ML"},
		{"91","ML/day"},
		{"92","N/A"},
		{"93","N/A"},
		{"94","MWH/MW"},
		{"96","MWH/cfs-day"},
		{"97","MW/cms"},
		{"98","MWH/cms-day"},
		{"99","MWH/m3"},
		{"100","MW/cfs"},
		{"101","N/A"},
		{"102","N/A"},
		{"103","N/A"},
		{"104","N/A"},
		{"105","N/A"},
		{"106","cm/day"},
		{"107","cm/hour"},
		{"108","cm/month"},
		{"109","ft/day"},
		{"110","ft/month"},
		{"111","ft/year"},
		{"112","in/hour"},
		{"113","in/month"},
		{"114","m/month"},
		{"115","m/year"},
		{"116","metric_tons"},
		{"117","tons"},
		{"118","N/A"},
		{"119","J"},
		{"120","KJ"},
		{"121","g/ft2day"},
		{"122","g/m2day"},
		{"123","g/m2hr"},
		{"124","g/m2sec"},
		{"125","ft2/day"},
		{"126","ft2/s"},
		{"127","m2/day"},
		{"128","m2/s"},
		{"129","cal/cm2day"},
		{"130","J/m2day"},
		{"131","J/m2sec"},
		{"132","kcal/m2hr"},
		{"133","acre-ft/days"},
		{"134","acre-ft/dayhr"},
		{"135","acre-ft/day2"},
		{"136","cfsday"},
		{"137","cfshr"},
		{"138","cfss"},
		{"139","cmss"},
		{"140","cmshr"},
		{"141","cmsday"},
		{"142","cmshour"},
		{"143","95%CI_acre-ft/day"},
		{"144","95%CI_acre-ft/month"},
		{"145","95%CI_acre-ft/year"},
		{"146","95%CI_CFS"},
		{"147","95%CI_CMS"},
		{"148","stdDev_acre-ft/day"},
		{"149","stdDev_acre-ft/month"},
		{"150","stdDev_acre-ft/year"},
		{"151","stdDev_CFS"},
		{"152","stdDev_CMS"},
		{"153","ft/cfs"},
		{"154","m/cms"},
		{"155","N/A"},
		{"156","N/A"},
		{"157","N/A"},
		{"158","N/A"},
		{"159","N/A"},
		{"160","ft/cfs-day"},
		{"161","m/m3"},
		{"162","95%CI_cm"},
		{"163","95%CI_ft"},
		{"164","95%CI_in"},
		{"165","95%CI_km"},
		{"166","95%CI_m"},
		{"167","95%CI_mi"},
		{"168","95%CI_yd"},
		{"169","stdDev_cm"},
		{"170","stdDev_ft"},
		{"171","stdDev_in"},
		{"172","stdDev_km"},
		{"173","stdDev_m"},
		{"174","stdDev_mi"},
		{"175","stdDev_yd"}
					};
		for (int i = 0; i < rwUnits.length; i++)
		{
			if (idUnit.equalsIgnoreCase(rwUnits[i] [0]))
			{
				getRiverWareUnitsGivenUnitId = rwUnits[i] [1];
				break;
			}
		}
		return getRiverWareUnitsGivenUnitId;

	}	// end method

	// method to get RiverWare units given HDB common unit name

	public static String getRiverWareUnitsGivenUnitName(String uName)
	{
		String getRiverWareUnitsGivenUnitName = "N/A";
		String[][] rwUnits	= {
		{"acre-feet","acre-feet"},
		{"cubic feet per second","cfs"},
		{"degrees Fahrenheit","F"},
		{"feet","feet"},
		{"inches","in"},
		{"percent fraction","percent"},
		{"cfs-days","cfs-day"},
		{"cfs/feet","cfs/ft"},
		{"cubic meters per second","cms"},
		{"meters","m"},
		{"centimeters","cm"},
		{"degrees Kelvin","N/A"},
		{"degrees Centigrade","C"},
		{"megawatts","MW"},
		{"megawatt hours","MWH"},
		{"decimal fraction","decimal"},
		{"hours","hour"},
		{"cms/meter","cms/m"},
		{"kilowatt hours per AF","KWH/acre-ft"},
		{"acres","acre"},
		{"square miles","mi2"},
		{"cubic yards","N/A"},
		{"cfs/mile","N/A"},
		{"milligrams per liter","mg/l"},
		{"miles per hour","mi/hour"},
		{"dollars","N/A"},
		{"dollars per megawatt hr","$/MWh"},
		{"Langleys","N/A"},
		{"miles","mi"},
		{"milligrams","mg"},
		{"liters","liters"},
		{"kilowatt hours","KWH"},
		{"cubic feet","ft3"},
		{"cubic meters","m3"},
		{"seconds","sec"},
		{"minutes","min"},
		{"inches per day","in/day"},
		{"days","day"},
		{"kilometers","km"},
		{"yards","yd"},
		{"square meters","m3"},
		{"acre-feet per month","acre-ft/month"},
		{"square feet","ft2"},
		{"hectares","ha"},
		{"acre-feet per day","acre-ft/day"},
		{"acre-feet per year","acre-ft/year"},
		{"gigawatts","GW"},
		{"kilowatts","KW"},
		{"horse-power","HP"},
		{"gigawatt hours","GWH"},
		{"grams","g"},
		{"kilograms","kg/m3"},
		{"micrograms","mg"},
		{"tons per acre-foot","N/A"},
		{"kilograms per cubic foot","N/A"},
		{"parts per million","ppm"},
		{"1/second","1/sec"},
		{"1/minute","N/A"},
		{"1/hour","1/hr"},
		{"1/day","1/day"},
		{"feet per second","ft/s"},
		{"meters per second","m/s"},
		{"centimeters per second","cm/s"},
		{"kilometers per hour","km/hour"},
		{"calories","cal"},
		{"tons per day","N/A"},
		{"months","month"},
		{"years","year"},
		{"water years","year"},
		{"siemens per cm","N/A"},
		{"degrees","N/A"},
		{"tens of degrees","N/A"},
		{"voltage","N/A"},
		{"count","NONE"},
		{"turbidity-NTU","N/A"},
		{"turbidity-FTU","N/A"},
		{"pH","N/A"},
		{"microsiemens/cm @ 25 Cel","N/A"},
		{"flag value","NONE"},
		{"psi","N/A"},
		{"cfs * ppm","N/A"},
		{"CFS * Microsiemens/cm","N/A"},
		{"gallons per day","N/A"},
		{"Julian date","N/A"},
		{"inches per year","N/A"},
		{"BG","N/A"},
		{"gal","gal"},
		{"GL","GL"},
		{"ML","ML"},
		{"ML/day","ML/day"},
		{"mg/day","N/A"},
		{"KVA","N/A"},
		{"MWH/MW","MWH/MW"},
		{"MWH/cfs-day","MWH/cfs-day"},
		{"MW/cms","MW/cms"},
		{"MWH/cms-day","MWH/cms-day"},
		{"MWH/m3","MWH/m3"},
		{"MW/cfs","MW/cfs"},
		{"cfs-day/ft","N/A"},
		{"g/m3","N/A"},
		{"kg/m3","N/A"},
		{"mg/m3","N/A"},
		{"lb/ft3","N/A"},
		{"cm/day","cm/day"},
		{"cm/hour","cm/hour"},
		{"cm/month","cm/month"},
		{"ft/day","ft/day"},
		{"ft/month","ft/month"},
		{"ft/year","ft/year"},
		{"in/hour","in/hour"},
		{"in/month","in/month"},
		{"m/month","m/month"},
		{"m/year","m/year"},
		{"metric tons","metric_tons"},
		{"tons","tons"},
		{"cms/m3","N/A"},
		{"J","J"},
		{"KJ","KJ"},
		{"g/ft2day","g/ft2day"},
		{"g/m2day","g/m2day"},
		{"g/m2hr","g/m2hr"},
		{"g/m2sec","g/m2sec"},
		{"ft2/day","ft2/day"},
		{"ft2/s","ft2/s"},
		{"m2/day","m2/day"},
		{"m2/s","m2/s"},
		{"cal/cm2day","cal/cm2day"},
		{"J/m2day","J/m2day"},
		{"J/m2sec","J/m2sec"},
		{"kcal/m2hr","kcal/m2hr"},
		{"acre-ft/day/hr","acre-ft/days"},
		{"acre-ft/day/s","acre-ft/dayhr"},
		{"acre-ft/day2","acre-ft/day2"},
		{"cfs/day","cfsday"},
		{"cfs/hr","cfshr"},
		{"cfs/s","cfss"},
		{"cms/day","cmss"},
		{"cms/hr","cmshr"},
		{"cms/s","cmsday"},
		{"cmshour","cmshour"},
		{"95%CI_acre-ft/day","95%CI_acre-ft/day"},
		{"95%CI_acre-ft/month","95%CI_acre-ft/month"},
		{"95%CI_acre-ft/year","95%CI_acre-ft/year"},
		{"95%CI_CFS","95%CI_CFS"},
		{"95%CI_CMS","95%CI_CMS"},
		{"stdDev_acre-ft/day","stdDev_acre-ft/day"},
		{"stdDev_acre-ft/month","stdDev_acre-ft/month"},
		{"stdDev_acre-ft/year","stdDev_acre-ft/year"},
		{"stdDev_CFS","stdDev_CFS"},
		{"stdDev_CMS","stdDev_CMS"},
		{"ft/cfs","ft/cfs"},
		{"m/cms","m/cms"},
		{"cm/C","N/A"},
		{"ft/F","N/A"},
		{"in/F","N/A"},
		{"m/C","N/A"},
		{"m/F","N/A"},
		{"ft/cfs-day","ft/cfs-day"},
		{"m/m3","m/m3"},
		{"95%CI_cm","95%CI_cm"},
		{"95%CI_ft","95%CI_ft"},
		{"95%CI_in","95%CI_in"},
		{"95%CI_km","95%CI_km"},
		{"95%CI_m","95%CI_m"},
		{"95%CI_mi","95%CI_mi"},
		{"95%CI_yd","95%CI_yd"},
		{"stdDev_cm","stdDev_cm"},
		{"stdDev_ft","stdDev_ft"},
		{"stdDev_in","stdDev_in"},
		{"stdDev_km","stdDev_km"},
		{"stdDev_m","stdDev_m"},
		{"stdDev_mi","stdDev_mi"},
		{"stdDev_yd","stdDev_yd"}
					};
		for (int i = 0; i < rwUnits.length; i++)
		{
			if (uName.equalsIgnoreCase(rwUnits[i] [0]))
			{
				getRiverWareUnitsGivenUnitName = rwUnits[i] [1];
				break;
			}
		}
		return getRiverWareUnitsGivenUnitName;

	}	// end method

	/**
	* Method to real table data using stored procedure
	* @param ourConn JDBC connection.
	* @param site_datatype_id Site datatype id.
	* @param startDate Start date of model data period.
	* @param endDate End date of model data period.
	* @param agen_id Agency id.
	* @param loading_application_id Loading application id.
	* @param timeStep Timestep.
	* @exception Exception
	* @return True if successfully completed.
	*/

	public static boolean deleteRealRPC (Connection conn, int model_run_id,
			int site_datatype_id, ModelDateTime startDate, ModelDateTime endDate,
			int agen_id, int loading_application_id, String timeStep) throws Exception
	{
		System.out.println("Deleting real data using stored procedure.");
		String interval = null;
		CallableStatement cs = null;

		// java stored procedure

		// cs = conn.prepareCall("{call delete_r_base (?,?,?,?,?,?)}");
		// PL/SQL stored procedure

		cs = conn.prepareCall("begin delete_r_base (?,?,?,?,?,?); end;");

		if (timeStep.equalsIgnoreCase("1YEAR"))
		{
			interval = "year";
		}
		else if (timeStep.equalsIgnoreCase("1MONTH"))
		{
			interval = "month";
		}
		else if (timeStep.equalsIgnoreCase("1DAY"))
		{
			interval = "day";
		}
		else if (timeStep.equalsIgnoreCase("1HOUR"))
		{
			interval = "hour";
		}
		else if (timeStep.equalsIgnoreCase("INSTANT"))
		{
			interval = "instant";
		}
		else
		{
			System.out.println ("Timestep " + timeStep + " is invalid.");
			return false;
		}
		cs.setInt(1, site_datatype_id);
		cs.setString(2, interval);
		cs.setTimestamp(3, startDate.getJDBCDateTime());
		cs.setTimestamp(4, endDate.getJDBCDateTime());
		cs.setInt(5, agen_id);
		cs.setInt(6, loading_application_id);

		// run stored procedure

		try
		{
			cs.execute();
			cs.close();
		}
		catch (SQLException e)
		{
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println ("Message: " + e.getMessage ());
			System.err.println ("Vendor: " + e.getErrorCode ());
			e.printStackTrace();
			return false;
		}
		return true;
	}	// end method

	/**
	* Method to model table data using stored procedure
	* @param ourConn JDBC connection.
	* @param site_datatype_id Site datatype id.
	* @param model_run_id Model run id.
	* @param startDate Start date of model data period.
	* @param endDate End date of model data period.
	* @param timeStep Timestep.
	* @exception Exception
	* @return True if successfully completed.
	*/

	public static boolean deleteModelRPC (Connection conn, int model_run_id,
			int site_datatype_id, ModelDateTime startDate, ModelDateTime endDate,
			String timeStep) throws Exception
	{
		System.out.println("Deleting model data using stored procedure.");
		String interval = null;
		CallableStatement cs = null;

		// java stored procedure

		// cs = conn.prepareCall("{call delete_m_table (?,?,?,?,?)}");
		// PL/SQL stored procedure

		cs = conn.prepareCall("begin delete_m_table (?,?,?,?,?); end;");

		if (timeStep.equalsIgnoreCase("1YEAR"))
		{
			interval = "year";
		}
		else if (timeStep.equalsIgnoreCase("1MONTH"))
		{
			interval = "month";
		}
		else if (timeStep.equalsIgnoreCase("1DAY"))
		{
			interval = "day";
		}
		else if (timeStep.equalsIgnoreCase("1HOUR"))
		{
			interval = "hour";
		}
		else if (timeStep.equalsIgnoreCase("INSTANT"))
		{
			interval = "instant";
		}
		else
		{
			System.out.println ("Timestep " + timeStep + " is invalid.");
			return false;
		}
		cs.setInt(1, model_run_id);
		cs.setInt(2, site_datatype_id);
		cs.setTimestamp(3, startDate.getJDBCDateTime());
		cs.setTimestamp(4, endDate.getJDBCDateTime());
		cs.setString(5, interval);

		// run stored procedure

		try
		{
			cs.execute();
			cs.close();
		}
		catch (SQLException e)
		{
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println ("Message: " + e.getMessage ());
			System.err.println ("Vendor: " + e.getErrorCode ());
			e.printStackTrace();
			return false;
		}
		return true;
	}	// end method
}  // end class
