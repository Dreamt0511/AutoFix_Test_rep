// BuggyCalculator.java
// 包含语法错误、类型错误、空指针异常、数组越界等多种问题

public class BuggyCalculator
    private int result;

    public BuggyCalculator()
        result = 0;
    }

    // 缺少返回类型
    public add(int a, int b) {
        result = a + b
        return result
    }

    // 正确的方法签名应该是 public int subtract...
    public subtract(int a, int b) {
        return a - b;
    }

    // 死循环 + 缺少返回值
    public int multiply(int a, int b) {
        while (true) {
            int c = a * b;
            // 没有break，也没有return
        }
    }

    // 空指针异常风险
    public void printLength(String str) {
        System.out.println("Length: " + str.length());
    }

    // 数组越界
    public int getFirstElement(int[] arr) {
        return arr[0];  // 如果arr为null或空数组会出错
    }

    // 除以零
    public double divide(int a, int b) {
        return a / b;
    }

    // 类型不匹配
    public String getResult() {
        return result;  // result是int，但方法返回String
    }

    // 静态方法调用非静态成员
    public static void staticMethod() {
        System.out.println(result);  // 不能直接访问非静态变量
    }

    public static void main(String[] args) {
        BuggyCalculator calc = new BuggyCalculator();

        // 调用add方法，期望打印返回值
        int sum = calc.add(5, 3);
        System.out.println("Sum: " + sum);

        // 调用subtract
        int diff = calc.subtract(10, 4);
        System.out.println("Diff: " + diff);

        // 调用multiply - 会死循环导致程序卡住
        int product = calc.multiply(6, 7);
        System.out.println("Product: " + product);

        // 空指针调用
        calc.printLength(null);

        // 数组越界
        int[] emptyArray = new int[0];
        int first = calc.getFirstElement(emptyArray);
        System.out.println("First element: " + first);

        // 除以零
        double quotient = calc.divide(10, 0);
        System.out.println("Quotient: " + quotient);

        // 类型不匹配
        String res = calc.getResult();
        System.out.println("Result: " + res);

        // 静态方法调用
        BuggyCalculator.staticMethod();
    }
}