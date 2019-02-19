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

            if(quad.getOperator() instanceof Operator.BoundsCheck){
                CheckerSet checkedRegisters = (CheckerSet) mustReachBoundsChecks.getIn(quad);
                
                boolean checked = false;

                Operand array = Operator.BoundsCheck.getRef(quad);
        			Operand index = Operator.BoundsCheck.getIndex(quad);
        			
        			if (array instanceof RegisterOperand)
        			{
        				RegisterOperand regarray = (RegisterOperand) array;
        				
        				if (index instanceof RegisterOperand)
        				{
        					RegisterOperand regindex = (RegisterOperand) index;
        					String key = regindex.getRegister().toString();
        					CheckedArraySet cas = checkedRegisters.get(key);
        					
        					if (cas.hasArray(regarray.getRegister().toString()))
        					{
        						checked = true;
        					}
        				}
        				else if (index instanceof IConstOperand)
        				{
        					IConstOperand constindex = (IConstOperand) index;
        					Integer c = constindex.getValue();
        					String key = c.toString();
        					CheckedArraySet cas = checkedRegisters.get(key);
        					
        					if (cas.hasArray(regarray.getRegister().toString()))
        					{
        						checked = true;
        					}
        				}
        			}
        			else if(array instanceof AConstOperand)
        			{
        				AConstOperand constarray = (AConstOperand) array;
        				
        				if (index instanceof RegisterOperand)
        				{
        				    RegisterOperand regindex = (RegisterOperand) index;
        					String key = regindex.getRegister().toString();
        					CheckedArraySet cas = checkedRegisters.get(key);
        					
        					if (cas.hasArray(constarray.getValue().toString()))
        					{
        						checked = true;
        					}
        				}
        				else if (index instanceof IConstOperand)
        				{
        					IConstOperand constindex = (IConstOperand) index;
        					Integer c = constindex.getValue();
        					String key = c.toString();
        					CheckedArraySet cas = checkedRegisters.get(key);
        					
        					if (cas.hasArray(constarray.getValue().toString()))
        					{
        						checked = true;
        					}
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