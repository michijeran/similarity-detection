package upc.similarity.similaritydetectionapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import upc.similarity.similaritydetectionapi.AdaptersController;
import upc.similarity.similaritydetectionapi.adapter.ComponentAdapter;
import upc.similarity.similaritydetectionapi.adapter.SemilarAdapter;
import upc.similarity.similaritydetectionapi.entity.Project;
import upc.similarity.similaritydetectionapi.entity.input_output.*;
import upc.similarity.similaritydetectionapi.entity.Requirement;
import upc.similarity.similaritydetectionapi.exception.*;
import upc.similarity.similaritydetectionapi.values.Component;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;

@Service("similarityService")
public class SimilarityServiceImpl implements SimilarityService {

    private static String path = "../testing/output/";

    //TODO remove repeated code


    //Main operations

    @Override
    public Result_id simReqReq(String stakeholderId, String req1, String req2, String compare, String url, JsonReqReq input) throws BadRequestException, InternalErrorException, NotFoundException {

        String component = "Semilar";
        Result_id id = get_id();

        if (!validCompare(compare)) throw new BadRequestException("The provided attribute to compare is not valid. Please use: \'true\' or \'false\'."); // Error no valid compare attribute

        if (!input.OK()) throw new BadRequestException("The provided json has not requirements");

        //Searching requirements with ids req1 and req2
        List<String> requirements_ids = new ArrayList<>();

        requirements_ids.add(req1);
        requirements_ids.add(req2);

        List<Requirement> requirements = search_requirements(requirements_ids,input.getRequirements());

        Requirement requirement1 = requirements.get(0);
        Requirement requirement2 = requirements.get(1);

        //Create file to save resulting dependencies
        File file = create_file(path+id.getId());

        //New thread
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream fis = null;
                String success = "false";
                ObjectMapper mapper = new ObjectMapper();
                try {
                    ComponentAdapter componentAdapter = AdaptersController.getInstance().getAdpapter(Component.valueOf(component));
                    componentAdapter.similarity(stakeholderId,compare, requirement1, requirement2,id.getId(),input.getDependencies());
                    fis = new FileInputStream(file);
                    success = "true";
                } catch (ComponentException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(511,"Component error",e.getMessage()).getBytes());
                } catch (BadRequestException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(411,"Bad request",e.getMessage()).getBytes());
                } catch (NotFoundException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(410,"Not found",e.getMessage()).getBytes());
                } catch (FileNotFoundException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(510,"Internal error",e.getMessage()).getBytes());
                }
                finally {
                    update_client(fis,url,id.getId(),success,"ReqReq");
                    try {
                        delete_file(file);
                    } catch (InternalErrorException e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
        });

        thread.start();
        return id;
    }

    @Override
    public Result_id simReqProj(String stakeholderId, List<String> req, String project, String compare, float threshold, String url, JsonProject input) throws BadRequestException, InternalErrorException, NotFoundException {

        String component = "Semilar";
        Result_id id = get_id();

        if (!validCompare(compare)) throw new BadRequestException("The provided attribute to compare is not valid. Please use: \'true\' or \'false\'."); // Error no valid compare attribute
        if (!input.OK()) throw new BadRequestException("The provided json has not requirements or has not projects");

        //search requirements to compare
        List<Requirement> requirements_to_compare = search_requirements(req,input.getRequirements());
        //search project to compare
        Project project_specified = search_project(project,input.getProjects());
        //search project requirements
        List<Requirement> project_requirements = search_project_requirements(project_specified,input.getRequirements());

        //Create file to save resulting dependencies
        File file = create_file(path+id.getId());

        //New thread
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream fis = null;
                String success = "false";
                try {
                    ComponentAdapter componentAdapter = AdaptersController.getInstance().getAdpapter(Component.valueOf(component));
                    componentAdapter.similarityReqProject(stakeholderId,compare,threshold,id.getId(),requirements_to_compare,project_requirements,input.getDependencies());
                    fis = new FileInputStream(file);
                    success = "true";
                } catch (ComponentException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(511,"Component error",e.getMessage()).getBytes());
                } catch (BadRequestException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(411,"Bad request",e.getMessage()).getBytes());
                } catch (NotFoundException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(410,"Not found",e.getMessage()).getBytes());
                } catch (FileNotFoundException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(510,"Internal error",e.getMessage()).getBytes());
                }
                finally {
                    update_client(fis,url,id.getId(),success,"ReqProj");
                    try {
                        delete_file(file);
                    } catch (InternalErrorException e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
        });

        thread.start();
        return id;
    }

    @Override
    public Result_id simProj(String stakeholderId,String project, String compare, float threshold, String url, JsonProject input) throws BadRequestException, InternalErrorException, NotFoundException {

        String component = "Semilar";
        Result_id id = get_id();

        if (!validCompare(compare)) throw new BadRequestException("The provided attribute to compare is not valid. Please use: \'true\' or \'false\'."); // Error no valid compare attribute
        if (!input.OK()) throw new BadRequestException("The provided json has not requirements or has not projects");

        //search project to compare
        Project project_specified = search_project(project,input.getProjects());
        //search project requirements
        List<Requirement> project_requirements = search_project_requirements(project_specified,input.getRequirements());

        //Create file to save resulting dependencies
        File file = create_file(path+id.getId());

        //New thread
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream fis = null;
                String success = "false";
                try {
                    ComponentAdapter componentAdapter = AdaptersController.getInstance().getAdpapter(Component.valueOf(component));
                    componentAdapter.similarityProject(stakeholderId,compare,threshold,id.getId(),project_requirements,input.getDependencies());

                    fis = new FileInputStream(file);
                    success = "true";
                } catch (ComponentException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(511,"Component error",e.getMessage()).getBytes());
                } catch (BadRequestException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(411,"Bad request",e.getMessage()).getBytes());
                } catch (NotFoundException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(410,"Not found",e.getMessage()).getBytes());
                } catch (FileNotFoundException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(510,"Internal error",e.getMessage()).getBytes());
                }
                finally {
                    update_client(fis,url,id.getId(),success,"Proj");
                    try {
                        delete_file(file);
                    } catch (InternalErrorException e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
        });

        thread.start();
        return id;
    }

    @Override
    public Result_id addRequirements(String stakeholderId, Requirements input, String url) throws ComponentException, BadRequestException {

        Result_id id = get_id();

        if (!input.OK()) throw new BadRequestException("The provided json has not requirements");

        //New thread
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream fis = null;
                String success = "false";
                try {
                    SemilarAdapter semilarAdapter = new SemilarAdapter();
                    semilarAdapter.processRequirements(stakeholderId,input.getRequirements());
                    System.out.println("finish preprocess");
                    String result = "{\"result\":\"Success!\"}";
                    fis = new ByteArrayInputStream(result.getBytes());
                    success = "true";
                } catch (ComponentException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(511,"Component error",e.getMessage()).getBytes());
                } catch (BadRequestException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(411,"Bad request",e.getMessage()).getBytes());
                }
                finally {
                    update_client(fis,url,id.getId(),success,"AddReqs");
                }
            }
        });

        thread.start();
        return id;
    }

    @Override
    public void clearDB() throws SemilarException, BadRequestException {

        SemilarAdapter semilarAdapter = new SemilarAdapter();
        semilarAdapter.clearDB();
    }

    @Override
    public Result_id simCluster(String project, String compare, float threshold, String url, String type, JsonProject input) throws BadRequestException, InternalErrorException, NotFoundException {

        String component = "Semilar";
        Result_id id = get_id();

        if (!validCompare(compare)) throw new BadRequestException("The provided attribute to compare is not valid. Please use: \'true\' or \'false\'."); // Error no valid compare attribute
        if (!input.OK()) throw new BadRequestException("The provided json has not requirements or has not projects");

        //search project to compare
        Project project_specified = search_project(project,input.getProjects());
        //search project requirements
        List<Requirement> project_requirements = search_project_requirements(project_specified,input.getRequirements());

        //Create file to save resulting dependencies
        File file = create_file(path+id.getId());

        //New thread
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream fis = null;
                String success = "false";
                try {
                    ComponentAdapter componentAdapter = AdaptersController.getInstance().getAdpapter(Component.valueOf(component));
                    componentAdapter.similarityCluster(type,compare,threshold,id.getId(),project_requirements,input.getDependencies());

                    fis = new FileInputStream(file);
                    success = "true";
                } catch (ComponentException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(511,"Component error",e.getMessage()).getBytes());
                } catch (BadRequestException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(411,"Bad request",e.getMessage()).getBytes());
                } catch (NotFoundException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(410,"Not found",e.getMessage()).getBytes());
                } catch (FileNotFoundException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(510,"Internal error",e.getMessage()).getBytes());
                }
                finally {
                    update_client(fis,url,id.getId(),success,"Clusters");
                    try {
                        delete_file(file);
                    } catch (InternalErrorException e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
        });

        thread.start();
        return id;
    }

    @Override
    public Result_id iniClusters(String stakeholderId, String compare, String url, JsonCluster input) throws BadRequestException, InternalErrorException, NotFoundException {

        String component = "Semilar";
        Result_id id = get_id();

        if (!validCompare(compare)) throw new BadRequestException("The provided attribute to compare is not valid. Please use: \'true\' or \'false\'."); // Error no valid compare attribute
        if (input.getRequirements().size() == 0) throw new BadRequestException("The provided json has not requirements");

        //search requirements in dependencies
        //List<Requirement> dependencies_requirements = search_dependencies_requirements(input.getDependencies(),input.getRequirements());

        //Create file to save result
        File file = create_file(path+id.getId());

        //New thread
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream fis = null;
                String success = "false";
                try {
                    ComponentAdapter componentAdapter = AdaptersController.getInstance().getAdpapter(Component.valueOf(component));
                    componentAdapter.iniClusters(compare,id.getId(),stakeholderId,input.getRequirements(),input.getDependencies());
                    String result = "{\"result\":\"Success!\"}";
                    fis = new ByteArrayInputStream(result.getBytes());
                    success = "true";
                } catch (ComponentException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(511,"Component error",e.getMessage()).getBytes());
                } catch (BadRequestException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(411,"Bad request",e.getMessage()).getBytes());
                } catch (NotFoundException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(410,"Not found",e.getMessage()).getBytes());
                }
                finally {
                    update_client(fis,url,id.getId(),success,"iniClusters");
                    try {
                        delete_file(file);
                    } catch (InternalErrorException e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
        });

        thread.start();
        return id;
    }

    @Override
    public Result_id computeClusters(boolean type, String stakeholderId, String compare, String url) throws BadRequestException, InternalErrorException, NotFoundException {

        String component = "Semilar";
        Result_id id = get_id();

        if (!validCompare(compare)) throw new BadRequestException("The provided attribute to compare is not valid. Please use: \'true\' or \'false\'."); // Error no valid compare attribute

        //Create file to save result
        File file = create_file(path+id.getId());

        //New thread
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream fis = null;
                String success = "false";
                try {
                    ComponentAdapter componentAdapter = AdaptersController.getInstance().getAdpapter(Component.valueOf(component));
                    componentAdapter.computeClusters(type,compare,id.getId(),stakeholderId);
                    fis = new FileInputStream(file);
                    success = "true";
                } catch (ComponentException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(511,"Component error",e.getMessage()).getBytes());
                } catch (BadRequestException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(411,"Bad request",e.getMessage()).getBytes());
                } catch (NotFoundException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(410,"Not found",e.getMessage()).getBytes());
                } catch (FileNotFoundException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(510,"Internal error",e.getMessage()).getBytes());
                }
                finally {
                    update_client(fis,url,id.getId(),success,"computeClusters");
                    try {
                        delete_file(file);
                    } catch (InternalErrorException e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
        });

        thread.start();
        return id;
    }

    @Override
    public Result_id updateClusters(boolean type, String stakeholderId, String compare, String url, JsonCluster input) throws BadRequestException, InternalErrorException, NotFoundException {

        String component = "Semilar";
        Result_id id = get_id();

        if (!validCompare(compare)) throw new BadRequestException("The provided attribute to compare is not valid. Please use: \'true\' or \'false\'."); // Error no valid compare attribute
        if (input.getRequirements().size() == 0 && input.getDependencies().size() == 0) throw new BadRequestException("The provided json has not requirements neither dependencies");

        //search requirements in dependencies
        //List<Requirement> dependencies_requirements = search_dependencies_requirements(input.getDependencies(),input.getRequirements());

        //Create file to save result
        File file = create_file(path+id.getId());

        //New thread
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream fis = null;
                String success = "false";
                try {
                    ComponentAdapter componentAdapter = AdaptersController.getInstance().getAdpapter(Component.valueOf(component));
                    componentAdapter.updateClusters(type,compare,id.getId(),stakeholderId,input.getRequirements(),input.getDependencies());
                    fis = new FileInputStream(file);
                    success = "true";
                } catch (ComponentException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(511,"Component error",e.getMessage()).getBytes());
                } catch (BadRequestException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(411,"Bad request",e.getMessage()).getBytes());
                } catch (NotFoundException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(410,"Not found",e.getMessage()).getBytes());
                } catch (FileNotFoundException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(510,"Internal error",e.getMessage()).getBytes());
                }
                finally {
                    update_client(fis,url,id.getId(),success,"updateClusters");
                    try {
                        delete_file(file);
                    } catch (InternalErrorException e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
        });

        thread.start();
        return id;
    }

    @Override
    public Result_id modifyThreshold(boolean type, String compare, String stakeholderId, float threshold, String url) throws InternalErrorException {

        String component = "Semilar";
        Result_id id = get_id();

        //Create file to save result
        File file = create_file(path+id.getId());

        //New thread
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream fis = null;
                String success = "false";
                try {
                    ComponentAdapter componentAdapter = AdaptersController.getInstance().getAdpapter(Component.valueOf(component));
                    componentAdapter.modifyThreshold(type,id.getId(),compare,stakeholderId,threshold);
                    String result = "{\"result\":\"Success!\"}";
                    fis = new ByteArrayInputStream(result.getBytes());
                    success = "true";
                } catch (ComponentException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(511,"Component error",e.getMessage()).getBytes());
                } catch (BadRequestException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(411,"Bad request",e.getMessage()).getBytes());
                }
                finally {
                    update_client(fis,url,id.getId(),success,"modifyThreshold");
                    try {
                        delete_file(file);
                    } catch (InternalErrorException e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
        });

        thread.start();
        return id;
    }

    public Result_id resetStakeholder(String stakeholderId, String url) throws InternalErrorException, BadRequestException, NotFoundException {

        String component = "Semilar";
        Result_id id = get_id();

        //Create file to save result
        File file = create_file(path+id.getId());

        //New thread
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream fis = null;
                String success = "false";
                try {
                    ComponentAdapter componentAdapter = AdaptersController.getInstance().getAdpapter(Component.valueOf(component));
                    componentAdapter.resetStakeholder(stakeholderId);
                    String result = "{\"result\":\"Success!\"}";
                    fis = new ByteArrayInputStream(result.getBytes());
                    success = "true";
                } catch (ComponentException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(511,"Component error",e.getMessage()).getBytes());
                } catch (BadRequestException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(411,"Bad request",e.getMessage()).getBytes());
                }
                finally {
                    update_client(fis,url,id.getId(),success,"resetOrganization");
                    try {
                        delete_file(file);
                    } catch (InternalErrorException e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
        });

        thread.start();
        return id;
    }

    public Result_id projects(String stakeholderId, List<String> projects, String url, Projects input) throws InternalErrorException, BadRequestException, NotFoundException {

        if (input.getProjects().size() == 0) throw new BadRequestException("The provided json has no projects");

        String component = "Semilar";
        Result_id id = get_id();

        //Create file to save result
        File file = create_file(path+id.getId());

        List<String> project_requirements_id = new ArrayList<>();

        for (String project_id: projects) {
            Project project = search_project(project_id, input.getProjects());
            project_requirements_id.addAll(project.getSpecifiedRequirements());
        }

        //New thread
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream fis = null;
                String success = "false";
                try {
                    ComponentAdapter componentAdapter = AdaptersController.getInstance().getAdpapter(Component.valueOf(component));
                    componentAdapter.projects(stakeholderId,id.getId(),project_requirements_id);
                    fis = new FileInputStream(file);
                    success = "true";
                } catch (ComponentException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(511,"Component error",e.getMessage()).getBytes());
                } catch (BadRequestException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(411,"Bad request",e.getMessage()).getBytes());
                }catch (FileNotFoundException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(510,"Internal error",e.getMessage()).getBytes());
                }
                finally {
                    update_client(fis,url,id.getId(),success,"projects");
                    try {
                        delete_file(file);
                    } catch (InternalErrorException e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
        });

        thread.start();
        return id;

    }

    public Result_id reqProject(String stakeholderId, String project_id, String requirement_id, String url, Projects input) throws InternalErrorException, BadRequestException, NotFoundException {

        if (input.getProjects().size() == 0) throw new BadRequestException("The provided json has no projects");

        String component = "Semilar";
        Result_id id = get_id();

        //Create file to save result
        File file = create_file(path+id.getId());

        Project project = search_project(project_id, input.getProjects());


        //New thread
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream fis = null;
                String success = "false";
                try {
                    ComponentAdapter componentAdapter = AdaptersController.getInstance().getAdpapter(Component.valueOf(component));
                    componentAdapter.reqProject(stakeholderId,id.getId(),requirement_id,project.getSpecifiedRequirements());
                    fis = new FileInputStream(file);
                    success = "true";
                } catch (ComponentException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(511,"Component error",e.getMessage()).getBytes());
                } catch (BadRequestException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(411,"Bad request",e.getMessage()).getBytes());
                }catch (FileNotFoundException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(510,"Internal error",e.getMessage()).getBytes());
                }
                finally {
                    update_client(fis,url,id.getId(),success,"reqProject");
                    try {
                        delete_file(file);
                    } catch (InternalErrorException e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
        });

        thread.start();
        return id;
    }



    //Auxiliary operations

    /*private List<Requirement> search_dependencies_requirements(List<Dependency> dependencies, List<Requirement> requirements) {

        List<Requirement> result = new ArrayList<>();

        HashSet<String> ids = new HashSet<>();
        for (Dependency dependency: dependencies) {
            if (dependency.getStatus().equals("accepted") && dependency.getDependency_type().equals("duplicates")) {
                ids.add(dependency.getFromid());
                ids.add(dependency.getToid());
            } else dependencies.remove(dependency);
        }

        for (Requirement requirement: requirements) {
            if (ids.contains(requirement.getId())) {
                ids.remove(requirement.getId());
                result.add(requirement);
            }
        }

        return result;
    }*/


    /*
    auxiliary operations
     */

    private Result_id get_id() {
        Random rand = new Random();
        return new Result_id(System.currentTimeMillis() + "_" + rand.nextInt(1000));
    }

    private List<Requirement> search_requirements(List<String> req, List<Requirement> requirements) throws NotFoundException {

        List<Requirement> result = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (String id: req) ids.add(id);

        for (int i = 0; (!ids.isEmpty()) && (i < requirements.size()); ++i) {
            Requirement req_aux = requirements.get(i);
            if (req_ok(req_aux) && ids.contains(req_aux.getId())) {
                result.add(req_aux);
                ids.remove(req_aux.getId());
            }
        }

        if (!ids.isEmpty()) {
            for (String id: ids) {
                throw new NotFoundException("There is not requirement with id \'" + id + "\' in the JSON provided"); //Error: req not found
            }
        }

        return result;
    }

    private Project search_project(String project, List<Project> projects) throws NotFoundException {

        boolean found = false;
        Project project_input = null;
        for (int i = 0; (!found) && (i < projects.size()); ++i) {
            project_input = projects.get(i);
            if (project_ok(project_input) && project_input.getId().equals(project)) found = true;
        }

        if (!found) throw new NotFoundException("There is not project with id \'" + project + "\' in the JSON provided"); //Error: project not found

        return project_input;
    }

    private List<Requirement> search_project_requirements(Project project, List<Requirement> requirements) {

        List<Requirement> result = new ArrayList<>();
        List<String> requirements_id = project.getSpecifiedRequirements();
        Set<String> ids = new HashSet<>();
        for (String id: requirements_id) ids.add(id);

        for (int i = 0; (!ids.isEmpty()) && (i < requirements.size()); i++) {
            Requirement requirement_aux = requirements.get(i);
            String id_aux = requirement_aux.getId();
            if (id_aux != null && ids.contains(id_aux)) {
                result.add(requirement_aux);
                ids.remove(id_aux);
            }
        }

        return result;
    }

    private boolean validCompare(String compare) {

        if (compare.equals("true") || compare.equals("false")) return true;
        else return false;

        /*String[] parts = compare.split("-");
        boolean correcto = true;
        for (int i = 0; (correcto) && (i < parts.length); ++i) {
            String aux = parts[i];
            if (!aux.equals("")) {
                if (!aux.equals("Name") && !aux.equals("Text") && !aux.equals("Comments")) correcto = false;
            }
        }
        return correcto;*/
    }

    private boolean project_ok(Project project) {
        if (project.getId() == null) return false;
        else return true;
    }

    private boolean req_ok(Requirement req) {
        if (req.getId() == null) return false;
        else return true;
    }

    private String exception_to_JSON(int status, String error, String message) {
        JSONObject result = new JSONObject();
        result.put("status",status);
        result.put("error",error);
        result.put("message",message);
        return result.toString();
    }

    private void update_client(InputStream targetStream, String url, String id, String success, String operation) {
        String finish = "";
        try {
            //prepare post
            HttpClient httpclient = HttpClients.createDefault();
            HttpPost httppost = new HttpPost(url);
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            builder.addPart("result", new InputStreamBody(targetStream, "filename"));
            JSONObject json_to_send = new JSONObject();
            json_to_send.put("id",id);
            json_to_send.put("success",success);
            json_to_send.put("operation",operation);
            builder.addPart("info", new StringBody(json_to_send.toString(), ContentType.APPLICATION_JSON));
            HttpEntity entity = builder.build();
            httppost.setEntity(entity);
            //execute post
            HttpResponse response = httpclient.execute(httppost);
            int statusCode = response.getStatusLine().getStatusCode();
            if ((statusCode >= 200) && (statusCode < 300)) finish = "SUCCESS";
            else finish = "ERROR in external server";
        } catch (IOException e) {
            finish = "ERROR connecting with external server";
        }
        finally {
            LocalDateTime now = LocalDateTime.now();
            int year = now.getYear();
            int month = now.getMonthValue();
            int day = now.getDayOfMonth();
            int hour = now.getHour();
            int minute = now.getMinute();
            System.out.println(finish + " -- " + hour + ":" + minute + "  " + month + "/" + day + "/" + year);
        }
    }

    private File create_file(String filename) throws InternalErrorException {

        File file;
        try {
            file = new File(filename);
            if (!file.createNewFile()) throw new InternalErrorException("Error while creating new file");
        } catch (IOException e) {
            throw new InternalErrorException(e.getMessage());
        }
        return file;
    }

    private void delete_file(File file) throws InternalErrorException {

        if (!file.delete()) throw new InternalErrorException("Error deleting file with name: " + file.getName());
    }


}