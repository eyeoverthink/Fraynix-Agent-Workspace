package fraymus.workspace;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Bounded Fray v1 arithmetic used by built-in Workbench recipes. */
final class FrayFormula {
    private final double[] constants;
    private final byte[] instructions;

    private FrayFormula(double[] constants, byte[] instructions) {
        this.constants = constants;
        this.instructions = instructions;
    }

    static FrayFormula read(byte[] bytes, String variable) {
        var input = ByteBuffer.wrap(bytes);
        if (input.remaining() < 13 || input.getInt() != 0x46524159 || input.get() != 1)
            throw new IllegalArgumentException("Unsupported Fray conversion program header");
        int count = Short.toUnsignedInt(input.getShort());
        int variables = Short.toUnsignedInt(input.getShort());
        int length = input.getInt();
        byte[] name = (variable + "\0").getBytes(StandardCharsets.US_ASCII);
        if (count < 1 || count > 8 || variables != 1 || length < 1 || length > 64
                || input.remaining() != count * Double.BYTES + name.length + length)
            throw new IllegalArgumentException("Invalid Fray conversion program size");
        double[] constants = new double[count];
        for (int i = 0; i < count; i++) {
            constants[i] = input.getDouble();
            if (!Double.isFinite(constants[i])) throw new IllegalArgumentException("Invalid Fray constant");
        }
        byte[] actualName = new byte[name.length];
        input.get(actualName);
        if (!Arrays.equals(name, actualName)) throw new IllegalArgumentException("Unexpected Fray input name");
        byte[] instructions = new byte[length];
        input.get(instructions);
        var formula = new FrayFormula(constants, instructions);
        formula.evaluate(0);
        return formula;
    }

    double evaluate(double variable) {
        var code = ByteBuffer.wrap(instructions);
        double[] stack = new double[instructions.length];
        int size = 0;
        while (code.hasRemaining()) {
            int opcode = Byte.toUnsignedInt(code.get());
            switch (opcode) {
                case 0x01, 0x02 -> {
                    if (code.remaining() < 2) throw new IllegalArgumentException("Truncated Fray operand");
                    int index = Short.toUnsignedInt(code.getShort());
                    if (opcode == 0x01 && index < constants.length) stack[size++] = constants[index];
                    else if (opcode == 0x02 && index == 0) stack[size++] = variable;
                    else throw new IllegalArgumentException("Unknown Fray operand");
                }
                case 0x10, 0x12, 0x13 -> {
                    if (size < 2) throw new IllegalArgumentException("Fray instruction needs two operands");
                    double right = stack[--size];
                    double left = stack[size - 1];
                    double result = switch (opcode) {
                        case 0x10 -> left + right;
                        case 0x12 -> left * right;
                        default -> left / right;
                    };
                    if (!Double.isFinite(result)) throw new IllegalArgumentException("Non-finite Fray result");
                    stack[size - 1] = result;
                }
                case 0xff -> {
                    if (code.hasRemaining() || size != 1) throw new IllegalArgumentException("Invalid Fray return");
                    return stack[0];
                }
                default -> throw new IllegalArgumentException("Unsupported Fray instruction");
            }
        }
        throw new IllegalArgumentException("Fray program has no return");
    }
}
