package de.unileipzig.ontomed.simpleanno.categoryextractor;

import org.osgi.service.component.annotations.Component;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;


/**
 * @author Ralph Schaefermeier
 */
@Component(immediate = true,
        service = Object.class,
        property = "javax.ws.rs:Boolean=true")
@Path("/category")
public class App {

    @GET
    public OWLClass getUpperCategory(@QueryParam("iri") String iri) {
        return new OWLClassImpl(IRI.create(iri + "-test"));
    }
}
