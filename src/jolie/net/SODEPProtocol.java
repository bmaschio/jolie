/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi                                     *
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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Iterator;

import jolie.Scanner;
import jolie.TempVariable;
import jolie.Variable;

/*
Simple Operation Data Exchange Protocol BNF grammar:

<SodepPacket>		::=		<Operation> <Values>
<Operation>			::=		"operation" ":" <id> 
<Values>			::=		"values" "{" <ValuesListN> "}"
<ValuesListN>		::=		<string> <ValuesListNSep>
					|		<int> <ValuesListNSep>
					|		epsilon

<ValuesListNSep>	::=		"," <string> <ValuesListNSep>
					|		"," <int> <ValuesListNSep>
					|		epsilon

<id>				::=		[a-zA-Z][a-zA-Z0-9]*
<int>				::=		[0-9]+
<string>			::=		"[[:graph:]]"

Example:
operation: displayMessage
values
{
"Hello world"
}
*/

public class SODEPProtocol implements CommProtocol
{	
	public void send( OutputStream ostream, CommMessage message )
		throws IOException
	{
		String mesg = "operation:" + message.inputId() + '\n' + "values{";
		Variable var;
		Iterator< Variable > it = message.iterator();
		while( it.hasNext() ) {
			var = it.next();
			if ( var.isString() || !var.isDefined() )
				mesg += '"' + var.strValue() + '"';
			else if ( var.isInt() )
				mesg += var.intValue();
			else
				throw new IOException( "sodep packet creation: invalid variable type or undefined variable" );
			if ( it.hasNext() )
				mesg += ',';
		}
		mesg += '}';
		
		mesg = Scanner.addStringTerminator( mesg );

		BufferedWriter writer = new BufferedWriter( new OutputStreamWriter( ostream ) );
		writer.write( mesg );
		writer.flush();
	}
	
	public CommMessage recv( InputStream istream )
		throws IOException
	{
		Scanner.Token token;
		CommMessage message = null;
		TempVariable var;
		boolean stop = false;
		Scanner scanner = new Scanner( istream, "network" );
		
		token = scanner.getToken();
		if ( token.type() == Scanner.TokenType.EOF )
			return null;
		
		if ( token.type() != Scanner.TokenType.ID || !(token.content().equals( "operation" )) )
			throw new IOException( "malformed SODEP packet. operation keyword expected" );
		token = scanner.getToken();
		if ( token.type() != Scanner.TokenType.COLON  )
			throw new IOException( "malformed SODEP packet. : expected" );
		token = scanner.getToken();
		if ( token.type() != Scanner.TokenType.ID )
			throw new IOException( "malformed SODEP packet. operation identifier expected" );
		
		message = new CommMessage( token.content() );

		token = scanner.getToken();
		if ( token.type() != Scanner.TokenType.ID || !(token.content().equals( "values" )) )
			throw new IOException( "malformed SODEP packet. values keyword expected" );
		token = scanner.getToken();
		if ( token.type() != Scanner.TokenType.LCURLY  )
			throw new IOException( "malformed SODEP packet. { expected" );
		token = scanner.getToken();
		
		while ( token.type() != Scanner.TokenType.RCURLY && !stop ) { 
			if ( token.type() == Scanner.TokenType.STRING )
				var = new TempVariable( token.content() );
			else if ( token.type() == Scanner.TokenType.INT )
				var = new TempVariable( Integer.parseInt( token.content() ) );
			else
				throw new IOException( "malformed SODEP packet. invalid variable type" );
			message.addValue( var );
			token = scanner.getToken();
			if ( token.type() != Scanner.TokenType.COMMA )
				stop = true;
			else
				token = scanner.getToken();
		}
		
		if ( token.type() != Scanner.TokenType.RCURLY )
			throw new IOException( "malformed SODEP packet. } expected" );
		
		return message;
	}
}