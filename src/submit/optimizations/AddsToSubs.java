

package submit.optimizations;

import flow.Flow;
import java.util.*;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operand.IConstOperand;
import joeq.Compiler.Quad.*;

import submit.analyses.MySolver;
import submit.analyses.ConstantProp;
import submit.analyses.ConstantProp.*;

public class AddsToSubs extends Optimization {
 
    private boolean isConst(Operand op) {
        return (op instanceof IConstOperand);
    }

    private int getConst (Operand op) {
        if (op instanceof IConstOperand) {
            return ((IConstOperand)op).getValue();
        }
        throw new IllegalArgumentException("Tried to getConst a non-Const!");
    }

    public void visitCFG(ControlFlowGraph cfg) {

        QuadIterator iter = new QuadIterator(cfg);

        while(iter.hasNext())
        {
            Quad quad = iter.next();

            if (quad.getOperator() instanceof Operator.Binary.ADD_I){
                Operand op1 =  Operator.Binary.getSrc1(quad);
                Operand op2 =  Operator.Binary.getSrc2(quad);

                boolean modified = false;

                //System.out.println("******");

                if (isConst(op1) && !isConst(op2)) 
                {
                    //System.out.println(quad);
                    op1 = new IConstOperand(-1*getConst(op1));
                    modified = true;
                }
                else if (!isConst(op1) && isConst(op2))
                {
                    //System.out.println(quad);
                    op2 = new IConstOperand(-1*getConst(op2));
                    modified = true;
                }


                if (modified) {
                    Quad subTemp = Operator.Binary.create(cfg.getNewQuadID(), Operator.Binary.SUB_I.INSTANCE, (RegisterOperand) Operator.Binary.getDest(quad).copy(), op1.copy(), op2.copy());

                    int index = iter.getCurrentBasicBlock().getQuadIndex(quad);
                    iter.getCurrentBasicBlock().replaceQuad(index, subTemp);

                    //System.out.println(subTemp);

                    modifiedFlowGraph = true;
                }
            }
        }
    }
}
        