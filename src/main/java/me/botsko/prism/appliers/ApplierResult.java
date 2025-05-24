package me.botsko.prism.appliers;

import java.util.ArrayList;
import java.util.HashMap;
import me.botsko.prism.actionlibs.QueryParameters;

public class ApplierResult {
   private final int changes_applied;
   private final int changes_skipped;
   private final int changes_planned;
   private final boolean is_preview;
   private final HashMap entities_moved;
   private final ArrayList blockStateChanges;
   private final QueryParameters params;

   public ApplierResult(boolean is_preview, int changes_applied, int changes_skipped, int changes_planned, ArrayList blockStateChanges, QueryParameters params, HashMap entities_moved) {
      this.changes_applied = changes_applied;
      this.changes_skipped = changes_skipped;
      this.changes_planned = changes_planned;
      this.is_preview = is_preview;
      this.blockStateChanges = blockStateChanges;
      this.params = params;
      this.entities_moved = entities_moved;
   }

   public int getChangesApplied() {
      return this.changes_applied;
   }

   public int getChangesSkipped() {
      return this.changes_skipped;
   }

   public int getChangesPlanned() {
      return this.changes_planned;
   }

   public boolean isPreview() {
      return this.is_preview;
   }

   public HashMap getEntitiesMoved() {
      return this.entities_moved;
   }

   public ArrayList getBlockStateChanges() {
      return this.blockStateChanges;
   }

   public PrismProcessType getProcessType() {
      return this.params.getProcessType();
   }

   public QueryParameters getParameters() {
      return this.params;
   }
}
