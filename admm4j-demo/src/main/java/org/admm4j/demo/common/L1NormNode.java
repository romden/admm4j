/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package org.admm4j.demo.common;


import org.admm4j.core.Node;

import com.google.gson.JsonObject;

/**
 * Implements a node with a local subproblem of the form
 * min: lambda * ||x||_1
 * This can be used for L1 regularization
 *
 * @author Roman Denysiuk
 */
public class L1NormNode extends Node{
    
    double[] lambda;
    
    @Override
    public void build(JsonObject input) {
        
        lambda = gson.fromJson(input.getAsJsonArray("lambda"), double[].class);
    }
    
    /**
     * solves: lambda * ||x||_1 + rho/2 * ||x - y||_2
     */
    @Override
    public void solve() {        
        
        String neighbor = neighbors.get(0);
        double rho = scalingParameters.get(neighbor);
        int n = numVariables.get(neighbor);
        
        // single neighbor, Proximal Point
        double[] y = getProximalPoint(neighbor);
        
        // init vars
        double[] x = new double[n];
        
        for(int i = 0; i < n; i++){
            
            if(lambda[i] == 0){
                x[i] = y[i];
                continue;
            }
            
            double x_positive = Math.max(0, y[i] - lambda[i] / rho); // x >= 0
            double x_negative = Math.min(0, y[i] + lambda[i] / rho); // x <= 0
            
            double fx_positive = lambda[i] * Math.abs(x_positive) + (rho/2)*Math.pow(x_positive - y[i], 2);
            double fx_negative = lambda[i] * Math.abs(x_negative) + (rho/2)*Math.pow(x_negative - y[i], 2);
            
            if(fx_positive < fx_negative){
                x[i] = x_positive;
            }else{
                x[i] = x_negative;
            }
        }
        
        variables.put(neighbor, x);
    }
    
    @Override
    public double evalObjective() {
        
        String neighbor = neighbors.get(0);
        int n = numVariables.get(neighbor);
        
        double fx = 0;
        for(int i = 0; i < n; i++){
            fx += lambda[i] * Math.abs(variables.get(neighbor)[i]);
        }
        
        return fx;
    }
    
}
