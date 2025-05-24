package me.botsko.prism.actionlibs;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class QueryResult {
   protected List actionResults = new ArrayList();
   protected final QueryParameters parameters;
   protected long queryTime;
   protected final int total_results;
   protected int per_page = 5;
   protected int total_pages = 0;
   protected int page = 1;
   protected int lastTeleportIndex = 0;

   public QueryResult(List actions, QueryParameters parameters) {
      this.actionResults = actions;
      this.parameters = parameters;
      this.setQueryTime();
      this.total_results = this.actionResults.size();
      this.setPerPage(this.per_page);
   }

   public void setQueryTime() {
      Date date = new Date();
      this.queryTime = date.getTime();
   }

   public List getActionResults() {
      return this.actionResults;
   }

   public List getPaginatedActionResults() {
      int limit = this.page * this.per_page;
      int offset = limit - this.per_page;
      if (offset <= this.total_results) {
         if (limit > this.total_results) {
            limit = this.total_results;
         }

         return this.actionResults.subList(offset, limit);
      } else {
         return null;
      }
   }

   public QueryParameters getParameters() {
      return this.parameters;
   }

   public int getTotalResults() {
      return this.total_results;
   }

   public long getQueryTime() {
      return this.queryTime;
   }

   public int getPerPage() {
      return this.per_page;
   }

   public int getLastTeleportIndex() {
      return this.lastTeleportIndex;
   }

   public int setLastTeleportIndex(int index) {
      return this.lastTeleportIndex = index;
   }

   public int getIndexOfFirstResult() {
      int index = this.page * this.per_page - this.per_page;
      return index + 1;
   }

   public void setPerPage(int per_page) {
      this.per_page = per_page;
      this.total_pages = (int)Math.ceil((double)this.total_results / (double)per_page);
   }

   public int getPage() {
      return this.page;
   }

   public void setPage(int page) {
      this.page = page;
   }

   public int getTotal_pages() {
      return this.total_pages;
   }
}
