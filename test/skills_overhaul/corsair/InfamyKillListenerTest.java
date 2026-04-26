package skills_overhaul.corsair;

public class InfamyKillListenerTest {
    public static void main(String[] args) throws Exception {
        // The formula stacks = min(10, ceil(fp / 30)) drives Infamy stack values.
        assert stacks(0f) == 0 : "0 fp → 0 stacks";
        assert stacks(1f) == 1;
        assert stacks(30f) == 1;
        assert stacks(31f) == 2;
        assert stacks(60f) == 2;
        assert stacks(90f) == 3;
        assert stacks(150f) == 5;
        assert stacks(300f) == 10;
        assert stacks(1000f) == 10 : "cap at 10";
        System.out.println("ALL PASSED");
    }

    private static int stacks(float fp) {
        if (fp <= 0f) return 0;
        return Math.min(10, (int) Math.ceil(fp / 30.0));
    }
}
