<@namespace ont="http://example.org/service-description#" />
<@namespace ehub="http://stanbol.apache.org/ontology/entityhub/entityhub#" />
<@namespace cc="http://creativecommons.org/ns#" />
<@namespace dct="http://purl.org/dc/terms/" />

<html>
  <head>
    <title>SimpleAnno Category Service</title>
    <link type="text/css" rel="stylesheet" href="styles/category-service.css" />
  </head>

  <body>
    <h1>Lookup Top Domain Category</h1>
    
    <form action="<@ldpath path="."/>">
        IRI of the Content Categorization Ontology (CCO) entity to look up (e.g. http://simple-anno.de/ontologies/dental_care_process#Notfallbehandlung) <br/>
        <label for="iri" /><input type="text" name="iri" 
                value="<@ldpath path="ont:describes"/>" size="90"/><br/>
        <input type="submit" value="look up" />
    </form>

    <@ldpath path="

    <@ldpath path="ont:describes">
        <p>
        Note: you can also get an rdf representation of this description 
        by setting a respective Accept header, e.g.<br/>
        <code>curl -H "Accept: text/turtle" <@ldpath path="^ont:describes"/>?iri=<@ldpath path="."/></code>
        <#if evalLDPath("rdfs:label")??>
            <h2>Resource Description</h2>
            <h3>Labels:</h3>
            <ul>
            <@ldpath path="rdfs:label"><li><@ldpath path="."/></li></@ldpath>
            </ul>
            <h3>Comment</h3>
            <div><@ldpath path="rdfs:comment"/></div>
            <@ldpath path="^ehub:about">
                <h2>Resource Metadata</h2>
                <div>
                Is cached locally: <@ldpath path="ehub:isChached"/> 
                </div>
                <div>
                License: <@ldpath path="dct:license"/> 
                </div>
                <div>
                Attribution URL: <a href="<@ldpath path="cc:attributionURL"/>" >
                <@ldpath path="cc:attributionURL"/></a> 
                </div>
            </@ldpath>
        </#if>
        <h2>Upload CCO or GLODMED Ontology</h2>
        </p>
    </@ldpath>
    <form action="<@ldpath path="."/>" method="post" enctype="multipart/form-data">
        Select the ontology files<br/>
        <!--label for="cco">CCO</label><input type="file" name="cco" /><br/>
        <label for="glodmed">GLODMED</label><input type="file" name="glodmed" /><br/>
        <label for="locations">Anatomy Ontology</label><input type="file" name="locations" /><br/>
        <label for="gfo">General Formal Ontology (GFO)</label><input type="file" name="gfo" /><br/-->
         <label for="files">GLODMED</label><input type="file" name="files" multiple /><br/>
        <input type="submit" value="Upload" />
    </form>
    <#include "/html/includes/footer.ftl">
  </body>
</html>

