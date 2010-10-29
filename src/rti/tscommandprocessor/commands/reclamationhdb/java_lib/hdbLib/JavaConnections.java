// Provided by Dave King, Reclamation
//package java_lib.hdbLib;
package rti.tscommandprocessor.commands.reclamationhdb.java_lib.hdbLib;

// db driver and connection methods

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
* Class to support java jdbc connections.
*/

public class JavaConnections extends Object
{

	// public variables

	public Connection ourConn = null;
	String rolePassword = null;

	/**
	* Constructor loads Driver Manager and makes connection HDB Constructor
	*/

	public JavaConnections(String dbType, String dbUrl,
			String userName, String userPassword, String reqRole,
			String passwordUser, String hdbLocal)
	{
		try
		{
			if (this.loadDriverManager(dbType))
			{
				ourConn = this.makeConnection(dbType, dbUrl,
					userName, userPassword, reqRole, passwordUser, hdbLocal);
			}
		}
		catch (java.lang.Exception e)
		{
			e.printStackTrace();
		}

	}	// end constructor

	/**
	* Constructor loads Driver Manager and makes connection
	* Non HDB Constructor useing url or dsn
	*/

	public JavaConnections(String dbType, String dbUrl,	String userName, String userPassword)
	{
		try
		{
			if (this.loadDriverManager(dbType))
			{
				ourConn = this.makeConnection(dbType, dbUrl, userName, userPassword);
			}
		}
		catch (java.lang.Exception e)
		{
			e.printStackTrace();
		}

	}	// end constructor

	/**
	* Method to load driver manager
	* @param dbType Database type
	* @return True if successfully completed.
	*/

	public boolean loadDriverManager(String dbType)

	{
		try
		{
			if (dbType.equalsIgnoreCase("OracleHDB"))
					Class.forName("oracle.jdbc.driver.OracleDriver");
			if (dbType.equalsIgnoreCase("OracleRDB"))
					Class.forName("oracle.jdbc.driver.OracleDriver");
			else if (dbType.equalsIgnoreCase("mysqlHDB"))

				// hdb mysql

				Class.forName("org.gjt.mm.mysql.Driver");
			else if (dbType.equalsIgnoreCase("mysql"))

				// general mysql

				Class.forName("org.gjt.mm.mysql.Driver");
			else if (dbType.equalsIgnoreCase("postgresql"))

				// hdb postgresql

				Class.forName("org.postgresql.Driver");
			else if (dbType.equalsIgnoreCase("psqlHDB"))

				// general postgresql

				Class.forName("org.postgresql.Driver");
			else if (dbType.equalsIgnoreCase("odbc"))

				// general non Access odbc

				Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
			else if (dbType.equalsIgnoreCase("access"))

				// general Access odbc

				Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
			else

				// default to non HDB Oracle for now

				Class.forName("oracle.jdbc.driver.OracleDriver");

			System.out.println("JDBC driver manager loaded successfully for database type " + dbType + ".");
			return true;
		}
		catch(java.lang.ClassNotFoundException e)
		{
			System.out.println("Unable to load JDBC driver manager for database type " + dbType + ".");
			e.printStackTrace();
			return false;
		}
	}

	// connect to HDB type database

	/**
	* Method to connect to database.
	* @param dbType Database type
	* @param dbUrl Database URL
	* @param userName Database user name.
	* @param userPassword Password for database user.
	* @param reqRole Requested role for connection.
	* @param passwordUser Password user.
	* @param hdbLocal Name of local Hdb instance.
	* @return Connection object.
	*/

