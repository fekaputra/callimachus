/*
 * Portions Copyright (c) 2009-10 Zepheira LLC and James Leigh, Some
  Rights Reserved
 * Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.callimachusproject.behaviours;

import static org.callimachusproject.stream.SPARQLWriter.toSPARQL;
import static org.openrdf.query.QueryLanguage.SPARQL;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.tools.FileObject;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHttpRequest;
import org.callimachusproject.concepts.Page;
import org.callimachusproject.rdfa.RDFEventReader;
import org.callimachusproject.rdfa.RDFParseException;
import org.callimachusproject.rdfa.RDFaReader;
import org.callimachusproject.rdfa.events.BuiltInCall;
import org.callimachusproject.rdfa.events.ConditionalOrExpression;
import org.callimachusproject.rdfa.events.Expression;
import org.callimachusproject.rdfa.events.RDFEvent;
import org.callimachusproject.rdfa.events.Subject;
import org.callimachusproject.rdfa.events.TriplePattern;
import org.callimachusproject.rdfa.model.IRI;
import org.callimachusproject.rdfa.model.PlainLiteral;
import org.callimachusproject.rdfa.model.TermFactory;
import org.callimachusproject.rdfa.model.VarOrTerm;
import org.callimachusproject.stream.BufferedXMLEventReader;
import org.callimachusproject.stream.IterableRDFEventReader;
import org.callimachusproject.stream.RDFStoreReader;
import org.callimachusproject.stream.RDFXMLEventReader;
import org.callimachusproject.stream.RDFaProducer;
import org.callimachusproject.stream.ReducedTripleReader;
import org.callimachusproject.stream.SPARQLPosteditor;
import org.callimachusproject.stream.SPARQLProducer;
import org.callimachusproject.stream.TriplePatternStore;
import org.callimachusproject.stream.TriplePatternVariableStore;
import org.callimachusproject.traits.SoundexTrait;
import org.openrdf.http.object.annotations.header;
import org.openrdf.http.object.annotations.query;
import org.openrdf.http.object.annotations.type;
import org.openrdf.http.object.client.HTTPObjectClient;
import org.openrdf.http.object.exceptions.BadRequest;
import org.openrdf.http.object.exceptions.ResponseException;
import org.openrdf.http.object.util.ChannelUtil;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.impl.TupleQueryResultImpl;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.xslt.XMLEventReaderFactory;
import org.openrdf.repository.object.xslt.XSLTransformer;

/**
 * Implements the construct search method to lookup resources by label prefix
 * and options method to list all possible values.
 * 
 * @author James Leigh 
 * @author Steve Battle
 * 
 */
