/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package org.admm4j.demo.ml.linearmodel;

import org.admm4j.core.Node;

import org.admm4j.demo.util.GradientDescent;

import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tdouble.algo.DenseDoubleAlgebra;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * @author Roman Denysiuk
 */
public class LogisticRegressionNode extends Node{
    
    DoubleMatrix2D F; // features
    DoubleMatrix1D T; // target lables
    DoubleMatrix2D F_T; // transpose of feature matrix
    
    DenseDoubleAlgebra algebra = new DenseDoubleAlgebra();
    
    GradientDescent solver = new GradientDescent();
    
    
    @Override
    public void build(JsonObject input) {
        
        JsonArray features = input.getAsJsonArray("data");
        JsonArray targets = input.get("target").getAsJsonArray();
        
        int n = features.size();
        
        double[][] data = new double[n][];
        double[] target = gson.fromJson(targets, double[].class); 
        
        for(int i = 0; i < n; i++){
            data[i] = gson.fromJson(features.get(i).getAsJsonArray(), double[].class);
            //target[i] = (target[i]==0)?-1:target[i]; // for labels {-1, +1}
        }
        
        F = DoubleFactory2D.dense.make(data);
        T = DoubleFactory1D.dense.make(target);
        
        F_T = algebra.transpose(F.copy());
    }
    
    @Override
    public void solve() {
        
        String neighbor = neighbors.get(0);
        
        double[] y = getProximalPoint(neighbor);
        DoubleMatrix1D Y = DoubleFactory1D.dense.make(y);
        
        // initial point
        DoubleMatrix1D sol = DoubleFactory1D.dense.make(variables.get(neighbor));
        
        solver.execute(sol, X -> evalGradient(X, Y), X -> objective(X, Y));
        
        variables.put(neighbor, sol.toArray());
    }
    
    @Override
    public double evalObjective() {
        String neighbor = neighbors.get(0);
        double[] x = variables.get(neighbor);
        return objective(DoubleFactory1D.dense.make(x), DoubleFactory1D.dense.make(x));
    }
    
    /*
    * sum [-t*log(o)-(1-t)*log(1-o)] + rho/2 * ||x - y||^2
    */
    double objective(DoubleMatrix1D X, DoubleMatrix1D Y){
        
        String neighbor = neighbors.get(0);
        double rho = scalingParameters.get(neighbor);
        
        DoubleMatrix1D O = algebra.mult(F, X).assign(z -> 1./(1+Math.exp(-z)));
        
        DoubleMatrix1D term1 = T.copy().assign(O, (t,o) -> t*Math.log(Math.max(o, 1e-8)));
        DoubleMatrix1D term2 = T.copy().assign(O, (t,o) -> (1-t)*Math.log(Math.max(1-o, 1e-8)));
        DoubleMatrix1D L = term1.assign(term2, (t1, t2) -> -t1 - t2);
        
        DoubleMatrix1D P = X.copy().assign(Y, (x, y) -> Math.pow(x - y, 2));
        
        return L.zSum() + rho/2 * P.zSum();
    }
    
    
    /*
    * derivative for targets {0, 1}: o - t
    * 
    * derivative of prox function
    * F^T*(O - T) + rho*(W - Y)
    */
    DoubleMatrix1D evalGradient(DoubleMatrix1D W, DoubleMatrix1D Y){
        
        String neighbor = neighbors.get(0);
        double rho = scalingParameters.get(neighbor);
        
        DoubleMatrix1D O = algebra.mult(F, W).assign((z) -> 1./(1+Math.exp(-z))); // logit
        
        DoubleMatrix1D dL = algebra.mult(F_T, O.assign(T, (o,t) -> o-t)); // derivative of logistic loss
        
        DoubleMatrix1D dP = W.copy().assign(Y, (w,y) -> rho*(w-y)); // derivative of proximal
        
        DoubleMatrix1D dW = dL.assign(dP, (dl,dp) -> dl+dp); // whole derivative
        
        return dW;
    }
    
    /*
    * logistic, input {-1, +1} the same as above
    * L = sum log(1 + exp(-c * D * x))
    * derivative of prox function
    * dL = (-c*D)*exp(-c*D*x)/(1+exp(-c*D*x))
    * dP = rho*(W - Y)
    * dW = dL + dP
    */
//    DoubleMatrix1D evalGradient(DoubleMatrix1D W, DoubleMatrix1D Y){
//
//        String neighbor = terminals.get(0);
//        double rho = scalingParameters.get(neighbor);
//
//        DoubleMatrix1D Z = algebra.mult(F, W).assign(T, (z,t) -> -t*Math.exp(-t*z)/(1+Math.exp(-t*z)));
//
//        DoubleMatrix1D dL = algebra.mult(F_T, Z);
//
//        DoubleMatrix1D dP = W.copy().assign(Y, (w,y) -> rho*(w-y)); // derivative of proximal
//
//        DoubleMatrix1D dW = dL.assign(dP, (dl,dp) -> dl+dp); // whole derivative
//
//        return dW;
//    }
    
}