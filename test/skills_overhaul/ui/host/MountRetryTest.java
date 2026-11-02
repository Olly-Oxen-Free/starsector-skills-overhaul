package skills_overhaul.ui.host;

import skills_overhaul.ui.host.MountRetry.Status;

public class MountRetryTest {

    /** Fake attempt: fails until call number succeedOn (0 = never), counts calls. */
    static final class FakeAttempt implements MountRetry.Attempt {
        final int succeedOn;
        int calls;
        FakeAttempt(int succeedOn) { this.succeedOn = succeedOn; }
        @Override public boolean attempt() {
            calls++;
            return succeedOn > 0 && calls >= succeedOn;
        }
    }

    public static void main(String[] args) {
        exhaustsAfter180FramesOnce();
        stopsOnFirstSuccess();
        succeedsOnFrame180();
        armedByListenerAlone();
        armedByPollAlone();
        bothTriggersOneSequence();
        pollRearmsAfterTabLeaves();
        exhaustionDoesNotRearmSameOpen();
        leavingMidSequenceStartsFresh();
        idleWhenNeverArmed();
        System.out.println("ALL PASSED");
    }

    static void exhaustsAfter180FramesOnce() {
        MountRetry r = new MountRetry();
        FakeAttempt a = new FakeAttempt(0);
        r.onPoll(true);
        int exhausted = 0;
        for (int f = 1; f <= 400; f++) {
            r.onPoll(true);
            Status s = r.tick(a);
            if (s == Status.EXHAUSTED) {
                exhausted++;
                assert f == MountRetry.MAX_FRAMES : "exhausted on frame " + f;
            } else if (f < MountRetry.MAX_FRAMES) {
                assert s == Status.RETRYING : "frame " + f + ": " + s;
            } else {
                assert s == Status.IDLE : "frame " + f + ": " + s;
            }
        }
        assert MountRetry.MAX_FRAMES == 180;
        assert exhausted == 1 : "exhausted " + exhausted + " times";
        assert a.calls == 180 : "attempts " + a.calls;
    }

    static void stopsOnFirstSuccess() {
        MountRetry r = new MountRetry();
        FakeAttempt a = new FakeAttempt(1);
        r.onPoll(true);
        assert r.tick(a) == Status.SUCCEEDED;
        for (int f = 0; f < 50; f++) {
            r.onPoll(true);
            assert r.tick(a) == Status.IDLE;
        }
        assert a.calls == 1 : "attempts " + a.calls;

        MountRetry r2 = new MountRetry();
        FakeAttempt a2 = new FakeAttempt(7);
        r2.armFromListener();
        Status last = null;
        for (int f = 0; f < 20; f++) {
            Status s = r2.tick(a2);
            if (s != Status.IDLE) last = s;
        }
        assert last == Status.SUCCEEDED;
        assert a2.calls == 7 : "attempts " + a2.calls;
    }

    static void succeedsOnFrame180() {
        MountRetry r = new MountRetry();
        FakeAttempt a = new FakeAttempt(180);
        r.onPoll(true);
        Status last = null;
        int exhausted = 0;
        for (int f = 0; f < 300; f++) {
            Status s = r.tick(a);
            if (s == Status.EXHAUSTED) exhausted++;
            if (s != Status.IDLE) last = s;
        }
        assert last == Status.SUCCEEDED : "last " + last;
        assert exhausted == 0;
        assert a.calls == 180;
    }

    static void armedByListenerAlone() {
        MountRetry r = new MountRetry();
        assert !r.isArmed();
        r.armFromListener();
        assert r.isArmed();
        FakeAttempt a = new FakeAttempt(1);
        assert r.tick(a) == Status.SUCCEEDED;
        assert a.calls == 1;
    }

    static void armedByPollAlone() {
        MountRetry r = new MountRetry();
        r.onPoll(false);
        assert !r.isArmed();
        r.onPoll(true);
        assert r.isArmed();
        FakeAttempt a = new FakeAttempt(1);
        assert r.tick(a) == Status.SUCCEEDED;
        assert a.calls == 1;
    }

