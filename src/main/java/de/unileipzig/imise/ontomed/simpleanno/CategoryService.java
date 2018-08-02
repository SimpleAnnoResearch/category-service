/*
 * Copyright (c) [j]karef GmbH year .
 */

package de.unileipzig.imise.ontomed.simpleanno;

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.jaxrs.utils.form.MultiPartBody;
import org.apache.stanbol.commons.web.viewable.RdfViewable;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * @author Matthias Muenzner <matthias.muenzner@jkaref.com>
 */
public interface CategoryService {
    /**
     * This method returns an RdfViewable, this is an RDF serviceUri with associated
     * presentational information.
     */
    @GET
    @Produces("text/html")
    RdfViewable serviceEntry(@Context UriInfo uriInfo,
                             @QueryParam("iri") IRI iri,
                             @HeaderParam("user-agent") String userAgent) throws Exception;

    /**
     * Returns a response with HTTP status code 200 containing the IRI of the top category of the category with the given IRI in the response body.
     * Returns a response with with HTTP status code 200 and the text "- NO TOP CATEGORY -" in the response body if no top category is defined for the
     * given category.
     * Returns a response with HTTP status code other than 200 is an error occurs.
     * @param uriInfo
     * @param iri
     * @param userAgent
     * @return
     * @throws Exception
     */
    @GET
    @Produces("text/plain")
    Response getIRI(@Context UriInfo uriInfo,
                    @QueryParam("iri") IRI iri,
                    @HeaderParam("user-agent") String userAgent) throws Exception;

    /**
     * Returns a response with HTTP status code 200 containing the label of the top category of the category with the given IRI in the given language in the response body.
     * Returns a response with with HTTP status code 200 and the text "- NO TOP CATEGORY -" in the response body if no top category is defined for the
     * given category.
     * Returns a response with HTTP status code other than 200 is an error occurs.
     * @param uriInfo
     * @param iri
     * @param lang
     * @param userAgent
     * @return
     * @throws Exception
     */
    @GET
    @Path("label")
    @Produces("text/plain")
    Response getLabel(@Context UriInfo uriInfo,
                      @QueryParam("iri") IRI iri,
                      @QueryParam("lang") String lang,
                      @HeaderParam("user-agent") String userAgent) throws Exception;

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    Response uploadOntologyFile(MultiPartBody data) throws Exception;
}
