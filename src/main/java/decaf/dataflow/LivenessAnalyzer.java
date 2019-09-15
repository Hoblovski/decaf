package decaf.dataflow;

import decaf.instr.InstrLike;

import java.util.TreeSet;
import java.util.function.Consumer;

public class LivenessAnalyzer<I extends InstrLike> implements Consumer<CFG<I>> {

    @Override
    public void accept(CFG<I> graph) {
        for (var bb : graph.nodes) {
            computeDefAndLiveUseFor(bb);
            bb.liveIn = new TreeSet<>();
            bb.liveIn.addAll(bb.liveUse);
            bb.liveOut = new TreeSet<>();
        }

        var changed = true;
        do {
            changed = false;
            for (var bb : graph.nodes) {
                for (var next : graph.getSucc(bb.bbNum)) {
                    bb.liveOut.addAll(graph.getBlock(next).liveIn);
                }
                bb.liveOut.removeAll(bb.def);
                if (bb.liveIn.addAll(bb.liveOut)) {
                    changed = true;
                }
                for (var next : graph.getSucc(bb.bbNum)) {
                    bb.liveOut.addAll(graph.getBlock(next).liveIn);
                }
            }
        } while (changed);

        for (var bb : graph.nodes) {
            analyzeLivenessForEachLocIn(bb);
        }
    }

    /**
     * Compute the {@code def} and {@code liveUse} set for basic block {@code bb}.
     *
     * Recall the definition:
     * - {@code def}: set of all variables (i.e. temps) that are assigned to a value. Thus, we simply union all the
     * written temps of every instruction.
     * - {@code liveUse}: set of all variables (i.e. temps) that are used before they are assigned to a value in this
     * basic block. Note this is NOT simply equal to the union set all read temps, but only those are not yet
     * assigned/reassigned.
     *
     * @param bb the basic block
     */
    private void computeDefAndLiveUseFor(BasicBlock<I> bb) {
        bb.def = new TreeSet<>();
        bb.liveUse = new TreeSet<>();

        for (var loc : bb) {
            bb.def.addAll(loc.instr.getWritten());
            for (var read : loc.instr.getRead()) {
                if (!bb.def.contains(read)) {
                    // used before being assigned to a value
                    bb.liveUse.add(read);
                }
            }
        }
    }

    /**
     * Perform liveness analysis for every single location in a basic block, so that we know at each program location,
     * which variables stay alive.
     *
     * Idea: realizing that every location loc can be regarded as a "mini" basic block -- a block containing that
     * instruction solely, then the data flow equations also hold, and the situation becomes much simpler:
     * - loc.liveOut = loc.next.liveIn
     * - loc.def is simply the set of written temps
     * - loc.liveUse is simply the set of read temps, since it is impossible to read and write a same temp
     * simultaneously
     * So you see, to back propagate every location solves the problem.
     *
     * @param bb the basic block
     */
    private void analyzeLivenessForEachLocIn(BasicBlock<I> bb) {
        var liveIn = new TreeSet<>(bb.liveOut);
        var it = bb.backwardIterator();
        while (it.hasNext()) {
            var loc = it.next();
            loc.liveOut = new TreeSet<>(liveIn);
            liveIn.addAll(loc.instr.getRead());
            liveIn.removeAll(loc.instr.getWritten());
            loc.liveIn = new TreeSet<>(liveIn);
        }
        // assert liveIn == bb.liveIn
    }
}