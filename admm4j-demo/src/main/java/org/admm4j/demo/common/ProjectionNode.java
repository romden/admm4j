/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package org.admm4j.demo.common;

import org.admm4j.core.Node;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

/**
 * Implements a node that projects a point onto a set 
 * The following sets are supported
 * 
 * Affine: A*x = b
 * 
 * Polyhedral: A*x <= b
 * 
 * Rectangle: lb <= x <= ub
 * 
 * Ball: sum x^2 = 1
 * 
 * LpBox
 *
 * @author Roman Denysiuk
 */
public class ProjectionNode extends Node{
    
    List<Constraint> constraints;
    
    @Override
    public void build(JsonObject input) {
        
        constraints = new ArrayList();
        
        if(input.has("Affine")){
            JsonArray A = input.getAsJsonObject("Affine").getAsJsonArray("A");
            JsonArray b = input.getAsJsonObject("Affine").getAsJsonArray("b");
            for(int i = 0; i < b.size(); i++){
                double[] _a = gson.fromJson(A.get(i).getAsJsonArray(), double[].class);
                double _b = b.get(i).getAsDouble();
                constraints.add( new Hyperplane(_a, _b) );
            }
        }
        
        if(input.has("Polyhedral")){
            JsonArray A = input.getAsJsonObject("Polyhedral").getAsJsonArray("A");
            JsonArray b = input.getAsJsonObject("Polyhedral").getAsJsonArray("b");
            for(int i = 0; i < b.size(); i++){
                double[] _a = gson.fromJson(A.get(i).getAsJsonArray(), double[].class);
                double _b = b.get(i).getAsDouble();
                constraints.add( new Halfspace(_a, _b) );
            }
        }
        
        if(input.has("Rectangle")){
            double[] lb = gson.fromJson(input.getAsJsonObject("Rectangle").getAsJsonArray("lb"), double[].class);
            double[] ub = gson.fromJson(input.getAsJsonObject("Rectangle").getAsJsonArray("ub"), double[].class);
            constraints.add( new Rectangle(lb, ub) );
        }
        
        if(input.has("Box")){
            double lb = input.getAsJsonObject("Box").get("lb").isJsonNull() ? Double.NEGATIVE_INFINITY : input.get("lb").getAsDouble();
            double ub = input.getAsJsonObject("Box").get("ub").isJsonNull() ? Double.POSITIVE_INFINITY : input.get("ub").getAsDouble();
            constraints.add( new Box(lb, ub) );
        }
        
        if(input.has("LpBox")){
            int p = input.getAsJsonObject("LpBox").get("p").getAsInt(); // defines Lp norm
            double shift = input.getAsJsonObject("LpBox").get("shift").getAsDouble(); // center
            constraints.add( new LpBox(p, shift) );
        }
        
        if(input.has("Ball")){
            constraints.add( new Ball() );
        }
        
        if(input.has("SparseAffine")){
            JsonArray idx = input.getAsJsonObject("SparseAffine").getAsJsonArray("idx");
            JsonArray A = input.getAsJsonObject("SparseAffine").getAsJsonArray("A");
            JsonArray b = input.getAsJsonObject("SparseAffine").getAsJsonArray("b");
            for(int i = 0; i < b.size(); i++){
                int[] _idx = gson.fromJson(idx.get(i).getAsJsonArray(), int[].class);
                double[] _a = gson.fromJson(A.get(i).getAsJsonArray(), double[].class);
                double _b = b.get(i).getAsDouble();
                constraints.add( new SparseHyperplane(_idx, _a, _b) );
            }
        }
        
        if(input.has("SparsePolyhedral")){
            JsonArray idx = input.getAsJsonObject("SparsePolyhedral").getAsJsonArray("idx");
            JsonArray A = input.getAsJsonObject("SparsePolyhedral").getAsJsonArray("A");
            JsonArray b = input.getAsJsonObject("SparsePolyhedral").getAsJsonArray("b");
            for(int i = 0; i < b.size(); i++){
                int[] _idx = gson.fromJson(idx.get(i).getAsJsonArray(), int[].class);
                double[] _a = gson.fromJson(A.get(i).getAsJsonArray(), double[].class);
                double _b = b.get(i).getAsDouble();
                constraints.add( new SparseHalfspace(_idx, _a, _b) );
            }
        }
    }
    
