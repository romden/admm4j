/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.admm4j.impl.base;

/**
 * Implements a message exchanged by computing agents
 * 
 * @author Roman Denysiuk
 */
public class Message {
    
    String sender = null;
    
    double[] variables = null;
    
    double[] multipliers = null;
    
    double[] residuals = null;
    
    boolean stopping = false;
}
