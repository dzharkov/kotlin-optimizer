package ru.spbau.mit.optimizer;

import org.apache.commons.io.FileUtils;
import org.jetbrains.org.objectweb.asm.ClassReader;
import org.jetbrains.org.objectweb.asm.ClassWriter;
import org.jetbrains.org.objectweb.asm.tree.ClassNode;
import ru.spbau.mit.ClassMethodsTransformer;

import java.io.*;

public class Main implements Runnable {
    private final File root;
    private final ClassMethodsTransformer classMethodsTransformer = new ClassMethodsTransformer(new RedundantBoxingMethodTransformer(null));

    public Main(File root) {
        this.root = root;
    }

    public static void main(String[] args) throws Exception {
        new Thread(new Main(new File(args[0]))).run();
    }

    @Override
    public void run() {
        try {
            for (Object file : FileUtils.listFiles(root, new String[]{"class"}, true)) {
                optimizeClass((File) file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void optimizeClass(File file) throws IOException {

        ClassReader cr = new ClassReader(new FileInputStream(file));
        ClassNode classNode = new ClassNode();
        cr.accept(classNode, ClassReader.SKIP_DEBUG);


        classMethodsTransformer.transform(classNode);

        ClassWriter classWriter = new BinaryClassWriter();
        classNode.accept(classWriter);

        File outputFile = new File(
                "test2/" +
                file.getPath().substring(5)
        );

        outputFile.getParentFile().mkdirs();

        FileOutputStream outputStream = new FileOutputStream(
                outputFile
        );

        outputStream.write(classWriter.toByteArray());

    }
}
