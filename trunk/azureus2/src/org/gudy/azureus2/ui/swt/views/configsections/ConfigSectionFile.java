/*
 * File    : ConfigPanelFile.java
 * Created : 11 mar. 2004
 * By      : TuxPaper
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.ui.swt.views.configsections;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.DirectoryDialog;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.ui.config.ConfigSectionSWT;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.Messages;

public class ConfigSectionFile implements ConfigSectionSWT {
  public String configSectionGetParentSection() {
    return ConfigSection.SECTION_ROOT;
  }

	public String configSectionGetName() {
		return ConfigSection.SECTION_FILES;
	}

  public void configSectionSave() {
  }

  public void configSectionDelete() {
  }

  public Composite configSectionCreate(final Composite parent) {
    Image imgOpenFolder = ImageRepository.getImage("openFolderButton");
    GridData gridData;
    Composite cArea;

    Composite gFile = new Composite(parent, SWT.NULL);
    
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    gFile.setLayout(layout);
    Label label;

    label = new Label(gFile, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.zeronewfiles"); //$NON-NLS-1$
    BooleanParameter zeroNew = new BooleanParameter(gFile, "Zero New", false); //$NON-NLS-1$

    label = new Label(gFile, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.incrementalfile"); //$NON-NLS-1$
    BooleanParameter incremental = new BooleanParameter(gFile, "Enable incremental file creation", false); //$NON-NLS-1$

    //Make the incremental checkbox (button) deselect when zero new is used
    Button[] btnIncremental = {(Button)incremental.getControl()};
    zeroNew.setAdditionalActionPerformer(new ExclusiveSelectionActionPerformer(btnIncremental));

    //Make the zero new checkbox(button) deselct when incremental is used
    Button[] btnZeroNew = {(Button)zeroNew.getControl()};
    incremental.setAdditionalActionPerformer(new ExclusiveSelectionActionPerformer(btnZeroNew));

    label = new Label(gFile, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.checkOncompletion"); //$NON-NLS-1$
    new BooleanParameter(gFile, "Check Pieces on Completion", true);


    label = new Label(gFile, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.usefastresume"); //$NON-NLS-1$
    cArea = new Composite(gFile, SWT.NULL);
    layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layout.numColumns = 3;
    cArea.setLayout(layout);
    cArea.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    
    BooleanParameter bpUseResume = new BooleanParameter(cArea, "Use Resume", true); //$NON-NLS-1$

    label = new Label(cArea, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.saveresumeinterval"); //$NON-NLS-1$

    final String saveResumeLabels[] = new String[19];
    final int saveResumeValues[] = new int[19];
    for (int i = 2; i < 21; i++) {
      saveResumeLabels[i - 2] = " " + i + " min"; //$NON-NLS-1$ //$NON-NLS-2$
      saveResumeValues[i - 2] = i;
    }

    IntListParameter listSave = new IntListParameter(cArea, "Save Resume Interval", 5, saveResumeLabels, saveResumeValues); //$NON-NLS-1$

    Control[] controls = new Control[2];
    controls[0] = label;
    controls[1] = listSave.getControl();
    IAdditionalActionPerformer performer = new ChangeSelectionActionPerformer(controls);
    bpUseResume.setAdditionalActionPerformer(performer);

    // savepath
    label = new Label(gFile, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.defaultsavepath"); //$NON-NLS-1$

    cArea = new Composite(gFile, SWT.NULL);
    layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layout.numColumns = 3;
    cArea.setLayout(layout);
    cArea.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    BooleanParameter saveDefault = new BooleanParameter(cArea, "Use default data dir", true); //$NON-NLS-1$

    gridData = new GridData(GridData.FILL_HORIZONTAL);
    final StringParameter pathParameter = new StringParameter(cArea, "Default save path", ""); //$NON-NLS-1$ //$NON-NLS-2$
    pathParameter.setLayoutData(gridData);

    Button browse = new Button(cArea, SWT.PUSH);
    browse.setImage(imgOpenFolder);
    imgOpenFolder.setBackground(browse.getBackground());
    browse.setToolTipText(MessageText.getString("ConfigView.button.browse"));

    browse.addListener(SWT.Selection, new Listener() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
       */
      public void handleEvent(Event event) {
        DirectoryDialog dialog = new DirectoryDialog(parent.getShell(), SWT.APPLICATION_MODAL);
        dialog.setFilterPath(pathParameter.getValue());
        dialog.setText(MessageText.getString("ConfigView.dialog.choosedefaultsavepath")); //$NON-NLS-1$
        String path = dialog.open();
        if (path != null) {
          pathParameter.setValue(path);
        }
      }
    });

    controls = new Control[2];
    controls[0] = pathParameter.getControl();
    controls[1] = browse;
    IAdditionalActionPerformer defaultSave = new ChangeSelectionActionPerformer(controls);
    saveDefault.setAdditionalActionPerformer(defaultSave);

    // Move Completed
    label = new Label(gFile, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.movecompleted"); //$NON-NLS-1$
    BooleanParameter moveCompleted = new BooleanParameter(gFile, "Move Completed When Done", false); //$NON-NLS-1$

    Composite gMoveCompleted = new Composite(gFile, SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalIndent = 15;
    gridData.horizontalSpan = 2;
    gMoveCompleted.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 3;
    gMoveCompleted.setLayout(layout);

    label = new Label(gMoveCompleted, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.directory"); //$NON-NLS-1$

    gridData = new GridData(GridData.FILL_HORIZONTAL);
    final StringParameter movePathParameter = new StringParameter(gMoveCompleted, "Completed Files Directory", "");
    movePathParameter.setLayoutData(gridData);

    Button browse3 = new Button(gMoveCompleted, SWT.PUSH);
    browse3.setImage(imgOpenFolder);
    imgOpenFolder.setBackground(browse3.getBackground());
    browse3.setToolTipText(MessageText.getString("ConfigView.button.browse"));

    browse3.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        DirectoryDialog dialog = new DirectoryDialog(parent.getShell(), SWT.APPLICATION_MODAL);
        dialog.setFilterPath(movePathParameter.getValue());
        dialog.setText(MessageText.getString("ConfigView.dialog.choosemovepath")); //$NON-NLS-1$
        String path = dialog.open();
        if (path != null) {
          movePathParameter.setValue(path);
        }
      }
    });


    Label lMoveTorrent = new Label(gMoveCompleted, SWT.NULL);
    Messages.setLanguageText(lMoveTorrent, "ConfigView.label.movetorrent"); //$NON-NLS-1$
    BooleanParameter moveTorrent = new BooleanParameter(gMoveCompleted, "Move Torrent When Done", true); //$NON-NLS-1$
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    moveTorrent.setLayoutData(gridData);

    Label lMoveOnly = new Label(gMoveCompleted, SWT.NULL);
    Messages.setLanguageText(lMoveOnly, "ConfigView.label.moveonlyusingdefaultsave"); //$NON-NLS-1$
    BooleanParameter moveOnly = new BooleanParameter(gMoveCompleted, "Move Only When In Default Save Dir", true); //$NON-NLS-1$
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    moveOnly.setLayoutData(gridData);


    controls = new Control[6];
    controls[0] = movePathParameter.getControl();
    controls[1] = browse3;
    controls[2] = lMoveTorrent;
    controls[3] = moveTorrent.getControl();
    controls[4] = lMoveOnly;
    controls[5] = moveOnly.getControl();
    IAdditionalActionPerformer grayPathAndButton2 = new ChangeSelectionActionPerformer(controls);
    moveCompleted.setAdditionalActionPerformer(grayPathAndButton2);


    // Auto-Prioritize
    label = new Label(gFile, SWT.WRAP);
    gridData = new GridData();
    gridData.widthHint = 180;
    label.setLayoutData(gridData);
    Messages.setLanguageText(label, "ConfigView.label.priorityExtensions"); //$NON-NLS-1$

    cArea = new Composite(gFile, SWT.NULL);
    layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layout.numColumns = 3;
    cArea.setLayout(layout);
    cArea.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    gridData = new GridData(GridData.FILL_HORIZONTAL);
    new StringParameter(cArea, "priorityExtensions", "").setLayoutData(gridData); //$NON-NLS-1$

    label = new Label(cArea, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.ignoreCase");
    new BooleanParameter(cArea, "priorityExtensionsIgnoreCase");

    // Confirm Delete
    label = new Label(gFile, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.file.confirm_data_delete");
    new BooleanParameter(gFile, "Confirm Data Delete", true);
    // Max Open Files
    
    label = new Label(gFile, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.file.max_open_files");
    IntParameter file_max_open = new IntParameter(gFile, "File Max Open", 0);
    gridData = new GridData();
    gridData.widthHint = 30;
    file_max_open.setLayoutData( gridData );
    
    return gFile;
  }
    
}
