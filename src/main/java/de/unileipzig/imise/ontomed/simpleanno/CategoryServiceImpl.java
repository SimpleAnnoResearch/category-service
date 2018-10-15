package de.unileipzig.imise.ontomed.simpleanno;

import java.security.Permission;
import java.util.Collections;
import java.util.HashMap;
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
import org.apache.stanbol.entityhub.model.clerezza.RdfValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.site.SiteManager;
import org.apache.stanbol.ontologymanager.servicesapi.scope.Scope;
import org.apache.stanbol.ontologymanager.servicesapi.scope.ScopeManager;
import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxEditorParser;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.expression.ParserException;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.BidirectionalShortFormProvider;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import org.semanticweb.owlapi.vocab.SKOSVocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;

import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl;

/**
 * @author Ralph Schaefermeier
 */
@Component
@Service(Object.class)
@Property(name="javax.ws.rs", boolValue=true)
@Path("category")
public class CategoryServiceImpl implements CategoryService {

    private static final org.semanticweb.owlapi.model.IRI ccoOntologyIRI = org.semanticweb.owlapi.model.IRI.create("http://simple-anno.de/ontologies/dental_care_process");
    private static final org.semanticweb.owlapi.model.IRI glodmedIRI = org.semanticweb.owlapi.model.IRI.create("http://glodmed.simple-anno.de/glodmed#");

    private static final String ROOT_PREFIX = "__root__";
    
    private boolean ontologiesDirty = true;

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
    ).map(s -> new OWLClassImpl(org.semanticweb.owlapi.model.IRI.create(s))).collect(Collectors.toMap(c -> c, c -> 0));

    private static final Logger log = LoggerFactory.getLogger(CategoryServiceImpl.class);

    private ServiceRegistration registration;
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

    @Reference
    private ScopeManager scopeManager;

    private BundleContext bundleContext;

//    private File ccoOntologyFile, glodmedOntologyFile, anatomyOntologyFile, gfoOntologyFile;
//    private File ontologyStorageFolder = bundleContext.getDataFile("ontologies");

//    private Set<OWLOntologyIRIMapper> mappers;

    private static final org.semanticweb.owlapi.model.IRI rootOntologyAnnotationProperteryIRI = org.semanticweb.owlapi.model.IRI.create("http://simple-anno.de/ontologies/dental_care_process#rootOntology"); 
    
//    private OWLOntologyManager man = OWLManager.createOWLOntologyManager();

//    private OWLOntology ccoOntology;
//    private OWLOntology anatomyOntology;
//    private PelletReasoner reasoner;

//    private TreeSet<File> ontologyFoldersByChangeDate = new TreeSet<>((file1, file2) -> ((Long)file1.lastModified()).compareTo((Long)file2.lastModified()));
//    private TreeMap<File, OWLOntologyManager> ontologyManagers = new TreeMap<>();
    
    private HashMap<String, OWLOntologyManager> ontologyManagersByScope = new HashMap<>();
    private HashMap<String, OWLOntology> rootOntologiesByScope = new HashMap<>();
    private HashMap<String, PelletReasoner> reasonersByScope = new HashMap<>();
    private HashMap<String, BidirectionalShortFormProvider> shortFormProviders = new HashMap<>();

