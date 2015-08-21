package rti.tscommandprocessor.commands.reclamationpisces;

/**
Hold data from the Reclamation Pisces database that for the ref_parameter table.
*/
public class ReclamationPisces_Ref_Parameter
{

private String id = "";
private String description = "";

/**
Constructor.
*/
public ReclamationPisces_Ref_Parameter ()
{   super();
}

public String getDescription ()
{
	return this.description;
}

public String getID ()
{
	return this.id;
}

public void setDescription ( String description )
{
    this.description = description;
}

public void setID ( String id )
{
    this.id = id;
}

}