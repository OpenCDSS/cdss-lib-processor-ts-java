//package java_lib.hdbLib;
// Provided by Dave King, Reclamation
package rti.tscommandprocessor.commands.reclamationhdb.java_lib.hdbLib;

import java.sql.Connection;

// this function establishes source id and validation for real tables being populated

// import classes stored in libraries

/**
* A class to establishe source id and validation for real tables being populated
*/

public class SetSourceId
{

	public boolean IdIsValid;
	public int source_id;
	public String validation;

	private String sourceType;
	private String sourceValue;

	public SetSourceId(Connection con, String st, String sv)

	{
		sourceType = st;
		sourceValue = sv;
		try
		{

			// determine if we are using user model run id or 
			// are going to make one up

			if (sourceType.equals("SourceName"))
			{

				// user provided a source by name - get it's source id

				source_id = HdbDmiUtils.getHdbSourceIdGivenName(con, sourceValue);
				try
				{
					validation = HdbDmiUtils.getValidationGivenSourceId(con, source_id);
					IdIsValid = true;
				}
				catch (Exception e)
				{
					e.printStackTrace();
					IdIsValid = false;
				}
			}
			else
			{
				if (sourceType.equals("SourceExe"))
				{

					// user provided an executable name

					source_id = HdbDmiUtils.getHdbSourceIdGivenExe(con, sourceValue);
					try
					{
						validation = HdbDmiUtils.getValidationGivenSourceId(con,source_id);
						IdIsValid = true;
					}
					catch (Exception e)
					{
						IdIsValid = false;
						e.printStackTrace();
					}
				}
				else
				{

					// user provided a source id

					source_id = Integer.valueOf(sourceValue).intValue();
					try
					{
						if (VerifyRecords.verifyHdbSource(con, source_id))
						{

							validation = HdbDmiUtils.getValidationGivenSourceId(con, source_id);
							IdIsValid = true;
						}
						else
						{
							IdIsValid = false;
						}
					}
					catch (Exception e)
					{
						e.printStackTrace();
						IdIsValid = false;
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			IdIsValid = false;
		}

	} // end method/constructor

}  // end class
