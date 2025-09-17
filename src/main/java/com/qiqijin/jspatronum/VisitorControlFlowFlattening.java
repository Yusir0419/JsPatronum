package com.qiqijin.jspatronum;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.mozilla.javascript.ast.*;

/**
 * 控制流平坦化混淆器
 * 将复杂的控制流结构转换为基于状态机的扁平结构，保持功能等价性
 *
 * @author Gin
 */
public class VisitorControlFlowFlattening implements NodeVisitor {

    private Random random = new Random();
    private int stateCounter = 0;
    private String stateVarName = "state_" + Math.abs(random.nextInt());
    private String switchVarName = "switch_" + Math.abs(random.nextInt());

    /**
     * 创建状态变量声明
     */
    private VariableDeclaration createStateVariable() {
        VariableDeclaration stateDecl = new VariableDeclaration();
        VariableInitializer stateInit = new VariableInitializer();

        Name stateName = new Name();
        stateName.setIdentifier(stateVarName);

        NumberLiteral initialState = new NumberLiteral();
        initialState.setValue("0");

        stateInit.setTarget(stateName);
        stateInit.setInitializer(initialState);
        stateDecl.addVariable(stateInit);

        return stateDecl;
    }

    /**
     * 创建不透明谓词 - 总是返回true的复杂表达式
     */
    private AstNode createOpaquePredicateTrue() {
        // (x * 2 + 1) % 2 == 1，其中x是任意整数，这个表达式总是为true
        NumberLiteral x = new NumberLiteral();
        x.setValue(String.valueOf(random.nextInt(1000) + 1));

        NumberLiteral two = new NumberLiteral();
        two.setValue("2");

        NumberLiteral one = new NumberLiteral();
        one.setValue("1");

        // x * 2
        InfixExpression mult = new InfixExpression();
        mult.setOperator(org.mozilla.javascript.Token.MUL);
        mult.setLeft(x);
        mult.setRight(two);

        // (x * 2) + 1
        InfixExpression add = new InfixExpression();
        add.setOperator(org.mozilla.javascript.Token.ADD);
        add.setLeft(mult);
        add.setRight(one);

        // ((x * 2) + 1) % 2
        InfixExpression mod = new InfixExpression();
        mod.setOperator(org.mozilla.javascript.Token.MOD);
        mod.setLeft(add);
        NumberLiteral twoClone = new NumberLiteral();
        twoClone.setValue("2");
        mod.setRight(twoClone);

        // 最终比较 == 1
        InfixExpression compare = new InfixExpression();
        compare.setOperator(org.mozilla.javascript.Token.EQ);
        compare.setLeft(mod);
        NumberLiteral oneClone = new NumberLiteral();
        oneClone.setValue("1");
        compare.setRight(oneClone);

        return compare;
    }

    /**
     * 创建虚假的控制流分支
     */
    private IfStatement createBogusControlFlow() {
        // 创建永远为false的条件
        NumberLiteral zero = new NumberLiteral();
        zero.setValue("0");

        NumberLiteral one = new NumberLiteral();
        one.setValue("1");

        InfixExpression falseCondition = new InfixExpression();
        falseCondition.setOperator(org.mozilla.javascript.Token.GT);
        falseCondition.setLeft(zero);
        falseCondition.setRight(one);

        // 创建虚假代码块
        Block bogusBlock = new Block();
        ExpressionStatement bogusStmt = new ExpressionStatement();

        // alert("This will never execute")
        FunctionCall alert = new FunctionCall();
        Name alertName = new Name();
        alertName.setIdentifier("console");
        PropertyGet consoleDotLog = new PropertyGet();
        consoleDotLog.setTarget(alertName);
        Name logName = new Name();
        logName.setIdentifier("log");
        consoleDotLog.setProperty(logName);

        StringLiteral bogusMsg = new StringLiteral();
        bogusMsg.setValue("This will never execute - " + random.nextInt());
        bogusMsg.setQuoteCharacter('"');

        alert.setTarget(consoleDotLog);
        alert.addArgument(bogusMsg);
        bogusStmt.setExpression(alert);
        bogusBlock.addStatement(bogusStmt);

        IfStatement bogusIf = new IfStatement();
        bogusIf.setCondition(falseCondition);
        bogusIf.setThenPart(bogusBlock);

        return bogusIf;
    }

    /**
     * 获取Block中的所有语句
     */
    private List<AstNode> getBlockStatements(Block block) {
        List<AstNode> statements = new ArrayList<AstNode>();
        for (AstNode node : block) {
            statements.add(node);
        }
        return statements;
    }

