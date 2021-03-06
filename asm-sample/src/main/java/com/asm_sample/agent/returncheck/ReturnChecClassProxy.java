package com.asm_sample.agent.returncheck;

import com.asm_sample.util.CustomLogger;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;

/**
 * @author zacconding
 * @Date 2018-01-22
 * @GitHub : https://github.com/zacscoding
 */
public class ReturnChecClassProxy extends ClassVisitor implements Opcodes {
    public ReturnChecClassProxy(ClassVisitor cv) {
        super(ASM5, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if(mv == null) {
            return mv;
        }

        if(name.startsWith("get") && desc.startsWith("(I)")) {
            CustomLogger.println("## find target name : {} , desc : {}", name, desc);
            return new ReturnCheckClassProxy2MV(access, desc, mv);
        }

        if("getName".equals(name) && "(ILjava/lang/String;)Ljava/lang/String;".equals(desc)) {
            System.out.println("## find target");
            return new ReturnCheckClassProxyMV(access, desc, mv);
        }
        return mv;
    }
}

class ReturnCheckClassProxy2MV extends LocalVariablesSorter implements Opcodes {
    private String paramDesc;
    private Type returnType;
    protected ReturnCheckClassProxy2MV(int access, String desc, MethodVisitor mv) {
        super(ASM5, access, desc, mv);
        returnType = Type.getReturnType(desc);
        String returnDesc = returnType.getDescriptor();

        if(returnDesc.length() == 1) {
            paramDesc = "(" + returnDesc + ")Ljava/lang/String;";
        }
        else if(returnDesc.endsWith("String;")) {
            paramDesc = null;
        }
        else {
            paramDesc = "(Ljava/lang/Object;)Ljava/lang/String;";
        }
        System.out.println("paramDesc :: " + paramDesc);
    }

    @Override
    public void visitInsn(int opcode) {
        if ((opcode >= IRETURN && opcode <= RETURN)) {
            switch(returnType.getSort()) {
                case Type.LONG :
                case Type.DOUBLE :
                    mv.visitInsn(Opcodes.DUP2);
                    break;
                default :
                    mv.visitInsn(Opcodes.DUP);
            }
            if(paramDesc != null) {
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/String", "valueOf", paramDesc, false);
            }
            mv.visitMethodInsn(INVOKESTATIC, "com/asm_sample/agent/returncheck/ReturnCheckPrinter", "displayReturn", "(Ljava/lang/String;)V", false);
        }
        mv.visitInsn(opcode);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        mv.visitMaxs(maxStack +2, maxLocals);
    }
}


class ReturnCheckClassProxyMV extends LocalVariablesSorter implements Opcodes {
    private int firstParamIdx, secondParamIdx;
    protected ReturnCheckClassProxyMV(int access, String desc, MethodVisitor mv) {
        super(ASM5, access, desc, mv);
    }

    @Override
    public void visitCode() {
        // int firstParamIdxLocalVariable = first arg
        mv.visitVarInsn(ILOAD, 1);
        firstParamIdx = newLocal(Type.getType(int.class));
        mv.visitVarInsn(ISTORE, firstParamIdx);

        // String secondParamIdxLocalVariable = second arg
        mv.visitVarInsn(ALOAD, 2);
        secondParamIdx = newLocal(Type.getType(String.class));
        mv.visitVarInsn(ASTORE, secondParamIdx);

        // check local variable
        mv.visitLdcInsn("[check local variable]");
        mv.visitVarInsn(ILOAD, 1);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "com/asm_sample/agent/returncheck/ReturnCheckPrinter", "displayLocalVariable", "(Ljava/lang/String;ILjava/lang/String;)V", false);
        mv.visitCode();
    }

    @Override
    public void visitInsn(int opcode) {
        if ((opcode >= IRETURN && opcode <= RETURN)) {
            // pop & copy from stack == Return Object
            mv.visitInsn(Opcodes.DUP);
            // first local variable
            mv.visitVarInsn(Opcodes.ILOAD, firstParamIdx);
            // second local variable
            mv.visitVarInsn(Opcodes.ALOAD, secondParamIdx);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "com/asm_sample/agent/returncheck/ReturnCheckPrinter", "displayReturnAndParam", "(Ljava/lang/String;ILjava/lang/String;)V", false);
        }
        mv.visitInsn(opcode);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        mv.visitMaxs(maxStack +2, maxLocals + 2);
    }
}


