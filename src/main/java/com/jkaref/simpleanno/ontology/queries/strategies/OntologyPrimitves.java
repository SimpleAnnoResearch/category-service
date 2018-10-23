package com.jkaref.simpleanno.ontology.queries.strategies;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import java.util.Set;
import java.util.stream.Stream;

/**
 * @author Matthias Muenzner <matthias.muenzner@jkaref.com>
 */
public class OntologyPrimitves {

    /**
     * @param classExpression
     * @param reasoner
     * @return
     */
    public static Stream<OWLClass> getEquivalentClasses(
            final OWLClassExpression classExpression,
            final OWLReasoner reasoner) {

        Node<OWLClass> equivalentClasses =
                reasoner.getEquivalentClasses(classExpression);

        Stream<OWLClass> result;

        if (classExpression.isAnonymous())
            result = equivalentClasses.entities();

        else
            result = equivalentClasses.getEntitiesMinus(
                    classExpression.asOWLClass()
            ).stream();

        return result;
    }

    /**
     * @param classExpression
     * @param reasoner
     * @param direct
     * @return
     */
    public static Stream<OWLClass> getSubClasses(
            final OWLClassExpression classExpression,
            OWLReasoner reasoner, boolean direct) {

        NodeSet<OWLClass> subClasses = reasoner.getSubClasses(
                classExpression,
                direct
        );

        return subClasses.entities();
    }

    /**
     * @param classExpression
     * @param reasoner
     * @param direct
     * @return
     */
    public static Stream<OWLNamedIndividual> getInstances(
            final OWLClassExpression classExpression,
            final OWLReasoner reasoner,
            boolean direct) {

        NodeSet<OWLNamedIndividual> individuals = reasoner.getInstances(
                classExpression,
                direct
        );

        return individuals.entities();
    }
}
