/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package org.admm4j.demo.common;

import org.admm4j.core.Node;

/**
 * Implements projection by averaging.
 * 
 * @author Roman Denysiuk
 */
public class AveragingNode extends Node{
    
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
    
}
