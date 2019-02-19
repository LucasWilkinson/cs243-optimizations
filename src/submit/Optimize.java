package submit;

import java.util.List;
import joeq.Compiler.Quad.*;
import joeq.Class.jq_Class;
import joeq.Main.Helper;

import submit.optimizations.RemoveRedundantNullChecks;
import submit.optimizations.CopyPropagation;
import submit.optimizations.RemoveDeadCode;
import submit.optimizations.PartialRedundancyElimination;
import submit.optimizations.Optimization;
import submit.optimizations.ConstantPropagation;

public class Optimize {
    /*
     * optimizeFiles is a list of names of class that should be optimized
     * if nullCheckOnly is true, disable all optimizations except "remove redundant NULL_CHECKs."
     */
    public static void optimize(List<String> optimizeFiles, boolean nullCheckOnly) {
        for (int i = 0; i < optimizeFiles.size(); i++) {
            jq_Class classToOptimize = (jq_Class)Helper.load(optimizeFiles.get(i));
            // Run your optimization on each classes.

            if (nullCheckOnly){
                RemoveRedundantNullChecks redundantNullChecks = new RemoveRedundantNullChecks();
                redundantNullChecks.optimizeClass(classToOptimize);
            }
            else
            {
                boolean modified = false;

                Optimization copyPropagation = new CopyPropagation();
                Optimization redundantNullChecks = new RemoveRedundantNullChecks();
                Optimization deadCode = new RemoveDeadCode();
                Optimization pre = new PartialRedundancyElimination();
                Optimization constantPropagation = new ConstantPropagation();

                do {

                    modified = false;

                    if (constantPropagation.optimizeClass(classToOptimize)){
                    		modified = true;
                    }
               
                    if (copyPropagation.optimizeClass(classToOptimize)){
                        modified = true;
                    }

                    if (deadCode.optimizeClass(classToOptimize)){
                        modified = true;
                    }

                    if (pre.optimizeClass(classToOptimize)){
                        modified = true;
                    }

                    if (redundantNullChecks.optimizeClass(classToOptimize)){
                        modified = true;
                    }
                    
                
                } while(modified);
            }

            if (!nullCheckOnly){
                CopyPropagation copyPropagation = new CopyPropagation();
                copyPropagation.optimizeClass(classToOptimize);
            }

            Helper.runPass(classToOptimize, new PrintCFG());
        }
    }
}
