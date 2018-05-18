package de.unileipzig.imise.ontomed.simpleanno;

import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.jaxrs.utils.TrailingSlash;
import org.apache.clerezza.jaxrs.utils.form.FormFile;
import org.apache.clerezza.jaxrs.utils.form.MultiPartBody;
import org.apache.clerezza.jaxrs.utils.form.ParameterValue;
import org.apache.clerezza.jaxrs.utils.form.StringParameterValue;
import org.apache.clerezza.rdf.core.access.EntityAlreadyExistsException;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.access.security.TcAccessController;
import org.apache.clerezza.rdf.core.access.security.TcPermission;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.clerezza.rdf.ontologies.RDFS;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.felix.scr.annotations.*;
import org.apache.stanbol.commons.indexedgraph.IndexedGraph;
import org.apache.stanbol.commons.web.viewable.RdfViewable;
import org.apache.stanbol.entityhub.model.clerezza.RdfValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.site.SiteManager;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.io.ReaderDocumentSource;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.util.SimpleIRIMapper;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import org.semanticweb.owlapi.vocab.SKOSVocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.manchester.cs.owl.owlapi.OWLAnnotationPropertyImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl;

import javax.swing.text.html.Option;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.*;
import java.security.Permission;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Ralph Schaefermeier
 */
@Component
@Service(Object.class)
@Property(name="javax.ws.rs", boolValue=true)
@Path("category")
public class CategoryService {

    private static final org.semanticweb.owlapi.model.IRI ccoOntologyIRI = org.semanticweb.owlapi.model.IRI.create("http://simple-anno.de/ontologies/dental_care_process");
    private static final org.semanticweb.owlapi.model.IRI glodmedIRI = org.semanticweb.owlapi.model.IRI.create("http://glodmed.simple-anno.de/glodmed#");

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

    private static final Logger log = LoggerFactory.getLogger(CategoryService.class);

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

    private BundleContext bundleContext;

    private File ccoOntologyFile, glodmedOntologyFile;

    private OWLOntology ccoOntology;
    private PelletReasoner reasoner;
    
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

//        registration = bundleContext.registerService( MultiPartFeature.class, new MultiPartFeature(), null );


        // load the CCO ontology from the Stanbol OntoNet
        // OWLOntology ccoOntology = onManager.getScope("CCO").getCustomSpace().getOntology(new OWLOntologyID())
        ccoOntologyFile = bundleContext.getDataFile("cco.owl");
        glodmedOntologyFile = bundleContext.getDataFile("glodmed.owl");