    static void bothTriggersOneSequence() {
        // Listener fires while the old tab is still current; the poll sees the old tab, then
        // CHARACTER on the next frame. One sequence only.
        MountRetry r = new MountRetry();
        FakeAttempt a = new FakeAttempt(3);
        int sequences = 0;
        r.armFromListener();
        r.onPoll(false);           // frame 0: switch not complete yet
        assert r.isArmed() : "old-tab poll cancelled the listener arming";
        for (int f = 1; f <= 30; f++) {
            r.onPoll(true);
            if (f == 2) r.armFromListener(); // a late duplicate trigger
            Status s = r.tick(a);
            if (s == Status.SUCCEEDED) sequences++;
        }
        assert sequences == 1 : "sequences " + sequences;
        assert a.calls == 3 : "attempts " + a.calls;

        // Poll first, then listener for the same open.
        MountRetry r2 = new MountRetry();
        FakeAttempt a2 = new FakeAttempt(1);
        r2.onPoll(true);
        r2.armFromListener();
        assert r2.tick(a2) == Status.SUCCEEDED;
        r2.armFromListener();
        r2.onPoll(true);
        assert r2.tick(a2) == Status.IDLE;
        assert a2.calls == 1;
    }

    static void pollRearmsAfterTabLeaves() {
        MountRetry r = new MountRetry();
        FakeAttempt a = new FakeAttempt(1);
        // Tab already CHARACTER when the host starts.
        r.onPoll(true);
        assert r.tick(a) == Status.SUCCEEDED;
        r.onPoll(true);
        assert r.tick(a) == Status.IDLE;
        // CHARACTER -> other -> CHARACTER
        r.onPoll(false);
        assert !r.isArmed();
        r.onPoll(true);
        assert r.isArmed() : "poll did not re-arm on reopen";
        assert r.tick(a) == Status.SUCCEEDED;
        assert a.calls == 2 : "attempts " + a.calls;

        // Listener re-arms after leave too.
        r.onPoll(false);
        r.armFromListener();
        assert r.isArmed();
        assert r.tick(a) == Status.SUCCEEDED;
        assert a.calls == 3;
    }

    static void exhaustionDoesNotRearmSameOpen() {
        MountRetry r = new MountRetry();
        FakeAttempt a = new FakeAttempt(0);
        r.onPoll(true);
        int exhausted = 0;
        for (int f = 0; f < 200; f++) {
            r.onPoll(true);
            if (r.tick(a) == Status.EXHAUSTED) exhausted++;
        }
        assert exhausted == 1;
        assert a.calls == 180;
        // Reopen: a fresh sequence and a fresh single exhaustion.
        r.onPoll(false);
        for (int f = 0; f < 200; f++) {
            r.onPoll(true);
            if (r.tick(a) == Status.EXHAUSTED) exhausted++;
        }
        assert exhausted == 2 : "exhausted " + exhausted;
        assert a.calls == 360 : "attempts " + a.calls;
    }

    static void leavingMidSequenceStartsFresh() {
        MountRetry r = new MountRetry();
        FakeAttempt a = new FakeAttempt(0);
        r.onPoll(true);
        for (int f = 0; f < 50; f++) r.tick(a);
        assert r.getFrames() == 50;
        r.onPoll(false);        // tab left before success: sequence dropped
        assert !r.isArmed();
        assert r.tick(a) == Status.IDLE;
        r.onPoll(true);         // reopen: fresh 180-frame budget
        assert r.isArmed();
        assert r.getFrames() == 0;
        int exhaustedAt = -1;
        for (int f = 1; f <= 200; f++) {
            if (r.tick(a) == Status.EXHAUSTED) exhaustedAt = f;
        }
        assert exhaustedAt == 180 : "exhausted at " + exhaustedAt;
    }

    static void idleWhenNeverArmed() {
        MountRetry r = new MountRetry();
        FakeAttempt a = new FakeAttempt(1);
        for (int f = 0; f < 10; f++) {
            r.onPoll(false);
            assert r.tick(a) == Status.IDLE;
        }
        assert a.calls == 0;
        r.onPoll(true);
        r.reset();
        assert !r.isArmed();
        assert r.tick(a) == Status.IDLE;
    }
}
