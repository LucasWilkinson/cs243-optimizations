package submit.optimizations;

import flow.Flow;
import java.util.*;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Class.jq_Class;
import joeq.Main.Helper;
import joeq.Compiler.Quad.*;

import submit.analyses.MySolver;
import submit.analyses.Faintness;
import submit.analyses.Faintness.*;

public class RemoveDeadCode extends Optimization {

    public void visitCFG(ControlFlowGraph cfg) {

        MySolver solver = new MySolver();
        Faintness faintAnalysis = new Faintness();
        solver.registerAnalysis(faintAnalysis);
        solver.visitCFG(cfg);

        QuadIterator iter = new QuadIterator(cfg);

        while(iter.hasNext())
        {
            Quad quad = iter.next();

            VarSet faintVars = (VarSet) faintAnalysis.getOut(quad);
            boolean deadCode = false;

            for (RegisterOperand def : quad.getDefinedRegisters()) 
            {
            		if (faintVars.isFaint(def.getRegister().toString()))
            		{
            			deadCode = true;
            		}
            }

            if (deadCode) {
                iter.remove();
                modifiedFlowGraph = true;
            }
        }
    }
}