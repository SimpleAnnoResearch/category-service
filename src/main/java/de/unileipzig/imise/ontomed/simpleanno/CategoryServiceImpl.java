package de.unileipzig.imise.ontomed.simpleanno;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.jaxrs.utils.TrailingSlash;
import org.apache.clerezza.rdf.core.access.EntityAlreadyExistsException;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.access.security.TcAccessController;
import org.apache.clerezza.rdf.core.access.security.TcPermission;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.clerezza.rdf.ontologies.RDFS;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.indexedgraph.IndexedGraph;
import org.apache.stanbol.commons.web.viewable.RdfViewable;
import org.apache.stanbol.entityhub.servicesapi.site.SiteManager;
import org.apache.stanbol.ontologymanager.servicesapi.scope.Scope;
import org.apache.stanbol.ontologymanager.servicesapi.scope.ScopeManager;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.json.simple.JSONArray;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.expression.ParserException;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.BidirectionalShortFormProvider;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.semanticweb.owlapi.util.mansyntax.ManchesterOWLSyntaxParser;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import org.semanticweb.owlapi.vocab.SKOSVocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import com.jkaref.simpleanno.ontology.queries.strategies.LookupStrategies;
import com.jkaref.simpleanno.ontology.queries.strategies.OntologyPrimitves;
import com.jkaref.simpleanno.ontology.queries.strategies.OntologyQueryStrategy;
import com.jkaref.simpleanno.ontology.queries.strategies.OntologyQueryStrategyFactory;

import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl;

/**
 * @author Ralph Schaefermeier
 */
@Component
@Service(Object.class)
@Property(
        name = "javax.ws.rs",
        boolValue = true
)
@Path("category")
public class CategoryServiceImpl implements CategoryService {

    private static final Logger LOG = LoggerFactory.getLogger(CategoryServiceImpl.class);

    /**
     * This is the name of the graph in which we "LOG" the requests
     */
    private IRI REQUEST_LOG_GRAPH_NAME = new IRI(
            "http://example.org/resource-resolver-LOG.graph"
    );

    private static final org.semanticweb.owlapi.model.IRI rootOntologyAnnotationProperteryIRI =
            org.semanticweb.owlapi.model.IRI.create(
                    "http://simple-anno.de/ontologies/dental_care_process#rootOntology"
            );

    public static Map<OWLClass, Integer> topClassHits = Stream.of(
            "http://simple-anno.de/ontologies/dental_care_process#Anamnese",
            "http://glodmed.simple-anno.de/glodmed#1344",
            "http://simple-anno.de/ontologies/dental_care_process#Anatomie",
            "http://simple-anno.de/ontologies/dental_care_process#Ausblick",
            "http://simple-anno.de/ontologies/dental_care_process#Befund",
            "http://simple-anno.de/ontologies/dental_care_process#Dd",
            "http://simple-anno.de/ontologies/dental_care_process#Definitionen",
            "http://glodmed.simple-anno.de/glodmed#2985",
            "http://simple-anno.de/ontologies/dental_care_process#Diagnostik",
            "http://simple-anno.de/ontologies/dental_care_process#Er",
            "http://simple-anno.de/ontologies/dental_care_process#Indikation",
            "http://simple-anno.de/ontologies/dental_care_process#Kieferorthopädie",
            "http://simple-anno.de/ontologies/dental_care_process#Material",
            "http://simple-anno.de/ontologies/dental_care_process#Mikroorganismen",
            "http://simple-anno.de/ontologies/dental_care_process#Personen_und_Personengruppen",
            "http://simple-anno.de/ontologies/dental_care_process#Prozesse",
            "http://simple-anno.de/ontologies/dental_care_process#Risikofaktobewertung",
            "http://simple-anno.de/ontologies/dental_care_process#Studie",
            "http://simple-anno.de/ontologies/dental_care_process#Te",
            "http://simple-anno.de/ontologies/dental_care_process#Therapie"
    ).map(s ->
            new OWLClassImpl(
                    org.semanticweb.owlapi.model.IRI.create(s)
            )
    ).collect(
            Collectors.toMap(
                    c -> c,
                    c -> 0
            )
    );

