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
 * Implements a computing agent associated with a node of type I
 * 
 * @author Roman Denysiuk
 */
public class AgentI implements Runnable {
    
    Node node;
    Map<String, BlockingQueue> queues;
    boolean stopping;
    
    public AgentI(Node node, Map<String, BlockingQueue> queues){
        this.node = node;
        this.queues = queues;
    }
    
    @Override
    public void run() {
        while(true){
            // recieve vars and mults
            receiveVariables();
            
            // check convergence from controller
            if(stopping){
                break;
            }
            
            // eval proximal operator
            node.solve();
            
            // send vars
            sendVariables();
        }
    }
    
    // receives vars, mults, and stopping from neighboring agentII
    public void receiveVariables(){
        
        int count = node.neighbors.size();
        BlockingQueue queue = queues.get(node.name);
        Message message = null;
        
        while(count > 0){
            try {
                message = (Message) queue.take();
                node.neighborVariables.put(message.sender, message.variables);
                node.multipliers.put(message.sender, message.multipliers);
            } catch (InterruptedException ex) {
                System.err.println(ex);
            }
            count--;
        }
        
        stopping = message.stopping;
    }
    
    // sends vars to neighboring agentII
    public void sendVariables(){
        for(String neighbor: node.neighbors){
            
            Message message = new Message();
            message.sender = node.name;
            message.variables = node.variables.get(neighbor);
            
            BlockingQueue queue = queues.get(neighbor);
            
            try {
                queue.put(message);
            } catch (InterruptedException ex) {
                System.err.println(ex);
            }
        }
    }
}
