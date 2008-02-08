/**
 * Created on Feb 2, 2008 
 *
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */

package com.aelitis.azureus.ui.swt.columns.vuzeactivity;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTGraphic;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTGraphicImpl;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.impl.TableCellImpl;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.core.messenger.config.PlatformConfigMessenger;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.common.table.TableCellCore;
import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.columns.torrent.ColumnMediaThumb;
import com.aelitis.azureus.ui.swt.columns.torrent.ColumnRate;
import com.aelitis.azureus.ui.swt.utils.ColorCache;
import com.aelitis.azureus.ui.swt.utils.ImageLoader;
import com.aelitis.azureus.ui.swt.utils.ImageLoaderFactory;
import com.aelitis.azureus.ui.swt.views.list.*;
import com.aelitis.azureus.util.VuzeActivitiesEntry;

import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * @author TuxPaper
 * @created Feb 2, 2008
 *
 */
public class ColumnVuzeActivity
	extends CoreTableColumn
	implements TableCellRefreshListener, TableCellDisposeListener,
	TableCellAddedListener, TableCellMouseMoveListener,
	TableCellVisibilityListener
{
	private static final int EVENT_INDENT = 21;

	private static final int MARGIN_WIDTH = 8 - ListView.COLUMN_MARGIN_WIDTH;

	private static final int MARGIN_HEIGHT = 7 - ListView.ROW_MARGIN_HEIGHT;

	public static String COLUMN_ID = "name";

	public static Font headerFont = null;

	/** Default Constructor */
	public ColumnVuzeActivity(String sTableID) {
		super(COLUMN_ID, POSITION_LAST, 250, sTableID);
		setObfustication(true);
		setType(TableColumn.TYPE_GRAPHIC);
		setRefreshInterval(INTERVAL_LIVE);
	}

	public void cellAdded(TableCell cell) {
		cell.setMarginWidth(MARGIN_WIDTH);
		cell.setMarginHeight(0);

		Object ds = cell.getDataSource();
		if (!(ds instanceof VuzeActivitiesEntry)) {
			return;
		}
		VuzeActivitiesEntry entry = (VuzeActivitiesEntry) ds;
		entry.tableColumn = (TableColumnCore) cell.getTableColumn();
		//((TableCellCore) cell).getTableRowCore().setHeight(
		//		(int) (Math.random() * 30) + 20);
	}

	public void refresh(TableCell cell) {
		refresh(cell, false);
	}

	public void refresh(TableCell cell, boolean force) {
		TableCellImpl thumbCell = getThumbCell(cell);
		TableCellImpl ratingCell = getRatingCell(cell);
		if (!force) {
			if (thumbCell != null
					&& (!thumbCell.isValid() || thumbCell.getVisuallyChangedSinceRefresh())) {
				force = true;
			}
			if (ratingCell != null
					&& (!ratingCell.isValid() || ratingCell.getVisuallyChangedSinceRefresh())) {
				force = true;
			}
		}

		Object ds = cell.getDataSource();
		if (!(ds instanceof VuzeActivitiesEntry)) {
			return;
		}
		VuzeActivitiesEntry entry = (VuzeActivitiesEntry) ds;

		int width = cell.getWidth();
		int height = cell.getHeight();
		if (width <= 0 || height <= 0) {
			cell.setSortValue(entry.getTimestamp());
			return;
		}

		Image image = null;

		Graphic graphic = cell.getGraphic();
		if (graphic instanceof UISWTGraphic) {
			image = ((UISWTGraphic) graphic).getImage();
		}

		Rectangle imgBounds = image == null ? null : image.getBounds();
		force |= imgBounds == null
				|| !imgBounds.equals(new Rectangle(0, 0, width, height));

		if (!cell.setSortValue(entry) && cell.isValid() && !force) {
			return;
		}
		((TableColumnCore) cell.getTableColumn()).setSortValueLive(false);

		if (force) {
			graphic = cell.getBackgroundGraphic();
			if (graphic instanceof UISWTGraphic) {
				image = ((UISWTGraphic) graphic).getImage();
				if (image == null) {
					return;
				}
			}
			imgBounds = image.getBounds();
		}

		Image imgIcon;
		if (entry.icon == null) {
			imgIcon = null;
		} else {
			ImageLoader imageLoader = ImageLoaderFactory.getInstance();
			imgIcon = imageLoader.getImage(entry.icon);
		}

		int x = entry.type == 0 ? 0 : EVENT_INDENT;
		int y = 0;
		boolean isMouseOver = ((TableCellCore) cell).isMouseOver();

		int style = SWT.WRAP;
		Device device = Display.getDefault();
		GCStringPrinter stringPrinter;
		GC gcQuery = new GC(device);
		try {
			gcQuery.setAdvanced(true);
			if (entry.type == 0) {
				if (headerFont == null) {
					gcQuery.setTextAntialias(SWT.ON);
					// no sync required, SWT is on single thread
					FontData[] fontData = gcQuery.getFont().getFontData();
					//fontData[0].setStyle(SWT.BOLD);
					// we can do a few more pixels because we have no text hanging below baseline
					Utils.getFontHeightFromPX(device, fontData, gcQuery, 20);
					headerFont = new Font(device, fontData);
				}
				gcQuery.setFont(headerFont);
			}
			Rectangle potentialArea = new Rectangle(x, 2, width - x - 2, 10000);
			stringPrinter = new GCStringPrinter(gcQuery, entry.text, potentialArea,
					0, style);
			stringPrinter.calculateMetrics();
			Point size = stringPrinter.getCalculatedSize();
			//System.out.println(size + ";" + entry.text);

			if (stringPrinter.getUrlHitArea() != null) {
				if (isMouseOver) {
					stringPrinter.setUrlColor(ColorCache.getColor(device, "#6060f0"));
				} else {
					stringPrinter.setUrlColor(ColorCache.getColor(device, "#8080ff"));
				}
			}

			//boolean focused = ((ListRow) cell.getTableRow()).isFocused();
			//if (focused) size.y += 40;
			if (entry.type == 0) {
				height = 35 - 4;
				style |= SWT.BOTTOM;
			} else {
				height = size.y; // + 8; // 4 for border, 2 because textextent sucks
				height += (MARGIN_HEIGHT * 2);
				y = MARGIN_HEIGHT + 1;
			}

			if (entry.dm != null || entry.imageBytes != null) {
				height += 60;
			}
			style |= SWT.TOP;

			//
			if (height < 24) {
				height = 24;
				y = (height - MARGIN_HEIGHT - size.y) / 2;
			}
			boolean heightChanged = ((TableCellCore) cell).getTableRowCore().setHeight(
					height);

			if (heightChanged) {
				disposeExisting(cell, null);
				graphic = cell.getBackgroundGraphic();
				if (graphic instanceof UISWTGraphic) {
					image = ((UISWTGraphic) graphic).getImage();
					if (image == null) {
						return;
					}
				}
				imgBounds = image.getBounds();
			}

			//System.out.println("height=" + height + ";b=" + imgBounds);
		} finally {
			gcQuery.dispose();
		}

		GC gc = new GC(image);
		try {
			Color black = ColorCache.getColor(device, "#000000");
			Color colorLine = ColorCache.getColor(device, "#232323");
			gc.setAdvanced(true);
			gc.setBackground(ColorCache.getColor(device, cell.getBackground()));
			gc.setForeground(ColorCache.getColor(device, cell.getForeground()));

			if (entry.type == 0) {
				gc.setTextAntialias(SWT.ON);
				gc.setFont(headerFont);
				gc.setForeground(ColorCache.getColor(device, "#707070"));

				((ListRow) cell.getTableRow()).setBackgroundColor(black);
				gc.setBackground(black);
				gc.fillRectangle(imgBounds);
				height = height - 5;
			} else {
				TableRow row = cell.getTableRow();
				Color color = cell.getTableRow().isSelected() ? colorLine : black;
				((ListRow) row).setBackgroundColor(color);
				gc.setBackground(color);

				gc.fillRectangle(imgBounds);
				if (imgIcon != null) {
					Rectangle iconBounds = imgIcon.getBounds();
					gc.drawImage(imgIcon, iconBounds.x, iconBounds.y, iconBounds.width,
							iconBounds.height, 0, MARGIN_HEIGHT, 16, 16);
				}
			}

			stringPrinter.printString(gc, new Rectangle(x, y, width - x - 2, height - y),
					style);
			entry.urlHitArea = stringPrinter.getUrlHitArea();
			entry.url = stringPrinter.getUrl();

			if (entry.dm != null || entry.imageBytes != null) {
				Rectangle dmThumbRect = getDMImageRect(height);
				if (thumbCell == null) {
					ListCell listCell = new ListCellGraphic(null, SWT.LEFT, dmThumbRect);

					thumbCell = new TableCellImpl((TableRowCore) cell.getTableRow(),
							new ColumnMediaThumb(cell.getTableID()), 0, listCell);
					listCell.setTableCell(thumbCell);

					setThumbCell(cell, thumbCell);
				}

				((ListCell) thumbCell.getBufferedTableItem()).setBackground(gc.getBackground());
				((ListCell) thumbCell.getBufferedTableItem()).setBounds(dmThumbRect);
				thumbCell.invalidate();
				thumbCell.refresh(true);
				thumbCell.doPaint(gc);

				if (entry.dm != null
						&& PlatformTorrentUtils.isContent(entry.dm.getTorrent(), true)) {
					Rectangle dmRatingRect = getDMRatingRect(width, height);
					if (ratingCell == null) {
						ListCell listCell = new ListCellGraphic(null, SWT.LEFT,
								dmRatingRect);

						ratingCell = new TableCellImpl((TableRowCore) cell.getTableRow(),
								new ColumnRate(cell.getTableID(), true), 0, listCell);
						listCell.setTableCell(ratingCell);

						setRatingCell(cell, ratingCell);
					}

					((ListCell) ratingCell.getBufferedTableItem()).setBackground(gc.getBackground());
					((ListCell) ratingCell.getBufferedTableItem()).setBounds(dmRatingRect);
					ratingCell.invalidate();
					ratingCell.refresh(true);
					ratingCell.doPaint(gc);
				}
			}

			if (entry.type > 0) {
				gc.setForeground(colorLine);
				gc.drawLine(EVENT_INDENT, imgBounds.height - 1, imgBounds.width - 1,
						imgBounds.height - 1);
			}

		} finally {
			gc.dispose();
		}

		disposeExisting(cell, image);

		cell.setGraphic(new UISWTGraphicImpl(image));
	}

	private void disposeExisting(TableCell cell, Image exceptIfThisImage) {
		Graphic oldGraphic = cell.getGraphic();
		//log(cell, oldGraphic);
		if (oldGraphic instanceof UISWTGraphic) {
			Image oldImage = ((UISWTGraphic) oldGraphic).getImage();
			if (oldImage != null && !oldImage.isDisposed()
					&& oldImage != exceptIfThisImage) {
				//log(cell, "dispose");
				cell.setGraphic(null);
				oldImage.dispose();
			}
		}
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellDisposeListener#dispose(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void dispose(TableCell cell) {
		disposeExisting(cell, null);
		TableCellImpl thumbCell = getThumbCell(cell);
		if (thumbCell != null) {
			thumbCell.dispose();
			setThumbCell(cell, null);
		}
	}

	private void setThumbCell(TableCell cell, TableCellImpl thumbCell) {
		TableRow tableRow = cell.getTableRow();
		if (tableRow instanceof TableRowCore) {
			TableRowCore tableRowCore = (TableRowCore) tableRow;
			tableRowCore.setData("ThumbCell", thumbCell);
		}
	}

	private TableCellImpl getThumbCell(TableCell cell) {
		TableRow tableRow = cell.getTableRow();
		if (tableRow instanceof TableRowCore) {
			TableRowCore tableRowCore = (TableRowCore) tableRow;
			return (TableCellImpl) tableRowCore.getData("ThumbCell");
		}
		return null;
	}

	private void setRatingCell(TableCell cell, TableCellImpl ratingCell) {
		TableRow tableRow = cell.getTableRow();
		if (tableRow instanceof TableRowCore) {
			TableRowCore tableRowCore = (TableRowCore) tableRow;
			tableRowCore.setData("RatingCell", ratingCell);
		}
	}

	private TableCellImpl getRatingCell(TableCell cell) {
		TableRow tableRow = cell.getTableRow();
		if (tableRow instanceof TableRowCore) {
			TableRowCore tableRowCore = (TableRowCore) tableRow;
			return (TableCellImpl) tableRowCore.getData("RatingCell");
		}
		return null;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellMouseListener#cellMouseTrigger(org.gudy.azureus2.plugins.ui.tables.TableCellMouseEvent)
	public void cellMouseTrigger(TableCellMouseEvent event) {
		TableCellImpl thumbCell = getThumbCell(event.cell);
		TableCellImpl ratingCell = getRatingCell(event.cell);
		if (thumbCell != null || ratingCell != null) {
			Rectangle dmThumbRect = getDMImageRect(event.cell.getHeight());
			Rectangle dmRatingRect = getDMRatingRect(event.cell.getWidth(),
					event.cell.getHeight());

			TableCellMouseEvent subCellEvent = new TableCellMouseEvent();
			subCellEvent.button = event.button;
			subCellEvent.data = event.data;
			subCellEvent.eventType = event.eventType;
			subCellEvent.row = event.row;

			boolean ok;
			ok = thumbCell != null
					&& (event.eventType == TableRowMouseEvent.EVENT_MOUSEENTER
							|| event.eventType == TableRowMouseEvent.EVENT_MOUSEEXIT || dmThumbRect.contains(
							event.x, event.y));
			if (ok) {
				if (thumbCell != null
						&& (thumbCell.getTableColumn() instanceof TableCellMouseListener)) {
					subCellEvent.x = event.x - dmThumbRect.x;
					subCellEvent.y = event.y - dmThumbRect.y;
					subCellEvent.cell = thumbCell;

					TableColumn tc = thumbCell.getTableColumn();
					if (tc instanceof TableColumnCore) {
						((TableColumnCore) tc).invokeCellMouseListeners(subCellEvent);
					}
					if (thumbCell instanceof TableCellCore) {
						((TableCellCore) thumbCell).invokeMouseListeners(subCellEvent);
					}
					event.skipCoreFunctionality |= subCellEvent.skipCoreFunctionality;
				}
			}

			ok = ratingCell != null
					&& (event.eventType == TableRowMouseEvent.EVENT_MOUSEENTER
							|| event.eventType == TableRowMouseEvent.EVENT_MOUSEEXIT || dmRatingRect.contains(
							event.x, event.y));
			if (ok) {
				subCellEvent.cell = ratingCell;
				subCellEvent.x = event.x - dmRatingRect.x;
				subCellEvent.y = event.y - dmRatingRect.y;

				TableColumn tc = ratingCell.getTableColumn();
				if (tc instanceof TableColumnCore) {
					((TableColumnCore) tc).invokeCellMouseListeners(subCellEvent);
				}
				if (ratingCell instanceof TableCellCore) {
					((TableCellCore) ratingCell).invokeMouseListeners(subCellEvent);
				}
				event.skipCoreFunctionality |= subCellEvent.skipCoreFunctionality;
			}

			if (thumbCell != null) {
				subCellEvent.cell = thumbCell;
				subCellEvent.x = event.x - dmThumbRect.x;
				subCellEvent.y = event.y - dmThumbRect.y;

				if (thumbCell instanceof TableCellCore) {
					((TableCellCore) thumbCell).getTableRowCore().invokeMouseListeners(
							subCellEvent);
				}
				event.skipCoreFunctionality |= subCellEvent.skipCoreFunctionality;
			}
			if (ratingCell != null) {
				subCellEvent.cell = ratingCell;
				subCellEvent.x = event.x - dmRatingRect.x;
				subCellEvent.y = event.y - dmRatingRect.y;

				if (ratingCell instanceof TableCellCore) {
					((TableCellCore) thumbCell).getTableRowCore().invokeMouseListeners(
							subCellEvent);
				}
				event.skipCoreFunctionality |= subCellEvent.skipCoreFunctionality;
			}
		}

		Comparable sortValue = event.cell.getSortValue();
		if (sortValue instanceof VuzeActivitiesEntry) {
			VuzeActivitiesEntry entry = (VuzeActivitiesEntry) sortValue;
			if (entry.urlHitArea instanceof Rectangle) {
				Rectangle urlHitArea = (Rectangle) entry.urlHitArea;
				//((ListView)((ListRow)event.cell.getTableRow()).getView()).getTableCellCursorOffset()
				//System.out.println(entry.urlHitArea);
				if (urlHitArea.contains(event.x, event.y)) {
					if (event.eventType == TableCellMouseEvent.EVENT_MOUSEUP) {
						if (PlatformConfigMessenger.isURLBlocked(entry.url)) {
							Utils.launch(entry.url);
						} else {
							UIFunctionsSWT uif = UIFunctionsManagerSWT.getUIFunctionsSWT();
							if (uif != null) {
								uif.viewURL(entry.url, "browse", 0, 0, false, false);
								return;
							}
						}
					}

					((TableCellSWT) event.cell).setCursorID(SWT.CURSOR_HAND);
				} else {
					((TableCellSWT) event.cell).setCursorID(SWT.CURSOR_ARROW);
				}
			}
		}

		refresh(event.cell, true);
	}

	private Rectangle getDMImageRect(int cellHeight) {
		return new Rectangle(EVENT_INDENT + 4, cellHeight - 52 - MARGIN_HEIGHT, 82,
				52);
	}

	private Rectangle getDMRatingRect(int cellWidth, int cellHeight) {
		return new Rectangle(cellWidth - ColumnRate.COLUMN_WIDTH - 10, cellHeight
				- 42 - MARGIN_HEIGHT, ColumnRate.COLUMN_WIDTH, 38);
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellVisibilityListener#cellVisibilityChanged(org.gudy.azureus2.plugins.ui.tables.TableCell, int)
	public void cellVisibilityChanged(TableCell cell, int visibility) {
		TableCellImpl thumbCell = getThumbCell(cell);
		if (thumbCell != null) {
			thumbCell.invokeVisibilityListeners(visibility, true);
		}
		TableCellImpl ratingCell = getRatingCell(cell);
		if (ratingCell != null) {
			ratingCell.invokeVisibilityListeners(visibility, true);
		}
	}
}