	public Connection makeConnection(String dbType, String dbUrl,
			String userName, String userPassword, String reqRole,
			String passwordUser, String hdbLocal)
	{
		Connection cReturn;
		try
		{
			if (dbType.equalsIgnoreCase("OracleHDB"))
				cReturn = this.roleConnect(dbUrl, userName, userPassword, reqRole,
							passwordUser, hdbLocal);
			else if (dbType.equalsIgnoreCase("OracleRDB"))
				cReturn = this.userThinConnect(dbUrl, userName, userPassword);
			else if (dbType.equalsIgnoreCase("mysqlHDB"))
				cReturn = this.userThinConnect(dbUrl, userName, userPassword);
			else if (dbType.equalsIgnoreCase("mysql"))
				cReturn = this.userThinConnect(dbUrl, userName, userPassword);
			else if (dbType.equalsIgnoreCase("psqlHDB"))
				cReturn = this.userThinConnect(dbUrl, userName, userPassword);
			else if (dbType.equalsIgnoreCase("postgresql"))
				cReturn = this.userThinConnect(dbUrl, userName, userPassword);
			else if (dbType.equalsIgnoreCase("odbc"))
				cReturn = this.userOdbcConnect(dbUrl, userName, userPassword);
			else if (dbType.equalsIgnoreCase("access"))
				cReturn = this.userOdbcConnect(dbUrl, userName, userPassword);
			else

				// default to thin quick connect

				cReturn = this.userThinConnect(dbUrl, userName, userPassword);

			return cReturn;
		}
		catch(java.lang.Exception e)
		{
			System.out.println("Unable to connect to url " + dbUrl + ".");
			e.printStackTrace();
			return null;
		}
	}

	// connect to a general database using url or dsn

	/**
	* Method to connect to database.
	* @param dbType Database type
	* @param dbUrl Database URL or DSN
	* @param userName Database user name.
	* @param userPassword Password for database user.
	* @return Connection object.
	*/

	public Connection makeConnection(String dbType, String dbUrl, String userName, String userPassword)
	{
		Connection cReturn;
		try
		{
			if (dbType.equalsIgnoreCase("odbc") || dbType.equalsIgnoreCase("access"))

				// odbc - dbUrl is used as dsn

				cReturn = this.userOdbcConnect(dbUrl, userName, userPassword);
			else
				cReturn = this.userThinConnect(dbUrl, userName, userPassword);
			return cReturn;
		}
		catch(java.lang.Exception e)
		{
			System.out.println("Unable to connect to dsn/url " + dbUrl + ".");
			e.printStackTrace();
			return null;
		}
	}

	// Connection using HDB's app role connection approach

	/**
	* Method to connect using HDB's app role connection approach.
	* @param dbUrl Database URL
	* @param userName User name.
	* @param userPassword User password.
	* @param requestedRole Requested role.
	* @param passwordUser Password user.
	* @param hdbLocal Name of local hdb instance.
	* @return True if successfully completed.
	*/

	public Connection roleConnect(String dbUrl, String userName,
				String userPassword, String requestedRole,
				String passwordUser, String hdbLocal)
	{
		Connection theConn = null;
		try
		{

			// connect as password user to check roles

			try
			{
				theConn = DriverManager.getConnection(dbUrl, passwordUser, hdbLocal);

				// check if user is granted requested role

				if (checkGrantedRole(userName, requestedRole, theConn))
				{
					if (getRolePassword(requestedRole, theConn))
					{

						// close connection as password user and reconnect as user

						theConn.close();
						theConn = userThinConnect(dbUrl, userName, userPassword);
						if (theConn != null)
						{

							// Enable user as requested role

							if (!(enableUserAsRole(requestedRole, theConn)))
							{
								System.out.println("Unable to enable requested role to user.");
								theConn.close();
								return null;
							}
						}
						else
						{
							theConn.close();
							return null;
						}
					}
					else // invalid role
					{
						System.out.println("Unable to retrieve role password.");
						theConn.close();
						return null;
					}
				}
				else // invalid role
				{
					System.out.println("Invalid user role.");
					theConn.close();
					return null;
				}
			}
			catch(java.lang.Exception e) // catch of password user login
			{
				System.out.println("Invalid password user.");
				e.printStackTrace();
				return null;
			}
			System.out.println("User is enabled as requested role.");
			return theConn;
		}
		catch(java.lang.Exception e)   // catch of method
		{
			e.printStackTrace();
			System.out.println("Unable to connect as requested role.");
			return null;
		}
	}

