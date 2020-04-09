/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.admm4j.demo.util;

import java.util.function.Function;

import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.algo.DenseDoubleAlgebra;

/**
 *
 * @author Roman Denysiuk
 */
public class GradientDescent {    
    
    public double tol = 1e-5; // tolerance
    
    DenseDoubleAlgebra algebra = new DenseDoubleAlgebra();
    
    
    // gradient descent with backtracking line search
    public void execute(DoubleMatrix1D X, 
                        Function<DoubleMatrix1D, DoubleMatrix1D> gradient, 
                        Function<DoubleMatrix1D, Double> objective){         
        
        for(;;){
            // save current value in the beginning of iteration
            DoubleMatrix1D Xold = X.copy();
            
            // calc gradient
            DoubleMatrix1D dX = gradient.apply(X);            
            
            // descent direction
            DoubleMatrix1D S = dX.copy().assign(dx -> -dx);
            
            // determine stepsize
            double eta = lineSearch(X, dX, S, objective);
            
            // gradient descend
            X.assign(S, (x, s) -> x + eta*s);           
            
            // check stopping criterion
            if(algebra.norm2(Xold.assign(X, (xold, x) -> xold - x)) < tol){
                break;
            }            
        }
    }
    
    // Backtracking Line Search
    public static double lineSearch(DoubleMatrix1D x, 
                                    DoubleMatrix1D gradient, 
                                    DoubleMatrix1D direction, 
                                    Function<DoubleMatrix1D, Double> objective){
        
        double alpha = 1.;
        double beta = 0.5;
        double tau = 0.618;
        
        double fx = objective.apply(x);
        double prod = gradient.zDotProduct(direction);
        DoubleMatrix1D xnew = x.copy();
        
        while(true){
            
            for(int j = 0; j < x.size(); j++){
                xnew.set(j, x.get(j) + alpha*direction.get(j));
            }
            
            if(objective.apply(xnew) > fx + alpha*beta*prod){
                alpha *= tau;
            }
            else{
                break;
            }
        }
        
        return alpha;
    }
    
    
    // fixed step size
    public void execute0(DoubleMatrix1D X, Function<DoubleMatrix1D, DoubleMatrix1D> gradient, Function<DoubleMatrix1D, DoubleMatrix1D> projection){
        
        double eta = 1e-3; // step size
        
        for(;;){
            // save current value in the beginning of iteration
            DoubleMatrix1D Xold = X.copy();
            
            // calc gradient
            DoubleMatrix1D dX = gradient.apply(X);
            
            // gradient descend
            X.assign(dX, (x,dx) -> x - eta*dx);
            
            // projection
            if(projection != null){
                X = projection.apply(X);
            }            
            
            // check stopping criterion
            if(algebra.norm2(Xold.assign(X, (a,b) -> a-b)) < tol){
                break;
            }            
        }
    }
}