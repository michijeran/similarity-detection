package upc.similarity.semilarapi.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import semilar.config.ConfigManager;
import semilar.sentencemetrics.GreedyComparer;
import semilar.tools.StopWords;
import semilar.tools.semantic.WordNetSimilarity;
import semilar.wordmetrics.WNWordMetric;
import upc.similarity.semilarapi.config.Configuration;
import upc.similarity.semilarapi.dao.RequirementDAO;
import upc.similarity.semilarapi.dao.SQLiteDAO;
import upc.similarity.semilarapi.entity.*;
import upc.similarity.semilarapi.entity.input.*;
import upc.similarity.semilarapi.exception.BadRequestException;
import upc.similarity.semilarapi.exception.InternalErrorException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ForkJoinPool;

@Service("semilarService")
public class SemilarServiceImpl implements SemilarService {
    
    static {
        ConfigManager.setSemilarDataRootFolder("./Models/");
        wnMetricLin = new WNWordMetric(WordNetSimilarity.WNSimMeasure.LIN, false);
        stopWords = new StopWords();
        number_threads = Configuration.getInstance().getNumber_threads();
    }

    private static WNWordMetric wnMetricLin;
    private RequirementDAO requirementDAO = getValue();
    private String component = "Similarity-Semilar";

    private static int number_threads;
    //private static final GreedyComparer greedyComparerWNLin = new GreedyComparer(wnMetricLin, 0.3f, true);
    private static StopWords stopWords;
    private static final GreedyComparer greedyComparerWNLin = new GreedyComparer(wnMetricLin,stopWords,0.3f, true, "NONE","AVERAGE",false,true,false,true,true,false, false);

