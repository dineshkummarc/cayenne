
package org.objectstyle.art.oneway;

import org.objectstyle.cayenne.access.event.DataObjectTransactionEvents;

public class Artist extends _Artist implements DataObjectTransactionEvents
{
	private boolean _receivedWillCommit = false;
	private boolean _receivedDidCommit = false;

	public Artist()
	{
		super();
	}
	
	public void didCommit()
	{
		_receivedDidCommit = true;
	}

	public void willCommit()
	{
		_receivedWillCommit = true;
	}

	public boolean receivedDidCommit()
	{
		return _receivedDidCommit;
	}
	
	public boolean receivedWillCommit()
	{
		return _receivedWillCommit;
	}
}



