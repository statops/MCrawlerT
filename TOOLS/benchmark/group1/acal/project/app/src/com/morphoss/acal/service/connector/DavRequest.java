/*
 * Copyright (C) 2011 Morphoss Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.morphoss.acal.service.connector;


import java.net.URI;

import org.apache.http.Header;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;

public class DavRequest extends HttpEntityEnclosingRequestBase  {
		
		private String method;
		//private URI uri;
		
		public DavRequest(String method) {
			super();
			this.method = method;
		}
		
		public DavRequest(String method, URI uri ) {
			super();
			this.method = method;
			setURI(uri);
		}

		public DavRequest(String method, String uri )  {
			super();
			this.method = method;
			setURI(URI.create(uri));
		}
		
		public String getMethod() {
			return this.method;
		}
		
		public void addHeaders(Header[] headers) {
			for (Header h : headers) {
				this.setHeader(h.getName(), h.getValue());
			}
		}
    }
