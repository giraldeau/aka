package org.eclipse.linuxtools.lttng2.kernel.aka.views;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.lttng.studio.model.graph.Span;

/*
 * Implementation based on Lars Vogel: Eclipse JFace Table - Advanced Tutorial
 * http://www.vogella.com/articles/EclipseJFaceTableAdvanced/article.html#sortcolumns
 */

public class SpanColumnSorter extends ViewerComparator {
	  private int propertyIndex;
	  private static final int DESCENDING = 1;
	  private int direction = DESCENDING;

	  public SpanColumnSorter() {
	    this.propertyIndex = 0;
	    direction = DESCENDING;
	  }

	  public int getDirection() {
	    return direction == 1 ? SWT.DOWN : SWT.UP;
	  }

	  public void setColumn(int column) {
	    if (column == this.propertyIndex) {
	      // Same column as last sort; toggle the direction
	      direction = 1 - direction;
	    } else {
	      // New column; do an ascending sort
	      this.propertyIndex = column;
	      direction = DESCENDING;
	    }
	  }

	  @Override
	  public int compare(Viewer viewer, Object e1, Object e2) {
	    Span p1 = (Span) e1;
	    Span p2 = (Span) e2;
	    int rc = 0;
	    switch (propertyIndex) {
	    case 0:
	      rc = p1.getOwner().toString().compareTo(p2.getOwner().toString());
	      break;
	    case 1:
	      rc = p1.getCount() > p2.getCount() ? 1 : ( p1.getCount() == p2.getCount() ? 0 : -1);
	      break;
	    case 2:
	    case 3:
	      rc = p1.getTotal() > p2.getTotal() ? 1 : (p1.getTotal() == p2.getTotal() ? 0 : -1);
	      break;
	    default:
	      rc = 0;
	    }
	    // If descending order, flip the direction
	    if (direction == DESCENDING) {
	      rc = -rc;
	    }
	    return rc;
	  }

}
