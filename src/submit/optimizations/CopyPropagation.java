package submit.optimizations;

import flow.Flow;
import java.util.*;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.*;

import submit.analyses.MySolver;
import submit.analyses.MustReachCopies;
import submit.analyses.MustReachCopies.*;

public class CopyPropagation extends Optimization {

    public void visitCFG(ControlFlowGraph cfg) {

        //System.out.println("Method: "+cfg.getMethod().getName().toString());

        MySolver solver = new MySolver();
        MustReachCopies mustReachCopies = new MustReachCopies();
        solver.registerAnalysis(mustReachCopies);
        solver.visitCFG(cfg);

        HashMap<String, TreeSet<Integer>> copies = new HashMap<String, TreeSet<Integer>>();
        HashMap<Integer, Quad> quadLookup = new HashMap<Integer, Quad>();

        // Generate copies map
        QuadIterator qit = new QuadIterator(cfg);
        while (qit.hasNext())
        {
            Quad q = (Quad)qit.next();
            int id = q.getID();

            quadLookup.put(id, q);

            if (q.getOperator() instanceof Operator.Move) {
                for (RegisterOperand def : q.getDefinedRegisters()) 
                {
                    String key = def.getRegister().toString();
                    
                    if(copies.get(key) == null)  copies.put(key, new TreeSet<Integer>());
                    TreeSet<Integer> copySet = copies.get(key);
                    
                    copySet.add(id);
                    copies.put(key, copySet);
                }
            }
        }

        qit = new QuadIterator(cfg);
        while(qit.hasNext())
        {
            Quad quad = qit.next();

            MustReachCopyFlowObject reachingCopies = (MustReachCopyFlowObject) mustReachCopies.getIn(quad);

            for (RegisterOperand def : quad.getUsedRegisters()) {
                String key = def.getRegister().toString();
                Set<Integer> copySet = copies.get(key);
                
                if (copySet != null){

                    Set<Integer> optimizableCopies = reachingCopies.intersection(copySet);
                    if (optimizableCopies.size() == 1){
                        //System.out.println(quad);
                        for (Integer copy : optimizableCopies){
                            Quad copyQuad = quadLookup.get(copy);
                            
                            //System.out.println(copyQuad);

                            RegisterOperand src =  (RegisterOperand) Operator.Move.getSrc(copyQuad).copy();
                            RegisterOperand dest = (RegisterOperand) Operator.Move.getDest(copyQuad).copy();

                            if (!src.getRegister().equals(dest.getRegister()))
                            {
                                for (RegisterOperand op : quad.getUsedRegisters()){
                                    if (op.getRegister().equals(dest.getRegister())){
                                        op.setRegister(src.getRegister());
                                        modifiedFlowGraph = true;
                                    }
                                }
                            }
                        }
                        //System.out.println(quad);

                    }else if(optimizableCopies.size() > 1){
                        throw new RuntimeException("Too many optomizable copies");
                    }   
                }
            }
        }
    }
}