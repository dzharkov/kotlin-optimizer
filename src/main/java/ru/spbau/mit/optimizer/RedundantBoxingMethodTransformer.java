package ru.spbau.mit.optimizer;

import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode;
import org.jetbrains.org.objectweb.asm.tree.MethodNode;
import org.jetbrains.org.objectweb.asm.tree.analysis.Analyzer;
import org.jetbrains.org.objectweb.asm.tree.analysis.AnalyzerException;
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue;
import ru.spbau.mit.MethodTransformer;

import java.util.List;

public class RedundantBoxingMethodTransformer extends MethodTransformer {
    public RedundantBoxingMethodTransformer(MethodTransformer methodTransformer) {
        super(methodTransformer);
    }

    @Override
    public void transform(String owner, MethodNode node) {
        /*
        if (!owner.contains("com/jetbrains/jetpeople/company/CompanyPackage-Team-c763aba1") || !node.name.equals("getTeams")) {
            return;
        }
        */

        for (BoxedBasicValue value : analyze(owner, node)) {
            for (AbstractInsnNode insnNode : value.getAssociatedInsns()) {
                node.instructions.remove(insnNode);
            }
        }

        super.transform(owner, node);
    }

    private static List<BoxedBasicValue> analyze(String owner, MethodNode node) {
        RedundantBoxingInterpreter interpreter = new RedundantBoxingInterpreter(node.instructions);
        Analyzer<BasicValue> analyzer = new Analyzer<BasicValue>(interpreter);

        try {
            analyzer.analyze(owner, node);
        } catch (AnalyzerException e) {
            throw new RuntimeException(e);
        }

        return interpreter.getCandidatesBoxedValues();
    }

}
