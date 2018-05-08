package de.unileipzig.ontomed.simpleanno.categoryextractor;

import org.osgi.service.component.annotations.Component;
import org.semanticweb.owlapi.model.OWLClass;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * @author Ralph Schaefermeier
 */
@Component(immediate = true,
        service = Object.class,
        property = "javax.ws.rs:Boolean=true")
@Provider
@Produces("text/html")
public class CategoryWriter implements MessageBodyWriter<OWLClass> {


    @Override
    public boolean isWriteable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return OWLClass.class.isAssignableFrom(aClass);
    }

    @Override
    public long getSize(OWLClass owlClass, Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(OWLClass cls, Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> multivaluedMap, OutputStream out) throws IOException, WebApplicationException {
        PrintWriter writer = new PrintWriter(out);
        writer.println("<html>");
        writer.println("<body>");
        writer.println("Category: " + cls.getIRI().toString());
        writer.println("</body>");
        writer.println("</html>");
        writer.flush();
    }
}
