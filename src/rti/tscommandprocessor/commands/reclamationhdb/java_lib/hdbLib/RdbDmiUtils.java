//package java_lib.hdbLib;
// Provided by Dave King, Reclamation
package rti.tscommandprocessor.commands.reclamationhdb.java_lib.hdbLib;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import rti.tscommandprocessor.commands.reclamationhdb.java_lib.dmiLib.ModelDateTime;

// various utilities to support generic RDB DMI's

// import classes stored in libraries

/**
* Class to support generic RDB DMI's
*/

public class RdbDmiUtils
{

// No constructor needed because we won't be creating an actual object

	/**
	* Method to execute sql insert, delete, update command given connection.
	* @param conn JDBC connection.
	* @param sqlCommand Properly formatted sql command.
	* @return True if complete.
	*/

	public static boolean sendSqlCommand (Connection con, String sqlCommand)
											throws SQLException
	{
		boolean sqc = false;
		Statement stmt = con.createStatement();
		try
		{
			stmt.executeUpdate(sqlCommand);
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing sendSqlCommand.");
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			e.printStackTrace();
		}
		finally
		{
			stmt.close();
			return sqc;
		}
	}	// end method

	/**
	* Method to query entire table into a result set for specified order.
	* @param conn JDBC connection.
	* @param tableToQuery Table to query.
	* @param order Order to query in.
	* @return Table contents in order.
	*/

