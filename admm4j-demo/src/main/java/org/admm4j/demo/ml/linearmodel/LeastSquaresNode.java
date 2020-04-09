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
 *
 * analytical solution to traditional least squares problem:
 * x = (A_T*A)^−1 (A_T*b)
 *
 * Here used by consensus
 * 
 * @author Roman Denysiuk
 */
public class LeastSquaresNode extends Node{
    
    DoubleMatrix2D AA;
    DoubleMatrix1D Ab;
    DenseDoubleAlgebra algebra = new DenseDoubleAlgebra();
    
    DoubleMatrix2D A;
    DoubleMatrix1D b;
    
    GradientDescent solver = new GradientDescent();
    
    String METHOD = "iterative";
    
    @Override
    public void build(JsonObject input) {
        
        if(input.has("method")){
            METHOD = input.get("method").getAsString();
        }
        
        JsonArray jsonA = input.getAsJsonArray("A");
        JsonArray jsonb = input.getAsJsonArray("b");
        
        int n = jsonA.size();
        
        double[][] _A_ = new double[n][];
        double[] _b_ = new double[n];
        
        for(int i = 0; i < n; i++){
            _A_[i] = gson.fromJson(jsonA.get(i).getAsJsonArray(), double[].class);
            _b_[i] = jsonb.get(i).getAsDouble();
        }
        
        A = DoubleFactory2D.dense.make(_A_);
        b = DoubleFactory1D.dense.make(_b_);
        
        DoubleMatrix2D A_T = algebra.transpose(A.copy());
        AA = algebra.mult(A_T, A);
        Ab = algebra.mult(A_T, b);
    }
    
    @Override
    public double evalObjective() {
        String neighbor = neighbors.get(0);
        DoubleMatrix1D x = DoubleFactory1D.dense.make(variables.get(neighbor));
        return objective(x, x);
    }
    
    
    @Override
    public void solve() {
        
        if(METHOD.equals("analytical")){
            analyticalSolution();  
        }
        else{
            iterativeSolution();
        }
    }    
    
    /*
    * analytical solution
    * f = 1/2*||Ax-b||^2 + rho/2*||x-y||^2
    * df = A^T*A*x - A^T*b + rho*x - rho*y
    * (A^T*A + rho*I)*x = A^T*b + rho*y
    */
    void analyticalSolution() {
        String neighbor = neighbors.get(0);
        double rho = scalingParameters.get(neighbor);
        int n = numVariables.get(neighbor);
        
        DoubleMatrix2D Left = AA.copy();
        DoubleMatrix1D Right = DoubleFactory1D.dense.make(n);
        
        for(int i = 0; i < n; i++){
            double y = neighborVariables.get(neighbor)[i] - multipliers.get(neighbor)[i];
            Left.setQuick(i, i, AA.getQuick(i,i) + rho); // AA + I*rho
            Right.setQuick(i, Ab.getQuick(i) + rho*y);
        }
        
        DoubleMatrix1D X = algebra.solve(Left, Right);
        
        variables.put(neighbor, X.toArray());
    }
    
    /*
    * iterative solution using gradient descent
    */ 
    void iterativeSolution() {
        
        String neighbor = neighbors.get(0);
        
        double[] x = variables.get(neighbor);
        double[] y = getProximalPoint(neighbor);
        
        DoubleMatrix1D Y = DoubleFactory1D.dense.make(y);
        DoubleMatrix1D sol = DoubleFactory1D.dense.make(x);
        
        solver.execute(sol, X -> evalGradient(X, Y), X -> objective(X, Y));
        
        variables.put(neighbor, sol.toArray());
    }
    
    /*
    *  1/2 * ||A*x - b|| + rho/2 * ||x - y||^2
    */
    double objective(DoubleMatrix1D X, DoubleMatrix1D Y){
        
        String neighbor = neighbors.get(0);
        double rho = scalingParameters.get(neighbor);
        
        DoubleMatrix1D L = algebra.mult(A, X).assign(b, (a,b) -> Math.pow(a-b, 2));
        
        DoubleMatrix1D P = X.copy().assign(Y, (x,y) -> Math.pow(x-y, 2));
        
        return 0.5 * L.zSum() + rho/2 * P.zSum();
    }
    
    DoubleMatrix1D evalGradient(DoubleMatrix1D X, DoubleMatrix1D Y){
        
        String neighbor = neighbors.get(0);
        double rho = scalingParameters.get(neighbor);
        
        DoubleMatrix1D dL = algebra.mult(AA, X).assign(Ab, (aa,ab) -> aa-ab); // derivative of loss
        
        DoubleMatrix1D dP = X.copy().assign(Y, (w,y) -> rho*(w-y)); // derivative of proximal
        
        DoubleMatrix1D dX = dL.assign(dP, (dl,dp) -> dl+dp); // whole derivative
        
        return dX;
    }
}