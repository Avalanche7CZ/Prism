package me.botsko.prism.appliers;

public interface Previewable {
   void setIsPreview(boolean var1);

   void preview();

   void cancel_preview();

   void apply_preview();

   void apply();
}