    private RequirementDAO getValue() {
        try {
            return new SQLiteDAO();
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    //Similarity
    @Override
    public void similarity(String stakeholderId, String compare, String filename, PairReq input) throws SQLException, BadRequestException, InternalErrorException {

        try {
            RequirementId req1 = input.getReq1();
            RequirementId req2 = input.getReq2();
            if (req1.getId() == null || req2.getId() == null)
                throw new BadRequestException("One requirement has id equal to null");
            if (req1.getId().equals(req2.getId()))
                throw new BadRequestException("The requirements to be compared have the same id");
            Requirement r1 = requirementDAO.getRequirement(req1.getId(), stakeholderId);
            Requirement r2 = requirementDAO.getRequirement(req2.getId(), stakeholderId);
            ComparisonBetweenSentences comparer = new ComparisonBetweenSentences(greedyComparerWNLin, compare, 0, false, component);
            if (comparer.existsDependency(r1.getId(), r2.getId(), input.getDependencies()))
                throw new BadRequestException("Already exists another similar or duplicate dependency between the same requirements");
            float result = comparer.compare_two_requirements(r1, r2);
            Dependency aux = new Dependency(result, req1.getId(), req2.getId(), "proposed", "similar", component);

            //TODO improve this part
            Path p = Paths.get("../testing/output/" + filename);
            String s = System.lineSeparator() + "{\"dependencies\": [";

            write_to_file(s, p);

            s = System.lineSeparator() + aux.print_json();

            write_to_file(s, p);

            s = System.lineSeparator() + "]}";

            write_to_file(s, p);
        }  catch (ClassNotFoundException e) {
            throw new InternalErrorException("Database error: Class not found.");
        }
    }

    @Override
    public void similarityReqProj(String stakeholderId, String compare, float threshold, String filename, ReqProjOp input) throws InternalErrorException {

        List<RequirementId> requirements = input.getRequirements();
        List<RequirementId> project_requirements = input.getProject_requirements();

        List<Requirement> requirements_loaded = new ArrayList<>();
        List<Requirement> project_requirements_loaded = new ArrayList<>();

        for (RequirementId aux: requirements) {
            try {
                requirements_loaded.add(requirementDAO.getRequirement(aux.getId(),stakeholderId));
            } catch (SQLException e) {
                //nothing
            } catch (ClassNotFoundException e) {
                throw new InternalErrorException("Database error: Class not found.");
            }
        }

        for (RequirementId aux: project_requirements) {
            try {
                project_requirements_loaded.add(requirementDAO.getRequirement(aux.getId(),stakeholderId));
            } catch (SQLException e) {
                //nothing
            } catch (ClassNotFoundException e) {
                throw new InternalErrorException("Database error: Class not found.");
            }
        }

        ComparisonBetweenSentences comparer = new ComparisonBetweenSentences(greedyComparerWNLin,compare,threshold,true,component);

        Path p = Paths.get("../testing/output/"+filename);
        String s = System.lineSeparator() + "{\"dependencies\": [";

        write_to_file(s,p);

        boolean firsttimeComa = true;
        int cont = 0;
        String result = "";

        for (int i = 0; i < requirements_loaded.size(); ++i) {
            System.out.println(requirements_loaded.size() - i);
            Requirement req1 = requirements_loaded.get(i);
            for (Requirement req2 : project_requirements_loaded) {
                Dependency aux = comparer.compare_two_requirements_dep(req1,req2);
                if (aux != null) {
                    if (aux.getDependency_score() >= threshold && !comparer.existsDependency(aux.getFromid(), aux.getToid(),input.getDependencies())) {
                        s = System.lineSeparator() + aux.print_json();
                        if (!firsttimeComa) s = "," + s;
                        firsttimeComa = false;
                        result = result.concat(s);
                        ++cont;
                        if (cont >= 5000) {
                            write_to_file(result,p);
                            result = "";
                            cont = 0;
                        }
                    }
                }
            }
            project_requirements_loaded.add(req1);
        }

        if (!result.equals("")) write_to_file(result,p);

        s = System.lineSeparator() + "]}";
        write_to_file(s,p);
    }

    @Override
    public void similarityProj(String stakeholderId, String compare, float threshold, String filename, ProjOp input) throws InternalErrorException {

        int cont_left = 0;
        int max = input.getRequirements().size()*input.getRequirements().size()/2;
        int per = 0;

        show_time("start");

        List<RequirementId> requirements = input.getRequirements();

        //load reqs from db
        List<Requirement> requirements_loaded = new ArrayList<>();
        for (RequirementId aux: requirements) {
            try {
                requirements_loaded.add(requirementDAO.getRequirement(aux.getId(),stakeholderId));
            } catch (SQLException e) {
                //nothing
            } catch (ClassNotFoundException e) {
                throw new InternalErrorException("Database error: Class not found.");
            }
        }

        Path p = Paths.get("../testing/output/"+filename);
        String s = System.lineSeparator() + "{\"dependencies\": [";

        write_to_file(s,p);

        ComparisonBetweenSentences comparer = new ComparisonBetweenSentences(greedyComparerWNLin,compare,threshold,true,component);

        boolean firsttimeComa = true;
        int cont = 0;
        String result = "";


        for (int i = 0; i < requirements_loaded.size(); i++) {
            cont_left += requirements_loaded.size() - i - 1;
            int aux_left = cont_left*100/max;
            if (aux_left >= per + 10) {
                per = aux_left;
                show_time(aux_left+"%");
            }
            System.out.println(requirements_loaded.size() - i);
            Requirement req1 = requirements_loaded.get(i);
            for (int j = i + 1; j < requirements_loaded.size(); j++) {
                Requirement req2 = requirements_loaded.get(j);
                Dependency aux = comparer.compare_two_requirements_dep(req1,req2);
                if (aux != null) {
                    if (aux.getDependency_score() >= threshold && !comparer.existsDependency(aux.getFromid(), aux.getToid(), input.getDependencies())) {
                        s = System.lineSeparator() + aux.print_json();
                        if (!firsttimeComa) s = "," + s;
                        firsttimeComa = false;
                        result = result.concat(s);
                        ++cont;
                        if (cont >= 5000) {
                            write_to_file(result, p);
                            result = "";
                            cont = 0;
                        }
                    }
                }
            }
        }

        if (!result.equals("")) write_to_file(result,p);

        s = System.lineSeparator() + "]}";
        write_to_file(s,p);

        show_time("finish");
    }

    @Override
    public void similarityProj_Large(String stakeholderId, String compare, float threshold, String filename, ProjOp input) throws InternalErrorException{

        show_time("start");

        List<RequirementId> requirements = input.getRequirements();

        //load reqs from db
        List<Requirement> requirements_loaded = new ArrayList<>();
        for (RequirementId aux: requirements) {
            try {
                requirements_loaded.add(requirementDAO.getRequirement(aux.getId(),stakeholderId));
            } catch (SQLException e) {
                //nothing
            } catch (ClassNotFoundException e) {
                throw new InternalErrorException("Database error: Class not found.");
            }
        }

        Path p = Paths.get("../testing/output/"+filename);
        String s = System.lineSeparator() + "{\"dependencies\": [";

        write_to_file(s,p);

        ForkJoinPool commonPool = new ForkJoinPool(number_threads);
        LargeProjTask customRecursiveTask = new LargeProjTask(number_threads,threshold,0,number_threads,compare,requirements_loaded,input.getDependencies(),new_comparer(),p);
        commonPool.execute(customRecursiveTask);
        customRecursiveTask.join();

        delete_last_comma("../testing/output/"+filename);

        s = System.lineSeparator() + "]}";
        write_to_file(s,p);

        show_time("finish");
    }

    @Override
    public void iniClusters(String compare, String stakeholderId, String filename, IniClusterOp input) throws InternalErrorException, BadRequestException {

        HashMap<String,Requirement> requirements_loaded = new HashMap<>();
        for (Requirement requirement: input.getRequirements()) requirements_loaded.put(requirement.getId(), requirement);

        List<Cluster> clusters = new ArrayList<>();

        show_time("start loop dependencies");
        for (Dependency dependency: input.getDependencies()) {
            //test dependency_type && status != null before
            if (dependency.getDependency_type().equals("duplicates") && dependency.getStatus().equals("accepted")) {
                Requirement req1 = requirements_loaded.get(dependency.getFromid());
                Requirement req2 = requirements_loaded.get(dependency.getToid());
                if (req1.getCluster() == null && req2.getCluster() == null) {
                    Cluster new_cluster = new Cluster();
                    new_cluster.addReq(req1);
                    new_cluster.addReq(req2);
                    clusters.add(new_cluster);
                } else if (req1.getCluster() == null) {
                    req2.getCluster().addReq(req1);
                } else if (req2.getCluster() == null) {
                    req1.getCluster().addReq(req2);
                } else if (req1.getCluster() != req2.getCluster()) {
                    merge_clusters(req1.getCluster(), req2.getCluster());
                    clusters.remove(req2.getCluster());
                }
            }
        }
        show_time("finish loop dependencies");

        show_time("start requirements preprocess");
        int cont = 0;
        for (Requirement requirement: input.getRequirements()) {
            try {
                System.out.println(cont);
                ++cont;
                requirement.compute_sentence();
                if (requirement.getCluster() == null) {
                    Cluster new_cluster = new Cluster();
                    new_cluster.addReq(requirement);
                }
                requirementDAO.savePreprocessed(requirement,stakeholderId);
                //save memory
                requirement.setSentence_name(null);
                requirement.setSentence_text(null);
            } catch (SQLException e) {
                throw new BadRequestException("Database exception: Error while saving a requirement with id " + requirement.getId() + " to the database. There is no stakeholder called "+stakeholderId+".");
            } catch (ClassNotFoundException e) {
                throw new InternalErrorException("Database error: Class not found.");
            }
        }
        show_time("finish requirements preprocess");

        show_time("start saving dependencies");
        for (Dependency dependency: input.getDependencies()) {
            if (dependency.getDependency_type().equals("duplicates") && dependency.getStatus().equals("accepted")) {
                try {
                    requirementDAO.saveDependency(dependency, true, stakeholderId);
                } catch (SQLException e) {
                    throw new BadRequestException("Database exception: Error while saving a dependency (fromid:" + dependency.getFromid() + "toid:" + dependency.getToid() + ") to the database. There is no stakeholder called "+stakeholderId+".");
                } catch (ClassNotFoundException e) {
                    throw new InternalErrorException("Database error: Class not found.");
                }
            } else if (dependency.getDependency_type().equals("duplicates") && dependency.getStatus().equals("rejected")) {
                try {
                    requirementDAO.saveDependency(dependency,false,stakeholderId);
                } catch (SQLException e) {
                    throw new BadRequestException("Database exception: Error while saving a dependency (fromid:" + dependency.getFromid() + "toid:" + dependency.getToid() + ") to the database. There is no stakeholder called "+stakeholderId+".");
                } catch (ClassNotFoundException e) {
                    throw new InternalErrorException("Database error: Class not found.");
                }
            }
        }
        show_time("stop saving dependencies");
    }

    @Override
    public void updateClusters(String compare, String stakeholderId, String filename, IniClusterOp input) throws InternalErrorException, BadRequestException {

        show_time("start initialization");

        float threshold;
        try {
            threshold = requirementDAO.getThreshold(stakeholderId);
        } catch (SQLException e) {
            throw new BadRequestException("Database error: Stakeholder with id " + stakeholderId + " does not exists in db.");
        } catch (ClassNotFoundException e) {
            throw new InternalErrorException("Database error: Class not found.");
        }

        List<Requirement> loaded_requirements;
        try {
            loaded_requirements = requirementDAO.getRequirements(stakeholderId);
        } catch (SQLException e) {
            throw new InternalErrorException("Database error: Error while loading database requirements");
        } catch (ClassNotFoundException e) {
            throw new InternalErrorException("Database error: Class not found.");
        }

        List<Cluster> clusters_listed = new ArrayList<>();
        HashMap<Integer,Cluster> clusters = new HashMap<>();
        HashMap<String,Requirement> hash_requirements = new HashMap<>();

        for (Requirement requirement: loaded_requirements) {
            hash_requirements.put(requirement.getId(),requirement);
            int clusterId = requirement.getClusterId();
            if(clusters.containsKey(clusterId)) {
                clusters.get(clusterId).addReq(requirement);
            } else {
                Cluster aux_cluster = new Cluster(clusterId);
                aux_cluster.addReq(requirement);
                clusters.put(clusterId,aux_cluster);
                clusters_listed.add(aux_cluster);
            }
        }

        clusters = null;
        loaded_requirements = null;

        List<Requirement> free_requirements = new ArrayList<>();
        List<Dependency> dependencies_to_add = new ArrayList<>();
        List<Requirement> deleted_requirements = new ArrayList<>(); //TODO specify these requirements (input data)
        List<Dependency> added_dependencies = new ArrayList<>(); //TODO specify these dependencies (input data)
        List<Requirement> edited_requirements = new ArrayList<>(); //TODO specify these requirements (input data)
        List<Requirement> added_requirements = new ArrayList<>(); //TODO specify these requirements (input data)
        List<Dependency> result_dependencies = new ArrayList<>();

        show_time("finish initialization");


        ComparisonBetweenSentences comparer = new ComparisonBetweenSentences(greedyComparerWNLin,compare,threshold,true,component);

        update_deleted_rejected_dependencies(input.getDependencies(),hash_requirements,free_requirements,comparer,threshold, stakeholderId);

        update_deleted_requirements(deleted_requirements,free_requirements,clusters_listed,stakeholderId);

        update_added_accepted_dependencies(added_dependencies,hash_requirements,clusters_listed,dependencies_to_add,stakeholderId);

        update_edited_requirements(edited_requirements,free_requirements,clusters_listed,stakeholderId);

        update_added_requirements(added_requirements,clusters_listed,result_dependencies,comparer,threshold,stakeholderId);
    }

    @Override
    public void computeClusters(String compare, String stakeholderId, String filename) throws InternalErrorException, BadRequestException {

        show_time("start initialization");

        float threshold;
        try {
            threshold = requirementDAO.getThreshold(stakeholderId);
        } catch (SQLException e) {
            throw new BadRequestException("Database error: Stakeholder with id " + stakeholderId + " does not exists in db.");
        } catch (ClassNotFoundException e) {
            throw new InternalErrorException("Database error: Class not found.");
        }

        List<Requirement> loaded_requirements;
        try {
            loaded_requirements = requirementDAO.getRequirements(stakeholderId);
        } catch (SQLException e) {
            throw new InternalErrorException("Database error: Error while loading database requirements");
        } catch (ClassNotFoundException e) {
            throw new InternalErrorException("Database error: Class not found.");
        }

        List<Cluster> clusters_listed = new ArrayList<>();
        HashMap<Integer,Cluster> clusters = new HashMap<>();
        HashMap<String,Requirement> hash_requirements = new HashMap<>();

        for (Requirement requirement: loaded_requirements) {
            hash_requirements.put(requirement.getId(),requirement);
            int clusterId = requirement.getClusterId();
            if(clusters.containsKey(clusterId)) {
                clusters.get(clusterId).addReq(requirement);
            } else {
                Cluster aux_cluster = new Cluster(clusterId);
                aux_cluster.addReq(requirement);
                clusters.put(clusterId,aux_cluster);
                clusters_listed.add(aux_cluster);
            }
        }

        clusters = null;

        ComparisonBetweenSentences comparer = new ComparisonBetweenSentences(greedyComparerWNLin,compare,threshold,true,component);

        show_time("stop initialization");

        show_time("start loop");
        List<Dependency> result = new ArrayList<>();

        if (true) all_to_all_algorithm(loaded_requirements,threshold,comparer,result,clusters_listed);
        else all_to_masters_algorithm(loaded_requirements,threshold,comparer,result,clusters_listed);
        show_time("stop loop");
    }

    private void all_to_all_algorithm(List<Requirement> loaded_requirements, float threshold, ComparisonBetweenSentences comparer, List<Dependency> result, List<Cluster> clusters_listed) {

        for (int i = 0; i < loaded_requirements.size(); ++i) {
            Requirement req1 = loaded_requirements.get(i);
            for (int j = i + 1; j < loaded_requirements.size(); ++j) {
                Requirement req2 = loaded_requirements.get(j);
                Dependency aux = comparer.compare_two_requirements_dep(req1,req2);
                if (aux != null && aux.getDependency_score() >= threshold) {
                    if (req1.getCluster() != req2.getCluster()) {
                        String old_master_req1 = req1.getCluster().getReq_older().getId();
                        String old_master_req2 = req2.getCluster().getReq_older().getId();
                        merge_clusters(req1.getCluster(), req2.getCluster());
                        String new_master = req1.getCluster().getReq_older().getId();
                        if (!old_master_req1.equals(new_master)) result.add(new Dependency(new_master,old_master_req1,"proposed","duplicates"));
                        if (!old_master_req2.equals(new_master)) result.add(new Dependency(new_master,old_master_req2,"proposed","duplicates"));
                        clusters_listed.remove(req2.getCluster());
                    }
                }
            }
        }
    }

    private void all_to_masters_algorithm(List<Requirement> loaded_requirements, float threshold, ComparisonBetweenSentences comparer, List<Dependency> result, List<Cluster> clusters_listed) {

        for (int i = 0; i < loaded_requirements.size(); ++i) {
            Requirement req1 = loaded_requirements.get(i);
            for (int j = 0; j < clusters_listed.size(); ++j) {
                Cluster cluster = clusters_listed.get(j);
                if (cluster.getClusterid() != req1.getCluster().getClusterid()) {
                    Requirement master = cluster.getReq_older();
                    Dependency aux = comparer.compare_two_requirements_dep(req1, master);
                    if (aux != null && aux.getDependency_score() >= threshold) {
                        String old_master_req1 = req1.getCluster().getReq_older().getId();
                        String old_master_cluster = cluster.getReq_older().getId();
                        merge_clusters(req1.getCluster(),cluster);
                        String new_master = req1.getCluster().getReq_older().getId();
                        if (!old_master_req1.equals(new_master)) result.add(new Dependency(new_master,old_master_req1,"proposed","duplicates"));
                        if (!old_master_cluster.equals(new_master)) result.add(new Dependency(new_master,old_master_cluster,"proposed","duplicates"));
                        clusters_listed.remove(cluster);
                        --j;
                    }
                }
            }
        }
    }

    @Override
    public void modifyThreshold(String stakeholderId, float threshold) throws InternalErrorException {
        try {
            requirementDAO.updateThreshold(stakeholderId,threshold);
        } catch (SQLException e) {
            throw new InternalErrorException("Database exception: Error while updating threshold.");
        } catch (ClassNotFoundException e) {
            throw new InternalErrorException("Database error: Class not found.");
        }
    }

    //Database
    @Override
    public void savePreprocessed(String stakeholderId, List<Requirement> reqs) throws InternalErrorException {
        show_time("start");
        int i = 0;
        int aux_main = 0;
        for (Requirement r : reqs) {
            try {
                System.out.println(reqs.size() - i);
                int aux = i * 100 / reqs.size();
                if (aux >= aux_main + 10) {
                    aux_main = aux;
                    show_time(aux + "%");
                }
                r.compute_sentence();
                requirementDAO.savePreprocessed(r, stakeholderId);
                ++i;
            } catch (ClassNotFoundException e) {
                throw new InternalErrorException("Database error: Class not found.");
            } catch (SQLException e) {
                throw new InternalErrorException("Database error: Error while saving a requirement with id:"+r.getId()+".");
            }
        }
        show_time("finish");
    }

    @Override
    public void clearDB() throws InternalErrorException {
        try {
            requirementDAO.clearDB();
        } catch (ClassNotFoundException e) {
            throw new InternalErrorException("Database error: Class not found.");
        } catch (SQLException e) {
            throw new InternalErrorException("Database error: Error while clearing database.");
        }
    }

    /*
    Test operations
     */


    public String getDependencies(String stakeholderid) throws InternalErrorException {
        try {
            List<Dependency> result = requirementDAO.getDependencies(stakeholderid);

            JSONObject json = new JSONObject();
            JSONArray jsonArray = new JSONArray();

            for (Dependency dependency : result) {
                JSONObject json_dep = new JSONObject();
                json_dep.put("fromid", dependency.getFromid());
                json_dep.put("toid", dependency.getToid());
                json_dep.put("status", dependency.getStatus());
                jsonArray.put(json_dep);
            }

            json.put("dependencies", jsonArray);
            return json.toString();

        } catch (SQLException e) {
            throw new InternalErrorException("Database Error: Error loading a dependency");
        } catch (ClassNotFoundException e) {
            throw new InternalErrorException("Database error: Class not found.");
        }
    }

    public String getRequirements(String stakeholderid) throws InternalErrorException {
        try {
            List<Requirement> result = requirementDAO.getRequirements(stakeholderid);

            JSONObject json = new JSONObject();
            JSONArray jsonArray = new JSONArray();

            for (Requirement requirement : result) {
                JSONObject json_dep = new JSONObject();
                json_dep.put("id",requirement.getId());
                json_dep.put("cluster",requirement.getClusterId());
                json_dep.put("master",requirement.isMaster());
                jsonArray.put(json_dep);
            }

            json.put("requirements", jsonArray);
            return json.toString();

        } catch (SQLException e) {
            throw new InternalErrorException("Database Error: Error loading a requirement");
        } catch (ClassNotFoundException e) {
            throw new InternalErrorException("Database error: Class not found.");
        }
    }

    public String getStakeholders() throws InternalErrorException {
        try {
            List<Stakeholder> result = requirementDAO.getStakeholders();

            JSONObject json = new JSONObject();
            JSONArray jsonArray = new JSONArray();

            for (Stakeholder stakeholder : result) {
                JSONObject json_dep = new JSONObject();
                json_dep.put("id",stakeholder.getId());
                json_dep.put("threshold",stakeholder.getThreshold());
                jsonArray.put(json_dep);
            }

            json.put("stakeholders", jsonArray);
            return json.toString();

        } catch (SQLException e) {
            throw new InternalErrorException("Database Error: Error loading a stakeholder");
        } catch (ClassNotFoundException e) {
            throw new InternalErrorException("Database error: Class not found.");
        }
    }





    /*
    auxiliary operations
     */

    private void update_deleted_rejected_dependencies(List<Dependency> dependencies, HashMap<String,Requirement> requirements, List<Requirement> free_requirements, ComparisonBetweenSentences comparer, float threshold, String stakeholderid) throws InternalErrorException, BadRequestException {

        for (Dependency dependency: dependencies) {
            if (dependency.getDependency_type().equals("duplicates") && (dependency.getStatus().equals("deleted") || dependency.getStatus().equals("rejected"))) {
                try {
                    try {
                        requirementDAO.deleteDependency(dependency, stakeholderid);
                    } catch (SQLException e) {
                        throw new InternalErrorException("Database error: Error while deleting a dependency from database");
                    }
                    try {
                        requirementDAO.saveDependency(dependency, false, stakeholderid);
                    } catch (SQLException e) {
                        throw new BadRequestException("Database error: Error while adding a new dependency to the database. There are no requirements with id " + dependency.getFromid() + " or " + dependency.getToid());
                    }
                } catch (ClassNotFoundException e) {
                    throw new InternalErrorException("Database error: Class not found.");
                }
                Requirement req1 = requirements.get(dependency.getFromid());
                Requirement req2 = requirements.get(dependency.getToid());
                if (req1.getCluster() != null && req2.getCluster() != null) {
                    if (!more_than_two_in_cluster(req1.getCluster())) {
                        free_requirements.add(req1);
                        free_requirements.add(req2);
                    } else {
                        split_and_join_clusters(req1,req2,comparer,threshold);
                    }
                }
            }
        }

    }

    private boolean more_than_two_in_cluster(Cluster req1) {
        return req1.getSpecifiedRequirements().size() > 2;
    }

    private void split_and_join_clusters(Requirement req1, Requirement req2, ComparisonBetweenSentences comparer, float threshold) {

        Cluster aux_cluster = req1.getCluster();
        List<Requirement> split1 = new ArrayList<>();
        split1.add(req1);
        List<Requirement> split2 = new ArrayList<>();
        split2.add(req2);

        boolean found = false;
        for (int i = 0; (!found) && (i < aux_cluster.getSpecifiedRequirements().size()); ++i) {
            Requirement requirement1 = aux_cluster.getSpecifiedRequirements().get(i);
            boolean found1 = false;
            boolean found2 = false;
            for (int j = 0; (!found1) && (j < split1.size()); ++j) {
                Requirement requirement2 = split1.get(j);
                Dependency aux = comparer.compare_two_requirements_dep(requirement1,requirement2);
                if(aux != null && aux.getDependency_score() > threshold) {
                    found1 = true;
                    split1.add(requirement2);
                }
            }
            for (int j = 0; (!found2) && (j < split2.size()); ++j) {
                Requirement requirement2 = split2.get(j);
                Dependency aux = comparer.compare_two_requirements_dep(requirement1,requirement2);
                if(aux != null && aux.getDependency_score() > threshold) {
                    found2 = true;
                    split2.add(requirement2);
                }
            }
            if(found1 && found2) found = true;
        }

        if (!found) {
            Cluster cluster = new Cluster();
            for (Requirement requirement: split2) {
                cluster.addReq(requirement);
            }
        }
    }

    private void update_deleted_requirements(List<Requirement> requirements, List<Requirement> free_requirements, List<Cluster> clusters, String stakeholderid) throws InternalErrorException {

        for (Requirement requirement: requirements) {
            free_requirements.remove(requirement);
            try {
                requirementDAO.deleteRequirementDependencies(requirement,stakeholderid);
            } catch (SQLException e) {
                throw new InternalErrorException("Database error: Error while deleting a dependency from database.");
            } catch (ClassNotFoundException e) {
                throw new InternalErrorException("Database error: Class not found.");
            }

            if(requirement.getCluster() != null) {
                Cluster aux_cluster = requirement.getCluster();
                aux_cluster.removeReq(requirement);
                if (aux_cluster.getSpecifiedRequirements().size() == 1) free_requirements.add(requirement);
                if (aux_cluster.getSpecifiedRequirements().size() <= 1) clusters.remove(aux_cluster);
                try {
                    requirementDAO.deleteRequirement(requirement, stakeholderid);
                } catch (SQLException e) {
                    throw new InternalErrorException("Database error: Error while deleting a requirement with id " + requirement.getId() + " from database.");
                } catch (ClassNotFoundException e) {
                    throw new InternalErrorException("Database error: Class not found.");
                }
            }
        }
    }

    private void update_added_accepted_dependencies(List<Dependency> dependencies, HashMap<String,Requirement> requirements, List<Cluster> clusters, List<Dependency> dependencies_to_add, String stakeholderid) throws InternalErrorException {

        for (Dependency dependency: dependencies) {
            try {
                try {
                    requirementDAO.deleteDependency(dependency, stakeholderid);
                } catch (SQLException e) {
                    throw new InternalErrorException("Database error: Error while deleting a dependency from the database");
                }
                dependencies_to_add.add(dependency);
                Requirement req1 = requirements.get(dependency.getFromid());
                Requirement req2 = requirements.get(dependency.getToid());
                if (req1.getCluster() == null && req2.getCluster() == null) {
                    Cluster new_cluster = new Cluster();
                    new_cluster.addReq(req1);
                    new_cluster.addReq(req2);
                    clusters.add(new_cluster);
                } else if (req1.getCluster() == null) {
                    req2.getCluster().addReq(req1);
                } else if (req2.getCluster() == null) {
                    req1.getCluster().addReq(req2);
                } else if (req1.getCluster() != req2.getCluster()) {
                    merge_clusters(req1.getCluster(), req2.getCluster());
                    clusters.remove(req2.getCluster());
                }
            } catch (ClassNotFoundException e) {
                throw new InternalErrorException("Database error: Class not found.");
            }

        }
    }

    private void update_edited_requirements(List<Requirement> requirements, List<Requirement> free_requirements, List<Cluster> clusters, String stakeholderid) throws InternalErrorException {

        update_deleted_requirements(requirements,free_requirements,clusters,stakeholderid);

        for (Requirement requirement: requirements) {
            free_requirements.add(requirement);
        }
    }

    private void update_added_requirements(List<Requirement> requirements, List<Cluster> clusters, List<Dependency> result_dependencies, ComparisonBetweenSentences comparer, float threshold, String stakeholderId) throws BadRequestException, InternalErrorException{

        for (Requirement requirement: requirements) {
            List<Cluster> clusters_with_superior_threshold = new ArrayList<>();
            for (Cluster cluster: clusters) {
                Dependency aux_dep = comparer.compare_two_requirements_dep(requirement,cluster.getReq_older());
                if(aux_dep != null && aux_dep.getDependency_score() >= threshold) {
                    clusters_with_superior_threshold.add(cluster);
                }
            }
            if (clusters_with_superior_threshold.size() == 0) {
                Cluster aux_cluster = new Cluster();
                aux_cluster.addReq(requirement);
                clusters.add(aux_cluster);
            } else if (clusters_with_superior_threshold.size() == 1) {
                Cluster aux_cluster_2 = clusters_with_superior_threshold.get(0);
                String req_older = aux_cluster_2.getReq_older().getId();
                aux_cluster_2.addReq(requirement);
                if (aux_cluster_2.getReq_older().getId().equals(requirement.getId())) result_dependencies.add(new Dependency(requirement.getId(), req_older, "proposed","duplicates"));
            } else {
                Cluster aux_cluster = clusters_with_superior_threshold.get(0);
                aux_cluster.addReq(requirement);
                List<String> reqs_older = new ArrayList<>();
                for (int i = 1; i < clusters.size(); ++i) {
                    Cluster aux_cluster2 = clusters_with_superior_threshold.get(i);
                    reqs_older.add(aux_cluster2.getReq_older().getId());
                    merge_clusters(aux_cluster,aux_cluster2);
                    clusters.remove(aux_cluster2);
                }
                if (aux_cluster.getReq_older().getId().equals(requirement.getId())) {
                    for (String req_older: reqs_older) {
                        result_dependencies.add(new Dependency(requirement.getId(),req_older,"proposed","duplicates"));
                    }
                } else {
                    for (String req_older: reqs_older) {
                        result_dependencies.add(new Dependency(aux_cluster.getReq_older().getId(),req_older,"proposed","duplicates"));
                    }
                }
            }
        }

        for (Requirement requirement: requirements) {
            try {
                requirement.compute_sentence();
                requirementDAO.savePreprocessed(requirement,stakeholderId);
                //save memory
                requirement.setSentence_name(null);
                requirement.setSentence_text(null);
            } catch (SQLException e) {
                throw new BadRequestException("Database exception: Error while saving a requirement with id " + requirement.getId() + " to the database. There is no stakeholder called "+stakeholderId+".");
            } catch (ClassNotFoundException e) {
                throw new InternalErrorException("Database error: Class not found.");
            }
        }
    }

    private void merge_clusters(Cluster cluster1, Cluster cluster2) {

        for (Requirement requirement: cluster2.getSpecifiedRequirements()) {
            cluster1.addReq(requirement);
        }
    }

    private void delete_last_comma(String path) throws InternalErrorException{
        try {
            RandomAccessFile f = new RandomAccessFile(path, "rw");
            long length = f.length() - 1;
            f.setLength(length);
            f.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new InternalErrorException(e.getMessage());
        }
    }

    private void show_time(String text) {
        LocalDateTime now = LocalDateTime.now();
        int year = now.getYear();
        int month = now.getMonthValue();
        int day = now.getDayOfMonth();
        int hour = now.getHour();
        int minute = now.getMinute();
        System.out.println(text + " -- " + hour + ":" + minute + "  " + month + "/" + day + "/" + year);
    }

    private void write_to_file(String text, Path p) throws InternalErrorException {
        try (BufferedWriter writer = Files.newBufferedWriter(p, StandardOpenOption.APPEND)) {
            writer.write(text);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new InternalErrorException("Write start to file fail");
        }
    }

    private GreedyComparer new_comparer() {

        GreedyComparer greedyComparerWNLin = new GreedyComparer(wnMetricLin, 0.3f, true);

        Requirement aux1 = new Requirement();
        aux1.setName("testing");
        Requirement aux2 = new Requirement();
        aux2.setName("just waiting for an answer");
        aux1.compute_sentence();
        aux2.compute_sentence();

        greedyComparerWNLin.computeSimilarity(aux1.getSentence_name(), aux2.getSentence_name());

        return greedyComparerWNLin;
    }
}
