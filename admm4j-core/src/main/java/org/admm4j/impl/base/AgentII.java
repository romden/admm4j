/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package org.admm4j.impl.base;

import org.admm4j.core.Node;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

/**
 * Implements a computing agent associated with a node of type II
 * 
 * @author Roman Denysiuk
 */
public class AgentII implements Runnable {
    
    Node node;
    Map<String, BlockingQueue> queues;
    boolean stopping;
    
    public AgentII(Node node, Map<String, BlockingQueue> queues){
        this.node = node;
        this.queues = queues;
    }
    
    @Override
    public void run() {
        stopping = false;
        
        while(true){
            // send variables, multipliers, and stopping criterion
            sendVariables();
            
            // check convergence from controller
            if(stopping){
                break;
            }
            
            // recieve vars and mults
            receiveVariables();
            
            // set previous varibles
            node.updatePreviousVariables();
            
            // eval proximal operator
            node.solve();
            
            // update multipliers
            node.updateMultipliers();
            
            // eval residuals
            node.updateResiduals();
            
            // send local convergence
            sendResiduals();
            
            // check convergence from controller
            receiveStopping();
        }
    }
    
    // communicates vars, mults, and stopping to neighboring agentI
    public void sendVariables(){
        for(String neighbor: node.neighbors){
            
            Message message = new Message();
            message.sender = node.name;
            message.variables = node.variables.get(neighbor);
            message.multipliers = node.multipliers.get(neighbor);
            message.stopping = stopping;
            
            BlockingQueue queue = queues.get(neighbor);
            
            try {
                queue.put(message);
            } catch (InterruptedException ex) {
                System.err.println(ex);
            }
        }
    }
    
    // receives vars from neighboring agentI
    public void receiveVariables(){
        
        int count = node.neighbors.size();
        BlockingQueue queue = queues.get(node.name);
        
        while(count > 0){
            try {
                Message message = (Message) queue.take();
                node.neighborVariables.put(message.sender, message.variables);
            } catch (InterruptedException ex) {
                System.err.println(ex);
            }
            count--;
        }
    }
    
    // sends residuals to controller
    public void sendResiduals(){
        
        Message message = new Message();
        message.sender = node.name;
        message.residuals = node.residuals;
        
        BlockingQueue queue = queues.get("ADMM");
        
        try {
            queue.put(message);
        } catch (InterruptedException ex) {
            System.err.println(ex);
        }
    }
    
    // receives stopping from controller
    public void receiveStopping(){
        
        try {
            BlockingQueue queue = queues.get(node.name);
            Message message = (Message) queue.take();
            stopping = message.stopping;
        } catch (InterruptedException ex) {
            System.err.println(ex);
        }
    }
}