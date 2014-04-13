package ru.spbau.mit.optimizer.boxing;

import org.jetbrains.org.objectweb.asm.ClassVisitor;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Opcodes;

public class BasicClassVisitor extends ClassVisitor {

    private String className;

    public BasicClassVisitor() {
        super(Opcodes.ASM5);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        className = name;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        return new RedundantBoxingMethodVisitor(className, access, name, desc, signature);
    }
}
