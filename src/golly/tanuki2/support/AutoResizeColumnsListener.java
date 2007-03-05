package golly.tanuki2.support;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

/**
 * @author Golly
 * @since 04/03/2007
 */
public class AutoResizeColumnsListener implements Listener {

	// =============================================================================================== //
	// = Public
	// =============================================================================================== //

	public boolean enabled= true;
	public boolean disableRedraw= true;
	private final WidgetWithColumns wwc;
	private final Composite c;

	public AutoResizeColumnsListener(WidgetWithColumns wwc) {
		this.wwc= wwc;
		this.c= wwc.getWidget();
	}

	public void handleEvent(Event event) {
		if (enabled && event != null && event.widget.equals(c) && c.isVisible())
			resizeColumns();
	}

	public void resizeColumns() {
		final boolean wasEnabled= this.enabled;
		this.enabled= false;

		final int columnCount= wwc.getColumnCount();
		final int availableWidth= c.getSize().x - (c.getBorderWidth() << 1) - (wwc.isVerticalBarVisible() ? c.getVerticalBar().getSize().x : 0);

		if (availableWidth > 8 && columnCount != 0) {

			if (disableRedraw)
				c.setRedraw(false);

			// Calculate total column width (packed)
			int i= columnCount;
			int currentTotalWidth= 0;
			final int[] widths= new int[i];
			if (wwc.isEmpty())
				while (i-- > 0)
					currentTotalWidth+= (widths[i]= 0);
			else {
				wwc.packAllColumns();
				while (i-- > 0)
					currentTotalWidth+= (widths[i]= wwc.getColumnWidth(i));
			}

			// Resize columns if columns dont fill available space
			if (currentTotalWidth < availableWidth) {
				i= columnCount;
				int remainingExtra= availableWidth - currentTotalWidth;
				while (i-- > 0) {
					final int x= (int) (((double) widths[i]) / ((double) currentTotalWidth) * ((double) remainingExtra));
					wwc.setColumnWidth(i, widths[i] + x);
					currentTotalWidth-= widths[i];
					remainingExtra-= x;
				}
			}

			if (disableRedraw)
				c.setRedraw(true);
		}
		this.enabled= wasEnabled;
	}

	// =============================================================================================== //
	// = Internal
	// =============================================================================================== //

	static interface WidgetWithColumns {
		public abstract Composite getWidget();

		public abstract int getColumnCount();

		public abstract int getColumnWidth(int column);

		public abstract void setColumnWidth(int column, int width);

		public abstract void packAllColumns();

		public abstract boolean isEmpty();

		public abstract boolean isVerticalBarVisible();
	}

	static final class WidgetWithColumns_Table implements WidgetWithColumns {
		private final Table w;

		public WidgetWithColumns_Table(Table w) {
			this.w= w;
		}

		public int getColumnCount() {
			return w.getColumnCount();
		}

		public int getColumnWidth(int column) {
			return w.getColumn(column).getWidth();
		}

		public void packAllColumns() {
			for (TableColumn c : w.getColumns())
				c.pack();
		}

		public void setColumnWidth(int column, int width) {
			w.getColumn(column).setWidth(width);
		}

		public Composite getWidget() {
			return w;
		}

		public boolean isEmpty() {
			return w.getItemCount() == 0;
		}

		public boolean isVerticalBarVisible() {
			return w.computeSize(SWT.DEFAULT, SWT.DEFAULT).y > w.getClientArea().height + w.getHeaderHeight();
		}
	}

	static final class WidgetWithColumns_Tree implements WidgetWithColumns {
		private final Tree w;

		public WidgetWithColumns_Tree(Tree w) {
			this.w= w;
		}

		public int getColumnCount() {
			return w.getColumnCount();
		}

		public int getColumnWidth(int column) {
			return w.getColumn(column).getWidth();
		}

		public void packAllColumns() {
			for (TreeColumn c : w.getColumns())
				c.pack();
		}

		public void setColumnWidth(int column, int width) {
			w.getColumn(column).setWidth(width);
		}

		public Composite getWidget() {
			return w;
		}

		public boolean isEmpty() {
			return w.getItemCount() == 0;
		}

		public boolean isVerticalBarVisible() {
			return w.getVerticalBar().isVisible();
		}
	}
}
