/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package org.admm4j.impl.base;

import org.admm4j.core.Node;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

/**
 * This class allows the execution of ADMM algorithm
 * It allows to provide JSON based input with the specification of nodes 
 * in the graph, to obtain the results of optimization and
 * to set the algorithm's parameters
 * 
 * @author Roman Denysiuk
 */
public class AdmmContext{
    
    public Admm solver = new Admm();
    
    public JsonObject settings = new JsonObject();
    
    public AdmmContext(){
        
        // these are set when equal for all agents, otherwise provided in neighbors arrays
        settings.addProperty("numVariables", 0); // Mandatory to set   
        settings.addProperty("scalingParameter", 0); // Mandatory to set | 1 is typical value
        
        // ADMM settings        
        settings.addProperty("tolPrimal", 1e-4);
        settings.addProperty("tolDual", 1e-4);
        settings.addProperty("iterLimit", Integer.MAX_VALUE);
        settings.addProperty("timeLimit", Long.MAX_VALUE);
        settings.addProperty("verbose", 0);
        settings.add("outputFile", null);
    }
    
    public void execute(JsonObject input){
        
        // set parameters
        solver.tolPrimal = settings.get("tolPrimal").getAsDouble();
        solver.tolDual = settings.get("tolDual").getAsDouble();
        solver.iterLimit = settings.get("iterLimit").getAsInt();
        solver.timeLimit = settings.get("timeLimit").getAsLong();
        solver.verbose = settings.get("verbose").getAsInt();
        
        // init
        solver.nodesI = new HashMap();
        solver.nodesII = new HashMap();
        solver.tasks = new LinkedList();
        solver.queues = new HashMap();
        
        // ACTORS
        for (JsonElement element: input.getAsJsonArray("nodesI")){
            
            // json model of agent
            JsonObject model = element.getAsJsonObject(); 
            if(!model.has("neighbors") || model.get("neighbors").isJsonNull()){
                addNeighbors(model, input.getAsJsonArray("nodesII"));
            }
            if(settings.get("numVariables").getAsInt() > 0 || settings.get("scalingParameter").getAsDouble() > 0){
                addNeighborsParameters(model);
            }
            
            // instantiate node
            Node node = Node.instantiate(model.get("class").getAsString(), model.get("name").getAsString(), 1);
            
            // init
            node.initialize(model);
            
            solver.nodesI.put(node.name, node);
            
            // init queue
            int capacity = node.neighbors.size();
            BlockingQueue queue = new LinkedBlockingQueue(capacity);
            solver.queues.put(node.name, queue);
            
            solver.tasks.add(new AgentI(node, solver.queues));
        }
        
        // NETS
        for (JsonElement element: input.getAsJsonArray("nodesII")){
            
            // json model of agent
            JsonObject model = element.getAsJsonObject();
            if(!model.has("neighbors") || model.get("neighbors").isJsonNull()){
                addNeighbors(model, input.getAsJsonArray("nodesI"));
            }
            if(settings.get("numVariables").getAsInt() > 0 || settings.get("scalingParameter").getAsDouble() > 0){
                addNeighborsParameters(model);
            }
            
            // instantiate node
            Node node = Node.instantiate(model.get("class").getAsString(), model.get("name").getAsString(), 2);
            
            // init
            node.initialize(model);
            
            solver.nodesII.put(node.name, node);
            
            // init queue
            int capacity = node.neighbors.size();
            BlockingQueue queue = new LinkedBlockingQueue(capacity);
            solver.queues.put(node.name, queue);
            
            solver.tasks.add(new AgentII(node, solver.queues));
        }
        
        // ADMM controller
        int capacity = solver.nodesII.size();
        BlockingQueue queue = new LinkedBlockingQueue(capacity);
        solver.queues.put("ADMM", queue);
        
        // RUN ADMM solver
        solver.execute();
        
        // save output to file
        if(settings.has("outputFile") && !settings.get("outputFile").isJsonNull()){
            saveJsonToFile(getOutput(), settings.get("outputFile").getAsString());
        }
    }
    
    public void addNeighbors(JsonObject model, JsonArray nodes){
        
        model.add("neighbors", new JsonArray());
        for(int i = 0; i < nodes.size(); i++){
            JsonObject node = nodes.get(i).getAsJsonObject();
            for(int j = 0; j < node.getAsJsonArray("neighbors").size(); j++){
                String[] tokens = node.getAsJsonArray("neighbors").get(j).getAsString().split(":");
                if(model.get("name").getAsString().equals(tokens[0])){
                    StringBuilder sb = new StringBuilder(node.get("name").getAsString());
                    for(int k = 1; k < tokens.length; k++){
                        sb.append(":").append(tokens[k]);
                    }
                    model.getAsJsonArray("neighbors").add(sb.toString());
                    break;
                }
            }
        }
    }
    
    public void addNeighborsParameters(JsonObject model){
        
        int nvars = settings.get("numVariables").getAsInt();
        double rho = settings.get("scalingParameter").getAsDouble();
        
        JsonArray neighbors = model.getAsJsonArray("neighbors");
        model.add("neighbors", new JsonArray());
        
        for(int j = 0; j < neighbors.size(); j++){
            String[] tokens = neighbors.get(j).getAsString().split(":");
            StringBuilder sb = new StringBuilder(tokens[0]);
            if(nvars > 0){
                sb.append(":").append(nvars);
            }
            else{
                sb.append(":").append(tokens[1]);
            }
            if(rho > 0){
                sb.append(":").append(rho);
            }
            model.getAsJsonArray("neighbors").add(sb.toString());
        }       
    }
    
    public JsonObject getOutput(){
        
        // MAKE JSON OUTPUT
        JsonObject stats = new JsonObject();
        stats.addProperty("runTime", solver.runTime);
        stats.addProperty("numIterations", solver.iterCounter);
        
        JsonObject output = new JsonObject();
        output.add("nodesI", makeOutput(solver.nodesI));
        output.add("nodesII", makeOutput(solver.nodesII));
        output.add("stats", stats);
        
        return output;
    }
    
    public JsonArray makeOutput(Map<String, Node> agents){
        JsonArray output = new JsonArray();
        for(String name: agents.keySet()){
            output.add(agents.get(name).getJsonOutput());
        }
        return output;
    }
    
    public JsonObject getJsonFromFile(String fileName){
        try {
            FileReader fileReader = new FileReader(fileName.replace('\\','/'));
            JsonReader jsonReader = new JsonReader(fileReader);
            JsonParser parser = new JsonParser();
            JsonElement element = parser.parse(jsonReader);
            return element.getAsJsonObject();
        } catch (FileNotFoundException ex) {
            System.err.println(ex);
        }
        
        return null;
    }
    
    public void saveJsonToFile(JsonObject jsonObject, String fileName){
        try {
            FileWriter file = new FileWriter(fileName.replace('\\','/'));
            file.write(jsonObject.toString());
            file.flush();
            file.close();
        }
        catch (IOException ex){
            System.err.println(ex);
        }
    }
}
