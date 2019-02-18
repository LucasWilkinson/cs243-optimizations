package submit.optimizations;

import flow.Flow;
import java.util.*;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operand.IConstOperand;
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

		System.out.println("Fuck you!");

        // Generate copies map
        QuadIterator qit = new QuadIterator(cfg);
        while (qit.hasNext())
        {
            Quad q = (Quad)qit.next();

            ConstantPropTable ct = (ConstantPropTable) constantProp.getIn(q);
			
			if (q.getOperator() instanceof Operator.Move)
			{
				Operand op = Operator.Move.getSrc(q);
				
				if (op instanceof RegisterOperand)
				{
					System.out.println("MM!");
					RegisterOperand regop = (RegisterOperand) op;
					
					String key = regop.getRegister().toString();
					
					SingleCP scp = (SingleCP) ct.get(key);
					
					Integer hash = scp.hashCode();
					
					System.out.println(hash.toString());
					
					if (scp.isConst())
					{
						System.out.println("Modifying Move!");
						IConstOperand c = new IConstOperand(scp.getConst());
						Operator.Move.setSrc(q, c);
						modifiedFlowGraph = true;
					}
				}
			}
			else if (q.getOperator() instanceof Operator.Binary)
			{
				Operand opone = Operator.Binary.getSrc1(q);
				Operand optwo = Operator.Binary.getSrc2(q);
				
				if (opone instanceof RegisterOperand)
				{
					System.out.println("MB1!");
					RegisterOperand regopone = (RegisterOperand) opone;
					
					String keyone = regopone.getRegister().toString();
					SingleCP scpone = (SingleCP) ct.get(keyone);
					
					Integer hash = scpone.hashCode();
					
					System.out.println(hash.toString());
					
					if (scpone.isConst())
					{
						System.out.println("Modifying Binary1!");
						IConstOperand c = new IConstOperand(scpone.getConst());
						Operator.Binary.setSrc1(q, c);
						modifiedFlowGraph = true;
					}
					
				}
				
				if (optwo instanceof RegisterOperand)
				{
					System.out.println("MB2!");
					RegisterOperand regoptwo = (RegisterOperand) optwo;
					
					String keytwo = regoptwo.getRegister().toString();
					SingleCP scptwo = (SingleCP) ct.get(keytwo);
					
					Integer hash = scptwo.hashCode();
					
					System.out.println(hash.toString());
					
					if (scptwo.isConst())
					{
						System.out.println("Modifying Binary2!");
						IConstOperand c = new IConstOperand(scptwo.getConst());
						Operator.Binary.setSrc2(q, c);
						modifiedFlowGraph = true;
					}
				}
			}
			else if (q.getOperator() instanceof Operator.Unary)
			{
				Operand op = Operator.Unary.getSrc(q);
				
				if (op instanceof RegisterOperand)
				{
					System.out.println("MU!");
					RegisterOperand regop = (RegisterOperand) op;
					
					String key = regop.getRegister().toString();
					
					SingleCP scp = (SingleCP) ct.get(key);
					
					Integer hash = scp.hashCode();
					
					System.out.println(hash.toString());
					
					if (scp.isConst())
					{
						System.out.println("Modifying Unary!");
						IConstOperand c = new IConstOperand(scp.getConst());
						Operator.Unary.setSrc(q, c);
						modifiedFlowGraph = true;
					}
				}
			}
        }
    }
}