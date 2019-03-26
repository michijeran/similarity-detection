package upc.similarity.similaritydetectionapi.test;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.impl.client.HttpClients;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestProjectsNew extends ControllerTest {

    private String path = "../testing/integration/projectsNew/";
    @LocalServerPort
    private int port;

    @Before
    public void ClearDB() {
        //auxiliary operation
        HttpClient httpclient = HttpClients.createDefault();
        HttpDelete httpdelete = new HttpDelete("http://localhost:9405/upc/Semilar/Clear");

        //Execute and get the response.
        try {
            httpclient.execute(httpdelete);
        } catch (IOException e) {
            System.out.println("Error conecting with server");
        }
    }

    /*
    ReqProjectNew operation
     */

    @Test
    public void aReqproject() throws InterruptedException {
        First_Result first_result = connect_to_component("http://localhost:"+port+"/upc/similarity-detection/ReqProjectNew?requirement=QM-1&requirement=QM-2&project=P1&compare=true&type=true&threshold=0.3&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"/reqproject/input_projectsNew_reqproject.json"));
        assertEquals(200,first_result.httpStatus);
        while(!finished) {Thread.sleep(2000);}
        assertEquals(read_file(path+"/reqproject/output_projectsNew_reqproject.json"),second_result.result);
        assertEquals(create_json_info(first_result.id,"ReqProjectNew","true"),second_result.result_info);
        finished = false;
    }

    /*
    ProjectsNew operation
     */

    @Test
    public void bProjectsNew() throws InterruptedException {
        First_Result first_result = connect_to_component("http://localhost:"+port+"/upc/similarity-detection/ProjectsNew?project=P1&project=P2&compare=true&type=true&threshold=0.3&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"/projects/input_projectsNew_projects.json"));
        assertEquals(200,first_result.httpStatus);
        while(!finished) {Thread.sleep(2000);}
        assertEquals(read_file(path+"/projects/output_projectsNew_projects.json"),second_result.result);
        assertEquals(create_json_info(first_result.id,"ProjectsNew","true"),second_result.result_info);
        finished = false;
    }
}
