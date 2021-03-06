/*
 * Copyright (c) [j]karef GmbH year .
 */

package de.unileipzig.imise.ontomed.simpleanno;

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.stanbol.commons.web.viewable.RdfViewable;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * @author Matthias Muenzner <matthias.muenzner@jkaref.com>
 * TODO Ralph: Separate ontology upload and specific services. Rename project to SimpleAnno-Services
 */
public interface CategoryService {

    /**
     * This method returns an RdfViewable, this is an RDF serviceUri with associated
     * presentational information.
     */
    RdfViewable serviceEntry(@Context UriInfo uriInfo,
                             @QueryParam("iri") IRI iri,
                             @HeaderParam("user-agent") String userAgent) throws Exception;

    /**
     * Returns a response with HTTP status code 200 containing the IRI of the top category of the category with the given IRI in the response body.
     * Returns a response with with HTTP status code 200 and the text "- NO TOP CATEGORY -" in the response body if no top category is defined for the
     * given category.
     * Returns a response with HTTP status code other than 200 is an error occurs.
     *
     * @param uriInfo
     * @param iri
     * @param userAgent
     * @return
     * @throws Exception
     */
    Response getIRI(@Context UriInfo uriInfo,
                    @QueryParam("iri") IRI iri,
                    @QueryParam("scope") final String scopeID,
                    @HeaderParam("user-agent") String userAgent) throws Exception;

    /**
     * Returns a response with HTTP status code 200 containing the
     * label of the top category of the category with the given IRI in the
     * given language in the response body.
     * Returns a response with with HTTP status code 200 and the text
     * "- NO TOP CATEGORY -" in the response body if no top category is
     * defined for the
     * given category.
     * Returns a response with HTTP status code other than 200 is an error occurs.
     *
     * @param uriInfo
     * @param iri
     * @param lang
     * @param userAgent
     * @return
     * @throws Exception
     */
    Response getLabel(
            @Context UriInfo uriInfo,
            	      @QueryParam("scope") final String scopeID,
                      @QueryParam("iri") IRI iri,
                      @QueryParam("lang") String lang,
                      @HeaderParam("user-agent") String userAgent) throws Exception;
    
    /**
     * @param uriInfo
     * @param scopeID
     * @param type
     * @param query
     * @param userAgent
     * @return
     * @throws Exception
     */
    Response executeDLQuery(@Context final UriInfo uriInfo,
				         @QueryParam("scope") final String scopeID,
				         @QueryParam("type") final String type,
		                 @QueryParam("dlquery") final String query,
		                 @QueryParam("direct") final boolean direct,
		                 @HeaderParam("user-agent") String userAgent) throws Exception;

}
