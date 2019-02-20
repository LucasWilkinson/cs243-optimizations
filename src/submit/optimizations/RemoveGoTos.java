

package submit.optimizations;

import flow.Flow;
import java.util.*;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operand.IConstOperand;
import joeq.Compiler.Quad.*;

import submit.analyses.MySolver;

public class RemoveGoTos extends Optimization 
{
    public void visitCFG(ControlFlowGraph cfg) 
    {
    		System.out.println("Method: "+cfg.getMethod().getName().toString());

        QuadIterator iter = new QuadIterator(cfg);
        
        int quadIndex = 0;
        boolean modified = false;
        BasicBlock modifier = cfg.entry();

        while(iter.hasNext())
        {
            Quad quad = iter.next();
           
            if (quad.getOperator() instanceof Operator.Goto)
            {
            		modifier = iter.getCurrentBasicBlock();
				quadIndex = iter.getCurrentBasicBlock().getQuadIndex(quad);
				
				Integer id = quad.getID();
				System.out.println(id.toString());
				
				modified = true;
				break;
            }
        }
        
        
        if (modified)
        {
	        List<BasicBlock> succ = modifier.getSuccessors();
	        BasicBlock bbm = cfg.entry(); 
	            		
	    		for (BasicBlock bb : succ)
	    		{           			
	    			Iterator<Quad> quads = bb.iterator();

	    			while(quads.hasNext())
	    			{
	    				Quad cq = quads.next();
	    				
	    				Quad nq = cq.copy(cfg.getNewQuadID());
	    				
	    				System.out.println(nq.toString());
	    				modifier.appendQuad(nq);
	    			}
	    			
	    			bbm = bb;
	    		}  
	    		
	    		//change successors
    			modifier.removeAllSuccessors();
    			
    			List<BasicBlock> bbsucc = bbm.getSuccessors();
    			
    			for (BasicBlock bbs : bbsucc)
    			{
    				modifier.addSuccessor(bbs);
    			}
	    		
	    		modifier.removeQuad(quadIndex); 
	    		
	    		//change predecessors of bb
    			bbm.removePredecessor(modifier);
	    		
			cfg.removeUnreachableBasicBlocks();
			
	    		modifiedFlowGraph = true;
	    	}
    }
}
        