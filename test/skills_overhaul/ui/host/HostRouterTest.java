package skills_overhaul.ui.host;

public class HostRouterTest {
    public static void main(String[] args) {
        HostRouter.reset();
        assert HostRouter.claimMessage() : "first claim succeeds";
        assert !HostRouter.claimMessage() : "SiC plus probe failure shows one message";
        assert !HostRouter.claimMessage() : "later tab opens do not repeat";

        HostRouter.reset();
        assert HostRouter.claimMessage() : "reload re-arms the message";

        assert HostRouter.messageText("K").contains("K") : "text names the hotkey";

        System.out.println("ALL PASSED");
    }
}
