package submit.optimizations;

import flow.Flow;
import java.util.*;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.*;

import submit.analyses.MySolver;
import submit.analyses.MustReachNullChecks;
import submit.analyses.MustReachNullChecks.*;

public class RemoveRedundantNullChecks extends Optimization {

    public void visitCFG(ControlFlowGraph cfg) {

		//System.out.println("Method: "+cfg.getMethod().getName().toString());
		//System.out.println("Null Checks");
        MySolver solver = new MySolver();
        MustReachNullChecks mustReachNullChecks = new MustReachNullChecks();
        solver.registerAnalysis(mustReachNullChecks);
        solver.visitCFG(cfg);
        
        //System.out.println("Null Analysis Complete");

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
                    iter.remove();
                    modifiedFlowGraph = true;
                }
            }
        }
    }
}