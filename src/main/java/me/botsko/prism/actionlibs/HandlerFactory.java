package me.botsko.prism.actionlibs;

import me.botsko.prism.actions.Handler;

public class HandlerFactory {
   final Class handlerClass;

   public HandlerFactory(Class handlerClass) {
      this.handlerClass = handlerClass;
   }

   public Handler create() throws InstantiationException, IllegalAccessException {
      return (Handler)this.handlerClass.newInstance();
   }
}
