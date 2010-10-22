/**
 * Created on Oct 21, 2010
 *
 * Copyright 2010 Vuze, Inc.  All rights reserved.
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

package com.aelitis.azureus.ui.swt.views.skin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.download.impl.DownloadManagerAdapter;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerAdapter;
import org.gudy.azureus2.core3.global.GlobalManagerStats;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.menus.MenuManager;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.ui.swt.shells.CoreWaiterSWT;
import org.gudy.azureus2.ui.swt.shells.CoreWaiterSWT.TriggerInThread;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.speedmanager.SpeedManager;
import com.aelitis.azureus.core.torrent.HasBeenOpenedListener;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoManager;
import com.aelitis.azureus.ui.mdi.*;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBarVitalityImageSWT;

/**
 * @author TuxPaper
 * @created Oct 21, 2010
 *
 */
public class SB_Transfers
{

	private static final String ID_VITALITY_ACTIVE = "image.sidebar.vitality.dl";

	private static final String ID_VITALITY_ALERT = "image.sidebar.vitality.alert";

	private static final long DL_VITALITY_REFRESH_RATE = 15000;

	private static final boolean DL_VITALITY_CONSTANT = true;

	public static class stats
	{
		int numSeeding = 0;

		int numDownloading = 0;

		int numComplete = 0;

		int numIncomplete = 0;

		int numErrorComplete = 0;

		String errorInCompleteTooltip;

		int numErrorInComplete = 0;

		String errorCompleteTooltip;

		int numUnOpened = 0;

		int numStoppedAll = 0;

		int numStoppedIncomplete = 0;

		boolean includeLowNoise;
	};

	private static stats statsWithLowNoise = new stats();

	private static stats statsNoLowNoise = new stats();

	private static List<countRefreshListener> listeners = new ArrayList<countRefreshListener>();

	private static boolean first = true;

	static {
		statsNoLowNoise.includeLowNoise = false;
		statsWithLowNoise.includeLowNoise = true;
	}