    private static final String DEFAULT_SCOPE_ID = "SIMPLEANNO";

    /**
     * OSGi Bundle Activator
     *
     * @param context
     */
    @Activate
    protected void activate(ComponentContext context) {

        LOG.info("Activating SimpleAnno category service");

        try {

            tcManager.createGraph(REQUEST_LOG_GRAPH_NAME);

            //now make sure everybody can read from the graph
            //or more precisly, anybody who can read the content-graph
            TcAccessController tca = tcManager.getTcAccessController();
            tca.setRequiredReadPermissions(
                    REQUEST_LOG_GRAPH_NAME,
                    Collections.singleton(
                            new TcPermission(
                                    "urn:x-localinstance:/content.graph", "read"
                            )
                    )
            );

        } catch (EntityAlreadyExistsException ex) {

            LOG.debug("The graph for the request LOG already exists");

        }

        this.bundleContext = context.getBundleContext();

        registration = bundleContext.registerService(
                MultiPartFeature.class,
                new MultiPartFeature(),
                null
        );

        startMonitorOntologiesInBackground();

    }

    /**
     * OSGi Bundle Deactivator
     *
     * @param context
     */
    @Deactivate
    protected void deactivate(ComponentContext context) {

        LOG.info("Deactivating SimpleAnno category service");

        if (registration != null)
            registration.unregister();
    }


    @Override
    @GET
    @Produces(MediaType.TEXT_HTML)
    public RdfViewable serviceEntry(
            @Context final UriInfo uriInfo,
            @QueryParam("iri") final IRI iri,
            @HeaderParam("user-agent") String userAgent) throws Exception {

        LOG.info(String.format("[serviceEntry] Params: %s %s %s", uriInfo, iri, userAgent));

        //this maks sure we are nt invoked with a trailing slash which would affect
        //relative resolution of links (e.g. css)
        TrailingSlash.enforcePresent(uriInfo);
        final String resourcePath = uriInfo.getAbsolutePath().toString();

        //The URI at which this service was accessed accessed, this will be the
        //central serviceUri in the response
        final IRI serviceUri = new IRI(resourcePath);

        //the in memory graph to which the triples for the response are added
        final Graph responseGraph = new IndexedGraph();

        //This GraphNode represents the service within our result graph
        final GraphNode node = new GraphNode(serviceUri, responseGraph);

        //The triples will be added to the first graph of the union
        //i.e. to the in-memory responseGraph
        node.addProperty(RDF.type, Ontology.ResourceResolver);
        node.addProperty(RDFS.comment, new PlainLiteralImpl("A RDFTerm Resolver"));

        //What we return is the GraphNode we created with a template path
        return new RdfViewable("CategoryService", node, CategoryServiceImpl.class);
    }

    @Override
    @GET
    @Path("iri")
    @Produces({
            MediaType.APPLICATION_JSON
    })
    public Response getIRI(@Context final UriInfo uriInfo,
                           @QueryParam("iri") final IRI iri,
                           @QueryParam("scope") final String scopeID,
                           @HeaderParam("user-agent") String userAgent)
            throws Exception {

        LOG.info(String.format("[getIRI] Params: %s %s %s %s", uriInfo, iri, scopeID, userAgent));

        String effectiveScopeID = scopeID;
        if (effectiveScopeID == null)
            effectiveScopeID = DEFAULT_SCOPE_ID;

        final Optional<OntologyQueryStrategy> strategy = OntologyQueryStrategyFactory.create(
                LookupStrategies.fromValue(
                        uriInfo.getQueryParameters().getFirst("s")
                )
        );

        OWLOntology rootOntology = rootOntologiesByScope.get(
                effectiveScopeID
        );

        if (rootOntology == null) {
            return Response.status(
                    Response.Status.SERVICE_UNAVAILABLE
            ).entity(
                    String.format(
                            "No ontology found. Please upload at least the %s ontology into the '%s' scope.",
                            scopeID, scopeID
                    )
            ).build();
        }

        OWLClass clazz = new OWLClassImpl(
                org.semanticweb.owlapi.model.IRI.create(iri.getUnicodeString())
        );

        if (!rootOntology.containsClassInSignature(clazz.getIRI(), true)) {
            return Response.status(
                    Response.Status.BAD_REQUEST
            ).entity(
                    String.format("The ontology does not contain a class with IRI %s.", iri)
            ).build();
        }

        PelletReasoner reasoner = (PelletReasoner) getOrCreateReasoner(
                effectiveScopeID, rootOntology
        );

        Set<OWLClass> result = new HashSet();
        if (strategy.isPresent())
            result.addAll(strategy.get().lookup(clazz, reasoner));
        else
            result.add(getTopClass(clazz, reasoner).get());

        if (!result.isEmpty())
            return Response.ok(
                    toJson(
                            result.stream().map(s -> s.getIRI())
                                    .collect(Collectors.toList())
                    ).toJSONString()
            ).build();

        else
            return Response
                    .ok("- NO TOP CATEGORY -")
                    .build();

    }


