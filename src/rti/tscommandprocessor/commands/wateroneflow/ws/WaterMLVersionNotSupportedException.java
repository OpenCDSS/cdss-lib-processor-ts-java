package rti.tscommandprocessor.commands.wateroneflow.ws;

import java.security.InvalidParameterException;

/**
This exception should be thrown when a method does not support a certain WaterML version.
*/
public class WaterMLVersionNotSupportedException extends InvalidParameterException
{

/**
Construct with a string message.
@param s String message.
*/
public WaterMLVersionNotSupportedException( String s )
{	super ( s );
}

}