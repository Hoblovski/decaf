package decaf.backend.asm.x86;

import decaf.backend.asm.AsmEmitter;
import decaf.backend.asm.HoleInstr;
import decaf.backend.asm.SubroutineEmitter;
import decaf.backend.asm.SubroutineInfo;
import decaf.lowlevel.StringUtils;
import decaf.lowlevel.X86;
import decaf.lowlevel.instr.PseudoInstr;
import decaf.lowlevel.label.IntrinsicLabel;
import decaf.lowlevel.label.Label;
import decaf.lowlevel.tac.*;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

import static decaf.lowlevel.X86.*;

/**
 * Emit x86 assembly code.
 */
public final class X86AsmEmitter extends AsmEmitter {

    public X86AsmEmitter() {
        super("x86", X86.allocatableRegs, callerSaved);

        printer.println("# start of header");
        printer.println(".text");
        printer.println(".globl main");
        printer.println("# end of header");
        printer.println();
    }

    @Override
    public void emitVTable(VTable vtbl) {
        // vtable begin
        printer.println(".data");
        printer.println(".align 2");

        printer.printLabel(vtbl.label, "virtual table for " + vtbl.className);

        if (vtbl.parent.isPresent()) {
            var parent = vtbl.parent.get();
            printer.println(".word %s    # parent: %s", parent.label, parent.className);
        } else {
            printer.println(".word 0    # parent: none");
        }

        var index = pool.add(vtbl.className);
        printer.println(".word %s%d    # class name", STR_PREFIX, index);

        for (var entry : vtbl.getItems()) {
            printer.println(".word %s    # member method", entry.name);
        }

        printer.println();
        // vtable end
    }

    @Override
    public Pair<List<PseudoInstr>, SubroutineInfo> selectInstr(TacFunc func) {
        var selector = new X86InstrSelector(func.entry);
        for (var instr : func.getInstrSeq()) {
            instr.accept(selector);
        }

        // TODO: better representation of SubroutineInfo. `argSize` and `hasCall` are not used.
        var info = new SubroutineInfo(func.entry, func.numArgs, false, 0);
        return Pair.of(selector.seq, info);
    }

    @Override
    public void emitSubroutineBegin() {
        printer.println(".text");
    }

    @Override
    public SubroutineEmitter emitSubroutine(SubroutineInfo info) {
        return new X86SubroutineEmitter(this, info);
    }

    @Override
    public String emitEnd() {
        if (!usedIntrinsics.isEmpty()) {
            printer.println("# start of intrinsics");
            if (usedIntrinsics.contains(Intrinsic.READ_LINE.entry)) {
                loadReadLine();
            }
            if (usedIntrinsics.contains(Intrinsic.STRING_EQUAL.entry)) {
                loadStringEqual();
            }
            if (usedIntrinsics.contains(Intrinsic.PRINT_BOOL.entry)) {
                loadPrintBool();
            }
            printer.println("# end of intrinsics");
            printer.println();
        }

        printer.println("# start of constant strings");
        printer.println(".data");
        var i = 0;
        for (var str : pool) {
            printer.printLabel(new Label(STR_PREFIX + i));
            printer.println(".asciz %s", StringUtils.quote(str));
            i++;
        }
        printer.println("# end of constant strings");

        return printer.close();
    }

    private void loadReadLine() {
        var loop = new Label(Intrinsic.READ_LINE.entry + "_loop");
        var exit = new Label(Intrinsic.READ_LINE.entry + "_exit");

        printer.printLabel(Intrinsic.READ_LINE.entry, "intrinsic: read line");
        printer.println("TODO: readline");
        printer.println();
    }

    private void loadStringEqual() {
        var loop = new Label(Intrinsic.STRING_EQUAL.entry + "_loop");
        var exit = new Label(Intrinsic.STRING_EQUAL.entry + "_exit");

        printer.printLabel(Intrinsic.STRING_EQUAL.entry, "intrinsic: string equal");
        printer.println("TODO: streq");
        printer.println();
    }

    private void loadPrintBool() {
        var trueString = new Label(Intrinsic.PRINT_BOOL.entry + "_S_true");
        var falseString = new Label(Intrinsic.PRINT_BOOL.entry + "_S_false");
        var isFalse = new Label(Intrinsic.PRINT_BOOL.entry + "_false");

        printer.printLabel(Intrinsic.PRINT_BOOL.entry, "intrinsic: print bool");
        printer.println("TODO: print bool");
    }

