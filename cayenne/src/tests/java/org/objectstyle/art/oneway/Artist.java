package org.objectstyle.art.oneway;

import org.objectstyle.cayenne.access.event.DataContextEvent;
import org.objectstyle.cayenne.access.event.DataObjectTransactionEventListener;

public class Artist
    extends _Artist
    implements DataObjectTransactionEventListener {
    private boolean _receivedWillCommit = false;
    private boolean _receivedDidCommit = false;

    public Artist() {
        super();
    }

    public void didCommit(DataContextEvent event) {
        _receivedDidCommit = true;
    }

    public void willCommit(DataContextEvent event) {
        _receivedWillCommit = true;
    }

    public boolean receivedDidCommit() {
        return _receivedDidCommit;
    }

    public boolean receivedWillCommit() {
        return _receivedWillCommit;
    }

    public void resetEvents() {
        _receivedWillCommit = false;
        _receivedDidCommit = false;
    }
}
