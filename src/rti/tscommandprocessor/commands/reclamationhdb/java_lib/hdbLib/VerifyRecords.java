//package java_lib.hdbLib;
// Provided by Dave King, Reclamation
package rti.tscommandprocessor.commands.reclamationhdb.java_lib.hdbLib;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

// verifies that a record exist in hdb for various tables// or retrieves a id for a specified source of data

// import classes stored in libraries


/**
* A class to verify that a record exists in hdb for various tables or retrieve an id for a specified source of data
*/

public class VerifyRecords
{

// No constructor needed because we won't be creating an actual object

	// verifies that a model id exists in hdb_model table

	public static boolean verifyHdbModel(Connection con, int model_id)
	{
		Statement stmt = null;
		ResultSet rs = null;
		boolean ranOk = false;
		String sqlCommand = null;

		sqlCommand = "select model_id from hdb_model where model_id = " + model_id;
   		try
		{
			stmt = con.createStatement();
			rs = stmt.executeQuery(sqlCommand);
			if (rs.next())
			{
				ranOk = true;
			}
			else
			{
				System.out.println("Invalid model id");
			}
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing verifyHdbModel");
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
				System.out.println ("SQL error executing verifyHdbModel");
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				e.printStackTrace();
			}
		}
		return ranOk;
	}	// end method

	// verifies that an external data source id id exists in hdb_ext_data_source table

	public static boolean verifyExtDataSource(Connection con, int ext_data_source_id)
	{
		Statement stmt = null;
		ResultSet rs = null;
		boolean ranOk = false;
		String sqlCommand = null;

		sqlCommand = "select ext_data_source_id from hdb_ext_data_source where ext_data_source_id = " + ext_data_source_id;
   		try
		{
			stmt = con.createStatement();
			rs = stmt.executeQuery(sqlCommand);
			if (rs.next())
			{
				ranOk = true;
			}
			else
			{
				System.out.println("Invalid model id");
			}
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing verifyExtDataSource");
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
				System.out.println ("SQL error executing verifyExtDataSource");
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				e.printStackTrace();
			}
		}
		return ranOk;
	}	// end method

	// verifies that a model run id exists in ref_model_run table

	public static boolean verifyRefModelRun(Connection con, int model_run_id)
	{
		Statement stmt = null;
		ResultSet rs = null;
		boolean ranOk = false;
		String sqlCommand = null;

		sqlCommand = "select model_run_id from ref_model_run" + " where model_run_id = " + model_run_id;

   		try
		{
			stmt = con.createStatement();
			rs = stmt.executeQuery(sqlCommand);
			if (rs.next())
			{
				ranOk = true;
			}
			else
			{
				System.out.println("Invalid model run id");
			}
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing verifyRefModelRun");
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
				System.out.println ("SQL error executing verifyRefModelRun");
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				e.printStackTrace();
			}
		}
		return ranOk;
	}	// end method

	// verifies that a source id exists in hdb_data_source table

	public static boolean verifyHdbSource(Connection con, int source_id)
	{
		Statement stmt = null;
		ResultSet rs = null;
		boolean ranOk = false;
		String sqlCommand = null;

		sqlCommand = "select source_id from hdb_data_source where source_id = " + source_id;
   		try
		{
			stmt = con.createStatement();
			rs = stmt.executeQuery(sqlCommand);
			if (rs.next())
			{
				ranOk = true;
			}
			else
			{
				System.out.println("Invalid source id");
			}
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing verifyHdbSource");
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
				System.out.println ("SQL error executing verifyRefModelRun");
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				e.printStackTrace();
			}
		}
		return ranOk;
	}	// end method

	// verifies that a site_datatype_id exists in hdb_site_datatype table

	public static boolean verifyHdbSiteDatatype(Connection con, int site_datatype_id)
	{
		Statement stmt = null;
		ResultSet rs = null;
		boolean ranOk = false;
		String sqlCommand = null;

		sqlCommand = "select site_datatype_id from hdb_site_datatype where site_datatype_id = " + site_datatype_id;
   		try
		{
			// Set up and sql statement

			stmt = con.createStatement();
			rs = stmt.executeQuery(sqlCommand);
 			if (rs.next())
			{
				ranOk = true;
			}
			else
			{
				System.out.println("Invalid site_datatype id");
			}
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing verifyHdbSiteDatatype");
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			e.printStackTrace();
		}
		catch (NullPointerException e)
		{
			System.out.println ("Null pointer exception found executing verifyHdbSiteDatatype");
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
				System.out.println ("SQL error executing verifyHdbSiteDatatype");
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				e.printStackTrace();
			}
		}
		return ranOk;
	}	// end method

	// verifies that an agency id exists in hdb_agency table

	public static boolean verifyHdbAgency(Connection con, int agen_id)
	{
		Statement stmt = null;
		ResultSet rs = null;
		boolean ranOk = false;
		String sqlCommand = null;

		sqlCommand = "select agen_id from hdb_agen where agen_id = " + agen_id;
   		try
		{
			stmt = con.createStatement();
			rs = stmt.executeQuery(sqlCommand);
			if (rs.next())
			{
				ranOk = true;
			}
			else
			{
				System.out.println("Invalid agency id");
			}
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing verifyHdbAgency");
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
				System.out.println ("SQL error executing verifyHdbAgency");
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				e.printStackTrace();
			}
		}
		return ranOk;
	}	// end method

	// verifies that a collection_system id exists in hdb_collection_system table

	public static boolean verifyHdbCollectionSystem(Connection con, int collection_system_id)
	{
		Statement stmt = null;
		ResultSet rs = null;
		boolean ranOk = false;
		String sqlCommand = null;

		sqlCommand	= "select collection_system_id from hdb_collection_system where collection_system_id = "
					+ collection_system_id;
   		try
		{
			stmt = con.createStatement();
			rs = stmt.executeQuery(sqlCommand);
			if (rs.next())
			{
				ranOk = true;
			}
			else
			{
				System.out.println("Invalid collection_system id");
			}
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing verifyHdbCollectionSystem");
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
				System.out.println ("SQL error executing verifyHdbCollectionSystem");
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				e.printStackTrace();
			}
		}
		return ranOk;
	}	// end method

	// verifies that a loading_application id exists in hdb_loading_application table

	public static boolean verifyHdbLoadingApplication(Connection con, int loading_application_id)
	{
		Statement stmt = null;
		ResultSet rs = null;
		boolean ranOk = false;
		String sqlCommand = null;

		sqlCommand	= "select loading_application_id from hdb_loading_application where loading_application_id = "
					+ loading_application_id;
   		try
		{
			stmt = con.createStatement();
			rs = stmt.executeQuery(sqlCommand);
			if (rs.next())
			{
				ranOk = true;
			}
			else
			{
				System.out.println("Invalid loading_application id");
			}
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing verifyHdbLoadingApplication");
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
				System.out.println ("SQL error executing verifyHdbLoadingApplication");
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				e.printStackTrace();
			}
		}
		return ranOk;
	}	// end method

	// verifies that a method id exists in hdb_method table

	public static boolean verifyHdbMethod(Connection con, int method_id)
	{
		Statement stmt = null;
		ResultSet rs = null;
		boolean ranOk = false;
		String sqlCommand = null;

		sqlCommand = "select method_id from hdb_method where method_id = " + method_id;
   		try
		{
			stmt = con.createStatement();
			rs = stmt.executeQuery(sqlCommand);
			if (rs.next())
			{
				ranOk = true;
			}
			else
			{
				System.out.println("Invalid method id");
			}
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing verifyHdbMethod");
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
				System.out.println ("SQL error executing verifyHdbMethod");
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				e.printStackTrace();
			}
		}
		return ranOk;
	}	// end method

	// verifies that a site_datatype_id exists in a generic rdb site_datatype table

	public static boolean verifyRdbSiteDatatype(Connection con, int site_datatype_id,
			String sdtTable, String sdtidField, String dbType)
	{
		String sqlCommand = null;
		Statement stmt = null;
		ResultSet rs = null;
		boolean ranOk = false;
   		try
		{
			stmt = con.createStatement();
			if (dbType.equalsIgnoreCase("mysql"))
				sqlCommand	= "select `" + sdtidField + "` from " + sdtTable
							+ " where `" + sdtidField + "` = " + site_datatype_id;
			else if (dbType.equalsIgnoreCase("mysqlHDB"))
				sqlCommand	= "select `" + sdtidField + "` from " + sdtTable
							+ " where `" + sdtidField + "` = " + site_datatype_id;
			else
				sqlCommand	= "select \"" + sdtidField + "\" from " + sdtTable
							+ " where \"" + sdtidField + "\" = " + site_datatype_id;
//			System.out.println("sql command is '" + sqlCommand + "'.");
			rs = stmt.executeQuery(sqlCommand);
			if (rs.next())
			{
//				System.out.println("Valid site datatype id found.");
				ranOk = true;
			}
			else
			{
				System.out.println("Invalid site_datatype id.");
			}
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing verifyRdbSiteDatatype.");
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			e.printStackTrace();
		}
		catch (NullPointerException e)
		{
			System.out.println ("Null pointer exception found executing verifyRdbSiteDatatype.");
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
				System.out.println ("SQL error executing verifyRdbSiteDatatype");
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				e.printStackTrace();
			}
		}
		return ranOk;
	}	// end method
}  // end class
