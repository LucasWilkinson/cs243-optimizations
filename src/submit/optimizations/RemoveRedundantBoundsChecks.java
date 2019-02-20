package submit.optimizations;

import flow.Flow;
import java.util.*;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.*;
import joeq.Compiler.Quad.Operand.*;

import submit.analyses.MySolver;
import submit.analyses.MustReachBoundsChecks;
import submit.analyses.MustReachBoundsChecks.*;

public class RemoveRedundantBoundsChecks extends Optimization {

    public void visitCFG(ControlFlowGraph cfg) {

        MySolver solver = new MySolver();
        MustReachBoundsChecks mustReachBoundsChecks = new MustReachBoundsChecks();
        solver.registerAnalysis(mustReachBoundsChecks);
        solver.visitCFG(cfg);

        QuadIterator iter = new QuadIterator(cfg);

        while(iter.hasNext())
        {
            Quad quad = iter.next();

            if(quad.getOperator() instanceof Operator.BoundsCheck)
            {
                CheckerSet checkedRegisters = (CheckerSet) mustReachBoundsChecks.getIn(quad);
                
                boolean checked = false;

                Operand array = Operator.BoundsCheck.getRef(quad);
                Operand index = Operator.BoundsCheck.getIndex(quad);

                CheckedArraySet checkedArrays = (CheckedArraySet) checkedRegisters.get(index.toString());

                if (checkedArrays != null)
                {
                    //System.out.println(checkedArrays.toString() + array.toString() + checkedArrays.hasArray(array.toString()));

                    if (checkedArrays.hasArray(array.toString())){
                        //System.out.println("Redundant = " + quad);
                        checked = true;
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