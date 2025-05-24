package me.botsko.prism.commandlibs;

import java.util.List;

public interface SubHandler {
   void handle(CallInfo var1);

   List handleComplete(CallInfo var1);
}
