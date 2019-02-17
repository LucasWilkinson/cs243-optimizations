package submit;

import flow.Flow;
import java.util.*;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Class.jq_Class;
import joeq.Main.Helper;
import joeq.Compiler.Quad.*;

import hw2.MySolver;
import submit.MustReachNullChecks.*;

// some useful things to import. add any additional imports you need.


public class FindRedundantNullChecks {

    public static class PrintRedundantNullChecks implements ControlFlowGraphVisitor {

        public void visitCFG(ControlFlowGraph cfg) {

            MySolver solver = new MySolver();
            MustReachNullChecks mustReachNullChecks = new MustReachNullChecks();
            solver.registerAnalysis(mustReachNullChecks);
            solver.visitCFG(cfg);

            List<Integer> redundantNullChecks = new Vector(); 
            QuadIterator iter = new QuadIterator(cfg);

            while(iter.hasNext())
            {
                Quad quad = iter.next();

                if(quad.getOperator() instanceof Operator.NullCheck){
                    CheckedRegistersFlowObject checkedRegisters = (CheckedRegistersFlowObject) mustReachNullChecks.getIn(quad);
                    boolean checked = true;

                    for (RegisterOperand def : quad.getUsedRegisters()) {
                        if (!checkedRegisters.isChecked(def.getRegister().toString())){
                            checked = false;
                        }
                    }

                    if (checked) {
                        redundantNullChecks.add(quad.getID());
                    }
                }
            }

            Collections.sort(redundantNullChecks);
            StringBuilder redundantNullChecksString = new StringBuilder();

            for (Integer id : redundantNullChecks){
                redundantNullChecksString.append(" ");
                redundantNullChecksString.append(id);
            }

            System.out.println(cfg.getMethod().getName().toString() + redundantNullChecksString);
        }
    }

    /*
     * args is an array of class names
     * method should print out a list of quad ids of redundant null checks
     * for each function as described on the course webpage
     */
    public static void main(String[] args) {

        PrintRedundantNullChecks printer = new PrintRedundantNullChecks();

        jq_Class[] classes = new jq_Class[args.length];
        for (int i=0; i < classes.length; i++){
            classes[i] = (jq_Class)Helper.load(args[i]);
            Helper.runPass(classes[i], printer);
        }
    }
}
