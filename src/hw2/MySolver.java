package hw2;

// some useful things to import. add any additional imports you need.
import joeq.Class.jq_Class;
import joeq.Compiler.Quad.*;
import flow.Flow;
import java.util.*;
import joeq.Compiler.Quad.Operand.*;

/**
 * Skeleton class for implementing the Flow.Solver interface.
 */
public class MySolver implements Flow.Solver {

    protected Flow.Analysis analysis;

    /**
     * Sets the analysis.  When visitCFG is called, it will
     * perform this analysis on a given CFG.
     *
     * @param analyzer The analysis to run
     */
    public void registerAnalysis(Flow.Analysis analyzer) 
    {
        this.analysis = analyzer;
    }

    /**
     * Returns whether a list of quads contains an entry
     * or exit
     */
    private boolean containsEntryOrExit(Iterator<Quad> quads)
    {
        while(quads.hasNext())
        {
            Quad P = (Quad)quads.next();
            if(P==null)
                return true;
        }
        return false;
    }

    /**
     * Computes the meet over all predecessors/successors
     */
    private Flow.DataflowObject computeConfluence(Iterator<Quad> quads, boolean direction)
    {
        Flow.DataflowObject temp = analysis.newTempVar();
        while(quads.hasNext())
        {
            Quad P = (Quad)quads.next();
            if(P!=null)
            {
                if(direction)
                    temp.meetWith( analysis.getOut(P) );
                else
                    temp.meetWith( analysis.getIn(P) );
            }
            else
            {
                if(direction)
                    temp.meetWith( analysis.getEntry() );
                else
                    temp.meetWith( analysis.getExit() );
            }
        }
        return temp;
    }
    /**
     * Processes a quad in the specified direction.
     * Return value is true if changes were made
     */
    private boolean transfer(Quad quad, boolean direction)
    {
        Flow.DataflowObject original, modified;
        if(direction)
            original = analysis.getOut(quad);
        else
            original = analysis.getIn(quad);
        analysis.processQuad(quad);

        if(direction)
            modified = analysis.getOut(quad);
        else
            modified = analysis.getIn(quad);

        return !modified.equals(original);
    }

    /**
     * Runs the solver over a given control flow graph.  Prior
     * to calling this, an analysis must be registered using
     * registerAnalysis
     *
     * @param cfg The control flow graph to analyze.
     */
    public void visitCFG(ControlFlowGraph cfg) {

        // this needs to come first.
        analysis.preprocess(cfg);
        boolean changesMade = true;
        while(changesMade)
        {
            changesMade = false;
            QuadIterator iter = new QuadIterator(cfg, analysis.isForward() );
            if(analysis.isForward())
            {
                while(iter.hasNext()) // Iterate forward
                {
                    Quad quad = (Quad)iter.next();
                    Iterator<Quad> pre = iter.predecessors();
                    analysis.setIn(quad, computeConfluence(pre, true) );
                    if(transfer(quad, true))
                        changesMade = true;

                    // Check if we need to update the exit
                    if(containsEntryOrExit(iter.successors()))
                    {
                        Flow.DataflowObject temp = analysis.getExit();
                        temp.meetWith(analysis.getOut(quad));
                        analysis.setExit(temp);
                    }
                }
            }
            else
            {
                while(iter.hasPrevious()) // Iterate backward
                {
                    Quad quad = (Quad)iter.previous();
                    Iterator<Quad> pre = iter.successors();
                    analysis.setOut(quad, computeConfluence(pre, false ) );
                    if(transfer(quad, false))
                        changesMade = true;

                    // Check if we need to update the entry
                    if(containsEntryOrExit(iter.predecessors()))
                    {
                        Flow.DataflowObject temp = analysis.getEntry();
                        temp.meetWith(analysis.getIn(quad));
                        analysis.setEntry(temp);
                    }
                }
            }
        }
        // this needs to come last.
        analysis.postprocess(cfg);
    }
}
