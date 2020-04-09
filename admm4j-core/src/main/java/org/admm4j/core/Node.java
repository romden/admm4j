/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package org.admm4j.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

/**
 * Abstract class representing a node in a bipartite graph 
 * A minimal implementation requires overriding the solve method which is 
 * responsible for solving a local subproblem
 * 
 * @author Roman Denysiuk
 */
public abstract class Node implements Serializable{
    
    public int type; // {1,2} - identifies the type of node (nodeI or nodeII)
    public String name;
    public ArrayList<String> neighbors; // neighbors associated with this node
    public HashMap<String, Integer> numVariables; // number of variables in each neighbor
    public HashMap<String, double[]> variables; // variables being optimized in each neighbor
    public HashMap<String, double[]> previousVariables; // variables from the previous iteration in each neighbor
    public HashMap<String, double[]> neighborVariables; // variables of neighbor in each neighbor
    public HashMap<String, double[]> multipliers; // scaled dual variables (Lagrange multipliers) in each neighbor
    public HashMap<String, Double> scalingParameters; // scaling parameter in each neighbor
    public double[] residuals; // primal and dual residuals
    
    public Gson gson = new Gson();
    public JsonParser jsonParser = new JsonParser();    
   
    /**
     * should be implemented
     * 
     * Proximal points:
     *  NodeI: (z - u)
     * NodeII: (x + u)
     * where:
     * x - NodeI variables
     * z - NodeII variables
     * u - multipliers
     */
    public abstract void solve();
    
    /**
     * The following methods:
     * build, getJsonInternal, evalObjective, evalConstraintViolation
     * are not mandatory to implement but they can be overridden
     * to provide necessary functionality
     */
    public void build(JsonObject input){};
    
    /**
     * could be but not mandatory to implement
     * it provides user defined output for the node 
     */
    public JsonObject getJsonInternal(){
        return null;
    }
    
    /**
     * could be but not mandatory to implement
     */
    public double evalObjective() {
        return 0;
    }
    
    /**
     * could be but not mandatory to implement
     */
    public double evalConstraintViolation() {
        return 0;
    }
    
    public void initialize(JsonObject model){
        
        if(model.has("neighbors") && model.get("neighbors").isJsonArray()){
            initVariables(model.getAsJsonArray("neighbors"));
        }
        
        if(model.has("variables") && model.get("variables").isJsonObject()){
            setVariables(model.getAsJsonObject("variables"));
        }
        
        if(model.has("input") && model.get("input").isJsonObject()){
            build(model.getAsJsonObject("input"));
        }
    }
    
    public void initVariables(JsonArray array){
        
        neighbors = new ArrayList();
        numVariables = new HashMap();
        variables = new HashMap();
        previousVariables = new HashMap();
        neighborVariables = new HashMap();
        multipliers = new HashMap();
        scalingParameters = new HashMap();
        
        for(JsonElement element: array){
            String[] tokens = element.getAsString().split(":");
            String neighbor = tokens[0];
            int nvars = tokens.length > 1 ? Integer.parseInt(tokens[1]) : 1;
            double rho = tokens.length > 2 ? Double.parseDouble(tokens[2]) : 1d;           
            
            neighbors.add(neighbor);
            numVariables.put(neighbor, nvars);
            variables.put(neighbor, new double[nvars]);
            previousVariables.put(neighbor, new double[nvars]);
            neighborVariables.put(neighbor, new double[nvars]);
            multipliers.put(neighbor, new double[nvars]);
            scalingParameters.put(neighbor, rho);
        }
        residuals = new double[]{0, 0};
    }  
    
    public void setVariables(JsonObject warmstart){
        
        if(warmstart.has("variables")){
            JsonObject vars = warmstart.getAsJsonObject("variables");
            for(String neighbor: neighbors) {
                if(vars.has(neighbor)){
                    variables.put(neighbor, gson.fromJson(vars.getAsJsonArray(neighbor), double[].class));
                }
            }
        }
        
        if(warmstart.has("multipliers")){
            JsonObject mults = warmstart.getAsJsonObject("multipliers");
            for(String neighbor: neighbors) {
                if(mults.has(neighbor)){
                    multipliers.put(neighbor, gson.fromJson(mults.getAsJsonArray(neighbor), double[].class));
                }
            }
        }
    }    
    