    /**
     * 转换函数体为控制流平坦化结构
     */
    private void flattenFunctionBody(FunctionNode function) {
        AstNode body = function.getBody();
        if (!(body instanceof Block)) {
            return;
        }

        Block originalBlock = (Block) body;
        List<AstNode> originalStatements = getBlockStatements(originalBlock);

        if (originalStatements.size() < 2) {
            return; // 太简单的函数不需要混淆
        }

        // 创建新的函数体
        Block newBody = new Block();

        // 添加状态变量声明
        newBody.addStatement(createStateVariable());

        // 添加虚假控制流
        newBody.addStatement(createBogusControlFlow());

        // 创建主控制循环
        WhileLoop mainLoop = new WhileLoop();

        // 循环条件：不透明谓词 (总是为true)
        mainLoop.setCondition(createOpaquePredicateTrue());

        // 创建switch语句
        SwitchStatement switchStmt = new SwitchStatement();
        Name switchVar = new Name();
        switchVar.setIdentifier(stateVarName);
        switchStmt.setExpression(switchVar);

        // 为每个原始语句创建case
        for (int i = 0; i < originalStatements.size(); i++) {
            SwitchCase caseStmt = new SwitchCase();
            NumberLiteral caseValue = new NumberLiteral();
            caseValue.setValue(String.valueOf(i));
            caseStmt.setExpression(caseValue);

            // 添加原始语句
            Block caseBlock = new Block();
            caseBlock.addStatement(originalStatements.get(i));

            // 添加状态转移
            if (i < originalStatements.size() - 1) {
                ExpressionStatement stateUpdate = new ExpressionStatement();
                Assignment assignment = new Assignment();

                Name stateVar = new Name();
                stateVar.setIdentifier(stateVarName);
                assignment.setLeft(stateVar);
                assignment.setOperator(org.mozilla.javascript.Token.ASSIGN);

                NumberLiteral nextState = new NumberLiteral();
                nextState.setValue(String.valueOf(i + 1));
                assignment.setRight(nextState);

                stateUpdate.setExpression(assignment);
                caseBlock.addStatement(stateUpdate);

                // 添加continue
                ContinueStatement continueStmt = new ContinueStatement();
                caseBlock.addStatement(continueStmt);
            } else {
                // 最后一个case，添加return或break
                BreakStatement breakStmt = new BreakStatement();
                caseBlock.addStatement(breakStmt);
            }

            caseStmt.setStatements(caseBlock);
            switchStmt.addCase(caseStmt);
        }

        // 添加default case防止无限循环
        SwitchCase defaultCase = new SwitchCase();
        Block defaultBlock = new Block();
        BreakStatement defaultBreak = new BreakStatement();
        defaultBlock.addStatement(defaultBreak);
        defaultCase.setStatements(defaultBlock);
        switchStmt.addCase(defaultCase);

        Block loopBody = new Block();
        loopBody.addStatement(switchStmt);
        mainLoop.setBody(loopBody);

        newBody.addStatement(mainLoop);

        // 替换原函数体
        function.setBody(newBody);
    }

    /**
     * 混淆if语句为复杂的三元表达式和状态机
     */
    private AstNode obfuscateIfStatement(IfStatement ifStmt) {
        // 如果条件过于简单，先添加不透明谓词
        AstNode condition = ifStmt.getCondition();

        // 创建混淆后的条件：(原条件 && 不透明谓词)
        InfixExpression obfuscatedCondition = new InfixExpression();
        obfuscatedCondition.setOperator(org.mozilla.javascript.Token.AND);
        obfuscatedCondition.setLeft(condition);
        obfuscatedCondition.setRight(createOpaquePredicateTrue());

        ifStmt.setCondition(obfuscatedCondition);

        return ifStmt;
    }

    @Override
    public boolean visit(AstNode node) {
        if (node instanceof FunctionNode) {
            FunctionNode function = (FunctionNode) node;
            // 跳过太小的函数避免过度混淆
            if (function.getBody() instanceof Block) {
                Block body = (Block) function.getBody();
                if (body.size() >= 3) { // 只对有一定复杂度的函数进行处理
                    flattenFunctionBody(function);
                }
            }
        } else if (node instanceof IfStatement) {
            IfStatement ifStmt = (IfStatement) node;
            // 为if语句添加不透明谓词
            return obfuscateIfStatement(ifStmt) != null;
        }

        return true;
    }
}