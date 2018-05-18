# category-service

# Installation

## Preliminaries

Before you install the category service bundle in your OSGI environment, the following dependencies need to be installed (and activated, if applicable):

* [OWLAPI 5.1.5](http://central.maven.org/maven2/net/sourceforge/owlapi/owlapi-osgidistribution/5.1.5/owlapi-osgidistribution-5.1.5.jar)
* [guava 20.0](http://central.maven.org/maven2/com/google/guava/guava/20.0/guava-20.0.jar)
* [guice 4.1.0](http://central.maven.org/maven2/com/google/inject/guice/4.1.0/guice-4.1.0.jar)
* [guice-assistedinject 4.1.0](http://central.maven.org/maven2/com/google/inject/extensions/guice-assistedinject/4.1.0/guice-assistedinject-4.1.0.jar)
* [guice-multibindings 4.1.0](http://central.maven.org/maven2/com/google/inject/extensions/guice-multibindings/4.1.0/guice-multibindings-4.1.0.jar)
* [mimepull 1.9.3](http://central.maven.org/maven2/org/jvnet/mimepull/mimepull/1.9.3/mimepull-1.9.3.jar)

## Installing the Category Service bundle

Download the [latest binary release](https://github.com/SimpleAnnoResearch/category-service/releases). All our binary release files are OSGI bundles. Install the bundle using the felix console of your Stanbol installation by pointing your browser to the URL http://localhost:8080/system/console/bundles and clicking on the "Install/Update" button. Select the bundle and install it. Make sure to also activate it.

Once installed and activated the bundle will provide a REST service endpoint at http://localhost:8080/category.

*Important:* In order to use the service you first need to upload the CCO and the GLODMED ontology files, which we cannot publish here for copyright reasons. If you open http://localhost:8080/category in your browser you will be provided a file upload form which you can use for uploading the two ontology files. The service will only work after you have uploaded both ontology files. The order in which you upload the two files is not important.

# Using the serice

In the following paragraphs, we assume that stanbol is running on your local machine and is reachable via calling http//localhost:8080. If your stanbol installation is on a different machine or uses a different port, replace the host address and port accordingly.

## HTML interface

Navigate your web browser to http://localhost:8080/category. From the web page, you can either look up a top catagory by entering a catagory IRI into the text field and pressing the "look up" button. If a top category exists it will be displayed along with a description of the top catgory, if available.

### Uploading the ontology files

The web page also contains the upload form for the two ontology files, which are necessary for the operation of the service. For instructions on how to upload the ontology files, please refer to the "Installation" section above.

## Looking up a top category IRI

In order to use the service in order to retrieve the plain top category IRI, call the same URL http://localhost:8080/category.

The following query parameters are available:

* `iri` (mandatory): The IRI of the category the top category of which should be looked up.

The following HTTP headers need to be set:

* `Accept= text/plain`

*Example:*

`curl -H "Accept: text/plain" http://stanbol.simple-anategory/?iri=http://glodmed.simple-anno.de/glodmed%232746`

Note that special characters (such as the hashmark) in the IRI need to be URL encoded.

## Looking up the label(s) of a top category

In order to get back the labels instead of a IRI, call http://localhost:8080/category/label

The following query parameters are available:

* `iri` (mandatory): The IRI of the category the top category of which should be looked up.
* `lang` (optional): The ISO language tag of the labels that should be returned. If omitted, German ("de") will be used as the default language.

The following HTTP headers need to be set:

* `Accept= text/plain`

*Example:*

`curl -H "Accept: text/plain" http://stanbol.simple-anategory/label?iri=http://glodmed.simple-anno.de/glodmed%232746?lang=en`

# Developer Notes

The category service uses the open source version of the Pellet reasoner and talks to it using the pellet-owlapi API. There is a dependency on [com.github.ansell.aterms:shared-objects-1.4.9](https://mvnrepository.com/artifact/com.github.ansell.aterms/shared-objects/1.4.9). We bundle this dependency (among others) because it is not OSGI enabled. However, there seems to be a packaging error on Maven Central with this particular maven bundle. Instead of the binary jar, Maven Central hosts the javadoc jar. The binary jar is not available. This is why we compiled and packaged our own binary jar from the sources (which are correctly packaed on Maven Central) and included it in our binary relase. Make sure to do the same if you wish to compile the category service bundle from the sources. We might add the depencency in a local repository in the future.
