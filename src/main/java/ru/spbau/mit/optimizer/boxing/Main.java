package ru.spbau.mit.optimizer.boxing;

import org.apache.commons.io.FileUtils;
import org.jetbrains.org.objectweb.asm.ClassReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Main implements Runnable {
    private final File root;

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
                checkClass((File)file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void checkClass(File file) throws IOException {
        ClassReader cr = new ClassReader(new FileInputStream(file));

        BasicClassVisitor visitor = new BasicClassVisitor();

        cr.accept(visitor, ClassReader.SKIP_DEBUG);
    }
}
