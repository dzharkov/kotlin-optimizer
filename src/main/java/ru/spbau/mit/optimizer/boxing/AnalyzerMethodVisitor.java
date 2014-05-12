package ru.spbau.mit.optimizer.boxing;


import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.tree.MethodNode;

abstract public class AnalyzerMethodVisitor extends MethodVisitor {
    protected final String owner;

    public AnalyzerMethodVisitor(String owner, int access, String name, String desc, String signature) {
        super(Opcodes.ASM5, new MethodNode(access, name, desc, signature, null));
        this.owner = owner;
    }
}
