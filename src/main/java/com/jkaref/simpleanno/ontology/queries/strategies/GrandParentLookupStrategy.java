package com.jkaref.simpleanno.ontology.queries.strategies;

import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import org.semanticweb.owlapi.model.OWLClass;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Matthias Muenzner <matthias.muenzner@jkaref.com>
 */
public class GrandParentLookupStrategy implements OntologyQueryStrategy {
    /**
     * Lookup a set of successor classes for a given ontology class.
     *
     * @param c
     * @param reasoner
     * @return
     */
    @Override
    public Set<OWLClass> lookup(final OWLClass c, final PelletReasoner reasoner) {

        return reasoner
                .getSuperClasses(
                        c, true
                ).getFlattened()
                .stream()
                .flatMap(
                        p -> reasoner
                                .getSuperClasses(p, true)
                                .getFlattened()
                                .stream()
                ).collect(
                        Collectors.toSet()
                );

    }
}
