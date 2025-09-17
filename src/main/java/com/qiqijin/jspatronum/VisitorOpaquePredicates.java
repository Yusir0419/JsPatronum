package com.qiqijin.jspatronum;

import java.util.Random;

import org.mozilla.javascript.ast.*;

/**
 * 不透明谓词混淆器
 * 在代码中插入看起来复杂但结果可预测的表达式，增加逆向分析难度
 *
 * @author Gin
 */
public class VisitorOpaquePredicates implements NodeVisitor {

    private Random random = new Random();
    private int predicateCounter = 0;

    /**
     * 生成总是为true的不透明谓词
     * 使用数学恒等式确保结果可预测
     */
    private AstNode createAlwaysTruePredicate() {
        predicateCounter++;
        int choice = random.nextInt(4);

        switch (choice) {
            case 0:
                // (x * x) >= 0 对于任意实数x总是为真
                return createSquareNonNegative();
            case 1:
                // (x * 2 + 1) % 2 == 1 对于任意整数x总是为真
                return createOddExpression();
            case 2:
                // Math.abs(x) >= 0 总是为真
                return createAbsoluteNonNegative();
            default:
                // (x | 0) == (x | 0) 总是为真（位运算恒等式）
                return createBitwiseIdentity();
        }
    }

    /**
     * 生成总是为false的不透明谓词
     */
    private AstNode createAlwaysFalsePredicate() {
        int choice = random.nextInt(3);

        switch (choice) {
            case 0:
                // (x * x) < 0 对于任意实数x总是为假
                return createSquareNegative();
            case 1:
                // (x & (x + 1)) < 0 总是为假（位运算性质）
                return createBitwiseNegative();
            default:
                // Math.abs(x) < 0 总是为假
                return createAbsoluteNegative();
        }
    }

    private AstNode createSquareNonNegative() {
        int x = random.nextInt(100) + 1;

        NumberLiteral num = new NumberLiteral();
        num.setValue(String.valueOf(x));

        // x * x
        InfixExpression square = new InfixExpression();
        square.setOperator(org.mozilla.javascript.Token.MUL);
        square.setLeft(num);
        square.setRight((NumberLiteral) num.clone());

        // (x * x) >= 0
        NumberLiteral zero = new NumberLiteral();
        zero.setValue("0");

        InfixExpression compare = new InfixExpression();
        compare.setOperator(org.mozilla.javascript.Token.GE);
        compare.setLeft(square);
        compare.setRight(zero);

        return compare;
    }

    private AstNode createOddExpression() {
        int x = random.nextInt(50);

        NumberLiteral num = new NumberLiteral();
        num.setValue(String.valueOf(x));

        NumberLiteral two = new NumberLiteral();
        two.setValue("2");

        NumberLiteral one = new NumberLiteral();
        one.setValue("1");

        // x * 2
        InfixExpression mult = new InfixExpression();
        mult.setOperator(org.mozilla.javascript.Token.MUL);
        mult.setLeft(num);
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
        mod.setRight((NumberLiteral) two.clone());

        // 结果 == 1
        InfixExpression compare = new InfixExpression();
        compare.setOperator(org.mozilla.javascript.Token.EQ);
        compare.setLeft(mod);
        compare.setRight((NumberLiteral) one.clone());

        return compare;
    }

    private AstNode createAbsoluteNonNegative() {
        int x = random.nextInt(200) - 100; // -100 to 100

        FunctionCall mathAbs = new FunctionCall();
        PropertyGet mathProp = new PropertyGet();

        Name math = new Name();
        math.setIdentifier("Math");
        Name abs = new Name();
        abs.setIdentifier("abs");

        mathProp.setTarget(math);
        mathProp.setProperty(abs);
        mathAbs.setTarget(mathProp);

        NumberLiteral num = new NumberLiteral();
        num.setValue(String.valueOf(x));
        mathAbs.addArgument(num);

        // Math.abs(x) >= 0
        NumberLiteral zero = new NumberLiteral();
        zero.setValue("0");

        InfixExpression compare = new InfixExpression();
        compare.setOperator(org.mozilla.javascript.Token.GE);
        compare.setLeft(mathAbs);
        compare.setRight(zero);

        return compare;
    }

    private AstNode createBitwiseIdentity() {
        int x = random.nextInt(1000);

        NumberLiteral num = new NumberLiteral();
        num.setValue(String.valueOf(x));

        NumberLiteral zero = new NumberLiteral();
        zero.setValue("0");

        // x | 0
        InfixExpression bitwiseOr1 = new InfixExpression();
        bitwiseOr1.setOperator(org.mozilla.javascript.Token.BITOR);
        bitwiseOr1.setLeft(num);
        bitwiseOr1.setRight(zero);

        // x | 0 (右侧)
        InfixExpression bitwiseOr2 = new InfixExpression();
        bitwiseOr2.setOperator(org.mozilla.javascript.Token.BITOR);
        bitwiseOr2.setLeft((NumberLiteral) num.clone());
        bitwiseOr2.setRight((NumberLiteral) zero.clone());

        // (x | 0) == (x | 0)
        InfixExpression compare = new InfixExpression();
        compare.setOperator(org.mozilla.javascript.Token.EQ);
        compare.setLeft(bitwiseOr1);
        compare.setRight(bitwiseOr2);

        return compare;
    }