	public static ResultSet getTableContents(Connection conn, String tableToQuery, String order)
	{
		String sqlCommand = null;
		ResultSet rs = null;
		Statement stmt = null;
		if (order.length() == 0)
			sqlCommand = "select * from " + tableToQuery;
		else
			sqlCommand = "select * from " + tableToQuery + " order by " + order;
      	System.out.println("SQL command is '" + sqlCommand + "'");
		try
		{
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sqlCommand);
			System.out.println("Able to query table " + tableToQuery);
			return rs;
		}
		catch (Exception e)
		{
			System.out.println("Unable to query table " + tableToQuery);
		}
		return rs;
	}	// end method

	/**
	* Method to query table into a result set for specified order and where clause.
	* @param conn JDBC connection.
	* @param tableToQuery Table to query.
	* @param order Order to query in.
	* @param whereClause Properly formatted sql where clause.
	* @return Table contents in order and filtered.
	*/

	public static ResultSet runQueryGivenWhereClause(Connection conn, String tableToQuery,
					String order, String whereClause)
	{
		String sqlCommand = null;
		ResultSet rs = null;
		Statement stmt = null;
		if (order.length() == 0)
			if (whereClause.length() == 0)
				sqlCommand = "select * from " + tableToQuery;
			else
				sqlCommand = "select * from " + tableToQuery + " " + whereClause;
		else
			if (whereClause.length() == 0)
				sqlCommand = "select * from " + tableToQuery + " order by " + order;
			else
				sqlCommand	= "select * from " + tableToQuery + " " + whereClause
							+ " order by " + order;
		System.out.println("SQL command is " + sqlCommand);
		try
		{
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sqlCommand);
			System.out.println("Able to query table " + tableToQuery);
			return rs;
		}
		catch (Exception e)
		{
			System.out.println("Unable to query table " + tableToQuery);
			return rs;
		}
	}	// end method

	/**
	* Method to write an entire table to a text file using user's requested delimitor and result set.
	* @param rs Result set.
	* @param writeBuffer Buffered write object.
	* @param delimitor Delimitor.
	* @return True if successful.
	*/

	public static boolean writeDelimitedFile(ResultSet rs, PrintWriter writeBuffer, String delimitor)
	{
		try
		{
			// parse every record in the query and write to a text file

			ResultSetMetaData rsmd = rs.getMetaData();
			ModelDateTime mdt = null;
			while (rs.next())
			{
				try
				{
					for (int i = 1; i <= rsmd.getColumnCount(); i++)
					{
						if (i > 1)
						{
							if(delimitor.equals("\\t"))
								writeBuffer.print("\t");
							else
								writeBuffer.print(delimitor);
						}
						if (rsmd.getColumnType(i) == 93)
						{
							// field is a date

							if (rs.getString(i) != null)
							{
								writeBuffer.print("'");
								mdt = new ModelDateTime(rs.getString(i));
								writeBuffer.print(mdt.getSQLDateTime());
								writeBuffer.print("'");
							}
							else
							{
								writeBuffer.print("null");
							}
						}
						else
						{
							if (rsmd.getColumnType(i) == 12 || rsmd.getColumnType(i) == 1
								|| rsmd.getColumnType(i) == -4) writeBuffer.print('"');
							writeBuffer.print(rs.getString(i));
							if (rsmd.getColumnType(i) == 12 || rsmd.getColumnType(i) == 1
								|| rsmd.getColumnType(i) == -4) writeBuffer.print('"');
						}
					}
					writeBuffer.println("");
				}
				catch (Exception e)
				{
					System.out.println("Unable to complete data write.");
					writeBuffer.close();
					return false;
				}
			}
			System.out.println("Able to complete data write.");
			return true;
		}
		catch (Exception e)
		{
			return false;
		}

	}	// end method

	/**
	* Method to write an entire table to a text file as inserting statements.
	* @param rs Result set.
	* @param writeBuffer Buffered write object.
	* @param table Output table.
	* @return True if successful.
	*/

	public static boolean writeInsertingFile(ResultSet rs, PrintWriter writeBuffer, String table)
	{
		try
		{
			// parse every record in the query and write a text file

			String iBase = "insert into " + table + " (";
			int i = 0;
			ResultSetMetaData rsmd = rs.getMetaData();
			int columnCount = rsmd.getColumnCount();
			ModelDateTime mdt = null;

			for (i = 1; i <= columnCount; i++)
			{
				iBase = iBase + rsmd.getColumnName(i);
				if (i < columnCount)iBase = iBase + ", ";
			}
			iBase = iBase + ") values (";

			while (rs.next())
			{
				try
				{
					writeBuffer.print(iBase);
					for (i = 1; i <= columnCount; i++)
					{
						if (i > 1) writeBuffer.print(", ");
						if (rs.getString(i) == null)
						{
							writeBuffer.print("null");
						}
						else
						{
							if (rsmd.getColumnType(i) == 93)
							{
								// field is a date

								writeBuffer.print("to_date('");
								mdt = new ModelDateTime(rs.getString(i));
								writeBuffer.print(mdt.getOracleDateTime());
								writeBuffer.print("', 'YYYY-MM-DD HH24-MI-SS')");
							}
							else
							{
								if (rsmd.getColumnType(i) == 12 || rsmd.getColumnType(i) == 1
									|| rsmd.getColumnType(i) == -4) writeBuffer.print("'");
								writeBuffer.print(rs.getString(i));
								if (rsmd.getColumnType(i) == 12 || rsmd.getColumnType(i) == 1
									|| rsmd.getColumnType(i) == -4) writeBuffer.print("'");
							}
						}
					}
					writeBuffer.println(");");
				}
				catch (Exception e)
				{
					System.out.println("Unable to complete data write.");
					writeBuffer.close();
					return false;
				}
			}
			System.out.println("Able to complete data write.");
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}	// end method

	/**
	* Method to write an entire table to a text file as updating statements
	* @param rs Result set.
	* @param writeBuffer Buffered write object.
	* @param table Output table.
	* @param numberPrimaryFields Number of primary fields.
	* @param primaryFields[] List of primary field names.
	* @return True if successful.
	*/

	public static boolean writeUpdatingFile(ResultSet rs, PrintWriter writeBuffer,
				String table, int numberPrimaryFields, String primaryFields[])
	{
		try
		{
			// parse every record in the query and write to a text file

			String uBase = "update " + table + " set ";
			int i = 0;
			int j = 0;
			ResultSetMetaData rsmd = rs.getMetaData();
			int columnCount = rsmd.getColumnCount();
			ModelDateTime mdt = null;

			while (rs.next())
			{
				try
				{
					// write values

					writeBuffer.print(uBase);
					for (i = 1; i <= columnCount; i++)
					{
						writeBuffer.print(rsmd.getColumnName(i) + " = ");
						if (rsmd.getColumnType(i) == 93)
						{
							// field is a date

							writeBuffer.print("to_date('");
							mdt = new ModelDateTime(rs.getString(i));
							writeBuffer.print(mdt.getOracleDateTime());
							writeBuffer.print("', 'YYYY-MM-DD HH24-MI-SS')");
						}
						else
						{
							if (rsmd.getColumnType(i) == 12 || rsmd.getColumnType(i) == 1
								|| rsmd.getColumnType(i) == -4) writeBuffer.print("'");
							writeBuffer.print(rs.getString(i));
							if (rsmd.getColumnType(i) == 12 || rsmd.getColumnType(i) == 1
								|| rsmd.getColumnType(i) == -4) writeBuffer.print("'");
						}
						if (i < columnCount) writeBuffer.print(", ");
					}

					// write where clause

					writeBuffer.print(" where ");
					for (j = 0; j < numberPrimaryFields; j++)
					{
						for (i = 1; i <= columnCount; i++)
						{
							if(rsmd.getColumnName(i).equalsIgnoreCase(primaryFields[j]))
							{
								writeBuffer.print(rsmd.getColumnName(i) + " = ");
								if (rsmd.getColumnType(i) == 93)
								{
									// field is a date

									writeBuffer.print("to_date('");
									mdt = new ModelDateTime(rs.getString(i));
									writeBuffer.print(mdt.getOracleDateTime());
									writeBuffer.print("', 'YYYY-MM-DD HH24-MI-SS')");
								}
								else
								{
									if (rsmd.getColumnType(i) == 12 || rsmd.getColumnType(i) == 1
										|| rsmd.getColumnType(i) == -4) writeBuffer.print('"');
									writeBuffer.print(rs.getString(i));
									if (rsmd.getColumnType(i) == 12 || rsmd.getColumnType(i) == 1
										|| rsmd.getColumnType(i) == -4) writeBuffer.print('"');
								}
							}

						}	// end of loop thru columns

						if (j < (numberPrimaryFields - 1))writeBuffer.print(" and ");

					}	// end of loop thru primary fields

					writeBuffer.println(";");
				}
				catch (Exception e)
				{
					System.out.println("Unable to complete data write.");
					writeBuffer.close();
					return false;
				}
			}
			System.out.println("Able to complete data write.");
			return true;
		}
		catch (Exception e)
		{
			return false;
		}

	}	// end method

	/**
	* Method to retrieve a site name for a given site id.
	* @param conn JDBC connection.
	* @param site_id Site id.
	* @param siteTable Site table name.
	* @param idField Field that contains site id's.
	* @param nameField Field that contains site names.
	* @exception IOException
	* @return Site name.
	*/

	public static String getSiteNameGivenSiteId(Connection conn, int site_id,
			String siteTable, String idField, String nameField) throws IOException
	{
		String site_name = null;
   		try
		{
			// Create sql statement

			String sqlCommand = null;
			Statement stmt = conn.createStatement();

			// set up and do query

//			sqlCommand = "select \"" + nameField + "\" from " + siteTable + " where \"" + idField + "\" = " + site_id;
			sqlCommand = "select `" + nameField + "` from " + siteTable + " where `" + idField + "` = " + site_id;
			ResultSet rs = stmt.executeQuery(sqlCommand);
			if (rs.next())
			{
				site_name = rs.getString(1);
				rs.close();
				stmt.close();
				return site_name;
			}
			else
			{
				rs.close();
				stmt.close();
				System.out.println("Invalid site id.");
				return site_name;
			}
		}
		catch (SQLException e)
		{
			System.out.println ("SQL error executing getSiteNameGivenSiteId.");
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			e.printStackTrace();
			return site_name;
		}
		catch (Exception e)
		{
			System.out.println ("Error executing getSiteNameGivenSiteId.");
			System.out.println(e.getMessage());
			e.printStackTrace();
			return site_name;
   		}
	}  // end method

	/**
	* Method to get site data type id for a site id and data type id.
	* @param conn JDBC connection.
	* @param site_id Site id.
	* @param datatype_id Datatype id.
	* @param sdtTable Site datatype table name.
	* @param sidField Field that contains site id's.
	* @param dtField Field that contains datatype id's.
	* @param sdtidField Field that contains site datatype id's.
	* @exception Exception
	* @return Site datatype id.
	*/

	public static int getSiteDataType(Connection con, int site_id, int datatype_id,
		String sdtTable, String sidField, String dtField, String sdtidField) throws Exception
	{
		String sqlCommand = null;
		Statement stmt;
		ResultSet rs;
		stmt = con.createStatement();
		int sdtid = 0;

		// set up and do query

		sqlCommand	= "select `" + sdtidField + "` from " + sdtTable
					+ " where `" + sidField + "` = " + site_id
					+ " and `" + dtField + "` = " + datatype_id;
		rs = stmt.executeQuery(sqlCommand);
		if (rs.next())
		{
			sdtid = rs.getInt(1);
			rs.close();
			stmt.close();
			return sdtid;
		}
		else
		{
			rs.close();
			stmt.close();
 			System.out.println("No site datatype id exists for site " + site_id +
				" and datatype " + datatype_id + " in table " + sdtTable);
			return 0;
		}
	}	// end method

	/**
	* Method to get site data type id for an object, slot, and model_id from an internal mapping table.
	* @param conn JDBC connection.
	* @param object_name Object name.
	* @param slot_name data name.
	* @param model_id Model id.
	* @param mapTable Mapping table name.
	* @param objField Field that contains objects.
	* @param slotField Field that contains slots.
	* @param sdtidField Field that contains site datatype id's.
	* @param midField Field that contains model id's.
	* @exception Exception
	* @return Site data type.
	*/

	public static int getSiteDataType(Connection con, String object_name, String slot_name, int model_id,
		String mapTable, String objField, String slotField, String sdtidField, String midField) throws Exception
	{
		String sqlCommand = null;
		Statement stmt;
		ResultSet rs;
		stmt = con.createStatement();
		int sdtid = 0;

		// set up and do query

		sqlCommand	= "select `" + sdtidField + "` from " + mapTable
					+ " where `" + objField + "` = " + object_name
					+ " and `" + slotField + "` = " + slot_name
					+ " and `" + midField + "` = " + model_id;
		rs = stmt.executeQuery(sqlCommand);
		if (rs.next())
		{
			sdtid = rs.getInt(1);
			rs.close();
			stmt.close();
			return sdtid;
		}
		else
		{
			rs.close();
			stmt.close();
 			System.out.println("No site datatype id exists for object " + object_name +
				" and slot " + slot_name + " in table " + mapTable);
			return 0;
		}
	}	// end method

	/**
	* Method to get site data type id for an object and slot from an internal mapping table.
	* @param conn JDBC connection.
	* @param object_name Object name.
	* @param slot_name data name.
	* @param mapTable Mapping table name.
	* @param objField Field that contains objects.
	* @param slotField Field that contains slots.
	* @param sdtidField Field that contains site datatype id's.
	* @exception Exception
	* @return Site data type.
	*/

	public static int getSiteDataType(Connection con, String object_name, String slot_name,
		String mapTable, String objField, String slotField, String sdtidField) throws Exception
	{
		String sqlCommand = null;
		Statement stmt;
		ResultSet rs;
		stmt = con.createStatement();
		int sdtid = 0;

		// set up and do query

		sqlCommand	= "select `" + sdtidField + "` from " + mapTable
					+ " where `" + objField + "` = " + object_name
					+ " and `" + slotField + "` = " + slot_name;
		rs = stmt.executeQuery(sqlCommand);
		if (rs.next())
		{
			sdtid = rs.getInt(1);
			rs.close();
			stmt.close();
			return sdtid;
		}
		else
		{
			rs.close();
			stmt.close();
 			System.out.println("No site datatype id exists for object " + object_name +
				" and slot " + slot_name + " in table " + mapTable);
			return 0;
		}
	}	// end method

	/**
	* Method to retrieve a site id for a given site datatype id.
	* @param conn JDBC connection.
	* @param site_datatype_id Site datatype id.
	* @param sdtTable Site datatype table name.
	* @param sidField Field that contains site id's.
	* @param sdtidField Field that contains site datatype id's.
	* @exception Exception
	* @return site id.
	*/

	public static int getSiteIdGivenSiteDatatypeId(Connection con, int site_datatype_id,
		String sdtTable, String sidField, String sdtidField) throws Exception
	{
		String sqlCommand = null;
		Statement stmt;
		ResultSet rs;
		stmt = con.createStatement();
		int sid = 0;

		// set up and do query

		sqlCommand	= "select `" + sidField + "` from " + sdtTable
				+ " where `" + sdtidField + "` = " + site_datatype_id;
		rs = stmt.executeQuery(sqlCommand);
		if (rs.next())
		{
			sid = rs.getInt(1);
			rs.close();
			stmt.close();
			return sid;
		}
		else
		{
			rs.close();
			stmt.close();
 			System.out.println("No site id exists for site datatype id " + site_datatype_id + " in table " + sdtTable);
			return 0;
		}
	}	// end method

	/**
	* Method to retrieve a datatype id for a given site datatype id.
	* @param conn JDBC connection.
	* @param site_datatype_id Site datatype id.
	* @param sdtTable Site datatype table name.
	* @param dtidField Field that contains site id's.
	* @param sdtidField Field that contains site datatype id's.
	* @exception Exception
	* @return datatype id.
	*/

	public static int getDatatypeIdGivenSiteDatatypeId(Connection con, int site_datatype_id,
		String sdtTable, String dtidField, String sdtidField) throws Exception
	{
		String sqlCommand = null;
		Statement stmt;
		ResultSet rs;
		stmt = con.createStatement();
		int dtid = 0;

		// set up and do query

		sqlCommand	= "select `" + dtidField + "` from " + sdtTable
				+ " where `" + sdtidField + "` = " + site_datatype_id;
		rs = stmt.executeQuery(sqlCommand);
		if (rs.next())
		{
			dtid = rs.getInt(1);
			rs.close();
			stmt.close();
			return dtid;
		}
		else
		{
			rs.close();
			stmt.close();
 			System.out.println("No datatype id exists for site datatype id " + site_datatype_id + " in table " + sdtTable);
			return 0;
		}
	}	// end method

	// method to get a generic rdb site datatype id

	public static int getSiteDatatypeId(Connection conn, int sid, int dtid, String sdtTable,
								String sidField, String dtField, String sdtidField)
	{
		int sdtid = 0;
		try
		{
			sdtid = RdbDmiUtils.getSiteDataType(conn, sid, dtid, sdtTable, sidField, dtField, sdtidField);
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

}  // end class

