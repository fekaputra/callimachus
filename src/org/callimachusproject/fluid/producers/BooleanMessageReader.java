/*
 * Copyright (c) 2009, James Leigh All rights reserved.
 * Copyright (c) 2011 Talis Inc., Some rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package org.callimachusproject.fluid.producers;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;

import org.callimachusproject.fluid.producers.base.MessageReaderBase;
import org.callimachusproject.io.ChannelUtil;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.resultio.BooleanQueryResultFormat;
import org.openrdf.query.resultio.BooleanQueryResultParserFactory;
import org.openrdf.query.resultio.BooleanQueryResultParserRegistry;
import org.openrdf.query.resultio.QueryResultParseException;

/**
 * Reads a boolean query result.
 * 
 * @author James Leigh
 * 
 */
public class BooleanMessageReader
		extends
		MessageReaderBase<BooleanQueryResultFormat, BooleanQueryResultParserFactory, Boolean> {

	public BooleanMessageReader() {
		super(BooleanQueryResultParserRegistry.getInstance(), Boolean.class);
	}

	@Override
	protected boolean classEquals(Class<?> type) {
		return type.equals(Boolean.class) || type.equals(Boolean.TYPE);
	}

	@Override
	public Boolean readFrom(BooleanQueryResultParserFactory factory,
			ReadableByteChannel in, Charset charset, String base)
			throws QueryResultParseException, TupleQueryResultHandlerException,
			IOException, QueryEvaluationException {
		if (in == null)
			return null;
		try {
			return factory.getParser().parse(ChannelUtil.newInputStream(in));
		} finally {
			in.close();
		}
	}

}