//    private TreeMap<String, OWLOntology> ontologiesByFolder = new TreeMap<>();

    /**
     * This is the name of the graph in which we "log" the requests
     */
    private IRI REQUEST_LOG_GRAPH_NAME = new IRI("http://example.org/resource-resolver-log.graph");
    
    @Activate
    protected void activate(ComponentContext context) {
        log.info("Activating SimpleAnno category service");
        try {
            tcManager.createGraph(REQUEST_LOG_GRAPH_NAME);
            //now make sure everybody can read from the graph
            //or more precisly, anybody who can read the content-graph
            TcAccessController tca = tcManager.getTcAccessController();
            tca.setRequiredReadPermissions(REQUEST_LOG_GRAPH_NAME, 
                    Collections.singleton((Permission)new TcPermission(
                    "urn:x-localinstance:/content.graph", "read")));
        } catch (EntityAlreadyExistsException ex) {
            log.debug("The graph for the request log already exists");
        }

        this.bundleContext = context.getBundleContext();

        registration = bundleContext.registerService( MultiPartFeature.class, new MultiPartFeature(), null );

//
//
//        ccoOntologyFile = bundleContext.getDataFile("cco.owl");
//        glodmedOntologyFile = bundleContext.getDataFile("glodmed.owl");
//        anatomyOntologyFile = bundleContext.getDataFile("locations.owl");
//        gfoOntologyFile = bundleContext.getDataFile("gfo-basic.owl");
//
//        mappers = Stream.of(
//                new SimpleIRIMapper(org.semanticweb.owlapi.model.IRI.create("http://simple-anno.de/ontologies/dental_care_process"), org.semanticweb.owlapi.model.IRI.create(ccoOntologyFile)),
//                new SimpleIRIMapper(org.semanticweb.owlapi.model.IRI.create("http://glodmed.simple-anno.de/glodmed#"), org.semanticweb.owlapi.model.IRI.create(glodmedOntologyFile)),
//                new SimpleIRIMapper(org.semanticweb.owlapi.model.IRI.create("http://www.simple-anno.de/locations"), org.semanticweb.owlapi.model.IRI.create(anatomyOntologyFile)),
//                new SimpleIRIMapper(org.semanticweb.owlapi.model.IRI.create("http://www.onto-med.de/ontologies/gfo-basic.owl"), org.semanticweb.owlapi.model.IRI.create(gfoOntologyFile))
//        ).collect(Collectors.toSet());
//
//        man.setIRIMappers(mappers);

        if (ontologiesDirty) {
        	new Thread(new Runnable() {				
				@Override
				public void run() {
		        	reloadOntologies();
				}
			}).start();
        	
        }
    }
    
    @Deactivate
    protected void deactivate(ComponentContext context) {
        log.info("Deactivating SimpleAnno category service");
        if( registration != null ) {
            registration.unregister();
        }
    }

    @Override
    @GET
    @Produces("text/html")
    public RdfViewable serviceEntry(@Context final UriInfo uriInfo,
                                    @QueryParam("iri") final IRI iri,
                                    @HeaderParam("user-agent") String userAgent) throws Exception {
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
//        if (iri != null && ccoOntology != null) {
//            OWLClass clazz = new OWLClassImpl(org.semanticweb.owlapi.model.IRI.create(iri.getUnicodeString()));
//            if (ccoOntology.containsClassInSignature(clazz.getIRI(), Imports.INCLUDED)) {
//                Optional<OWLClass> topClass = getTopClass(clazz, reasoner);
//
//                if (topClass.isPresent()) {
//                    IRI topClassIRI = new IRI(topClass.get().getIRI().toString());
//                    node.addProperty(Ontology.describes, topClassIRI);
//                    addResourceDescription(topClassIRI, responseGraph);
//                }
//            }
//        }
        //What we return is the GraphNode we created with a template path
        return new RdfViewable("CategoryService", node, CategoryServiceImpl.class);
    }

    @Override
    @GET
    @Produces("text/plain")
    public Response getIRI(@Context final UriInfo uriInfo,
                           @QueryParam("iri") final IRI iri,
                           @HeaderParam("user-agent") String userAgent) throws Exception {
    	
    	OWLOntology ccoOntology = rootOntologiesByScope.get("cco");
    	
        if (ccoOntology == null) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("No CCO ontology. Please upload the CCO ontology into the 'cco' scope.").build();
        }

        OWLClass clazz = new OWLClassImpl(org.semanticweb.owlapi.model.IRI.create(iri.getUnicodeString()));
        if (!ccoOntology.containsClassInSignature(clazz.getIRI(), true)) {
            return Response.status(Response.Status.BAD_REQUEST).entity(String.format("The ontology does not contain a class with IRI %s.", iri)).build();
        }
        
        PelletReasoner reasoner = reasonersByScope.get("cco");

        Optional<OWLClass> topClass = getTopClass(clazz, reasoner);
        if (topClass.isPresent()) {
            return Response.ok(topClass.get().getIRI().toString()).build();
        } else {
            return Response.ok("- NO TOP CATEGORY -").build();
        }
    }

    @Override
    @GET
    @Path("label")
    @Produces("text/plain")
    public Response getLabel(@Context final UriInfo uriInfo,
				         @QueryParam("scope") final String scopeID,
				         @QueryParam("iri") final IRI iri,
		                 @QueryParam("lang") final String lang,
		                 @HeaderParam("user-agent") String userAgent) throws Exception {
    	
    	OWLOntology rootOntology = rootOntologiesByScope.get(scopeID);
    	
        if (rootOntology == null) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(String.format("No CCO ontology. Please upload the CCO ontology into the '%s' scope.", scopeID)).build();
        }

        OWLClass clazz = new OWLClassImpl(org.semanticweb.owlapi.model.IRI.create(iri.getUnicodeString()));
        if (!rootOntology.containsClassInSignature(clazz.getIRI(), true)) {
            return Response.status(Response.Status.BAD_REQUEST).entity(String.format("The ontology does not contain a class with IRI %s.", iri)).build();
        }

        PelletReasoner reasoner = reasonersByScope.get(scopeID);
        
        Optional<OWLClass> topClass = getTopClass(clazz, reasoner);
        if (topClass.isPresent()) {
            return Response.ok(getLabel(rootOntology, topClass.get(), lang)).build();
        } else {
            return Response.ok("- NO TOP CATEGORY -").build();
        }
    }
    
    @Override
    @GET
    @Path("query")
    @Produces("text/plain")
    public Response executeDLQuery(@Context final UriInfo uriInfo,
				         @QueryParam("scope") final String scopeID,
				         @QueryParam("type") final String type,
		                 @QueryParam("dlquery") final String query,
		                 @QueryParam("direct") final boolean direct,
		                 @HeaderParam("user-agent") String userAgent) throws Exception {
    	
    	PelletReasoner reasoner = reasonersByScope.get(scopeID);
    	if (reasoner == null) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(String.format("The '%s' scope does not exist.", scopeID)).build();
    	}
    	
    	OWLOntology rootOntology = reasoner.getRootOntology();
        if (rootOntology == null) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(String.format("No root ontology. Please upload the an ontology into the '%s' scope.", scopeID)).build();
        }

        BidirectionalShortFormProvider bidiShortFormProvider = shortFormProviders.get(scopeID);
        		
        OWLDataFactory dataFactory = rootOntology.getOWLOntologyManager()
                .getOWLDataFactory();
        ManchesterOWLSyntaxEditorParser parser = new ManchesterOWLSyntaxEditorParser(
                dataFactory, query);
