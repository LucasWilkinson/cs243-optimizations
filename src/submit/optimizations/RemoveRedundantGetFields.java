package submit.optimizations;

import flow.Flow;
import java.util.*;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.*;
import joeq.Class.jq_Type;

import submit.analyses.MySolver;
import submit.analyses.MustReachGetField;
import submit.analyses.MustReachGetField.*;

public class RemoveRedundantGetFields extends Optimization {

    public void visitCFG(ControlFlowGraph cfg) {

        //System.out.println("Method: "+cfg.getMethod().getName().toString());

        MySolver solver = new MySolver();
        MustReachGetField mustReachGetField = new MustReachGetField();
        solver.registerAnalysis(mustReachGetField);
        solver.visitCFG(cfg);

        HashMap<String, TreeSet<Integer>> getField = new HashMap<String, TreeSet<Integer>>();
        HashMap<Integer, Quad> quadLookup = new HashMap<Integer, Quad>();

        // Generate copies map
        QuadIterator qit = new QuadIterator(cfg);
        while (qit.hasNext())
        {
            Quad q = (Quad)qit.next();
            int id = q.getID();

            quadLookup.put(id, q);
        }

        qit = new QuadIterator(cfg);
        while(qit.hasNext())
        {
            Quad quad = qit.next();

            if (mustReachGetField.isGetField(quad) && quad.getOperator() instanceof Operator.Getfield.GETFIELD_I){

                GetfieldSet getFieldSet = (GetfieldSet) mustReachGetField.getIn(quad);

                for (Integer id : getFieldSet.getSet() ){
                    Quad getQuad = quadLookup.get(id);

                    if (mustReachGetField.getFieldString(getQuad).equals(mustReachGetField.getFieldString(quad)))
                    {
                        RegisterOperand dest = (RegisterOperand) Operator.Getfield.getDest(quad).copy();
                        RegisterOperand src  = (RegisterOperand) Operator.Getfield.getDest(getQuad).copy();

                        //if (
                        //    src.getRegister().equals(dest.getRegister())){
                        //    qit.remove();
                        //    System.out.println(getQuad.toString() + " : " + quad.toString() + " -> Removed");
                        //}else{
                            jq_Type regType = dest.getType();

                            Operator.Move moveType = Operator.Move.getMoveOp(regType);
                            Quad copyTemp = Operator.Move.create(cfg.getNewQuadID(), moveType, dest, src);

                            int index = qit.getCurrentBasicBlock().getQuadIndex(quad);
                            qit.getCurrentBasicBlock().replaceQuad(index, copyTemp);

                            System.out.println(getQuad.toString() + " : " + quad.toString() + " -> " + copyTemp.toString());
                        //}

                        modifiedFlowGraph = true;
                        break;
                    }
                }

            }
        }
    }
}