    @Override
    public void solve() {
        
        String neighbor = neighbors.get(0);
        
        // init
        double[] y = getProximalPoint(neighbor); // starting point
        double[] x; // projected solution
        
        if(constraints.size() == 1){
            // single constraint
            x = constraints.get(0).projection(y);
        }
        else{
            // set of constraint
            x = doDykstraProjections(y, constraints);
        }
        
        // put values into variables
        variables.put(neighbor, x);
    }
    
    @Override
    public double evalConstraintViolation() {
        
        String neighbor = neighbors.get(0);
        double[] x = variables.get(neighbor);
        
        double cv = 0;
        Iterator<Constraint> itr = constraints.iterator();
        
        while(itr.hasNext()){
            cv += itr.next().violation(x);
        }
        
        return cv;
    }
    
    /**
     * Implements Dykstra's alternating projection algorithm
     */
    public double[] doDykstraProjections(double[] y, List<Constraint> constraints){
        
        double tol = 1e-5;
        
        int n = y.length;
        double[] x = y.clone();
        double[] xI;
        double[] xold;
        
        double[][] I = new double[constraints.size()][n];
        
        do {
            xold = x.clone();
            
            for(int i = 0; i < constraints.size(); i++){
                xI = minus(x, I[i]);
                x = constraints.get(i).projection(xI);
                I[i] = minus(x, xI);
            }
            
        }while(distance(x, xold) > tol);
        
        return x;
    }
    
    public double[] minus(double[] a, double[] b) {
        int n = a.length;
        double[] c = new double[n];
        for(int i = 0; i < n; i++) {
            c[i] = a[i] - b[i];
        }
        
        return c;
    }
    
    public double distance(double[] a, double[] b) {
        double dist = 0;
        for(int i = 0; i < a.length; i++) {
            dist += Math.pow(a[i] - b[i], 2);
        }
        return Math.sqrt(dist);
    }
    
    /**
     * Abstract class representing Constraint
     */
    public abstract class Constraint {
        
        public abstract double violation(double[] x);
        
        public abstract double[] projection(double[] x);
    }
    
    /**
     * LpBox
     */
    public class LpBox extends Constraint{
        // default values
        int p = 2; // defines Lp norm
        double shift = 0.5; // center
        
        public LpBox(int p, double shift) {
            this.p = p;
            this.shift = shift;
        }
        
        @Override
        public double violation(double[] x) {
            int n = x.length;
            double norm = 0;
            for(int i = 0; i < n; i++){
                norm += Math.pow(Math.abs(x[i] - shift), p);
            }            
            return Math.abs(norm - n/Math.pow(2, p));
        }
        
        @Override
        public double[] projection(double[] y) {
            
            int n = y.length;
            
            double[] x = new double[n];
            double normp_shift = 0;
            for(int i = 0; i < n; i++){
                x[i] = y[i] - shift;
                normp_shift += Math.pow(Math.abs(x[i]), p);
            }
            normp_shift = Math.pow(normp_shift, 1d/p);
            
            double k = normp_shift/(Math.pow(n,1d/p)/2d);
            for(int i = 0; i < n; i++){
                x[i] = x[i]/k + shift;
            }
            return x;
        }
    }
    
    /**
     * lb <= x <= ub
     */
    public class Box extends Constraint{
        
        double lb;
        double ub;
        
        public Box(double lb, double ub) {
            this.lb = lb;
            this.ub = ub;
        }
        
        @Override
        public double violation(double[] x) {
            double cv = 0;
            for(int i = 0; i < x.length; i++){
                cv += Math.max(lb-x[i], 0);
                cv += Math.max(x[i]-ub, 0);
            }
            return cv;
        }
        
        @Override
        public double[] projection(double[] x) {
            for(int j = 0; j < x.length; j++){
                x[j] = Math.min(Math.max(lb, x[j]), ub);
            }
            return x;
        }
    }
    
    public class Rectangle extends Constraint{
        
        double[] lb;
        double[] ub;
        
        public Rectangle(double[] lb, double[] ub) {
            this.lb = lb;
            this.ub = ub;
        }
        
        @Override
        public double violation(double[] x) {
            double cv = 0;
            for(int i = 0; i < x.length; i++){
                cv += Math.max(lb[i]-x[i], 0);
                cv += Math.max(x[i]-ub[i], 0);
            }
            return cv;
        }
        
        @Override
        public double[] projection(double[] x) {
            for(int j = 0; j < x.length; j++){
                x[j] = Math.min(Math.max(lb[j], x[j]), ub[j]);
            }
            return x;
        }
    }
        
    /**
     * c*x <= d
     */
    public class Halfspace extends Constraint{
        
        public double[] a;
        public double b;
        