        reloadOntology();
    }
    
    @Deactivate
    protected void deactivate(ComponentContext context) {
        log.info("Deactivating SimpleAnno category service");
        if( registration != null ) {
            registration.unregister();
        }
    }

    /**
     * This method returns an RdfViewable, this is an RDF serviceUri with associated
     * presentational information.
     */
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
        if (iri != null && ccoOntology != null) {
            OWLClass clazz = new OWLClassImpl(org.semanticweb.owlapi.model.IRI.create(iri.getUnicodeString()));
            if (ccoOntology.containsClassInSignature(clazz.getIRI(), Imports.INCLUDED)) {
                Optional<OWLClass> topClass = getTopClass(clazz, reasoner);

                if (topClass.isPresent()) {
                    IRI topClassIRI = new IRI(topClass.get().getIRI().toString());
                    node.addProperty(Ontology.describes, topClassIRI);
                    addResourceDescription(topClassIRI, responseGraph);
                }
            }
        }
        //What we return is the GraphNode we created with a template path
        return new RdfViewable("CategoryService", node, CategoryService.class);
    }

    /**
     * Returns a response with HTTP status code 200 containing the IRI of the top category of the category with the given IRI in the response body.
     * Returns a response with with HTTP status code 200 and the text "- NO TOP CATEGORY -" in the response body if no top category is defined for the
     * given category.
     * Returns a response with HTTP status code other than 200 is an error occurs.
     * @param uriInfo
     * @param iri
     * @param userAgent
     * @return
     * @throws Exception
     */
    @GET
    @Produces("text/plain")
    public Response getIRI(@Context final UriInfo uriInfo,
            @QueryParam("iri") final IRI iri,
            @HeaderParam("user-agent") String userAgent) throws Exception {
        if (!ccoOntologyFile.exists() && !glodmedOntologyFile.exists()) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("No ontology. Please upload the CCO and GLODMED ontologies.").build();
        }
        if (!ccoOntologyFile.exists()) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("No CCO ontology. Please upload the CCO ontology.").build();
        }
        if (!glodmedOntologyFile.exists()) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("No GLODMED ontology. Please upload the GLODMED ontology.").build();
        }

        OWLClass clazz = new OWLClassImpl(org.semanticweb.owlapi.model.IRI.create(iri.getUnicodeString()));
        if (!ccoOntology.containsClassInSignature(clazz.getIRI(), Imports.INCLUDED)) {
            return Response.status(Response.Status.BAD_REQUEST).entity(String.format("The ontology does not contain a class with IRI %s.", iri)).build();
        }

        Optional<OWLClass> topClass = getTopClass(clazz, reasoner);
        if (topClass.isPresent()) {
            return Response.ok(topClass.get().getIRI().getIRIString()).build();
        } else {
            return Response.ok("- NO TOP CATEGORY -").build();
        }
    }

    /**
     * Returns a response with HTTP status code 200 containing the label of the top category of the category with the given IRI in the given language in the response body.
     * Returns a response with with HTTP status code 200 and the text "- NO TOP CATEGORY -" in the response body if no top category is defined for the
     * given category.
     * Returns a response with HTTP status code other than 200 is an error occurs.
     * @param uriInfo
     * @param iri
     * @param lang
     * @param userAgent
     * @return
     * @throws Exception
     */
    @GET
    @Path("label")
    @Produces("text/plain")
    public Response getLabel(@Context final UriInfo uriInfo,
                           @QueryParam("iri") final IRI iri,
                           @QueryParam("lang") final String lang,
                           @HeaderParam("user-agent") String userAgent) throws Exception {
        if (!ccoOntologyFile.exists() && !glodmedOntologyFile.exists()) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("No ontology. Please upload the CCO and GLODMED ontologies.").build();
        }
        if (!ccoOntologyFile.exists()) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("No CCO ontology. Please upload the CCO ontology.").build();
        }
        if (!glodmedOntologyFile.exists()) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("No GLODMED ontology. Please upload the GLODMED ontology.").build();
        }

        OWLClass clazz = new OWLClassImpl(org.semanticweb.owlapi.model.IRI.create(iri.getUnicodeString()));
        if (!ccoOntology.containsClassInSignature(clazz.getIRI(), Imports.INCLUDED)) {
            return Response.status(Response.Status.BAD_REQUEST).entity(String.format("The ontology does not contain a class with IRI %s.", iri)).build();
        }

        Optional<OWLClass> topClass = getTopClass(clazz, reasoner);
        if (topClass.isPresent()) {
            return Response.ok(getLabel(topClass.get(), lang)).build();
        } else {
            return Response.ok("- NO TOP CATEGORY -").build();
        }
    }

    private String getLabel(OWLClass cls, String lang) {
        String actualLang = lang == null || lang.isEmpty() ? "de" : lang;
        StringBuilder buf = new StringBuilder();
        ccoOntology.annotationAssertionAxioms(cls.getIRI(), Imports.INCLUDED).filter(
                annotationAxiom -> (annotationAxiom.getProperty().getIRI().equals(SKOSVocabulary.PREFLABEL.getIRI())
                        || annotationAxiom.getProperty().getIRI().equals(OWLRDFVocabulary.RDFS_LABEL.getIRI()))
        && annotationAxiom.getValue().isLiteral()
        && ((OWLLiteral)annotationAxiom.getValue()).hasLang(lang)).forEach(annotationAxiom -> {
            buf.append(((OWLLiteral)annotationAxiom.getValue()).getLiteral());
            buf.append("\n");
        });
        return buf.toString();
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadOntologyFile(MultiPartBody data) throws Exception {

        Reader body;

        FormFile[] sentFiles = data.getFormFileParameterValues("file");
        if (sentFiles.length != 0) {
            FormFile file = sentFiles[0];
            body = new InputStreamReader(new ByteArrayInputStream(file.getContent()));
        } else {
            ParameterValue[] paramValues = data.getParameteValues("file");
            if (paramValues.length != 0) {
                StringParameterValue fileValue = (StringParameterValue) paramValues[0];
                body = new StringReader(fileValue.toString());
            } else {
                return Response.status(Response.Status.BAD_REQUEST).entity("There was no ontology file with field name 'file' in the request body.").build();
            }
        }

        OWLOntologyManager om = OWLManager.createOWLOntologyManager();
        try {
            OWLOntology onto = om.loadOntologyFromOntologyDocument(new ReaderDocumentSource(body), new OWLOntologyLoaderConfiguration().setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT));
            String ontologyID = onto.getOntologyID().getOntologyIRI().get().toString();
            if (ontologyID.equals("http://simple-anno.de/ontologies/dental_care_process")) {
                om.saveOntology(onto, new FileOutputStream(ccoOntologyFile));
            } else if (ontologyID.equals("http://glodmed.simple-anno.de/glodmed#")) {
                om.saveOntology(onto, new FileOutputStream(glodmedOntologyFile));
            } else {
                return Response.status(Response.Status.BAD_REQUEST).entity("Unexpected ontology ID (expected: <http://simple-anno.de/ontologies/dental_care_process> or <http://glodmed.simple-anno.de/glodmed#>)").build();
            }
            reloadOntology();
            return Response.ok().build();
        } catch (OWLOntologyCreationException ex) {
            log.error("Error while saving ontology file.", ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("An error occured. " + ex.getMessage()).build();
        }
    }

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


    /**
     * (Re-)loads the ontologies from the OSGI bundle's persistent storage and initializes the reasoner.
     */
    private void reloadOntology() {
        if (!(ccoOntologyFile.exists() && glodmedOntologyFile.exists())) {
            // There's no use in reloading if one of the two ontology files is missing.
            return;
        }

        OWLOntologyManager man = OWLManager.createOWLOntologyManager();
        man.setIRIMappers(Stream.of(new SimpleIRIMapper(org.semanticweb.owlapi.model.IRI.create("http://glodmed.simple-anno.de/glodmed#"), org.semanticweb.owlapi.model.IRI.create(glodmedOntologyFile))).collect(Collectors.toSet()));
        try {
            ccoOntology = man.loadOntologyFromOntologyDocument(new FileDocumentSource(ccoOntologyFile), new OWLOntologyLoaderConfiguration());
            reasoner = PelletReasonerFactory.getInstance().createReasoner(ccoOntology);
        } catch (OWLOntologyCreationException e) {
            log.error("Error reloading ontologies", e);
        }
    }

}