    @Override
    @GET
    @Path("label")
    @Produces({
            MediaType.APPLICATION_JSON
    })
    public Response getLabel(
            @Context final UriInfo uriInfo,
            @QueryParam("scope") final String scopeID,
            @QueryParam("iri") final IRI iri,
            @QueryParam("lang") final String lang,
            @HeaderParam("user-agent") String userAgent) throws Exception {

        LOG.info(String.format("[getLabel] Params: %s %s %s %s", uriInfo, iri, scopeID, userAgent));

        String effectiveScopeID = scopeID;
        if (effectiveScopeID == null)
            effectiveScopeID = DEFAULT_SCOPE_ID;

        final Optional<OntologyQueryStrategy> strategy = OntologyQueryStrategyFactory.create(
                LookupStrategies.fromValue(
                        uriInfo.getQueryParameters().getFirst("s")
                )
        );

        boolean resolveRootClazz = "root".equalsIgnoreCase(
                uriInfo.getQueryParameters().getFirst("resolve")
        );

        OWLOntology rootOntology = rootOntologiesByScope.get(
                effectiveScopeID
        );

        if (rootOntology == null) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(
                    String.format("No ontology found. Please upload the %s ontology into the '%s' scope.", scopeID, scopeID)
            ).build();
        }

        OWLClass clazz = new OWLClassImpl(
                org.semanticweb.owlapi.model.IRI.create(
                        iri.getUnicodeString()
                )
        );
        if (!rootOntology.containsClassInSignature(clazz.getIRI(), true)) {
            return Response.status(
                    Response.Status.BAD_REQUEST
            ).entity(
                    String.format("The ontology does not contain a class with IRI %s.", iri)
            ).build();
        }

        PelletReasoner reasoner = (PelletReasoner) getOrCreateReasoner(
                effectiveScopeID, rootOntology
        );

        LOG.info("Selected reasoner: {}", reasoner);

        Set<OWLClass> result = new HashSet();
        if (strategy.isPresent())
            result.addAll(strategy.get().lookup(clazz, reasoner));
        else
            result.add(getTopClass(clazz, reasoner, resolveRootClazz).get());

        if (!result.isEmpty()) {
            return Response.ok(
                    toJson(
                            result.stream().map(
                                    s -> getLabel(
                                            rootOntology,
                                            s,
                                            lang
                                    )
                            ).collect(Collectors.toList())
                    ).toJSONString()
            ).build();
        } else {
            return Response.ok(
                    "- NO TOP CATEGORY -"
            ).build();
        }

    }

    @Override
    @GET
    @Path("query")
    @Produces({
            MediaType.APPLICATION_JSON
    })
    public Response executeDLQuery(
            @Context final UriInfo uriInfo,
            @QueryParam("scope") final String scopeID,
            @QueryParam("type") final String type,
            @QueryParam("dlquery") final String query,
            @QueryParam("direct") final boolean direct,
            @HeaderParam("user-agent") String userAgent) throws Exception {

        LOG.info(String.format("[executeDLQuery] Params: %s %s %s %s %s", uriInfo, scopeID, type, query, userAgent));

        String effectiveScopeID = scopeID;
        if (effectiveScopeID == null)
            effectiveScopeID = DEFAULT_SCOPE_ID;

        PelletReasoner reasoner = (PelletReasoner) getCachedReasoner(
                effectiveScopeID
        );

        if (reasoner == null) {
            return Response.status(
                    Response.Status.SERVICE_UNAVAILABLE
            ).entity(
                    String.format("A reasoner for '%s' scope does not exist.", scopeID)
            ).build();
        }

        OWLOntology rootOntology = reasoner.getRootOntology();

        if (rootOntology == null) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(
                    String.format("No root ontology. Please upload the an ontology into the '%s' scope.", scopeID)
            ).build();
        }

        BidirectionalShortFormProvider bidiShortFormProvider =
                shortFormProviders.get(effectiveScopeID);

        OWLDataFactory dataFactory = rootOntology
                .getOWLOntologyManager()
                .getOWLDataFactory();
        ManchesterOWLSyntaxParser parser = OWLManager.createManchesterParser();

