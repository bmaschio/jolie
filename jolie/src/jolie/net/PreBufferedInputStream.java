/***************************************************************************
 *   Copyright (C) 2006-2015 by Fabrizio Montesi <famontesi@gmail.com>     *
 *   Copyright (C) 2015 by Matthias Dieter Wallnöfer                       *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU Library General Public License as       *
 *   published by the Free Software Foundation; either version 2 of the    *
 *   License, or (at your option) any later version.                       *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU Library General Public     *
 *   License along with this program; if not, write to the                 *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 *                                                                         *
 *   For details about the authors of this software, see the AUTHORS file. *
 ***************************************************************************/


package jolie.net;

import java.io.IOException;
import java.io.InputStream;

public class PreBufferedInputStream extends InputStream
{
	private static final int INITIAL_BUFFER_SIZE = 10;
	private final InputStream istream;
	private byte[] buffer = new byte[ INITIAL_BUFFER_SIZE ];
	private int writePos = 0;
	private int readPos = 0;
	private int count = 0;

	public PreBufferedInputStream( InputStream istream )
	{
		this.istream = istream;
	}

	public synchronized boolean hasCachedData()
	{
		return count > 0;
	}

	public synchronized void append( byte b )
		throws IOException
	{
		if ( buffer == null ) {
			throw new IOException( "Stream closed" );
		}

		if ( count < 0 ) {
			count = 0;
		}
		count++;
		if ( count >= buffer.length ) { // We need to enlarge the buffer
			byte[] newBuffer = new byte[ buffer.length * 2] ;
			System.arraycopy( buffer, 0, newBuffer, 0, buffer.length );
			buffer = newBuffer;
		}
		if ( writePos >= buffer.length ) {
			writePos = 0;
		}

		buffer[ writePos++ ] = b;
	}

	@Override
	public synchronized int available()
		throws IOException
	{
		if ( buffer == null ) {
			throw new IOException( "Stream closed" );
		}
		return Math.max( count, 0 ) + istream.available();
	}

	@Override
	public synchronized int read()
		throws IOException
	{
		if ( buffer == null ) {
			throw new IOException( "Stream closed" );
		}

		if ( count < 1 ) { // No bytes to read
			return istream.read();
		}

		count--;
		if ( readPos >= buffer.length ) {
			readPos = 0;
		}

		return buffer[ readPos++ ];
	}

	@Override
	public int read( byte[] b )
		throws IOException
	{
		return read( b, 0, b.length );
	}

	@Override
	public synchronized int read( byte[] b, int off, int len )
		throws IOException
	{
		if ( buffer == null ) {
			throw new IOException( "Stream closed" );
		}

		int lenFromBuffer = count >= len ? len : count;
		if ( lenFromBuffer > 0 ) {
			// match implementation of read()
			count -= lenFromBuffer;
			for ( int i = 0; i < lenFromBuffer; i++ ) {
				if ( readPos >= buffer.length ) {
					readPos = 0;
				}
				b[ off + i ] = buffer[ readPos++ ];
			}
		} else {
			// count could have been negative, so reset lenFromBuffer to 0
			lenFromBuffer = 0;
		}

		int lenFromStream = 0;
		if ( lenFromBuffer != len ) {
			lenFromStream = istream.read( b, off+lenFromBuffer, len-lenFromBuffer );
			if ( lenFromStream < 0 && lenFromBuffer > 0 ) {
				// we return -1 (EOF) only when lenFromBuffer == 0
				lenFromStream = 0;
			}
		}

		return lenFromBuffer + lenFromStream;
	}

	@Override
	public synchronized long skip( long n )
		throws IOException
	{
		if ( buffer == null ) {
			throw new IOException( "Stream closed" );
		}

		if ( n <= 0 ) {
			return 0;
		}

		long skipFromBuffer = count >= n ? n : count;
		if ( skipFromBuffer > 0 ) {
			// match implementation of read()
			// skipFromBuffer <= count
			count -= (int)skipFromBuffer;
			for ( int i = 0; i < (int)skipFromBuffer; i++ ) {
				if ( readPos >= buffer.length ) {
					readPos = 0;
				}
				readPos++;
			}
		} else {
			// count could have been negative, so reset skipFromBuffer to 0
			skipFromBuffer = 0;
		}

		long skipFromStream = 0;
		if ( skipFromBuffer != n ) {
			skipFromStream = istream.skip( n-skipFromBuffer );
		}

		return skipFromBuffer + skipFromStream;
	}

	@Override
	public synchronized void close()
		throws IOException
	{
		buffer = null;
		// Java's semantic: a stream closes also its underlying ones
		istream.close();
	}
}
