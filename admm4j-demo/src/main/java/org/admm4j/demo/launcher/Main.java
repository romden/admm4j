/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package org.admm4j.demo.launcher;

import org.admm4j.impl.base.AdmmContext;

import com.google.gson.JsonObject;

/**
 *
 * @author Roman Denysiuk
 */
public class Main {
    
    public static void printUsage() {
        System.out.println("Usage:");
        System.out.println("java -jar admm4j.jar [parameters]");
        System.out.println();
        System.out.println("Mandatory parameters:");
        System.out.println("    -input <path to JSON based input file>");
        System.out.println("    -nvar and -rho should be provided either in command line or in json input");
        System.out.println("    -nvar <number of variables>");
        System.out.println("    -rho <scaling parameter | 1.0 can be initial choice>");
        System.out.println();
        System.out.println("Optional parameters:");        
        System.out.println("    -tolPrimal <primal tolerance | default: 0.0001>");
        System.out.println("    -tolDual <dual tolerance | default: 0.0001>");
        System.out.println("    -iter <max number of iterations | default: Integer.MAX_VALUE>");
        System.out.println("    -time <time limit | default: Long.MAX_VALUE>");
        System.out.println("    -verbose <display iterations summary | default: 0");
        System.out.println("    -output <name of output file | default: output.json>");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("java -jar admm4j.jar -input myinput.json -output myoutput.json -nvar 10 -rho 1");
    }
    
    public static void main(String[] args){
        
        if(args.length < 1 || args[0].equalsIgnoreCase("-help")){
            printUsage();
            return;
        }
        
        AdmmContext context = new AdmmContext();
        JsonObject input = null;
        
        // user settings
        int i = 0;
        while (i < args.length) {
            if (args[i].startsWith("-")) {
                if(args[i].equalsIgnoreCase("-nvar")){
                    context.settings.addProperty("numVariables", Integer.parseInt(args[++i]));
                }
                else if(args[i].equalsIgnoreCase("-rho")){
                    context.settings.addProperty("scalingParameter", Double.parseDouble(args[++i]));
                }
                else if(args[i].equalsIgnoreCase("-tolPrimal")){
                    context.settings.addProperty("tolPrimal", Double.parseDouble(args[++i]));
                }
                else if(args[i].equalsIgnoreCase("-tolDual")){
                    context.settings.addProperty("tolDual", Double.parseDouble(args[++i]));
                }
                else if(args[i].equalsIgnoreCase("-iter")){
                    context.settings.addProperty("iterLimit", Integer.parseInt(args[++i]));
                }
                else if(args[i].equalsIgnoreCase("-time")){
                    context.settings.addProperty("iterLimit", Long.parseLong(args[++i]));
                }
                else if(args[i].equalsIgnoreCase("-verbose")){
                    context.settings.addProperty("verbose", Integer.parseInt(args[++i]));
                }
                else if(args[i].equalsIgnoreCase("-output")){
                    context.settings.addProperty("outputFile", args[++i]);
                }
                else if(args[i].equalsIgnoreCase("-input")){
                    input = context.getJsonFromFile(args[++i]);
                }
            }
            ++i;
        }
        
        // run ADMM
        if(input != null){
            context.execute(input);
        }
        else{
            System.out.println("No input file ...");
        }        
    }
}