//        parser.setBase(rootOntology.getOntologyID().toString());
        parser.setDefaultOntology(rootOntology);
        OWLEntityChecker entityChecker = new ShortFormEntityChecker(bidiShortFormProvider);
        parser.setOWLEntityChecker(entityChecker);
        OWLClassExpression classExpression = parser.parseClassExpression();
        
        if ("instances".equalsIgnoreCase(type)) {
        	Set<OWLNamedIndividual> instances = getInstances(classExpression, reasoner, direct);
            return Response.ok(createResponseBody(instances)).build();
        } else if ("subclasses".equalsIgnoreCase(type)) {
        	Set<OWLClass> subclasses = getSubClasses(classExpression, reasoner, direct);
            return Response.ok(createResponseBody(subclasses)).build();
        } else if ("superclasses".equalsIgnoreCase(type)) {
        	Set<OWLClass> superclasses = getSuperClasses(classExpression, reasoner, direct);
            return Response.ok(createResponseBody(superclasses)).build();
        } else if ("equivalentclasses".equalsIgnoreCase(type)) {
        	Set<OWLClass> equivalentClasses = getEquivalentClasses(classExpression, reasoner);
            return Response.ok(createResponseBody(equivalentClasses)).build();
        } else {
            return Response.status(500).build();
        }
        
    }
    
	public Set<OWLClass> getSuperClasses(OWLClassExpression classExpression, OWLReasoner reasoner, boolean direct)
			throws ParserException {
		NodeSet<OWLClass> superClasses = reasoner.getSuperClasses(classExpression, direct);
		return superClasses.getFlattened();
	}

	public Set<OWLClass> getEquivalentClasses(OWLClassExpression classExpression, OWLReasoner reasoner)
			throws ParserException {
		Node<OWLClass> equivalentClasses = reasoner.getEquivalentClasses(classExpression);
		Set<OWLClass> result = null;
		if (classExpression.isAnonymous()) {
			result = equivalentClasses.getEntities();
		} else {
			result = equivalentClasses.getEntitiesMinus(classExpression.asOWLClass());
		}
		return result;
	}

	public Set<OWLClass> getSubClasses(OWLClassExpression classExpression, OWLReasoner reasoner, boolean direct)
			throws ParserException {
		NodeSet<OWLClass> subClasses = reasoner.getSubClasses(classExpression, direct);
		return subClasses.getFlattened();
	}

	public Set<OWLNamedIndividual> getInstances(OWLClassExpression classExpression, OWLReasoner reasoner,
			boolean direct) throws ParserException {
		NodeSet<OWLNamedIndividual> individuals = reasoner.getInstances(classExpression, direct);
		return individuals.getFlattened();
	}
	
	private String createResponseBody(Set<? extends OWLEntity> entities) {
		StringBuilder buf = new StringBuilder();
		for (OWLEntity owlEntity : entities) {
			buf.append(owlEntity.getIRI().getFragment());
			buf.append("\n");
		}
		return buf.toString();
	}
	

    private String getLabel(OWLOntology rootOntology, OWLClass cls, String lang) {
        String actualLang = lang == null || lang.isEmpty() ? "de" : lang;
        StringBuilder buf = new StringBuilder();
        rootOntology.getAnnotationAssertionAxioms(cls.getIRI()).stream().filter(
                annotationAxiom -> (annotationAxiom.getProperty().getIRI().equals(SKOSVocabulary.PREFLABEL.getIRI())
                        || annotationAxiom.getProperty().getIRI().equals(OWLRDFVocabulary.RDFS_LABEL.getIRI()))
        && annotationAxiom.getValue() instanceof OWLLiteral
        && ((OWLLiteral)annotationAxiom.getValue()).hasLang(lang)).forEach(annotationAxiom -> {
            buf.append(((OWLLiteral)annotationAxiom.getValue()).getLiteral());
            buf.append("\n");
        });
        return buf.toString();
    }