    // this method is called before evaluating prox operator
    public void updatePreviousVariables() {
        for(String neighbor: neighbors) {
            int n = numVariables.get(neighbor);
            System.arraycopy(variables.get(neighbor), 0, previousVariables.get(neighbor), 0, n);
        }
    }
    
    // this method is called only by net agent
    public void updateMultipliers() {
        for(String neighbor: neighbors) {
            int n = numVariables.get(neighbor);
            double[] x = neighborVariables.get(neighbor);
            double[] z = variables.get(neighbor);
            double[] u = multipliers.get(neighbor);
            for(int i = 0; i < n; i++){
                u[i] += x[i] - z[i];
            }
        }
    }
    
    public void updateResiduals(){
        double primalResidual = 0;
        double dualResidual = 0;
        int nvars = 0;
        
        for(String neighbor: neighbors){
            double[] x = neighborVariables.get(neighbor);
            double[] z = variables.get(neighbor);
            double[] zold = previousVariables.get(neighbor);
            double rho = scalingParameters.get(neighbor);
            int n = numVariables.get(neighbor);
            nvars += n;
            for(int i = 0; i < n; i++){
                primalResidual += Math.pow(x[i] - z[i], 2);
                dualResidual += Math.pow(rho*(z[i] - zold[i]), 2);
            }
        }
        
        residuals[0] = Math.sqrt(primalResidual/nvars);
        residuals[1] = Math.sqrt(dualResidual/nvars);
    }
    
    /**
     * returns the proximal point:
     * (z - u) if NodeI (type=1)
     * (x + u) if NodeII (type=2) 
     */
    public double[] getProximalPoint(String neighbor) {
        
        int n = numVariables.get(neighbor);
        double[] y = new double[n];
        
        switch(type){
            case 1:
                for(int i = 0; i < n; i++){
                    y[i] = neighborVariables.get(neighbor)[i] - multipliers.get(neighbor)[i];
                }
                break;
            case 2:
                for(int i = 0; i < n; i++){
                    y[i] = neighborVariables.get(neighbor)[i] + multipliers.get(neighbor)[i];
                }
                break;
        }
        
        return y;
    }
    
    public JsonObject getJsonOutput(){       
        
        // compose JSON output
        JsonObject jsonVariables = new JsonObject();
        JsonObject jsonMultipliers = new JsonObject();
        for(String neighbor: neighbors){
            jsonVariables.add(neighbor, gson.toJsonTree(variables.get(neighbor)));
            jsonMultipliers.add(neighbor, gson.toJsonTree(multipliers.get(neighbor)));
        }
        
        // make output object
        JsonObject jsonOutput = new JsonObject();
        jsonOutput.addProperty("name", name);
        jsonOutput.addProperty("objective", evalObjective());
        jsonOutput.addProperty("cv", evalConstraintViolation());
        jsonOutput.add("variables", jsonVariables);
        jsonOutput.add("multipliers", jsonMultipliers);
        jsonOutput.add("internal", getJsonInternal());
        
        return jsonOutput;
    }
    
    
    public static Node instantiate(String nodeClass, String nodeName, int nodeType){
        
        // INSTANTIATE OBJECT
        Node node = null;
        try {
            Class<?> clazz = Class.forName(nodeClass); // className - full path of the class
            Constructor<?> constructor = clazz.getConstructor(); // here types of input to constractor can be put (String.class, Integer.class, etc)
            node = (Node) constructor.newInstance(); // here input to constractor can be put (...)
        } catch (ClassNotFoundException | IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchMethodException | SecurityException | InvocationTargetException ex) {
            System.err.println(ex);
        }
        
        node.name = nodeName;
        node.type = nodeType;
        
        return node;
    }
}