public abstract class FormSupport implements Page, SoundexTrait,
		RDFObject, FileObject {
	private static TermFactory tf = TermFactory.newInstance();
	private static ValueFactory vf = new ValueFactoryImpl();
	
	
	static final XSLTransformer HTML_XSLT;
	static {
		String path = "org/callimachusproject/xsl/xhtml-to-html.xsl";
		ClassLoader cl = ViewSupport.class.getClassLoader();
		String url = cl.getResource(path).toExternalForm();
		InputStream input = cl.getResourceAsStream(path);
		InputStreamReader reader = new InputStreamReader(input);
		HTML_XSLT = new XSLTransformer(reader, url);
	}

	/**
	 * Extracts an element from the template (without variables).
	 * TODO strip out RDFa variables and expressions
	 */
	@query("template")
	@type("text/html")
	public String template(@query("query") String query,
			@query("element") String element) throws Exception {
		String html = asHtmlString(xslt(query, element));
		html = html.replaceAll("\\{[^\\}<>]*\\}", "");
		html = html.replaceAll("(\\s)(content|resource|about)(=[\"'])\\?\\w+([\"'])", "$1$2$3$4");
		return html;
	}

	@Override
	public String calliConstructHTML(Object target) throws Exception {
		return calliConstructHTML(target, null);
	}
	
	@Override
	public String calliConstructHTML(Object target, String query)
			throws Exception {
		return asHtmlString(calliConstruct(target, query));
	}

	/**
	 * Extracts an element from the template (without variables) and populates
	 * the element with the properties of the about resource.
	 */
	@query("construct")
	@type("text/html")
	@header("cache-control:no-store")
	public InputStream calliConstruct(@query("about") URI about,
			@query("query") String query, @query("element") String element)
			throws Exception {
		if (about != null && (element == null || element.equals("/1")))
			throw new BadRequest("Missing element parameter");
		if (about == null && query == null && element == null) {
			ValueFactory vf = getObjectConnection().getValueFactory();
			about = vf.createURI(this.toString());
		}
		XMLEventReader xhtml = calliConstructXhtml(about, query, element);
		return HTML_XSLT.transform(xhtml, this.toString()).asInputStream();
	}

	private String asHtmlString(XMLEventReader xhtml) throws Exception {
		return HTML_XSLT.transform(xhtml, this.toString()).asString();
	}
	
	private XMLEventReader calliConstructXhtml(URI about, String query, String element) 
	throws Exception {
		ObjectConnection con = getObjectConnection();
		TupleQueryResult results;
		Map<String,String> origins;
		if (about==null) {
			List<String> names = Collections.emptyList();
			List<BindingSet> bindings = Collections.emptyList();
			results = new TupleQueryResultImpl(names, bindings.iterator());
			origins = Collections.emptyMap();
		}
		else { // evaluate SPARQL derived from the template
			String base = about.stringValue();
			String sparql = sparql(query, element);
			TupleQuery q = con.prepareTupleQuery(SPARQL, sparql, base);
			q.setBinding("this", about);
			results = q.evaluate();
			origins = SPARQLProducer.getOrigins(sparql);
		}
		return new RDFaProducer(xslt(query,element), results, origins, about, con);
	}

	private String sparql(String query, String element) throws IOException {
		InputStream in = request("sparql", query, element);
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ChannelUtil.transfer(in, out);
			return new String(out.toByteArray());
		} finally {
			in.close();
		}
	}

	/**
	 * Returns the given element with all known possible children.
	 * TODO limit the result set
	 */

	@query("options")
	@type("application/html")
	@header("cache-control:no-store")
//		public InputStream options
//	(@query("query") String query, @query("element") String element) throws Exception {
//		String base = toUri().toASCIIString();
//		BufferedXMLEventReader template = new BufferedXMLEventReader(xslt(query, element));
//		int n = template.mark();
//		TriplePatternStore patterns = readPatternStore(template,query, element, base);
//		TriplePattern pattern = patterns.getFirstTriplePattern();
//		RDFEventReader rq = patterns.selectBySubject(pattern.getPartner());
//		String sparql = toSPARQL(rq);
//		RepositoryConnection con = getObjectConnection();
//		TupleQuery q = con.prepareTupleQuery(SPARQL, sparql, base);
//		TupleQueryResult results = q.evaluate();
//		Map<String,String> origins = SPARQLProducer.getOrigins(sparql);
//		URI about = vf.createURI(base);
//		template.reset(n);
//		RDFaProducer xhtml = new RDFaProducer(template, results, origins, about, con);
//		
//		return HTML_XSLT.transform(xhtml, this.toString()).asInputStream();
//	}
	public InputStream options
	(@query("query") String query, @query("element") String element) throws Exception {
		String base = toUri().toASCIIString();
		BufferedXMLEventReader template = new BufferedXMLEventReader(xslt(query, element));
		template.mark();
		RDFEventReader rdfa = new RDFaReader(base, template, toString());
		SPARQLProducer rq = new SPARQLProducer(rdfa);
		SPARQLPosteditor ed = new SPARQLPosteditor(rq);
		// only pass object vars (excluding prop-exps and content) beyond a certain depth: 
		// ^(/\d+){3,}$|^(/\d+)*\s.*$
		ed.addMatcher(new SPARQLPosteditor.OriginMatcher(rq.getOrigins(),null,"^(/\\d+){3,}$|^(/\\d+)*\\s.*$"));
		String sparql = toSPARQL(ed);
		RepositoryConnection con = getObjectConnection();
		TupleQuery q = con.prepareTupleQuery(SPARQL, sparql, base);
		TupleQueryResult results = q.evaluate();
		URI about = vf.createURI(base);
		template.reset(0);
		RDFaProducer xhtml = new RDFaProducer(template, results, rq.getOrigins(), about, con);
		return HTML_XSLT.transform(xhtml, this.toString()).asInputStream();
	}

	/**
	 * Returns an HTML page listing suggested resources for the given element.
	 */
	@query("search")
	@type("application/rdf+xml")
	@header("cache-control:no-validate,max-age=60")
	public XMLEventReader constructSearch(@query("query") String query,
			@query("element") String element, @query("q") String q)
			throws Exception {
		String base = toUri().toASCIIString();
		TriplePatternStore patterns = readPatternStore(query, element, base);
		TriplePattern pattern = patterns.getFirstTriplePattern();
		patterns.consume(filterPrefix(patterns, pattern, q));
		RDFEventReader qry = constructPossibleTriples(patterns, pattern);
		ObjectConnection con = getObjectConnection();
		RDFEventReader rdf = new RDFStoreReader(toSPARQL(qry), patterns, con);
		return new RDFXMLEventReader(new ReducedTripleReader(rdf));
	}

	private TriplePatternStore readPatternStore(String query, String element,
			String about) throws XMLStreamException, IOException,
			TransformerException, RDFParseException {
		String base = toUri().toASCIIString();
		TriplePatternStore qry = new TriplePatternVariableStore(base);
		RDFEventReader reader = openPatternReader(about, query, element);
		try {
			qry.consume(reader);
		} finally {
			reader.close();
		}
		return qry;
	}
	
