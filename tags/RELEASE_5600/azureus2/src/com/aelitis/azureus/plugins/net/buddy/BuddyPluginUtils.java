/*
 * Created on Nov 5, 2014
 * Created by Paul Gardner
 * 
 * Copyright 2014 Azureus Software, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or 
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.aelitis.azureus.plugins.net.buddy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.AENetworkClassifier;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.AsyncDispatcher;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginBeta.ChatInstance;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginViewInterface.ViewListener;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.UIFunctionsUserPrompter;

public class 
BuddyPluginUtils 
{
	private static BuddyPlugin
	getPlugin()
	{
		PluginInterface pi = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByID( "azbuddy", true );
		
		if ( pi != null ){
			
			return((BuddyPlugin)pi.getPlugin());
		}
		
		return( null );
	}
	
	public static BuddyPluginBeta
	getBetaPlugin()
	{
		BuddyPlugin bp = getPlugin();
		
		if ( bp != null && bp.isBetaEnabled()){
			
			BuddyPluginBeta beta = bp.getBeta();
			
			if ( beta.isAvailable()){
				
				return( beta );
			}
		}
		
		return( null );
	}
	
	public static boolean
	isBetaChatAvailable()
	{
		BuddyPlugin bp = getPlugin();
		
		if ( bp != null && bp.isBetaEnabled()){
			
			return( bp.getBeta().isAvailable());
		}
		
		return( false );
	}
	
	public static boolean
	isBetaChatAnonAvailable()
	{
		BuddyPlugin bp = getPlugin();
		
		if ( bp != null && bp.isBetaEnabled()){
			
			return( bp.getBeta().isAvailable() && bp.getBeta().isI2PAvailable());
		}
		
		return( false );
	}
	
	public static void
	createBetaChat(
		final String		network,
		final String		key,
		final Runnable		callback )
	{
		new AEThread2( "Chat create async" )
		{
			public void
			run()
			{
				try{
					BuddyPlugin bp = getPlugin();
					
					bp.getBeta().getAndShowChat( network, key );
					
				}catch( Throwable e ){
					
					Debug.out( e );
					
				}finally{
					
					if ( callback != null ){
					
						callback.run();
					}
				}
			}
		}.start();
	}
	
	public static Map<String,Object>
	peekChat(
		String		net,
		String		key )
	{
		BuddyPlugin bp = getPlugin();
		
		if ( bp != null && bp.isBetaEnabled()){
			
			return( bp.getBeta().peekChat( net, key ));
		}
		
		return( null );
	}
	
	public static Map<String,Object>
	peekChat(
		Download		download )
	{
		BuddyPlugin bp = getPlugin();
		
		if ( bp != null && bp.isBetaEnabled()){
			
			return( bp.getBeta().peekChat( download ));
		}
		
		return( null );
	}
	
	private static AsyncDispatcher peek_dispatcher = new AsyncDispatcher( "peeker" );
	
	public static void
	peekChatAsync(
		final String		net,
		final String		key,
		final Runnable		done )
	{
		boolean	async = false;
		
		try{
			if ( isBetaChatAvailable()){
	
				if ( net != AENetworkClassifier.AT_PUBLIC && !isBetaChatAnonAvailable()){
					
					return;
				}
				
				if ( peek_dispatcher.getQueueSize() > 200 ){
					
					return;
				}
								
				peek_dispatcher.dispatch(
					new AERunnable() {
						
						@Override
						public void 
						runSupport() 
						{
							try{
								Map<String,Object> peek_data = BuddyPluginUtils.peekChat( net, key );
									
								if ( peek_data != null ){
									
									Number	message_count 	= (Number)peek_data.get( "m" );
									Number	node_count 		= (Number)peek_data.get( "n" );
									
									if ( message_count != null && node_count != null ){
										
										if ( message_count.intValue() > 0 ){
											
											BuddyPluginBeta.ChatInstance chat = BuddyPluginUtils.getChat( net, key );
						
											if ( chat != null ){
												
												chat.setAutoNotify( true );
											}
										}
									}	
								}
							}finally{
								
								done.run();
							}
						}
					});
				
				async = true;
			}
		}finally{
			
			if ( !async ){
				
				done.run();
			}
		}
	}
	
	public static ChatInstance
	getChat(
		String		net,
		String		key )
	{
		BuddyPlugin bp = getPlugin();
		
		if ( bp != null && bp.isBetaEnabled()){
			
			try{
				return( bp.getBeta().getChat( net, key ));
				
			}catch( Throwable e ){
				
			}
		}
		
		return( null );
	}
	
	public static ChatInstance
	getChat(
		Download		download )
	{
		BuddyPlugin bp = getPlugin();
		
		if ( bp != null && bp.isBetaEnabled()){
			
			return( bp.getBeta().getChat( download ));
		}
		
		return( null );
	}
	
	public static BuddyPluginViewInterface.View
	buildChatView(
		Map<String,Object>	properties,
		ViewListener		listener )
	{
		BuddyPlugin bp = getPlugin();
		
		if ( bp != null && bp.isBetaEnabled() && bp.getBeta().isAvailable()){
			
			BuddyPluginViewInterface ui = bp.getSWTUI();
			
			if ( ui != null ){
				
				return( ui.buildView( properties, listener ));
			}
		}

		return( null );
	}
	
	public static String
	getChatKey(
		TOTorrent		torrent )
	{
		if ( torrent == null ){
			
			return( null );
		}
		
		return( getChatKey( PluginCoreUtils.wrap( torrent )));
	}
	
	public static String
	getChatKey(
		Download		download )
	{
		return( getChatKey( download.getTorrent()));
	}
	
	public static String
	getChatKey(
		Torrent		torrent )
	{		
		if ( torrent == null ){
			
			return( null );
		}
		
			// use torrent name here to canonicalize things in case user has renamed download display name
			// also it is more important to get a consistent string rather than something correctly localised
		
		String	torrent_name = null;
		
		try{
			TOTorrent to_torrent = PluginCoreUtils.unwrap( torrent );
			
			torrent_name = to_torrent.getUTF8Name();
			
			if ( torrent_name == null ){
				
				torrent_name = new String( to_torrent.getName(), "UTF-8" );
			}
		}catch( Throwable e ){
			
		}
		
		if ( torrent_name == null ){
			
			torrent_name = torrent.getName();
		}
		
		String key = "Download: " + torrent_name + " {" + ByteFormatter.encodeString( torrent.getHash()) + "}";

		return( key );
	}
	
	private static final Object i2p_install_lock = new Object();
	
	private static boolean i2p_installing = false;
	
	public static boolean
	isInstallingI2PHelper()
	{
		synchronized( i2p_install_lock ){
			
			return( i2p_installing );
		}
	}

	public static boolean
	installI2PHelper(
		String				remember_id,
		final boolean[]		install_outcome,
		final Runnable		callback )
	{
		return installI2PHelper(null, remember_id, install_outcome, callback);
	}

	public static boolean
	installI2PHelper(
		String extra_text,
		String				remember_id,
		final boolean[]		install_outcome,
		final Runnable		callback )
	{
		synchronized( i2p_install_lock ){
			
			if ( i2p_installing ){
				
				Debug.out( "I2P Helper already installing" );
				
				return( false );
			}
			
			i2p_installing = true;
		}
		
		boolean	installing = false;
		
		try{
			UIFunctions uif = UIFunctionsManager.getUIFunctions();
			
			if ( uif == null ){
				
				Debug.out( "UIFunctions unavailable - can't install plugin" );
				
				return( false );
			}
			
			String title = MessageText.getString("azneti2phelper.install");
			
			String text = "";
			if (extra_text != null) {
				text = extra_text + "\n\n";
			}
			text += MessageText.getString("azneti2phelper.install.text" );
			
			UIFunctionsUserPrompter prompter = uif.getUserPrompter(title, text, new String[] {
				MessageText.getString("Button.yes"),
				MessageText.getString("Button.no")
			}, 0);
			
			if ( remember_id != null ){

				prompter.setRemember( 
					remember_id, 
					false,
					MessageText.getString("MessageBoxWindow.nomoreprompting"));
			}
			
			prompter.setAutoCloseInMS(0);
			
			prompter.open(null);
			
			boolean	install = prompter.waitUntilClosed() == 0;
			
			if ( install ){
	
				installing = true;
				
				uif.installPlugin(
						"azneti2phelper",
						"azneti2phelper.install",
						new UIFunctions.actionListener()
						{
							public void
							actionComplete(
								Object		result )
							{
								try{
									if ( callback != null ){
										
										if ( result instanceof Boolean ){
											
											install_outcome[0] = (Boolean)result;
										}
										
										callback.run();
									}
								}finally{
																		
									synchronized( i2p_install_lock ){
											
										i2p_installing = false;
									}
								}	
							}
						});
			
			}else{
				
				Debug.out( "I2P Helper install declined (either user reply or auto-remembered)" );
			}
			
			return( install );
			
		}finally{
			
			if ( !installing ){
			
				synchronized( i2p_install_lock ){
					
					i2p_installing = false;
				}
			}
		}
	}
}
