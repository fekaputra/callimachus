/*
   Portions Copyright (c) 2009-10 Zepheira LLC, Some Rights Reserved
   Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */
package org.callimachusproject.engine.impl;

import org.callimachusproject.engine.model.CURIE;

/**
 * Compact Uniform Resource Identifier (CURIE).
 * 
 * @author James Leigh
 *
 */
public class CURIEImpl extends CURIE {
	private String namespaceURI;
	private String reference;
	private String prefix;

	public CURIEImpl(String ns, String reference, String prefix) {
		assert ns != null;
		assert reference != null;
		assert prefix != null;
		this.namespaceURI = ns.endsWith("/") || ns.endsWith("#") ? ns
				: (ns + "#");
		this.reference = reference;
		this.prefix = prefix;
	}

	public String getNamespaceURI() {
		return namespaceURI;
	}

	public String getReference() {
		return reference;
	}

	public String getPrefix() {
		return prefix;
	}

}
