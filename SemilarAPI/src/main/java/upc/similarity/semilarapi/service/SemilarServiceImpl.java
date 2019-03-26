package upc.similarity.semilarapi.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
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
    public void modifyThreshold(boolean type, String filename, String compare, String stakeholderId, float threshold) throws InternalErrorException, BadRequestException {

        boolean new_stakeholder = true;
        try {
            requirementDAO.createStakeholder(stakeholderId,threshold,0);
        } catch (SQLException e) {
            if (!e.getMessage().contains("PRIMARY KEY")) throw new InternalErrorException("Database exception: Error while creating new organization.");
            new_stakeholder = false;
        } catch (ClassNotFoundException e) {
            throw new InternalErrorException("Database error: Class not found.");
        }

        List<Requirement> requirements = new ArrayList<>();
        List<Dependency> dependencies = new ArrayList<>();

        if (!new_stakeholder) {
            try {
                try {
                    requirementDAO.updateThreshold(stakeholderId, threshold);
                } catch (SQLException e) {
                    throw new InternalErrorException("Database exception: Error while updating threshold.");
                }
                try {
                    requirements = requirementDAO.getRequirements(stakeholderId);
                } catch (SQLException e) {
                    throw new InternalErrorException("Database exception: Error while loading stakeholder requirements.");
                }
                try {
                    dependencies = requirementDAO.getDependencies(stakeholderId);
                } catch (SQLException e) {
                    throw new InternalErrorException("Database exception: Error while loading stakeholder dependencies.");
                }
            } catch (ClassNotFoundException e) {
                throw new InternalErrorException("Database error: Class not found.");
            }

            long last_cluster_id = 0;

            HashMap<String, Requirement> requirements_loaded = new HashMap<>();
            for (Requirement requirement : requirements) requirements_loaded.put(requirement.getId(), requirement);

            List<Cluster> clusters_listed = new ArrayList<>();

            show_time("start loop dependencies");
            for (Dependency dependency : dependencies) {
                if (dependency.getDependency_type() != null && dependency.getStatus() != null) {
                    if (dependency.getDependency_type().equals("duplicates") && dependency.getStatus().equals("accepted")) {
                        Requirement req1 = requirements_loaded.get(dependency.getFromid());
                        Requirement req2 = requirements_loaded.get(dependency.getToid());
                        if (req1.getCluster() == null && req2.getCluster() == null) {
                            Cluster new_cluster = new Cluster(last_cluster_id);
                            ++last_cluster_id;
                            new_cluster.addReq(req1);
                            new_cluster.addReq(req2);
                            clusters_listed.add(new_cluster);
                        } else if (req1.getCluster() == null) {
                            req2.getCluster().addReq(req1);
                        } else if (req2.getCluster() == null) {
                            req1.getCluster().addReq(req2);
                        } else if (req1.getCluster() != req2.getCluster()) {
                            merge_clusters(req1.getCluster(), req2.getCluster());
                            clusters_listed.remove(req2.getCluster());
                        }
                    }
                }
            }
            show_time("finish loop dependencies");

            show_time("start requirements loop");
            for (Requirement requirement: requirements) {
                requirement.compute_sentence();
                if (requirement.getCluster() == null) {
                    Cluster new_cluster = new Cluster(last_cluster_id);
                    ++last_cluster_id;
                    new_cluster.addReq(requirement);
                }
            }
            show_time("finish requirements loop");

            try {
                requirementDAO.updateLastClusterId(last_cluster_id, stakeholderId);
            } catch (SQLException e) {
                throw new BadRequestException("Database exception: Error while updating clusters.");
            } catch (ClassNotFoundException e) {
                throw new InternalErrorException("Database error: Class not found.");
            }

            ComparisonBetweenSentences comparer = new ComparisonBetweenSentences(greedyComparerWNLin, compare, threshold, true, component);

            show_time("start loop");
            List<Dependency> result = new ArrayList<>();

            if (type) all_to_all_algorithm(requirements, threshold, comparer, result, clusters_listed, stakeholderId);
            else all_to_masters_algorithm(requirements, threshold, comparer, result, clusters_listed, stakeholderId);
            show_time("stop loop");

            //save changes to db
            for (Requirement requirement : requirements) {
                try {
                    requirementDAO.updateRequirementCluster(requirement, stakeholderId);
                    //save memory
                    requirement.setSentence_name(null);
                    requirement.setSentence_text(null);
                } catch (SQLException e) {
                    throw new InternalErrorException("Database exception: Error while saving a requirement with id " + requirement.getId() + " to the database.");
                } catch (ClassNotFoundException e) {
                    throw new InternalErrorException("Database error: Class not found.");
                }
            }
        }
    }

    @Override
    public void resetStakeholder(String stakeholderId) throws InternalErrorException, BadRequestException {

        try {
            requirementDAO.resetStakeholder(stakeholderId);
        } catch (SQLException e) {
            throw new InternalErrorException("Database exception: Error while deleting stakeholder's tables.");
        } catch (ClassNotFoundException e) {
            throw new InternalErrorException("Database error: Class not found.");
        }

    }

    @Override
    public void iniClusters(String stakeholderId, String filename, IniClusterOp input) throws InternalErrorException, BadRequestException {

        long last_cluster_id;

        try {
            last_cluster_id = requirementDAO.getLastClusterId(stakeholderId);
        } catch (SQLException e) {
            if (e.getMessage().contains("Stakeholder with id")) throw new BadRequestException("Database exception: " + e.getMessage());
            throw new InternalErrorException("Database exception: Error while loading clusters.");
        } catch (ClassNotFoundException e) {
            throw new InternalErrorException("Database error: Class not found.");
        }

        HashMap<String,Requirement> requirements_loaded = new HashMap<>();
        for (Requirement requirement: input.getRequirements()) requirements_loaded.put(requirement.getId(), requirement);

        List<Cluster> clusters = new ArrayList<>();

        show_time("start loop dependencies");
        for (Dependency dependency: input.getDependencies()) {
            if (dependency.getDependency_type() != null && dependency.getStatus() != null) {
                if (dependency.getDependency_type().equals("duplicates") && dependency.getStatus().equals("accepted")) {
                    Requirement req1 = requirements_loaded.get(dependency.getFromid());
                    Requirement req2 = requirements_loaded.get(dependency.getToid());
                    if (req1.getCluster() == null && req2.getCluster() == null) {
                        Cluster new_cluster = new Cluster(last_cluster_id);
                        ++last_cluster_id;
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
                    Cluster new_cluster = new Cluster(last_cluster_id);
                    ++last_cluster_id;
                    new_cluster.addReq(requirement);
                }
                requirementDAO.savePreprocessed(requirement,stakeholderId);
                //save memory
                requirement.setSentence_name(null);
                requirement.setSentence_text(null);
            } catch (SQLException e) {
                throw new BadRequestException("Database exception: Error while saving a requirement with id " + requirement.getId() + " to the database. The requirement appears two times in the input array.");
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

        try {
            requirementDAO.updateLastClusterId(last_cluster_id,stakeholderId);
        } catch (SQLException e) {
            throw new BadRequestException("Database exception: Error while updating clusters.");
        } catch (ClassNotFoundException e) {
            throw new InternalErrorException("Database error: Class not found.");
        }
    }

    @Override
    public void computeClusters(boolean type, String compare, String stakeholderId, String filename) throws InternalErrorException, BadRequestException {

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
        HashMap<Long,Cluster> clusters = new HashMap<>();

        for (Requirement requirement: loaded_requirements) {
            long clusterId = requirement.getClusterId();
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

        if (type) all_to_all_algorithm(loaded_requirements,threshold,comparer,result,clusters_listed,stakeholderId);
        else all_to_masters_algorithm(loaded_requirements,threshold,comparer,result,clusters_listed,stakeholderId);
        show_time("stop loop");

        //save changes to db
        for (Requirement requirement: loaded_requirements) {
            try {
                requirementDAO.updateRequirementCluster(requirement,stakeholderId);
                //save memory
                requirement.setSentence_name(null);
                requirement.setSentence_text(null);
            } catch (SQLException e) {
                throw new InternalErrorException("Database exception: Error while saving a requirement with id " + requirement.getId() + " to the database.");
            } catch (ClassNotFoundException e) {
                throw new InternalErrorException("Database error: Class not found.");
            }
        }


        Path p = Paths.get("../testing/output/"+filename);
        String s = System.lineSeparator() + "{\"dependencies\": [";
        write_to_file(s,p);
        boolean firstComa = true;

        for(Dependency dependency: result) {
            String aux = System.lineSeparator() + dependency.print_json();
            if (!firstComa) aux = "," + aux;
            firstComa = false;
            write_to_file(aux,p);
        }

        s = System.lineSeparator() + "]}";
        write_to_file(s,p);
    }

    private void all_to_all_algorithm(List<Requirement> loaded_requirements, float threshold, ComparisonBetweenSentences comparer, List<Dependency> result, List<Cluster> clusters_listed, String stakeholderid) throws InternalErrorException {

        for (int i = 0; i < loaded_requirements.size(); ++i) {
            Requirement req1 = loaded_requirements.get(i);
            for (int j = i + 1; j < loaded_requirements.size(); ++j) {
                Requirement req2 = loaded_requirements.get(j);
                Dependency aux_db = null;
                if (!stakeholderid.equals("")) {
                    try {
                        aux_db = requirementDAO.getDependency(req1.getId(), req2.getId(), stakeholderid);
                    } catch (SQLException e) {
                        //continue
                        if (!e.getMessage().contains("The dependency does not exist in the database"))
                            throw new InternalErrorException("Database error: Error while getting a dependency.");
                        //e.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        throw new InternalErrorException("Database error: Class not found.");
                    }
                }
                //only continue if the dependency does not exist in the DB
                if (aux_db == null) {
                    Dependency aux = comparer.compare_two_requirements_dep(req1, req2);
                    if (aux != null && aux.getDependency_score() >= threshold) {
                        if (req1.getCluster() != req2.getCluster()) {
                            String old_master_req1 = req1.getCluster().getReq_older().getId();
                            String old_master_req2 = req2.getCluster().getReq_older().getId();
                            Cluster aux_cluster = req2.getCluster();
                            merge_clusters(req1.getCluster(), req2.getCluster());
                            String new_master = req1.getCluster().getReq_older().getId();
                            if (!stakeholderid.equals("")) {
                                if (!old_master_req1.equals(new_master))
                                    result.add(new Dependency(new_master, old_master_req1, "proposed", "duplicates"));
                                if (!old_master_req2.equals(new_master))
                                    result.add(new Dependency(new_master, old_master_req2, "proposed", "duplicates"));
                            }
                            clusters_listed.remove(aux_cluster);
                        }
                    }
                }
            }
        }
    }

    private void all_to_masters_algorithm(List<Requirement> loaded_requirements, float threshold, ComparisonBetweenSentences comparer, List<Dependency> result, List<Cluster> clusters_listed, String stakeholderid) throws InternalErrorException {

        for (int i = 0; i < loaded_requirements.size(); ++i) {
            Requirement req1 = loaded_requirements.get(i);
            for (int j = 0; j < clusters_listed.size(); ++j) {
                Cluster cluster = clusters_listed.get(j);
                if (cluster.getClusterid() != req1.getCluster().getClusterid()) {
                    Requirement master = cluster.getReq_older();
                    Dependency aux_db = null;
                    if (!stakeholderid.equals("")) {
                        try {
                            aux_db = requirementDAO.getDependency(req1.getId(), master.getId(), stakeholderid);
                        } catch (SQLException e) {
                            //continue
                            //e.printStackTrace();
                            if (!e.getMessage().contains("The dependency does not exist in the database"))
                                throw new InternalErrorException("Database error: Error while getting a dependency.");
                        } catch (ClassNotFoundException e) {
                            throw new InternalErrorException("Database error: Class not found.");
                        }
                    }
                    //only continue if the dependency does not exist in the DB
                    if (aux_db == null) {
                        Dependency aux = comparer.compare_two_requirements_dep(req1, master);
                        if (aux != null && aux.getDependency_score() >= threshold) {
                            String old_master_req1 = req1.getCluster().getReq_older().getId();
                            String old_master_cluster = cluster.getReq_older().getId();
                            merge_clusters(req1.getCluster(), cluster);
                            String new_master = req1.getCluster().getReq_older().getId();
                            if (!stakeholderid.equals("")) {
                                if (!old_master_req1.equals(new_master))
                                    result.add(new Dependency(new_master, old_master_req1, "proposed", "duplicates"));
                                if (!old_master_cluster.equals(new_master))
                                    result.add(new Dependency(new_master, old_master_cluster, "proposed", "duplicates"));
                            }
                            clusters_listed.remove(cluster);
                            --j;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void updateClusters(boolean type, String compare, String stakeholderId, String filename, IniClusterOp input) throws InternalErrorException, BadRequestException {

        show_time("start initialization");

        long last_cluster_id;

        try {
            last_cluster_id = requirementDAO.getLastClusterId(stakeholderId);
        } catch (SQLException e) {
            if (e.getMessage().contains("Stakeholder with id")) throw new BadRequestException("Database exception: " + e.getMessage());
            throw new InternalErrorException("Database exception: Error while loading clusters.");
        } catch (ClassNotFoundException e) {
            throw new InternalErrorException("Database error: Class not found.");
        }

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
        HashMap<Long,Cluster> clusters = new HashMap<>();
        HashMap<String,Requirement> hash_requirements = new HashMap<>();

        for (Requirement requirement: loaded_requirements) {
            hash_requirements.put(requirement.getId(),requirement);
            long clusterId = requirement.getClusterId();
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

        HashMap<String,Dependency> dependencies_to_add = new HashMap<>();
        List<Dependency> result_dependencies = new ArrayList<>();
        List<Requirement> requirements_to_add = new ArrayList<>();
        List<Requirement> requirements_to_update = new ArrayList<>();

        show_time("finish initialization");

        show_time("start computing");
        ComparisonBetweenSentences comparer = new ComparisonBetweenSentences(greedyComparerWNLin,compare,threshold,true,component);

        update_deleted_rejected_dependencies(dependencies_to_add,requirements_to_update,requirements_to_add,input.getDependencies(),clusters_listed,hash_requirements,comparer,threshold, stakeholderId,last_cluster_id);

        update_deleted_edited_requirements(requirements_to_update,requirements_to_add,hash_requirements,input.getRequirements(),clusters_listed,stakeholderId);

        update_added_accepted_dependencies(requirements_to_update,input.getDependencies(),hash_requirements,clusters_listed,dependencies_to_add,stakeholderId,last_cluster_id);

        update_added_requirements(requirements_to_update,input.getRequirements(),requirements_to_add,clusters_listed,dependencies_to_add,result_dependencies,comparer,threshold,stakeholderId,last_cluster_id,type);

        try {
            requirementDAO.updateLastClusterId(last_cluster_id,stakeholderId);
        } catch (SQLException e) {
            throw new BadRequestException("Database exception: Error while updating clusters.");
        } catch (ClassNotFoundException e) {
            throw new InternalErrorException("Database error: Class not found.");
        }

        for (Requirement requirement: input.getRequirements()) {
            if (requirement.getStatus() != null && (requirement.getStatus().equals("accepted") || requirement.getStatus().equals("added"))) {
                try {
                    //requirement.compute_sentence();
                    requirementDAO.savePreprocessed(requirement, stakeholderId);
                    //save memory
                    requirement.setSentence_name(null);
                    requirement.setSentence_text(null);
                } catch (SQLException e) {
                    try {
                        if (e.getMessage().contains("PRIMARY KEY"))
                            requirementDAO.updateRequirementCluster(requirement, stakeholderId);
                        else
                            throw new InternalErrorException("Database exception: Error while saving a requirement with id " + requirement.getId() + " to the database.");
                    } catch (SQLException g) {
                        throw new InternalErrorException("Database exception: Error while updating a requirement with id " + requirement.getId() + ".");
                    } catch (ClassNotFoundException g) {
                        throw new InternalErrorException("Database error: Class not found.");
                    }
                } catch (ClassNotFoundException e) {
                    throw new InternalErrorException("Database error: Class not found.");
                }
            }
        }

        for (Requirement requirement: requirements_to_add) {
            try {
                //requirement.compute_sentence();
                requirementDAO.updateRequirementCluster(requirement, stakeholderId);
                //save memory
                requirement.setSentence_name(null);
                requirement.setSentence_text(null);
            } catch (SQLException e) {
                throw  new InternalErrorException("Database exception: Error while updating a requirement with id " + requirement.getId() + ".");
            } catch (ClassNotFoundException e) {
                throw new InternalErrorException("Database error: Class not found.");
            }
        }

        for (Requirement requirement: requirements_to_update) {
            try {
                //requirement.compute_sentence();
                requirementDAO.updateRequirementCluster(requirement, stakeholderId);
                //save memory
                requirement.setSentence_name(null);
                requirement.setSentence_text(null);
            } catch (SQLException e) {
                throw  new InternalErrorException("Database exception: Error while updating a requirement with id " + requirement.getId() + ".");
            } catch (ClassNotFoundException e) {
                throw new InternalErrorException("Database error: Class not found.");
            }
        }

        Iterator it = dependencies_to_add.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            Dependency dependency = (Dependency) pair.getValue();
            try {
                if (dependency.getStatus().equals("added") || dependency.getStatus().equals("accepted")) requirementDAO.saveDependency(dependency, true, stakeholderId);
                else if (dependency.getStatus().equals("rejected") || dependency.getStatus().equals("deleted")) requirementDAO.saveDependency(dependency, false, stakeholderId);
            } catch (SQLException e) {
                //TODO avisar de esto al usuario? que ha metido una dependencia entre dos requisitos que no existen
                if (!e.getMessage().contains("FOREIGN KEY")) throw new BadRequestException("Database exception: Error while saving a dependency (fromid:" + dependency.getFromid() + ", toid:" + dependency.getToid() + ") to the database.");
            } catch (ClassNotFoundException e) {
                throw new InternalErrorException("Database error: Class not found.");
            }
            it.remove();
        }

        Path p = Paths.get("../testing/output/"+filename);
        String s = System.lineSeparator() + "{\"dependencies\": [";
        write_to_file(s,p);
        boolean firstComa = true;

        for (Dependency dependency: result_dependencies) {
            Dependency aux_dep = null;
            try {
                aux_dep = requirementDAO.getDependency(dependency.getFromid(),dependency.getToid(),stakeholderId);
            } catch (SQLException e) {
                if (!e.getMessage().equals("The dependency does not exist in the database")) throw new BadRequestException("Database exception: Error while saving a dependency (fromid:" + dependency.getFromid() + "toid:" + dependency.getToid() + ") to the database.");
            } catch (ClassNotFoundException e) {
                throw new InternalErrorException("Database error: Class not found.");
            }
            if (aux_dep == null) {
                String aux = System.lineSeparator() + dependency.print_json();
                if (!firstComa) aux = "," + aux;
                firstComa = false;
                write_to_file(aux, p);
            }
        }

        s = System.lineSeparator() + "]}";
        write_to_file(s,p);

        show_time("finish computing");
    }

    @Override
    public void reqProject(String stakeholderId, String filename, String requirement_id, Projects input) throws BadRequestException, InternalErrorException {

        boolean stakeholder;
        try {
            stakeholder = requirementDAO.existStakeholder(stakeholderId);
        } catch (SQLException e) {
            throw new InternalErrorException("Database exception: Error while loading the stakeholder.");
        } catch (ClassNotFoundException e) {
            throw new InternalErrorException("Database error: Class not found.");
        }
        if (!stakeholder) throw new BadRequestException("Database exception: There is no stakeholder with id " + stakeholderId + ".");

        List<String> ids = new ArrayList<>();
        Requirement requirement = null;
        try {
            try {
                requirement = requirementDAO.getRequirement(requirement_id,stakeholderId);
            } catch (SQLException e) {
                if (e.getMessage().contains("does not exist in the database")) throw new BadRequestException("Database error: " + e.getMessage());
                throw new InternalErrorException("Database error: Error while loading the requirement.");
            }
            try {
                ids = requirementDAO.getClusterRequirementsId(requirement.getClusterId(),stakeholderId);
            } catch (SQLException e) {
                throw new InternalErrorException("Database exception: Error while loading requirements.");
            }
        }  catch (ClassNotFoundException e) {
            throw new InternalErrorException("Database error: Class not found.");
        }

        HashSet<String> project_requirements_ids = new HashSet();
        project_requirements_ids.addAll(input.getRequirements());

        Path p = Paths.get("../testing/output/"+filename);
        String s = System.lineSeparator() + "{\"dependencies\": [";
        write_to_file(s,p);
        boolean firstComa = true;

        for(String id: ids) {
            if (project_requirements_ids.contains(id) && !id.equals(requirement.getId())) {
                Dependency aux_db = null;
                try {
                    aux_db = requirementDAO.getDependency(requirement.getId(),id,stakeholderId);
                } catch (SQLException e) {
                    if (!e.getMessage().contains("The dependency does not exist in the database")) throw new InternalErrorException("Database error: Error while loading a dependency.");
                } catch (ClassNotFoundException e) {
                    throw new InternalErrorException("Database error: Class not found.");
                }
                if (aux_db == null) {
                    Dependency dependency = new Dependency(requirement.getId(), id, "proposed", "duplicates");
                    String aux = System.lineSeparator() + dependency.print_json();
                    if (!firstComa) aux = "," + aux;
                    firstComa = false;
                    write_to_file(aux, p);
                }
                project_requirements_ids.remove(id);
            }
        }

        s = System.lineSeparator() + "]}";
        write_to_file(s,p);


    }

    @Override
    public void projects(String stakeholderId, String filename, Projects input) throws BadRequestException, InternalErrorException {

        boolean stakeholder;
        try {
            stakeholder = requirementDAO.existStakeholder(stakeholderId);
        } catch (SQLException e) {
            throw new InternalErrorException("Database exception: Error while loading the stakeholder.");
        } catch (ClassNotFoundException e) {
            throw new InternalErrorException("Database error: Class not found.");
        }
        if (!stakeholder) throw new BadRequestException("Database exception: There is no stakeholder with id " + stakeholderId + ".");

        Path p = Paths.get("../testing/output/"+filename);
        String s = System.lineSeparator() + "{\"dependencies\": [";
        write_to_file(s,p);
        boolean firstComa = true;

        List<Cluster> listed_clusters = new ArrayList<>();
        HashMap<Long,Cluster> clusters = new HashMap<>();
        for (String requirementId: input.getRequirements()) {
            try {
                Requirement requirement = requirementDAO.getRequirement(requirementId, stakeholderId);
                if (clusters.containsKey(requirement.getClusterId())) {
                    clusters.get(requirement.getClusterId()).addReq(requirement);
                } else {
                    Cluster aux = new Cluster(requirement.getClusterId());
                    aux.addReq(requirement);
                    clusters.put(requirement.getClusterId(),aux);
                    listed_clusters.add(aux);
                }
            } catch (SQLException e) {
                if (e.getMessage().contains("does not exist in the database")) throw new BadRequestException("Database error: " + e.getMessage());
                throw new InternalErrorException("Database error: Error while loading a requirement.");
            } catch (ClassNotFoundException e) {
                throw new InternalErrorException("Database error: Class not found.");
            }
        }

        clusters = null;

        for (int i = 0; i < listed_clusters.size(); ++i) {
            Cluster cluster = listed_clusters.get(i);
            Requirement master = cluster.getReq_older();
            for (int j = 0; j < cluster.getSpecifiedRequirements().size(); ++j) {
                Requirement requirement = cluster.getSpecifiedRequirements().get(j);
                if (!master.getId().equals(requirement.getId())) {
                    Dependency aux_db = null;
                    try {
                        aux_db = requirementDAO.getDependency(master.getId(),requirement.getId(),stakeholderId);
                    } catch (SQLException e) {
                        if (!e.getMessage().contains("The dependency does not exist in the database")) throw new InternalErrorException("Database error: Error while loading a dependency.");
                    } catch (ClassNotFoundException e) {
                        throw new InternalErrorException("Database error: Class not found.");
                    }
                    if (aux_db == null) {
                        Dependency dependency = new Dependency(master.getId(),requirement.getId(), "proposed", "duplicates");
                        String aux = System.lineSeparator() + dependency.print_json();
                        if (!firstComa) aux = "," + aux;
                        firstComa = false;
                        write_to_file(aux, p);
                    }
                }
            }
        }

        s = System.lineSeparator() + "]}";
        write_to_file(s,p);
    }


    @Override
    public void reqProjectNew(boolean type, String compare, float threshold, String filename, ReqProjNewOp input) throws InternalErrorException {

        long cluster_id = 0;

        List<Cluster> clusters = new ArrayList<>();
        Set<String> ids = new HashSet<>(); //avoid repeated clusters

        List<Requirement> requirements = new ArrayList<>();

        for (Requirement requirement: input.getRequirements()) {
            if (!ids.contains(requirement.getId())) {
                requirement.compute_sentence();
                ids.add(requirement.getId());
                Cluster cluster = new Cluster(cluster_id);
                cluster.addReq(requirement);
                ++cluster_id;
                clusters.add(cluster);
                requirements.add(requirement);
            }
        }

        for (Requirement requirement: input.getProject_requirements()) {
            if (!ids.contains(requirement.getId())) {
                requirement.compute_sentence();
                ids.add(requirement.getId());
                Cluster cluster = new Cluster(cluster_id);
                cluster.addReq(requirement);
                ++cluster_id;
                clusters.add(cluster);
                requirements.add(requirement);
            }
        }

        ids = null;

        ComparisonBetweenSentences comparer = new ComparisonBetweenSentences(greedyComparerWNLin,compare,threshold,true,component);

        if (type) all_to_all_algorithm(requirements,threshold,comparer,new ArrayList<>(),clusters,"");
        else all_to_masters_algorithm(requirements,threshold,comparer,new ArrayList<>(),clusters, "");

        Path p = Paths.get("../testing/output/"+filename);
        String s = System.lineSeparator() + "{\"dependencies\": [";

        write_to_file(s,p);

        boolean firstComa = true;
        for (Requirement requirement1: input.getRequirements()) {
            Cluster cluster = requirement1.getCluster();
            for (Requirement requirement2: cluster.getSpecifiedRequirements()) {
                if (!input.getRequirements().contains(requirement2)) { //TODO improve efficiency
                    Dependency dependency = new Dependency(requirement1.getId(), requirement2.getId(), "proposed", "duplicates");
                    s = System.lineSeparator() + dependency.print_json();
                    if (!firstComa) s = "," + s;
                    firstComa = false;
                    write_to_file(s, p);
                }
            }
        }

        s = System.lineSeparator() + "]}";
        write_to_file(s,p);
    }

    @Override
    public void projectsNew(boolean type, String compare, float threshold, String filename, Requirements input) throws InternalErrorException {

        long cluster_id = 0;

        List<Cluster> clusters = new ArrayList<>();
        Set<String> ids = new HashSet<>(); //avoid repeated clusters

        List<Requirement> requirements = new ArrayList<>();

        for (Requirement requirement: input.getRequirements()) {
            if (!ids.contains(requirement.getId())) {
                requirement.compute_sentence();
                ids.add(requirement.getId());
                Cluster cluster = new Cluster(cluster_id);
                cluster.addReq(requirement);
                ++cluster_id;
                clusters.add(cluster);
                requirements.add(requirement);
            }
        }

        ids = null;

        ComparisonBetweenSentences comparer = new ComparisonBetweenSentences(greedyComparerWNLin,compare,threshold,true,component);

        if (type) all_to_all_algorithm(requirements,threshold,comparer,new ArrayList<>(),clusters,"");
        else all_to_masters_algorithm(requirements,threshold,comparer,new ArrayList<>(),clusters, "");

        Path p = Paths.get("../testing/output/"+filename);
        String s = System.lineSeparator() + "{\"dependencies\": [";

        write_to_file(s,p);

        boolean firstComa = true;
        for (Cluster cluster: clusters) {
            Requirement master = cluster.getReq_older();
            for (Requirement requirement: cluster.getSpecifiedRequirements()) {
                if (!master.getId().equals(requirement.getId())) {
                    Dependency dependency = new Dependency(master.getId(),requirement.getId(),"proposed", "duplicates");
                    s = System.lineSeparator() + dependency.print_json();
                    if (!firstComa) s = "," + s;
                    firstComa = false;
                    write_to_file(s, p);
                }
            }
        }

        s = System.lineSeparator() + "]}";
        write_to_file(s,p);

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

    private void update_deleted_rejected_dependencies(HashMap<String,Dependency> dependencies_to_add, List<Requirement> requirements_to_update, List<Requirement> requirements_to_add, List<Dependency> dependencies, List<Cluster> clusters_listed, HashMap<String,Requirement> requirements, ComparisonBetweenSentences comparer, float threshold, String stakeholderid, long last_cluster_id) throws InternalErrorException, BadRequestException {

        for (Dependency dependency: dependencies) {
            if (dependency.getDependency_type().equals("duplicates") && ((dependency.getStatus().equals("deleted") || dependency.getStatus().equals("rejected")))) {
                try {
                    requirementDAO.deleteDependency(dependency, stakeholderid);
                } catch (SQLException e) {
                    throw new InternalErrorException("Database error: Error while deleting a dependency from database");
                } catch (ClassNotFoundException e) {
                    throw new InternalErrorException("Database error: Class not found.");
                }
                dependencies_to_add.put(dependency.getFromid()+dependency.getToid(),dependency);
                Requirement req1 = requirements.get(dependency.getFromid());
                Requirement req2 = requirements.get(dependency.getToid());
                if (req1 != null && req2 != null) {
                    if (req1.getCluster() != null && req2.getCluster() != null) {
                        if (req1.getCluster().getClusterid() == req2.getCluster().getClusterid()) {
                            if (!more_than_two_in_cluster(req1.getCluster())) {
                                req1.setStatus("added");
                                req2.setStatus("added");
                                clusters_listed.remove(req1.getCluster());
                                req1.setCluster(null);
                                req2.setCluster(null);
                                requirements_to_add.add(req1);
                                requirements_to_add.add(req2);
                            } else {
                                split_and_join_clusters(requirements_to_update, req1, req2, comparer, threshold, last_cluster_id);
                            }
                        }
                    }
                }
            }
        }

    }

    private boolean more_than_two_in_cluster(Cluster req1) {
        return req1.getSpecifiedRequirements().size() > 2;
    }

    private void split_and_join_clusters(List<Requirement> requirements_to_update, Requirement req1, Requirement req2, ComparisonBetweenSentences comparer, float threshold, long last_cluster_id) {

        Cluster aux_cluster = req1.getCluster();
        HashMap<String,Requirement> split1 = new HashMap<>();
        split1.put(req1.getId(),req1);
        HashMap<String,Requirement> split2 = new HashMap<>();
        split2.put(req2.getId(),req2);

        //TODO no tenemos en cuenta el conocimiento de las tablas de depenedencias !!!!!!!!!!!!!!!!!!!!!

        boolean found = false;
        for (int i = 0; (!found) && (i < aux_cluster.getSpecifiedRequirements().size()); ++i) {
            Requirement requirement1 = aux_cluster.getSpecifiedRequirements().get(i);
            if (!requirement1.getId().equals(req1.getId()) && !requirement1.getId().equals(req2.getId())) {
                boolean found1 = false;
                boolean found2 = false;
                if (!split1.containsKey(requirement1.getId())) {
                    Iterator it1 = split1.entrySet().iterator();
                    while (!found1 && it1.hasNext()) {
                        Map.Entry pair = (Map.Entry) it1.next();
                        Requirement requirement2 = (Requirement) pair.getValue();
                        Dependency aux = comparer.compare_two_requirements_dep(requirement1, requirement2);
                        if (aux != null && aux.getDependency_score() > threshold) {
                            found1 = true;
                            split1.put(requirement1.getId(), requirement1);
                        }
                    }
                }
                if (!split2.containsKey(requirement1.getId())) {
                    Iterator it2 = split2.entrySet().iterator();
                    while (!found2 && it2.hasNext()) {
                        Map.Entry pair = (Map.Entry) it2.next();
                        Requirement requirement2 = (Requirement) pair.getValue();
                        Dependency aux = comparer.compare_two_requirements_dep(requirement1, requirement2);
                        if (aux != null && aux.getDependency_score() > threshold) {
                            found2 = true;
                            split2.put(requirement1.getId(), requirement1);
                        }
                    }
                }
                if (found1 && found2) found = true;
            }
        }

        if (!found) {
            Cluster cluster = new Cluster(last_cluster_id);
            ++last_cluster_id;
            Iterator it2 = split2.entrySet().iterator();
            while (it2.hasNext()) {
                Map.Entry pair = (Map.Entry)it2.next();
                cluster.addReq((Requirement)pair.getValue());
                requirements_to_update.add((Requirement)pair.getValue());
            }
        }
    }

    private void update_deleted_edited_requirements(List<Requirement> requirements_to_update, List<Requirement> requirements_to_add, HashMap<String,Requirement> loaded_requirements, List<Requirement> requirements, List<Cluster> clusters, String stakeholderid) throws InternalErrorException {

        for (Requirement requirement: requirements) {
            if (requirement.getStatus() != null && (requirement.getStatus().equals("deleted") || requirement.getStatus().equals("edited"))) {
                Requirement requirement_db = loaded_requirements.get(requirement.getId());
                if (requirement_db != null) {
                    try {
                        requirementDAO.deleteRequirementDependencies(requirement_db, stakeholderid);
                    } catch (SQLException e) {
                        throw new InternalErrorException("Database error: Error while deleting a dependency from database.");
                    } catch (ClassNotFoundException e) {
                        throw new InternalErrorException("Database error: Class not found.");
                    }

                    Cluster aux_cluster = requirement_db.getCluster();
                    aux_cluster.removeReq(requirement_db);
                    if (aux_cluster.getSpecifiedRequirements().size() == 1) {
                        aux_cluster.getReq_older().setStatus("added");
                        aux_cluster.getReq_older().setMaster(false);
                        aux_cluster.getReq_older().setCluster(null);
                        requirements_to_add.add(aux_cluster.getReq_older());
                    } else if (aux_cluster.getSpecifiedRequirements().size() != 0) requirements_to_update.add(aux_cluster.getReq_older()); //TODO only do this if the master has changed
                    if (aux_cluster.getSpecifiedRequirements().size() <= 1) clusters.remove(aux_cluster);
                    loaded_requirements.remove(requirement_db.getId());
                    try {
                        requirementDAO.deleteRequirement(requirement_db, stakeholderid);
                    } catch (SQLException e) {
                        throw new InternalErrorException("Database error: Error while deleting a requirement with id " + requirement_db.getId() + " from database.");
                    } catch (ClassNotFoundException e) {
                        throw new InternalErrorException("Database error: Class not found.");
                    }

                    if (requirement.getStatus().equals("edited")) {
                        //requirements_to_add.add(requirement);
                        requirement.setStatus("added");
                        requirement.setMaster(false);
                        requirement.setCluster(null);
                    }
                }
            }
        }
    }

    private void update_added_accepted_dependencies(List<Requirement> requirements_to_update, List<Dependency> dependencies, HashMap<String,Requirement> requirements, List<Cluster> clusters, HashMap<String,Dependency> dependencies_to_add, String stakeholderid, long last_cluster_id) throws InternalErrorException {

        for (Dependency dependency: dependencies) {
            if (dependency.getDependency_type().equals("duplicates") && (dependency.getStatus().equals("added") || dependency.getStatus().equals("accepted"))) {
                try {
                    requirementDAO.deleteDependency(dependency, stakeholderid);
                } catch (SQLException e) {
                    throw new InternalErrorException("Database error: Error while deleting a dependency from the database");
                } catch (ClassNotFoundException e) {
                    throw new InternalErrorException("Database error: Class not found.");
                }
                dependencies_to_add.put(dependency.getToid()+dependency.getFromid(),dependency);
                Requirement req1 = requirements.get(dependency.getFromid());
                Requirement req2 = requirements.get(dependency.getToid());
                if (req1 != null && req2 != null) {
                    if (req1.getCluster() != req2.getCluster()) {
                        Cluster aux_cluster1 = req1.getCluster();
                        Cluster aux_cluster2 = req2.getCluster();
                        merge_clusters_update_reqs(aux_cluster1,aux_cluster2,requirements_to_update);
                        clusters.remove(aux_cluster2);
                    }
                }
            }
        }
    }

    private void update_added_requirements(List<Requirement> requirements_to_update, List<Requirement> requirements, List<Requirement> requirements_to_add, List<Cluster> clusters, HashMap<String,Dependency> dependencies_to_add, List<Dependency> result_dependencies, ComparisonBetweenSentences comparer, float threshold, String stakeholderId, long last_cluster_id, boolean type) throws BadRequestException, InternalErrorException{

        List<Requirement> requirements_new = new ArrayList<>();

        for (Requirement requirement: requirements) {
            if (requirement.getStatus() != null && (requirement.getStatus().equals("added"))) {
                requirement.compute_sentence();
                requirements_new.add(requirement);
            }
        }

        requirements_to_add.addAll(requirements_new);

        if (type) update_all_to_all(requirements_to_update,requirements_to_add,clusters,comparer,threshold,last_cluster_id,dependencies_to_add,result_dependencies,stakeholderId);
        else update_all_to_masters(requirements_to_update,requirements_to_add,clusters,comparer,threshold,last_cluster_id,dependencies_to_add,result_dependencies,stakeholderId);

    }

    private void update_all_to_all(List<Requirement> requirements_to_update, List<Requirement> requirements, List<Cluster> clusters, ComparisonBetweenSentences comparer, float threshold, long last_cluster_id, HashMap<String,Dependency> dependencies_to_add, List<Dependency> result_dependencies, String stakeholderid) throws InternalErrorException {
        for (Requirement requirement: requirements) {
            //TODO comprobar si este if es 100% necesario o es rebundante
            if (requirement.getStatus() != null && (requirement.getStatus().equals("added"))) {
                List<Cluster> clusters_with_superior_threshold = new ArrayList<>();
                for (Cluster cluster : clusters) {
                    boolean found = false;
                    for (int j = 0; (!found) && (j < cluster.getSpecifiedRequirements().size()); ++j) {
                        Requirement cluster_requirement = cluster.getSpecifiedRequirements().get(j);
                        Dependency aux_db = null;
                        String id1 = requirement.getId();
                        String id2 = cluster_requirement.getId();
                        try {
                            aux_db = requirementDAO.getDependency(id1, id2, stakeholderid);
                        } catch (SQLException e) {
                            if (!e.getMessage().contains("The dependency does not exist in the database"))
                                throw new InternalErrorException("Database error: Error while getting a dependency.");
                            aux_db = dependencies_to_add.get(id1 + id2);
                            if (aux_db == null) aux_db = dependencies_to_add.get(id2 + id1);
                        } catch (ClassNotFoundException e) {
                            throw new InternalErrorException("Database error: Class not found.");
                        }
                        //only continue if the dependency does not exist or it is accepted or added
                        if (aux_db != null && (aux_db.getStatus().equals("accepted") || aux_db.getStatus().equals("added"))) {
                            clusters_with_superior_threshold.add(cluster);
                            found = true;
                        } else if (aux_db == null) {
                            Dependency aux_dep = comparer.compare_two_requirements_dep(requirement,cluster_requirement);
                            if (aux_dep != null && aux_dep.getDependency_score() >= threshold) {
                                clusters_with_superior_threshold.add(cluster);
                                found = true;
                            }
                        }
                    }
                }
                requirements_to_update.add(requirement);
                if (clusters_with_superior_threshold.size() == 0) {
                    Cluster aux_cluster = new Cluster(last_cluster_id);
                    ++last_cluster_id;
                    aux_cluster.addReq(requirement);
                    clusters.add(aux_cluster);
                } else if (clusters_with_superior_threshold.size() == 1) {
                    Cluster aux_cluster_2 = clusters_with_superior_threshold.get(0);
                    Requirement req_older = aux_cluster_2.getReq_older();
                    aux_cluster_2.addReq(requirement);
                    if (aux_cluster_2.getReq_older().getId().equals(requirement.getId())) {
                        requirements_to_update.add(req_older);
                        result_dependencies.add(new Dependency(requirement.getId(), req_older.getId(), "proposed", "duplicates"));
                    }
                    else result_dependencies.add(new Dependency(req_older.getId(),requirement.getId(), "proposed", "duplicates"));

                } else {
                    List<String> reqs_older = new ArrayList<>();
                    Cluster aux_cluster = clusters_with_superior_threshold.get(0);
                    Requirement old_master = aux_cluster.getReq_older();
                    reqs_older.add(old_master.getId());
                    aux_cluster.addReq(requirement);
                    if (aux_cluster.getReq_older().getId().equals(requirement.getId())) {
                        requirements_to_update.add(old_master);
                        old_master = requirement;
                    }
                    for (int i = 1; i < clusters_with_superior_threshold.size(); ++i) {
                        Cluster aux_cluster2 = clusters_with_superior_threshold.get(i);
                        Requirement aux_master = aux_cluster2.getReq_older();
                        reqs_older.add(aux_master.getId());
                        merge_clusters_update_reqs(aux_cluster,aux_cluster2,requirements_to_update);
                        clusters.remove(aux_cluster2);
                        if (aux_cluster.getReq_older().getId().equals(aux_master.getId())) {
                            requirements_to_update.add(old_master);
                            old_master = aux_master;
                        }
                    }
                    if (aux_cluster.getReq_older().getId().equals(requirement.getId())) {
                        for (String req_older : reqs_older) {
                            result_dependencies.add(new Dependency(requirement.getId(),req_older,"proposed", "duplicates"));
                        }
                    } else {
                        result_dependencies.add(new Dependency(aux_cluster.getReq_older().getId(),requirement.getId(),"proposed", "duplicates"));
                        for (String req_older : reqs_older) {
                            if(!aux_cluster.getReq_older().getId().equals(req_older)) result_dependencies.add(new Dependency(aux_cluster.getReq_older().getId(), req_older, "proposed", "duplicates"));
                        }
                    }
                }
            }
        }
    }

    private void update_all_to_masters(List<Requirement> requirements_to_update, List<Requirement> requirements, List<Cluster> clusters, ComparisonBetweenSentences comparer, float threshold, long last_cluster_id, HashMap<String,Dependency> dependencies_to_add, List<Dependency> result_dependencies, String stakeholderid) throws InternalErrorException {

        for (Requirement requirement: requirements) {
            //TODO comprobar si este if es 100% necesario o es rebundante
            if (requirement.getStatus() != null && (requirement.getStatus().equals("added"))) {
                List<Cluster> clusters_with_superior_threshold = new ArrayList<>();
                for (Cluster cluster : clusters) {
                    Dependency aux_db = null;
                    String id1 = requirement.getId();
                    String id2 = cluster.getReq_older().getId();
                    try {
                        aux_db = requirementDAO.getDependency(id1,id2,stakeholderid);
                    } catch (SQLException e) {
                        if (!e.getMessage().contains("The dependency does not exist in the database")) throw new InternalErrorException("Database error: Error while getting a dependency.");
                        aux_db = dependencies_to_add.get(id1+id2);
                        if (aux_db == null) aux_db = dependencies_to_add.get(id2+id1);
                    } catch (ClassNotFoundException e) {
                        throw new InternalErrorException("Database error: Class not found.");
                    }
                    //only continue if the dependency does not exist or it is accepted or added
                    if (aux_db != null && (aux_db.getStatus().equals("accepted") || aux_db.getStatus().equals("added"))) {
                        clusters_with_superior_threshold.add(cluster);
                    } else if (aux_db == null) {
                        Dependency aux_dep = comparer.compare_two_requirements_dep(requirement, cluster.getReq_older());
                        if (aux_dep != null && aux_dep.getDependency_score() >= threshold) {
                            clusters_with_superior_threshold.add(cluster);
                        }
                    }
                }
                requirements_to_update.add(requirement);
                if (clusters_with_superior_threshold.size() == 0) {
                    Cluster aux_cluster = new Cluster(last_cluster_id);
                    ++last_cluster_id;
                    aux_cluster.addReq(requirement);
                    clusters.add(aux_cluster);
                } else if (clusters_with_superior_threshold.size() == 1) {
                    Cluster aux_cluster_2 = clusters_with_superior_threshold.get(0);
                    Requirement req_older = aux_cluster_2.getReq_older();
                    aux_cluster_2.addReq(requirement);
                    if (aux_cluster_2.getReq_older().getId().equals(requirement.getId())) {
                        requirements_to_update.add(req_older);
                        result_dependencies.add(new Dependency(requirement.getId(), req_older.getId(), "proposed", "duplicates"));
                    }
                    else result_dependencies.add(new Dependency(req_older.getId(),requirement.getId(), "proposed", "duplicates"));

                } else {
                    List<String> reqs_older = new ArrayList<>();
                    Cluster aux_cluster = clusters_with_superior_threshold.get(0);
                    Requirement old_master = aux_cluster.getReq_older();
                    reqs_older.add(old_master.getId());
                    aux_cluster.addReq(requirement);
                    if (aux_cluster.getReq_older().getId().equals(requirement.getId())) {
                        requirements_to_update.add(old_master);
                        old_master = requirement;
                    }
                    for (int i = 1; i < clusters_with_superior_threshold.size(); ++i) {
                        Cluster aux_cluster2 = clusters_with_superior_threshold.get(i);
                        Requirement aux_master = aux_cluster2.getReq_older();
                        reqs_older.add(aux_master.getId());
                        merge_clusters_update_reqs(aux_cluster,aux_cluster2,requirements_to_update);
                        clusters.remove(aux_cluster2);
                        if (aux_cluster.getReq_older().getId().equals(aux_master.getId())) {
                            requirements_to_update.add(old_master);
                            old_master = aux_master;
                        }
                    }
                    if (aux_cluster.getReq_older().getId().equals(requirement.getId())) {
                        for (String req_older : reqs_older) {
                            result_dependencies.add(new Dependency(requirement.getId(),req_older,"proposed", "duplicates"));
                        }
                    } else {
                        result_dependencies.add(new Dependency(aux_cluster.getReq_older().getId(),requirement.getId(),"proposed", "duplicates"));
                        for (String req_older : reqs_older) {
                            if(!aux_cluster.getReq_older().getId().equals(req_older)) result_dependencies.add(new Dependency(aux_cluster.getReq_older().getId(), req_older, "proposed", "duplicates"));
                        }
                    }
                }
            }
        }
    }

    private void merge_clusters(Cluster cluster1, Cluster cluster2) {

        for (Requirement requirement: cluster2.getSpecifiedRequirements()) {
            cluster1.addReq(requirement);
        }
    }

    private void merge_clusters_update_reqs(Cluster cluster1, Cluster cluster2, List<Requirement> requirements_to_update) {

        for (Requirement requirement: cluster2.getSpecifiedRequirements()) {
            requirements_to_update.add(requirement);
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
