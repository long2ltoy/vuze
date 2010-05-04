/*
 * Created on Feb 9, 2009
 * Created by Paul Gardner
 * 
 * Copyright 2009 Vuze, Inc.  All rights reserved.
 * 
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.aelitis.azureus.core.devices.impl;

import java.io.IOException;
import java.net.Socket;


public class 
TranscodePipeStreamSource 
	extends TranscodePipe
{
	private int				source_port;
		
	protected
	TranscodePipeStreamSource(
		int		_source_port )
	
		throws IOException
	{
		super( null );
		
		source_port	= _source_port;
	}		

	
	protected void
	handleSocket(
		Socket		socket1 )
	{
		synchronized( this ){
			
			if ( destroyed ){
				
				try{
					socket1.close();
					
				}catch( Throwable e ){				
				}
				
				return;
			}
			
			sockets.add( socket1 );
		}

		try{
			Socket socket2 = new Socket( "127.0.0.1", source_port );
	
			synchronized( this ){

				if ( destroyed ){
					
					try{
						socket1.close();
						
					}catch( Throwable e ){				
					}
					
					try{
						socket2.close();
						
					}catch( Throwable e ){				
					}
					
					sockets.remove( socket1 );
					
					return;
				}
				
				sockets.add( socket2 );
			}
			
			handlePipe( socket1.getInputStream(), socket2.getOutputStream());
			
			handlePipe( socket2.getInputStream(), socket1.getOutputStream());
			
		}catch( Throwable e ){
			
			try{
				socket1.close();
				
			}catch( Throwable f ){
			}
			
			synchronized( this ){

				sockets.remove( socket1 );
			}
		}
	}
}