        public Halfspace(double[] a, double b) {
            this.a = a;
            this.b = b;
        }
        
        @Override
        public double violation(double[] x) {
            double ax = 0;
            for(int i = 0; i < x.length; i++){
                ax += a[i] * x[i];
            }
            return Math.max(ax - b, 0);
        }
        
        @Override
        public double[] projection(double[] x) {
            int n = a.length;
            
            double ax = 0;
            double aa = 0;
            for(int i = 0; i < n; i++){
                ax += a[i]*x[i];
                aa += a[i]*a[i];
            }
            
            if(ax > b){
                double d = (b - ax)/aa;
                
                // projection
                double[] y = new double[n];
                for(int i = 0; i < n; i++){
                    y[i] = x[i] + d*a[i];
                }
                
                return y;
            }            
            return x;
        }
    }
    
    /**
     * a*x = b
     */
    public class Hyperplane extends Constraint{
        
        double[] a;
        double b;
        
        public Hyperplane(double[] a, double b) {
            this.a = a;
            this.b = b;
        }
        
        @Override
        public double violation(double[] x) {
            double ax = 0;
            for(int i = 0; i < x.length; i++){
                ax += a[i] * x[i];
            }
            return Math.abs(ax - b);
        }
        
        @Override
        public double[] projection(double[] x) {
            int n = a.length;
            
            double ax = 0;
            double aa = 0;
            for(int i = 0; i < n; i++){
                ax += a[i]*x[i];
                aa += a[i]*a[i];
            }
            
            double d = (b - ax)/aa;
            
            // projection
            double[] y = new double[n];
            for(int i = 0; i < n; i++){
                y[i] = x[i] + d*a[i];
            }            
            return y;
        }
    }
    
    /**
     * a*x <= b
     */
    public class SparseHalfspace extends Constraint{
        
        int[] idx;
        double[] a;
        double b;
        
        public SparseHalfspace(int[] idx, double[] a, double b) {
            this.idx = idx;
            this.a = a;
            this.b = b;
        }
        
        @Override
        public double violation(double[] x) {
            double ax = 0;
            for(int i = 0; i < idx.length; i++){
                ax += a[i] * x[idx[i]];
            }
            return Math.max(ax - b, 0);
        }
        
        @Override
        public double[] projection(double[] x) {
            
            double ax = 0;
            double aa = 0;
            for(int i = 0; i < idx.length; i++){
                ax += a[i]*x[idx[i]];
                aa += a[i]*a[i];
            }
            
            if(ax > b){
                double d = (b - ax)/aa;
                
                // projection
                double[] y = x.clone();
                for(int i = 0; i < idx.length; i++){
                    y[idx[i]] += d*a[i];
                }
                
                return y;
            }            
            return x;
        }
    }
    
    /**
     * A*x = b
     */
    public class SparseHyperplane extends Constraint{
        
        int[] idx;
        double[] a;
        double b;
        
        public SparseHyperplane(int[] idx, double[] a, double b) {
            this.idx = idx;
            this.a = a;
            this.b = b;
        }
        
        @Override
        public double violation(double[] x) {
            double ax = 0;
            for(int i = 0; i < a.length; i++){
                ax += a[i] * x[idx[i]];
            }
            return Math.abs(ax - b);
        }
        
        @Override
        public double[] projection(double[] x) {
            double ax = 0;
            double aa = 0;
            for(int i = 0; i < idx.length; i++){
                ax += a[i]*x[idx[i]];
                aa += a[i]*a[i];
            }
            
            double d = (b - ax)/aa;
            
            // projection
            double[] y = x.clone();
            for(int i = 0; i < idx.length; i++){
                y[idx[i]] += d*a[i];
            }
            return y;
        }
    }
    
    /**
     * sum x^2 = 1
     */
    public class Ball extends Constraint{
        
        @Override
        public double violation(double[] x) {
            int n = x.length;
            double norm = 0;
            for(int i = 0; i < n; i++){
                norm += x[i]*x[i];
            }
            norm = Math.sqrt(norm);
            return Math.abs(norm - 1.);
        }
        
        @Override
        public double[] projection(double[] y) {
            int n = y.length;
            
            double[] x = new double[n];
            double norm = 0;
            for(int i = 0; i < n; i++){
                norm += y[i]*y[i];
            }
            norm = Math.sqrt(norm);
            
            if(norm < Double.MIN_VALUE){
                x[0] = 1d;
                return x;
            }
            
            for(int i = 0; i < n; i++){
                x[i] = y[i]/norm;
            }
            return x;
        }
    }
    
}