	public static void setup(final MultipleDocumentInterface mdi) {

		mdi.registerEntry(SideBar.SIDEBAR_SECTION_LIBRARY_DL,
				new MdiEntryCreationListener() {
					public MdiEntry createMDiEntry(String id) {
						return createDownloadingEntry(mdi);
					}
				});

		mdi.registerEntry(SideBar.SIDEBAR_SECTION_LIBRARY_CD,
				new MdiEntryCreationListener() {
					public MdiEntry createMDiEntry(String id) {
						return createSeedingEntry(mdi);
					}
				});

		
		if (first) {
			AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
				public void azureusCoreRunning(AzureusCore core) {
					setupViewTitleWithCore(core);
				}
			});
		}
		PlatformTorrentUtils.addHasBeenOpenedListener(new HasBeenOpenedListener() {
			public void hasBeenOpenedChanged(DownloadManager dm, boolean opened) {
				recountUnopened();
				refreshAllLibraries();
			}
		});
		
		addMenuUnwatched();
	}
	
	private static void addMenuUnwatched() {
		PluginInterface pi = PluginInitializer.getDefaultInterface();
		UIManager uim = pi.getUIManager();
		MenuManager menuManager = uim.getMenuManager();

		MenuItem menuItem = menuManager.addMenuItem("sidebar."
				+ SideBar.SIDEBAR_SECTION_LIBRARY, "v3.activity.button.watchall");
		menuItem.addListener(new MenuItemListener() {
			public void selected(MenuItem menu, Object target) {
				CoreWaiterSWT.waitForCore(TriggerInThread.ANY_THREAD,
						new AzureusCoreRunningListener() {
							public void azureusCoreRunning(AzureusCore core) {
								GlobalManager gm = core.getGlobalManager();
								List<?> downloadManagers = gm.getDownloadManagers();
								for (Iterator<?> iter = downloadManagers.iterator(); iter.hasNext();) {
									DownloadManager dm = (DownloadManager) iter.next();

									if (!PlatformTorrentUtils.getHasBeenOpened(dm)
											&& dm.getAssumedComplete()) {
										PlatformTorrentUtils.setHasBeenOpened(dm, true);
									}
								}
							}
						});
			}
		});
	}



	/**
	 * @param mdi
	 * @return
	 *
	 * @since 4.5.1.1
	 */
	protected static MdiEntry createSeedingEntry(MultipleDocumentInterface mdi) {
		ViewTitleInfo titleInfoSeeding = new ViewTitleInfo() {
			public Object getTitleInfoProperty(int propertyID) {
				if (propertyID == TITLE_INDICATOR_TEXT) {
					return null; //numSeeding + " of " + numComplete;
				}

				if (propertyID == TITLE_INDICATOR_TEXT_TOOLTIP) {
					return "There are " + statsNoLowNoise.numComplete
							+ " complete torrents, " + statsNoLowNoise.numSeeding
							+ " of which are currently seeding";
				}
				return null;
			}
		};

		MdiEntry entry = mdi.createEntryFromSkinRef(SideBar.SIDEBAR_HEADER_TRANSFERS,
				SideBar.SIDEBAR_SECTION_LIBRARY_DL, "library",
				MessageText.getString("sidebar.LibraryDL"), titleInfoSeeding, null, false,
				-1);
		entry.setImageLeftID("image.sidebar.downloading");

		MdiEntryVitalityImage vitalityImage = entry.addVitalityImage(ID_VITALITY_ALERT);
		vitalityImage.setVisible(false);

		entry.setViewTitleInfo(titleInfoSeeding);

		return entry;
	}

	protected static MdiEntry createDownloadingEntry(MultipleDocumentInterface mdi) {
		ViewTitleInfo titleInfoDownloading = new ViewTitleInfo() {
			public Object getTitleInfoProperty(int propertyID) {
				if (propertyID == TITLE_INDICATOR_TEXT) {
					if (statsNoLowNoise.numIncomplete > 0)
						return statsNoLowNoise.numIncomplete + ""; // + " of " + numIncomplete;
				}

				if (propertyID == TITLE_INDICATOR_TEXT_TOOLTIP) {
					return "There are " + statsNoLowNoise.numIncomplete
							+ " incomplete torrents, " + statsNoLowNoise.numDownloading
							+ " of which are currently downloading";
				}

				return null;
			}
		};
		MdiEntry entry = mdi.createEntryFromSkinRef(SideBar.SIDEBAR_HEADER_TRANSFERS,
				SideBar.SIDEBAR_SECTION_LIBRARY_DL, "library",
				MessageText.getString("sidebar.LibraryDL"), titleInfoDownloading, null, false,
				-1);
		entry.setImageLeftID("image.sidebar.downloading");

		MdiEntryVitalityImage vitalityImage = entry.addVitalityImage(ID_VITALITY_ACTIVE);
		vitalityImage.setVisible(false);

		vitalityImage = entry.addVitalityImage(ID_VITALITY_ALERT);
		vitalityImage.setVisible(false);

		if (!DL_VITALITY_CONSTANT) {
			SimpleTimer.addPeriodicEvent("DLVitalityRefresher",
					DL_VITALITY_REFRESH_RATE, new TimerEventPerformer() {
						public void perform(TimerEvent event) {
							MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
							MdiEntry entry = mdi.getEntry(SideBar.SIDEBAR_SECTION_LIBRARY_DL);
							MdiEntryVitalityImage[] vitalityImages = entry.getVitalityImages();
							for (int i = 0; i < vitalityImages.length; i++) {
								MdiEntryVitalityImage vitalityImage = vitalityImages[i];
								if (vitalityImage.getImageID().equals(ID_VITALITY_ACTIVE)) {
									refreshDLSpinner((SideBarVitalityImageSWT) vitalityImage);
								}
							}
						}
					});
		}

		return entry;
	}

	protected static void setupViewTitleWithCore(AzureusCore core) {
		if (!first) {
			return;
		}
		first = false;
		final GlobalManager gm = core.getGlobalManager();
		final DownloadManagerListener dmListener = new DownloadManagerAdapter() {
			public void stateChanged(DownloadManager dm, int state) {
				stateChanged(dm, state, statsNoLowNoise);
				stateChanged(dm, state, statsWithLowNoise);
			}

			public void stateChanged(DownloadManager dm, int state, stats stats) {
				if (!stats.includeLowNoise
						&& PlatformTorrentUtils.isAdvancedViewOnly(dm)) {
					return;
				}

				updateDMCounts(dm);

				boolean complete = dm.getAssumedComplete();
				Boolean wasErrorStateB = (Boolean) dm.getUserData("wasErrorState");
				boolean wasErrorState = wasErrorStateB == null ? false
						: wasErrorStateB.booleanValue();
				boolean isErrorState = state == DownloadManager.STATE_ERROR;
				if (isErrorState != wasErrorState) {
					int rel = isErrorState ? 1 : -1;
					if (complete) {
						stats.numErrorComplete += rel;
					} else {
						stats.numErrorInComplete += rel;
					}
					updateErrorTooltip(stats);
					dm.setUserData("wasErrorState", new Boolean(isErrorState));
				}
				refreshAllLibraries();
			}

			public void completionChanged(DownloadManager dm, boolean completed) {
				completionChanged(dm, completed, statsNoLowNoise);
				completionChanged(dm, completed, statsWithLowNoise);
			}

			public void completionChanged(DownloadManager dm, boolean completed,
					stats stats) {
				if (!stats.includeLowNoise
						&& PlatformTorrentUtils.isAdvancedViewOnly(dm)) {
					return;
				}

				updateDMCounts(dm);
				if (completed) {
					stats.numComplete++;
					stats.numIncomplete--;
					if (dm.getState() == DownloadManager.STATE_ERROR) {
						stats.numErrorComplete++;
						stats.numErrorInComplete--;
					}
					if (dm.getState() == DownloadManager.STATE_STOPPED) {
						statsNoLowNoise.numStoppedIncomplete--;
					}

				} else {
					stats.numComplete--;
					stats.numIncomplete++;

					if (dm.getState() == DownloadManager.STATE_ERROR) {
						stats.numErrorComplete--;
						stats.numErrorInComplete++;
					}
					if (dm.getState() == DownloadManager.STATE_STOPPED) {
						statsNoLowNoise.numStoppedIncomplete++;
					}
				}
				recountUnopened();
				updateErrorTooltip(stats);
				refreshAllLibraries();
			}

			protected void updateErrorTooltip(stats stats) {
				if (stats.numErrorComplete < 0) {
					stats.numErrorComplete = 0;
				}
				if (stats.numErrorInComplete < 0) {
					stats.numErrorInComplete = 0;
				}

				if (stats.numErrorComplete > 0 || stats.numErrorInComplete > 0) {

					String comp_error = null;
					String incomp_error = null;

					List downloads = gm.getDownloadManagers();

					for (int i = 0; i < downloads.size(); i++) {

						DownloadManager download = (DownloadManager) downloads.get(i);

						if (download.getState() == DownloadManager.STATE_ERROR) {

							if (download.getAssumedComplete()) {

								if (comp_error == null) {

									comp_error = download.getDisplayName() + ": "
											+ download.getErrorDetails();
								} else {

									comp_error += "...";
								}
							} else {
								if (incomp_error == null) {

									incomp_error = download.getDisplayName() + ": "
											+ download.getErrorDetails();
								} else {

									incomp_error += "...";
								}
							}
						}
					}

					stats.errorCompleteTooltip = comp_error;
					stats.errorInCompleteTooltip = incomp_error;
				}
			}
		};

		gm.addListener(new GlobalManagerAdapter() {
			public void downloadManagerRemoved(DownloadManager dm) {
				downloadManagerRemoved(dm, statsNoLowNoise);
				downloadManagerRemoved(dm, statsWithLowNoise);
			}

			public void downloadManagerRemoved(DownloadManager dm, stats stats) {
				if (!stats.includeLowNoise
						&& PlatformTorrentUtils.isAdvancedViewOnly(dm)) {
					return;
				}

				recountUnopened();
				if (dm.getAssumedComplete()) {
					stats.numComplete--;
					Boolean wasDownloadingB = (Boolean) dm.getUserData("wasDownloading");
					if (wasDownloadingB != null && wasDownloadingB.booleanValue()) {
						stats.numDownloading--;
					}
				} else {
					stats.numIncomplete--;
					Boolean wasSeedingB = (Boolean) dm.getUserData("wasSeeding");
					if (wasSeedingB != null && wasSeedingB.booleanValue()) {
						stats.numSeeding--;
					}
				}

				Boolean wasStoppedB = (Boolean) dm.getUserData("wasStopped");
				boolean wasStopped = wasStoppedB == null ? false
						: wasStoppedB.booleanValue();
				if (wasStopped) {
					stats.numStoppedAll--;
					if (!dm.getAssumedComplete()) {
						stats.numStoppedIncomplete--;
					}
				}

				refreshAllLibraries();
				dm.removeListener(dmListener);
			}

			public void downloadManagerAdded(DownloadManager dm) {
				dm.addListener(dmListener, false);
				recountUnopened();

				downloadManagerAdded(dm, statsNoLowNoise);
				downloadManagerAdded(dm, statsWithLowNoise);
				refreshAllLibraries();
			}

			public void downloadManagerAdded(DownloadManager dm, stats stats) {
				if (!stats.includeLowNoise
						&& PlatformTorrentUtils.isAdvancedViewOnly(dm)) {
					return;
				}

				if (dm.getAssumedComplete()) {
					stats.numComplete++;
					if (dm.getState() == DownloadManager.STATE_SEEDING) {
						stats.numSeeding++;
					}
				} else {
					stats.numIncomplete++;
					if (dm.getState() == DownloadManager.STATE_DOWNLOADING) {
						dm.setUserData("wasDownloading", Boolean.TRUE);
						stats.numDownloading++;
					} else {
						dm.setUserData("wasDownloading", Boolean.FALSE);
					}
				}
			}
		}, false);
		List downloadManagers = gm.getDownloadManagers();
		for (Iterator iter = downloadManagers.iterator(); iter.hasNext();) {
			DownloadManager dm = (DownloadManager) iter.next();
			boolean lowNoise = PlatformTorrentUtils.isAdvancedViewOnly(dm);
			dm.addListener(dmListener, false);
			int state = dm.getState();
			if (state == DownloadManager.STATE_STOPPED) {
				dm.setUserData("wasStopped", Boolean.TRUE);
				statsWithLowNoise.numStoppedAll++;
				if (!dm.getAssumedComplete()) {
					statsWithLowNoise.numStoppedIncomplete++;
				}
				if (!lowNoise) {
					statsNoLowNoise.numStoppedAll++;
					if (!dm.getAssumedComplete()) {
						statsNoLowNoise.numStoppedIncomplete++;
					}
				}
			} else {
				dm.setUserData("wasStopped", Boolean.FALSE);
			}
			if (dm.getAssumedComplete()) {
				statsWithLowNoise.numComplete++;
				if (!lowNoise) {
					statsNoLowNoise.numComplete++;
				}
				if (state == DownloadManager.STATE_SEEDING) {
					dm.setUserData("wasSeeding", Boolean.TRUE);
					statsWithLowNoise.numSeeding++;
					if (!lowNoise) {
						statsNoLowNoise.numSeeding++;
					}
				} else {
					dm.setUserData("wasSeeding", Boolean.FALSE);
				}
			} else {
				statsWithLowNoise.numIncomplete++;
				if (!lowNoise) {
					statsNoLowNoise.numIncomplete++;
				}
				if (state == DownloadManager.STATE_DOWNLOADING) {
					statsWithLowNoise.numDownloading++;
					if (!lowNoise) {
						statsNoLowNoise.numDownloading++;
					}
				}
			}
		}

		recountUnopened();
		refreshAllLibraries();
	}

	protected static void updateDMCounts(DownloadManager dm) {
		boolean isSeeding;
		boolean isDownloading;
		boolean isStopped;

		Boolean wasSeedingB = (Boolean) dm.getUserData("wasSeeding");
		boolean wasSeeding = wasSeedingB == null ? false
				: wasSeedingB.booleanValue();
		Boolean wasDownloadingB = (Boolean) dm.getUserData("wasDownloading");
		boolean wasDownloading = wasDownloadingB == null ? false
				: wasDownloadingB.booleanValue();
		Boolean wasStoppedB = (Boolean) dm.getUserData("wasStopped");
		boolean wasStopped = wasStoppedB == null ? false
				: wasStoppedB.booleanValue();

		if (dm.getAssumedComplete()) {
			isSeeding = dm.getState() == DownloadManager.STATE_SEEDING;
			isDownloading = false;
		} else {
			isDownloading = dm.getState() == DownloadManager.STATE_DOWNLOADING;
			isSeeding = false;
		}

		isStopped = dm.getState() == DownloadManager.STATE_STOPPED;
		boolean lowNoise = PlatformTorrentUtils.isAdvancedViewOnly(dm);

		if (isDownloading != wasDownloading) {
			if (isDownloading) {
				statsWithLowNoise.numDownloading++;
				if (!lowNoise) {
					statsNoLowNoise.numDownloading++;
				}
			} else {
				statsWithLowNoise.numDownloading--;
				if (!lowNoise) {
					statsNoLowNoise.numDownloading--;
				}
			}
			dm.setUserData("wasDownloading", new Boolean(isDownloading));
		}

		if (isSeeding != wasSeeding) {
			if (isSeeding) {
				statsWithLowNoise.numSeeding++;
				if (!lowNoise) {
					statsNoLowNoise.numSeeding++;
				}
			} else {
				statsWithLowNoise.numSeeding--;
				if (!lowNoise) {
					statsNoLowNoise.numSeeding--;
				}
			}
			dm.setUserData("wasSeeding", new Boolean(isSeeding));
		}

		if (isStopped != wasStopped) {
			if (isStopped) {
				statsWithLowNoise.numStoppedAll++;
				if (!dm.getAssumedComplete()) {
					statsWithLowNoise.numStoppedIncomplete++;
				}
				if (!lowNoise) {
					statsNoLowNoise.numStoppedAll++;
					if (!dm.getAssumedComplete()) {
						statsNoLowNoise.numStoppedIncomplete++;
					}
				}
			} else {
				statsWithLowNoise.numStoppedAll--;
				if (!dm.getAssumedComplete()) {
					statsWithLowNoise.numStoppedIncomplete--;
				}
				if (!lowNoise) {
					statsNoLowNoise.numStoppedAll--;
					if (!dm.getAssumedComplete()) {
						statsNoLowNoise.numStoppedIncomplete--;
					}
				}
			}
			dm.setUserData("wasStopped", new Boolean(isStopped));
		}

	}

	private static void recountUnopened() {
		if (!AzureusCoreFactory.isCoreRunning()) {
			return;
		}
		GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
		List dms = gm.getDownloadManagers();
		statsNoLowNoise.numUnOpened = 0;
		for (Iterator iter = dms.iterator(); iter.hasNext();) {
			DownloadManager dm = (DownloadManager) iter.next();
			if (!PlatformTorrentUtils.getHasBeenOpened(dm) && dm.getAssumedComplete()) {
				statsNoLowNoise.numUnOpened++;
			}
		}
		statsWithLowNoise.numUnOpened = statsNoLowNoise.numUnOpened;
	}

	protected static void addCountRefreshListener(countRefreshListener l) {
		l.countRefreshed(statsWithLowNoise, statsNoLowNoise);
		listeners.add(l);
	}

	public static void triggerCountRefreshListeners() {
		for (countRefreshListener l : listeners) {
			l.countRefreshed(statsWithLowNoise, statsNoLowNoise);
		}
	}

	/**
	 * 
	 *
	 * @since 3.1.1.1
	 */
	protected static void refreshAllLibraries() {
		for (countRefreshListener l : listeners) {
			l.countRefreshed(statsWithLowNoise, statsNoLowNoise);
		}
		MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
		if (mdi == null) {
			return;
		}

		if (statsNoLowNoise.numIncomplete > 0) {
			MdiEntry entry = mdi.getEntry(SideBar.SIDEBAR_SECTION_LIBRARY_DL);
			if (entry == null) {
				mdi.loadEntryByID(SideBar.SIDEBAR_SECTION_LIBRARY_DL, false);
			}
		} else {
			MdiEntry entry = mdi.getEntry(SideBar.SIDEBAR_SECTION_LIBRARY_DL);
			if (entry != null) {
				entry.close(true);
			}
		}
		MdiEntry entry = mdi.getEntry(SideBar.SIDEBAR_SECTION_LIBRARY_DL);
		if (entry != null) {
			MdiEntryVitalityImage[] vitalityImages = entry.getVitalityImages();
			for (int i = 0; i < vitalityImages.length; i++) {
				MdiEntryVitalityImage vitalityImage = vitalityImages[i];
				String imageID = vitalityImage.getImageID();
				if (imageID == null) {
					continue;
				}
				if (imageID.equals(ID_VITALITY_ACTIVE)) {
					vitalityImage.setVisible(statsNoLowNoise.numDownloading > 0);

					refreshDLSpinner((SideBarVitalityImageSWT) vitalityImage);

				} else if (imageID.equals(ID_VITALITY_ALERT)) {
					vitalityImage.setVisible(statsNoLowNoise.numErrorInComplete > 0);
					if (statsNoLowNoise.numErrorInComplete > 0) {
						vitalityImage.setToolTip(statsNoLowNoise.errorInCompleteTooltip);
					}
				}
			}
			ViewTitleInfoManager.refreshTitleInfo(entry.getViewTitleInfo());
		}

		entry = mdi.getEntry(SideBar.SIDEBAR_SECTION_LIBRARY_CD);
		if (entry != null) {
			MdiEntryVitalityImage[] vitalityImages = entry.getVitalityImages();
			for (int i = 0; i < vitalityImages.length; i++) {
				MdiEntryVitalityImage vitalityImage = vitalityImages[i];
				String imageID = vitalityImage.getImageID();
				if (imageID == null) {
					continue;
				}
				if (imageID.equals(ID_VITALITY_ALERT)) {
					vitalityImage.setVisible(statsNoLowNoise.numErrorComplete > 0);
					if (statsNoLowNoise.numErrorComplete > 0) {
						vitalityImage.setToolTip(statsNoLowNoise.errorCompleteTooltip);
					}
				}
			}
		}

	}

	public static void refreshDLSpinner(SideBarVitalityImageSWT vitalityImage) {
		if (DL_VITALITY_CONSTANT) {
			return;
		}

		if (vitalityImage.getImageID().equals(ID_VITALITY_ACTIVE)) {
			if (!vitalityImage.isVisible()) {
				return;
			}
			SpeedManager sm = AzureusCoreFactory.getSingleton().getSpeedManager();
			if (sm != null) {
				GlobalManagerStats stats = AzureusCoreFactory.getSingleton().getGlobalManager().getStats();

				int delay = 100;
				int limit = NetworkManager.getMaxDownloadRateBPS();
				if (limit <= 0) {
					limit = sm.getEstimatedDownloadCapacityBytesPerSec().getBytesPerSec();
				}

				// smoothing
				int current = stats.getDataReceiveRate() / 10;
				limit /= 10;

				if (limit > 0) {
					if (current > limit) {
						delay = 25;
					} else {
						// 40 incrememnts of 5.. max 200
						current += 39;
						delay = (40 - (current * 40 / limit)) * 5;
						if (delay < 35) {
							delay = 35;
						} else if (delay > 200) {
							delay = 200;
						}
					}
					if (vitalityImage instanceof SideBarVitalityImageSWT) {
						SideBarVitalityImageSWT viSWT = (SideBarVitalityImageSWT) vitalityImage;
						if (viSWT.getDelayTime() != delay) {
							viSWT.setDelayTime(delay);
							//System.out.println("new delay: " + delay + "; via " + current + " / " + limit);
						}
					}
				}
			}
		}
	}

	public static String getTableIdFromFilterMode(int torrentFilterMode,
			boolean big) {
		if (torrentFilterMode == SBC_LibraryView.TORRENTS_COMPLETE) {
			return big ? TableManager.TABLE_MYTORRENTS_COMPLETE_BIG
					: TableManager.TABLE_MYTORRENTS_COMPLETE;
		} else if (torrentFilterMode == SBC_LibraryView.TORRENTS_INCOMPLETE) {
			return big ? TableManager.TABLE_MYTORRENTS_INCOMPLETE_BIG
					: TableManager.TABLE_MYTORRENTS_INCOMPLETE;
		} else if (torrentFilterMode == SBC_LibraryView.TORRENTS_ALL) {
			return TableManager.TABLE_MYTORRENTS_ALL_BIG;
		}
		return null;
	}

	protected static interface countRefreshListener
	{
		public void countRefreshed(stats statsWithLowNoise, stats statsNoLowNoise);
	}
}
