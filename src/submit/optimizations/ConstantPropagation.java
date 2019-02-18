package submit.optimizations;

import flow.Flow;
import java.util.*;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.*;

import submit.analyses.MySolver;
import submit.analyses.ConstantProp;
import submit.analyses.ConstantProp.*;

public class ConstantPropagation extends Optimization {

    public void visitCFG(ControlFlowGraph cfg) {

        MySolver solver = new MySolver();
        ConstantProp constantProp = new ConstantProp();
        solver.registerAnalysis(constantProp);
        solver.visitCFG(cfg);

        // Generate copies map
        QuadIterator qit = new QuadIterator(cfg);
        while (qit.hasNext())
        {
            Quad q = (Quad)qit.next();

            ConstantPropTable ct = (ConstantPropTable) constantProp.getIn(quad);
			
			for (RegisterOperand def : quad.getUsedRegisters())
			{
				SingleCP scp = (SingleCP) ct.get(def.getRegister().toString());
				
				if (scp.isConst())
				{
					def.setRegister(scp.getConst());
				}
			}
           	
        }
    }
}