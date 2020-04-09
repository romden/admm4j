/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package org.admm4j.impl.base;

import org.admm4j.core.Node;

import java.util.Map;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * Implements main steps of ADMM algorithm
 * Each node in a bipartite graph is associated with a computing agent
 * Each agent is executed as a separate thread
 * Agents exchange messages and are coordinated by this class
 * 
 * @author Roman Denysiuk
 */
public class Admm {
    
    public double tolPrimal;
    public double tolDual;
    public int iterLimit;
    public long timeLimit;
    public int verbose;    
    
    boolean stopping; // indicates global stopping criterion   
    boolean convergence; // local convergences
    int iterCounter;
    long runTime;    
    
    public Map<String, Node> nodesI;
    public Map<String, Node> nodesII;    
    
    public List<Runnable> tasks;        
    public Map<String, BlockingQueue> queues;    
    
    
    public void execute(){
        
        // set params
        stopping = false;
        convergence = false;
        iterCounter = 0;        
        runTime = 0;
        long startTime = System.currentTimeMillis();               
        
        // execute agents I and II as cuncurrent threads
        tasks.forEach((t) -> new Thread(t).start());
        
        // execute ADMM steps
        while(!stopping){
            
            // receive local convergences            
            receiveResiduals();
            
            // update interation and time counters
            iterCounter++;
            runTime = System.currentTimeMillis() - startTime;
            
            // check stopping criteria
            stopping = convergence || (iterCounter == iterLimit) || (runTime >= timeLimit);
            
            // send stopping indicating whether to proceed
            sendStopping();
            
            // print current statistics
            if(verbose > 0 && iterCounter % verbose == 0){
                printStats();
            }            
        }
        
    }
    
    public void receiveResiduals(){
        
        convergence = true;
        int count = nodesII.size();
        BlockingQueue queue = queues.get("ADMM");
        
        while(count > 0){
            try {
                Message message = (Message) queue.take();
                boolean local = (message.residuals[0] < tolPrimal) && (message.residuals[1] < tolDual);
                convergence = (convergence && local);
            } catch (InterruptedException ex) {
                System.err.println(ex);
            }
            count--;
        }
    }
    
    public void sendStopping(){
        for(String name: nodesII.keySet()){
            
            Message message = new Message();
            message.sender = "ADMM";
            message.stopping = stopping;
            
            BlockingQueue queue = queues.get(name);
            
            try {
                queue.put(message);
            } catch (InterruptedException ex) {
                System.err.println(ex);
            }
        }
    }
    
    public void printStats(){
        System.out.printf("Iter: %d   Runtime: %d\n", iterCounter, runTime);
        for(Node node: nodesII.values()){
            System.out.printf("NodeII: %s primal: %.6f dual: %.6f\n", node.name, node.residuals[0], node.residuals[1]);
        }
    }
}


