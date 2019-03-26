package upc.similarity.similaritydetectionapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.service.Tag;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;
import upc.similarity.similaritydetectionapi.RestApiController;

@Configuration
@PropertySource("classpath:swagger.properties")
@ComponentScan(basePackageClasses = RestApiController.class)
@EnableSwagger2
public class SwaggerConfig {

    private static final String	LICENSE_TEXT	    = "License";
    private static final String	title		    = "SIMILARITY DETECTION COMPONENT";
    private static final String	description	    = "" +
            "<p>The component is based in <a href=\"http://deeptutor2.memphis.edu/Semilar-Web/public/downloads/ACL-13.SEMILAR.DEMO.pdf\">Semilar</a> semantic similarity library. The aim of the API is to found duplicate requirements."+
            "</p><p>This service groups duplicates requirements inside clusters. Each organization has its own requirements grouped into clusters in an internal database. Also, each organization has a set of accepted " +
            "and rejected dependencies which are taken into account when calculating the similarity between the different requirements. Instead of comparing the requirements separately, this service" +
            " compares between clusters of requirements. There are two forms to compare multiple clusters. This is used in the cluster operations and is represented by the parameter \"type\". The first form consists in comparing only the more representative requirements of each cluster (type = false). The other way " +
            " consists in comparing between all the requirements of each cluster (type = true). Also, there are two ways to compare between two individual requirements. The default method consists in comparing the name of the requirements. The other way lies in comparing the name and the text of the requirements and taking into account the maximum of the two. </p> " +
            "<p>There are four different types of operations: </p>" +
            "<ul>" +
            "<li>Clusters: Creates and updates a set of clusters of the different organizations. </li>" +
            "<li>InputProjectsOp: Returns the duplicate dependencies between different requirements using the knowledge saved in the database.</li>" +
            "<li>ProjectsNew: Returns the duplicate dependencies between different requirements without using the knowledge saved in the database.</li>" +
            "<li>Auxiliary: Auxiliary operations.</li></ul>" +
            "<p>All the operations in this service are asynchronous. It is necessary to write a server URL as parameter in all of them. The result of the operation will be returned to that url. All operations follow the same pattern:</p>" +
            "<ol><li>The client calls the operation with all necessary parameters</li>" +
            "<li>The service receives the request and checks the main conditions</li>" +
            "<li>The service returns if the client request has been accepted or not and closes the connection" +
            "<ul><li>(httpStatus!=200) The request has not been accepted. The message body contains the exception cause.</li>" +
            "<li>(httpStatus==200) The request has been accepted. The similarity calculation runs in the background. The message body contains the request identifier i.e. <em>{\"id\": \"1548924677975_523\"}</em></li></ul>" +
            "<li>When the calculation finishes (only if the request has been accepted) the service opens a connection with the server url specified as parameter.<br>" +
            "It sends a multipart file with a JSON object and an InputStream. The JSON object contains the information about the request and the InputStream contains the resulting body.<br>" +
            "Example of JSON object: <em> {\"success\":\"true\",\"id\":\"1548924677975_523\",\"operation\":\"Proj\"} </em>. Shows if the request has been successful, the request identifier and the name of the request operation.<ul>" +
            "<li>(success==false) The InputStream contains the exception cause.</li>" +
            "<li>(success==true) The InputStream contains the result of the operation.</li></ul></li></ol>" +
            "<p>The API uses UTF-8 charset. Also, it uses the OpenReq format for input and output JSONs (it is specified in the Models section).</p>";

    /**
     * API Documentation Generation.
     * @return
     */
    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .useDefaultResponseMessages(false)
                .apiInfo(apiInfo())
                .pathMapping("/")
                .select()
                .paths(PathSelectors.regex("/upc/similarity-detection/ModifyThreshold|/upc/similarity-detection/InitializeClusters|/upc/similarity-detection/ComputeClusters|/upc/similarity-detection/UpdateClusters|/upc/similarity-detection/ResetOrganization|/upc/similarity-detection/ReqProject|/upc/similarity-detection/InputProjectsOp|/upc/similarity-detection/ReqProjectNew||/upc/similarity-detection/ProjectsNew"))
                .apis(RequestHandlerSelectors.basePackage("upc.similarity.similaritydetectionapi")).paths(PathSelectors.regex("/upc.*"))
                .build().tags(new Tag("Similarity detection Service", "API related to similarity detection"));
    }
    /**
     * Informtion that appear in the API Documentation Head.
     *
     * @return
     */
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder().title(title).description(description).license(LICENSE_TEXT)
                .contact(new Contact("UPC-GESSI (OPENReq)", "http://openreq.eu/", ""))
                .build();
    }
}