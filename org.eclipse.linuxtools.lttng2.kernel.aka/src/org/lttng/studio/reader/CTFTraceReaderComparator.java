package org.lttng.studio.reader;

import java.util.Comparator;

import org.eclipse.linuxtools.ctf.core.trace.CTFTraceReader;

public class CTFTraceReaderComparator implements Comparator<CTFTraceReader> {

	@Override
	public int compare(CTFTraceReader a, CTFTraceReader b) {
		if (a.getCurrentEventDef() == null || b.getCurrentEventDef() == null)
			return 0;
        long ta = a.getCurrentEventDef().getTimestamp();
        long tb = b.getCurrentEventDef().getTimestamp();

        if (ta < tb) {
            return -1;
        } else if (ta > tb) {
            return 1;
        } else {
            return 0;
        }
	}

}
