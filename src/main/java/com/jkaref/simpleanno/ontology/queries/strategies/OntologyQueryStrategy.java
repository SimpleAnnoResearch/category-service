package com.jkaref.simpleanno.ontology.queries.strategies;

import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import org.semanticweb.owlapi.model.OWLClass;

import java.util.Set;

/**
 * @author Matthias Muenzner <matthias.muenzner@jkaref.com>
 */
public interface OntologyQueryStrategy {

    /**
     * Lookup a set of successor classes for a given ontology class.
     *
     * @param c
     * @param reasoner
     * @return
     */
    Set<OWLClass> lookup(
            final OWLClass c,
            final PelletReasoner reasoner);

}
