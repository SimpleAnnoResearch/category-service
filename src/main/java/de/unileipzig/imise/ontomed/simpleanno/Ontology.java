package de.unileipzig.imise.ontomed.simpleanno;

import org.apache.clerezza.commons.rdf.IRI;


/**
 * Ideally this should be a dereferenceable ontology on the web. Given such 
 * an ontology a class of constant (similar to this) can be generated with
 * the org.apache.clerezza:maven-ontologies-plugin
 */
public class Ontology {
    /**
     * Resources of this type can be dereferenced and will return a description
     * of the resource of which the IRI is specified in the "iri" query parameter.
     * 
     */
    public static final IRI ResourceResolver = new IRI("http://simple-anno.de/service-description#CategoryService");
    
    /**
     * Point to the resource resolved by the subject.
     */
    public static final IRI describes = new IRI("http://simple-anno.de/service-description#describes");

    /**
     *
     */
    public static final IRI scopes = new IRI("http://simple-anno.de/service-description#scopes");

    /**
     * The description of a Request in the log.
     */
    public static final IRI LoggedRequest = new IRI("http://simple-anno.de/service-description#LoggedRequest");
    
    /**
     * The User Agent performing the requested described by the subject.
     */
    public static final IRI userAgent = new IRI("http://simple-anno.de/service-description#userAgent");
    
    /**
     * The Entity of which a description was requested in the request
     * described by the subject.
     */
    public static final IRI requestedEntity = new IRI("http://simple-anno.de/service-description#requestedEntity");
}