//        dataFactory, query
        
        parser.setDefaultOntology(rootOntology);

        OWLEntityChecker entityChecker = new ShortFormEntityChecker(bidiShortFormProvider);
        parser.setOWLEntityChecker(entityChecker);

        OWLClassExpression classExpression = parser.parseClassExpression(query); 

        if ("instances".equalsIgnoreCase(type)) {
        	Stream<OWLNamedIndividual> instances = OntologyPrimitves.getInstances(classExpression, reasoner, direct);
            return Response.ok(createResponseBody(instances)).build();
        } else if ("subcategories".equalsIgnoreCase(type)) {
        	Stream<OWLClass> subclasses = OntologyPrimitves.getSubClasses(classExpression, reasoner, direct);
            return Response.ok(createResponseBody(subclasses)).build();
        } else if ("supercategories".equalsIgnoreCase(type)) {
        	Stream<OWLClass> superclasses = getSuperClasses(classExpression, reasoner, direct);
            return Response.ok(createResponseBody(superclasses)).build();
        } else if ("equivalentcategories".equalsIgnoreCase(type)) {
        	Stream<OWLClass> equivalentClasses = OntologyPrimitves.getEquivalentClasses(classExpression, reasoner);
            return Response.ok(createResponseBody(equivalentClasses)).build();
        } else {
            return Response.status(500).build();
        }

    }

    public Stream<OWLClass> getSuperClasses(OWLClassExpression classExpression, OWLReasoner reasoner, boolean direct) {
        NodeSet<OWLClass> superClasses = reasoner.getSuperClasses(classExpression, direct);
        return superClasses.entities();
    }

    @GET
    @Path("reload")
    @Produces({
            MediaType.TEXT_PLAIN
    })
    public Response reload(@Context final UriInfo uriInfo) {
        reloadOntologies();
        return Response.ok().build();
    }

    private String createResponseBody(Stream<? extends OWLEntity> entities) {
    	
    	// TODO Produce json output
    	
        StringBuilder buf = new StringBuilder();
        entities.forEach(owlEntity -> {
            if (!(owlEntity.isTopEntity()
                    || owlEntity.isBottomEntity())) {
                buf.append(owlEntity.getIRI().getFragment());
                buf.append("\n");
            }
        });
        return buf.toString();
    }

    private String getLabel(OWLOntology rootOntology, OWLClass cls, String lang) {

        String effectiveLang = (lang == null || lang.isEmpty())
                ? "de"
                : lang;

        StringBuilder buf = new StringBuilder();

        rootOntology
                .annotationAssertionAxioms(cls.getIRI())
                .filter(
                        annotationAxiom -> (annotationAxiom.getProperty().getIRI().equals(SKOSVocabulary.PREFLABEL.getIRI())
                                || annotationAxiom.getProperty().getIRI().equals(OWLRDFVocabulary.RDFS_LABEL.getIRI()))
                                && annotationAxiom.getValue() instanceof OWLLiteral
                                && ((OWLLiteral) annotationAxiom.getValue()).hasLang(effectiveLang))
                .forEach(annotationAxiom -> {
                    buf.append(((OWLLiteral) annotationAxiom.getValue()).getLiteral());
                    buf.append("\n");
                });

        return buf.toString();
    }

    /**
     * This works under the assumption that the CCO has also been uploaded to the entity hub.
     */
