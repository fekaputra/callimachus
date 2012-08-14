<?xml version="1.0" encoding="UTF-8" ?>
<p:pipeline version="1.0"
        xmlns:p="http://www.w3.org/ns/xproc"
        xmlns:c="http://www.w3.org/ns/xproc-step"
        xmlns:l="http://xproc.org/library"
        xmlns:cx="http://xmlcalabash.com/ns/extensions"
        xmlns:calli="http://callimachusproject.org/rdf/2009/framework#"
        xmlns:sparql="http://www.w3.org/2005/sparql-results#">

    <p:serialization port="result" media-type="text/html" method="html" doctype-system="about:legacy-compat" />

    <p:option name="systemId" required="true" />

    <p:variable name="find-realm-uri" select="concat('../queries/find-realm.rq?results&amp;this=', encode-for-uri($systemId))" />
    <p:variable name="realm" select="doc($find-realm-uri)//sparql:uri" />

    <p:xslt>
        <p:input port="stylesheet">
            <p:document href="../transforms/article-view.xsl" />
        </p:input>
    </p:xslt>

    <p:xslt>
        <p:with-param name="systemId" select="$systemId"/>
        <p:with-param name="xsltId" select="resolve-uri('../template.xsl')" />
        <p:with-param name="realm" select="$realm" />
        <p:input port="stylesheet">
            <p:document href="../template.xsl" />
        </p:input>
    </p:xslt>

    <p:xslt>
        <p:with-param name="this" select="$systemId"/>
        <p:with-param name="query" select="'view'"/>
        <p:input port="stylesheet">
            <p:document href="../transforms/page.xsl" />
        </p:input>
    </p:xslt>
    <p:xslt>
        <p:input port="stylesheet">
            <p:document href="../transforms/xhtml-to-html.xsl" />
        </p:input>
    </p:xslt>

</p:pipeline>