package com.aelitis.azureus.ui.swt.views.skin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.ui.UIFunctionsUserPrompter;
import com.aelitis.azureus.ui.UserPrompterResultListener;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.views.skin.SkinnedDialog.SkinnedDialogClosedListener;

public class VuzeMessageBox
	implements UIFunctionsUserPrompter, SkinnedDialogClosedListener
{

	private String title;

	private String text;

	private String[] buttons;

	private int defaultButtonPos;

	private int result = -1;

	private UserPrompterResultListener resultListener;

	private VuzeMessageBoxListener vuzeMessageBoxListener;

	private SWTSkinObjectContainer soExtra;

	private SkinnedDialog dlg;

	private String iconResource;

	public VuzeMessageBox(final String title, final String text,
			final String[] buttons, final int defaultOption) {
		this.title = title;
		this.text = text;
		this.buttons = buttons == null ? new String[0] : buttons;
		this.defaultButtonPos = defaultOption;
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.UIFunctionsUserPrompter#getAutoCloseInMS()
	 */
	public int getAutoCloseInMS() {
		return 0;
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.UIFunctionsUserPrompter#getHtml()
	 */
	public String getHtml() {
		return null;
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.UIFunctionsUserPrompter#getRememberID()
	 */
	public String getRememberID() {
		return null;
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.UIFunctionsUserPrompter#getRememberText()
	 */
	public String getRememberText() {
		return null;
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.UIFunctionsUserPrompter#isAutoClosed()
	 */
	public boolean isAutoClosed() {
		return false;
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.UIFunctionsUserPrompter#open(com.aelitis.azureus.ui.UserPrompterResultListener)
	 */
	public void open(final UserPrompterResultListener l) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				_open(l);
			}
		});
	}

	protected void _open(UserPrompterResultListener l) {
		this.resultListener = l;
		dlg = new SkinnedDialog("skin3_dlg_generic", "shell");
		dlg.setTitle(title);
		dlg.addCloseListener(this);

		SWTSkin skin = dlg.getSkin();
		SWTSkinObjectText soTopTitle = (SWTSkinObjectText) skin.getSkinObject("top-title");
		if (soTopTitle != null) {
			soTopTitle.setText(title);
		}

		SWTSkinObjectText soText = (SWTSkinObjectText) skin.getSkinObject("middle-title");
		if (soText != null) {
			soText.setText(text);
		}
		
		if (iconResource != null) {
  		SWTSkinObjectImage soTopLogo = (SWTSkinObjectImage) dlg.getSkin().getSkinObject("top-logo");
  		if (soTopLogo != null) {
  			soTopLogo.setImageByID(iconResource, null);
  		}
		}
		
		SWTSkinObjectContainer soBottomArea = (SWTSkinObjectContainer) skin.getSkinObject("bottom-area");
		if (soBottomArea != null) {
			FormData fd;
			Composite cBottomArea = soBottomArea.getComposite();
			Composite cCenterH = new Composite(cBottomArea, SWT.NONE);
			fd = new FormData();
			fd.height = 1;
			fd.left = new FormAttachment(0);
			fd.right = new FormAttachment(100);
			cCenterH.setLayoutData(fd);

			Composite cCenterV = new Composite(cBottomArea, SWT.NONE);
			fd = new FormData();
			fd.height = 1;
			fd.top = new FormAttachment(0);
			fd.bottom = new FormAttachment(100);
			cCenterV.setLayoutData(fd);

			Composite cButtonArea = new Composite(cBottomArea, SWT.NONE);
			// Fix button BG not right on Win7
			cButtonArea.setBackgroundMode(SWT.INHERIT_FORCE);
			fd = new FormData();
			fd.top = new FormAttachment(cCenterV, 0, SWT.CENTER);
			fd.left = new FormAttachment(cCenterH, 0, SWT.CENTER);
			cButtonArea.setLayoutData(fd);

			RowLayout rowLayout = new RowLayout(SWT.HORIZONTAL);
			rowLayout.center = true;
			rowLayout.spacing = 8;
			rowLayout.pack = false;
			cButtonArea.setLayout(rowLayout);
			for (int i = 0; i < buttons.length; i++) {
				String buttonText = buttons[i];
				Button button = new Button(cButtonArea, SWT.PUSH);
				button.setText(buttonText);
				if (defaultButtonPos == i) {
					button.getShell().setDefaultButton(button);
				}
				button.setData("ButtonNo", new Integer(i));
				button.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event event) {
						close(((Number) event.widget.getData("ButtonNo")).intValue());
					}
				});
			}

			cBottomArea.getParent().layout(true, true);
		}

		if (vuzeMessageBoxListener != null) {
			soExtra = (SWTSkinObjectContainer) skin.getSkinObject("middle-extra");
			vuzeMessageBoxListener.shellReady(dlg.getShell(), soExtra);
		}

		dlg.open();
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.UIFunctionsUserPrompter#setAutoCloseInMS(int)
	 */
	public void setAutoCloseInMS(int autoCloseInMS) {
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.UIFunctionsUserPrompter#setHtml(java.lang.String)
	 */
	public void setHtml(String html) {
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.UIFunctionsUserPrompter#setIconResource(java.lang.String)
	 */
	public void setIconResource(String resource) {
		this.iconResource = resource;
		if (dlg != null) {
  		SWTSkinObjectImage soTopLogo = (SWTSkinObjectImage) dlg.getSkin().getSkinObject("top-logo");
  		if (soTopLogo != null) {
  			soTopLogo.setImageByID(iconResource, null);
  		}
		}
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.UIFunctionsUserPrompter#setRelatedObject(java.lang.Object)
	 */
	public void setRelatedObject(Object relatedObject) {
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.UIFunctionsUserPrompter#setRelatedObjects(java.lang.Object[])
	 */
	public void setRelatedObjects(Object[] relatedObjects) {
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.UIFunctionsUserPrompter#setRemember(java.lang.String, boolean, java.lang.String)
	 */
	public void setRemember(String rememberID, boolean rememberByDefault,
			String rememberText) {
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.UIFunctionsUserPrompter#setRememberText(java.lang.String)
	 */
	public void setRememberText(String rememberText) {
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.UIFunctionsUserPrompter#setUrl(java.lang.String)
	 */
	public void setUrl(String url) {
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.UIFunctionsUserPrompter#waitUntilClosed()
	 */
	public int waitUntilClosed() {
		return 0;
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.swt.views.skin.SkinnedDialog.SkinnedDialogClosedListener#skinDialogClosed(com.aelitis.azureus.ui.swt.views.skin.SkinnedDialog)
	 */
	public void skinDialogClosed(SkinnedDialog dialog) {
		if (resultListener != null) {
			resultListener.prompterClosed(result);
		}
	}

	public void setListener(VuzeMessageBoxListener l) {
		this.vuzeMessageBoxListener = l;
	}

	public void close(int result) {
		this.result = result;
		dlg.close();
	}
}