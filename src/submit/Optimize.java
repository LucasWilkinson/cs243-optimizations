package submit;

import java.util.List;
import joeq.Compiler.Quad.*;
import joeq.Class.jq_Class;
import joeq.Main.Helper;

import submit.RemoveRedundantNullChecks.*;

public class Optimize {
    /*
     * optimizeFiles is a list of names of class that should be optimized
     * if nullCheckOnly is true, disable all optimizations except "remove redundant NULL_CHECKs."
     */
    public static void optimize(List<String> optimizeFiles, boolean nullCheckOnly) {
        for (int i = 0; i < optimizeFiles.size(); i++) {
            jq_Class classToOptimize = (jq_Class)Helper.load(optimizeFiles.get(i));
            // Run your optimization on each classes.

            RemoveRedundantNullChecks redundantNullChecks = new RemoveRedundantNullChecks();
            redundantNullChecks.optimizeClass(classToOptimize);
        }
    }
}
