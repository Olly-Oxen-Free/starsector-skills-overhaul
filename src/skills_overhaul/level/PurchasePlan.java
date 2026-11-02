package skills_overhaul.level;

import skills_overhaul.state.SkillMemory;
import skills_overhaul.state.Tree;
import skills_overhaul.tree.Node;
import skills_overhaul.tree.PendingPurchaseTopoSort;
import skills_overhaul.tree.PrereqValidator;
import skills_overhaul.tree.TreeDef;

import java.util.*;

/**
 * Pure dry run of a purchase batch for one tab. Orders the batch per TreeDef with
 * {@link PendingPurchaseTopoSort} (concatenated in def order), then validates each
 * node with {@link PrereqValidator#check} and spends it with {@link SpendService#spend}
 * on ONE copy of the input snapshot. The input map is never modified.
 */
public final class PurchasePlan {

    /** Why a batch failed. */
    public enum Reason { UNKNOWN, OWNED, CYCLE, PREREQ, TIER, BUDGET }

    /** Dry-run outcome: success with the purchase order, or the first failing node. */
    public static final class Result {
        public final boolean success;
        /** Nodes in purchase order (topological per def); empty on failure. */
        public final List<String> order;
        /** Offending node id on failure, else null. */
        public final String failedNodeId;
        /** Failure reason, else null. */
        public final Reason reason;
        /** Points missing for BUDGET failures, else 0. */
        public final int shortBy;
        /** The snapshot copy after the dry run (all nodes applied on success). */
        public final Map<String,Object> after;

        private Result(boolean success, List<String> order, String failedNodeId,
                       Reason reason, int shortBy, Map<String,Object> after) {
            this.success = success;
            this.order = Collections.unmodifiableList(order);
            this.failedNodeId = failedNodeId;
            this.reason = reason;
            this.shortBy = shortBy;
            this.after = after;
        }

        static Result ok(List<String> order, Map<String,Object> after) {
            return new Result(true, order, null, null, 0, after);
        }

        static Result fail(String nodeId, Reason reason, int shortBy, Map<String,Object> after) {
            return new Result(false, new ArrayList<String>(), nodeId, reason, shortBy, after);
        }
    }

    /**
     * Dry-runs {@code batch} against a copy of {@code snapshot}.
     *
     * @param snapshot whole-tab snapshot (points, pools, lock, purchased flags); unchanged
     * @param tab      tab whose pools are spent from
     * @param defs     every TreeDef of the tab, in display order
     * @param batch    node ids to buy; empty gives success with an empty order
     */
    public static Result dryRun(Map<String,Object> snapshot, Tree tab,
                                List<TreeDef> defs, Collection<String> batch) {
        Map<String,Object> m = new HashMap<String,Object>(snapshot);
        Set<String> all = new LinkedHashSet<String>(batch);

        // Every id must belong to a def of this tab and not be owned yet.
        Map<String, TreeDef> defOf = new HashMap<String, TreeDef>();
        for (String id : all) {
            TreeDef found = null;
            for (TreeDef d : defs) {
                if (d.getNode(id) != null) { found = d; break; }
            }
            if (found == null) return Result.fail(id, Reason.UNKNOWN, 0, m);
            if (Boolean.TRUE.equals(m.get(SkillMemory.purchasedKey(id)))) {
                return Result.fail(id, Reason.OWNED, 0, m);
            }
            defOf.put(id, found);
        }

        List<String> order = new ArrayList<String>();
        for (TreeDef d : defs) {
            Set<String> defPending = new LinkedHashSet<String>();
            for (String id : all) if (defOf.get(id) == d) defPending.add(id);
            if (defPending.isEmpty()) continue;
            try {
                order.addAll(PendingPurchaseTopoSort.sort(d, defPending));
            } catch (IllegalStateException e) {
                return Result.fail(defPending.iterator().next(), Reason.CYCLE, 0, m);
            }
        }

        // Validate against owned (grows as nodes apply) + the whole batch: the
        // batch lands as one unit, so a tier-(y-1) batch node supports tier y
        // even when no prereq edge orders it first.
        for (String id : order) {
            TreeDef d = defOf.get(id);
            PrereqValidator.Check c = PrereqValidator.check(d, m, all, id);
            if (c == PrereqValidator.Check.PREREQ) return Result.fail(id, Reason.PREREQ, 0, m);
            if (c == PrereqValidator.Check.TIER) return Result.fail(id, Reason.TIER, 0, m);
            if (c != PrereqValidator.Check.OK) return Result.fail(id, Reason.UNKNOWN, 0, m);

            Node n = d.getNode(id);
            SpendService.Result r = SpendService.spend(m, tab, id, n.getCost());
            if (!r.success) return Result.fail(id, Reason.BUDGET, shortBy(r, n.getCost()), m);
        }
        return Result.ok(order, m);
    }

    /**
     * Points a failed spend was short. {@link SpendService#spend} fills the per-pool
     * takes before it fails, so the shortfall is the cost minus what it could take.
     */
    public static int shortBy(SpendService.Result r, int cost) {
        int taken = r.fromTreeLocked + r.fromFreeUni + r.fromMigrationToPilot
                  + r.fromMigrationToCommander + r.fromMigrationToAdmin;
        return Math.max(0, cost - taken);
    }

    private PurchasePlan() {}
}
