package submit;

import flow.Flow;
import java.util.*;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Class.jq_Class;
import joeq.Main.Helper;
import joeq.Compiler.Quad.*;

import hw2.MySolver;
import submit.MustReachNullChecks.*;

public class RemoveRedundantNullChecks extends Optimization {

    public void visitCFG(ControlFlowGraph cfg) {

        MySolver solver = new MySolver();
        MustReachNullChecks mustReachNullChecks = new MustReachNullChecks();
        solver.registerAnalysis(mustReachNullChecks);
        solver.visitCFG(cfg);

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