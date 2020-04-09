/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package org.admm4j.demo.ml.cluster;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.admm4j.core.Node;

import java.util.HashMap;

/**
 * Implements master node for k-means
 * 
 * @author Roman Denysiuk
 */
public class KMeansMaster extends Node{  
    
    int numClusters;
    
    @Override
    public void build(JsonObject input) {
        numClusters = input.get("k").getAsInt();
    }
    
    /**
     * averaging of variables received from neighbors
     */
    @Override
    public void solve() {
        
        int numVars = numVariables.get(neighbors.get(0));
        
        for(int i = 0; i < numVars; i++){
            // compute average
            double average = 0;            
            for(String neighbor: neighbors){
                average += neighborVariables.get(neighbor)[i];
            }            
            average /= neighbors.size();
            
            // assign
            for(String neighbor: neighbors){
                variables.get(neighbor)[i] = average;
            }
        }
    }
    
    public HashMap<String, double[]> previousNeighborVariables = new HashMap();
    
    /**
     * We modify primal residual calculation to allow convergence
     */
    @Override
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
        
        // here is modification
        if(!previousNeighborVariables.isEmpty()){
            primalResidual = 0;
            for(String neighbor: neighbors){                
                double[] x = neighborVariables.get(neighbor);
                double[] xold = previousNeighborVariables.get(neighbor);
                int n = numVariables.get(neighbor);
                for(int i = 0; i < n; i++){
                    primalResidual += Math.pow(x[i] - xold[i], 2);
                }           
            }
            
            // update previous variables
            for(String neighbor: neighbors){
                int n = numVariables.get(neighbor);
                System.arraycopy(neighborVariables.get(neighbor), 0, previousNeighborVariables.get(neighbor), 0, n);
            }
        }
        else{
            for(String neighbor: neighbors){
                previousNeighborVariables.put(neighbor, neighborVariables.get(neighbor).clone());
            }
        }
        
        residuals[0] = Math.sqrt(primalResidual/nvars);
        residuals[1] = Math.sqrt(dualResidual/nvars);
    }
    
    // for convenience we decode cluster centers and provide them in output
    public JsonObject getJsonInternal(){
        
        String neighbor = neighbors.get(0);
        
        int numFeatures = numVariables.get(neighbor)/numClusters;
                
        JsonObject internal = new JsonObject();
        
        for(int k = 0; k < numClusters; k++){
            JsonArray center = new JsonArray();
            int start = k*numFeatures;
            for(int j = 0; j < numFeatures; j++){
                center.add(variables.get(neighbor)[start+j]);
            }
            internal.add("c"+k, center);
        }
        
        return internal;
    }
}