    private AstNode createSquareNegative() {
        int x = random.nextInt(50) + 1;

        NumberLiteral num = new NumberLiteral();
        num.setValue(String.valueOf(x));

        // x * x
        InfixExpression square = new InfixExpression();
        square.setOperator(org.mozilla.javascript.Token.MUL);
        square.setLeft(num);
        square.setRight((NumberLiteral) num.clone());

        // (x * x) < 0
        NumberLiteral zero = new NumberLiteral();
        zero.setValue("0");

        InfixExpression compare = new InfixExpression();
        compare.setOperator(org.mozilla.javascript.Token.LT);
        compare.setLeft(square);
        compare.setRight(zero);

        return compare;
    }

    private AstNode createBitwiseNegative() {
        int x = random.nextInt(100);

        NumberLiteral num = new NumberLiteral();
        num.setValue(String.valueOf(x));

        NumberLiteral one = new NumberLiteral();
        one.setValue("1");

        // x + 1
        InfixExpression add = new InfixExpression();
        add.setOperator(org.mozilla.javascript.Token.ADD);
        add.setLeft(num);
        add.setRight(one);

        // x & (x + 1)
        InfixExpression bitAnd = new InfixExpression();
        bitAnd.setOperator(org.mozilla.javascript.Token.BITAND);
        bitAnd.setLeft((NumberLiteral) num.clone());
        bitAnd.setRight(add);

        // (x & (x + 1)) < 0
        NumberLiteral zero = new NumberLiteral();
        zero.setValue("0");

        InfixExpression compare = new InfixExpression();
        compare.setOperator(org.mozilla.javascript.Token.LT);
        compare.setLeft(bitAnd);
        compare.setRight(zero);

        return compare;
    }

    private AstNode createAbsoluteNegative() {
        int x = random.nextInt(100);

        FunctionCall mathAbs = new FunctionCall();
        PropertyGet mathProp = new PropertyGet();

        Name math = new Name();
        math.setIdentifier("Math");
        Name abs = new Name();
        abs.setIdentifier("abs");

        mathProp.setTarget(math);
        mathProp.setProperty(abs);
        mathAbs.setTarget(mathProp);

        NumberLiteral num = new NumberLiteral();
        num.setValue(String.valueOf(x));
        mathAbs.addArgument(num);

        // Math.abs(x) < 0
        NumberLiteral zero = new NumberLiteral();
        zero.setValue("0");

        InfixExpression compare = new InfixExpression();
        compare.setOperator(org.mozilla.javascript.Token.LT);
        compare.setLeft(mathAbs);
        compare.setRight(zero);

        return compare;
    }

    /**
     * 为if语句添加不透明谓词
     */
    private void obfuscateIfStatement(IfStatement ifStmt) {
        AstNode condition = ifStmt.getCondition();

        // 创建复合条件：(原条件 && 总是为true的谓词) || (总是为false的谓词)
        InfixExpression truePredicate = (InfixExpression) createAlwaysTruePredicate();
        InfixExpression falsePredicate = (InfixExpression) createAlwaysFalsePredicate();

        // 原条件 && 真谓词
        InfixExpression leftPart = new InfixExpression();
        leftPart.setOperator(org.mozilla.javascript.Token.AND);
        leftPart.setLeft(condition);
        leftPart.setRight(truePredicate);

        // (原条件 && 真谓词) || 假谓词
        InfixExpression obfuscatedCondition = new InfixExpression();
        obfuscatedCondition.setOperator(org.mozilla.javascript.Token.OR);
        obfuscatedCondition.setLeft(leftPart);
        obfuscatedCondition.setRight(falsePredicate);

        ifStmt.setCondition(obfuscatedCondition);
    }

    /**
     * 为while循环添加不透明谓词
     */
    private void obfuscateWhileLoop(WhileLoop whileLoop) {
        AstNode condition = whileLoop.getCondition();

        // 原条件 && 总是为true的谓词
        InfixExpression obfuscatedCondition = new InfixExpression();
        obfuscatedCondition.setOperator(org.mozilla.javascript.Token.AND);
        obfuscatedCondition.setLeft(condition);
        obfuscatedCondition.setRight(createAlwaysTruePredicate());

        whileLoop.setCondition(obfuscatedCondition);
    }

    @Override
    public boolean visit(AstNode node) {
        if (node instanceof IfStatement) {
            obfuscateIfStatement((IfStatement) node);
        } else if (node instanceof WhileLoop) {
            obfuscateWhileLoop((WhileLoop) node);
        } else if (node instanceof ForLoop) {
            ForLoop forLoop = (ForLoop) node;
            if (forLoop.getCondition() != null) {
                // 为for循环的条件部分添加不透明谓词
                AstNode condition = forLoop.getCondition();
                InfixExpression obfuscated = new InfixExpression();
                obfuscated.setOperator(org.mozilla.javascript.Token.AND);
                obfuscated.setLeft(condition);
                obfuscated.setRight(createAlwaysTruePredicate());
                forLoop.setCondition(obfuscated);
            }
        }

        return true;
    }
}