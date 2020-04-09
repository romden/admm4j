/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package org.admm4j.demo.ml.cluster;

import org.admm4j.core.Node;

import java.util.List;
import java.util.ArrayList;

import com.google.gson.JsonObject;

/**
 * Implements KMeans node. 
 * It should be noted that it is not an application of ADMM algorithm.
 * This example is used to show the flexibility of the framework.
 * 
 * @author Roman Denysiuk
 */
public class KMeansWorker extends Node{
    
    List<double[]> data;    
    int numClusters;
    int numFeatures;
    
    @Override
    public void build(JsonObject input) {
        
        data = new ArrayList();
        for(int i = 0; i < input.getAsJsonArray("data").size(); i++){
            data.add( gson.fromJson(input.getAsJsonArray("data").get(i).getAsJsonArray(), double[].class) );
        }
        
        numClusters = input.get("k").getAsInt();
        numFeatures = data.get(0).length;
    }
    
    @Override
    public void solve() { 
        
        // node has one neighbor
        String neighbor = neighbors.get(0);
        
        // neighborVariables is a vector that contains cluster centers being concatenated
        double[] currentCenters = neighborVariables.get(neighbor);
        
        // init new cluster centers
        double[] newCenters = new double[numClusters*numFeatures];
        
        // number of points closest to the current cluster centers
        int[] pointsCounter = new int[numClusters];
        
        // for each data point
        for(double[] point: data){            
            // find closest cluster center
            int closest = 0; // init
            double minDist = distance(point, currentCenters, 0);            
            for(int k = 1; k < numClusters; k++){
                double tmpDist = distance(point, currentCenters, k*numFeatures);
                if(tmpDist < minDist){
                    minDist = tmpDist;
                    closest = k;
                }
            }
            
            // update counter
            pointsCounter[closest]++;
            
            // update new cluster center
            int start = closest*numFeatures;
            for(int j = 0; j < numFeatures; j++){
                newCenters[start+j] += point[j];
            }
        }
        
        for(int k = 0; k < numClusters; k++){
            if(pointsCounter[k] > 0){
                int start = k*numFeatures; 
                for(int j = 0; j < numFeatures; j++){
                    newCenters[start+j] /= pointsCounter[k];
                }
            }
        }
        
        // update variables
        variables.put(neighbor, newCenters);
    }
    
    public double distance(double[] point, double[] c, int start) {
        double dist = 0;
        for(int j = 0; j < numFeatures; j++) {
            dist += Math.pow(point[j] - c[start+j], 2);
        }
        return Math.sqrt(dist);
    }
    
}
