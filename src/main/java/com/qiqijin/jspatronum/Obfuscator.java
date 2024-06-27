package com.qiqijin.jspatronum;

import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.*;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 *
 * 代码混淆器的主要功能类
 * @author
 */
public class Obfuscator {
    private AstRoot astRoot;
    private CompilerEnvirons compilerEnvirons = new CompilerEnvirons();
    private ErrorReporter errorReporter;
    private Boolean compress;
    private Boolean disableConsole;
    String hostName;
    
    /**
     * 
     * 
     * @param in 待混淆的代码
     * @param errorReporter 记录代码的 error
     *
     * @throws IOException
     */
    public Obfuscator(Reader in, Boolean compress, Boolean disableConsole, String hostName,ErrorReporter errorReporter) throws IOException {
        this.compress = compress;
        this.disableConsole = disableConsole;
        this.hostName = hostName;
        this.errorReporter = errorReporter;
        this.compilerEnvirons = CompilerEnvirons.ideEnvirons();
        this.astRoot = new Parser(this.compilerEnvirons, this.errorReporter).parse(in, null, 1);
    }

    /**
     *
     * 刷新修改后的语法树
     */
    private void freshAST() {
        this.astRoot = new Parser(this.compilerEnvirons, this.errorReporter).parse(this.astRoot.toSource(), null, 1);
    }

    /**
     *
     * 打印语法树信息
     */
    private void printAst() {
        this.astRoot.visit(new NodeVisitor() {
                @Override
                public boolean visit(AstNode astNode) {
                    String indent = "%1$Xs".replace("X", String.valueOf(astNode.depth() + 1));
                    System.out.format(indent, "").println(astNode.getClass());
                    return true;
                }
            });
    }

    /**
     *
     * 测试各个类是否正常执行
     */
    private void Test() {
        if (this.disableConsole != null && this.disableConsole.booleanValue()) {
            VisitorDisableConsole visitorDisableConsole = new VisitorDisableConsole();
            this.astRoot.visit(visitorDisableConsole);
        }
        if (this.hostName != null) {
            VisitorHostNameBind visitorHostNameBind = new VisitorHostNameBind(hostName);
            this.astRoot.visit(visitorHostNameBind);
        }
        VisitorGlobalVar visitorGlobalVar = new VisitorGlobalVar();
        this.astRoot.visit(visitorGlobalVar);
        VisitorPropertyToElement visitorPropertyToElement = new VisitorPropertyToElement();
        this.astRoot.visit(visitorPropertyToElement);
        VisitorLiteralToVar visitorLiteralToVar = new VisitorLiteralToVar();
        this.astRoot.visit(visitorLiteralToVar);
        VisitorTopFunction visitorTopFunction = new VisitorTopFunction(visitorLiteralToVar.getParams(), visitorLiteralToVar.getArguments());
        this.astRoot.visit(visitorTopFunction);
        VisitorStringToArray visitorStringToArray = new VisitorStringToArray();
        this.astRoot.visit(visitorStringToArray);
        this.freshAST();
        VisitorSetScope visitorSetScope = new VisitorSetScope();
        this.astRoot.visit(visitorSetScope);
        VisitorLocalVar visitorLocalVar = new VisitorLocalVar();
        this.astRoot.visit(visitorLocalVar); 
        VisitorConstant visitorConstant = new VisitorConstant();
        this.astRoot.visit(visitorConstant);
    }

	/**
	 *
	 * 对外接口，执行混淆操作
	 */
    public void obfuscate() {
        // this.printAst();  
        this.Test();
    }

	/**
	 *
	 * 对外接口，执行压缩操作。
     * 生成混淆后的代码写到新文件中。
	 * @param out
	 *
	 * @throws IOException
	 */
    public void compress(Writer out) throws IOException {
        // String source = this.astRoot.toSource(); 
        // String string = this.removeBlankCharacter(source);
        String source;
        if (this.compress != null && this.compress.booleanValue()) {
            source = new Compressor(this.astRoot).compress();
        } else {
            source = this.astRoot.toSource();
        }
        // out.write(new StringHack().escapedCharacters(this.astRoot.toSource()));
        source = new StringHack().escapedCharacters(source);
        out.write(source);
    }
    
    String removeBlankCharacter(String source) {
        boolean isString = false;
        StringBuffer compressedBuffer = new StringBuffer();
        String[] sources = source.split(" |\n");
        for (String word : sources) {
            String[] split = word.split("\"");
            if (!word.isEmpty()) {
                compressedBuffer.append(word);
                if (!isString &&
                    split.length % 2 == 0) {
                    isString = true;
                } else if (split.length % 2 == 0) {
                    isString = false;
                }
            }
            if (isString) {
                compressedBuffer.append(" ");
            }
            if (word.equals("var") ||
                word.equals("(var") ||
                word.equals("continue") ||
                word.equals("return") ||
                word.equals("else")) {
                compressedBuffer.append(" ");
            }
        }
        return compressedBuffer.toString();
    }
}