    private class X86InstrSelector implements TacInstr.Visitor {

        X86InstrSelector(Label entry) {
            this.entry = entry;
        }

        List<PseudoInstr> seq = new ArrayList<>();

        Label entry;

        // Arguments are Parm'ed left to right,
        // but i386-cc requires arguments to be pushed right to left.
        private Stack<PseudoInstr> pushArgInstrs = new Stack<>();

        boolean hasCall = false;

        @Override
        public void visitAssign(TacInstr.Assign instr) {
            seq.add(new Move(instr.dst, instr.src));
        }

        @Override
        public void visitLoadVTbl(TacInstr.LoadVTbl instr) {
            seq.add(new LoadAddr(instr.dst, instr.vtbl.label));
        }

        @Override
        public void visitLoadImm4(TacInstr.LoadImm4 instr) {
            seq.add(new LoadImm(instr.dst, instr.value));
        }

        @Override
        public void visitLoadStrConst(TacInstr.LoadStrConst instr) {
            var index = pool.add(instr.value);
            seq.add(new LoadAddr(instr.dst, new Label(STR_PREFIX + index)));
        }

        @Override
        public void visitUnary(TacInstr.Unary instr) {
            var op = switch (instr.op) {
                case NEG -> UnaryOp.NEG;
                case LNOT -> UnaryOp.NOT;
            };
            seq.add(new Move(instr.dst, instr.operand));
            seq.add(new Unary(op, instr.dst));
        }

        @Override
        public void visitBinary(TacInstr.Binary instr) {
            var op = switch (instr.op) {
                case ADD -> BinaryOp.ADD;
                case SUB -> BinaryOp.SUB;
                case MUL -> BinaryOp.MUL;
                case DIV -> BinaryOp.DIV;
                case MOD -> BinaryOp.REM;
                case LAND -> BinaryOp.AND;
                case LOR -> BinaryOp.OR;
                default -> BinaryOp.CMP;
            };
            seq.add(new Move(instr.dst, instr.lhs));
            seq.add(new Binary(op, instr.dst, instr.rhs));
        }

        @Override
        public void visitBranch(TacInstr.Branch instr) {
            assert false;
        }

        @Override
        public void visitCondBranch(TacInstr.CondBranch instr) {
            assert false;
        }

        @Override
        public void visitReturn(TacInstr.Return instr) {
            instr.value.ifPresent(v -> seq.add(new Move(EAX, v)));
            seq.add(new JumpToEpilogue(entry));
        }

        @Override
        public void visitParm(TacInstr.Parm instr) {
            pushArgInstrs.push(new Push(instr.value));
        }

        @Override
        public void visitIndirectCall(TacInstr.IndirectCall instr) {
            assert false;
        }

        @Override
        public void visitDirectCall(TacInstr.DirectCall instr) {
            hasCall = true;

            callerSave();
            pushArgs();
            seq.add(new X86Call(new Label(instr.entry.name)));
            callerRestore();

            // move return value from RAX to the specified location
            instr.dst.ifPresent(temp -> seq.add(new Move(temp, EAX)));
        }

        private void pushArgs() {
            while (!pushArgInstrs.empty()) {
                seq.add(pushArgInstrs.pop());
            }
        }

        private void callerSave() {
            seq.add(HoleInstr.CallerSave);
        }

        private void callerRestore() {
            seq.add(HoleInstr.CallerRestore);
        }

        @Override
        public void visitMemory(TacInstr.Memory instr) {
            seq.add(switch (instr.op) {
                case LOAD -> new LoadWord(instr.dst, instr.base, instr.offset);
                case STORE -> new StoreWord(instr.dst, instr.base, instr.offset);
            });
        }

        @Override
        public void visitMark(TacInstr.Mark instr) {
            seq.add(new X86Label(instr.label));
        }

        @Override
        public void visitOthers(TacInstr instr) {
            assert false;
        }
    }

    private StringPool pool = new StringPool();

    private Set<IntrinsicLabel> usedIntrinsics = new TreeSet<>();
}
