/*
 * Created on 19-Apr-2004
 * Created by Paul Gardner
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.pluginsimpl.local.ui;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.plugins.ui.*;
import org.gudy.azureus2.plugins.ui.view.*;
import org.gudy.azureus2.plugins.ui.tables.mytracker.*;
import org.gudy.azureus2.pluginsimpl.local.ui.mytracker.*;

import org.gudy.azureus2.ui.swt.MainWindow;

public class 
UIManagerImpl 
	implements UIManager
{
	protected static UIManagerImpl	singleton;
	
	public synchronized static UIManagerImpl
	getSingleton()
	{
		if ( singleton == null ){
			
			singleton = new UIManagerImpl();
		}
		
		return( singleton );
	}
	
	protected MyTrackerImpl		my_tracker;
	
	protected
	UIManagerImpl()
	{
		my_tracker	= new MyTrackerImpl();
	}
	
	public BasicPluginView
	getBasicPluginView()
	{
		return( null );
	}

	public MyTracker
	getMyTracker()
	{
		return( my_tracker );
	}
	
	public void
	copyToClipBoard(
		String		data )
	
		throws UIException
	{
		try{
			MainWindow.copyToClipBoard( data );
			
		}catch( Throwable e ){
			
			throw( new UIException( "Failed to copy to clipboard", e ));
		}

	}
}
