package rti.tscommandprocessor.commands.nrcs.awdb;

/**
Network code information to allow lookup/translation of network code and description.
This information is not currently available from a web service request but might be in the future.
*/
public class NrcsAwdbNetworkCode
{
    
/**
Network code.
*/
private String __code;

/**
Network description.
*/
private String __description = "";
   
/**
Constructor.
@param code site type code
@param description site type description
*/
public NrcsAwdbNetworkCode ( String code, String description )
{
    __code = code;
    __description = description;
}

/**
Return the network code.
*/
public String getCode()
{
    return __code;
}

/**
Return the network description.
*/
public String getDescription()
{
    return __description;
}

}