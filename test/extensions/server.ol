/***************************************************************************
 *   Copyright (C) 2009 by Fabrizio Montesi <famontesi@gmail.com>          *
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

include "../AbstractTestUnit.iol"

include "private/server.iol"

outputPort SODEPServer {
Location: Location_SODEPServer
Protocol: sodep
Interfaces: ServerInterface
}

outputPort SOAPServer {
Location: Location_SOAPServer
Protocol: soap {
	.compression -> compression;
	.requestCompression -> requestCompression
}
Interfaces: ServerInterface
}

outputPort JSONRPCServer {
Location: Location_JSONRPCServer
Protocol: jsonrpc {
	.compression -> compression;
	.requestCompression -> requestCompression
}
Interfaces: ServerInterface
}

outputPort HTTPServer {
Location: Location_HTTPServer
Protocol: http {
	.method -> method;
	.method.queryFormat = "json";
	.format -> format;
	.compression -> compression;
	.requestCompression -> requestCompression
}
Interfaces: ServerInterface
}

embedded {
Jolie:
	"private/sodep_server.ol",
	"private/soap_server.ol",
	"private/jsonrpc_server.ol",
	"private/http_server2.ol"
}

define checkResponse
{
	if ( response.id != 123456789123456789L || response.firstName != "John" || response.lastName != "Döner" || response.age != 30 || response.size != 90.5 || response.male != true || response.unknown != "Hey" || response.unknown2 != void || #response.array != 3 || response.array[0] != 0 || response.array[1] != "Ho" || response.array[2] != 3.14 || response.object.data != 10L ) {
		throw( TestFailed, "Data <=> Query value mismatch" )
	};
	if ( response2 != reqVal ) {
		throw( TestFailed, "Data <=> Query value mismatch" )
	}
}

define test
{
	echoPerson@SODEPServer( person )( response );
	identity@SODEPServer( reqVal )( response2 );
	checkResponse;

	echoPerson@SOAPServer( person )( response );
	identity@SOAPServer( reqVal )( response2 );
	checkResponse;

	echoPerson@JSONRPCServer( person )( response );
	identity@JSONRPCServer( reqVal )( response2 );
	checkResponse;

	method = "post";
	format = "xml";
	echoPerson@HTTPServer( person )( response );
	identity@HTTPServer( reqVal )( response2 );
	checkResponse;
	format = "json";
	echoPerson@HTTPServer( person )( response );
	identity@HTTPServer( reqVal )( response2 );
	checkResponse;
	method = "get"; // JSON-ified
	echoPerson@HTTPServer( person )( response );
	identity@HTTPServer( reqVal )( response2 );
	checkResponse
}

define doTest
{
	with( person ) {
		.id = 123456789123456789L;
		.firstName = "John";
		.lastName = "Döner";
		.age = 30;
		.size = 90.5;
		.male = true;
		.unknown = "Hey";
		.unknown2 = void;
		.array[0] = 0;
		.array[1] = "Ho";
		.array[2] = 3.14;
		.object.data = 10L
	};
	reqVal = "Döner";
	scope( s ) {
		install( TypeMismatch => throw( TestFailed, s.TypeMismatch ) );

		// compression on (default), but no request compression
		test;
		// request compression
		requestCompression = "deflate";
		test;
		requestCompression = "gzip";
		test;
		// no compression at all
		compression = false;
		test;

		shutdown@SODEPServer();
		shutdown@SOAPServer();
		shutdown@JSONRPCServer();
		shutdown@HTTPServer()
	}
}
