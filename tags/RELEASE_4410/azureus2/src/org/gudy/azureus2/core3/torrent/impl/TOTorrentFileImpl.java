/*
 * File    : TOTorrentFileImpl.java
 * Created : 5 Oct. 2003
 * By      : Parg 
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

package org.gudy.azureus2.core3.torrent.impl;


import java.io.*;
import java.util.*;

import org.gudy.azureus2.core3.internat.*;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.util.*;

public class 
TOTorrentFileImpl
	implements TOTorrentFile
{
	private final TOTorrent	torrent;
	private final long		file_length;
	private final byte[][]	path_components;
	private final byte[][]	path_components_utf8;
	
	private final int		first_piece_number;
	private final int		last_piece_number;
	
	private final Map		additional_properties = new LightHashMap(1);
	
	private final boolean	is_utf8;

	protected
	TOTorrentFileImpl(
		TOTorrent		_torrent,
		long			_torrent_offset,
		long			_len,
		String			_path )
		
		throws TOTorrentException
	{
		torrent			= _torrent;
		file_length		= _len;

		first_piece_number 	= (int)( _torrent_offset / torrent.getPieceLength());
		last_piece_number	= (int)(( _torrent_offset + file_length - 1 ) /  torrent.getPieceLength());

		is_utf8	= true;
		
		try{
			
			
			Vector	temp = new Vector();
			
			int	pos = 0;
			
			while(true){
				
				int	p1 = _path.indexOf( File.separator, pos );
				
				if ( p1 == -1 ){
					
					temp.add( _path.substring( pos ).getBytes( Constants.DEFAULT_ENCODING ));
					
					break;
				}
				
				temp.add( _path.substring( pos, p1 ).getBytes( Constants.DEFAULT_ENCODING ));
				
				pos = p1+1;
			}
			
			path_components		= new byte[temp.size()][];
			
			temp.copyInto( path_components );

			path_components_utf8		= new byte[temp.size()][];
			
			temp.copyInto( path_components_utf8 );

			checkComponents();
			
		}catch( UnsupportedEncodingException e ){
	
			throw( new TOTorrentException( 	"Unsupported encoding for '" + _path + "'",
											TOTorrentException.RT_UNSUPPORTED_ENCODING));
		}
	}

	protected
	TOTorrentFileImpl(
		TOTorrent		_torrent,
		long			_torrent_offset,
		long			_len,
		byte[][]		_path_components )
	
		throws TOTorrentException
	{
		torrent				= _torrent;
		file_length			= _len;
		path_components		= _path_components;
		path_components_utf8 = null;
		
		first_piece_number 	= (int)( _torrent_offset / torrent.getPieceLength());
		last_piece_number	= (int)(( _torrent_offset + file_length - 1 ) /  torrent.getPieceLength());

		is_utf8				= false;
		
		checkComponents();
	}

	protected
	TOTorrentFileImpl(
		TOTorrent		_torrent,
		long			_torrent_offset,
		long			_len,
		byte[][]		_path_components,
		byte[][]		_path_components_utf8 )
	
		throws TOTorrentException
	{
		torrent				= _torrent;
		file_length			= _len;
		path_components		= _path_components;
		path_components_utf8 = _path_components_utf8;
		
		first_piece_number 	= (int)( _torrent_offset / torrent.getPieceLength());
		last_piece_number	= (int)(( _torrent_offset + file_length - 1 ) /  torrent.getPieceLength());

		is_utf8				= false;
		
		checkComponents();
	}
		
	protected void
	checkComponents()
	
		throws TOTorrentException
	{
		for (int i=0;i<path_components.length;i++){
			
			byte[] comp = path_components[i];
			if (comp.length == 2 && comp[0] == (byte) '.' && comp[1] == (byte) '.')
				throw (new TOTorrentException("Torrent file contains illegal '..' component", TOTorrentException.RT_DECODE_FAILS));

			// intern directories as they're likely to repeat
			if(i < (path_components.length - 1))
				path_components[i] = StringInterner.internBytes(path_components[i]);
		}
	}
	
	public TOTorrent
	getTorrent()
	{
		return( torrent );
	}
	
	public long
	getLength()
	{
		return( file_length );
	}
	
	public byte[][]
	getPathComponentsBasic()
	{
		return( path_components );
	}

	public byte[][]
	getPathComponents()
	{
		return path_components_utf8 == null ? path_components : path_components_utf8;
	}

	public byte[][]
	getPathComponentsUTF8()
	{
		return( path_components_utf8 );
	}
	

	protected boolean
	isUTF8()
	{
		return( is_utf8 );
	}
	
	protected void
	setAdditionalProperty(
		String		name,
		Object		value )
	{
		additional_properties.put( name, value );
	}
	
	protected Map
	getAdditionalProperties()
	{
		return( additional_properties );
	}
	
	public int
	getFirstPieceNumber()
	{
		return( first_piece_number );
	}
	
	public int
	getLastPieceNumber()
	{
		return( last_piece_number );
	}
	
	public int
	getNumberOfPieces()
	{
		return( getLastPieceNumber() - getFirstPieceNumber() + 1 );
	}
	
	public String getRelativePath() {
		if (torrent == null)
			return "";
		String sRelativePath = "";

		LocaleUtilDecoder decoder = null;
		try {
			decoder = LocaleTorrentUtil.getTorrentEncodingIfAvailable(torrent);
			if (decoder == null) {
				LocaleUtil localeUtil = LocaleUtil.getSingleton();
				decoder = localeUtil.getSystemDecoder();
			}
		} catch (Exception e) {
			// Do Nothing
		}

		if (decoder != null) {
			byte[][]components = getPathComponents();
			for (int j = 0; j < components.length; j++) {

				try {
					String comp;
					try {
						comp = decoder.decodeString(components[j]);
					} catch (UnsupportedEncodingException e) {
						System.out.println("file - unsupported encoding!!!!");
						try {
							comp = new String(components[j]);
						} catch (Exception e2) {
							comp = "UnsupportedEncoding";
						}
					}
	
					comp = FileUtil.convertOSSpecificChars(comp, j != components.length-1 );
	
					sRelativePath += (j == 0 ? "" : File.separator) + comp;
				} catch (Exception ex) {
					Debug.out(ex);
				}

			}

		}
		return sRelativePath;
	}

	/**
	 * @return
	 *
	 * @since 4.1.0.5
	 */
	public Map serializeToMap() {
		Map	file_map = new HashMap();

		file_map.put( TOTorrentImpl.TK_LENGTH, new Long( getLength()));
		
		List path = new ArrayList();
		
		file_map.put( TOTorrentImpl.TK_PATH, path );
		
		byte[][]	path_comps = getPathComponentsBasic();
		
		for (int j=0;j<path_comps.length;j++){
			
			path.add( path_comps[j]);
		}
		
		if ( isUTF8()){
			
			List utf8_path = new ArrayList();
			
			file_map.put( TOTorrentImpl.TK_PATH_UTF8, utf8_path );
								
			for (int j=0;j<path_comps.length;j++){
				
				utf8_path.add( path_comps[j]);
			}
		} else {
			
			byte[][]	utf8_path_comps = getPathComponentsUTF8();

			if (utf8_path_comps != null) {
  			List utf8_path = new ArrayList();
  			
  			file_map.put( TOTorrentImpl.TK_PATH_UTF8, utf8_path );
  								
  			for (int j=0;j<utf8_path_comps.length;j++){
  				
  				utf8_path.add( utf8_path_comps[j]);
  			}
			}
		}
		
		Map file_additional_properties = getAdditionalProperties();
		
		Iterator prop_it = file_additional_properties.keySet().iterator();
		
		while( prop_it.hasNext()){
			
			String	key = (String)prop_it.next();
			
			file_map.put( key, file_additional_properties.get( key ));
		}

		return file_map;
	}
}