//    private void addResourceDescription(IRI iri, Graph mGraph) {
//        final Entity entity = siteManager.getEntity(iri.getUnicodeString());
//        if (entity != null) {
//            final RdfValueFactory valueFactory = new RdfValueFactory(mGraph);
//            final Representation representation = entity.getRepresentation();
//            if (representation != null) {
//                valueFactory.toRdfRepresentation(representation);
//            }
//            final Representation metadata = entity.getMetadata();
//            if (metadata != null) {
//                valueFactory.toRdfRepresentation(metadata);
//            }
//        }
//    }

    /**
     * @param c        an owl class
     * @param reasoner a pellet reasoner instance which is expected to be initialized with the CCO and imported GLODMED
     *                 ontology.
     * @return An Optional containing the top class of the given class according to the above table or Optional.empty()
     * if no such top class exists.
     */
    private Optional<OWLClass> getTopClass(
            OWLClass c,
            PelletReasoner reasoner) {
        return getTopClass(c, reasoner, true);
    }

    /**
     * Gets the topmost class of an ontological entry. (
     *
     * @param c
     * @param reasoner
     * @param resolveRootClazz
     * @return
     */
    private Optional<OWLClass> getTopClass(
            OWLClass c,
            PelletReasoner reasoner,
            boolean resolveRootClazz) {

        if (!resolveRootClazz)
            return Optional.of(c);
        else {
            if (topClassHits.containsKey(c))
                return Optional.of(c);

            Set<OWLClass> superClasses = reasoner
                    .getSuperClasses(c, true)
                    .getFlattened();

            for (OWLClass superClass : superClasses) {

                LOG.info("[getTopClass] Superclass: {} --> {}", c, superClass.toString());

                Optional<OWLClass> result = getTopClass(superClass, reasoner);

                if (result.isPresent())
                    return result;
                else
                    return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    /**
     * Gets or creates a reasoner and stores it in a Map of scope, reasoner entries
     * for efficiency reasons.
     *
     * @param scopeID
     * @param ontology
     * @return
     */
    private OWLReasoner getOrCreateReasoner(
            final String scopeID,
            final OWLOntology ontology) {
        OWLReasoner result = null;

        try {
            reasonersByScope.computeIfAbsent(
                    scopeID,
                    s -> PelletReasonerFactory
                            .getInstance()
                            .createReasoner(ontology)
            );
        } catch (Exception e) {
            LOG.warn("Cannot create reasoner without ontology");
        }

        result = reasonersByScope.get(scopeID);

        return result;
    }

    private OWLReasoner getCachedReasoner(final String scopeID) {
        return getOrCreateReasoner(scopeID, null);
    }


    private void startMonitorOntologiesInBackground() {
        final Thread thread = new Thread(this::monitorOntologies);
        thread.start();
    }

    private void monitorOntologies() {
        while (this.monitorShouldRun) {
            if (this.ontologiesDirty)
                reloadOntologies();

        }
    }

    /**
     * (Re-)loads the ontologies from Stanbol ontonet
     */
    private void reloadOntologies() {
        LOG.info("Start (re)-loading ontologies for all scopes...");
        scopeManager
                .getRegisteredScopes()
                .forEach(scope -> {
                    try {
                        reloadOntologyByScope(scope);
                    } catch (Exception e) {
                        LOG.error("Unable to load ontology in scope ", scope, e);
                    }
                });
        LOG.info("(Re)-loading of ontologies for all scopes done.");
        ontologiesDirty = false;

    }


    private void reloadOntologyByScopeId(String scopeID) {
        Scope scope = scopeManager.getScope(scopeID);

        if (scope == null) {
            LOG.error("No such scope: " + scopeID);
            return;
        }

        reloadOntologyByScope(scope);
    }

    private void reloadOntologyByScope(Scope scope) {
        String scopeID = scope.getID();

        LOG.info("Loading ontologies in scope '{}'", scopeID);

        OWLOntologyManager man = OWLManager
                .createOWLOntologyManager();

        ontologyManagersByScope.put(scopeID, man);

        scope
                .getCustomSpace()
                .listManagedOntologies()
                .parallelStream()
                .forEach(ontologyID -> {

                    LOG.info("Loading ontology with ID {}...",
                            ontologyID.getOntologyIRI().get().toQuotedString());

                    OWLOntology ontology = scope
                            .getCustomSpace()
                            .getOntology(
                                    ontologyID, OWLOntology.class
                            );

                    LOG.info(
                            "Finished loading ontology with ID {}",
                            ontologyID.getOntologyIRI().get().toQuotedString()
                    );

                    try {
                        OWLOntology newOntology = man.createOntology(
                                ontology.axioms(),
                                ontology.getOntologyID().getOntologyIRI().get()
                        );
                        if (newOntology.containsAnnotationPropertyInSignature(rootOntologyAnnotationProperteryIRI)
                                || rootOntologiesByScope.get(scopeID) == null) {

                            // Make sure each scope has a root ontology.
                            // We arbitrarily chose the ontology that's loaded
                            // first as the temporary root ontology.
                            // If another ontology is explicitly declared as being the
                            // root ontology, then this gets overwritten
                            // If several ontologies in the same scope are accidentally
                            // explicitly declared as being the root ontology, then the
                            // one that's loaded last is arbitrarily choses to be the
                            // root ontology.
                            rootOntologiesByScope.put(scopeID, newOntology);

                            LOG.info(
                                    "Setting {} as new root ontology for scope {}",
                                    ontologyID.getOntologyIRI().get().toQuotedString(),
                                    scopeID
                            );
                        }
                    } catch (OWLOntologyCreationException e) {
                        LOG.error("Error creating ontology " + ontologyID.getOntologyIRI().get().toQuotedString(), e);
                    }
                });

        OWLOntology rootOntology = rootOntologiesByScope.get(scopeID);

        if (rootOntology != null) {

            OWLOntologyManager manager = rootOntology.getOWLOntologyManager();
            Set<OWLOntology> importsClosure = man.getOntologies();

            for(OWLOntology ontology : importsClosure) {
            	if (ontology != rootOntology) {
            		man.applyChange(new AddImport(rootOntology, man.getOWLDataFactory().getOWLImportsDeclaration(ontology.getOntologyID().getOntologyIRI().get())));
            	}
            }
            
            PelletReasoner reasoner =
                    PelletReasonerFactory.getInstance()
                            .createReasoner(rootOntology);

            reasonersByScope.put(scopeID, reasoner);

            rootOntology = reasoner.getRootOntology(); // don't know if this is the same ontology, but just in case.

            shortFormProviders.put(
                    scopeID, new BidirectionalShortFormProviderAdapter(
                            manager,
                            importsClosure,
                            new SimpleShortFormProvider()
                    )
            );
        }
    }

    private JSONArray toJson(Collection c) {
        JSONArray result = new JSONArray();
        result.addAll(c);
        return result;
    }

    public SiteManager getSiteManager() {
        return siteManager;
    }

    public void setSiteManager(SiteManager siteManager) {
        this.siteManager = siteManager;
    }

    public TcManager getTcManager() {
        return tcManager;
    }

    public void setTcManager(TcManager tcManager) {
        this.tcManager = tcManager;
    }

    public ScopeManager getScopeManager() {
        return scopeManager;
    }

    public void setScopeManager(ScopeManager scopeManager) {
        this.scopeManager = scopeManager;
    }

    private boolean monitorShouldRun = true;
    private boolean ontologiesDirty = true;

    /**
     * OSGi Components
     */
    private ServiceRegistration registration;
    private BundleContext bundleContext;

    /**
     * This service allows to get entities from configures sites
     */
    @Reference
    private SiteManager siteManager;

    /**
     * This service allows accessing and creating persistent triple collections
     */
    @Reference
    private TcManager tcManager;

    /**
     * Service accessing Ontonet scopes
     */
    @Reference
    private ScopeManager scopeManager;


    /* use HashTable classes because of its synchronized access */

    private Map<String, OWLOntologyManager> ontologyManagersByScope =
            new Hashtable<>();

    private Map<String, OWLOntology> rootOntologiesByScope =
            new Hashtable<>();

    private Map<String, PelletReasoner> reasonersByScope =
            new Hashtable<>();

    private Map<String, BidirectionalShortFormProvider> shortFormProviders =
            new Hashtable<>();

}
