package RTi.TS;

import java.util.Vector;

public interface TSCommandProcessorUI
{
	void quitProgram( int exitStatus );
	Vector getCommandsAboveSelected();
	void setWaitCursor ( boolean wait );
}
