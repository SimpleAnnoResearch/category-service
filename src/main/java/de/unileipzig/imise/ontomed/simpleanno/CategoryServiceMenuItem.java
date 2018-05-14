package de.unileipzig.imise.ontomed.simpleanno;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.web.base.NavigationLink;

@Component
@Service(NavigationLink.class)
public class CategoryServiceMenuItem extends NavigationLink {
    
    public CategoryServiceMenuItem() {
        super("category/", "/category", "A service for looking up the Content Categorization Ontology (CCO) top category for a given category IRI", 300);
    }
    
}