	// quick and dirty connection as user using thin protocol

	/**
	* Method to connect as user using thin protocol.
	* @param dbUrl Database Url.
	* @param userName User name.
	* @param userPassword User password.
	* @return True if successfully completed.
	*/

	public Connection userThinConnect(String dbUrl, String userName, String userPassword)
	{
		Connection theConn = null;
		try
		{

			theConn = DriverManager.getConnection(dbUrl, userName, userPassword);
			System.out.println("User " + userName + " is connected to database.");

		}
		catch(java.lang.Exception e)
		{
			System.out.println("Unable to connect to database as " + userName);
			e.printStackTrace();
		}
		return theConn;
	}	// end method

	// quick and dirty connection as user using odbc protocol

	/**
	* Method to connect as user using odbc protocol.
	* @param dsn Data Source Name.
	* @param userName User name.
	* @param userPassword User password.
	* @return True if successfully completed.
	*/

	public Connection userOdbcConnect(String dsn, String userName, String userPassword)
	{
		Connection theConn = null;
		String cString = null;
		cString	= "jdbc:odbc:" + dsn + ";UID=" + userName + ";PWD=" + userPassword;
		try
		{
			theConn = DriverManager.getConnection(cString);
			System.out.println("User is connected to dsn " + dsn + ".");
		}
		catch(java.lang.Exception e)
		{
			System.out.println("Unable to connect to dsn " + dsn + ".");
			e.printStackTrace();
		}
		return theConn;
	}	// end method

	// checks if user is granted a requested role

	private boolean checkGrantedRole(String userName, String roleName,
									Connection theConn)
	{

		try
		{

			// Create sql statement

			StringBuffer buf = null;
			buf = new StringBuffer(1000);
			buf.append("select GRANTEE from dba_role_privs where GRANTEE = '");
			buf.append(userName.toUpperCase());
			buf.append("' and GRANTED_ROLE = '");
			buf.append(roleName.toUpperCase());
			buf.append("'");

			Statement stmt = theConn.createStatement();

			// do query

			ResultSet rs = stmt.executeQuery(buf.toString());
			if (rs.next())
			{
			    rs.close();
			    return true;
			}
			else
			{
				rs.close();
				return false;
			}
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
	}

	// get role password

	private boolean getRolePassword(String roleName, Connection theConn)
	{

		try
		{

			// Create sql statement

			StringBuffer buf = null;
			buf = new StringBuffer(1000);
			buf.append("select PSSWD from role_psswd where ROLE = '");
			buf.append(roleName);
			buf.append("'");

			Statement stmt = theConn.createStatement();

			// do query

			ResultSet rs = stmt.executeQuery(buf.toString());
			if (rs.next())
			{
				rolePassword = rs.getString(1);
			    rs.close();
			    return true;
			}
			else
			{
				rs.close();
				return false;
			}
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
	}

	// Enable user at requested role
	// This is an Oracle specific command

	private boolean enableUserAsRole(String requestedRole, Connection theConn)
	{

		try
		{

			// Create sql statement

			StringBuffer buf = null;
			buf = new StringBuffer(1000);
			buf.append("set role ");
			buf.append(requestedRole);
			buf.append(" identified by ");
			buf.append(rolePassword);
			buf.append(", CONNECT");

			Statement stmt = theConn.createStatement();

			// do query

			if (stmt.execute(buf.toString()))
			{

				// returns true if result is a result set
				// which is not what we expect

			    stmt.close();
				return false;
			}
			else
			{

				// returns false if result is an integer
				// which is what we think is correct ???

				stmt.close();
			    return true;
			}
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
	}

	// method to close primary connection

	public void close()
	{
		try
		{
			ourConn.close();
		}
		catch (java.lang.Exception e)
		{
			e.printStackTrace();
		}
	}

	// method to close other connections

	static public void close(Connection theConn)
	{
		try
		{
			theConn.close();
		}
		catch (java.lang.Exception e)
		{
			e.printStackTrace();
		}
	}
}