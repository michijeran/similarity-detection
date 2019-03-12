package upc.similarity.similaritydetectionapi.test;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestClusters extends ControllerTest {

    private static class First_Result {
        int httpStatus;
        String id;
        First_Result(int httpStatus, String id) {
            this.httpStatus = httpStatus;
            this.id = id;
        }
    }

    private String path = "../testing/integration/clusters/";
    //private static boolean finished = false;
    //private static Second_Result second_result = new Second_Result(null,null);
    @LocalServerPort
    private int port;

    /*
    Simple endpoints
     */
    @Before
    public void aClearDB() {
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

    @Test
    public void bModifyThreshold_simple() throws InterruptedException {
        First_Result first_result = connect_to_component("http://localhost:"+port+"/upc/similarity-detection/ModifyThreshold?stakeholderId=Test&threshold=0.4&url=http://localhost:"+port+"/upc/similarity-detection/Test",null);
        assertEquals(200,first_result.httpStatus);
        while(!finished) {Thread.sleep(2000);}
        assertEquals(create_json_info(first_result.id,"modifyTheshold","true"),second_result.result_info);
        assertEquals(read_file(path+"/modifyThreshold/output_modifyThreshold_simple_stakeholders.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetStakeholders"));
        finished = false;
    }

    @Test
    public void cIniClusters_simple() throws InterruptedException {
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/ModifyThreshold?stakeholderId=Test&threshold=0.4&url=http://localhost:"+port+"/upc/similarity-detection/Test",null);
        while(!finished) {Thread.sleep(2000);}
        finished = false;

        First_Result first_result = connect_to_component("http://localhost:"+port+"/upc/similarity-detection/InitializeClusters?stakeholderId=Test&compare=true&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"/iniClusters/simple/input_iniClusters_simple.json"));
        assertEquals(200,first_result.httpStatus);
        while(!finished) {Thread.sleep(2000);}
        assertEquals(read_file(path+"/iniClusters/simple/output_iniClusters_simple.json"),second_result.result);
        assertEquals(create_json_info(first_result.id,"iniClusters","true"),second_result.result_info);
        assertEquals(read_file(path+"/iniClusters/simple/output_iniClusters_simple_requirements.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetRequirements?stakeholderId=Test"));
        assertEquals(read_file(path+"/iniClusters/simple/output_iniClusters_simple_dependencies.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetDependencies?stakeholderId=Test"));
        finished = false;
    }


    /*
    More complex endpoints
     */
    @Test
    public void dIniClusters_addReq() throws InterruptedException {
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/ModifyThreshold?stakeholderId=Test&threshold=0.4&url=http://localhost:"+port+"/upc/similarity-detection/Test",null);
        while(!finished) {Thread.sleep(2000);}
        finished = false;

        First_Result first_result = connect_to_component("http://localhost:"+port+"/upc/similarity-detection/InitializeClusters?stakeholderId=Test&compare=true&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"/iniClusters/addReq/input_iniClusters_addReq.json"));
        assertEquals(200,first_result.httpStatus);
        while(!finished) {Thread.sleep(2000);}
        assertEquals(read_file(path+"/iniClusters/addReq/output_iniClusters_addReq.json"),second_result.result);
        assertEquals(create_json_info(first_result.id,"iniClusters","true"),second_result.result_info);
        assertEquals(read_file(path+"/iniClusters/addReq/output_iniClusters_addReq_requirements.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetRequirements?stakeholderId=Test"));
        assertEquals(read_file(path+"/iniClusters/addReq/output_iniClusters_addReq_dependencies.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetDependencies?stakeholderId=Test"));
        finished = false;
    }

    @Test
    public void eIniClusters_merge() throws InterruptedException {
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/ModifyThreshold?stakeholderId=Test&threshold=0.4&url=http://localhost:"+port+"/upc/similarity-detection/Test",null);
        while(!finished) {Thread.sleep(2000);}
        finished = false;

        First_Result first_result = connect_to_component("http://localhost:"+port+"/upc/similarity-detection/InitializeClusters?stakeholderId=Test&compare=true&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"/iniClusters/merge/input_iniClusters_merge.json"));
        assertEquals(200,first_result.httpStatus);
        while(!finished) {Thread.sleep(2000);}
        assertEquals(read_file(path+"/iniClusters/merge/output_iniClusters_merge.json"),second_result.result);
        assertEquals(create_json_info(first_result.id,"iniClusters","true"),second_result.result_info);
        assertEquals(read_file(path+"/iniClusters/merge/output_iniClusters_merge_requirements.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetRequirements?stakeholderId=Test"));
        assertEquals(read_file(path+"/iniClusters/merge/output_iniClusters_merge_dependencies.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetDependencies?stakeholderId=Test"));
        finished = false;
    }

    @Test
    public void eIniClusters_rejected() throws InterruptedException {
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/ModifyThreshold?stakeholderId=Test&threshold=0.4&url=http://localhost:"+port+"/upc/similarity-detection/Test",null);
        while(!finished) {Thread.sleep(2000);}
        finished = false;

        First_Result first_result = connect_to_component("http://localhost:"+port+"/upc/similarity-detection/InitializeClusters?stakeholderId=Test&compare=true&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"/iniClusters/rejected/input_iniClusters_rejected.json"));
        assertEquals(200,first_result.httpStatus);
        while(!finished) {Thread.sleep(2000);}
        assertEquals(read_file(path+"/iniClusters/rejected/output_iniClusters_rejected.json"),second_result.result);
        assertEquals(create_json_info(first_result.id,"iniClusters","true"),second_result.result_info);
        assertEquals(read_file(path+"/iniClusters/rejected/output_iniClusters_rejected_requirements.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetRequirements?stakeholderId=Test"));
        assertEquals(read_file(path+"/iniClusters/rejected/output_iniClusters_rejected_dependencies.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetDependencies?stakeholderId=Test"));
        finished = false;
    }




    /*
    Exceptions
     */





















    /*
    auxiliary methods
     */

    private String read_file(String path) {
        String result = "";
        String line = "";
        try {
            FileReader fileReader = new FileReader(path);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            while ((line = bufferedReader.readLine()) != null) {
                result = result.concat(line);
            }
            bufferedReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        JSONObject aux = new JSONObject(result);
        return aux.toString();
    }

    /*public static void setResult(String result, String result_info) {
        second_result = new Second_Result(result_info,new JSONObject(result).toString());
        finished = true;
    }*/

    private First_Result connect_to_component(String url, String json) {

        HttpClient httpclient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost(url);
        if(json != null) httppost.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
        int httpStatus = 500;
        String json_response = "";

        //Execute and get the response.
        try {
            HttpResponse response = httpclient.execute(httppost);
            httpStatus = response.getStatusLine().getStatusCode();
            json_response = EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
            System.out.println("Error conecting with server");
        }
        System.out.println(json_response);
        JSONObject aux = new JSONObject(json_response);
        String id = aux.getString("id");
        return new First_Result(httpStatus,id);
    }

    private String create_json_info(String id, String operation, String success) {
        JSONObject result = new JSONObject();
        result.put("id",id);
        result.put("success",success);
        result.put("operation",operation);
        return result.toString();
    }

    private String connect_to_component_simple(String url) {
        HttpClient httpclient = HttpClients.createDefault();
        HttpGet httpget = new HttpGet(url);
        String json_response = "";

        //Execute and get the response.
        try {
            HttpResponse response = httpclient.execute(httpget);
            json_response = EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
            System.out.println("Error conecting with server");
        }
        JSONObject aux = new JSONObject(json_response);
        return aux.toString();
    }

}
