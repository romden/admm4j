/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.admm4j.demo.common;

import org.admm4j.core.Node;

import java.util.Arrays;

import com.google.gson.JsonObject;

/**
 * Implements a node with a linear objective function:
 *     min: f(x) = c^T * x
 * subject: lb <= x <= ub
 * 
 * This node can have arbitrary number of neighbors
 * 
 * @author Roman Denysiuk
 */
public class LinearFunctionNode extends Node {
    
    public double[] c;    
    public double[] lb;
    public double[] ub;

    @Override
    public void build(JsonObject input) {
        int nvar = numVariables.values().iterator().next();
        
        if(input.has("c")){
            if(input.get("c").isJsonArray()){
                c = gson.fromJson(input.getAsJsonArray("c"), double[].class);
            }
            else{
                c = new double[nvar];
                Arrays.fill(c, input.get("c").getAsDouble());
            }
        }
        if(input.has("lb")){
            if(input.get("lb").isJsonArray()){
                lb = gson.fromJson(input.getAsJsonArray("lb"), double[].class);
            }
            else{
                lb = new double[nvar];
                Arrays.fill(lb, input.get("lb").getAsDouble());
            }
        }
        if(input.has("ub")){
            if(input.get("ub").isJsonArray()){
                ub = gson.fromJson(input.getAsJsonArray("ub"), double[].class);
            }
            else{
                ub = new double[nvar];
                Arrays.fill(lb, input.get("ub").getAsDouble());
            }
        }       
    }

    /**
     * Solves local subproblem of the form:
     * f(x) = c*x + \sum rho_i/2 ||x - y_i||
     * x = ( \sum rho_i*y_i - c) / \sum rho_i
     */
    @Override
    public void solve() {
        
        double rhos = 0;
        for(String neighbor: neighbors){
            rhos += scalingParameters.get(neighbor);
        }
        
        int nvar = numVariables.get(neighbors.iterator().next());
        double[] x = new double[nvar];
        
        for(int i = 0; i < nvar; i++){ 
            x[i] = 0;
            
            for(String neighbor: neighbors){
                if(type == 1){
                    x[i] += scalingParameters.get(neighbor) * (neighborVariables.get(neighbor)[i] - multipliers.get(neighbor)[i]);
                }
                else{
                    x[i] += scalingParameters.get(neighbor) * (neighborVariables.get(neighbor)[i] + multipliers.get(neighbor)[i]);
                }
            }            
            
            x[i] -= c[i];
            
            x[i] /= rhos;
            
            if(lb != null){
                x[i] = Math.max(lb[i], x[i]);
            }
            if(ub != null){
                x[i] = Math.min(x[i], ub[i]);
            }            
        }      

        for(String neighbor: neighbors){
            variables.put(neighbor, x);
        }
    }    
    
    @Override
    public double evalObjective() {       
        
        String neighbor = neighbors.get(0);  
        double[] x = variables.get(neighbor);
        int nvar = numVariables.get(neighbor);
        
        double fx = 0;        
        for(int i = 0; i < nvar; i++){ 
            fx += c[i] * x[i];
        }
        
        return fx;
    }
    
}