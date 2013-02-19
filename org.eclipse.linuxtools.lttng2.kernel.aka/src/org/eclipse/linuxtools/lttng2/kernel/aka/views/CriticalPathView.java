package org.eclipse.linuxtools.lttng2.kernel.aka.views;

import java.util.HashMap;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.lttng.studio.model.graph.CriticalPathStats;
import org.lttng.studio.model.graph.ExecGraph;
import org.lttng.studio.model.graph.ExecVertex;
import org.lttng.studio.model.graph.Span;
import org.lttng.studio.model.kernel.SystemModel;
import org.lttng.studio.model.kernel.Task;
import org.lttng.studio.reader.handler.IModelKeys;

public class CriticalPathView extends AbstractAKAView {

	public static final String ID = "org.eclipse.linuxtools.lttng2.kernel.aka.views.criticalpath"; //$NON-NLS-1$

	private TableViewer table;

	private Task fCurrentTask;

	private Composite composite;

	private long spanSum;

	private SpanColumnSorter comparator;

	public CriticalPathView() {
		super(ID);
	}

	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new FillLayout());
		table = new TableViewer(composite, SWT.H_SCROLL | SWT.V_SCROLL
				| SWT.BORDER);
		table.setContentProvider(ArrayContentProvider.getInstance());
		Table t = table.getTable();
		t.setHeaderVisible(true);
		t.setLinesVisible(true);
		createColumns();
		comparator = new SpanColumnSorter();
		table.setComparator(comparator);
	}

	private void createColumns() {
		TableViewerColumn col = new TableViewerColumn(table, SWT.NONE);
		col.getColumn().setWidth(200);
		col.getColumn().setText("Object");
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
			    Span entry = (Span) element;
			    return String.format("%s", entry.getOwner());
			}
		});
		col.getColumn().addSelectionListener(getSelectionAdapter(col.getColumn(), 0));

		col = new TableViewerColumn(table, SWT.NONE);
		col.getColumn().setWidth(200);
		col.getColumn().setText("Segment count");
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				Span entry = (Span) element;
				return String.format("%d", entry.getCount());
			}
		});
		col.getColumn().addSelectionListener(getSelectionAdapter(col.getColumn(), 1));

		col = new TableViewerColumn(table, SWT.NONE);
		col.getColumn().setWidth(200);
		col.getColumn().setText("Self time");
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				Span entry = (Span) element;
				return String.format("%.9f", ((double)entry.getTotal()) / 1000000000);
			}
		});
		col.getColumn().addSelectionListener(getSelectionAdapter(col.getColumn(), 2));

		col = new TableViewerColumn(table, SWT.NONE);
		col.getColumn().setWidth(200);
		col.getColumn().setText("Relative (%)");
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				Span entry = (Span) element;
				return String.format("%.3f", ((double)entry.getTotal()) / spanSum);
			}
		});
		col.getColumn().addSelectionListener(getSelectionAdapter(col.getColumn(), 3));

	}

	  private SelectionAdapter getSelectionAdapter(final TableColumn column, final int index) {
		    SelectionAdapter selectionAdapter = new SelectionAdapter() {
		      @Override
		      public void widgetSelected(SelectionEvent e) {
		        comparator.setColumn(index);
		        int dir = comparator.getDirection();
		        table.getTable().setSortDirection(dir);
		        table.getTable().setSortColumn(column);
		        table.refresh();
		      }
		    };
		    return selectionAdapter;
		  }


	@Override
	public void setFocus() {
		table.getControl().setFocus();
	}

	@Override
	protected void updateDataSafe() {
		System.out.println("CriticalPathView updateData " + registry);
		if (registry == null)
			return;
		SystemModel system = registry.getModel(IModelKeys.SHARED, SystemModel.class);
		ExecGraph graph = registry.getModel(IModelKeys.SHARED, ExecGraph.class);
		if (system == null || graph == null)
			return;
		Task task = system.getTask(currentTid);
		// change input if task has changed
		if (task != null || task != fCurrentTask) {
			fCurrentTask = task;
			computeCriticalPath(graph, task);
		}
	}

	private void computeCriticalPath(ExecGraph graph, Task task) {
		ExecVertex head = graph.getStartVertexOf(task);
		if (!graph.getGraph().containsVertex(head)) {
			System.err.println("WARNING: head vertex is null for task " + task);
			return;
		}
		setSpans(CriticalPathStats.compile(graph, head));
	}

	private void setSpans(HashMap<Object, Span> spans) {
		table.setInput(null);
		if (spans == null) {
			return;
		}
		spanSum = 0;
		for (Span span: spans.values()) {
			spanSum += span.getTotal();
		}
		table.setInput(spans.values());
	}

}
