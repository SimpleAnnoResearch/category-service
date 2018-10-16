package com.jkaref.simpleanno.ontology.queries.strategies;

import java.util.Optional;

/**
 * @author Matthias Muenzner <matthias.muenzner@jkaref.com>
 */
public class OntologyQueryStrategyFactory {

    /**
     *
     * @param s
     * @return
     */
    public static Optional<OntologyQueryStrategy> create(final LookupStrategies s) {

        OntologyQueryStrategy result = null;

        switch (s) {

            case UNCLE:
                result = new UncleLookupStrategy();
                break;

            case PARENT:
                result = new ParentLookupStrategy();
                break;

            case CHILD:
                result = new ChildLookupStrategy();
                break;

            case GRANDPARENT:
                result = new GrandParentLookupStrategy();
                break;

            default:
            case UNKNOWN:
                result = new SelfLookupStrategy();
                break;

        }

        return Optional.ofNullable(result);
    }
}