//	private TriplePatternStore readPatternStore(XMLEventReader xml, String query, String element,
//			String about) throws XMLStreamException, IOException,
//			TransformerException, RDFParseException {
//		String base = toUri().toASCIIString();
//		TriplePatternStore qry = new TriplePatternVariableStore(base);
//		RDFEventReader reader = openPatternReader(xml,about, query, element);
//		qry.consume(reader);
//		return qry;
//	}

	private IterableRDFEventReader filterPrefix(TriplePatternStore patterns,
			TriplePattern pattern, String q) {
		VarOrTerm obj = pattern.getPartner();
		PlainLiteral phone = tf.literal(asSoundex(q));
		String regex = regexStartsWith(q);
		List<RDFEvent> list = new ArrayList<RDFEvent>();
		list.add(new Subject(true, obj));
		list.add(new TriplePattern(obj, tf.iri(SOUNDEX), phone));
		boolean filter = false;
		for (String pred : LABELS) {
			IRI iri = tf.iri(pred);
			for (TriplePattern tp : patterns.getPatternsByPredicate(iri)) {
				if (tp.getAbout().equals(obj)) {
					if (filter) {
						list.add(new ConditionalOrExpression());
					} else {
						filter = true;
					}
					list.add(new BuiltInCall(true, "regex"));
					list.add(new BuiltInCall(true, "str"));
					list.add(new Expression(tp.getObject()));
					list.add(new BuiltInCall(false, "str"));
					list.add(new Expression(tf.literal(regex)));
					list.add(new Expression(tf.literal("i")));
					list.add(new BuiltInCall(false, "regex"));
				}
			}
		}
		list.add(new Subject(false, obj));
		return new IterableRDFEventReader(list);
	}

	public RDFEventReader constructPossibleTriples(TriplePatternStore patterns,
			TriplePattern pattern) {
		VarOrTerm subj = pattern.getPartner();
		return patterns.openQueryBySubject(subj);
	}

	
	private XMLEventReader xslt(String query, String element)
			throws IOException, XMLStreamException {
		XMLEventReaderFactory factory = XMLEventReaderFactory.newInstance();
		InputStream in = request("xslt", query, element);
		return factory.createXMLEventReader(in);
	}

	private InputStream request(String operation, String query, String element)
			throws IOException {
		String uri = getResource().stringValue();
		StringBuilder sb = new StringBuilder();
		sb.append(uri);
		sb.append("?");
		sb.append(operation);
		if (query != null) {
			sb.append("&query=");
			sb.append(query);
		}
		if (element != null) {
			sb.append("&element=");
			sb.append(element);
		}
		HTTPObjectClient client = HTTPObjectClient.getInstance();
		HttpRequest request = new BasicHttpRequest("GET", sb.toString());
		HttpResponse response = client.service(request);
		if (response.getStatusLine().getStatusCode() >= 300)
			throw ResponseException.create(response);
		InputStream in = response.getEntity().getContent();
		return in;
	}

}
