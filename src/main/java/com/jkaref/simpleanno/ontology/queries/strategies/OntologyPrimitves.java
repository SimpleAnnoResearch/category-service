package com.jkaref.simpleanno.ontology.queries.strategies;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import java.util.Set;

/**
 * @author Matthias Muenzner <matthias.muenzner@jkaref.com>
 */
public class OntologyPrimitves {

    /**
     * @param classExpression
     * @param reasoner
     * @return
     */
    public static Set<OWLClass> getEquivalentClasses(
            final OWLClassExpression classExpression,
            final OWLReasoner reasoner) {

        Node<OWLClass> equivalentClasses =
                reasoner.getEquivalentClasses(classExpression);

        Set<OWLClass> result;

        if (classExpression.isAnonymous())
            result = equivalentClasses.getEntities();

        else
            result = equivalentClasses.getEntitiesMinus(
                    classExpression.asOWLClass()
            );

        return result;
    }

    /**
     * @param classExpression
     * @param reasoner
     * @param direct
     * @return
     */
    public static Set<OWLClass> getSubClasses(
            final OWLClassExpression classExpression,
            OWLReasoner reasoner, boolean direct) {

        NodeSet<OWLClass> subClasses = reasoner.getSubClasses(
                classExpression,
                direct
        );

        return subClasses.getFlattened();
    }

    /**
     * @param classExpression
     * @param reasoner
     * @param direct
     * @return
     */
    public static Set<OWLNamedIndividual> getInstances(
            final OWLClassExpression classExpression,
            final OWLReasoner reasoner,
            boolean direct) {

        NodeSet<OWLNamedIndividual> individuals = reasoner.getInstances(
                classExpression,
                direct
        );

        return individuals.getFlattened();
    }
}