//    @Override
//    @POST
//    @Consumes(MediaType.MULTIPART_FORM_DATA)
//    public Response uploadOntologyFile(MultiPartBody data) throws Exception {
//
//        Stream.of("cco", "glodmed", "locations", "gfo").forEach(name -> {
//            Reader body;
//
//            FormFile[] sentFiles = data.getFormFileParameterValues(name);
//            if (sentFiles.length != 0) {
//                FormFile file = sentFiles[0];
//                body = new InputStreamReader(new ByteArrayInputStream(file.getContent()));
//            } else {
//                ParameterValue[] paramValues = data.getParameteValues(name);
//                if (paramValues.length != 0) {
//                    StringParameterValue fileValue = (StringParameterValue) paramValues[0];
//                    body = new StringReader(fileValue.toString());
//                } else {
//                    // TODO should we treat a missing ontology file as an error? It might be convenient for the user to be able to update a single ontology file
//                    //return Response.status(Response.Status.BAD_REQUEST).entity("There was no ontology file with field name 'file' in the request body.").build();
//                    return;
//                }
//            }
//
//            OWLOntologyManager om = OWLManager.createOWLOntologyManager();
//            try {
//                OWLOntology onto = om.loadOntologyFromOntologyDocument(new ReaderDocumentSource(body), new OWLOntologyLoaderConfiguration().setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT));
//                String ontologyID = onto.getOntologyID().getOntologyIRI().get().toString();
//
//                scopeManager.getScope("simpleAnno").getCustomSpace().addOntology(new StoredOntologySource());
//
//                man.getIRIMappers().forEach(mapper -> {
//                    if (ontologyID.equals(mapper.)) {
//                        om.saveOntology(onto, new FileOutputStream(ccoOntologyFile));
//                    }                });
//                if (ontologyID.equals("http://simple-anno.de/ontologies/dental_care_process")) {
//                    om.saveOntology(onto, new FileOutputStream(ccoOntologyFile));
//                } else if (ontologyID.equals("http://glodmed.simple-anno.de/glodmed#")) {
//                    om.saveOntology(onto, new FileOutputStream(glodmedOntologyFile));
//                } else {
//                    return Response.status(Response.Status.BAD_REQUEST).entity("Unexpected ontology ID (expected: <http://simple-anno.de/ontologies/dental_care_process> or <http://glodmed.simple-anno.de/glodmed#>)").build();
//                }
//                reloadOntologies();
//                return Response.seeOther(URI.create("category")).build();
//            } catch (OWLOntologyCreationException ex) {
//                log.error("Error while saving ontology file.", ex);
//                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("An error occured. " + ex.getMessage()).build();
//            } catch (FileNotFoundException ex) {
//                log.error("Error while saving ontology file.", ex);
//                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("An error occured. " + ex.getMessage()).build();
//            } catch (OWLOntologyStorageException ex) {
//                log.error("Error while saving ontology file.", ex);
//                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("An error occured. " + ex.getMessage()).build();
//            }
//        });
//
//    }

    /**
     * This works under the assumption that the CCO has also been uploaded to the entity hub.
     *
     */
    private void addResourceDescription(IRI iri, Graph mGraph) {
        final Entity entity = siteManager.getEntity(iri.getUnicodeString());
        if (entity != null) {
            final RdfValueFactory valueFactory = new RdfValueFactory(mGraph);
            final Representation representation = entity.getRepresentation();
            if (representation != null) {
                valueFactory.toRdfRepresentation(representation);
            }
            final Representation metadata = entity.getMetadata();
            if (metadata != null) {
                valueFactory.toRdfRepresentation(metadata);
            }
        }
    }


    /**
     *
     * @param c an owl class
     * @param reasoner a pellet reasoner instance which is expected to be initialized with the CCO and imported GLODMED
     *                 ontology.
     * @return An Optional containing the top class of the given class according to the above table or Optional.empty()
     * if no such top class exists.
     */
    private Optional<OWLClass> getTopClass(OWLClass c, PelletReasoner reasoner) {
        if (topClassHits.keySet().contains(c)) {
            // found a top class, we're done
            return Optional.of(c);
        }
        Set<OWLClass> superClasses = reasoner.getSuperClasses(c, true).getFlattened();
        for (OWLClass superClass : superClasses) {
            Optional<OWLClass> result = getTopClass(superClass, reasoner);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }


    private void reloadOntologies(String scopeID) {
    	Scope scope = scopeManager.getScope(scopeID);
    	if (scope == null) {
    		log.error("No such scope: " + scopeID);
    		return;
    	}
    	
    	reloadOntologies(scope);
    }
    
    private void reloadOntologies(Scope scope) {
		String scopeID = scope.getID();

		log.info("Loading ontologies in scope '{}'", scopeID);
		
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		ontologyManagersByScope.put(scopeID, man);
		scope.getCustomSpace().listManagedOntologies().forEach(ontologyID -> {
			
			log.info("Loading ontology with ID {}...", ontologyID.getOntologyIRI().toQuotedString());
			
			OWLOntology ontology = scope.getCustomSpace().getOntology(ontologyID, OWLOntology.class);
			
			log.info("Finished loading ontology with ID {}", ontologyID.getOntologyIRI().toQuotedString());

			try {
				OWLOntology newOntology = man.createOntology(ontology.getAxioms(), ontology.getOntologyID().getOntologyIRI());
				if (newOntology.containsAnnotationPropertyInSignature(rootOntologyAnnotationProperteryIRI) ||
						rootOntologiesByScope.get(scopeID) == null) {
					// Make sure each scope has a root ontology.
					// We arbitrarily chose the ontology that's loaded first as the temporary root ontology.
					// If another ontology is explicitly declared as being the root ontology, then this gets overwritten
					// If several ontologies in the same scope are accidentally explicitly declared as being the root ontology, then the one that's loaded last is arbitrarily choses to be the root ontology.
					rootOntologiesByScope.put(scopeID, newOntology);
					
					log.info("Setting {} as new root ontology for scope {}", ontologyID.getOntologyIRI().toQuotedString(), scopeID);
				}
			} catch (OWLOntologyCreationException e) {
				log.error("Error creating ontology " + ontologyID.getOntologyIRI().toQuotedString(), e);
			}
		});
		
		OWLOntology rootOntology = rootOntologiesByScope.get(scopeID);
		if (rootOntology != null) {
			PelletReasoner reasoner = PelletReasonerFactory.getInstance().createReasoner(rootOntology);
			reasonersByScope.put(scopeID, reasoner);
			rootOntology = reasoner.getRootOntology(); // don't know if this is the same ontology, but just in case.
	        OWLOntologyManager manager = rootOntology.getOWLOntologyManager();
	        Set<OWLOntology> importsClosure = rootOntology.getImportsClosure();
			shortFormProviders.put(scopeID, new BidirectionalShortFormProviderAdapter(manager, importsClosure, new SimpleShortFormProvider()));
		}
    }

    /**
     * (Re-)loads the ontologies from Stanbol ontonet
     */
    private void reloadOntologies() {
    	log.info("Start (re)-loading ontologies for all scopes...");
    	scopeManager.getRegisteredScopes().forEach(scope -> {
    		reloadOntologies(scope);
    	});
    	log.info("(Re)-loading of ontologies for all scopes done.");
    	ontologiesDirty = false;
    }

//    /**
//     * (Re-)loads the ontologies from the OSGI bundle's persistent storage and initializes the reasoner.
//     */
//    private void reloadOntologies() {
//        if (!ontologyStorageFolder.exists()) {
//            ontologyStorageFolder.mkdir();
//            return; // folder is empty since we just created it, nothing more to do
//        }
//
//        Stream.of(ontologyStorageFolder.listFiles(file -> file.isDirectory())).forEach(directory  -> {
//
//            ontologyFoldersByChangeDate.add(directory);
//
//            // scan for root ontology
//
//
//
//            // Traverse the sub folders
//            Stream.of(directory.listFiles(file -> file.isFile() && file)).forEach(file -> {
//
//            });
//
//            try {
//                OWLOntology onto = man.loadOntologyFromOntologyDocument(new FileDocumentSource(file), new OWLOntologyLoaderConfiguration());
//                reasoner = PelletReasonerFactory.getInstance().createReasoner(ccoOntology);
//            } catch (OWLOntologyCreationException e) {
//                log.error("Error reloading ontologies", e);
//            }
//            Date lastModified = new Date(file.lastModified());
//        });
//
//        try {
//            ccoOntology = man.loadOntologyFromOntologyDocument(new FileDocumentSource(ccoOntologyFile), new OWLOntologyLoaderConfiguration());
//            reasoner = PelletReasonerFactory.getInstance().createReasoner(ccoOntology);
//        } catch (OWLOntologyCreationException e) {
//            log.error("Error reloading ontologies", e);
//        }
//    }
}
